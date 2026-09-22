package com.example.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.R
import com.example.data.db.LifeTrackerDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

object AlarmAudioPlayer {
  private var mediaPlayer: MediaPlayer? = null
  private var vibrator: Vibrator? = null
  @Volatile private var isPlayingState = false

  fun isPlaying(): Boolean = isPlayingState

  fun startWithSavedSettings(context: Context) {
    CoroutineScope(Dispatchers.IO).launch {
      try {
        val db = LifeTrackerDatabase.getDatabase(context)
        val settings = db.userSettingsDao().getSettingsSync()
        val soundType = settings?.alarmSoundType ?: "DEFAULT"
        val customUri = settings?.customSoundUri
        val volume = settings?.alarmVolume ?: 1.0f
        val vibration = settings?.isVibrationEnabled ?: true
        start(context, soundType, customUri, volume, vibration)
      } catch (e: Exception) {
        Log.e("AlarmAudioPlayer", "Error loading alarm settings for playback", e)
        start(context, "DEFAULT", null, 1.0f, true)
      }
    }
  }

  @Synchronized
  fun start(
    context: Context,
    soundType: String = "DEFAULT",
    customUriString: String? = null,
    volume: Float = 1.0f,
    enableVibration: Boolean = true
  ) {
    // 1. Cleanly stop any existing playback before starting
    stop()
    isPlayingState = true

    // 2. Configure and start audio player
    try {
      val player = MediaPlayer()
      var dataSourceSet = false

      // Try custom sound first if selected
      if (soundType.equals("CUSTOM", ignoreCase = true) && !customUriString.isNullOrBlank()) {
        try {
          val customFile = File(customUriString)
          if (customFile.exists()) {
            player.setDataSource(customFile.absolutePath)
            dataSourceSet = true
          } else {
            val uri = Uri.parse(customUriString)
            player.setDataSource(context.applicationContext, uri)
            dataSourceSet = true
          }
        } catch (e: Exception) {
          Log.w("AlarmAudioPlayer", "Could not load custom sound ($customUriString), falling back to high-tone default", e)
          player.reset()
          dataSourceSet = false
        }
      }

      // Built-in High-Tone Sharp Alarm Sound
      if (!dataSourceSet) {
        try {
          val afd = context.resources.openRawResourceFd(R.raw.high_tone_alarm)
          if (afd != null) {
            player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            dataSourceSet = true
          }
        } catch (e: Exception) {
          Log.w("AlarmAudioPlayer", "Failed loading high_tone_alarm raw resource, falling back to system ringtone", e)
          player.reset()
          dataSourceSet = false
        }
      }

      // System fallback if both custom and raw failed
      if (!dataSourceSet) {
        val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
          ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
          ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        if (alertUri != null) {
          player.setDataSource(context.applicationContext, alertUri)
          dataSourceSet = true
        }
      }

      if (dataSourceSet) {
        player.setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        )
        val clampedVol = volume.coerceIn(0.05f, 1.0f)
        player.setVolume(clampedVol, clampedVol)
        player.isLooping = true
        player.prepare()
        player.start()
        mediaPlayer = player
      }
    } catch (e: Exception) {
      Log.e("AlarmAudioPlayer", "Error initializing alarm audio player", e)
    }

    // 3. Configure vibration if enabled
    if (enableVibration) {
      try {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
          vibratorManager?.defaultVibrator
        } else {
          @Suppress("DEPRECATION")
          context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        val pattern = longArrayOf(0, 800, 400, 800, 400)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
          @Suppress("DEPRECATION")
          vibrator?.vibrate(pattern, 0)
        }
      } catch (e: Exception) {
        Log.e("AlarmAudioPlayer", "Error starting vibration", e)
      }
    }
  }

  @Synchronized
  fun setVolume(volume: Float) {
    try {
      val clamped = volume.coerceIn(0.05f, 1.0f)
      mediaPlayer?.setVolume(clamped, clamped)
    } catch (e: Exception) {
      Log.e("AlarmAudioPlayer", "Error updating volume", e)
    }
  }

  @Synchronized
  fun stop() {
    isPlayingState = false
    try {
      mediaPlayer?.let {
        if (it.isPlaying) {
          it.stop()
        }
        it.release()
      }
    } catch (e: Exception) {
      Log.e("AlarmAudioPlayer", "Error releasing MediaPlayer", e)
    } finally {
      mediaPlayer = null
    }

    try {
      vibrator?.cancel()
    } catch (e: Exception) {
      Log.e("AlarmAudioPlayer", "Error cancelling vibration", e)
    } finally {
      vibrator = null
    }
  }
}
