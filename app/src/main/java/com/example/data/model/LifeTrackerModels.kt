package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

enum class TaskStatus(val score: Float, val label: String) {
  COMPLETE(1.0f, "Complete"),
  PARTIAL(0.5f, "Partial"),
  MISSED(0.0f, "Missed");

  companion object {
    fun fromString(value: String): TaskStatus =
      entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MISSED
  }
}

enum class ReflectionCategory(val label: String, val iconPrefix: String) {
  MISTAKE("गलती / कमी", "❌"),
  GOOD_DEED("अच्छाई", "✅"),
  OBSERVATION("Observation", "🔎");

  val fullDisplay: String
    get() = "$iconPrefix $label"

  companion object {
    fun fromString(value: String): ReflectionCategory =
      entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OBSERVATION
  }
}

@Entity(tableName = "day_tasks")
data class DayTaskEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val date: String, // YYYY-MM-DD
  val templateId: Long? = null, // null if Extra Task
  val name: String,
  val timeMinutes: Int, // Minutes from midnight (e.g., 300 = 5:00 AM)
  val category: String = "Routine",
  val status: TaskStatus = TaskStatus.MISSED,
  val notes: String = "",
  val isExtra: Boolean = false, // True for Today Extra Tasks
  val orderIndex: Int = 0
)

@Entity(tableName = "routine_templates")
data class RoutineTemplateEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val timeMinutes: Int, // Minutes from midnight
  val category: String = "Routine",
  val notes: String = "",
  val orderIndex: Int = 0,
  val daysMask: Int = 127, // Bitmask for 7 days (1..7 all active = 127)
  val isActive: Boolean = true
)

@Entity(tableName = "reflections")
data class ReflectionEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val title: String,
  val description: String = "",
  val date: String, // YYYY-MM-DD
  val timestamp: Long = System.currentTimeMillis(),
  val category: ReflectionCategory = ReflectionCategory.OBSERVATION,
  val tags: String = "" // Comma-separated tags
)

@Entity(tableName = "goals")
data class GoalEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val title: String,
  val description: String = "",
  val progress: Int = 0, // 0..100%
  val deadline: String? = null,
  val isCompleted: Boolean = false,
  val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
  @PrimaryKey val id: Int = 1,
  val anchorDate: String, // Date of Day 1 of Cycle
  val initializedDefaultRoutine: Boolean = true,
  val wakeUpMinutes: Int = 300, // Default 5:00 AM (e.g. 180 = 3:00 AM, 240 = 4:00 AM)
  val isAlarmEnabled: Boolean = false,
  val snoozeMinutes: Int = 10,
  val alarmSoundType: String = "DEFAULT", // "DEFAULT" or "CUSTOM"
  val customSoundUri: String? = null,
  val customSoundTitle: String = "High-Tone Digital Alarm",
  val alarmVolume: Float = 1.0f,
  val isVibrationEnabled: Boolean = true,
  val alarmRepeatMode: String = "DAILY", // "DAILY", "WEEKDAYS", "CUSTOM"
  val alarmCustomDaysMask: Int = 127
)

@Entity(tableName = "daily_snapshots")
data class DailySnapshotEntity(
  @PrimaryKey val date: String, // YYYY-MM-DD
  val dayOfCycle: Int,
  val totalTasks: Int,
  val completedCount: Int,
  val partialCount: Int,
  val missedCount: Int,
  val totalScore: Float,
  val completionPercentage: Int,
  val lastUpdated: Long = System.currentTimeMillis()
)

data class DaySummary(
  val date: String,
  val dayOfCycle: Int,
  val totalTasks: Int,
  val completedCount: Int,
  val partialCount: Int,
  val missedCount: Int,
  val totalScore: Float,
  val completionPercentage: Int
)

class LifeTrackerConverters {
  @TypeConverter
  fun fromTaskStatus(status: TaskStatus): String = status.name

  @TypeConverter
  fun toTaskStatus(value: String): TaskStatus = TaskStatus.fromString(value)

  @TypeConverter
  fun fromReflectionCategory(category: ReflectionCategory): String = category.name

  @TypeConverter
  fun toReflectionCategory(value: String): ReflectionCategory = ReflectionCategory.fromString(value)
}
