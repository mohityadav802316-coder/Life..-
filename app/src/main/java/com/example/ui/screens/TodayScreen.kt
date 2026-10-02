package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DashboardCustomize
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import com.example.data.model.DailyChallengeEntity
import com.example.data.model.DashboardPreset
import com.example.data.model.DashboardSectionId
import com.example.data.model.EnergyMode
import com.example.shortcontent.AccessibilityHelper
import com.example.ui.components.DashboardBuilderBottomSheet
import com.example.ui.components.ShortContentSummaryCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DayTaskEntity
import com.example.data.model.TaskStatus
import com.example.ui.components.AchievementsDialog
import com.example.ui.components.EmergencyRecoveryBottomSheet
import com.example.ui.components.GoalBottomSheet
import com.example.ui.components.PersonalInsightsDialog
import com.example.ui.components.QuickAccessBottomSheet
import com.example.ui.components.TaskBottomSheet
import com.example.ui.components.TaskItemCard
import com.example.ui.components.DashboardNextActivityCard
import com.example.ui.components.DashboardDailyProgressCard
import com.example.ui.components.ChronometerAdherenceDial
import com.example.ui.components.ChronometerScheduleTimeline
import com.example.ui.components.ChronoTimeBlockDialog
import com.example.ui.components.DashboardPriorityTaskCard
import com.example.ui.components.DashboardStreakCard
import com.example.ui.components.DashboardEnergyModeCard
import com.example.ui.components.DashboardMeditationCard
import com.example.ui.components.DashboardMusicCard
import com.example.ui.components.DashboardDailyChallengeCard
import com.example.ui.components.DashboardNextAlarmCard
import com.example.ui.components.DashboardRecoveryDayCard
import com.example.ui.components.DashboardWeeklySummaryCard
import com.example.ui.components.DashboardRoutineHeader
import com.example.ui.theme.ChronoSerifFamily
import com.example.ui.theme.GoldBrass
import com.example.ui.theme.GoldHighlight
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.SageGreen
import com.example.ui.theme.WarmMuted
import com.example.ui.theme.WarmOffWhite
import com.example.ui.theme.WarmParchment
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
import com.example.ui.viewmodel.MainTab
import com.example.util.TimeUtils

import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
  viewModel: LifeTrackerViewModel,
  modifier: Modifier = Modifier
) {
  val selectedDate by viewModel.selectedDate.collectAsState()
  val summary by viewModel.summaryForSelectedDate.collectAsState()
  val tasks by viewModel.tasksForSelectedDate.collectAsState()
  val streakInfo by viewModel.streakInfo.collectAsState()
  val userName by viewModel.userName.collectAsState()
  val isAlarmEnabled by viewModel.isAlarmEnabled.collectAsState()
  val wakeUpMinutes by viewModel.wakeUpMinutes.collectAsState()
  val currentCycleDay by viewModel.currentCycleDay.collectAsState()
  val nowMinutes by viewModel.currentTimeMinutes.collectAsState()
  val allMeditationSessions by viewModel.allMeditationSessions.collectAsState()
  val personalInsights by viewModel.personalInsightsData.collectAsState()
  val allTasks by viewModel.allTasks.collectAsState()
  val dailyEnergyMode by viewModel.dailyEnergyMode.collectAsState()
  val dailyChallenge by viewModel.dailyChallenge.collectAsState()
  val musicPlayerState by viewModel.musicPlayerState.collectAsState()
  val isRecoveryDayActive by viewModel.isRecoveryDayActive.collectAsState()
  val userSettings by viewModel.userSettings.collectAsState()

  val isToday = selectedDate == viewModel.todayDate
  val isFutureDate = remember(selectedDate) { TimeUtils.isFutureDate(selectedDate) }

  val routineTasks = remember(tasks) { tasks.filter { !it.isExtra } }
  val extraTasks = remember(tasks) { tasks.filter { it.isExtra } }

  // State dialogs
  var showQuickAccessSheet by remember { mutableStateOf(false) }
  var showAchievementsDialog by remember { mutableStateOf(false) }
  var showInsightsDialog by remember { mutableStateOf(false) }
  var showGoalsDialog by remember { mutableStateOf(false) }
  var showEmergencyRecoverySheet by remember { mutableStateOf(false) }
  var editingTask by remember { mutableStateOf<DayTaskEntity?>(null) }
  var isAddingTask by remember { mutableStateOf(false) }
  var showFilters by remember { mutableStateOf(false) }
  var showExtraTasksCollapsed by remember { mutableStateOf(true) }
  var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }

  val context = LocalContext.current
  val todayShortContentSummary by viewModel.todayShortContentSummary.collectAsState()
  var isAccessibilityEnabled by remember { mutableStateOf(AccessibilityHelper.isAccessibilityServiceEnabled(context)) }
  var showDashboardBuilderSheet by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    isAccessibilityEnabled = AccessibilityHelper.isAccessibilityServiceEnabled(context)
  }

  val currentPresetKey = userSettings?.dashboardPreset ?: "CUSTOM"
  val currentOrderKeys = userSettings?.dashboardOrder ?: DashboardSectionId.defaultOrderedList.joinToString(",") { it.key }
  val currentDisabledKeys = userSettings?.dashboardDisabled ?: ""

  val orderedSections = remember(currentOrderKeys) {
    val keys = currentOrderKeys.split(",").map { it.trim() }.filter { it.isNotBlank() }
    val mapped = keys.mapNotNull { DashboardSectionId.fromKey(it) }.toMutableList()
    DashboardSectionId.defaultOrderedList.forEach { sec ->
      if (!mapped.contains(sec)) mapped.add(sec)
    }
    mapped
  }

  val disabledSections = remember(currentDisabledKeys) {
    currentDisabledKeys.split(",").map { it.trim() }
      .mapNotNull { DashboardSectionId.fromKey(it) }
      .toSet()
  }

  // Find Current / Next Activity
  val nextOrCurrentTask = remember(routineTasks, isToday, nowMinutes) {
    if (!isToday || routineTasks.isEmpty()) {
      routineTasks.firstOrNull()
    } else {
      routineTasks.firstOrNull { it.timeMinutes >= nowMinutes && it.status != TaskStatus.COMPLETE }
        ?: routineTasks.lastOrNull { it.status != TaskStatus.COMPLETE }
        ?: routineTasks.lastOrNull()
    }
  }

  // Priority Task (Highest pending task for today)
  val priorityTask = remember(tasks) {
    tasks.firstOrNull { (it.priority == "HIGH" || it.priority == "IMPORTANT") && it.status != TaskStatus.COMPLETE }
      ?: tasks.firstOrNull { it.status != TaskStatus.COMPLETE }
  }

  val todayMeditationSessions = remember(allMeditationSessions, selectedDate) {
    allMeditationSessions.filter { it.date == selectedDate }
  }

  val filteredRoutineTasks = remember(routineTasks, selectedCategoryFilter) {
    if (selectedCategoryFilter.isNullOrBlank()) routineTasks
    else routineTasks.filter { it.category.equals(selectedCategoryFilter, ignoreCase = true) }
  }

  val listState = rememberLazyListState()

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBackground)
      .statusBarsPadding()
  ) {
    LazyColumn(
      state = listState,
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // 1. TOP HEADER: Status area showing current time, date/day and “Day X of 7”
      item {
        val liveTime by viewModel.currentLiveTime.collectAsState()
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              // Current Time in precision horological serif
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  text = liveTime.ifBlank { TimeUtils.getNowTimeShort() },
                  color = WarmOffWhite,
                  fontFamily = ChronoSerifFamily,
                  fontSize = 28.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                  modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(GoldBrass)
                )
              }
              Text(
                text = "${TimeUtils.formatDateDisplay(selectedDate)} • Day $currentCycleDay of 7",
                color = GoldBrass,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.4.sp
              )
            }

            // Top Status Action Badges
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
              // Energy Mode Pill
              val energyColor = when (dailyEnergyMode) {
                EnergyMode.LOW -> Color(0xFF7AA2BA)
                EnergyMode.NORMAL -> GoldBrass
                EnergyMode.HIGH -> GoldHighlight
              }
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .background(energyColor.copy(alpha = 0.14f))
                  .border(1.dp, energyColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                  .clickable {
                    val nextMode = when (dailyEnergyMode) {
                      EnergyMode.NORMAL -> EnergyMode.HIGH
                      EnergyMode.HIGH -> EnergyMode.LOW
                      EnergyMode.LOW -> EnergyMode.NORMAL
                    }
                    viewModel.setDailyEnergyMode(nextMode)
                  }
                  .padding(horizontal = 8.dp, vertical = 6.dp)
                  .testTag("home_energy_mode_pill")
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = dailyEnergyMode.emoji,
                    fontSize = 11.sp
                  )
                  Spacer(modifier = Modifier.width(3.dp))
                  Text(
                    text = dailyEnergyMode.label,
                    color = energyColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }

              // Alarm Pill
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .background(if (isAlarmEnabled) GoldBrass.copy(alpha = 0.14f) else ObsidianCard)
                  .border(1.dp, if (isAlarmEnabled) GoldBrass.copy(alpha = 0.4f) else ObsidianBorder, RoundedCornerShape(12.dp))
                  .clickable { viewModel.setTab(MainTab.ALARM_CENTER) }
                  .padding(horizontal = 8.dp, vertical = 6.dp)
                  .testTag("home_alarm_pill")
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = "Alarm Status",
                    tint = if (isAlarmEnabled) GoldBrass else WarmMuted,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = if (isAlarmEnabled) TimeUtils.minutesTo12Hour(wakeUpMinutes) else "Off",
                    color = if (isAlarmEnabled) GoldBrass else WarmMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }

              // Dashboard Builder Shortcut Icon
              Box(
                modifier = Modifier
                  .size(34.dp)
                  .clip(CircleShape)
                  .background(GoldBrass.copy(alpha = 0.16f))
                  .border(1.dp, GoldBrass.copy(alpha = 0.45f), CircleShape)
                  .clickable { showDashboardBuilderSheet = true }
                  .testTag("home_dashboard_builder_shortcut"),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.DashboardCustomize,
                  contentDescription = "Personal Dashboard Builder",
                  tint = GoldBrass,
                  modifier = Modifier.size(17.dp)
                )
              }
            }
          }
        }
      }

      // -----------------------------------------------------------
      // DYNAMIC DASHBOARD SECTIONS (Customized via Dashboard Builder)
      // -----------------------------------------------------------
      orderedSections.forEach { section ->
        if (disabledSections.contains(section)) return@forEach

        when (section) {
          DashboardSectionId.SHORT_CONTENT_TRACKER -> {
            item(key = "dash_short_content") {
              ShortContentSummaryCard(
                summary = todayShortContentSummary,
                isAccessibilityEnabled = isAccessibilityEnabled,
                onClick = { viewModel.navigateTo(MainTab.SHORT_CONTENT_TRACKER) }
              )
            }
          }

          DashboardSectionId.NEXT_ACTIVITY -> {
            item(key = "dash_next_activity") {
              DashboardNextActivityCard(
                nextOrCurrentTask = nextOrCurrentTask,
                isToday = isToday,
                nowMinutes = nowMinutes,
                onStatusChange = { task, status -> viewModel.updateTaskStatus(task, status) }
              )
            }
          }

          DashboardSectionId.DAILY_PROGRESS -> {
            item(key = "dash_daily_progress") {
              ChronometerAdherenceDial(
                completionPercentage = summary.completionPercentage,
                completedCount = summary.completedCount,
                partialCount = summary.partialCount,
                missedCount = summary.missedCount,
                totalTasks = summary.totalTasks,
                totalScore = summary.totalScore,
                currentCycleDay = currentCycleDay
              )
            }
          }

          DashboardSectionId.PRIORITY_TASK -> {
            item(key = "dash_priority_task") {
              DashboardPriorityTaskCard(
                priorityTask = priorityTask,
                onStatusChange = { task, status -> viewModel.updateTaskStatus(task, status) }
              )
            }
          }

          DashboardSectionId.STREAK -> {
            item(key = "dash_streak") {
              DashboardStreakCard(
                streakInfo = streakInfo
              )
            }
          }

          DashboardSectionId.ENERGY_MODE -> {
            item(key = "dash_energy_mode") {
              DashboardEnergyModeCard(
                dailyEnergyMode = dailyEnergyMode,
                onModeChange = { mode -> viewModel.setDailyEnergyMode(mode) }
              )
            }
          }

          DashboardSectionId.MEDITATION -> {
            item(key = "dash_meditation") {
              DashboardMeditationCard(
                sessionsCount = todayMeditationSessions.size,
                onStartMeditation = {
                  viewModel.startMeditation(com.example.data.model.MeditationType.BREATHING, 10)
                  viewModel.navigateTo(MainTab.MEDITATION)
                },
                onOpenMeditation = { viewModel.navigateTo(MainTab.MEDITATION) }
              )
            }
          }

          DashboardSectionId.MUSIC -> {
            item(key = "dash_music") {
              DashboardMusicCard(
                musicPlayerState = musicPlayerState,
                onTogglePlayPause = { viewModel.toggleMusicPlayPause() },
                onNextSong = { viewModel.nextMusicSong() },
                onOpenMusic = { viewModel.navigateTo(MainTab.MUSIC) }
              )
            }
          }

          DashboardSectionId.DAILY_CHALLENGE -> {
            val challenge = dailyChallenge
            if (userSettings?.dailyChallengeEnabled != false && challenge != null) {
              item(key = "dash_daily_challenge") {
                DashboardDailyChallengeCard(
                  challenge = challenge,
                  onComplete = { viewModel.completeDailyChallenge(selectedDate) },
                  onSkip = { viewModel.skipDailyChallenge(selectedDate) }
                )
              }
            }
          }

          DashboardSectionId.NEXT_ALARM -> {
            item(key = "dash_next_alarm") {
              DashboardNextAlarmCard(
                isAlarmEnabled = isAlarmEnabled,
                wakeUpMinutes = wakeUpMinutes,
                onOpenAlarm = { viewModel.setTab(MainTab.ALARM_CENTER) }
              )
            }
          }

          DashboardSectionId.RECOVERY_DAY -> {
            if (isRecoveryDayActive) {
              item(key = "dash_recovery_day") {
                DashboardRecoveryDayCard(
                  isRecoveryDayActive = true,
                  onRestoreRoutine = { viewModel.restoreDefaultRoutine() }
                )
              }
            }
          }

          DashboardSectionId.WEEKLY_SUMMARY -> {
            item(key = "dash_weekly_summary") {
              DashboardWeeklySummaryCard(
                currentCycleDay = currentCycleDay,
                onOpenWeek = { viewModel.navigateTo(MainTab.WEEK) }
              )
            }
          }

          DashboardSectionId.TODAYS_ROUTINE -> {
            item(key = "dash_todays_routine_timeline") {
              ChronometerScheduleTimeline(
                tasks = tasks,
                nowMinutes = nowMinutes,
                isToday = isToday,
                onStatusChange = { task, status -> viewModel.updateTaskStatus(task, status) },
                onEditTask = { task -> editingTask = task },
                onDeleteTask = { task -> viewModel.deleteTask(task.id) },
                onAddTask = { isAddingTask = true }
              )
            }
          }
        }
      }

      if (extraTasks.isNotEmpty()) {
        item(key = "dash_extra_tasks") {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(16.dp))
              .background(DarkSurfaceElevated.copy(alpha = 0.6f))
              .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
              .clickable { showExtraTasksCollapsed = !showExtraTasksCollapsed }
              .padding(14.dp)
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "TODAY EXTRA TASKS (${extraTasks.size})",
                  color = Color(0xFFFFB74D),
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold
                )
                Icon(
                  imageVector = if (showExtraTasksCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                  contentDescription = null,
                  tint = Color(0xFFFFB74D)
                )
              }

              AnimatedVisibility(visible = !showExtraTasksCollapsed) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  extraTasks.forEach { extraTask ->
                    val isExtraFuture = if (isToday) extraTask.timeMinutes > nowMinutes else isFutureDate
                    TaskItemCard(
                      task = extraTask,
                      onStatusChange = { viewModel.updateTaskStatus(extraTask, it) },
                      onEdit = { editingTask = extraTask },
                      onDelete = { viewModel.deleteTask(extraTask.id) },
                      isCurrentTask = false,
                      isFutureTask = isExtraFuture
                    )
                  }
                }
              }
            }
          }
        }
      }
    }

    // 8. SUBTLE BOTTOM SWIPE QUICK ACCESS HANDLE
    Box(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 12.dp)
        .clip(RoundedCornerShape(22.dp))
        .background(DarkSurfaceElevated.copy(alpha = 0.95f))
        .border(1.dp, CyanNeon.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
        .clickable { showQuickAccessSheet = true }
        .padding(horizontal = 18.dp, vertical = 8.dp)
        .testTag("quick_access_handle")
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.KeyboardArrowUp,
          contentDescription = null,
          tint = CyanNeon,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Quick Access Hub • Journal, Reports & More",
          color = TextPrimary,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }

  // -----------------------------------------------------------
  // DIALOGS & BOTTOM SHEETS
  // -----------------------------------------------------------

  // 1. Modern Quick Access Bottom Sheet
  if (showQuickAccessSheet) {
    QuickAccessBottomSheet(
      onDismiss = { showQuickAccessSheet = false },
      onNavigateTab = { tab ->
        showQuickAccessSheet = false
        viewModel.setTab(tab)
      },
      onOpenInsights = {
        showQuickAccessSheet = false
        showInsightsDialog = true
      },
      onOpenAchievements = {
        showQuickAccessSheet = false
        showAchievementsDialog = true
      },
      onOpenHabits = {
        showQuickAccessSheet = false
        showGoalsDialog = true
      }
    )
  }

  // 2. Achievements Dialog
  if (showAchievementsDialog) {
    val totalDone = allTasks.count { it.status == TaskStatus.COMPLETE }
    val totalMedMins = allMeditationSessions.filter { it.isCompleted }.sumOf { it.completedSeconds } / 60
    AchievementsDialog(
      streakInfo = streakInfo,
      totalTasksCompleted = totalDone,
      totalMeditationMinutes = totalMedMins,
      onDismiss = { showAchievementsDialog = false }
    )
  }

  // 3. Personal Insights Dialog
  if (showInsightsDialog) {
    PersonalInsightsDialog(
      insights = personalInsights,
      onDismiss = { showInsightsDialog = false }
    )
  }

  // 4. Habits & Goals Bottom Sheet
  if (showGoalsDialog) {
    GoalBottomSheet(
      initialGoal = null,
      onSave = { id, title, desc, prog, deadline, isCompleted ->
        viewModel.saveGoal(
          id = id,
          title = title,
          description = desc,
          progress = prog,
          deadline = deadline,
          isCompleted = isCompleted
        )
        showGoalsDialog = false
      },
      onDismiss = { showGoalsDialog = false }
    )
  }

  // 5. Emergency Recovery Sheet
  if (showEmergencyRecoverySheet) {
    EmergencyRecoveryBottomSheet(
      onDismiss = { showEmergencyRecoverySheet = false },
      onApplyForTomorrow = {
        viewModel.applyEmergencyRecoveryMode(forTomorrow = true)
        showEmergencyRecoverySheet = false
      },
      onApplyForToday = {
        viewModel.applyEmergencyRecoveryMode(forTomorrow = false)
        showEmergencyRecoverySheet = false
      }
    )
  }

  // 6. Precision Time Block Add / Edit Dialog
  if (isAddingTask || editingTask != null) {
    ChronoTimeBlockDialog(
      initialTask = editingTask,
      initialTimeMinutes = nowMinutes,
      onDismiss = {
        editingTask = null
        isAddingTask = false
      },
      onSave = { name, timeMins, cat, notes ->
        viewModel.saveTask(
          id = editingTask?.id ?: 0L,
          name = name,
          timeMinutes = timeMins,
          category = cat,
          notes = notes,
          isExtra = false
        )
        editingTask = null
        isAddingTask = false
      }
    )
  }

  // 7. Personal Dashboard Builder Bottom Sheet
  if (showDashboardBuilderSheet) {
    DashboardBuilderBottomSheet(
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
      currentPresetKey = currentPresetKey,
      currentOrderKeys = currentOrderKeys,
      currentDisabledKeys = currentDisabledKeys,
      onApply = { preset, ordered, disabled ->
        viewModel.updateDashboardConfiguration(preset, ordered, disabled)
        showDashboardBuilderSheet = false
      },
      onResetDefault = {
        viewModel.resetDashboardToDefault()
        showDashboardBuilderSheet = false
      },
      onDismiss = { showDashboardBuilderSheet = false }
    )
  }
}
