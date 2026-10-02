package com.example

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.example.alarm.AlarmScheduler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.meditation.MeditationAudioService
import com.example.ui.screens.AlarmCenterScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.MeditationScreen
import com.example.ui.screens.MusicScreen
import com.example.ui.screens.ReflectionScreen
import com.example.ui.screens.ReportScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ShortContentTrackerScreen
import com.example.ui.screens.TimelineScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.screens.WeekScreen
import com.example.ui.theme.CardBorderGradient
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.LifeTrackerTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletGlow
import com.example.ui.theme.VioletNeon
import com.example.ui.viewmodel.LifeTrackerViewModel
import com.example.ui.viewmodel.MainTab
import com.example.util.BatteryOptimizationDialog
import com.example.util.BatteryOptimizationHelper

class MainActivity : ComponentActivity() {
  private val viewModel: LifeTrackerViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
    }

    if (intent?.getBooleanExtra(MeditationAudioService.EXTRA_OPEN_MEDITATION, false) == true) {
      viewModel.setTab(MainTab.MEDITATION)
    }

    // Ensure notification channels are registered and timetable alarms scheduled
    AlarmScheduler.createNotificationChannels(this)
    AlarmScheduler.rescheduleAllTimetableAlarms(this)
    com.example.focus.FocusModeManager.onBootOrScheduleChange(this)
    com.example.blackscreen.BlackScreenManager.syncServiceState(this)

    setContent {
      LifeTrackerTheme {
        LifeTrackerApp(viewModel = viewModel)
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    if (intent.getBooleanExtra(MeditationAudioService.EXTRA_OPEN_MEDITATION, false)) {
      viewModel.setTab(MainTab.MEDITATION)
    }
  }

  override fun onResume() {
    super.onResume()
    viewModel.onAppResume()
  }

  override fun onPause() {
    super.onPause()
    viewModel.onAppPause()
  }
}

@Composable
fun LifeTrackerApp(viewModel: LifeTrackerViewModel) {
  val currentTab by viewModel.currentTab.collectAsState()
  val isAlarmEnabled by viewModel.isAlarmEnabled.collectAsState()
  val context = LocalContext.current

  var showBatteryPrompt by remember { mutableStateOf(false) }

  // Auto-request notification permission on Android 13+ if not yet granted
  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
    onResult = { isGranted ->
      if (isGranted) {
        AlarmScheduler.createNotificationChannels(context)
        AlarmScheduler.rescheduleAllTimetableAlarms(context)
      }
    }
  )

  LaunchedEffect(Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }

  // Register midnight date refresh receiver (Fix 3)
  DisposableEffect(context) {
    val receiver = object : BroadcastReceiver() {
      override fun onReceive(c: Context?, intent: Intent?) {
        viewModel.onSystemDateTick()
      }
    }
    val filter = IntentFilter().apply {
      addAction(Intent.ACTION_TIME_TICK)
      addAction(Intent.ACTION_DATE_CHANGED)
      addAction(Intent.ACTION_TIME_CHANGED)
      addAction(Intent.ACTION_TIMEZONE_CHANGED)
    }
    context.registerReceiver(receiver, filter)
    onDispose {
      try {
        context.unregisterReceiver(receiver)
      } catch (_: Exception) {}
    }
  }

  // Battery optimization prompt for recurring alarm reliability (Fix 2)
  androidx.compose.runtime.LaunchedEffect(isAlarmEnabled) {
    if (isAlarmEnabled && !BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)) {
      showBatteryPrompt = true
    }
  }

  if (showBatteryPrompt) {
    BatteryOptimizationDialog(
      onDismiss = { showBatteryPrompt = false },
      onOpenSettings = {
        BatteryOptimizationHelper.requestIgnoreBatteryOptimization(context)
      }
    )
  }

  val isFocusModeActive by viewModel.isFocusModeActive.collectAsState()
  val isBlackScreenOverlayActive by viewModel.isBlackScreenOverlayActive.collectAsState()

  if (isFocusModeActive) {
    com.example.ui.screens.FocusRestrictionScreen(viewModel = viewModel)
  } else if (isBlackScreenOverlayActive) {
    com.example.ui.screens.InAppBlackScreenOverlay(
      viewModel = viewModel,
      onExit = { viewModel.deactivateBlackScreenOverlay() }
    )
  } else {
    // Android hardware back button handler: returns to previous screen or Home
    BackHandler(enabled = currentTab != MainTab.TODAY) {
      viewModel.navigateBack()
    }

    Scaffold(
      modifier = Modifier
        .fillMaxSize()
        .background(DarkBackground),
      containerColor = DarkBackground,
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
      bottomBar = {
        LifeTrackerBottomBar(
          currentTab = currentTab,
          onTabSelected = { tab -> viewModel.navigateTo(tab) }
        )
      }
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
        contentAlignment = Alignment.TopCenter
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 680.dp)
        ) {
          Crossfade(
            targetState = currentTab,
            animationSpec = tween(durationMillis = 220),
            label = "tab_crossfade"
          ) { tab ->
            when (tab) {
              MainTab.TODAY -> TodayScreen(viewModel = viewModel)
              MainTab.ALARM_CENTER -> AlarmCenterScreen(viewModel = viewModel)
              MainTab.MEDITATION -> MeditationScreen(viewModel = viewModel)
              MainTab.MUSIC -> MusicScreen(viewModel = viewModel)
              MainTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
              MainTab.WEEK -> WeekScreen(viewModel = viewModel)
              MainTab.CALENDAR -> CalendarScreen(viewModel = viewModel)
              MainTab.REPORT -> ReportScreen(viewModel = viewModel)
              MainTab.REFLECTION -> ReflectionScreen(viewModel = viewModel)
              MainTab.TIMELINE -> TimelineScreen(viewModel = viewModel)
              MainTab.SHORT_CONTENT_TRACKER -> ShortContentTrackerScreen(viewModel = viewModel)
            }
          }
        }
      }
    }
  }
}

@Composable
fun LifeTrackerBottomBar(
  currentTab: MainTab,
  onTabSelected: (MainTab) -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .navigationBarsPadding()
      .padding(horizontal = 18.dp, vertical = 6.dp),
    contentAlignment = Alignment.Center
  ) {
    // Floating Glassmorphic Pill Container - constrained for tablet/foldable width
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .widthIn(max = 580.dp)
        .height(66.dp)
        .clip(RoundedCornerShape(26.dp))
        .background(com.example.ui.theme.ObsidianElevated.copy(alpha = 0.95f))
        .border(
          width = 1.dp,
          brush = Brush.linearGradient(
            listOf(
              com.example.ui.theme.GoldBrass.copy(alpha = 0.45f),
              com.example.ui.theme.ObsidianBorder,
              com.example.ui.theme.GoldDark.copy(alpha = 0.3f)
            )
          ),
          shape = RoundedCornerShape(26.dp)
        )
        .padding(horizontal = 6.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
      ) {
        val items = listOf(
          NavigationItemData(MainTab.TODAY, "Today", Icons.Filled.Home, Icons.Outlined.Home, "tab_today"),
          NavigationItemData(MainTab.CALENDAR, "Calendar", Icons.Filled.DateRange, Icons.Outlined.DateRange, "tab_calendar"),
          NavigationItemData(MainTab.REPORT, "Progress", Icons.Filled.Insights, Icons.Filled.Insights, "tab_progress"),
          NavigationItemData(MainTab.REFLECTION, "Notebook", Icons.Filled.Spa, Icons.Outlined.Spa, "tab_notebook"),
          NavigationItemData(MainTab.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "tab_settings")
        )

        items.forEach { item ->
          val isSelected = currentTab == item.tab
          val interactionSource = remember { MutableInteractionSource() }

          Box(
            modifier = Modifier
              .weight(1f)
              .height(50.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(
                if (isSelected) com.example.ui.theme.GoldBrass.copy(alpha = 0.14f) else Color.Transparent
              )
              .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(color = com.example.ui.theme.GoldBrass, bounded = true)
              ) { onTabSelected(item.tab) }
              .testTag(item.testTag),
            contentAlignment = Alignment.Center
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.label,
                tint = if (isSelected) com.example.ui.theme.GoldBrass else com.example.ui.theme.WarmMuted,
                modifier = Modifier.size(19.dp)
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = item.label,
                color = if (isSelected) com.example.ui.theme.WarmOffWhite else com.example.ui.theme.WarmMuted,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 0.3.sp
              )
              if (isSelected) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                  modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(com.example.ui.theme.GoldBrass)
                )
              }
            }
          }
        }
      }
    }
  }
}

private data class NavigationItemData(
  val tab: MainTab,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String
)
