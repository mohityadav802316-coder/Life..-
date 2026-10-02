package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MusicMood
import com.example.data.model.PlaylistEntity
import com.example.data.model.PlaylistRuleEntity
import com.example.data.model.SongEntity
import com.example.music.LocalMusicScanner
import com.example.music.RepeatMode
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.GlassGradient
import com.example.ui.theme.StatusComplete
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletNeon
import com.example.ui.viewmodel.LifeTrackerViewModel
import com.example.util.TimeUtils

enum class MusicSubTab(val title: String) {
  NOW_PLAYING("Now Playing"),
  PLAYLISTS("Playlists"),
  ALL_SONGS("All Songs"),
  MOOD_RULES("Time & Mood"),
  FAVORITES("Favorites")
}

@Composable
fun MusicScreen(
  viewModel: LifeTrackerViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val playerState by viewModel.musicPlayerState.collectAsState()
  val allSongs by viewModel.allSongs.collectAsState()
  val allPlaylists by viewModel.allPlaylists.collectAsState()
  val playlistRules by viewModel.playlistRules.collectAsState()
  val favoriteSongs by viewModel.favoriteSongs.collectAsState()
  val recentlyPlayed by viewModel.recentlyPlayedSongs.collectAsState()

  var selectedSubTab by remember { mutableStateOf(MusicSubTab.NOW_PLAYING) }
  var showCreatePlaylistDialog by remember { mutableStateOf(false) }
  var showAddRuleDialog by remember { mutableStateOf(false) }
  var selectedPlaylistForDetail by remember { mutableStateOf<PlaylistEntity?>(null) }
  var songToAddFromPicker by remember { mutableStateOf<SongEntity?>(null) }

  val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { granted ->
    if (granted) {
      viewModel.scanDeviceMusic()
      Toast.makeText(context, "Scanning local songs...", Toast.LENGTH_SHORT).show()
    } else {
      Toast.makeText(context, "Storage permission denied. Using built-in tranquil audio.", Toast.LENGTH_LONG).show()
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBackground)
      .statusBarsPadding()
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // 1. Top Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(
            onClick = {
              if (selectedPlaylistForDetail != null) {
                selectedPlaylistForDetail = null
              } else {
                viewModel.navigateBack()
              }
            },
            modifier = Modifier.size(38.dp)
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = TextPrimary
            )
          }
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "PERSONAL MUSIC MANAGER • संगीत",
              color = CyanNeon,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            )
            Text(
              text = selectedPlaylistForDetail?.name ?: "Life Tracker Music",
              color = TextPrimary,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          // Scan device audio button
          IconButton(
            onClick = {
              val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
              } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
              }
              if (LocalMusicScanner.hasStoragePermission(context)) {
                viewModel.scanDeviceMusic()
                Toast.makeText(context, "Scanning local audio files...", Toast.LENGTH_SHORT).show()
              } else {
                permissionLauncher.launch(perm)
              }
            },
            modifier = Modifier.size(38.dp)
          ) {
            Icon(
              Icons.Default.Refresh,
              contentDescription = "Scan Audio",
              tint = CyanNeon,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }

      // 2. Tab Row
      TabRow(
        selectedTabIndex = selectedSubTab.ordinal,
        containerColor = DarkBackground,
        contentColor = CyanNeon,
        indicator = { tabPositions ->
          TabRowDefaults.SecondaryIndicator(
            Modifier.tabIndicatorOffset(tabPositions[selectedSubTab.ordinal]),
            color = CyanNeon,
            height = 2.5.dp
          )
        },
        divider = {
          Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DarkSurfaceBorder))
        }
      ) {
        MusicSubTab.entries.forEach { tab ->
          Tab(
            selected = selectedSubTab == tab,
            onClick = {
              selectedSubTab = tab
              selectedPlaylistForDetail = null
            },
            text = {
              Text(
                text = tab.title,
                fontSize = 12.sp,
                fontWeight = if (selectedSubTab == tab) FontWeight.Bold else FontWeight.Medium,
                color = if (selectedSubTab == tab) CyanNeon else TextSecondary
              )
            }
          )
        }
      }

      // 3. Tab Content
      Box(modifier = Modifier.fillMaxSize().weight(1f)) {
        when (selectedSubTab) {
          MusicSubTab.NOW_PLAYING -> {
            NowPlayingView(
              state = playerState,
              onPlayPause = { viewModel.toggleMusicPlayPause() },
              onNext = { viewModel.nextMusicSong() },
              onPrev = { viewModel.previousMusicSong() },
              onSeek = { viewModel.seekMusicTo(it) },
              onToggleShuffle = { viewModel.toggleMusicShuffle() },
              onToggleRepeat = { viewModel.toggleMusicRepeat() },
              onToggleFavorite = { song -> viewModel.toggleSongFavorite(song) },
              onSetSleepTimer = { mins -> viewModel.setMusicSleepTimer(mins) }
            )
          }
          MusicSubTab.PLAYLISTS -> {
            val detailPlaylist = selectedPlaylistForDetail
            if (detailPlaylist == null) {
              PlaylistsListView(
                playlists = allPlaylists,
                onCreatePlaylistClick = { showCreatePlaylistDialog = true },
                onSelectPlaylist = { selectedPlaylistForDetail = it },
                onDeletePlaylist = { viewModel.deletePlaylist(it.id) }
              )
            } else {
              PlaylistDetailView(
                playlist = detailPlaylist,
                viewModel = viewModel,
                onBack = { selectedPlaylistForDetail = null }
              )
            }
          }
          MusicSubTab.ALL_SONGS -> {
            AllSongsView(
              songs = allSongs,
              playlists = allPlaylists,
              currentSongId = playerState.currentSong?.id,
              isPlaying = playerState.isPlaying,
              onPlaySong = { song -> viewModel.playSong(song) },
              onPlayAll = { songs -> viewModel.playMusicQueue(songs, 0) },
              onToggleFavorite = { song -> viewModel.toggleSongFavorite(song) },
              onAddSongToPlaylist = { playlistId, songId ->
                viewModel.addSongToPlaylist(playlistId, songId)
                Toast.makeText(context, "Song added to playlist", Toast.LENGTH_SHORT).show()
              }
            )
          }
          MusicSubTab.MOOD_RULES -> {
            MoodRulesView(
              rules = playlistRules,
              playlists = allPlaylists,
              onAddRuleClick = { showAddRuleDialog = true },
              onDeleteRule = { viewModel.deletePlaylistRule(it) },
              onPlayRuleNow = { viewModel.playByCurrentTimeRule() }
            )
          }
          MusicSubTab.FAVORITES -> {
            FavoritesAndHistoryView(
              favoriteSongs = favoriteSongs,
              recentlyPlayed = recentlyPlayed,
              onPlaySong = { song -> viewModel.playSong(song) },
              onToggleFavorite = { song -> viewModel.toggleSongFavorite(song) }
            )
          }
        }
      }
    }

    // Dialogs
    if (showCreatePlaylistDialog) {
      CreatePlaylistDialog(
        onDismiss = { showCreatePlaylistDialog = false },
        onCreate = { name, desc, color ->
          viewModel.createPlaylist(name, desc, color)
          showCreatePlaylistDialog = false
        }
      )
    }

    if (showAddRuleDialog) {
      AddPlaylistRuleDialog(
        playlists = allPlaylists,
        onDismiss = { showAddRuleDialog = false },
        onAdd = { rule ->
          viewModel.addPlaylistRule(rule)
          showAddRuleDialog = false
        }
      )
    }
  }
}

// -------------------------------------------------------------
// 1. NOW PLAYING VIEW
// -------------------------------------------------------------
@Composable
private fun NowPlayingView(
  state: com.example.music.MusicPlayerState,
  onPlayPause: () -> Unit,
  onNext: () -> Unit,
  onPrev: () -> Unit,
  onSeek: (Long) -> Unit,
  onToggleShuffle: () -> Unit,
  onToggleRepeat: () -> Unit,
  onToggleFavorite: (SongEntity) -> Unit,
  onSetSleepTimer: (Int) -> Unit
) {
  val song = state.currentSong
  var showSleepTimerMenu by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 24.dp),
    contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      // Album Art Aura Container
      Box(
        modifier = Modifier
          .size(240.dp)
          .clip(RoundedCornerShape(32.dp))
          .background(
            Brush.radialGradient(
              listOf(
                CyanNeon.copy(alpha = 0.35f),
                VioletNeon.copy(alpha = 0.25f),
                DarkSurfaceElevated
              )
            )
          )
          .border(2.dp, DarkSurfaceBorder, RoundedCornerShape(32.dp)),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = CyanNeon,
            modifier = Modifier.size(72.dp)
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = song?.mood?.let { "${MusicMood.fromString(it).emoji} ${MusicMood.fromString(it).titleEn}" } ?: "🎵 Tranquil Flow",
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    item {
      // Song Title, Artist, and Favorite
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = song?.title ?: "Select a Song or Playlist",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = song?.let { "${it.artist} • ${it.album}" } ?: "Life Tracker Local Music Manager",
            color = TextMuted,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }

        if (song != null) {
          IconButton(onClick = { onToggleFavorite(song) }) {
            Icon(
              imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
              contentDescription = "Favorite",
              tint = if (song.isFavorite) Color(0xFFFF4081) else TextMuted
            )
          }
        }
      }
    }

    item {
      // Seekbar
      val currentSec = (state.currentPositionMs / 1000).toInt()
      val totalSec = (state.durationMs / 1000).toInt().coerceAtLeast(1)

      Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
          value = state.currentPositionMs.toFloat(),
          onValueChange = { onSeek(it.toLong()) },
          valueRange = 0f..state.durationMs.toFloat().coerceAtLeast(1f),
          colors = SliderDefaults.colors(
            thumbColor = CyanNeon,
            activeTrackColor = CyanNeon,
            inactiveTrackColor = DarkSurfaceBorder
          ),
          modifier = Modifier.fillMaxWidth()
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Text(
            text = TimeUtils.formatSecondsToMmSs(currentSec),
            color = TextMuted,
            fontSize = 11.sp
          )
          Text(
            text = TimeUtils.formatSecondsToMmSs(totalSec),
            color = TextMuted,
            fontSize = 11.sp
          )
        }
      }
    }

    item {
      // Main Playback Controls
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Shuffle
        IconButton(onClick = onToggleShuffle) {
          Icon(
            Icons.Default.Shuffle,
            contentDescription = "Shuffle",
            tint = if (state.isShuffle) CyanNeon else TextMuted
          )
        }

        // Previous
        IconButton(
          onClick = onPrev,
          modifier = Modifier.size(48.dp)
        ) {
          Icon(
            Icons.Default.SkipPrevious,
            contentDescription = "Previous",
            tint = TextPrimary,
            modifier = Modifier.size(32.dp)
          )
        }

        // Play / Pause Main Button
        Box(
          modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(CyanNeon, VioletNeon)))
            .clickable { onPlayPause() }
            .testTag("music_play_pause_button"),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
            contentDescription = if (state.isPlaying) "Pause" else "Play",
            tint = Color(0xFF0A101D),
            modifier = Modifier.size(36.dp)
          )
        }

        // Next
        IconButton(
          onClick = onNext,
          modifier = Modifier.size(48.dp)
        ) {
          Icon(
            Icons.Default.SkipNext,
            contentDescription = "Next",
            tint = TextPrimary,
            modifier = Modifier.size(32.dp)
          )
        }

        // Repeat
        IconButton(onClick = onToggleRepeat) {
          Icon(
            imageVector = if (state.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
            contentDescription = "Repeat",
            tint = if (state.repeatMode != RepeatMode.OFF) CyanNeon else TextMuted
          )
        }
      }
    }

    item {
      // Secondary Controls: Sleep Timer, Situation / Playlist Banner
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(DarkSurfaceElevated)
          .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(14.dp))
          .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.QueueMusic, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = state.currentPlaylist?.name?.let { "Playlist: $it" } ?: "All Songs Queue",
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
          )
        }

        Box {
          TextButton(onClick = { showSleepTimerMenu = true }) {
            Icon(Icons.Default.Timer, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (state.sleepTimerMinutesRemaining > 0) "${state.sleepTimerMinutesRemaining}m" else "Sleep Timer",
              color = CyanNeon,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }

          DropdownMenu(
            expanded = showSleepTimerMenu,
            onDismissRequest = { showSleepTimerMenu = false },
            modifier = Modifier.background(DarkSurfaceElevated)
          ) {
            listOf(0, 15, 30, 45, 60).forEach { mins ->
              DropdownMenuItem(
                text = { Text(if (mins == 0) "Turn Off" else "$mins Minutes", color = TextPrimary) },
                onClick = {
                  onSetSleepTimer(mins)
                  showSleepTimerMenu = false
                }
              )
            }
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// 2. PLAYLISTS LIST VIEW
// -------------------------------------------------------------
@Composable
private fun PlaylistsListView(
  playlists: List<PlaylistEntity>,
  onCreatePlaylistClick: () -> Unit,
  onSelectPlaylist: (PlaylistEntity) -> Unit,
  onDeletePlaylist: (PlaylistEntity) -> Unit
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 14.dp, bottom = 100.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Header & Create Button
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("MY PLAYLISTS (अनलिमिटेड प्लेलिस्ट्स)", color = CyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          Text("Personalized Music Collections", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Button(
          onClick = onCreatePlaylistClick,
          colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("New Playlist", color = DarkBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    items(playlists) { playlist ->
      var showMenu by remember { mutableStateOf(false) }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(DarkSurfaceElevated)
          .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
          .clickable { onSelectPlaylist(playlist) }
          .padding(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(android.graphics.Color.parseColor(playlist.colorHex.ifBlank { "#00F0FF" })).copy(alpha = 0.2f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.PlaylistPlay,
                contentDescription = null,
                tint = Color(android.graphics.Color.parseColor(playlist.colorHex.ifBlank { "#00F0FF" })),
                modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = playlist.name,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              )
              if (playlist.description.isNotBlank()) {
                Text(
                  text = playlist.description,
                  color = TextMuted,
                  fontSize = 11.sp
                )
              }
            }
          }

          Box {
            IconButton(onClick = { showMenu = true }) {
              Icon(Icons.Default.MoreVert, contentDescription = null, tint = TextMuted)
            }
            DropdownMenu(
              expanded = showMenu,
              onDismissRequest = { showMenu = false },
              modifier = Modifier.background(DarkSurfaceElevated)
            ) {
              DropdownMenuItem(
                text = { Text("Open Playlist", color = TextPrimary) },
                onClick = {
                  showMenu = false
                  onSelectPlaylist(playlist)
                }
              )
              DropdownMenuItem(
                text = { Text("Delete Playlist", color = Color(0xFFFF5252)) },
                onClick = {
                  showMenu = false
                  onDeletePlaylist(playlist)
                }
              )
            }
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// 3. PLAYLIST DETAIL VIEW
// -------------------------------------------------------------
@Composable
private fun PlaylistDetailView(
  playlist: PlaylistEntity,
  viewModel: LifeTrackerViewModel,
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val songsFlow = remember(playlist.id) { viewModel.repository.getSongsForPlaylistFlow(playlist.id) }
  val songs by songsFlow.collectAsState(initial = emptyList())
  val allSongs by viewModel.allSongs.collectAsState()

  var showAddSongsDialog by remember { mutableStateOf(false) }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(playlist.name, color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
          Text("${songs.size} songs • ${playlist.description}", color = TextMuted, fontSize = 12.sp)
        }

        Row {
          Button(
            onClick = {
              if (songs.isNotEmpty()) {
                viewModel.playMusicQueue(songs, 0, playlist)
              } else {
                Toast.makeText(context, "Add songs to play", Toast.LENGTH_SHORT).show()
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Play All", color = DarkBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }

          Spacer(modifier = Modifier.width(8.dp))

          IconButton(
            onClick = { showAddSongsDialog = true },
            modifier = Modifier
              .clip(CircleShape)
              .background(DarkSurfaceElevated)
              .border(1.dp, DarkSurfaceBorder, CircleShape)
          ) {
            Icon(Icons.Default.Add, contentDescription = "Add Songs", tint = CyanNeon)
          }
        }
      }
    }

    if (songs.isEmpty()) {
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("No songs in this playlist yet", color = TextMuted, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))
            Button(
              onClick = { showAddSongsDialog = true },
              colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceElevated)
            ) {
              Text("+ Add Songs From Library", color = CyanNeon)
            }
          }
        }
      }
    }

    items(songs) { song ->
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(DarkSurfaceElevated)
          .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
          .clickable { viewModel.playMusicQueue(songs, songs.indexOf(song), playlist) }
          .padding(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(song.title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
              Text("${song.artist} • ${TimeUtils.formatSecondsToMmSs((song.durationMs / 1000).toInt())}", color = TextMuted, fontSize = 11.sp)
            }
          }

          IconButton(onClick = { viewModel.removeSongFromPlaylist(playlist.id, song.id) }) {
            Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextMuted, modifier = Modifier.size(16.dp))
          }
        }
      }
    }
  }

  if (showAddSongsDialog) {
    AddSongsToPlaylistDialog(
      allSongs = allSongs,
      existingSongIds = songs.map { it.id }.toSet(),
      onDismiss = { showAddSongsDialog = false },
      onAddSong = { songId -> viewModel.addSongToPlaylist(playlist.id, songId) }
    )
  }
}

// -------------------------------------------------------------
// 4. ALL SONGS VIEW
// -------------------------------------------------------------
@Composable
private fun AllSongsView(
  songs: List<SongEntity>,
  playlists: List<PlaylistEntity>,
  currentSongId: String?,
  isPlaying: Boolean,
  onPlaySong: (SongEntity) -> Unit,
  onPlayAll: (List<SongEntity>) -> Unit,
  onToggleFavorite: (SongEntity) -> Unit,
  onAddSongToPlaylist: (Long, String) -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }
  val filtered = remember(songs, searchQuery) {
    if (searchQuery.isBlank()) songs
    else songs.filter {
      it.title.contains(searchQuery, ignoreCase = true) ||
      it.artist.contains(searchQuery, ignoreCase = true) ||
      it.album.contains(searchQuery, ignoreCase = true)
    }
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    item {
      // Search Bar
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Search songs, artists, moods...", color = TextMuted) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyanNeon) },
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = CyanNeon,
          unfocusedBorderColor = DarkSurfaceBorder,
          focusedTextColor = TextPrimary,
          unfocusedTextColor = TextPrimary
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
      )
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("${filtered.size} SONGS AVAILABLE", color = CyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)

        if (filtered.isNotEmpty()) {
          TextButton(onClick = { onPlayAll(filtered) }) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Play All", color = CyanNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    items(filtered) { song ->
      var showAddToPlaylistMenu by remember { mutableStateOf(false) }
      val isThisSongPlaying = song.id == currentSongId && isPlaying

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(if (song.id == currentSongId) CyanNeon.copy(alpha = 0.1f) else DarkSurfaceElevated)
          .border(1.dp, if (song.id == currentSongId) CyanNeon else DarkSurfaceBorder, RoundedCornerShape(14.dp))
          .clickable { onPlaySong(song) }
          .padding(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (isThisSongPlaying) CyanNeon else DarkSurface),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = if (isThisSongPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = if (isThisSongPlaying) DarkBackground else CyanNeon,
                modifier = Modifier.size(18.dp)
              )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
              Text(
                text = song.title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "${song.artist} • ${TimeUtils.formatSecondsToMmSs((song.durationMs / 1000).toInt())}",
                color = TextMuted,
                fontSize = 11.sp,
                maxLines = 1
              )
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onToggleFavorite(song) }) {
              Icon(
                imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (song.isFavorite) Color(0xFFFF4081) else TextMuted,
                modifier = Modifier.size(18.dp)
              )
            }

            Box {
              IconButton(onClick = { showAddToPlaylistMenu = true }) {
                Icon(Icons.Default.PlaylistAdd, contentDescription = "Add to Playlist", tint = TextSecondary, modifier = Modifier.size(20.dp))
              }

              DropdownMenu(
                expanded = showAddToPlaylistMenu,
                onDismissRequest = { showAddToPlaylistMenu = false },
                modifier = Modifier.background(DarkSurfaceElevated)
              ) {
                Text(
                  text = "Add to Playlist:",
                  color = TextMuted,
                  fontSize = 11.sp,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
                playlists.forEach { pl ->
                  DropdownMenuItem(
                    text = { Text(pl.name, color = TextPrimary) },
                    onClick = {
                      onAddSongToPlaylist(pl.id, song.id)
                      showAddToPlaylistMenu = false
                    }
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// 5. MOOD & TIME RULES VIEW
// -------------------------------------------------------------
@Composable
private fun MoodRulesView(
  rules: List<PlaylistRuleEntity>,
  playlists: List<PlaylistEntity>,
  onAddRuleClick: () -> Unit,
  onDeleteRule: (Long) -> Unit,
  onPlayRuleNow: () -> Unit
) {
  val playlistMap = remember(playlists) { playlists.associateBy { it.id } }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .background(
            Brush.linearGradient(listOf(Color(0xFF0D2538), Color(0xFF1B1B38)))
          )
          .border(1.dp, CyanNeon.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
          .padding(16.dp)
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("PLAYLIST + TIME + MOOD SYSTEM", color = CyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
              Text("Automated Routine Music", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Button(
              onClick = onPlayRuleNow,
              colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
              shape = RoundedCornerShape(10.dp)
            ) {
              Text("Play Current", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
          }
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "समय और मूड के अनुसार परिभाषित नियम स्वचालित रूप से सही समय पर सही संगीत चुनते हैं।",
            color = TextSecondary,
            fontSize = 12.sp
          )
        }
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("ACTIVE TIME RULES (${rules.size})", color = CyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Button(
          onClick = onAddRuleClick,
          colors = ButtonDefaults.buttonColors(containerColor = VioletNeon.copy(alpha = 0.25f)),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = null, tint = VioletNeon, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("Add Rule", color = VioletNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    items(rules) { rule ->
      val playlist = playlistMap[rule.playlistId]
      val moodObj = MusicMood.fromString(rule.mood)

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(DarkSurfaceElevated)
          .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(14.dp))
          .padding(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "${TimeUtils.minutesTo12Hour(rule.startMinutes)} – ${TimeUtils.minutesTo12Hour(rule.endMinutes)}",
                color = CyanNeon,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
              )
              Spacer(modifier = Modifier.width(8.dp))
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(Color(android.graphics.Color.parseColor(moodObj.colorHex)).copy(alpha = 0.2f))
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(
                  text = "${moodObj.emoji} ${moodObj.titleEn}",
                  color = Color(android.graphics.Color.parseColor(moodObj.colorHex)),
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Playlist: ${playlist?.name ?: "All Songs"} • ${rule.situation}",
              color = TextPrimary,
              fontSize = 13.sp,
              fontWeight = FontWeight.Medium
            )
            if (rule.linkedCategory != null) {
              Text("Routine: ${rule.linkedCategory}", color = TextMuted, fontSize = 11.sp)
            }
          }

          IconButton(onClick = { onDeleteRule(rule.id) }) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(18.dp))
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// 6. FAVORITES & RECENTLY PLAYED
// -------------------------------------------------------------
@Composable
private fun FavoritesAndHistoryView(
  favoriteSongs: List<SongEntity>,
  recentlyPlayed: List<SongEntity>,
  onPlaySong: (SongEntity) -> Unit,
  onToggleFavorite: (SongEntity) -> Unit
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    item {
      Text("FAVORITE TRACKS (${favoriteSongs.size})", color = Color(0xFFFF4081), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }

    if (favoriteSongs.isEmpty()) {
      item {
        Text("No favorite songs yet. Tap the heart icon on any song.", color = TextMuted, fontSize = 12.sp)
      }
    }

    items(favoriteSongs) { song ->
      SongRowItem(song = song, onPlay = { onPlaySong(song) }, onToggleFav = { onToggleFavorite(song) })
    }

    item {
      Spacer(modifier = Modifier.height(10.dp))
      Text("RECENTLY PLAYED (${recentlyPlayed.size})", color = CyanNeon, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }

    items(recentlyPlayed) { song ->
      SongRowItem(song = song, onPlay = { onPlaySong(song) }, onToggleFav = { onToggleFavorite(song) })
    }
  }
}

@Composable
private fun SongRowItem(
  song: SongEntity,
  onPlay: () -> Unit,
  onToggleFav: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(DarkSurfaceElevated)
      .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
      .clickable { onPlay() }
      .padding(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Icon(Icons.Default.MusicNote, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(song.title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
          Text("${song.artist} • ${TimeUtils.formatSecondsToMmSs((song.durationMs / 1000).toInt())}", color = TextMuted, fontSize = 11.sp)
        }
      }

      IconButton(onClick = onToggleFav) {
        Icon(
          imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
          contentDescription = null,
          tint = if (song.isFavorite) Color(0xFFFF4081) else TextMuted,
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}

// -------------------------------------------------------------
// DIALOGS: CREATE PLAYLIST, ADD RULE, ADD SONGS PICKER
// -------------------------------------------------------------
@Composable
private fun CreatePlaylistDialog(
  onDismiss: () -> Unit,
  onCreate: (String, String, String) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var selectedColor by remember { mutableStateOf("#00F0FF") }
  val colors = listOf("#00F0FF", "#F59E0B", "#8B5CF6", "#10B981", "#EC4899", "#6366F1", "#F97316")

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Create New Playlist", color = TextPrimary, fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Playlist Name (e.g. Travel, Gym, Focus)") },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyanNeon,
            unfocusedBorderColor = DarkSurfaceBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
          ),
          modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Description (Optional)") },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyanNeon,
            unfocusedBorderColor = DarkSurfaceBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
          ),
          modifier = Modifier.fillMaxWidth()
        )

        Text("Select Accent Color", color = TextSecondary, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          colors.forEach { hex ->
            Box(
              modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color(android.graphics.Color.parseColor(hex)))
                .border(
                  2.dp,
                  if (selectedColor == hex) Color.White else Color.Transparent,
                  CircleShape
                )
                .clickable { selectedColor = hex }
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = { if (name.isNotBlank()) onCreate(name, description, selectedColor) },
        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
      ) {
        Text("Create", color = DarkBackground, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TextMuted)
      }
    },
    containerColor = DarkSurfaceElevated
  )
}

@Composable
private fun AddPlaylistRuleDialog(
  playlists: List<PlaylistEntity>,
  onDismiss: () -> Unit,
  onAdd: (PlaylistRuleEntity) -> Unit
) {
  var selectedPlaylistId by remember { mutableStateOf(playlists.firstOrNull()?.id ?: 1L) }
  var startHour by remember { mutableIntStateOf(6) }
  var endHour by remember { mutableIntStateOf(7) }
  var selectedMood by remember { mutableStateOf(MusicMood.CALM) }
  var situation by remember { mutableStateOf("Morning Routine") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Time + Mood Rule", color = TextPrimary, fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Select Playlist", color = TextSecondary, fontSize = 12.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(playlists) { pl ->
            val isSel = pl.id == selectedPlaylistId
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSel) CyanNeon.copy(alpha = 0.2f) else DarkSurface)
                .border(1.dp, if (isSel) CyanNeon else DarkSurfaceBorder, RoundedCornerShape(8.dp))
                .clickable { selectedPlaylistId = pl.id }
                .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Text(pl.name, color = if (isSel) CyanNeon else TextSecondary, fontSize = 12.sp)
            }
          }
        }

        Text("Select Mood", color = TextSecondary, fontSize = 12.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          items(MusicMood.entries) { m ->
            val isSel = m == selectedMood
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSel) CyanNeon.copy(alpha = 0.2f) else DarkSurface)
                .border(1.dp, if (isSel) CyanNeon else DarkSurfaceBorder, RoundedCornerShape(8.dp))
                .clickable { selectedMood = m }
                .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
              Text("${m.emoji} ${m.titleEn}", color = if (isSel) CyanNeon else TextSecondary, fontSize = 11.sp)
            }
          }
        }

        OutlinedTextField(
          value = situation,
          onValueChange = { situation = it },
          label = { Text("Situation / Activity Label") },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyanNeon,
            unfocusedBorderColor = DarkSurfaceBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
          ),
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onAdd(
            PlaylistRuleEntity(
              playlistId = selectedPlaylistId,
              startMinutes = startHour * 60,
              endMinutes = endHour * 60,
              mood = selectedMood.name,
              situation = situation.ifBlank { "Custom Rule" },
              autoPlay = false
            )
          )
        },
        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)
      ) {
        Text("Save Rule", color = DarkBackground, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel", color = TextMuted)
      }
    },
    containerColor = DarkSurfaceElevated
  )
}

@Composable
private fun AddSongsToPlaylistDialog(
  allSongs: List<SongEntity>,
  existingSongIds: Set<String>,
  onDismiss: () -> Unit,
  onAddSong: (String) -> Unit
) {
  val available = allSongs.filter { it.id !in existingSongIds }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Songs to Playlist", color = TextPrimary, fontWeight = FontWeight.Bold) },
    text = {
      if (available.isEmpty()) {
        Text("All available songs are already in this playlist!", color = TextMuted)
      } else {
        LazyColumn(
          modifier = Modifier.height(300.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(available) { song ->
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurface)
                .clickable {
                  onAddSong(song.id)
                }
                .padding(10.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(song.title, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                  Text(song.artist, color = TextMuted, fontSize = 10.sp)
                }
                Icon(Icons.Default.Add, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
              }
            }
          }
        }
      }
    },
    confirmButton = {
      Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = CyanNeon)) {
        Text("Done", color = DarkBackground, fontWeight = FontWeight.Bold)
      }
    },
    containerColor = DarkSurfaceElevated
  )
}
