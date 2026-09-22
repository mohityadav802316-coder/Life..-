package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DailySnapshotEntity
import com.example.data.model.DayTaskEntity
import com.example.data.model.GoalEntity
import com.example.data.model.JournalistEntryEntity
import com.example.data.model.JournalistPersonEntity
import com.example.data.model.ReflectionEntity
import com.example.data.model.RoutineTemplateEntity
import com.example.data.model.UserSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
  @Query("SELECT * FROM day_tasks WHERE date = :date ORDER BY timeMinutes ASC, orderIndex ASC")
  fun getTasksForDate(date: String): Flow<List<DayTaskEntity>>

  @Query("SELECT * FROM day_tasks WHERE date = :date ORDER BY timeMinutes ASC, orderIndex ASC")
  suspend fun getTasksForDateSync(date: String): List<DayTaskEntity>

  @Query("SELECT * FROM day_tasks WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC, timeMinutes ASC")
  fun getTasksBetweenDates(startDate: String, endDate: String): Flow<List<DayTaskEntity>>

  @Query("SELECT * FROM day_tasks ORDER BY date ASC, timeMinutes ASC")
  fun getAllTasks(): Flow<List<DayTaskEntity>>

  @Query("SELECT * FROM day_tasks ORDER BY date ASC, timeMinutes ASC")
  suspend fun getAllTasksSync(): List<DayTaskEntity>

  @Query("SELECT DISTINCT date FROM day_tasks ORDER BY date DESC")
  fun getDistinctDates(): Flow<List<String>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTask(task: DayTaskEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertTasks(tasks: List<DayTaskEntity>)

  @Update
  suspend fun updateTask(task: DayTaskEntity)

  @Delete
  suspend fun deleteTask(task: DayTaskEntity)

  @Query("DELETE FROM day_tasks WHERE id = :id")
  suspend fun deleteTaskById(id: Long)

  @Query("DELETE FROM day_tasks WHERE date = :date")
  suspend fun deleteTasksForDate(date: String)

  @Query("DELETE FROM day_tasks")
  suspend fun deleteAllTasks()
}

@Dao
interface RoutineDao {
  @Query("SELECT * FROM routine_templates ORDER BY timeMinutes ASC, orderIndex ASC")
  fun getAllRoutineTemplates(): Flow<List<RoutineTemplateEntity>>

  @Query("SELECT * FROM routine_templates ORDER BY timeMinutes ASC, orderIndex ASC")
  suspend fun getAllRoutineTemplatesSync(): List<RoutineTemplateEntity>

  @Query("SELECT * FROM routine_templates WHERE isActive = 1 ORDER BY timeMinutes ASC, orderIndex ASC")
  fun getActiveTemplates(): Flow<List<RoutineTemplateEntity>>

  @Query("SELECT * FROM routine_templates WHERE isActive = 1 ORDER BY timeMinutes ASC, orderIndex ASC")
  suspend fun getActiveTemplatesSync(): List<RoutineTemplateEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRoutineTemplate(template: RoutineTemplateEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRoutineTemplates(templates: List<RoutineTemplateEntity>)

  @Update
  suspend fun updateRoutineTemplate(template: RoutineTemplateEntity)

  @Delete
  suspend fun deleteRoutineTemplate(template: RoutineTemplateEntity)

  @Query("DELETE FROM routine_templates WHERE id = :id")
  suspend fun deleteRoutineTemplateById(id: Long)

  @Query("SELECT COUNT(*) FROM routine_templates")
  suspend fun countRoutineTemplates(): Int

  @Query("DELETE FROM routine_templates")
  suspend fun deleteAllTemplates()
}

@Dao
interface ReflectionDao {
  @Query("SELECT * FROM reflections ORDER BY date DESC, timestamp DESC")
  fun getAllReflections(): Flow<List<ReflectionEntity>>

  @Query("SELECT * FROM reflections ORDER BY date DESC, timestamp DESC")
  suspend fun getAllReflectionsSync(): List<ReflectionEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertReflection(reflection: ReflectionEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertReflections(reflections: List<ReflectionEntity>)

  @Update
  suspend fun updateReflection(reflection: ReflectionEntity)

  @Delete
  suspend fun deleteReflection(reflection: ReflectionEntity)

  @Query("DELETE FROM reflections WHERE id = :id")
  suspend fun deleteReflectionById(id: Long)

  @Query("DELETE FROM reflections")
  suspend fun deleteAllReflections()
}

@Dao
interface GoalDao {
  @Query("SELECT * FROM goals ORDER BY isCompleted ASC, createdAt DESC")
  fun getAllGoals(): Flow<List<GoalEntity>>

  @Query("SELECT * FROM goals ORDER BY isCompleted ASC, createdAt DESC")
  suspend fun getAllGoalsSync(): List<GoalEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertGoal(goal: GoalEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertGoals(goals: List<GoalEntity>)

  @Update
  suspend fun updateGoal(goal: GoalEntity)

  @Delete
  suspend fun deleteGoal(goal: GoalEntity)

  @Query("DELETE FROM goals WHERE id = :id")
  suspend fun deleteGoalById(id: Long)

  @Query("DELETE FROM goals")
  suspend fun deleteAllGoals()
}

@Dao
interface UserSettingsDao {
  @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
  fun getSettings(): Flow<UserSettingsEntity?>

  @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
  suspend fun getSettingsSync(): UserSettingsEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(settings: UserSettingsEntity)
}

@Dao
interface DailySnapshotDao {
  @Query("SELECT * FROM daily_snapshots WHERE date = :date LIMIT 1")
  fun getSnapshotForDate(date: String): Flow<DailySnapshotEntity?>

  @Query("SELECT * FROM daily_snapshots WHERE date = :date LIMIT 1")
  suspend fun getSnapshotForDateSync(date: String): DailySnapshotEntity?

  @Query("SELECT * FROM daily_snapshots WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC")
  fun getSnapshotsBetweenDates(startDate: String, endDate: String): Flow<List<DailySnapshotEntity>>

  @Query("SELECT * FROM daily_snapshots ORDER BY date DESC")
  fun getAllSnapshots(): Flow<List<DailySnapshotEntity>>

  @Query("SELECT * FROM daily_snapshots ORDER BY date DESC")
  suspend fun getAllSnapshotsSync(): List<DailySnapshotEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(snapshot: DailySnapshotEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdateAll(snapshots: List<DailySnapshotEntity>)

  @Query("DELETE FROM daily_snapshots WHERE date = :date")
  suspend fun deleteSnapshotForDate(date: String)

  @Query("DELETE FROM daily_snapshots")
  suspend fun deleteAllSnapshots()
}

@Dao
interface JournalistPersonDao {
  @Query("SELECT * FROM journalist_persons ORDER BY CASE WHEN id = 'self' THEN 0 ELSE 1 END, createdAt ASC")
  fun getAllPersons(): Flow<List<JournalistPersonEntity>>

  @Query("SELECT * FROM journalist_persons ORDER BY CASE WHEN id = 'self' THEN 0 ELSE 1 END, createdAt ASC")
  suspend fun getAllPersonsSync(): List<JournalistPersonEntity>

  @Query("SELECT * FROM journalist_persons WHERE id = :id LIMIT 1")
  suspend fun getPersonById(id: String): JournalistPersonEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPerson(person: JournalistPersonEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertPersons(persons: List<JournalistPersonEntity>)

  @Update
  suspend fun updatePerson(person: JournalistPersonEntity)

  @Delete
  suspend fun deletePerson(person: JournalistPersonEntity)

  @Query("DELETE FROM journalist_persons WHERE id = :id AND id != 'self'")
  suspend fun deletePersonById(id: String)
}

@Dao
interface JournalistEntryDao {
  @Query("SELECT * FROM journalist_entries ORDER BY date DESC, time DESC, createdAt DESC")
  fun getAllEntries(): Flow<List<JournalistEntryEntity>>

  @Query("SELECT * FROM journalist_entries ORDER BY date DESC, time DESC, createdAt DESC")
  suspend fun getAllEntriesSync(): List<JournalistEntryEntity>

  @Query("SELECT * FROM journalist_entries WHERE personId = :personId ORDER BY date DESC, time DESC, createdAt DESC")
  fun getEntriesForPerson(personId: String): Flow<List<JournalistEntryEntity>>

  @Query("SELECT * FROM journalist_entries WHERE personId = :personId ORDER BY date DESC, time DESC, createdAt DESC")
  suspend fun getEntriesForPersonSync(personId: String): List<JournalistEntryEntity>

  @Query("SELECT * FROM journalist_entries WHERE id = :id LIMIT 1")
  suspend fun getEntryById(id: String): JournalistEntryEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertEntry(entry: JournalistEntryEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertEntries(entries: List<JournalistEntryEntity>)

  @Update
  suspend fun updateEntry(entry: JournalistEntryEntity)

  @Delete
  suspend fun deleteEntry(entry: JournalistEntryEntity)

  @Query("DELETE FROM journalist_entries WHERE id = :id")
  suspend fun deleteEntryById(id: String)

  @Query("DELETE FROM journalist_entries WHERE personId = :personId")
  suspend fun deleteEntriesForPerson(personId: String)
}
