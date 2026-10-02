package com.example.focus

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.alarm.WakeUpAlarmReceiver
import com.example.data.db.LifeTrackerDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Manages Full-System Focus Mode (BlockSite-style blocking), Allowed Apps Whitelist,
 * AlarmManager Exact Schedule / Duration persistence, and reboot/restart restoration.
 */
object FocusModeManager {

  private const val TAG = "FocusModeManager"

  private const val PREFS_NAME = "life_tracker_focus_prefs"
  private const val KEY_FOCUS_ACTIVE = "focus_active"
  private const val KEY_FOCUS_END_TIMESTAMP = "focus_end_timestamp"
  private const val KEY_ALLOWED_PACKAGES = "focus_allowed_packages"

  const val ACTION_FOCUS_MODE_ACTIVATE = "com.example.alarm.ACTION_FOCUS_MODE_ACTIVATE"
  const val ACTION_FOCUS_MODE_DEACTIVATE = "com.example.alarm.ACTION_FOCUS_MODE_DEACTIVATE"

  const val FOCUS_NOTIFICATION_CHANNEL_ID = "life_tracker_focus_channel"
  const val FOCUS_NOTIFICATION_ID = 9001

  private const val REQUEST_CODE_ACTIVATE = 8011
  private const val REQUEST_CODE_DEACTIVATE = 8012
  private const val REQUEST_CODE_EXACT_END = 8015

  // =========================================================================
  // STATE & DURATION PERSISTENCE
  // =========================================================================

  fun isFocusModeActive(context: Context): Boolean {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val active = prefs.getBoolean(KEY_FOCUS_ACTIVE, false)
    if (!active) return false

    val endTimestamp = prefs.getLong(KEY_FOCUS_END_TIMESTAMP, 0L)
    if (endTimestamp > 0L && System.currentTimeMillis() >= endTimestamp) {
      // Auto-expired
      deactivateFocusMode(context)
      return false
    }
    return true
  }

  fun getFocusSessionEndTimestamp(context: Context): Long {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getLong(KEY_FOCUS_END_TIMESTAMP, 0L)
  }

  fun getRemainingSessionMillis(context: Context): Long {
    val endTimestamp = getFocusSessionEndTimestamp(context)
    val now = System.currentTimeMillis()
    return if (endTimestamp > now) endTimestamp - now else 0L
  }

  fun getRemainingSessionMinutes(context: Context): Int {
    val millis = getRemainingSessionMillis(context)
    return ((millis + 59999L) / 60000L).toInt().coerceAtLeast(0)
  }

  private fun saveFocusState(context: Context, active: Boolean, endTimestamp: Long) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit()
      .putBoolean(KEY_FOCUS_ACTIVE, active)
      .putLong(KEY_FOCUS_END_TIMESTAMP, endTimestamp)
      .apply()
  }

  // =========================================================================
  // ALLOWED APPS WHITELIST MANAGEMENT
  // =========================================================================

  fun getAllowedPackages(context: Context): Set<String> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getStringSet(KEY_ALLOWED_PACKAGES, emptySet()) ?: emptySet()
  }

  fun setAllowedPackages(context: Context, packages: Set<String>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putStringSet(KEY_ALLOWED_PACKAGES, packages).apply()
  }

  fun addAllowedPackage(context: Context, packageName: String) {
    val current = getAllowedPackages(context).toMutableSet()
    current.add(packageName)
    setAllowedPackages(context, current)
  }

  fun removeAllowedPackage(context: Context, packageName: String) {
    val current = getAllowedPackages(context).toMutableSet()
    current.remove(packageName)
    setAllowedPackages(context, current)
  }

  /**
   * Returns immutable system packages that MUST NEVER be blocked:
   * 1. This Life Tracker app
   * 2. Phone / Default Dialer (Emergency calls)
   * 3. Home Launcher
   * 4. System UI, Android OS, and Permission Controllers
   */
  fun getCoreWhitelistedPackages(context: Context): Set<String> {
    val core = mutableSetOf(
      context.packageName,
      "com.android.systemui",
      "android",
      "com.android.phone",
      "com.android.server.telecom",
      "com.google.android.permissioncontroller",
      "com.android.permissioncontroller",
      "com.google.android.apps.safetyhub"
    )

    // Default Phone / Dialer package
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        telecom?.defaultDialerPackage?.let { core.add(it) }
      }
    } catch (_: Exception) {}

    // Any dialer intent activities
    try {
      val pm = context.packageManager
      val dialIntent = Intent(Intent.ACTION_DIAL)
      val list = pm.queryIntentActivities(dialIntent, 0)
      for (info in list) {
        info.activityInfo?.packageName?.let { core.add(it) }
      }
    } catch (_: Exception) {}

    // Default Launcher / Home App
    try {
      val pm = context.packageManager
      val homeIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
      }
      val resolveInfo = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
      resolveInfo?.activityInfo?.packageName?.let { core.add(it) }
    } catch (_: Exception) {}

    return core
  }

  /**
   * Deterministically evaluates if [packageName] is permitted to run in foreground.
   */
  fun isPackageAllowed(context: Context, packageName: String): Boolean {
    if (packageName.isBlank()) return true

    // 1. Check core whitelist (Emergency, Dialer, Launcher, SystemUI, Life Tracker)
    if (getCoreWhitelistedPackages(context).contains(packageName)) {
      return true
    }

    // 2. Check user-configured allowed apps
    val userAllowed = getAllowedPackages(context)
    return userAllowed.contains(packageName)
  }

  // =========================================================================
  // ACTIVATION MODES (DURATION & SCHEDULE)
  // =========================================================================

  /**
   * Activates Focus Mode for a specific duration in minutes (e.g. 15m, 25m Pomodoro, 60m).
   */
  fun activateFocusDuration(context: Context, durationMinutes: Int) {
    createNotificationChannel(context)
    val now = System.currentTimeMillis()
    val endTimestamp = now + (durationMinutes * 60 * 1000L)

    saveFocusState(context, active = true, endTimestamp = endTimestamp)
    scheduleFocusExactEndAlarm(context, endTimestamp)
    showFocusNotification(context, durationMinutes, isScheduled = false)

    // Sync to Room Database
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val db = LifeTrackerDatabase.getDatabase(context)
        val settings = db.userSettingsDao().getSettingsSync()
        if (settings != null) {
          db.userSettingsDao().insertOrUpdate(
            settings.copy(
              isFocusModeActive = true,
              focusSessionEndTimestamp = endTimestamp
            )
          )
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to persist focus activation to Room", e)
      }
    }
  }

  /**
   * Activates / synchronizes scheduled Focus Mode (e.g. 21:00 to 06:00).
   */
  fun activateFocusSchedule(context: Context, startMinutes: Int, endMinutes: Int) {
    createNotificationChannel(context)
    val now = Calendar.getInstance()
    val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val shouldBeActive = isCurrentTimeInFocusSchedule(nowMinutes, startMinutes, endMinutes)

    val endTimestamp = if (shouldBeActive) {
      val remainingMins = calculateRemainingMinutes(nowMinutes, endMinutes)
      System.currentTimeMillis() + (remainingMins * 60 * 1000L)
    } else {
      0L
    }

    saveFocusState(context, active = shouldBeActive, endTimestamp = endTimestamp)
    scheduleFocusAlarms(context, startMinutes, endMinutes)

    if (shouldBeActive) {
      val remainingMins = calculateRemainingMinutes(nowMinutes, endMinutes)
      showFocusNotification(context, remainingMins, isScheduled = true)
    } else {
      cancelFocusNotification(context)
    }

    CoroutineScope(Dispatchers.IO).launch {
      try {
        val db = LifeTrackerDatabase.getDatabase(context)
        val settings = db.userSettingsDao().getSettingsSync()
        if (settings != null) {
          db.userSettingsDao().insertOrUpdate(
            settings.copy(
              isFocusModeActive = shouldBeActive,
              isFocusScheduleEnabled = true,
              focusStartTimeMinutes = startMinutes,
              focusEndTimeMinutes = endMinutes,
              focusSessionEndTimestamp = endTimestamp
            )
          )
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to persist focus schedule to Room", e)
      }
    }
  }

  /**
   * Deactivates Focus Mode, cancels alarms, removes blocking overlay, and notifies completion.
   */
  fun deactivateFocusMode(context: Context) {
    saveFocusState(context, active = false, endTimestamp = 0L)
    cancelFocusAlarms(context)
    cancelFocusNotification(context)
    FocusBlockingOverlayManager.dismiss()

    CoroutineScope(Dispatchers.IO).launch {
      try {
        val db = LifeTrackerDatabase.getDatabase(context)
        val settings = db.userSettingsDao().getSettingsSync()
        if (settings != null) {
          db.userSettingsDao().insertOrUpdate(
            settings.copy(
              isFocusModeActive = false,
              focusSessionEndTimestamp = 0L
            )
          )
        }
      } catch (e: Exception) {
        Log.e(TAG, "Failed to persist deactivate focus to Room", e)
      }
    }
  }

  // =========================================================================
  // EXACT ALARM SCHEDULING (ALARM MANAGER)
  // =========================================================================

  fun scheduleFocusExactEndAlarm(context: Context, endTimestamp: Long) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }

    val intent = Intent(context, WakeUpAlarmReceiver::class.java).apply {
      action = ACTION_FOCUS_MODE_DEACTIVATE
    }
    val pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE_EXACT_END, intent, flags)

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTimestamp, pendingIntent)
      } else {
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, endTimestamp, pendingIntent)
      }
      Log.d(TAG, "Scheduled exact focus end alarm at $endTimestamp")
    } catch (e: SecurityException) {
      Log.w(TAG, "Exact alarm permission missing; using inexact fallback: ${e.message}")
      alarmManager.set(AlarmManager.RTC_WAKEUP, endTimestamp, pendingIntent)
    }
  }

  fun scheduleFocusAlarms(context: Context, startTimeMinutes: Int, endTimeMinutes: Int) {
    createNotificationChannel(context)
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }

    val activateIntent = Intent(context, WakeUpAlarmReceiver::class.java).apply {
      action = ACTION_FOCUS_MODE_ACTIVATE
    }
    val activatePending = PendingIntent.getBroadcast(context, REQUEST_CODE_ACTIVATE, activateIntent, flags)
    val activateMillis = calculateNextTriggerMillis(startTimeMinutes)

    val deactivateIntent = Intent(context, WakeUpAlarmReceiver::class.java).apply {
      action = ACTION_FOCUS_MODE_DEACTIVATE
    }
    val deactivatePending = PendingIntent.getBroadcast(context, REQUEST_CODE_DEACTIVATE, deactivateIntent, flags)
    val deactivateMillis = calculateNextTriggerMillis(endTimeMinutes)

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, activateMillis, activatePending)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, deactivateMillis, deactivatePending)
      } else {
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, activateMillis, activatePending)
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, deactivateMillis, deactivatePending)
      }
    } catch (e: SecurityException) {
      alarmManager.set(AlarmManager.RTC_WAKEUP, activateMillis, activatePending)
      alarmManager.set(AlarmManager.RTC_WAKEUP, deactivateMillis, deactivatePending)
    }
  }

  fun cancelFocusAlarms(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_NO_CREATE
    }

    val intentExact = Intent(context, WakeUpAlarmReceiver::class.java).apply {
      action = ACTION_FOCUS_MODE_DEACTIVATE
    }
    val pendingExact = PendingIntent.getBroadcast(context, REQUEST_CODE_EXACT_END, intentExact, flags)
    if (pendingExact != null) {
      alarmManager.cancel(pendingExact)
      pendingExact.cancel()
    }

    val intentActivate = Intent(context, WakeUpAlarmReceiver::class.java).apply {
      action = ACTION_FOCUS_MODE_ACTIVATE
    }
    val pendingActivate = PendingIntent.getBroadcast(context, REQUEST_CODE_ACTIVATE, intentActivate, flags)
    if (pendingActivate != null) {
      alarmManager.cancel(pendingActivate)
      pendingActivate.cancel()
    }

    val intentDeactivate = Intent(context, WakeUpAlarmReceiver::class.java).apply {
      action = ACTION_FOCUS_MODE_DEACTIVATE
    }
    val pendingDeactivate = PendingIntent.getBroadcast(context, REQUEST_CODE_DEACTIVATE, intentDeactivate, flags)
    if (pendingDeactivate != null) {
      alarmManager.cancel(pendingDeactivate)
      pendingDeactivate.cancel()
    }
  }

  // =========================================================================
  // BOOT & REBOOT RESTORATION
  // =========================================================================

  fun onBootOrScheduleChange(context: Context) {
    val now = System.currentTimeMillis()
    val isFocusActive = isFocusModeActive(context)
    val endTimestamp = getFocusSessionEndTimestamp(context)

    if (isFocusActive && endTimestamp > 0L) {
      if (now < endTimestamp) {
        // Active session survives reboot
        scheduleFocusExactEndAlarm(context, endTimestamp)
        val remainingMins = ((endTimestamp - now) / 60000L).toInt().coerceAtLeast(1)
        showFocusNotification(context, remainingMins, isScheduled = false)
        Log.d(TAG, "Restored active focus duration session after reboot: $remainingMins mins remaining")
      } else {
        // Expired while device was off
        deactivateFocusMode(context)
        Log.d(TAG, "Focus duration session expired during reboot/off; auto-deactivated")
      }
    } else {
      // Evaluate schedule
      CoroutineScope(Dispatchers.IO).launch {
        try {
          val db = LifeTrackerDatabase.getDatabase(context)
          val settings = db.userSettingsDao().getSettingsSync() ?: return@launch
          if (settings.isFocusScheduleEnabled) {
            val cal = Calendar.getInstance()
            val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val shouldBeActive = isCurrentTimeInFocusSchedule(
              nowMinutes,
              settings.focusStartTimeMinutes,
              settings.focusEndTimeMinutes
            )

            if (shouldBeActive) {
              val remaining = calculateRemainingMinutes(nowMinutes, settings.focusEndTimeMinutes)
              saveFocusState(context, active = true, endTimestamp = now + (remaining * 60000L))
              showFocusNotification(context, remaining, isScheduled = true)
            } else {
              saveFocusState(context, active = false, endTimestamp = 0L)
              cancelFocusNotification(context)
            }
            scheduleFocusAlarms(context, settings.focusStartTimeMinutes, settings.focusEndTimeMinutes)
          }
        } catch (e: Exception) {
          Log.e(TAG, "Failed to restore focus schedule after reboot", e)
        }
      }
    }
  }

  // =========================================================================
  // ONGOING NOTIFICATION
  // =========================================================================

  fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        ?: return
      val channel = NotificationChannel(
        FOCUS_NOTIFICATION_CHANNEL_ID,
        "Focus Mode & Restriction",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Shows active Focus Mode status and remaining time"
        setShowBadge(true)
        enableLights(true)
      }
      notificationManager.createNotificationChannel(channel)
    }
  }

  fun showFocusNotification(context: Context, remainingMinutes: Int = -1, isScheduled: Boolean = false) {
    createNotificationChannel(context)
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      ?: return

    val openAppIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }
    val pendingIntent = PendingIntent.getActivity(context, 8020, openAppIntent, flags)

    val remainingText = if (remainingMinutes > 0) {
      val hrs = remainingMinutes / 60
      val mins = remainingMinutes % 60
      if (hrs > 0) "$hrs घंटे $mins मिनट शेष" else "$mins मिनट शेष"
    } else {
      "डिजिटल अनुशासन सक्रिय"
    }

    val builder = NotificationCompat.Builder(context, FOCUS_NOTIFICATION_CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("🎯 Focus Mode सक्रिय • $remainingText")
      .setContentText("गहन अध्ययन व आत्म-नियंत्रण। अनधिकृत ऐप्स ब्लॉक हैं।")
      .setOngoing(true)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_STATUS)
      .setContentIntent(pendingIntent)
      .setAutoCancel(false)

    notificationManager.notify(FOCUS_NOTIFICATION_ID, builder.build())
  }

  fun cancelFocusNotification(context: Context) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      ?: return
    notificationManager.cancel(FOCUS_NOTIFICATION_ID)
  }

  // =========================================================================
  // UTILITY TIME CALCULATIONS
  // =========================================================================

  fun isCurrentTimeInFocusSchedule(nowMinutes: Int, startMinutes: Int, endMinutes: Int): Boolean {
    if (startMinutes == endMinutes) return false
    return if (startMinutes < endMinutes) {
      nowMinutes in startMinutes until endMinutes
    } else {
      nowMinutes >= startMinutes || nowMinutes < endMinutes
    }
  }

  fun calculateRemainingMinutes(nowMinutes: Int, endMinutes: Int): Int {
    return if (nowMinutes <= endMinutes) {
      endMinutes - nowMinutes
    } else {
      (1440 - nowMinutes) + endMinutes
    }
  }

  fun calculateNextTriggerMillis(targetMinutes: Int): Long {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, targetMinutes / 60)
      set(Calendar.MINUTE, targetMinutes % 60)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }

    if (target.timeInMillis <= now.timeInMillis) {
      target.add(Calendar.DAY_OF_YEAR, 1)
    }
    return target.timeInMillis
  }
}
