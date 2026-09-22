package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ViewTimeline
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DayTaskEntity
import com.example.ui.components.CollapsibleTimeBlockCard
import com.example.ui.components.SummaryHeaderCard
import com.example.ui.components.TaskBottomSheet
import com.example.ui.components.TaskItemCard
import com.example.ui.components.TimeBlockData
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ExtraTaskAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletNeon
import com.example.ui.viewmodel.LifeTrackerViewModel
import com.example.util.TimeUtils
import java.util.Calendar

@Composable
fun TodayScreen(
  viewModel: LifeTrackerViewModel,
  modifier: Modifier = Modifier
) {
  val selectedDate by viewModel.selectedDate.collectAsState()
  val summary by viewModel.summaryForSelectedDate.collectAsState()
  val tasks by viewModel.tasksForSelectedDate.collectAsState()

  val isToday = selectedDate == viewModel.todayDate

  var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
  var editingTask by remember { mutableStateOf<DayTaskEntity?>(null) }
  var isAddingTask by remember { mutableStateOf(false) }
  var isAddingExtraTask by remember { mutableStateOf(false) }

  // Separate Extra Tasks from Routine Timeline
  val extraTasks = remember(tasks) { tasks.filter { it.isExtra } }
  val routineTasks = remember(tasks) { tasks.filter { !it.isExtra } }

  // Determine current active task and next task
  val nowMinutes = remember {
    val cal = Calendar.getInstance()
    cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
  }

  val currentTaskId = remember(routineTasks, isToday, nowMinutes) {
    if (!isToday || routineTasks.isEmpty()) null
    else {
      // Find the task currently active (last task whose time <= now, or first if before all)
      routineTasks.lastOrNull { it.timeMinutes <= nowMinutes }?.id ?: routineTasks.firstOrNull()?.id
    }
  }

  val nextTaskId = remember(routineTasks, isToday, nowMinutes, currentTaskId) {
    if (!isToday || routineTasks.isEmpty()) null
    else {
      routineTasks.firstOrNull { it.timeMinutes > nowMinutes && it.id != currentTaskId }?.id
    }
  }

  // Extract distinct categories from routine tasks
  val categories = remember(routineTasks) {
    routineTasks.map { it.category }.filter { it.isNotBlank() }.distinct()
  }

  val filteredRoutineTasks = remember(routineTasks, selectedCategoryFilter) {
    if (selectedCategoryFilter == null) {
      routineTasks
    } else {
      routineTasks.filter { it.category.equals(selectedCategoryFilter, ignoreCase = true) }
    }
  }

  // Group routine tasks into 1-hour Collapsible Time Blocks
  val timeBlocks = remember(filteredRoutineTasks) {
    if (filteredRoutineTasks.isEmpty()) emptyList()
    else {
      filteredRoutineTasks
        .groupBy { (it.timeMinutes / 60) * 60 }
        .map { (blockStart, blockTasks) ->
          val blockEnd = blockStart + 60
          TimeBlockData(
            startMinutes = blockStart,
            endMinutes = blockEnd,
            label = TimeUtils.minutesToHindiTimeRange(blockStart, blockEnd),
            tasks = blockTasks.sortedWith(compareBy({ it.timeMinutes }, { it.orderIndex }, { it.id }))
          )
        }
        .sortedBy { it.startMinutes }
    }
  }

  var targetBlockTimeForNewTask by remember { mutableStateOf<Int?>(null) }
  var taskToDelete by remember { mutableStateOf<DayTaskEntity?>(null) }
  val expandedBlockKeys = remember { mutableStateMapOf<Int, Boolean>() }

  val allExpanded = remember(timeBlocks, expandedBlockKeys.size) {
    timeBlocks.isNotEmpty() && timeBlocks.all { expandedBlockKeys[it.startMinutes] != false }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBackground)
  ) {
    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // 1. Top Cinematic Summary Header Card
      item {
        SummaryHeaderCard(
          summary = summary,
          selectedDate = selectedDate,
          isToday = isToday,
          onPrevDay = { viewModel.navigateDay(-1) },
          onNextDay = { viewModel.navigateDay(1) },
          onJumpToday = { viewModel.setToday() }
        )
      }

      // 2. Dedicated Section: TODAY EXTRA TASKS
      if (extraTasks.isNotEmpty()) {
        item {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(20.dp))
              .background(ExtraTaskAccent.copy(alpha = 0.05f))
              .border(1.dp, ExtraTaskAccent.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
              .padding(14.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Bolt,
                  contentDescription = null,
                  tint = ExtraTaskAccent,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "TODAY EXTRA TASKS • 1-DAY HORIZON",
                  color = ExtraTaskAccent,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Black,
                  letterSpacing = 0.8.sp
                )
              }

              Text(
                text = "${extraTasks.count { it.status == com.example.data.model.TaskStatus.COMPLETE }}/${extraTasks.size} done",
                color = ExtraTaskAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            extraTasks.forEach { extraTask ->
              TaskItemCard(
                task = extraTask,
                onStatusChange = { viewModel.updateTaskStatus(extraTask, it) },
                onEdit = { editingTask = extraTask },
                onDelete = { viewModel.deleteTask(extraTask.id) },
                modifier = Modifier.padding(bottom = 8.dp)
              )
            }
          }
        }
      }

      // 3. Daily Timeline Header & Category Filter
      item {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.ViewTimeline,
                contentDescription = null,
                tint = CyanNeon,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "DAILY TIMELINE ROUTINE",
                color = CyanNeon,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
              )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              if (timeBlocks.isNotEmpty()) {
                TextButton(
                  onClick = {
                    val nextState = !allExpanded
                    timeBlocks.forEach { expandedBlockKeys[it.startMinutes] = nextState }
                  },
                  contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text(
                    text = if (allExpanded) "सभी समेटें ▼" else "सभी खोलें ▲",
                    color = CyanNeon,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
                Spacer(modifier = Modifier.width(4.dp))
              }

              Text(
                text = "${routineTasks.size} tasks",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          // Filter chips
          val filterScroll = rememberScrollState()
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(filterScroll),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.FilterList,
              contentDescription = "Filter",
              tint = TextMuted,
              modifier = Modifier.size(16.dp)
            )

            TimelineFilterChip(
              label = "All (${routineTasks.size})",
              isSelected = selectedCategoryFilter == null,
              onClick = { selectedCategoryFilter = null }
            )

            categories.forEach { cat ->
              val count = routineTasks.count { it.category.equals(cat, ignoreCase = true) }
              TimelineFilterChip(
                label = "$cat ($count)",
                isSelected = selectedCategoryFilter.equals(cat, ignoreCase = true),
                onClick = { selectedCategoryFilter = cat }
              )
            }
          }
        }
      }

      // 4. Cinematic Collapsible Time Blocks
      if (filteredRoutineTasks.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 36.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = if (routineTasks.isEmpty()) "इस दिन के लिए कोई रूटीन टास्क तय नहीं है।" else "फ़िल्टर के अनुसार कोई टास्क नहीं मिला।",
                color = TextSecondary,
                fontSize = 13.sp
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "+ दबाकर टाइम ब्लॉक में नया छोटा टास्क जोड़ें।",
                color = TextMuted,
                fontSize = 11.sp
              )
            }
          }
        }
      } else {
        items(timeBlocks, key = { it.startMinutes }) { block ->
          val isExpanded = expandedBlockKeys[block.startMinutes] ?: true
          val isCurrentBlock = isToday && nowMinutes in block.startMinutes until block.endMinutes

          CollapsibleTimeBlockCard(
            block = block,
            isExpanded = isExpanded,
            onToggleExpand = {
              expandedBlockKeys[block.startMinutes] = !isExpanded
            },
            onAddSmallTask = {
              targetBlockTimeForNewTask = block.startMinutes
              expandedBlockKeys[block.startMinutes] = true
              isAddingTask = true
            },
            onStatusChange = { task, newStatus ->
              viewModel.updateTaskStatus(task, newStatus)
            },
            onEditTask = { task ->
              editingTask = task
            },
            onDeleteTask = { task ->
              taskToDelete = task
            },
            currentTaskId = currentTaskId,
            isCurrentBlock = isCurrentBlock,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
    }

    // Floating Glass Controls at bottom right
    Column(
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(end = 16.dp, bottom = 20.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      horizontalAlignment = Alignment.End
    ) {
      // "+ Extra Task" Glass Pill
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
          .clip(RoundedCornerShape(18.dp))
          .background(DarkSurfaceElevated.copy(alpha = 0.95f))
          .border(1.dp, ExtraTaskAccent.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
          .clickable { isAddingExtraTask = true }
          .padding(horizontal = 12.dp, vertical = 7.dp)
          .testTag("add_extra_task_button")
      ) {
        Icon(
          imageVector = Icons.Default.Bolt,
          contentDescription = null,
          tint = ExtraTaskAccent,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "+ Extra Task",
          color = ExtraTaskAccent,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
      }

      // Add Routine Task FAB (Glowing Cyan)
      FloatingActionButton(
        onClick = {
          targetBlockTimeForNewTask = null
          isAddingTask = true
        },
        containerColor = CyanNeon,
        contentColor = Color(0xFF00363D),
        shape = CircleShape,
        modifier = Modifier
          .size(52.dp)
          .testTag("add_task_fab")
      ) {
        Icon(Icons.Default.Add, contentDescription = "Add Task", modifier = Modifier.size(24.dp))
      }
    }
  }

  // Modals for Adding / Editing Tasks
  if (isAddingTask) {
    TaskBottomSheet(
      initialTask = null,
      isExtraTaskDefault = false,
      initialTimeMinutesDefault = targetBlockTimeForNewTask,
      onDismiss = {
        isAddingTask = false
        targetBlockTimeForNewTask = null
      },
      onSave = { _, name, timeMinutes, category, notes, isExtra ->
        viewModel.saveTask(
          name = name,
          timeMinutes = timeMinutes,
          category = category,
          notes = notes,
          isExtra = isExtra
        )
        val blockKey = (timeMinutes / 60) * 60
        expandedBlockKeys[blockKey] = true
        isAddingTask = false
        targetBlockTimeForNewTask = null
      }
    )
  }

  if (isAddingExtraTask) {
    TaskBottomSheet(
      initialTask = null,
      isExtraTaskDefault = true,
      onDismiss = { isAddingExtraTask = false },
      onSave = { _, name, timeMinutes, category, notes, isExtra ->
        viewModel.saveTask(
          name = name,
          timeMinutes = timeMinutes,
          category = category,
          notes = notes,
          isExtra = isExtra
        )
        isAddingExtraTask = false
      }
    )
  }

  if (editingTask != null) {
    TaskBottomSheet(
      initialTask = editingTask,
      isExtraTaskDefault = editingTask?.isExtra ?: false,
      onDismiss = { editingTask = null },
      onSave = { id, name, timeMinutes, category, notes, isExtra ->
        viewModel.saveTask(
          id = id,
          name = name,
          timeMinutes = timeMinutes,
          category = category,
          notes = notes,
          isExtra = isExtra
        )
        editingTask = null
      },
      onDelete = { id ->
        viewModel.deleteTask(id)
        editingTask = null
      }
    )
  }

  // Delete Task Confirmation Dialog
  if (taskToDelete != null) {
    androidx.compose.material3.AlertDialog(
      onDismissRequest = { taskToDelete = null },
      containerColor = DarkSurfaceElevated,
      shape = RoundedCornerShape(18.dp),
      title = {
        Text("टास्क डिलीट करें?", color = TextPrimary, fontWeight = FontWeight.Bold)
      },
      text = {
        Text(
          "क्या आप वाकई \"${taskToDelete?.name}\" को हटाना चाहते हैं? यह क्रिया पूर्ववत नहीं की जा सकती।",
          color = TextSecondary,
          fontSize = 13.sp
        )
      },
      confirmButton = {
        androidx.compose.material3.Button(
          onClick = {
            taskToDelete?.let { viewModel.deleteTask(it.id) }
            taskToDelete = null
          },
          colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = com.example.ui.theme.StatusMissed,
            contentColor = Color.White
          )
        ) {
          Text("डिलीट करें")
        }
      },
      dismissButton = {
        androidx.compose.material3.TextButton(onClick = { taskToDelete = null }) {
          Text("रद्द करें", color = TextSecondary)
        }
      }
    )
  }
}

@Composable
private fun TimelineFilterChip(
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .background(if (isSelected) CyanNeon else DarkSurfaceElevated)
      .border(1.dp, if (isSelected) CyanNeon else DarkSurfaceBorder, RoundedCornerShape(8.dp))
      .clickable { onClick() }
      .padding(horizontal = 10.dp, vertical = 5.dp)
  ) {
    Text(
      text = label,
      color = if (isSelected) Color(0xFF00363D) else TextSecondary,
      fontSize = 11.sp,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
    )
  }
}
