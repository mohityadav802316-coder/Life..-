package com.example.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.db.LifeTrackerDatabase
import com.example.util.TimeUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class WakeUpAlarmReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent?) {
    val action = intent?.action ?: return
    Log.d("WakeUpAlarmReceiver", "Received broadcast action: $action")

    when (action) {
      AlarmScheduler.ACTION_TRIGGER_ALARM -> {
        handleAlarmTrigger(context, intent)
      }
      AlarmScheduler.ACTION_STOP_ALARM -> {
        AlarmAudioPlayer.stop()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(AlarmScheduler.ALARM_NOTIFICATION_ID)
      }
      AlarmScheduler.ACTION_SNOOZE_ALARM -> {
        AlarmAudioPlayer.stop()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(AlarmScheduler.ALARM_NOTIFICATION_ID)
        CoroutineScope(Dispatchers.IO).launch {
          val db = LifeTrackerDatabase.getDatabase(context)
          val settings = db.userSettingsDao().getSettingsSync()
          val snoozeMins = settings?.snoozeMinutes ?: 10
          AlarmScheduler.scheduleSnooze(context, snoozeMins)
        }
      }
      Intent.ACTION_BOOT_COMPLETED -> {
        handleBootCompleted(context)
      }
    }
  }

  private fun handleAlarmTrigger(context: Context, intent: Intent) {
    // 1. Start audio and vibration with user saved settings (custom/high-tone, volume, vibration)
    AlarmAudioPlayer.startWithSavedSettings(context)

    // 2. Format 12-hour AM/PM time
    val cal = Calendar.getInstance()
    val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    val timeFormatted = TimeUtils.minutesTo12Hour(currentMinutes)

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

    // 3. Create High-Priority Alarm Notification Channel
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val existingChannel = notificationManager.getNotificationChannel(AlarmScheduler.ALARM_CHANNEL_ID)
      if (existingChannel == null) {
        val channel = NotificationChannel(
          AlarmScheduler.ALARM_CHANNEL_ID,
          "Wake-Up Alarm",
          NotificationManager.IMPORTANCE_HIGH
        ).apply {
          description = "Recurring wake-up alarms for Life Tracker"
          enableVibration(true)
          vibrationPattern = longArrayOf(0, 800, 400, 800, 400)
          val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ALARM)
            .build()
          val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
          setSound(alarmSound, audioAttributes)
          setBypassDnd(true)
        }
        notificationManager.createNotificationChannel(channel)
      }
    }

    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }

    // Full screen intent to AlarmActivity
    val fullScreenIntent = Intent(context, AlarmActivity::class.java).apply {
      this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val fullScreenPendingIntent = PendingIntent.getActivity(context, 201, fullScreenIntent, flags)

    // Stop Action
    val stopIntent = Intent(context, WakeUpAlarmReceiver::class.java).apply {
      this.action = AlarmScheduler.ACTION_STOP_ALARM
    }
    val stopPendingIntent = PendingIntent.getBroadcast(context, 202, stopIntent, flags)

    // Snooze Action
    val snoozeIntent = Intent(context, WakeUpAlarmReceiver::class.java).apply {
      this.action = AlarmScheduler.ACTION_SNOOZE_ALARM
    }
    val snoozePendingIntent = PendingIntent.getBroadcast(context, 203, snoozeIntent, flags)

    val notification = NotificationCompat.Builder(context, AlarmScheduler.ALARM_CHANNEL_ID)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle("⏰ Wake-Up Alarm • $timeFormatted")
      .setContentText("Rise and execute your scheduled daily routine!")
      .setPriority(NotificationCompat.PRIORITY_MAX)
      .setCategory(NotificationCompat.CATEGORY_ALARM)
      .setAutoCancel(false)
      .setOngoing(true)
      .setContentIntent(fullScreenPendingIntent)
      .setFullScreenIntent(fullScreenPendingIntent, true)
      .addAction(android.R.drawable.ic_lock_power_off, "Stop", stopPendingIntent)
      .addAction(android.R.drawable.ic_popup_sync, "Snooze", snoozePendingIntent)
      .build()

    notificationManager.notify(AlarmScheduler.ALARM_NOTIFICATION_ID, notification)

    // 4. Automatically reschedule for the next cycle occurrence
    val wakeUpMinutes = intent.getIntExtra(AlarmScheduler.EXTRA_WAKE_UP_MINUTES, currentMinutes)
    CoroutineScope(Dispatchers.IO).launch {
      val db = LifeTrackerDatabase.getDatabase(context)
      val settings = db.userSettingsDao().getSettingsSync()
      val repeatMode = settings?.alarmRepeatMode ?: "DAILY"
      val customDaysMask = settings?.alarmCustomDaysMask ?: 127
      AlarmScheduler.scheduleWakeUpAlarm(context, wakeUpMinutes, repeatMode, customDaysMask)
    }
  }

  private fun handleBootCompleted(context: Context) {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val db = LifeTrackerDatabase.getDatabase(context)
        val settings = db.userSettingsDao().getSettingsSync()
        if (settings != null && settings.isAlarmEnabled) {
          Log.d("WakeUpAlarmReceiver", "Rescheduling wake-up alarm after boot for ${settings.wakeUpMinutes} mins")
          AlarmScheduler.scheduleWakeUpAlarm(
            context,
            settings.wakeUpMinutes,
            settings.alarmRepeatMode,
            settings.alarmCustomDaysMask
          )
        }
      } catch (e: Exception) {
        Log.e("WakeUpAlarmReceiver", "Failed to reschedule alarm after boot", e)
      }
    }
  }
}
