package com.example.music

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.util.TimeUtils

class MusicPlaybackService : Service() {

  private var wakeLock: PowerManager.WakeLock? = null
  private val channelId = "music_playback_channel"
  private val notificationId = 9977

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    createNotificationChannel()
    acquireWakeLock()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    val action = intent?.action ?: ACTION_START
    val manager = MusicPlayerManager.getInstance(applicationContext)

    when (action) {
      ACTION_START -> {
        try {
          val notification = buildNotification(manager.playerState.value)
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
              notificationId,
              notification,
              ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
          } else {
            startForeground(notificationId, notification)
          }
        } catch (_: Exception) {}
      }
      ACTION_TOGGLE_PLAY -> {
        manager.togglePlayPause()
        updateNotification(manager.playerState.value)
      }
      ACTION_NEXT -> {
        manager.next()
        updateNotification(manager.playerState.value)
      }
      ACTION_PREV -> {
        manager.previous()
        updateNotification(manager.playerState.value)
      }
      ACTION_STOP -> {
        stopForeground(true)
        stopSelf()
        return START_NOT_STICKY
      }
      ACTION_UPDATE -> {
        updateNotification(manager.playerState.value)
      }
    }

    return START_STICKY
  }

  private fun updateNotification(state: MusicPlayerState) {
    if (state.currentSong == null) {
      stopForeground(true)
      stopSelf()
      return
    }
    val notification = buildNotification(state)
    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
    nm?.notify(notificationId, notification)
  }

  private fun buildNotification(state: MusicPlayerState): Notification {
    val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    } else {
      PendingIntent.FLAG_UPDATE_CURRENT
    }

    val openIntent = Intent(this, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
      putExtra(EXTRA_OPEN_MUSIC, true)
    }
    val contentPendingIntent = PendingIntent.getActivity(this, 201, openIntent, pendingIntentFlags)

    // Action: Previous
    val prevIntent = Intent(this, MusicPlaybackService::class.java).apply {
      action = ACTION_PREV
    }
    val prevPendingIntent = PendingIntent.getService(this, 202, prevIntent, pendingIntentFlags)

    // Action: Play/Pause Toggle
    val toggleIntent = Intent(this, MusicPlaybackService::class.java).apply {
      action = ACTION_TOGGLE_PLAY
    }
    val togglePendingIntent = PendingIntent.getService(this, 203, toggleIntent, pendingIntentFlags)
    val toggleTitle = if (state.isPlaying) "Pause" else "Play"
    val toggleIcon = if (state.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play

    // Action: Next
    val nextIntent = Intent(this, MusicPlaybackService::class.java).apply {
      action = ACTION_NEXT
    }
    val nextPendingIntent = PendingIntent.getService(this, 204, nextIntent, pendingIntentFlags)

    val song = state.currentSong
    val songTitle = song?.title ?: "Life Tracker Music"
    val songArtist = song?.artist ?: "Tranquil Flow"
    val situation = state.currentSituation ?: state.currentPlaylist?.name ?: "Routine Music"

    return NotificationCompat.Builder(this, channelId)
      .setSmallIcon(R.drawable.ic_meditation_notif)
      .setContentTitle(songTitle)
      .setContentText("$songArtist • $situation")
      .setContentIntent(contentPendingIntent)
      .setOngoing(state.isPlaying)
      .setOnlyAlertOnce(true)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .addAction(android.R.drawable.ic_media_previous, "Prev", prevPendingIntent)
      .addAction(toggleIcon, toggleTitle, togglePendingIntent)
      .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)
      .build()
  }

  private fun acquireWakeLock() {
    try {
      if (wakeLock == null) {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(
          PowerManager.PARTIAL_WAKE_LOCK,
          "LifeTracker:MusicWakeLock"
        )?.apply {
          setReferenceCounted(false)
          acquire(120 * 60 * 1000L) // 2 hour max safety timeout
        }
      }
    } catch (_: Exception) {}
  }

  private fun releaseWakeLock() {
    try {
      if (wakeLock?.isHeld == true) {
        wakeLock?.release()
      }
      wakeLock = null
    } catch (_: Exception) {}
  }

  private fun createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        channelId,
        "Music Playback Controls",
        NotificationManager.IMPORTANCE_LOW
      ).apply {
        description = "Provides background and lockscreen controls for Life Tracker Music"
        setShowBadge(false)
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
      }
      val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      nm?.createNotificationChannel(channel)
    }
  }

  override fun onDestroy() {
    releaseWakeLock()
    super.onDestroy()
  }

  companion object {
    const val ACTION_START = "com.example.music.ACTION_START"
    const val ACTION_TOGGLE_PLAY = "com.example.music.ACTION_TOGGLE_PLAY"
    const val ACTION_NEXT = "com.example.music.ACTION_NEXT"
    const val ACTION_PREV = "com.example.music.ACTION_PREV"
    const val ACTION_STOP = "com.example.music.ACTION_STOP"
    const val ACTION_UPDATE = "com.example.music.ACTION_UPDATE"
    const val EXTRA_OPEN_MUSIC = "extra_open_music"
  }
}
