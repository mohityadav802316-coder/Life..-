package com.example.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.MainActivity
import java.util.Calendar

object AlarmScheduler {
  const val ACTION_TRIGGER_ALARM = "com.mohit.lifetracker.ACTION_TRIGGER_ALARM"
  const val ACTION_STOP_ALARM = "com.mohit.lifetracker.ACTION_STOP_ALARM"
  const val ACTION_SNOOZE_ALARM = "com.mohit.lifetracker.ACTION_SNOOZE_ALARM"
  const val EXTRA_WAKE_UP_MINUTES = "extra_wake_up_minutes"

  const val ALARM_CHANNEL_ID = "life_tracker_wake_up_channel"
  const val ALARM_NOTIFICATION_ID = 4001
  private const val REQUEST_CODE_ALARM = 101
  private const val REQUEST_CODE_SNOOZE = 102

  fun calculateNextAlarmMillis(
    wakeUpMinutes: Int,
    repeatMode: String = "DAILY",
    customDaysMask: Int = 127
  ): Long {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, wakeUpMinutes / 60)
      set(Calendar.MINUTE, wakeUpMinutes % 60)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }

    if (target.timeInMillis <= now.timeInMillis) {
      target.add(Calendar.DAY_OF_YEAR, 1)
    }

    // Check repeat condition for up to 7 consecutive days
    for (i in 0..7) {
      val dayOfWeek = target.get(Calendar.DAY_OF_WEEK) // 1=Sun, 2=Mon, 3=Tue, 4=Wed, 5=Thu, 6=Fri, 7=Sat
      val matches = when (repeatMode.uppercase()) {
        "WEEKDAYS" -> dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY
        "CUSTOM" -> {
          // Bitmask: Mon=0, Tue=1, Wed=2, Thu=3, Fri=4, Sat=5, Sun=6
          val bitIndex = when (dayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
          }
          (customDaysMask and (1 shl bitIndex)) != 0
        }
        else -> true // DAILY
      }
      if (matches) break
      target.add(Calendar.DAY_OF_YEAR, 1)
    }

    return target.timeInMillis
  }

  fun scheduleWakeUpAlarm(
    context: Context,
    wakeUpMinutes: Int,
    repeatMode: String = "DAILY",
    customDaysMask: Int = 127
  ) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

    val triggerMillis = calculateNextAlarmMillis(wakeUpMinutes, repeatMode, customDaysMask)

    val intent = Intent(context, WakeUpAlarmReceiver::class.java).apply {
      action = ACTION_TRIGGER_ALARM
      putExtra(EXTRA_WAKE_UP_MINUTES, wakeUpMinutes)
    }

    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }

    val pendingIntent = PendingIntent.getBroadcast(
      context,
      REQUEST_CODE_ALARM,
      intent,
      flags
    )

    val showIntent = Intent(context, MainActivity::class.java)
    val showPendingIntent = PendingIntent.getActivity(
      context,
      0,
      showIntent,
      flags
    )

    try {
      val clockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent)
      alarmManager.setAlarmClock(clockInfo, pendingIntent)
      Log.d("AlarmScheduler", "Alarm scheduled for epoch: $triggerMillis (${java.util.Date(triggerMillis)})")
    } catch (e: SecurityException) {
      Log.e("AlarmScheduler", "Exact alarm permission missing, falling back to setAndAllowWhileIdle", e)
      alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
    }
  }

  fun scheduleSnooze(context: Context, snoozeMinutes: Int = 10) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val triggerMillis = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)

    val intent = Intent(context, WakeUpAlarmReceiver::class.java).apply {
      action = ACTION_TRIGGER_ALARM
    }

    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }

    val pendingIntent = PendingIntent.getBroadcast(
      context,
      REQUEST_CODE_SNOOZE,
      intent,
      flags
    )

    val showIntent = Intent(context, MainActivity::class.java)
    val showPendingIntent = PendingIntent.getActivity(
      context,
      0,
      showIntent,
      flags
    )

    try {
      val clockInfo = AlarmManager.AlarmClockInfo(triggerMillis, showPendingIntent)
      alarmManager.setAlarmClock(clockInfo, pendingIntent)
    } catch (e: Exception) {
      alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
    }
  }

  fun cancelAlarm(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

    val intent = Intent(context, WakeUpAlarmReceiver::class.java).apply {
      action = ACTION_TRIGGER_ALARM
    }
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }

    val pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE_ALARM, intent, flags)
    alarmManager.cancel(pendingIntent)

    val snoozeIntent = PendingIntent.getBroadcast(context, REQUEST_CODE_SNOOZE, intent, flags)
    alarmManager.cancel(snoozeIntent)

    AlarmAudioPlayer.stop()
  }
}
