package com.example.data.repository

import com.example.data.db.DailySnapshotDao
import com.example.data.db.GoalDao
import com.example.data.db.JournalistEntryDao
import com.example.data.db.JournalistPersonDao
import com.example.data.db.LifeTrackerDatabase
import com.example.data.db.ReflectionDao
import com.example.data.db.RoutineDao
import com.example.data.db.TaskDao
import com.example.data.db.UserSettingsDao
import com.example.data.model.DailySnapshotEntity
import com.example.data.model.DaySummary
import com.example.data.model.DayTaskEntity
import com.example.data.model.GoalEntity
import com.example.data.model.JournalistEntryEntity
import com.example.data.model.JournalistPersonEntity
import com.example.data.model.ReflectionEntity
import com.example.data.model.RoutineTemplateEntity
import com.example.data.model.TaskStatus
import com.example.data.model.UserSettingsEntity
import com.example.util.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class LifeTrackerRepository(private val database: LifeTrackerDatabase) {
  val taskDao: TaskDao = database.taskDao()
  val routineDao: RoutineDao = database.routineDao()
  val reflectionDao: ReflectionDao = database.reflectionDao()
  val goalDao: GoalDao = database.goalDao()
  val userSettingsDao: UserSettingsDao = database.userSettingsDao()
  val dailySnapshotDao: DailySnapshotDao = database.dailySnapshotDao()
  val journalistPersonDao: JournalistPersonDao = database.journalistPersonDao()
  val journalistEntryDao: JournalistEntryDao = database.journalistEntryDao()

  /**
   * Safe SQLite write with automatic retry to guarantee persistence
   * across rapid UI actions or transient lock states.
   */
  private suspend fun <T> safeDbWrite(maxAttempts: Int = 3, action: suspend () -> T): Result<T> {
    var lastException: Throwable? = null
    for (attempt in 1..maxAttempts) {
      try {
        val result = action()
        return Result.success(result)
      } catch (e: Exception) {
        lastException = e
        if (attempt < maxAttempts) {
          delay(attempt * 40L)
        }
      }
    }
    return Result.failure(lastException ?: Exception("Database write failed"))
  }

  suspend fun initializeDefaultsIfNeeded(todayDate: String) = withContext(Dispatchers.IO) {
    var settings = userSettingsDao.getSettingsSync()
    if (settings == null) {
      settings = UserSettingsEntity(
        id = 1,
        anchorDate = todayDate,
        initializedDefaultRoutine = true
      )
      safeDbWrite { userSettingsDao.insertOrUpdate(settings) }
    }

    // Seed master routine if empty
    val routineCount = routineDao.countRoutineTemplates()
    if (routineCount == 0) {
      val defaultRoutines = listOf(
        RoutineTemplateEntity(name = "Wake Up, Hydration & Cold Water", timeMinutes = 300, category = "Morning", orderIndex = 0),
        RoutineTemplateEntity(name = "Morning Workout & Flexibility", timeMinutes = 330, category = "Fitness", orderIndex = 1),
        RoutineTemplateEntity(name = "Shower & Nutritious Breakfast", timeMinutes = 390, category = "Health", orderIndex = 2),
        RoutineTemplateEntity(name = "Day Planning & Top 3 Priorities", timeMinutes = 450, category = "Deep Work", orderIndex = 3),
        RoutineTemplateEntity(name = "Deep Focus Block 1", timeMinutes = 510, category = "Deep Work", orderIndex = 4),
        RoutineTemplateEntity(name = "Review & Communication Sync", timeMinutes = 690, category = "Deep Work", orderIndex = 5),
        RoutineTemplateEntity(name = "Wholesome Lunch & Outdoor Walk", timeMinutes = 750, category = "Health", orderIndex = 6),
        RoutineTemplateEntity(name = "Deep Focus Block 2", timeMinutes = 840, category = "Deep Work", orderIndex = 7),
        RoutineTemplateEntity(name = "Skill Practice & Reading", timeMinutes = 990, category = "Learning", orderIndex = 8),
        RoutineTemplateEntity(name = "Evening Fitness / Outdoor Cardio", timeMinutes = 1080, category = "Fitness", orderIndex = 9),
        RoutineTemplateEntity(name = "Dinner & Mindful Decompression", timeMinutes = 1170, category = "Health", orderIndex = 10),
        RoutineTemplateEntity(name = "Reflection & Daily Review", timeMinutes = 1245, category = "Mindset", orderIndex = 11),
        RoutineTemplateEntity(name = "Night Wind-Down & Screens Off", timeMinutes = 1290, category = "Rest", orderIndex = 12),
        RoutineTemplateEntity(name = "Sleep Prep & Lights Out", timeMinutes = 1320, category = "Rest", orderIndex = 13)
      )
      safeDbWrite { routineDao.insertRoutineTemplates(defaultRoutines) }
    }

    // Ensure fixed default person "मैं (स्वयं)" exists
    val selfPerson = journalistPersonDao.getPersonById("self")
    if (selfPerson == null) {
      safeDbWrite {
        journalistPersonDao.insertPerson(
          JournalistPersonEntity(
            id = "self",
            name = "मैं (स्वयं)",
            emoji = "👤",
            createdAt = System.currentTimeMillis()
          )
        )
      }
    }

    // Ensure today's day tasks exist without altering any historical day
    val cycleDay = TimeUtils.calculateCycleDay(settings.anchorDate, todayDate)
    ensureDayInitialized(todayDate, cycleDay)

    // Ensure historical snapshots exist for all stored dates
    syncAllHistoricalSnapshots()
  }

  suspend fun ensureDayInitialized(date: String, cycleDay: Int) = withContext(Dispatchers.IO) {
    val existing = taskDao.getTasksForDateSync(date)
    if (existing.isEmpty()) {
      val templates = routineDao.getActiveTemplatesSync()
      val maskBit = 1 shl (cycleDay - 1)
      val applicableTemplates = templates.filter { (it.daysMask and maskBit) != 0 }

      val newTasks = applicableTemplates.mapIndexed { index, template ->
        DayTaskEntity(
          date = date,
          templateId = template.id,
          name = template.name,
          timeMinutes = template.timeMinutes,
          category = template.category,
          status = TaskStatus.MISSED,
          notes = template.notes,
          isExtra = false,
          orderIndex = index
        )
      }
      if (newTasks.isNotEmpty()) {
        safeDbWrite { taskDao.insertTasks(newTasks) }
      }
    }
    // Automatically persist the daily snapshot for this day
    syncDailySnapshot(date)
  }

  /**
   * Persists / updates the DailySnapshot for a given date in SQLite.
   */
  suspend fun syncDailySnapshot(date: String) = withContext(Dispatchers.IO) {
    safeDbWrite {
      val tasks = taskDao.getTasksForDateSync(date)
      val settings = userSettingsDao.getSettingsSync()
      val anchor = settings?.anchorDate ?: date
      val cycleDay = TimeUtils.calculateCycleDay(anchor, date)

      val total = tasks.size
      val completed = tasks.count { it.status == TaskStatus.COMPLETE }
      val partial = tasks.count { it.status == TaskStatus.PARTIAL }
      val missed = tasks.count { it.status == TaskStatus.MISSED }
      val totalScore = (completed * 1.0f) + (partial * 0.5f)
      val percentage = if (total > 0) ((totalScore / total.toFloat()) * 100f).roundToInt() else 0

      val snapshot = DailySnapshotEntity(
        date = date,
        dayOfCycle = cycleDay,
        totalTasks = total,
        completedCount = completed,
        partialCount = partial,
        missedCount = missed,
        totalScore = totalScore,
        completionPercentage = percentage,
        lastUpdated = System.currentTimeMillis()
      )
      dailySnapshotDao.insertOrUpdate(snapshot)
    }
  }

  /**
   * Scans all stored dates and guarantees that each day has a persistent snapshot in SQLite.
   */
  suspend fun syncAllHistoricalSnapshots() = withContext(Dispatchers.IO) {
    val allTasks = taskDao.getAllTasksSync()
    val distinctDates = allTasks.map { it.date }.distinct()
    for (d in distinctDates) {
      syncDailySnapshot(d)
    }
  }

  fun getTasksForDate(date: String): Flow<List<DayTaskEntity>> = taskDao.getTasksForDate(date)

  fun getDailySnapshot(date: String): Flow<DailySnapshotEntity?> = dailySnapshotDao.getSnapshotForDate(date)

  fun getAllDailySnapshots(): Flow<List<DailySnapshotEntity>> = dailySnapshotDao.getAllSnapshots()

  fun getDaySummary(date: String, cycleDay: Int): Flow<DaySummary> {
    return taskDao.getTasksForDate(date).map { tasks ->
      val total = tasks.size
      val completed = tasks.count { it.status == TaskStatus.COMPLETE }
      val partial = tasks.count { it.status == TaskStatus.PARTIAL }
      val missed = tasks.count { it.status == TaskStatus.MISSED }
      val totalScore = (completed * 1.0f) + (partial * 0.5f) + (missed * 0.0f)
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
  }

  suspend fun updateTaskStatus(task: DayTaskEntity, newStatus: TaskStatus) = withContext(Dispatchers.IO) {
    safeDbWrite {
      taskDao.updateTask(task.copy(status = newStatus))
    }
    syncDailySnapshot(task.date)
  }

  suspend fun updateTask(task: DayTaskEntity) = withContext(Dispatchers.IO) {
    safeDbWrite {
      taskDao.updateTask(task)
    }
    syncDailySnapshot(task.date)
  }

  suspend fun insertTask(task: DayTaskEntity): Long = withContext(Dispatchers.IO) {
    val id = safeDbWrite {
      taskDao.insertTask(task)
    }.getOrDefault(0L)
    syncDailySnapshot(task.date)
    id
  }

  suspend fun deleteTask(taskId: Long, date: String? = null) = withContext(Dispatchers.IO) {
    safeDbWrite {
      taskDao.deleteTaskById(taskId)
    }
    date?.let { syncDailySnapshot(it) }
  }

  // Routine templates
  fun getAllRoutineTemplates(): Flow<List<RoutineTemplateEntity>> = routineDao.getAllRoutineTemplates()

  suspend fun insertRoutineTemplate(item: RoutineTemplateEntity) = withContext(Dispatchers.IO) {
    safeDbWrite {
      routineDao.insertRoutineTemplate(item)
    }
  }

  suspend fun updateRoutineTemplate(item: RoutineTemplateEntity) = withContext(Dispatchers.IO) {
    safeDbWrite {
      routineDao.updateRoutineTemplate(item)
    }
  }

  suspend fun deleteRoutineTemplate(id: Long) = withContext(Dispatchers.IO) {
    safeDbWrite {
      routineDao.deleteRoutineTemplateById(id)
    }
  }

  // Reflections
  fun getAllReflections(): Flow<List<ReflectionEntity>> = reflectionDao.getAllReflections()

  suspend fun insertReflection(reflection: ReflectionEntity) = withContext(Dispatchers.IO) {
    safeDbWrite {
      reflectionDao.insertReflection(reflection)
    }
  }

  suspend fun updateReflection(reflection: ReflectionEntity) = withContext(Dispatchers.IO) {
    safeDbWrite {
      reflectionDao.updateReflection(reflection)
    }
  }

  suspend fun deleteReflection(id: Long) = withContext(Dispatchers.IO) {
    safeDbWrite {
      reflectionDao.deleteReflectionById(id)
    }
  }

  // Goals
  fun getAllGoals(): Flow<List<GoalEntity>> = goalDao.getAllGoals()

  suspend fun insertGoal(goal: GoalEntity) = withContext(Dispatchers.IO) {
    safeDbWrite {
      goalDao.insertGoal(goal)
    }
  }

  suspend fun updateGoal(goal: GoalEntity) = withContext(Dispatchers.IO) {
    safeDbWrite {
      goalDao.updateGoal(goal)
    }
  }

  suspend fun deleteGoal(id: Long) = withContext(Dispatchers.IO) {
    safeDbWrite {
      goalDao.deleteGoalById(id)
    }
  }

  // User Settings
  fun getUserSettings(): Flow<UserSettingsEntity?> = userSettingsDao.getSettings()

  suspend fun updateAnchorDate(newAnchorDate: String) = withContext(Dispatchers.IO) {
    val current = userSettingsDao.getSettingsSync() ?: UserSettingsEntity(anchorDate = newAnchorDate)
    safeDbWrite {
      userSettingsDao.insertOrUpdate(current.copy(anchorDate = newAnchorDate))
    }
    syncAllHistoricalSnapshots()
  }

  suspend fun updateWakeUpSchedule(
    newWakeUpMinutes: Int,
    isAlarmEnabled: Boolean,
    snoozeMinutes: Int = 10,
    updateFutureMasterRoutine: Boolean = true
  ) = withContext(Dispatchers.IO) {
    val current = userSettingsDao.getSettingsSync() ?: UserSettingsEntity(anchorDate = TimeUtils.getTodayDateString())
    val updated = current.copy(
      wakeUpMinutes = newWakeUpMinutes,
      isAlarmEnabled = isAlarmEnabled,
      snoozeMinutes = snoozeMinutes
    )
    safeDbWrite {
      userSettingsDao.insertOrUpdate(updated)
    }

    if (updateFutureMasterRoutine) {
      val templates = routineDao.getAllRoutineTemplatesSync()
      val wakeUpTemplate = templates.firstOrNull { it.category.equals("Morning", ignoreCase = true) || it.orderIndex == 0 }
      if (wakeUpTemplate != null) {
        safeDbWrite {
          routineDao.updateRoutineTemplate(wakeUpTemplate.copy(timeMinutes = newWakeUpMinutes))
        }
      }
    }
  }

  suspend fun setAlarmEnabled(isEnabled: Boolean) = withContext(Dispatchers.IO) {
    val current = userSettingsDao.getSettingsSync() ?: UserSettingsEntity(anchorDate = TimeUtils.getTodayDateString())
    safeDbWrite {
      userSettingsDao.insertOrUpdate(current.copy(isAlarmEnabled = isEnabled))
    }
  }

  suspend fun updateAlarmSound(
    soundType: String,
    customUri: String?,
    soundTitle: String
  ) = withContext(Dispatchers.IO) {
    val current = userSettingsDao.getSettingsSync() ?: UserSettingsEntity(anchorDate = TimeUtils.getTodayDateString())
    safeDbWrite {
      userSettingsDao.insertOrUpdate(
        current.copy(
          alarmSoundType = soundType,
          customSoundUri = customUri,
          customSoundTitle = soundTitle
        )
      )
    }
  }

  suspend fun updateAlarmVolume(volume: Float) = withContext(Dispatchers.IO) {
    val current = userSettingsDao.getSettingsSync() ?: UserSettingsEntity(anchorDate = TimeUtils.getTodayDateString())
    safeDbWrite {
      userSettingsDao.insertOrUpdate(current.copy(alarmVolume = volume.coerceIn(0.05f, 1.0f)))
    }
  }

  suspend fun updateVibrationEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
    val current = userSettingsDao.getSettingsSync() ?: UserSettingsEntity(anchorDate = TimeUtils.getTodayDateString())
    safeDbWrite {
      userSettingsDao.insertOrUpdate(current.copy(isVibrationEnabled = enabled))
    }
  }

  suspend fun updateAlarmSnoozeMinutes(snoozeMinutes: Int) = withContext(Dispatchers.IO) {
    val current = userSettingsDao.getSettingsSync() ?: UserSettingsEntity(anchorDate = TimeUtils.getTodayDateString())
    safeDbWrite {
      userSettingsDao.insertOrUpdate(current.copy(snoozeMinutes = snoozeMinutes))
    }
  }

  suspend fun updateAlarmRepeat(repeatMode: String, customDaysMask: Int = 127) = withContext(Dispatchers.IO) {
    val current = userSettingsDao.getSettingsSync() ?: UserSettingsEntity(anchorDate = TimeUtils.getTodayDateString())
    safeDbWrite {
      userSettingsDao.insertOrUpdate(
        current.copy(
          alarmRepeatMode = repeatMode,
          alarmCustomDaysMask = customDaysMask
        )
      )
    }
  }

  fun getAllTasks(): Flow<List<DayTaskEntity>> = taskDao.getAllTasks()

  // Journalist Persons
  fun getAllJournalistPersons(): Flow<List<JournalistPersonEntity>> = journalistPersonDao.getAllPersons()

  suspend fun insertJournalistPerson(person: JournalistPersonEntity) = withContext(Dispatchers.IO) {
    safeDbWrite {
      journalistPersonDao.insertPerson(person)
    }
  }

  suspend fun updateJournalistPerson(person: JournalistPersonEntity) = withContext(Dispatchers.IO) {
    safeDbWrite {
      journalistPersonDao.updatePerson(person)
    }
  }

  suspend fun deleteJournalistPerson(id: String) = withContext(Dispatchers.IO) {
    if (id == "self") return@withContext // Self can never be deleted
    safeDbWrite {
      journalistPersonDao.deletePersonById(id)
      journalistEntryDao.deleteEntriesForPerson(id)
    }
  }

  // Journalist Entries
  fun getAllJournalistEntries(): Flow<List<JournalistEntryEntity>> = journalistEntryDao.getAllEntries()

  fun getJournalistEntriesForPerson(personId: String): Flow<List<JournalistEntryEntity>> =
    journalistEntryDao.getEntriesForPerson(personId)

  suspend fun insertJournalistEntry(entry: JournalistEntryEntity) = withContext(Dispatchers.IO) {
    safeDbWrite {
      journalistEntryDao.insertEntry(entry)
    }
  }

  suspend fun updateJournalistEntry(entry: JournalistEntryEntity) = withContext(Dispatchers.IO) {
    safeDbWrite {
      journalistEntryDao.updateEntry(entry)
    }
  }

  suspend fun deleteJournalistEntry(id: String) = withContext(Dispatchers.IO) {
    safeDbWrite {
      journalistEntryDao.deleteEntryById(id)
    }
  }
}
