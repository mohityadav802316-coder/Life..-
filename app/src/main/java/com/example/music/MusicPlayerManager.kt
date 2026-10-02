package com.example.music

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.example.data.db.LifeTrackerDatabase
import com.example.data.model.PlaylistEntity
import com.example.data.model.SongEntity
import com.example.data.model.MusicMood
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

enum class RepeatMode {
  OFF, ALL, ONE
}

data class MusicPlayerState(
  val currentSong: SongEntity? = null,
  val isPlaying: Boolean = false,
  val currentPositionMs: Long = 0L,
  val durationMs: Long = 0L,
  val queue: List<SongEntity> = emptyList(),
  val currentIndex: Int = 0,
  val isShuffle: Boolean = false,
  val repeatMode: RepeatMode = RepeatMode.ALL,
  val currentPlaylist: PlaylistEntity? = null,
  val currentSituation: String? = null,
  val sleepTimerMinutesRemaining: Int = 0,
  val volume: Float = 1.0f
) {
  val currentMood: MusicMood
    get() = currentSong?.let { MusicMood.fromString(it.mood) } ?: MusicMood.CALM

  val progressFraction: Float
    get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}

class MusicPlayerManager private constructor(private val appContext: Context) {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  private val _playerState = MutableStateFlow(MusicPlayerState())
  val playerState: StateFlow<MusicPlayerState> = _playerState.asStateFlow()

  private var mediaPlayer: MediaPlayer? = null
  private var audioTrackSynthesizer: AudioTrack? = null
  private var synthJob: Job? = null
  private var progressTrackerJob: Job? = null
  private var sleepTimerJob: Job? = null

  private val db = LifeTrackerDatabase.getDatabase(appContext)

  fun playQueue(songs: List<SongEntity>, startIndex: Int = 0, playlist: PlaylistEntity? = null, situation: String? = null) {
    if (songs.isEmpty()) return
    val safeIndex = startIndex.coerceIn(0, songs.size - 1)

    _playerState.update {
      it.copy(
        queue = songs,
        currentIndex = safeIndex,
        currentPlaylist = playlist,
        currentSituation = situation
      )
    }

    playSong(songs[safeIndex])
  }

  fun playSong(song: SongEntity) {
    stopCurrentPlayback()

    _playerState.update {
      it.copy(
        currentSong = song,
        isPlaying = true,
        currentPositionMs = 0L,
        durationMs = song.durationMs
      )
    }

    // Record in history & bump playCount
    scope.launch(Dispatchers.IO) {
      db.musicDao().recordSongPlayed(song.id, System.currentTimeMillis())
    }

    if (song.isBuiltIn) {
      playBuiltInTrack(song)
    } else {
      playDeviceAudio(song)
    }

    startProgressTracker()
    startForegroundService()
  }

  fun togglePlayPause() {
    val state = _playerState.value
    if (state.currentSong == null) {
      // If nothing loaded, try to play first from queue or library
      scope.launch(Dispatchers.IO) {
        val allSongs = LocalMusicScanner.scanDeviceAudio(appContext)
        if (allSongs.isNotEmpty()) {
          launch(Dispatchers.Main) { playQueue(allSongs, 0) }
        }
      }
      return
    }

    if (state.isPlaying) {
      pause()
    } else {
      resume()
    }
  }

  fun pause() {
    _playerState.update { it.copy(isPlaying = false) }
    try {
      mediaPlayer?.pause()
    } catch (_: Exception) {}
    try {
      audioTrackSynthesizer?.pause()
    } catch (_: Exception) {}
    updateForegroundNotification()
  }

  fun resume() {
    _playerState.update { it.copy(isPlaying = true) }
    try {
      mediaPlayer?.start()
    } catch (_: Exception) {}
    try {
      audioTrackSynthesizer?.play()
    } catch (_: Exception) {}
    startProgressTracker()
    updateForegroundNotification()
  }

  fun next() {
    val state = _playerState.value
    if (state.queue.isEmpty()) return

    val nextIndex = if (state.isShuffle) {
      (0 until state.queue.size).random()
    } else {
      (state.currentIndex + 1) % state.queue.size
    }

    _playerState.update { it.copy(currentIndex = nextIndex) }
    playSong(state.queue[nextIndex])
  }

  fun previous() {
    val state = _playerState.value
    if (state.queue.isEmpty()) return

    val prevIndex = if (state.currentPositionMs > 3000L) {
      state.currentIndex // Restart current song if played > 3s
    } else {
      if (state.currentIndex - 1 < 0) state.queue.size - 1 else state.currentIndex - 1
    }

    _playerState.update { it.copy(currentIndex = prevIndex) }
    playSong(state.queue[prevIndex])
  }

  fun seekTo(positionMs: Long) {
    _playerState.update { it.copy(currentPositionMs = positionMs) }
    try {
      mediaPlayer?.seekTo(positionMs.toInt())
    } catch (_: Exception) {}
  }

  fun toggleShuffle() {
    _playerState.update { it.copy(isShuffle = !it.isShuffle) }
  }

  fun toggleRepeatMode() {
    _playerState.update {
      val next = when (it.repeatMode) {
        RepeatMode.OFF -> RepeatMode.ALL
        RepeatMode.ALL -> RepeatMode.ONE
        RepeatMode.ONE -> RepeatMode.OFF
      }
      it.copy(repeatMode = next)
    }
  }

  fun toggleFavorite(song: SongEntity) {
    scope.launch(Dispatchers.IO) {
      val newFav = !song.isFavorite
      db.musicDao().updateFavoriteStatus(song.id, newFav)
      _playerState.update { state ->
        if (state.currentSong?.id == song.id) {
          state.copy(currentSong = state.currentSong.copy(isFavorite = newFav))
        } else state
      }
    }
  }

  fun setSleepTimer(minutes: Int) {
    sleepTimerJob?.cancel()
    _playerState.update { it.copy(sleepTimerMinutesRemaining = minutes) }

    if (minutes > 0) {
      sleepTimerJob = scope.launch {
        var remaining = minutes
        while (remaining > 0 && isActive) {
          delay(60000L)
          remaining--
          _playerState.update { it.copy(sleepTimerMinutesRemaining = remaining) }
        }
        pause()
        _playerState.update { it.copy(sleepTimerMinutesRemaining = 0) }
      }
    }
  }

  /**
   * Smart Rule Playback:
   * Selects playlist & songs according to the user's defined Time + Mood rules
   */
  fun playByCurrentTimeRule(currentMinutes: Int) {
    scope.launch(Dispatchers.IO) {
      val rules = db.musicDao().getRulesMatchingTime(currentMinutes)
      val rule = rules.firstOrNull() ?: return@launch
      val playlist = db.musicDao().getPlaylistById(rule.playlistId)
      val songs = db.musicDao().getSongsForPlaylistSync(rule.playlistId)

      if (songs.isNotEmpty()) {
        val moodFiltered = songs.filter { it.mood.equals(rule.mood, ignoreCase = true) }
        val playlistSongs = if (moodFiltered.isNotEmpty()) moodFiltered else songs

        launch(Dispatchers.Main) {
          playQueue(
            songs = playlistSongs,
            startIndex = 0,
            playlist = playlist,
            situation = rule.situation
          )
        }
      }
    }
  }

  private fun playDeviceAudio(song: SongEntity) {
    try {
      mediaPlayer = MediaPlayer().apply {
        setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        )
        setDataSource(appContext, Uri.parse(song.contentUri))
        prepareAsync()
        setOnPreparedListener { mp ->
          val dur = mp.duration.toLong()
          _playerState.update { it.copy(durationMs = if (dur > 0) dur else song.durationMs) }
          mp.start()
        }
        setOnCompletionListener {
          handleSongCompletion()
        }
        setOnErrorListener { _, _, _ ->
          // Fallback to built-in peaceful soundscape if file cannot be read
          playBuiltInTrack(song)
          true
        }
      }
    } catch (_: Exception) {
      playBuiltInTrack(song)
    }
  }

  private fun playBuiltInTrack(song: SongEntity) {
    synthJob?.cancel()
    synthJob = scope.launch(Dispatchers.Default) {
      val sampleRate = 44100
      val baseFreq = when (song.mood) {
        "ENERGETIC" -> 440.0
        "FOCUS" -> 432.0
        "HAPPY" -> 528.0
        "SLEEP" -> 216.0
        "DEVOTIONAL" -> 288.0
        else -> 396.0 // CALM
      }

      val bufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT
      )

      val track = AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        )
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        )
        .setBufferSizeInBytes(bufferSize * 4)
        .setTransferMode(AudioTrack.MODE_STREAM)
        .build()

      audioTrackSynthesizer = track
      track.play()

      val samples = ShortArray(bufferSize)
      var phase = 0.0
      val twoPi = 2 * PI

      while (isActive && _playerState.value.isPlaying) {
        for (i in samples.indices) {
          // Harmonic wave with soothing envelope & modulation
          val wave = (
            0.65 * sin(phase) +
            0.25 * sin(phase * 2.0) +
            0.10 * sin(phase * 3.0)
          )
          samples[i] = (wave * Short.MAX_VALUE * 0.45).toInt().toShort()

          phase += twoPi * baseFreq / sampleRate
          if (phase > twoPi) phase -= twoPi
        }
        track.write(samples, 0, samples.size)
      }
    }
  }

  private fun handleSongCompletion() {
    when (_playerState.value.repeatMode) {
      RepeatMode.ONE -> {
        seekTo(0)
        resume()
      }
      RepeatMode.ALL -> next()
      RepeatMode.OFF -> {
        if (_playerState.value.currentIndex < _playerState.value.queue.size - 1) {
          next()
        } else {
          pause()
          seekTo(0)
        }
      }
    }
  }

  private fun startProgressTracker() {
    progressTrackerJob?.cancel()
    progressTrackerJob = scope.launch {
      while (isActive) {
        delay(500L)
        if (_playerState.value.isPlaying) {
          val pos = try {
            mediaPlayer?.currentPosition?.toLong() ?: (_playerState.value.currentPositionMs + 500L)
          } catch (_: Exception) {
            _playerState.value.currentPositionMs + 500L
          }
          val totalDur = _playerState.value.durationMs
          if (totalDur > 0 && pos >= totalDur && _playerState.value.currentSong?.isBuiltIn == true) {
            handleSongCompletion()
          } else {
            _playerState.update { it.copy(currentPositionMs = pos) }
          }
        }
      }
    }
  }

  private fun stopCurrentPlayback() {
    try {
      mediaPlayer?.stop()
      mediaPlayer?.release()
    } catch (_: Exception) {}
    mediaPlayer = null

    try {
      audioTrackSynthesizer?.stop()
      audioTrackSynthesizer?.release()
    } catch (_: Exception) {}
    audioTrackSynthesizer = null
    synthJob?.cancel()
  }

  fun stop() {
    stopCurrentPlayback()
    progressTrackerJob?.cancel()
    sleepTimerJob?.cancel()
    _playerState.update { it.copy(isPlaying = false, currentPositionMs = 0L) }
    stopForegroundService()
  }

  private fun startForegroundService() {
    try {
      val intent = Intent(appContext, MusicPlaybackService::class.java).apply {
        action = MusicPlaybackService.ACTION_START
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        appContext.startForegroundService(intent)
      } else {
        appContext.startService(intent)
      }
    } catch (_: Exception) {}
  }

  private fun updateForegroundNotification() {
    try {
      val intent = Intent(appContext, MusicPlaybackService::class.java).apply {
        action = MusicPlaybackService.ACTION_UPDATE
      }
      appContext.startService(intent)
    } catch (_: Exception) {}
  }

  private fun stopForegroundService() {
    try {
      val intent = Intent(appContext, MusicPlaybackService::class.java).apply {
        action = MusicPlaybackService.ACTION_STOP
      }
      appContext.startService(intent)
    } catch (_: Exception) {}
  }

  companion object {
    @Volatile
    private var instance: MusicPlayerManager? = null

    fun getInstance(context: Context): MusicPlayerManager {
      return instance ?: synchronized(this) {
        instance ?: MusicPlayerManager(context.applicationContext).also { instance = it }
      }
    }
  }
}
