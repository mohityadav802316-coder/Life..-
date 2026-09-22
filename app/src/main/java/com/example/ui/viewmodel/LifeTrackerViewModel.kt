package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.LifeTrackerDatabase
import com.example.data.model.DaySummary
import com.example.data.model.DayTaskEntity
import com.example.data.model.GoalEntity
import com.example.data.model.JournalistCategory
import com.example.data.model.JournalistEntryEntity
import com.example.data.model.JournalistPersonEntity
import com.example.data.model.ReflectionCategory
import com.example.data.model.ReflectionEntity
import com.example.data.model.RoutineTemplateEntity
import com.example.data.model.TaskStatus
import com.example.data.repository.LifeTrackerRepository
import com.example.util.ExportImportHelper
import com.example.util.TimeUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class MainTab {
  TODAY,
  WEEK,
  REPORT,
  REFLECTION,
  SETTINGS
}

class LifeTrackerViewModel(application: Application) : AndroidViewModel(application) {
  val database = LifeTrackerDatabase.getDatabase(application)
  val repository = LifeTrackerRepository(database)

  val todayDate: String
    get() = TimeUtils.getTodayDateString()

  private val _currentTab = MutableStateFlow(MainTab.TODAY)
  val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

  private val _selectedDate = MutableStateFlow(todayDate)
  val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

  val userSettings = repository.getUserSettings().stateIn(
    viewModelScope,
    SharingStarted.Eagerly,
    null
  )

  val anchorDate: StateFlow<String> = userSettings.map {
    it?.anchorDate ?: todayDate
  }.stateIn(viewModelScope, SharingStarted.Eagerly, todayDate)

  val wakeUpMinutes: StateFlow<Int> = userSettings.map {
    it?.wakeUpMinutes ?: 300 // Default 5:00 AM
  }.stateIn(viewModelScope, SharingStarted.Eagerly, 300)

  val isAlarmEnabled: StateFlow<Boolean> = userSettings.map {
    it?.isAlarmEnabled ?: false
  }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

  val snoozeMinutes: StateFlow<Int> = userSettings.map {
    it?.snoozeMinutes ?: 10
  }.stateIn(viewModelScope, SharingStarted.Eagerly, 10)

  val alarmSoundType: StateFlow<String> = userSettings.map {
    it?.alarmSoundType ?: "DEFAULT"
  }.stateIn(viewModelScope, SharingStarted.Eagerly, "DEFAULT")

  val customSoundUri: StateFlow<String?> = userSettings.map {
    it?.customSoundUri
  }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

  val customSoundTitle: StateFlow<String> = userSettings.map {
    it?.customSoundTitle ?: "High-Tone Digital Alarm"
  }.stateIn(viewModelScope, SharingStarted.Eagerly, "High-Tone Digital Alarm")

  val alarmVolume: StateFlow<Float> = userSettings.map {
    it?.alarmVolume ?: 1.0f
  }.stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)

  val isVibrationEnabled: StateFlow<Boolean> = userSettings.map {
    it?.isVibrationEnabled ?: true
  }.stateIn(viewModelScope, SharingStarted.Eagerly, true)

  val alarmRepeatMode: StateFlow<String> = userSettings.map {
    it?.alarmRepeatMode ?: "DAILY"
  }.stateIn(viewModelScope, SharingStarted.Eagerly, "DAILY")

  val alarmCustomDaysMask: StateFlow<Int> = userSettings.map {
    it?.alarmCustomDaysMask ?: 127
  }.stateIn(viewModelScope, SharingStarted.Eagerly, 127)

  val currentCycleDay: StateFlow<Int> = combine(anchorDate, selectedDate) { anchor, selected ->
    TimeUtils.calculateCycleDay(anchor, selected)
  }.stateIn(viewModelScope, SharingStarted.Eagerly, 1)

  @OptIn(ExperimentalCoroutinesApi::class)
  val tasksForSelectedDate: StateFlow<List<DayTaskEntity>> = _selectedDate.flatMapLatest { date ->
    repository.getTasksForDate(date)
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val summaryForSelectedDate: StateFlow<DaySummary> = combine(tasksForSelectedDate, currentCycleDay, selectedDate) { tasks, cycleDay, date ->
    val total = tasks.size
    val completed = tasks.count { it.status == TaskStatus.COMPLETE }
    val partial = tasks.count { it.status == TaskStatus.PARTIAL }
    val missed = tasks.count { it.status == TaskStatus.MISSED }
    val totalScore = (completed * 1.0f) + (partial * 0.5f)
    val percentage = if (total > 0) ((totalScore / total.toFloat()) * 100f).roundToInt() else 0

    DaySummary(
      date = date,
      dayOfCycle = cycleDay,
      totalTasks = total,
      completedCount = completed,
      partialCount = partial,
      missedCount = missed,
      totalScore = totalScore,
      completionPercentage = percentage
    )
  }.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    DaySummary(todayDate, 1, 0, 0, 0, 0, 0f, 0)
  )

  val allTasks: StateFlow<List<DayTaskEntity>> = repository.getAllTasks().stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    emptyList()
  )

  // 7-day Cycle Dates and Summaries
  val currentCycleDates: StateFlow<List<String>> = combine(anchorDate, selectedDate) { anchor, selected ->
    TimeUtils.getCycleDates(anchor, selected)
  }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  val cycleSummaries: StateFlow<List<DaySummary>> = combine(currentCycleDates, allTasks, anchorDate) { cycleDates, tasksList, anchor ->
    val tasksByDate = tasksList.groupBy { it.date }
    cycleDates.map { date ->
      val cycleDay = TimeUtils.calculateCycleDay(anchor, date)
      val tasks = tasksByDate[date] ?: emptyList()
      val total = tasks.size
      val completed = tasks.count { it.status == TaskStatus.COMPLETE }
      val partial = tasks.count { it.status == TaskStatus.PARTIAL }
      val missed = tasks.count { it.status == TaskStatus.MISSED }
      val totalScore = (completed * 1.0f) + (partial * 0.5f)
      val percentage = if (total > 0) ((totalScore / total.toFloat()) * 100f).roundToInt() else 0

      DaySummary(
        date = date,
        dayOfCycle = cycleDay,
        totalTasks = total,
        completedCount = completed,
        partialCount = partial,
        missedCount = missed,
        totalScore = totalScore,
        completionPercentage = percentage
      )
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Routine Templates
  val routineTemplates: StateFlow<List<RoutineTemplateEntity>> = repository.getAllRoutineTemplates().stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    emptyList()
  )

  // Reflections
  val allReflections: StateFlow<List<ReflectionEntity>> = repository.getAllReflections().stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    emptyList()
  )

  private val _reflectionCategoryFilter = MutableStateFlow<ReflectionCategory?>(null)
  val reflectionCategoryFilter: StateFlow<ReflectionCategory?> = _reflectionCategoryFilter.asStateFlow()

  private val _reflectionSearchQuery = MutableStateFlow("")
  val reflectionSearchQuery: StateFlow<String> = _reflectionSearchQuery.asStateFlow()

  private val _reflectionTagFilter = MutableStateFlow<String?>(null)
  val reflectionTagFilter: StateFlow<String?> = _reflectionTagFilter.asStateFlow()

  val filteredReflections: StateFlow<List<ReflectionEntity>> = combine(
    allReflections,
    _reflectionCategoryFilter,
    _reflectionSearchQuery,
    _reflectionTagFilter
  ) { list, cat, query, tag ->
    list.filter { item ->
      val matchesCat = cat == null || item.category == cat
      val matchesQuery = query.isBlank() ||
        item.title.contains(query, ignoreCase = true) ||
        item.description.contains(query, ignoreCase = true) ||
        item.tags.contains(query, ignoreCase = true)
      val matchesTag = tag.isNullOrBlank() || item.tags.contains(tag, ignoreCase = true)
      matchesCat && matchesQuery && matchesTag
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Journalist State
  val allJournalistPersons: StateFlow<List<JournalistPersonEntity>> = repository.getAllJournalistPersons().stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    emptyList()
  )

  val allJournalistEntries: StateFlow<List<JournalistEntryEntity>> = repository.getAllJournalistEntries().stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    emptyList()
  )

  private val _selectedPersonId = MutableStateFlow<String?>(null)
  val selectedPersonId: StateFlow<String?> = _selectedPersonId.asStateFlow()

  private val _journalistCategoryFilter = MutableStateFlow<JournalistCategory?>(null)
  val journalistCategoryFilter: StateFlow<JournalistCategory?> = _journalistCategoryFilter.asStateFlow()

  private val _journalistSearchQuery = MutableStateFlow("")
  val journalistSearchQuery: StateFlow<String> = _journalistSearchQuery.asStateFlow()

  // Filtered entries for the currently selected person profile
  val profileEntries: StateFlow<List<JournalistEntryEntity>> = combine(
    allJournalistEntries,
    _selectedPersonId,
    _journalistCategoryFilter,
    _journalistSearchQuery
  ) { entries, personId, cat, query ->
    if (personId == null) {
      emptyList()
    } else {
      entries.filter { item ->
        val matchesPerson = item.personId == personId
        val matchesCategory = cat == null || item.category == cat
        val matchesQuery = query.isBlank() ||
          item.text.contains(query, ignoreCase = true) ||
          item.context.contains(query, ignoreCase = true) ||
          item.intensity.contains(query, ignoreCase = true) ||
          item.date.contains(query, ignoreCase = true)
        matchesPerson && matchesCategory && matchesQuery
      }
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // Goals
  val allGoals: StateFlow<List<GoalEntity>> = repository.getAllGoals().stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    emptyList()
  )

  val dailySnapshots: StateFlow<List<com.example.data.model.DailySnapshotEntity>> = repository.getAllDailySnapshots().stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    emptyList()
  )

  init {
    viewModelScope.launch {
      repository.initializeDefaultsIfNeeded(todayDate)
    }
  }

  fun onAppResume() {
    val currentToday = todayDate
    viewModelScope.launch {
      val cycleDay = TimeUtils.calculateCycleDay(anchorDate.value, currentToday)
      repository.ensureDayInitialized(currentToday, cycleDay)
      if (_currentTab.value == MainTab.TODAY && _selectedDate.value != currentToday) {
        _selectedDate.value = currentToday
      }
      repository.syncDailySnapshot(currentToday)
    }
  }

  fun onAppPause() {
    viewModelScope.launch {
      repository.syncDailySnapshot(_selectedDate.value)
    }
  }

  fun setTab(tab: MainTab) {
    _currentTab.value = tab
  }

  fun selectDate(date: String) {
    _selectedDate.value = date
    viewModelScope.launch {
      val cycleDay = TimeUtils.calculateCycleDay(anchorDate.value, date)
      repository.ensureDayInitialized(date, cycleDay)
    }
  }

  fun navigateDay(delta: Int) {
    val newDate = TimeUtils.shiftDate(_selectedDate.value, delta)
    selectDate(newDate)
  }

  fun navigateCycle(deltaWeeks: Int) {
    val newDate = TimeUtils.shiftDate(_selectedDate.value, deltaWeeks * 7)
    selectDate(newDate)
  }

  fun setToday() {
    selectDate(todayDate)
  }

  fun updateTaskStatus(task: DayTaskEntity, newStatus: TaskStatus) {
    viewModelScope.launch {
      repository.updateTaskStatus(task, newStatus)
    }
  }

  fun saveTask(
    id: Long = 0,
    name: String,
    timeMinutes: Int,
    category: String,
    notes: String,
    isExtra: Boolean
  ) {
    viewModelScope.launch {
      if (id == 0L) {
        val newTask = DayTaskEntity(
          date = _selectedDate.value,
          templateId = if (isExtra) null else -1L,
          name = name,
          timeMinutes = timeMinutes,
          category = category,
          status = TaskStatus.MISSED,
          notes = notes,
          isExtra = isExtra,
          orderIndex = tasksForSelectedDate.value.size
        )
        repository.insertTask(newTask)
      } else {
        val existing = tasksForSelectedDate.value.firstOrNull { it.id == id }
        if (existing != null) {
          repository.updateTask(
            existing.copy(
              name = name,
              timeMinutes = timeMinutes,
              category = category,
              notes = notes,
              isExtra = isExtra
            )
          )
        }
      }
    }
  }

  fun deleteTask(taskId: Long) {
    val target = tasksForSelectedDate.value.firstOrNull { it.id == taskId }
    val date = target?.date ?: _selectedDate.value
    viewModelScope.launch {
      repository.deleteTask(taskId, date)
    }
  }

  // Routine templates management
  fun saveRoutineTemplate(
    id: Long = 0,
    name: String,
    timeMinutes: Int,
    category: String,
    notes: String,
    daysMask: Int,
    isActive: Boolean
  ) {
    viewModelScope.launch {
      if (id == 0L) {
        val item = RoutineTemplateEntity(
          name = name,
          timeMinutes = timeMinutes,
          category = category,
          notes = notes,
          daysMask = daysMask,
          isActive = isActive,
          orderIndex = routineTemplates.value.size
        )
        repository.insertRoutineTemplate(item)
      } else {
        val existing = routineTemplates.value.firstOrNull { it.id == id }
        if (existing != null) {
          repository.updateRoutineTemplate(
            existing.copy(
              name = name,
              timeMinutes = timeMinutes,
              category = category,
              notes = notes,
              daysMask = daysMask,
              isActive = isActive
            )
          )
        }
      }
    }
  }

  fun deleteRoutineTemplate(id: Long) {
    viewModelScope.launch {
      repository.deleteRoutineTemplate(id)
    }
  }

  // Reflection management
  fun setReflectionCategoryFilter(category: ReflectionCategory?) {
    _reflectionCategoryFilter.value = category
  }

  fun setReflectionSearchQuery(query: String) {
    _reflectionSearchQuery.value = query
  }

  fun setReflectionTagFilter(tag: String?) {
    _reflectionTagFilter.value = tag
  }

  fun saveReflection(
    id: Long = 0,
    title: String,
    description: String,
    date: String,
    category: ReflectionCategory,
    tags: String
  ) {
    viewModelScope.launch {
      if (id == 0L) {
        val entity = ReflectionEntity(
          title = title,
          description = description,
          date = date,
          timestamp = System.currentTimeMillis(),
          category = category,
          tags = tags
        )
        repository.insertReflection(entity)
      } else {
        val existing = allReflections.value.firstOrNull { it.id == id }
        if (existing != null) {
          repository.updateReflection(
            existing.copy(
              title = title,
              description = description,
              date = date,
              category = category,
              tags = tags
            )
          )
        }
      }
    }
  }

  fun deleteReflection(id: Long) {
    viewModelScope.launch {
      repository.deleteReflection(id)
    }
  }

  // Journalist actions
  fun setSelectedPerson(personId: String?) {
    _selectedPersonId.value = personId
    _journalistCategoryFilter.value = null
    _journalistSearchQuery.value = ""
  }

  fun setJournalistCategoryFilter(cat: JournalistCategory?) {
    _journalistCategoryFilter.value = cat
  }

  fun setJournalistSearchQuery(query: String) {
    _journalistSearchQuery.value = query
  }

  fun addJournalistPerson(name: String, emoji: String = "👤") {
    viewModelScope.launch {
      val trimmed = name.trim()
      if (trimmed.isEmpty()) return@launch
      val id = java.util.UUID.randomUUID().toString()
      val person = JournalistPersonEntity(
        id = id,
        name = trimmed,
        emoji = emoji.ifBlank { "👤" },
        createdAt = System.currentTimeMillis()
      )
      repository.insertJournalistPerson(person)
    }
  }

  fun deleteJournalistPerson(id: String) {
    viewModelScope.launch {
      repository.deleteJournalistPerson(id)
      if (_selectedPersonId.value == id) {
        _selectedPersonId.value = null
      }
    }
  }

  fun saveJournalistEntry(
    id: String? = null,
    personId: String,
    personName: String,
    category: JournalistCategory,
    text: String,
    date: String,
    time: String,
    context: String = "",
    intensity: String = ""
  ) {
    viewModelScope.launch {
      val trimmedText = text.trim()
      if (trimmedText.isEmpty()) return@launch

      val entryId = id ?: java.util.UUID.randomUUID().toString()
      val now = System.currentTimeMillis()
      val timestamp = "${date}T${time}:00"

      val entry = JournalistEntryEntity(
        id = entryId,
        personId = personId,
        personName = personName,
        category = category,
        text = trimmedText,
        timestamp = timestamp,
        date = date,
        time = time,
        context = context.trim(),
        intensity = intensity.trim(),
        createdAt = if (id == null) now else (allJournalistEntries.value.firstOrNull { it.id == id }?.createdAt ?: now),
        updatedAt = now
      )
      if (id == null) {
        repository.insertJournalistEntry(entry)
      } else {
        repository.updateJournalistEntry(entry)
      }
    }
  }

  fun deleteJournalistEntry(id: String) {
    viewModelScope.launch {
      repository.deleteJournalistEntry(id)
    }
  }

  // Goal management
  fun saveGoal(
    id: Long = 0,
    title: String,
    description: String,
    progress: Int,
    deadline: String?,
    isCompleted: Boolean
  ) {
    viewModelScope.launch {
      if (id == 0L) {
        val goal = GoalEntity(
          title = title,
          description = description,
          progress = progress,
          deadline = deadline,
          isCompleted = isCompleted || progress >= 100
        )
        repository.insertGoal(goal)
      } else {
        val existing = allGoals.value.firstOrNull { it.id == id }
        if (existing != null) {
          repository.updateGoal(
            existing.copy(
              title = title,
              description = description,
              progress = progress,
              deadline = deadline,
              isCompleted = isCompleted || progress >= 100
            )
          )
        }
      }
    }
  }

  fun deleteGoal(id: Long) {
    viewModelScope.launch {
      repository.deleteGoal(id)
    }
  }

  fun updateAnchorDate(newDate: String) {
    viewModelScope.launch {
      repository.updateAnchorDate(newDate)
    }
  }

  fun updateWakeUpSchedule(
    context: android.content.Context,
    newWakeUpMinutes: Int,
    isAlarmEnabled: Boolean,
    snoozeMinutes: Int = 10,
    updateFutureMasterRoutine: Boolean = true
  ) {
    viewModelScope.launch {
      repository.updateWakeUpSchedule(newWakeUpMinutes, isAlarmEnabled, snoozeMinutes, updateFutureMasterRoutine)
      if (isAlarmEnabled) {
        com.example.alarm.AlarmScheduler.scheduleWakeUpAlarm(
          context,
          newWakeUpMinutes,
          alarmRepeatMode.value,
          alarmCustomDaysMask.value
        )
      } else {
        com.example.alarm.AlarmScheduler.cancelAlarm(context)
      }
    }
  }

  fun toggleAlarm(context: android.content.Context, isEnabled: Boolean) {
    viewModelScope.launch {
      repository.setAlarmEnabled(isEnabled)
      if (isEnabled) {
        com.example.alarm.AlarmScheduler.scheduleWakeUpAlarm(
          context,
          wakeUpMinutes.value,
          alarmRepeatMode.value,
          alarmCustomDaysMask.value
        )
      } else {
        com.example.alarm.AlarmScheduler.cancelAlarm(context)
      }
    }
  }

  fun updateAlarmSound(soundType: String, customUri: String?, soundTitle: String) {
    viewModelScope.launch {
      repository.updateAlarmSound(soundType, customUri, soundTitle)
    }
  }

  fun updateAlarmVolume(volume: Float) {
    viewModelScope.launch {
      repository.updateAlarmVolume(volume)
    }
  }

  fun updateVibrationEnabled(enabled: Boolean) {
    viewModelScope.launch {
      repository.updateVibrationEnabled(enabled)
    }
  }

  fun updateAlarmSnoozeMinutes(snoozeMinutes: Int) {
    viewModelScope.launch {
      repository.updateAlarmSnoozeMinutes(snoozeMinutes)
    }
  }

  fun updateAlarmRepeat(repeatMode: String, customDaysMask: Int = 127, context: android.content.Context? = null) {
    viewModelScope.launch {
      repository.updateAlarmRepeat(repeatMode, customDaysMask)
      if (isAlarmEnabled.value && context != null) {
        com.example.alarm.AlarmScheduler.scheduleWakeUpAlarm(
          context,
          wakeUpMinutes.value,
          repeatMode,
          customDaysMask
        )
      }
    }
  }

  // Export / Import
  suspend fun exportJson(): String = ExportImportHelper.generateJsonBackup(database)
  suspend fun exportCsv(): String = ExportImportHelper.generateCsv(database)
  suspend fun exportMarkdown(): String = ExportImportHelper.generateMarkdown(database)
  suspend fun exportPlainText(): String = ExportImportHelper.generatePlainText(database)
  suspend fun exportAiAnalysis(): String = ExportImportHelper.generateAiAnalysisPrompt(database)

  fun importJson(jsonContent: String, onResult: (Boolean, String) -> Unit) {
    viewModelScope.launch {
      val res = ExportImportHelper.restoreFromJson(database, jsonContent)
      res.onSuccess { msg ->
        // Refresh today's state
        selectDate(_selectedDate.value)
        onResult(true, msg)
      }.onFailure { err ->
        onResult(false, err.message ?: "Failed to parse JSON backup.")
      }
    }
  }
}
