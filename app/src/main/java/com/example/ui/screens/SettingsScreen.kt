package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.alarm.AlarmScheduler
import com.example.data.model.EnergyMode
import com.example.data.model.GoalEntity
import com.example.data.model.RoutineTemplateEntity
import com.example.ui.components.ExportFormatTab
import com.example.ui.components.ExportImportDialog
import com.example.ui.components.GoalBottomSheet
import com.example.ui.components.MathChallengeDialog
import com.example.ui.components.RoutineChangePreviewDialog
import com.example.ui.components.RoutineTemplateBottomSheet
import com.example.ui.components.TimePickerDialog12Hour
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.GlassGradient
import com.example.ui.theme.StatusComplete
import com.example.ui.theme.StatusMissed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletNeon
import com.example.ui.theme.ChronoSerifFamily
import com.example.ui.theme.DustyRose
import com.example.ui.theme.GoldBrass
import com.example.ui.theme.GoldHighlight
import com.example.ui.theme.IceBlue
import com.example.ui.theme.MutedPlum
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.SageGreen
import com.example.ui.theme.WarmMuted
import com.example.ui.theme.WarmOffWhite
import com.example.ui.theme.WarmParchment
import com.example.ui.viewmodel.LifeTrackerViewModel
import com.example.ui.viewmodel.MainTab
import com.example.util.TimeUtils
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
  viewModel: LifeTrackerViewModel,
  modifier: Modifier = Modifier
) {
  var activeCategory by remember { mutableStateOf<SettingsCategory?>(null) }

  // Handle hardware back button when inside a category
  BackHandler(enabled = activeCategory != null) {
    activeCategory = null
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBackground)
      .statusBarsPadding()
  ) {
    val selectedCategory = activeCategory
    if (selectedCategory == null) {
      // 1. MAIN CATEGORIES MENU
      SettingsCategoryList(
        onSelectCategory = { activeCategory = it },
        onBack = { viewModel.navigateBack() }
      )
    } else {
      // 2. DEDICATED CATEGORY SCREEN
      SettingsCategoryDetail(
        category = selectedCategory,
        viewModel = viewModel,
        onBack = { activeCategory = null }
      )
    }
  }
}

@Composable
private fun SettingsCategoryList(
  onSelectCategory: (SettingsCategory) -> Unit,
  onBack: () -> Unit
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Header
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.size(40.dp)
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
            text = "CONTROL CENTER • सेटिंग्स",
            color = CyanNeon,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
          Text(
            text = "App Settings & Control",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold
          )
        }
      }
    }

    // 8 Categories
    items(SettingsCategory.entries) { category ->
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(18.dp))
          .background(DarkSurfaceElevated)
          .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
          .clickable { onSelectCategory(category) }
          .padding(16.dp)
          .testTag("settings_cat_${category.name.lowercase()}")
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
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(category.iconColor.copy(alpha = 0.15f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = category.icon,
                contentDescription = category.title,
                tint = category.iconColor,
                modifier = Modifier.size(22.dp)
              )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
              Text(
                text = category.title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = category.description,
                color = TextMuted,
                fontSize = 11.sp,
                maxLines = 1
              )
            }
          }

          Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun SettingsCategoryDetail(
  category: SettingsCategory,
  viewModel: LifeTrackerViewModel,
  onBack: () -> Unit
) {
  val context = LocalContext.current
  val userSettings by viewModel.userSettings.collectAsState()

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Top Bar
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = onBack,
          modifier = Modifier.size(40.dp)
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back to Settings Menu",
            tint = TextPrimary
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = category.hindiTitle.uppercase(),
            color = category.iconColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
          Text(
            text = category.title,
            color = TextPrimary,
            fontSize = 19.sp,
            fontWeight = FontWeight.ExtraBold
          )
        }
      }
    }

    when (category) {
      SettingsCategory.ROUTINE_SCHEDULE -> {
        item {
          RoutineAndScheduleCategory(viewModel = viewModel)
        }
      }
      SettingsCategory.ALARM_NOTIFICATIONS -> {
        item {
          AlarmsAndNotificationsCategory(viewModel = viewModel, onOpenAlarmCenter = { viewModel.setTab(MainTab.ALARM_CENTER) })
        }
      }
      SettingsCategory.APPEARANCE -> {
        item {
          AppearanceCategory(viewModel = viewModel)
        }
      }
      SettingsCategory.FOCUS_RESTRICTION -> {
        item {
          FocusDisciplineCategory(viewModel = viewModel)
        }
      }
      SettingsCategory.MEDITATION -> {
        item {
          MeditationSettingsCategory(viewModel = viewModel)
        }
      }
      SettingsCategory.GOALS_SCORING -> {
        item {
          GoalsAndScoringCategory(viewModel = viewModel)
        }
      }
      SettingsCategory.DATA_BACKUP -> {
        item {
          DataBackupCategory(viewModel = viewModel)
        }
      }
      SettingsCategory.BLACK_SCREEN_MODE -> {
        item {
          com.example.ui.components.BlackScreenSettingsCategory(viewModel = viewModel)
        }
      }
      SettingsCategory.PERMISSIONS_STATUS -> {
        item {
          com.example.ui.components.PermissionsStatusSettingsCategory()
        }
      }
      SettingsCategory.GENERAL -> {
        item {
          GeneralCategory(viewModel = viewModel)
        }
      }
    }
  }
}

// -------------------------------------------------------------
// CATEGORY 1: ALARMS & NOTIFICATIONS
// -------------------------------------------------------------
@Composable
private fun AlarmsAndNotificationsCategory(
  viewModel: LifeTrackerViewModel,
  onOpenAlarmCenter: () -> Unit
) {
  val context = LocalContext.current
  val isAlarmEnabled by viewModel.isAlarmEnabled.collectAsState()
  val wakeUpMinutes by viewModel.wakeUpMinutes.collectAsState()
  val snoozeMinutes by viewModel.snoozeMinutes.collectAsState()
  val smartReminderMinutes by viewModel.smartReminderMinutes.collectAsState()
  val isVibrationEnabled by viewModel.isVibrationEnabled.collectAsState()

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    // Quick link to Dedicated Alarm Center
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(CyanNeon.copy(alpha = 0.12f))
        .border(1.dp, CyanNeon.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
        .clickable { onOpenAlarmCenter() }
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Open Full Alarm Center →",
            color = CyanNeon,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Upcoming alarms, tone tests, repeat days & health",
            color = TextPrimary,
            fontSize = 12.sp
          )
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = CyanNeon)
      }
    }

    // Master Switch
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("Master Alarm System", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          Text(if (isAlarmEnabled) "Alarms active" else "Alarms muted", color = if (isAlarmEnabled) StatusComplete else TextMuted, fontSize = 12.sp)
        }
        Switch(
          checked = isAlarmEnabled,
          onCheckedChange = { checked ->
            viewModel.toggleAlarm(context, checked)
            if (checked) AlarmScheduler.rescheduleAllTimetableAlarms(context)
            else {
              AlarmScheduler.cancelAlarm(context)
              AlarmScheduler.cancelAllTimetableAlarms(context)
            }
          }
        )
      }
    }

    // Smart Reminder
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Smart Pre-Reminder", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("Pre-routine notification: ${if (smartReminderMinutes > 0) "$smartReminderMinutes min before" else "Off"}", color = TextMuted, fontSize = 12.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf(0 to "Off", 5 to "5m", 10 to "10m", 15 to "15m").forEach { (mins, label) ->
            val isSelected = smartReminderMinutes == mins
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) CyanNeon.copy(alpha = 0.15f) else DarkSurface)
                .border(1.dp, if (isSelected) CyanNeon else DarkSurfaceBorder, RoundedCornerShape(10.dp))
                .clickable {
                  viewModel.updateSmartReminderMinutes(mins)
                  AlarmScheduler.rescheduleAllTimetableAlarms(context)
                }
                .padding(vertical = 8.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(label, color = if (isSelected) CyanNeon else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // Vibration Alert
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("Vibration Alert", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          Text("Pulsing vibration for alarms", color = TextMuted, fontSize = 12.sp)
        }
        Switch(
          checked = isVibrationEnabled,
          onCheckedChange = { viewModel.updateVibrationEnabled(it) }
        )
      }
    }
  }
}

// -------------------------------------------------------------
// CATEGORY 2: ROUTINE & SCHEDULE
// -------------------------------------------------------------
@Composable
private fun RoutineAndScheduleCategory(viewModel: LifeTrackerViewModel) {
  val context = LocalContext.current
  val routineTemplates by viewModel.routineTemplates.collectAsState()
  val anchorDate by viewModel.anchorDate.collectAsState()
  var editingTemplate by remember { mutableStateOf<RoutineTemplateEntity?>(null) }
  var isAddingTemplate by remember { mutableStateOf(false) }

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    // 7-Day Anchor Date
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("7-Day Cycle Anchor Date", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("Current Anchor: $anchorDate", color = CyanNeon, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text("The starting date that defines continuous 7-day cycles", color = TextMuted, fontSize = 11.sp)
      }
    }

    // Routine Templates Management
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Master Routine Activities (${routineTemplates.size})", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
      Button(
        onClick = { isAddingTemplate = true },
        colors = ButtonDefaults.buttonColors(containerColor = CyanNeon.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Add", color = CyanNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }
    }

    routineTemplates.forEach { template ->
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(DarkSurfaceElevated)
          .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(14.dp))
          .padding(12.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(template.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("${TimeUtils.minutesTo12Hour(template.timeMinutes)} • ${template.category}", color = TextMuted, fontSize = 11.sp)
          }

          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = { editingTemplate = template }, modifier = Modifier.size(32.dp)) {
              Icon(Icons.Default.Edit, contentDescription = "Edit", tint = CyanNeon, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = { viewModel.deleteRoutineTemplate(context, template.id) }, modifier = Modifier.size(32.dp)) {
              Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = StatusMissed, modifier = Modifier.size(16.dp))
            }
          }
        }
      }
    }
  }

  if (isAddingTemplate || editingTemplate != null) {
    RoutineTemplateBottomSheet(
      initialTemplate = editingTemplate,
      onSave = { id, name, mins, cat, notes, days, active ->
        viewModel.saveRoutineTemplate(
          id = id,
          name = name,
          timeMinutes = mins,
          category = cat,
          notes = notes,
          daysMask = days,
          isActive = active
        )
        editingTemplate = null
        isAddingTemplate = false
      },
      onDismiss = {
        editingTemplate = null
        isAddingTemplate = false
      }
    )
  }
}

// -------------------------------------------------------------
// CATEGORY 3: MEDITATION
// -------------------------------------------------------------
@Composable
private fun MeditationSettingsCategory(viewModel: LifeTrackerViewModel) {
  val chimeEnabled by viewModel.meditationChimeEnabled.collectAsState()
  val voiceLang by viewModel.meditationVoiceLanguage.collectAsState()

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Guided Voice Language (वाणी भाषा)", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          listOf("HI" to "हिंदी (Hindi)", "EN" to "English").forEach { (code, label) ->
            val isSelected = voiceLang == code
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) VioletNeon.copy(alpha = 0.18f) else DarkSurface)
                .border(1.dp, if (isSelected) VioletNeon else DarkSurfaceBorder, RoundedCornerShape(10.dp))
                .clickable { viewModel.updateMeditationSettings(chimeEnabled, code) }
                .padding(vertical = 10.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(label, color = if (isSelected) VioletNeon else TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("Tibetan Singing Bowl & Bells", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          Text("Harmonic start chime & ending bells", color = TextMuted, fontSize = 12.sp)
        }
        Switch(
          checked = chimeEnabled,
          onCheckedChange = { viewModel.updateMeditationSettings(it, voiceLang) }
        )
      }
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Offline Audio Support", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("On-device synthesized Tibetan singing bowl and local Android TTS. 100% offline without internet connection.", color = TextMuted, fontSize = 12.sp)
      }
    }
  }
}

// -------------------------------------------------------------
// CATEGORY 4: FOCUS & DISCIPLINE
// -------------------------------------------------------------
@Composable
private fun FocusDisciplineCategory(viewModel: LifeTrackerViewModel) {
  val isFocusModeActive by viewModel.isFocusModeActive.collectAsState()
  val isFocusScheduleEnabled by viewModel.isFocusScheduleEnabled.collectAsState()
  val focusStartTimeMinutes by viewModel.focusStartTimeMinutes.collectAsState()
  val focusEndTimeMinutes by viewModel.focusEndTimeMinutes.collectAsState()
  val enableRecoveryMode by viewModel.enableRecoveryMode.collectAsState()

  var showStartTimePicker by remember { mutableStateOf(false) }
  var showEndTimePicker by remember { mutableStateOf(false) }
  var showMathChallengeToDisable by remember { mutableStateOf(false) }

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    // 1. LIVE FOCUS STATUS & MANUAL TOGGLE CARD
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, if (isFocusModeActive) GoldBrass.copy(alpha = 0.6f) else DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isFocusModeActive) GoldBrass else WarmMuted)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (isFocusModeActive) "FOCUS MODE ACTIVE • सक्रिय" else "FOCUS MODE INACTIVE • निष्क्रिय",
              color = if (isFocusModeActive) GoldBrass else WarmMuted,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.8.sp
            )
          }

          if (isFocusModeActive) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(GoldBrass.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                text = if (isFocusScheduleEnabled) "Scheduled" else "Manual",
                color = GoldHighlight,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Manual Focus Mode", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
              text = if (isFocusModeActive) "सक्रिय सत्र को बंद करने के लिए 3 गणितीय प्रश्नों का सही उत्तर आवश्यक है।" else "तत्काल डिजिटल अनुशासन सत्र चालू करें।",
              color = TextMuted,
              fontSize = 12.sp,
              lineHeight = 16.sp
            )
          }
          Switch(
            checked = isFocusModeActive,
            onCheckedChange = { shouldEnable ->
              if (shouldEnable) {
                viewModel.setFocusModeActive(true)
              } else {
                // If active, require Math Challenge unlock!
                showMathChallengeToDisable = true
              }
            }
          )
        }
      }
    }

    // 2. AUTOMATIC SCHEDULE CARD
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Automatic Schedule", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
              text = "निर्धारित समय पर फोकस मोड स्वतः चालू व बंद हो। रीबूट के बाद भी प्रभावी।",
              color = TextMuted,
              fontSize = 12.sp
            )
          }
          Switch(
            checked = isFocusScheduleEnabled,
            onCheckedChange = { enabled ->
              viewModel.updateFocusSchedule(enabled, focusStartTimeMinutes, focusEndTimeMinutes)
            }
          )
        }

        AnimatedVisibility(visible = isFocusScheduleEnabled) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            androidx.compose.material3.HorizontalDivider(color = DarkSurfaceBorder.copy(alpha = 0.5f), thickness = 0.8.dp)

            // Start Time Row
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ObsidianCard)
                .clickable { showStartTimePicker = true }
                .padding(horizontal = 14.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("Start Time (आरंभ समय)", color = WarmMuted, fontSize = 11.sp)
                Text(
                  text = TimeUtils.minutesTo12Hour(focusStartTimeMinutes),
                  color = WarmOffWhite,
                  fontFamily = ChronoSerifFamily,
                  fontSize = 18.sp,
                  fontWeight = FontWeight.Bold
                )
              }
              Text("बदलें →", color = GoldBrass, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            // End Time Row
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ObsidianCard)
                .clickable { showEndTimePicker = true }
                .padding(horizontal = 14.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Text("End Time (समाप्ति समय)", color = WarmMuted, fontSize = 11.sp)
                Text(
                  text = TimeUtils.minutesTo12Hour(focusEndTimeMinutes),
                  color = WarmOffWhite,
                  fontFamily = ChronoSerifFamily,
                  fontSize = 18.sp,
                  fontWeight = FontWeight.Bold
                )
              }
              Text("बदलें →", color = GoldBrass, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            // Overnight helper note
            val startFormatted = TimeUtils.minutesTo12Hour(focusStartTimeMinutes)
            val endFormatted = TimeUtils.minutesTo12Hour(focusEndTimeMinutes)
            val isOvernight = focusStartTimeMinutes > focusEndTimeMinutes
            Text(
              text = if (isOvernight) "🌙 रात्रि-कालीन शेड्यूल: $startFormatted से अगली सुबह $endFormatted तक।" else "☀️ दिन-कालीन शेड्यूल: $startFormatted से $endFormatted तक।",
              color = GoldHighlight,
              fontSize = 11.sp
            )
          }
        }
      }
    }

    // 3. EMERGENCY RECOVERY MODE CARD (Existing)
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Emergency Recovery Mode", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          Text("Automatically reschedules missed tasks into upcoming free blocks without guilt.", color = TextMuted, fontSize = 12.sp)
        }
        Switch(
          checked = enableRecoveryMode,
          onCheckedChange = { viewModel.updateFeatureToggle("recoveryMode", it) }
        )
      }
    }

    // 4. DISCIPLINE & MATH CHALLENGE OVERVIEW CARD
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("10th-Class Math Challenge Unlock", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(
          text = "फोकस मोड से समय पूर्व बाहर निकलने के लिए 3 अलग-अलग 10वीं कक्षा स्तर के गणित प्रश्नों (द्विघात समीकरण, समान्तर श्रेढ़ी, त्रिकोणमिति, आदि) का सही उत्तर देना अनिवार्य है ताकि अनपेक्षित व्याकुलता रोकी जा सके।",
          color = TextMuted,
          fontSize = 12.sp,
          lineHeight = 17.sp
        )
      }
    }
  }

  // DIALOGS:
  // Start Time Picker
  if (showStartTimePicker) {
    TimePickerDialog12Hour(
      initialMinutes = focusStartTimeMinutes,
      onDismiss = { showStartTimePicker = false },
      onConfirm = { newMins ->
        showStartTimePicker = false
        viewModel.updateFocusSchedule(isFocusScheduleEnabled, newMins, focusEndTimeMinutes)
      }
    )
  }

  // End Time Picker
  if (showEndTimePicker) {
    TimePickerDialog12Hour(
      initialMinutes = focusEndTimeMinutes,
      onDismiss = { showEndTimePicker = false },
      onConfirm = { newMins ->
        showEndTimePicker = false
        viewModel.updateFocusSchedule(isFocusScheduleEnabled, focusStartTimeMinutes, newMins)
      }
    )
  }

  // Math Challenge Dialog on manual disable attempt
  if (showMathChallengeToDisable) {
    MathChallengeDialog(
      onUnlockSuccess = {
        showMathChallengeToDisable = false
        viewModel.unlockFocusMode()
      },
      onDismiss = { showMathChallengeToDisable = false }
    )
  }
}

// -------------------------------------------------------------
// CATEGORY 3: APPEARANCE & THEME (Luxury Chronograph Instrument)
// -------------------------------------------------------------
@Composable
private fun AppearanceCategory(viewModel: LifeTrackerViewModel) {
  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(ObsidianCard)
        .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(GoldBrass))
          Spacer(modifier = Modifier.width(6.dp))
          Text("CHRONOMETER PRECISION INSTRUMENT", color = GoldBrass, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.0.sp)
        }
        Text("Luxury Watch Aesthetic", color = WarmOffWhite, fontFamily = ChronoSerifFamily, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(
          text = "Inspired by luxury mechanical chronographs. Crafted with deep warm obsidian charcoal, satin horological brass accents, warm off-white typography, and precision 60-bezel index marks.",
          color = WarmParchment,
          fontSize = 12.sp,
          lineHeight = 18.sp
        )

        // Palette Swatches
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf(
            Pair("Obsidian", Color(0xFF14120E)),
            Pair("Brass", GoldBrass),
            Pair("Champagne", GoldHighlight),
            Pair("Sage", SageGreen),
            Pair("Plum", MutedPlum),
            Pair("Ice Blue", IceBlue),
            Pair("Off-White", WarmOffWhite)
          ).forEach { (label, col) ->
            Box(
              modifier = Modifier
                .weight(1f)
                .height(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(col)
                .border(0.6.dp, ObsidianBorder, RoundedCornerShape(8.dp)),
              contentAlignment = Alignment.Center
            ) {
              Text(label.take(1), color = if (col == WarmOffWhite || col == GoldHighlight) Color(0xFF14120E) else Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(ObsidianCard)
        .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Typography & Dial Scaling", color = WarmOffWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("Serif numerals for major metrics and adherence dials; geometric sans-serif for labels, time ranges, and timetable blocks.", color = WarmMuted, fontSize = 12.sp)
      }
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(ObsidianCard)
        .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Time Display Format", color = WarmOffWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("12-Hour AM/PM with Indian Time Notations", color = GoldBrass, fontFamily = ChronoSerifFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
      }
    }
  }
}

// -------------------------------------------------------------
// CATEGORY 6: GOALS & SCORING
// -------------------------------------------------------------
@Composable
private fun GoalsAndScoringCategory(viewModel: LifeTrackerViewModel) {
  val streakInfo by viewModel.streakInfo.collectAsState()
  val goals by viewModel.allGoals.collectAsState()
  var showNewGoalSheet by remember { mutableStateOf(false) }

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    // 1. Scoring System Overview Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(ObsidianCard)
        .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(GoldBrass))
          Spacer(modifier = Modifier.width(6.dp))
          Text("DAILY ADHERENCE BENCHMARK", color = GoldBrass, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.0.sp)
        }
        Text("Daily Score Calibration: 40.0 pts", color = WarmOffWhite, fontFamily = ChronoSerifFamily, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(
          text = "• Completed Task: +2.5 pts (Full Adherence ✓)\n• Half / Partial Task: +1.25 pts (~ Half)\n• Missed Task: 0.0 pts (✕ Pending)\nTarget benchmark: 32.0 / 40.0 pts (80% Adherence).",
          color = WarmParchment,
          fontSize = 12.sp,
          lineHeight = 18.sp
        )
      }
    }

    // 2. Streak Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(ObsidianCard)
        .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("Consistency Streak", color = WarmOffWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          Text("Current: ${streakInfo.currentStreak} Days • Best: ${streakInfo.longestStreak} Days", color = WarmMuted, fontSize = 12.sp)
        }
        Text(
          text = "${streakInfo.currentStreak}d 🔥",
          color = GoldBrass,
          fontFamily = ChronoSerifFamily,
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }

    // 3. Active Goals List
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "ACTIVE GOALS (${goals.size})",
        color = GoldBrass,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp
      )
      Button(
        onClick = { showNewGoalSheet = true },
        colors = ButtonDefaults.buttonColors(containerColor = GoldBrass),
        shape = RoundedCornerShape(10.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = ObsidianElevated, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("+ New Goal", color = ObsidianElevated, fontSize = 11.sp, fontWeight = FontWeight.Bold)
      }
    }

    if (goals.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(ObsidianCard)
          .border(0.8.dp, ObsidianBorder, RoundedCornerShape(14.dp))
          .padding(20.dp),
        contentAlignment = Alignment.Center
      ) {
        Text("No active goals yet. Tap '+ New Goal' to begin tracking.", color = WarmMuted, fontSize = 12.sp)
      }
    } else {
      goals.forEach { goal ->
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianCard)
            .border(0.8.dp, ObsidianBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(goal.title, color = WarmOffWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
              Text("${goal.progress}%", color = GoldBrass, fontFamily = ChronoSerifFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            if (goal.description.isNotBlank()) {
              Text(goal.description, color = WarmMuted, fontSize = 11.sp)
            }
            LinearProgressIndicator(
              progress = { (goal.progress / 100f).coerceIn(0f, 1f) },
              modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
              color = if (goal.isCompleted) SageGreen else GoldBrass,
              trackColor = ObsidianElevated
            )
          }
        }
      }
    }
  }

  if (showNewGoalSheet) {
    GoalBottomSheet(
      initialGoal = null,
      onSave = { id, title, desc, prog, deadline, isCompleted ->
        viewModel.saveGoal(id, title, desc, prog, deadline, isCompleted)
        showNewGoalSheet = false
      },
      onDismiss = { showNewGoalSheet = false }
    )
  }
}

// -------------------------------------------------------------
// CATEGORY 8: GENERAL SETTINGS
// -------------------------------------------------------------
@Composable
private fun GeneralCategory(viewModel: LifeTrackerViewModel) {
  val context = LocalContext.current
  val userSettings by viewModel.userSettings.collectAsState()
  val userName by viewModel.userName.collectAsState()
  var nameInput by remember(userName) { mutableStateOf(userName) }
  var isEditingName by remember { mutableStateOf(false) }
  val dailyEnergyMode by viewModel.dailyEnergyMode.collectAsState()

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    // 1. Profile Name Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(ObsidianCard)
        .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text("User Profile & Greeting", color = WarmOffWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text("Displayed in morning status and voice audio", color = WarmMuted, fontSize = 11.sp)
          }
          IconButton(onClick = { isEditingName = !isEditingName }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Edit, contentDescription = "Edit Name", tint = GoldBrass, modifier = Modifier.size(16.dp))
          }
        }

        if (isEditingName) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            OutlinedTextField(
              value = nameInput,
              onValueChange = { nameInput = it },
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldBrass,
                unfocusedBorderColor = ObsidianBorder,
                focusedTextColor = WarmOffWhite,
                unfocusedTextColor = WarmOffWhite,
                focusedContainerColor = ObsidianElevated,
                unfocusedContainerColor = ObsidianElevated
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f)
            )
            Button(
              onClick = {
                if (nameInput.isNotBlank()) {
                  viewModel.updateUserName(nameInput.trim())
                  isEditingName = false
                  Toast.makeText(context, "नाम अपडेट किया गया: $nameInput", Toast.LENGTH_SHORT).show()
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = GoldBrass),
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("Save", color = ObsidianElevated, fontWeight = FontWeight.Bold)
            }
          }
        } else {
          Text(
            text = userName,
            color = GoldHighlight,
            fontFamily = ChronoSerifFamily,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    // 2. 7-Day Cycle Anchor Date
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(ObsidianCard)
        .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text("Cycle Anchor Date (Day 1)", color = WarmOffWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          Text("Determines 7-day cyclical routine repetition", color = WarmMuted, fontSize = 11.sp)
          Spacer(modifier = Modifier.height(2.dp))
          Text(userSettings?.anchorDate ?: "2026-09-24", color = GoldBrass, fontFamily = ChronoSerifFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
      }
    }

    // 3. Energy Mode Quick Selector
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(ObsidianCard)
        .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Daily Energy Mode", color = WarmOffWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          EnergyMode.entries.forEach { mode ->
            val isSelected = dailyEnergyMode == mode
            val color = when (mode) {
              EnergyMode.LOW -> IceBlue
              EnergyMode.NORMAL -> GoldBrass
              EnergyMode.HIGH -> GoldHighlight
            }
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSelected) color.copy(alpha = 0.2f) else ObsidianElevated)
                .border(1.dp, if (isSelected) color else ObsidianBorder, RoundedCornerShape(12.dp))
                .clickable { viewModel.setDailyEnergyMode(mode) }
                .padding(vertical = 10.dp),
              contentAlignment = Alignment.Center
            ) {
              Text("${mode.emoji} ${mode.label}", color = if (isSelected) color else WarmParchment, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // 4. Persistence & Architecture
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(ObsidianCard)
        .border(1.dp, ObsidianBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("System Persistence & Alarms", color = WarmOffWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("Precision alarms automatically reschedule upon device reboot. All data is persisted 100% offline in local Room SQLite database.", color = WarmMuted, fontSize = 12.sp, lineHeight = 17.sp)
      }
    }
  }
}

// -------------------------------------------------------------
// CATEGORY 6: REPORTS & TRACKING
// -------------------------------------------------------------
@Composable
private fun ReportsTrackingCategory(viewModel: LifeTrackerViewModel) {
  val enableMorningBrief by viewModel.enableMorningBrief.collectAsState()
  val enableLifeTimeline by viewModel.enableLifeTimeline.collectAsState()
  val enablePersonalInsights by viewModel.enablePersonalInsights.collectAsState()

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Smart Morning Brief", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          Text("Daily morning motivation card and priority heads-up", color = TextMuted, fontSize = 12.sp)
        }
        Switch(checked = enableMorningBrief, onCheckedChange = { viewModel.updateFeatureToggle("morningBrief", it) })
      }
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Life Timeline Tracking", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          Text("Record chronological daily snapshots & milestones", color = TextMuted, fontSize = 12.sp)
        }
        Switch(checked = enableLifeTimeline, onCheckedChange = { viewModel.updateFeatureToggle("lifeTimeline", it) })
      }
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Personal Insights Analytics", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          Text("Calculate peak focus hours and habit completion trends", color = TextMuted, fontSize = 12.sp)
        }
        Switch(checked = enablePersonalInsights, onCheckedChange = { viewModel.updateFeatureToggle("personalInsights", it) })
      }
    }
  }
}

// -------------------------------------------------------------
// CATEGORY 7: BACKUP & DATA
// -------------------------------------------------------------
@Composable
private fun DataBackupCategory(viewModel: LifeTrackerViewModel) {
  var showExportDialog by remember { mutableStateOf(false) }

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Export / Backup & Restore", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("Backup your entire routine, tasks, journal, and meditation records to JSON or copy as plain text.", color = TextMuted, fontSize = 12.sp)

        Button(
          onClick = { showExportDialog = true },
          colors = ButtonDefaults.buttonColors(containerColor = CyanNeon.copy(alpha = 0.2f)),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("Open Backup & Restore Center", color = CyanNeon, fontWeight = FontWeight.Bold)
        }
      }
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("100% Local Privacy", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("Your routine data is stored completely inside your Android device using SQLite Room database without third-party tracking.", color = TextMuted, fontSize = 12.sp)
      }
    }
  }

  if (showExportDialog) {
    ExportImportDialog(
      onDismiss = { showExportDialog = false },
      onGenerateContent = { tab ->
        when (tab) {
          ExportFormatTab.JSON -> viewModel.exportJson()
          ExportFormatTab.CSV -> viewModel.exportCsv()
          ExportFormatTab.MARKDOWN -> viewModel.exportMarkdown()
          ExportFormatTab.TXT -> viewModel.exportPlainText()
          ExportFormatTab.AI_PROMPT -> viewModel.exportAiAnalysis()
          else -> ""
        }
      },
      onImportJson = { json, cb -> viewModel.importJson(json, cb) }
    )
  }
}

// -------------------------------------------------------------
// CATEGORY 8: USER PROFILE
// -------------------------------------------------------------
@Composable
private fun ProfileCategory(viewModel: LifeTrackerViewModel) {
  val context = LocalContext.current
  val currentName by viewModel.userName.collectAsState()
  var nameInput by remember(currentName) { mutableStateOf(currentName) }

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Your Name (तुम्हारा नाम)", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text("This name is used in personalized voice announcements during meditation completion (\"मोहित, तुम्हारा मेडिटेशन...\") and morning greeting.", color = TextMuted, fontSize = 12.sp)

        OutlinedTextField(
          value = nameInput,
          onValueChange = { nameInput = it },
          label = { Text("Profile Name") },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyanNeon,
            unfocusedBorderColor = DarkSurfaceBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary
          ),
          modifier = Modifier.fillMaxWidth()
        )

        Button(
          onClick = {
            viewModel.updateUserName(nameInput)
            Toast.makeText(context, "नाम सफलतापूर्वक सहेजा गया!", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("Save Name", color = DarkBackground, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

// -------------------------------------------------------------
// CATEGORY 9: ENERGY MODE & CHALLENGE
// -------------------------------------------------------------
@Composable
private fun EnergyAndChallengeCategory(viewModel: LifeTrackerViewModel) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val dailyEnergyMode by viewModel.dailyEnergyMode.collectAsState()
  val userSettings by viewModel.userSettings.collectAsState()
  val isRecoveryActive by viewModel.isRecoveryDayActive.collectAsState()

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    // 1. Energy Mode Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("⚡ Daily Energy Mode • ऊर्जा मोड", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(
          text = "दिन के लिए अपना Energy Mode चुनें। आवश्यक रूटीन हमेशा बना रहेगा, पर Low Energy में वैकल्पिक कार्य हल्के कर दिए जाएंगे। यह अगले दिन स्वतः सामान्य हो जाता है।",
          color = TextMuted,
          fontSize = 12.sp,
          lineHeight = 18.sp
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          EnergyMode.entries.forEach { mode ->
            val isSelected = dailyEnergyMode == mode
            val modeColor = when (mode) {
              EnergyMode.LOW -> Color(0xFF4DD0E1)
              EnergyMode.NORMAL -> CyanNeon
              EnergyMode.HIGH -> Color(0xFFFFB300)
            }
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isSelected) modeColor.copy(alpha = 0.2f) else DarkSurface)
                .border(1.dp, if (isSelected) modeColor else DarkSurfaceBorder, RoundedCornerShape(12.dp))
                .clickable {
                  viewModel.setDailyEnergyMode(mode)
                  Toast.makeText(context, "${mode.label} ऊर्जा मोड सेट किया गया", Toast.LENGTH_SHORT).show()
                }
                .padding(vertical = 10.dp),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(mode.emoji, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(3.dp))
                Text(mode.label, color = if (isSelected) modeColor else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    }

    // 2. Recovery Day Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("🌿 Recovery Day • रिकवरी डे", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          if (isRecoveryActive) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF4DD0E1).copy(alpha = 0.2f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Text("Active", color = Color(0xFF4DD0E1), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
          }
        }
        Text(
          text = "जब दिन का रूटीन किसी कारण छूट जाए, तो तनाव लेने के बजाय Recovery Day शुरू करें। यह आवश्यक कार्यों को प्राथमिकता देकर हल्का शेड्यूल बनाता है और मूल समय सारणी को स्थाई रूप से नहीं बदलता।",
          color = TextMuted,
          fontSize = 12.sp,
          lineHeight = 18.sp
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Button(
            onClick = {
              viewModel.activateRecoveryDay()
              Toast.makeText(context, "रिकवरी डे सक्रिय किया गया!", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text("Start Recovery Day", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }

          if (isRecoveryActive) {
            Button(
              onClick = {
                viewModel.restoreDefaultRoutine()
                Toast.makeText(context, "मूल समय सारणी पुनः लागू की गई", Toast.LENGTH_SHORT).show()
              },
              colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f)
            ) {
              Text("Restore Default", color = CyanNeon, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
          }
        }
      }
    }

    // 3. Daily Challenge Toggle Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("🌱 Daily Challenge System", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(
              text = "दैनिक सकारात्मक चुनौती (पठन, ध्यान, सीखना, व्यायाम आदि)",
              color = TextMuted,
              fontSize = 11.sp
            )
          }
          Switch(
            checked = userSettings?.dailyChallengeEnabled ?: true,
            onCheckedChange = { enabled ->
              coroutineScope.launch {
                viewModel.repository.updateDailyChallengeEnabled(enabled)
              }
            }
          )
        }
      }
    }
  }
}

// -------------------------------------------------------------
// CATEGORY 10: MUSIC SYSTEM
// -------------------------------------------------------------
@Composable
private fun MusicSystemCategory(viewModel: LifeTrackerViewModel) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val allSongs by viewModel.allSongs.collectAsState()
  val allPlaylists by viewModel.allPlaylists.collectAsState()
  val playlistRules by viewModel.playlistRules.collectAsState()
  val userSettings by viewModel.userSettings.collectAsState()

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    // 1. Library Status Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("🎵 Music Library & Playlists", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(
          text = "Life Tracker में पूरा Music System उपलब्ध है। अपने फोन के गानों को लाइब्रेरी में स्कैन करें, प्लेलिस्ट बनाएं, और समय व मूड के हिसाब से ऑटोमैटिक रूटीन से जोड़ें।",
          color = TextMuted,
          fontSize = 12.sp,
          lineHeight = 18.sp
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column {
            Text("${allSongs.size}", color = CyanNeon, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text("कुल ऑडियो", color = TextMuted, fontSize = 11.sp)
          }
          Column {
            Text("${allPlaylists.size}", color = VioletNeon, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text("प्लेलिस्ट्स", color = TextMuted, fontSize = 11.sp)
          }
          Column {
            Text("${playlistRules.size}", color = Color(0xFFFFB300), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
            Text("मूड/टाइम रूल्स", color = TextMuted, fontSize = 11.sp)
          }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Button(
            onClick = {
              viewModel.scanDeviceMusic()
              Toast.makeText(context, "ऑडियो स्कैन किया जा रहा है...", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text("Scan Audio Files", color = CyanNeon, fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }

          Button(
            onClick = { viewModel.navigateTo(MainTab.MUSIC) },
            colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text("Open Music Player", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
        }
      }
    }

    // 2. Routine Auto-Sync Card
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Connect Routine with Music", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          Text(
            text = "दिनचर्या के समय अनुसार (सुबह, वर्कआउट, रात) परिभाषित मूड के गाने चुनें",
            color = TextMuted,
            fontSize = 11.sp
          )
        }
        Switch(
          checked = userSettings?.musicAutoRoutineEnabled ?: true,
          onCheckedChange = { enabled ->
            coroutineScope.launch {
              viewModel.repository.updateMusicAutoRoutineEnabled(enabled)
            }
          }
        )
      }
    }
  }
}

// -------------------------------------------------------------
// CATEGORY 11: HOME SCREEN WIDGET
// -------------------------------------------------------------
@Composable
private fun HomeWidgetCategory(viewModel: LifeTrackerViewModel) {
  val context = LocalContext.current

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(18.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(18.dp))
        .padding(16.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("📱 Android Home Screen Widgets", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(
          text = "Life Tracker में 3 प्रकार के नेटिव Android होम स्क्रीन विजेट उपलब्ध हैं। अपने फोन की होम स्क्रीन पर खाली जगह को लॉन्ग प्रेस करके 'Widgets' चुनें और Life Tracker जोड़ें:",
          color = TextMuted,
          fontSize = 12.sp,
          lineHeight = 18.sp
        )

        // Small Widget Info
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .padding(12.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("1. Small Widget (छोटा विजेट)", color = CyanNeon, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("• अगला कार्य (Next Activity)\n• आरंभ समय एवं समय शेष (Remaining Time)", color = TextSecondary, fontSize = 11.sp)
          }
        }

        // Medium Widget Info
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .padding(12.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("2. Medium Widget (मध्यम विजेट)", color = VioletNeon, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("• आज की प्रगति % एवं टास्क काउंटर\n• अगला कार्य एवं 1-टैप 'Complete' बटन", color = TextSecondary, fontSize = 11.sp)
          }
        }

        // Large Widget Info
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .padding(12.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("3. Large Widget (विस्तृत विजेट)", color = Color(0xFFFFB300), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("• आज की समय सारणी की लिस्ट\n• क्विक एक्शन: टास्क पूर्ण करें, ध्यान साधना शुरू करें, ऐप खोलें", color = TextSecondary, fontSize = 11.sp)
          }
        }

        Button(
          onClick = {
            com.example.widget.LifeTrackerWidgetProvider.updateAllWidgets(context)
            Toast.makeText(context, "विजेट्स को सफलतापूर्वक रीफ्रेश किया गया!", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = CyanNeon),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Refresh All Widgets Now", color = DarkBackground, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

enum class SettingsCategory(
  val title: String,
  val hindiTitle: String,
  val description: String,
  val icon: ImageVector,
  val iconColor: Color
) {
  ROUTINE_SCHEDULE(
    title = "Routine & Schedule",
    hindiTitle = "दिनचर्या एवं समय सारणी",
    description = "Timetable blocks, custom routine, points & 7-day cycle",
    icon = Icons.Default.Schedule,
    iconColor = GoldBrass
  ),
  ALARM_NOTIFICATIONS(
    title = "Alarm & Notifications",
    hindiTitle = "अलार्म एवं सूचनाएं",
    description = "Exact wake-up alarm, activity start alerts, sounds & vibration",
    icon = Icons.Default.Alarm,
    iconColor = GoldHighlight
  ),
  APPEARANCE(
    title = "Appearance",
    hindiTitle = "थीम एवं प्रदर्शन",
    description = "Obsidian charcoal, brass/gold chronometer accents & typography",
    icon = Icons.Default.Palette,
    iconColor = GoldBrass
  ),
  FOCUS_RESTRICTION(
    title = "Focus / Phone Restriction",
    hindiTitle = "फोकस एवं फोन नियंत्रण",
    description = "Focus Lock, Short Content limits & digital discipline",
    icon = Icons.Default.Psychology,
    iconColor = IceBlue
  ),
  MEDITATION(
    title = "Meditation",
    hindiTitle = "ध्यान साधना",
    description = "Ending voice, chime bell & sanctuary preferences",
    icon = Icons.Default.Spa,
    iconColor = SageGreen
  ),
  GOALS_SCORING(
    title = "Goals & Scoring",
    hindiTitle = "लक्ष्य एवं स्कोरिंग",
    description = "40 pts daily score targets, streak benchmarks & habits",
    icon = Icons.Default.Insights,
    iconColor = MutedPlum
  ),
  DATA_BACKUP(
    title = "Data & Backup",
    hindiTitle = "डेटा बैकअप एवं रिस्टोर",
    description = "JSON/CSV export, import, auto-backup & database reset",
    icon = Icons.Default.Storage,
    iconColor = DustyRose
  ),
  BLACK_SCREEN_MODE(
    title = "Black Screen Mode",
    hindiTitle = "सिस्टम-वाइड ब्लैक स्क्रीन मोड",
    description = "OLED Pure Black, Floating Dot, 3-Finger Gesture, Screen Off look",
    icon = Icons.Default.VisibilityOff,
    iconColor = CyanNeon
  ),
  PERMISSIONS_STATUS(
    title = "Permissions & System Status",
    hindiTitle = "अनुमतियां एवं सिस्टम स्थिति",
    description = "Exact alarm, overlays, accessibility, battery & crash diagnostics",
    icon = Icons.Default.Healing,
    iconColor = IceBlue
  ),
  GENERAL(
    title = "General",
    hindiTitle = "सामान्य सेटिंग्स",
    description = "User profile, name, energy mode, audio rules & about",
    icon = Icons.Default.Person,
    iconColor = WarmParchment
  )
}
