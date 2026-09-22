package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alarm.AlarmActivity
import com.example.alarm.AlarmAudioPlayer
import com.example.alarm.AlarmScheduler
import com.example.data.model.GoalEntity
import com.example.data.model.RoutineTemplateEntity
import com.example.ui.components.ExportFormatTab
import com.example.ui.components.ExportImportDialog
import com.example.ui.components.GoalBottomSheet
import com.example.ui.components.RoutineTemplateBottomSheet
import com.example.ui.components.TimePickerDialog12Hour
import com.example.ui.theme.CardBorderGradient
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
import com.example.ui.viewmodel.LifeTrackerViewModel
import com.example.util.TimeUtils
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
  viewModel: LifeTrackerViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val scope = rememberCoroutineScope()

  val routineTemplates by viewModel.routineTemplates.collectAsState()
  val goals by viewModel.allGoals.collectAsState()
  val anchorDate by viewModel.anchorDate.collectAsState()
  val wakeUpMinutes by viewModel.wakeUpMinutes.collectAsState()
  val isAlarmEnabled by viewModel.isAlarmEnabled.collectAsState()
  val alarmSoundType by viewModel.alarmSoundType.collectAsState()
  val customSoundUri by viewModel.customSoundUri.collectAsState()
  val customSoundTitle by viewModel.customSoundTitle.collectAsState()
  val alarmVolume by viewModel.alarmVolume.collectAsState()
  val isVibrationEnabled by viewModel.isVibrationEnabled.collectAsState()
  val alarmRepeatMode by viewModel.alarmRepeatMode.collectAsState()
  val alarmCustomDaysMask by viewModel.alarmCustomDaysMask.collectAsState()
  val snoozeMinutes by viewModel.snoozeMinutes.collectAsState()

  var isTestingSound by remember { mutableStateOf(false) }

  DisposableEffect(Unit) {
    onDispose {
      if (isTestingSound) {
        AlarmAudioPlayer.stop()
        isTestingSound = false
      }
    }
  }

  val audioPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent()
  ) { uri ->
    uri?.let { selectedUri ->
      scope.launch(kotlinx.coroutines.Dispatchers.IO) {
        try {
          var displayName = "Custom Audio Track"
          context.contentResolver.query(selectedUri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
              displayName = cursor.getString(nameIndex) ?: "Custom Audio Track"
            }
          }

          val localFile = java.io.File(context.filesDir, "custom_alarm_sound")
          context.contentResolver.openInputStream(selectedUri)?.use { input ->
            localFile.outputStream().use { output ->
              input.copyTo(output)
            }
          }

          viewModel.updateAlarmSound(
            soundType = "CUSTOM",
            customUri = localFile.absolutePath,
            soundTitle = displayName
          )
          kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            Toast.makeText(context, "Alarm sound set to: $displayName", Toast.LENGTH_SHORT).show()
          }
        } catch (e: Exception) {
          android.util.Log.e("SettingsScreen", "Failed to load audio file", e)
          kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            Toast.makeText(context, "Failed to load audio file", Toast.LENGTH_SHORT).show()
          }
        }
      }
    }
  }

  var editingTemplate by remember { mutableStateOf<RoutineTemplateEntity?>(null) }
  var isAddingTemplate by remember { mutableStateOf(false) }

  var editingGoal by remember { mutableStateOf<GoalEntity?>(null) }
  var isAddingGoal by remember { mutableStateOf(false) }

  var showExportDialog by remember { mutableStateOf(false) }
  var showCustomWakeUpPicker by remember { mutableStateOf(false) }

  // Confirmation dialog state
  var pendingDeleteTemplateId by remember { mutableStateOf<Long?>(null) }
  var pendingDeleteGoalId by remember { mutableStateOf<Long?>(null) }

  // Notification Permission Launcher (Android 13+)
  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
    onResult = { isGranted ->
      if (isGranted) {
        viewModel.toggleAlarm(context, true)
        Toast.makeText(context, "Alarm enabled for ${TimeUtils.minutesTo12Hour(wakeUpMinutes)}", Toast.LENGTH_SHORT).show()
      } else {
        Toast.makeText(context, "Notification permission is required for wake-up alarm", Toast.LENGTH_LONG).show()
      }
    }
  )

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBackground),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Header
    item {
      Column {
        Text(
          text = "CONFIGURATION & SYSTEM PREFERENCES",
          color = CyanNeon,
          fontSize = 11.sp,
          fontWeight = FontWeight.Black,
          letterSpacing = 1.sp
        )
        Text(
          text = "System Settings & Blueprint",
          color = TextPrimary,
          fontSize = 22.sp,
          fontWeight = FontWeight.Black
        )
      }
    }

    // 1. Wake-Up Schedule & Native Recurring Alarm
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .background(GlassGradient)
          .border(1.dp, CardBorderGradient, RoundedCornerShape(20.dp))
          .padding(18.dp)
          .testTag("wake_up_alarm_card")
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Alarm, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Wake-Up Schedule & Alarm",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
              )
            }

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isAlarmEnabled) CyanNeon.copy(alpha = 0.15f) else DarkSurfaceElevated)
                .border(1.dp, if (isAlarmEnabled) CyanNeon else DarkSurfaceBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                text = TimeUtils.minutesTo12Hour(wakeUpMinutes),
                color = if (isAlarmEnabled) CyanNeon else TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Select your daily wake-up target. Affects future routine schedule only. Past days and historical adherence records are strictly preserved.",
            color = TextSecondary,
            fontSize = 11.sp,
            lineHeight = 15.sp
          )

          Spacer(modifier = Modifier.height(14.dp))

          // Presets: 3:00 AM, 4:00 AM, 5:00 AM, 6:00 AM, Custom
          Text(
            text = "WAKE-UP PRESETS (12-HOUR AM/PM)",
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
          )
          Spacer(modifier = Modifier.height(8.dp))

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            val presets = listOf(
              180 to "🌙 रात 3:00 बजे",
              240 to "🌅 सुबह 4:00 बजे",
              300 to "🌅 सुबह 5:00 बजे",
              360 to "☀️ सुबह 6:00 बजे"
            )

            presets.forEach { (mins, label) ->
              val isSelected = wakeUpMinutes == mins
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(10.dp))
                  .background(if (isSelected) CyanNeon else DarkSurfaceElevated)
                  .border(1.dp, if (isSelected) CyanNeon else DarkSurfaceBorder, RoundedCornerShape(10.dp))
                  .clickable {
                    viewModel.updateWakeUpSchedule(
                      context = context,
                      newWakeUpMinutes = mins,
                      isAlarmEnabled = isAlarmEnabled,
                      updateFutureMasterRoutine = true
                    )
                    Toast.makeText(context, "Wake-up time set to $label", Toast.LENGTH_SHORT).show()
                  }
                  .padding(horizontal = 14.dp, vertical = 8.dp)
                  .testTag("preset_$mins")
              ) {
                Text(
                  text = label,
                  color = if (isSelected) Color(0xFF00363D) else TextPrimary,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            // Custom Time Button
            val isCustom = presets.none { it.first == wakeUpMinutes }
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (isCustom) CyanNeon else DarkSurfaceElevated)
                .border(1.dp, if (isCustom) CyanNeon else DarkSurfaceBorder, RoundedCornerShape(10.dp))
                .clickable { showCustomWakeUpPicker = true }
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .testTag("preset_custom")
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  Icons.Default.AccessTime,
                  contentDescription = null,
                  tint = if (isCustom) Color(0xFF00363D) else TextSecondary,
                  modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (isCustom) "Custom: ${TimeUtils.minutesToHindiTime(wakeUpMinutes)}" else "Custom...",
                  color = if (isCustom) Color(0xFF00363D) else TextPrimary,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Native Recurring Alarm Toggle
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(DarkSurfaceElevated)
              .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
              .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  Icons.Default.NotificationsActive,
                  contentDescription = null,
                  tint = if (isAlarmEnabled) CyanNeon else TextMuted,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Native Android Alarm",
                  color = TextPrimary,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.SemiBold
                )
              }
              Text(
                text = if (isAlarmEnabled) "Active daily at ${TimeUtils.minutesTo12Hour(wakeUpMinutes)} • Survives reboot" else "Off • Toggle on to ring daily",
                color = if (isAlarmEnabled) CyanNeon else TextMuted,
                fontSize = 11.sp
              )
            }

            Switch(
              checked = isAlarmEnabled,
              onCheckedChange = { enable ->
                if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                  notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                  viewModel.toggleAlarm(context, enable)
                  if (enable) {
                    Toast.makeText(context, "Alarm set for ${TimeUtils.minutesTo12Hour(wakeUpMinutes)}", Toast.LENGTH_SHORT).show()
                  } else {
                    Toast.makeText(context, "Alarm disabled", Toast.LENGTH_SHORT).show()
                  }
                }
              },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = CyanNeon,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = DarkBackground
              ),
              modifier = Modifier.testTag("alarm_switch")
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Divider
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(1.dp)
              .background(DarkSurfaceBorder)
          )

          Spacer(modifier = Modifier.height(14.dp))

          // Section: Sound Selection
          Text(
            text = "ALARM SOUND & MELODY / अलार्म की धुन",
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
          )
          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Built-in High Tone Sound
            val isDefaultSelected = alarmSoundType == "DEFAULT"
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isDefaultSelected) CyanNeon.copy(alpha = 0.2f) else DarkSurfaceElevated)
                .border(1.dp, if (isDefaultSelected) CyanNeon else DarkSurfaceBorder, RoundedCornerShape(10.dp))
                .clickable {
                  viewModel.updateAlarmSound("DEFAULT", null, "High-Tone Digital Alarm")
                  if (isTestingSound) {
                    AlarmAudioPlayer.stop()
                    AlarmAudioPlayer.start(context, "DEFAULT", null, alarmVolume, isVibrationEnabled)
                  }
                  Toast.makeText(context, "High-Tone sound selected", Toast.LENGTH_SHORT).show()
                }
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .testTag("sound_default_button")
            ) {
              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = if (isDefaultSelected) CyanNeon else TextSecondary,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = "High-Tone (Built-in)",
                    color = if (isDefaultSelected) CyanNeon else TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "Sharp piercing tone",
                  color = TextMuted,
                  fontSize = 9.sp
                )
              }
            }

            // Custom Sound File Picker
            val isCustomSelected = alarmSoundType == "CUSTOM"
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isCustomSelected) VioletNeon.copy(alpha = 0.2f) else DarkSurfaceElevated)
                .border(1.dp, if (isCustomSelected) VioletNeon else DarkSurfaceBorder, RoundedCornerShape(10.dp))
                .clickable {
                  audioPickerLauncher.launch("audio/*")
                }
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .testTag("sound_custom_button")
            ) {
              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = if (isCustomSelected) VioletNeon else TextSecondary,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    text = if (isCustomSelected) "Custom Track" else "Choose File...",
                    color = if (isCustomSelected) VioletNeon else TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = if (isCustomSelected) customSoundTitle.take(16) else "Pick MP3/WAV",
                  color = TextMuted,
                  fontSize = 9.sp,
                  maxLines = 1
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Section: Volume & Vibration
          Text(
            text = "VOLUME & VIBRATION / आवाज़ और कम्पन",
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
          )
          Spacer(modifier = Modifier.height(8.dp))

          // Volume Slider Row
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(DarkSurfaceElevated)
              .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
              .padding(horizontal = 12.dp, vertical = 8.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Alarm Volume", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
              }
              Text(
                text = "${(alarmVolume * 100).roundToInt()}%",
                color = CyanNeon,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
            Slider(
              value = alarmVolume,
              onValueChange = { newVol ->
                viewModel.updateAlarmVolume(newVol)
                if (isTestingSound) {
                  AlarmAudioPlayer.setVolume(newVol)
                }
              },
              valueRange = 0.05f..1.0f,
              colors = SliderDefaults.colors(
                thumbColor = CyanNeon,
                activeTrackColor = CyanNeon,
                inactiveTrackColor = DarkBackground
              ),
              modifier = Modifier.fillMaxWidth().testTag("alarm_volume_slider")
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Vibration Toggle Row
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(DarkSurfaceElevated)
              .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
              .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Vibration, contentDescription = null, tint = VioletNeon, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Column {
                Text("Vibrate on Alarm", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("Vibrate even in silent mode", color = TextMuted, fontSize = 10.sp)
              }
            }
            Switch(
              checked = isVibrationEnabled,
              onCheckedChange = { enabled ->
                viewModel.updateVibrationEnabled(enabled)
              },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = VioletNeon,
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = DarkBackground
              ),
              modifier = Modifier.testTag("alarm_vibration_switch")
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Section: Snooze Duration
          Text(
            text = "SNOOZE DURATION / स्नूज़ समय",
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
          )
          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf(5 to "5 min", 10 to "10 min", 15 to "15 min").forEach { (mins, label) ->
              val isSelected = snoozeMinutes == mins
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(10.dp))
                  .background(if (isSelected) CyanNeon else DarkSurfaceElevated)
                  .border(1.dp, if (isSelected) CyanNeon else DarkSurfaceBorder, RoundedCornerShape(10.dp))
                  .clickable {
                    viewModel.updateAlarmSnoozeMinutes(mins)
                  }
                  .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = label,
                  color = if (isSelected) Color(0xFF00363D) else TextPrimary,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Section: Repeat Schedule
          Text(
            text = "REPEAT SCHEDULE / दोहराएं",
            color = TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
          )
          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf("DAILY" to "Daily / रोज़", "WEEKDAYS" to "Mon–Fri", "CUSTOM" to "Custom").forEach { (mode, label) ->
              val isSelected = alarmRepeatMode == mode
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(10.dp))
                  .background(if (isSelected) CyanNeon else DarkSurfaceElevated)
                  .border(1.dp, if (isSelected) CyanNeon else DarkSurfaceBorder, RoundedCornerShape(10.dp))
                  .clickable {
                    viewModel.updateAlarmRepeat(mode, alarmCustomDaysMask, context)
                  }
                  .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = label,
                  color = if (isSelected) Color(0xFF00363D) else TextPrimary,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }

          // If Custom Repeat, show day selector
          if (alarmRepeatMode == "CUSTOM") {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")
              dayNames.forEachIndexed { index, name ->
                val bit = 1 shl index
                val isDayActive = (alarmCustomDaysMask and bit) != 0
                Box(
                  modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isDayActive) CyanNeon else DarkSurfaceElevated)
                    .border(1.dp, if (isDayActive) CyanNeon else DarkSurfaceBorder, CircleShape)
                    .clickable {
                      val newMask = alarmCustomDaysMask xor bit
                      viewModel.updateAlarmRepeat("CUSTOM", if (newMask == 0) bit else newMask, context)
                    },
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = name,
                    color = if (isDayActive) Color(0xFF00363D) else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Divider
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(1.dp)
              .background(DarkSurfaceBorder)
          )

          Spacer(modifier = Modifier.height(14.dp))

          // Test Sound & Vibration Toggle Action Button
          Button(
            onClick = {
              if (isTestingSound) {
                AlarmAudioPlayer.stop()
                isTestingSound = false
              } else {
                isTestingSound = true
                AlarmAudioPlayer.start(
                  context = context,
                  soundType = alarmSoundType,
                  customUriString = customSoundUri,
                  volume = alarmVolume,
                  enableVibration = isVibrationEnabled
                )
              }
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isTestingSound) StatusMissed else DarkSurfaceElevated,
              contentColor = if (isTestingSound) Color.White else CyanNeon
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
              brush = androidx.compose.ui.graphics.SolidColor(if (isTestingSound) StatusMissed else CyanNeon.copy(alpha = 0.4f))
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(44.dp)
              .testTag("test_sound_button")
          ) {
            Icon(
              if (isTestingSound) Icons.Default.Stop else Icons.Default.PlayArrow,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (isTestingSound) "STOP SOUND TEST (आवाज़ बंद करें)" else "TEST ALARM SOUND & VIBRATION (आवाज़ चलाकर देखें)",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Preview Full Alarm Screen Button
          OutlinedButton(
            onClick = {
              val intent = Intent(context, AlarmActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
              }
              context.startActivity(intent)
            },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = VioletNeon),
            border = ButtonDefaults.outlinedButtonBorder.copy(
              brush = androidx.compose.ui.graphics.SolidColor(VioletNeon.copy(alpha = 0.4f))
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(42.dp)
              .testTag("test_alarm_screen_button")
          ) {
            Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("PREVIEW FULL ALARM SCREEN (स्क्रीन प्रिव्यू)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // 2. Master Recurring Routine Blueprint
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .background(GlassGradient)
          .border(1.dp, CardBorderGradient, RoundedCornerShape(20.dp))
          .padding(18.dp)
          .testTag("routine_templates_card")
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Schedule, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Master Recurring Routine Blueprint",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              )
            }

            IconButton(
              onClick = { isAddingTemplate = true },
              modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(CyanNeon.copy(alpha = 0.15f))
                .border(1.dp, CyanNeon.copy(alpha = 0.4f), CircleShape)
                .testTag("add_template_button")
            ) {
              Icon(Icons.Default.Add, contentDescription = "Add Step", tint = CyanNeon, modifier = Modifier.size(16.dp))
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Wake-up to bedtime schedule (strictly 12-hour AM/PM). Templates define future day generation. Past historical records are strictly protected.",
            color = TextSecondary,
            fontSize = 11.sp,
            lineHeight = 15.sp
          )

          Spacer(modifier = Modifier.height(14.dp))

          if (routineTemplates.isEmpty()) {
            Text("No master routine templates defined.", color = TextMuted, fontSize = 12.sp)
          } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              routineTemplates.forEach { template ->
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceElevated)
                    .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
                    .clickable { editingTemplate = template }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                  ) {
                    Box(
                      modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurface)
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                      Text(
                        text = TimeUtils.minutesToHindiTime(template.timeMinutes),
                        color = CyanNeon,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                      )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                      Text(
                        text = template.name,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                      )
                      if (template.category.isNotBlank()) {
                        Text(
                          text = template.category,
                          color = TextMuted,
                          fontSize = 10.sp
                        )
                      }
                    }
                  }

                  Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                      onClick = { editingTemplate = template },
                      modifier = Modifier.size(28.dp)
                    ) {
                      Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextSecondary, modifier = Modifier.size(15.dp))
                    }
                    IconButton(
                      onClick = { pendingDeleteTemplateId = template.id },
                      modifier = Modifier.size(28.dp)
                    ) {
                      Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = StatusMissed, modifier = Modifier.size(15.dp))
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

    // 3. Continuous 7-Day Cycle Anchor Configuration
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .background(GlassGradient)
          .border(1.dp, CardBorderGradient, RoundedCornerShape(20.dp))
          .padding(18.dp)
      ) {
        Column {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Loop, contentDescription = null, tint = VioletNeon, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("7-Day Continuous Cycle", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(VioletNeon.copy(alpha = 0.15f))
                .border(1.dp, VioletNeon.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text("ACTIVE", color = VioletNeon, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Every day advances through a continuous Day 1..Day 7 cycle relative to your anchor date: ${TimeUtils.formatDateDisplay(anchorDate)}. Cycles repeat seamlessly without gaps.",
            color = TextSecondary,
            fontSize = 11.sp,
            lineHeight = 15.sp
          )

          Spacer(modifier = Modifier.height(12.dp))

          Button(
            onClick = {
              viewModel.updateAnchorDate(viewModel.todayDate)
              Toast.makeText(context, "Cycle anchor reset to today.", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = DarkSurfaceElevated,
              contentColor = TextPrimary
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(10.dp))
              .testTag("reset_anchor_button")
          ) {
            Icon(Icons.Default.DateRange, contentDescription = null, tint = VioletNeon, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reset Anchor to Today (Start Day 1 Today)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // 4. Personal Milestone Targets & Goals
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .background(GlassGradient)
          .border(1.dp, CardBorderGradient, RoundedCornerShape(20.dp))
          .padding(18.dp)
          .testTag("goals_card")
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Flag, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Target Milestones & Goals", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            IconButton(
              onClick = { isAddingGoal = true },
              modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(CyanNeon.copy(alpha = 0.15f))
                .border(1.dp, CyanNeon.copy(alpha = 0.4f), CircleShape)
                .testTag("add_goal_button")
            ) {
              Icon(Icons.Default.Add, contentDescription = "Add Goal", tint = CyanNeon, modifier = Modifier.size(16.dp))
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          if (goals.isEmpty()) {
            Text("No active goals tracked yet.", color = TextMuted, fontSize = 12.sp)
          } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              goals.forEach { goal ->
                GoalProgressRow(
                  goal = goal,
                  onProgressChange = { newProg ->
                    viewModel.saveGoal(
                      id = goal.id,
                      title = goal.title,
                      description = goal.description,
                      progress = newProg,
                      deadline = goal.deadline,
                      isCompleted = newProg >= 100
                    )
                  },
                  onEdit = { editingGoal = goal },
                  onDelete = { pendingDeleteGoalId = goal.id }
                )
              }
            }
          }
        }
      }
    }

    // 5. Data Management & AI Analysis Dossier
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .background(GlassGradient)
          .border(1.dp, CardBorderGradient, RoundedCornerShape(20.dp))
          .padding(18.dp)
          .testTag("data_hub_card")
      ) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Backup, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Data Portability & AI Intelligence", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Full offline backup, markdown journals, and ready-to-use AI prompts for performance analysis.",
            color = TextSecondary,
            fontSize = 11.sp
          )

          Spacer(modifier = Modifier.height(14.dp))

          // AI Analysis Prompt Button
          Button(
            onClick = {
              scope.launch {
                val prompt = viewModel.exportAiAnalysis()
                clipboardManager.setText(AnnotatedString(prompt))
                Toast.makeText(context, "AI Analysis Prompt copied to clipboard!", Toast.LENGTH_LONG).show()
                showExportDialog = true
              }
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = CyanNeon,
              contentColor = Color(0xFF00363D)
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("copy_ai_prompt_button")
          ) {
            Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Open Data Export & AI Intelligence Hub", fontSize = 12.sp, fontWeight = FontWeight.Black)
          }
        }
      }
    }
  }

  // Custom Wake-Up Time Picker (12-Hour AM/PM)
  if (showCustomWakeUpPicker) {
    TimePickerDialog12Hour(
      initialMinutes = wakeUpMinutes,
      onDismiss = { showCustomWakeUpPicker = false },
      onConfirm = { chosenMinutes ->
        viewModel.updateWakeUpSchedule(
          context = context,
          newWakeUpMinutes = chosenMinutes,
          isAlarmEnabled = isAlarmEnabled,
          updateFutureMasterRoutine = true
        )
        Toast.makeText(context, "Wake-up scheduled for ${TimeUtils.minutesTo12Hour(chosenMinutes)}", Toast.LENGTH_SHORT).show()
        showCustomWakeUpPicker = false
      }
    )
  }

  // Delete Confirmation: Routine Template
  if (pendingDeleteTemplateId != null) {
    AlertDialog(
      onDismissRequest = { pendingDeleteTemplateId = null },
      containerColor = DarkSurface,
      shape = RoundedCornerShape(18.dp),
      title = { Text("Delete Routine Step?", color = TextPrimary, fontWeight = FontWeight.Bold) },
      text = { Text("Are you sure you want to delete this routine step? Future days will no longer generate it. Existing historical records will remain safe.", color = TextSecondary, fontSize = 13.sp) },
      confirmButton = {
        Button(
          onClick = {
            pendingDeleteTemplateId?.let { viewModel.deleteRoutineTemplate(it) }
            pendingDeleteTemplateId = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = StatusMissed, contentColor = Color.White)
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { pendingDeleteTemplateId = null }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }

  // Delete Confirmation: Goal
  if (pendingDeleteGoalId != null) {
    AlertDialog(
      onDismissRequest = { pendingDeleteGoalId = null },
      containerColor = DarkSurface,
      shape = RoundedCornerShape(18.dp),
      title = { Text("Delete Milestone Goal?", color = TextPrimary, fontWeight = FontWeight.Bold) },
      text = { Text("Are you sure you want to delete this goal? This action cannot be undone.", color = TextSecondary, fontSize = 13.sp) },
      confirmButton = {
        Button(
          onClick = {
            pendingDeleteGoalId?.let { viewModel.deleteGoal(it) }
            pendingDeleteGoalId = null
          },
          colors = ButtonDefaults.buttonColors(containerColor = StatusMissed, contentColor = Color.White)
        ) {
          Text("Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { pendingDeleteGoalId = null }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }

  // Dialogs & Sheets
  if (showExportDialog) {
    ExportImportDialog(
      onDismiss = { showExportDialog = false },
      onGenerateContent = { formatTab ->
        when (formatTab) {
          ExportFormatTab.AI_PROMPT -> viewModel.exportAiAnalysis()
          ExportFormatTab.JSON -> viewModel.exportJson()
          ExportFormatTab.CSV -> viewModel.exportCsv()
          ExportFormatTab.MARKDOWN -> viewModel.exportMarkdown()
          ExportFormatTab.TXT -> viewModel.exportPlainText()
          ExportFormatTab.IMPORT -> ""
        }
      },
      onImportJson = { jsonToRestore, callback ->
        viewModel.importJson(jsonToRestore, callback)
      }
    )
  }

  if (isAddingTemplate) {
    RoutineTemplateBottomSheet(
      initialTemplate = null,
      onDismiss = { isAddingTemplate = false },
      onSave = { _, name, timeMinutes, category, notes, daysMask, isActive ->
        viewModel.saveRoutineTemplate(
          id = 0L,
          name = name,
          timeMinutes = timeMinutes,
          category = category,
          notes = notes,
          daysMask = daysMask,
          isActive = isActive
        )
      }
    )
  }

  if (editingTemplate != null) {
    RoutineTemplateBottomSheet(
      initialTemplate = editingTemplate,
      onDismiss = { editingTemplate = null },
      onSave = { id, name, timeMinutes, category, notes, daysMask, isActive ->
        viewModel.saveRoutineTemplate(
          id = id,
          name = name,
          timeMinutes = timeMinutes,
          category = category,
          notes = notes,
          daysMask = daysMask,
          isActive = isActive
        )
        editingTemplate = null
      },
      onDelete = { id ->
        pendingDeleteTemplateId = id
        editingTemplate = null
      }
    )
  }

  if (isAddingGoal) {
    GoalBottomSheet(
      initialGoal = null,
      onDismiss = { isAddingGoal = false },
      onSave = { _, title, description, progress, deadline, isCompleted ->
        viewModel.saveGoal(
          id = 0L,
          title = title,
          description = description,
          progress = progress,
          deadline = deadline,
          isCompleted = isCompleted
        )
      }
    )
  }

  if (editingGoal != null) {
    GoalBottomSheet(
      initialGoal = editingGoal,
      onDismiss = { editingGoal = null },
      onSave = { id, title, description, progress, deadline, isCompleted ->
        viewModel.saveGoal(
          id = id,
          title = title,
          description = description,
          progress = progress,
          deadline = deadline,
          isCompleted = isCompleted
        )
        editingGoal = null
      },
      onDelete = { id ->
        pendingDeleteGoalId = id
        editingGoal = null
      }
    )
  }
}

@Composable
private fun GoalProgressRow(
  goal: GoalEntity,
  onProgressChange: (Int) -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit
) {
  var sliderVal by remember(goal.progress) { mutableFloatStateOf(goal.progress.toFloat()) }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(DarkSurfaceElevated)
      .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(12.dp))
      .padding(12.dp)
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(goal.title, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
          if (!goal.deadline.isNullOrBlank()) {
            Text("Deadline: ${goal.deadline}", color = TextMuted, fontSize = 10.sp)
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "${sliderVal.roundToInt()}%",
            color = CyanNeon,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.width(6.dp))
          IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextSecondary, modifier = Modifier.size(13.dp))
          }
          IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = StatusMissed, modifier = Modifier.size(13.dp))
          }
        }
      }

      Spacer(modifier = Modifier.height(4.dp))

      Slider(
        value = sliderVal,
        onValueChange = { sliderVal = it },
        onValueChangeFinished = { onProgressChange(sliderVal.roundToInt()) },
        valueRange = 0f..100f,
        colors = SliderDefaults.colors(
          thumbColor = CyanNeon,
          activeTrackColor = CyanNeon,
          inactiveTrackColor = DarkSurface
        )
      )
    }
  }
}
