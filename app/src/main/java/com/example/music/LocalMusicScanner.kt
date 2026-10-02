package com.example.music

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.example.data.db.LifeTrackerDatabase
import com.example.data.model.MusicMood
import com.example.data.model.PlaylistEntity
import com.example.data.model.PlaylistRuleEntity
import com.example.data.model.PlaylistSongCrossRef
import com.example.data.model.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LocalMusicScanner {

  fun hasStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.READ_MEDIA_AUDIO
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
      ) == PackageManager.PERMISSION_GRANTED
    }
  }

  suspend fun scanDeviceAudio(context: Context): List<SongEntity> = withContext(Dispatchers.IO) {
    val songs = mutableListOf<SongEntity>()

    if (hasStoragePermission(context)) {
      try {
        val projection = arrayOf(
          MediaStore.Audio.Media._ID,
          MediaStore.Audio.Media.TITLE,
          MediaStore.Audio.Media.ARTIST,
          MediaStore.Audio.Media.ALBUM,
          MediaStore.Audio.Media.DURATION,
          MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 15000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
          MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
          projection,
          selection,
          null,
          sortOrder
        )?.use { cursor ->
          val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
          val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
          val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
          val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
          val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
          val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

          while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val title = cursor.getString(titleCol) ?: "Unknown Song"
            val artist = cursor.getString(artistCol) ?: "Unknown Artist"
            val album = cursor.getString(albumCol) ?: "Unknown Album"
            val duration = cursor.getLong(durationCol)
            val albumId = cursor.getLong(albumIdCol)

            val contentUri = ContentUris.withAppendedId(
              MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
              id
            ).toString()

            val albumArtUri = "content://media/external/audio/albumart/$albumId"

            // Heuristic default mood tag based on title keywords
            val lower = title.lowercase()
            val mood = when {
              lower.contains("sleep") || lower.contains("night") || lower.contains("lullaby") -> MusicMood.SLEEP.name
              lower.contains("workout") || lower.contains("gym") || lower.contains("run") || lower.contains("pump") -> MusicMood.ENERGETIC.name
              lower.contains("study") || lower.contains("focus") || lower.contains("deep") -> MusicMood.FOCUS.name
              lower.contains("happy") || lower.contains("joy") || lower.contains("celebrate") -> MusicMood.HAPPY.name
              lower.contains("bhajan") || lower.contains("aarti") || lower.contains("mantra") || lower.contains("devotional") -> MusicMood.DEVOTIONAL.name
              lower.contains("relax") || lower.contains("chill") || lower.contains("peace") || lower.contains("spa") -> MusicMood.RELAX.name
              else -> MusicMood.CALM.name
            }

            songs.add(
              SongEntity(
                id = "device_$id",
                title = title,
                artist = if (artist == "<unknown>") "Local Artist" else artist,
                album = if (album == "<unknown>") "Local Music" else album,
                durationMs = duration,
                contentUri = contentUri,
                albumArtUri = albumArtUri,
                isFavorite = false,
                isBuiltIn = false,
                mood = mood
              )
            )
          }
        }
      } catch (_: Exception) {}
    }

    // Always include built-in tranquil & ambient soundscapes so music is instantly playable
    val builtInSongs = getBuiltInSoundscapes()
    songs.addAll(builtInSongs)

    val db = LifeTrackerDatabase.getDatabase(context)
    val dao = db.musicDao()
    dao.insertSongs(songs)

    // Ensure default playlists and rules exist
    ensureDefaultPlaylistsAndRules(db, songs)

    songs
  }

  fun getBuiltInSoundscapes(): List<SongEntity> {
    return listOf(
      SongEntity(
        id = "builtin_morning_raga",
        title = "🌅 Morning Sun Serenade",
        artist = "Life Tracker Tranquil Sound",
        album = "Life Harmony Vol. 1",
        durationMs = 240000L, // 4 mins
        contentUri = "builtin://morning_raga",
        isBuiltIn = true,
        mood = MusicMood.CALM.name
      ),
      SongEntity(
        id = "builtin_432hz_focus",
        title = "🎯 432 Hz Alpha Flow (Binaural Focus)",
        artist = "Life Tracker Mind Labs",
        album = "Deep Work Waves",
        durationMs = 300000L, // 5 mins
        contentUri = "builtin://432hz_focus",
        isBuiltIn = true,
        mood = MusicMood.FOCUS.name
      ),
      SongEntity(
        id = "builtin_high_energy_pulse",
        title = "⚡ High Cadence Power Beats",
        artist = "Pulse Athletic Audio",
        album = "Workout Surge",
        durationMs = 210000L, // 3.5 mins
        contentUri = "builtin://high_energy",
        isBuiltIn = true,
        mood = MusicMood.ENERGETIC.name
      ),
      SongEntity(
        id = "builtin_zen_stream",
        title = "☕ Forest Stream & Bamboo Chimes",
        artist = "Nature Sanctuary",
        album = "Calm Horizons",
        durationMs = 270000L, // 4.5 mins
        contentUri = "builtin://zen_stream",
        isBuiltIn = true,
        mood = MusicMood.RELAX.name
      ),
      SongEntity(
        id = "builtin_delta_sleep",
        title = "🌙 Deep Delta Sleep Ocean",
        artist = "Night Rest Soundscapes",
        album = "Slumber Peace",
        durationMs = 360000L, // 6 mins
        contentUri = "builtin://delta_sleep",
        isBuiltIn = true,
        mood = MusicMood.SLEEP.name
      ),
      SongEntity(
        id = "builtin_sacred_temple",
        title = "🪔 Sacred Temple Harmony",
        artist = "Devotional Sound Lab",
        album = "Aura of Peace",
        durationMs = 250000L, // 4.1 mins
        contentUri = "builtin://sacred_temple",
        isBuiltIn = true,
        mood = MusicMood.DEVOTIONAL.name
      ),
      SongEntity(
        id = "builtin_cheerful_acoustic",
        title = "😊 Cheerful Morning Strings",
        artist = "Uplift Ensemble",
        album = "Bright Beginnings",
        durationMs = 220000L,
        contentUri = "builtin://cheerful_strings",
        isBuiltIn = true,
        mood = MusicMood.HAPPY.name
      )
    )
  }

  suspend fun ensureDefaultPlaylistsAndRules(db: LifeTrackerDatabase, allSongs: List<SongEntity>) {
    val musicDao = db.musicDao()

    val defaultPlaylists = listOf(
      Triple("Morning", "शांति और ऊर्जा से भरा सवेरा", "#00F0FF"),
      Triple("Workout", "हाई एनर्जी फिटनेस और रनिंग", "#F59E0B"),
      Triple("Study", "गहन एकाग्रता और पढ़ाई", "#8B5CF6"),
      Triple("Relax", "दिन का तनाव मुक्त करने वाला विश्राम", "#10B981"),
      Triple("Night", "गहरी नींद और सुकून भरी रात", "#6366F1"),
      Triple("Devotional", "आत्मिक शांति और भक्ति संगीत", "#F97316"),
      Triple("Travel", "यात्रा और शाम की सैर", "#EC4899")
    )

    for (p in defaultPlaylists) {
      val playlistId = musicDao.insertPlaylist(
        PlaylistEntity(
          name = p.first,
          description = p.second,
          colorHex = p.third
        )
      )

      // Add matching songs into this playlist
      val matchingSongs = allSongs.filter { song ->
        when (p.first) {
          "Morning" -> song.mood == MusicMood.CALM.name || song.mood == MusicMood.HAPPY.name
          "Workout" -> song.mood == MusicMood.ENERGETIC.name
          "Study" -> song.mood == MusicMood.FOCUS.name
          "Relax" -> song.mood == MusicMood.RELAX.name || song.mood == MusicMood.CALM.name
          "Night" -> song.mood == MusicMood.SLEEP.name
          "Devotional" -> song.mood == MusicMood.DEVOTIONAL.name
          "Travel" -> song.mood == MusicMood.ENERGETIC.name || song.mood == MusicMood.HAPPY.name
          else -> true
        }
      }

      matchingSongs.forEachIndexed { index, s ->
        musicDao.addSongToPlaylist(
          PlaylistSongCrossRef(
            playlistId = playlistId,
            songId = s.id,
            orderIndex = index
          )
        )
      }

      // Add default Time + Mood Rule for this playlist
      when (p.first) {
        "Morning" -> {
          musicDao.insertRule(
            PlaylistRuleEntity(
              playlistId = playlistId,
              startMinutes = 300, // 5:00 AM
              endMinutes = 360,   // 6:00 AM
              mood = MusicMood.CALM.name,
              situation = "Morning Routine (सुबह की शुरुआत)",
              linkedCategory = "Routine",
              autoPlay = true
            )
          )
          musicDao.insertRule(
            PlaylistRuleEntity(
              playlistId = playlistId,
              startMinutes = 360, // 6:00 AM
              endMinutes = 420,   // 7:00 AM
              mood = MusicMood.HAPPY.name,
              situation = "Day Warmup & Breakfast",
              linkedCategory = "Routine",
              autoPlay = false
            )
          )
        }
        "Workout" -> {
          musicDao.insertRule(
            PlaylistRuleEntity(
              playlistId = playlistId,
              startMinutes = 420, // 7:00 AM
              endMinutes = 480,   // 8:00 AM
              mood = MusicMood.ENERGETIC.name,
              situation = "Exercise / Workout Routine",
              linkedCategory = "Workout",
              autoPlay = true
            )
          )
        }
        "Study" -> {
          musicDao.insertRule(
            PlaylistRuleEntity(
              playlistId = playlistId,
              startMinutes = 540,  // 9:00 AM
              endMinutes = 780,  // 1:00 PM
              mood = MusicMood.FOCUS.name,
              situation = "Deep Work / Study Sessions",
              linkedCategory = "Study",
              autoPlay = false
            )
          )
        }
        "Relax" -> {
          musicDao.insertRule(
            PlaylistRuleEntity(
              playlistId = playlistId,
              startMinutes = 780,  // 1:00 PM
              endMinutes = 840,  // 2:00 PM
              mood = MusicMood.RELAX.name,
              situation = "Midday Rest & Lunch Break",
              linkedCategory = "Break",
              autoPlay = false
            )
          )
        }
        "Night" -> {
          musicDao.insertRule(
            PlaylistRuleEntity(
              playlistId = playlistId,
              startMinutes = 1260, // 9:00 PM
              endMinutes = 1380, // 11:00 PM
              mood = MusicMood.SLEEP.name,
              situation = "Night Routine & Wind Down",
              linkedCategory = "Sleep",
              autoPlay = true
            )
          )
        }
      }
    }
  }
}
