package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.DailySnapshotEntity
import com.example.data.model.DayTaskEntity
import com.example.data.model.GoalEntity
import com.example.data.model.JournalistConverters
import com.example.data.model.JournalistEntryEntity
import com.example.data.model.JournalistPersonEntity
import com.example.data.model.LifeTrackerConverters
import com.example.data.model.ReflectionEntity
import com.example.data.model.RoutineTemplateEntity
import com.example.data.model.UserSettingsEntity

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities = [
    DayTaskEntity::class,
    RoutineTemplateEntity::class,
    ReflectionEntity::class,
    GoalEntity::class,
    UserSettingsEntity::class,
    DailySnapshotEntity::class,
    JournalistPersonEntity::class,
    JournalistEntryEntity::class
  ],
  version = 5,
  exportSchema = false
)
@TypeConverters(LifeTrackerConverters::class, JournalistConverters::class)
abstract class LifeTrackerDatabase : RoomDatabase() {
  abstract fun taskDao(): TaskDao
  abstract fun routineDao(): RoutineDao
  abstract fun reflectionDao(): ReflectionDao
  abstract fun goalDao(): GoalDao
  abstract fun userSettingsDao(): UserSettingsDao
  abstract fun dailySnapshotDao(): DailySnapshotDao
  abstract fun journalistPersonDao(): JournalistPersonDao
  abstract fun journalistEntryDao(): JournalistEntryDao

  companion object {
    @Volatile
    private var INSTANCE: LifeTrackerDatabase? = null

    val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_settings ADD COLUMN wakeUpMinutes INTEGER NOT NULL DEFAULT 300")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN isAlarmEnabled INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN snoozeMinutes INTEGER NOT NULL DEFAULT 10")
      }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS daily_snapshots (
            date TEXT NOT NULL PRIMARY KEY,
            dayOfCycle INTEGER NOT NULL,
            totalTasks INTEGER NOT NULL,
            completedCount INTEGER NOT NULL,
            partialCount INTEGER NOT NULL,
            missedCount INTEGER NOT NULL,
            totalScore REAL NOT NULL,
            completionPercentage INTEGER NOT NULL,
            lastUpdated INTEGER NOT NULL
          )
          """.trimIndent()
        )
      }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS journalist_persons (
            id TEXT NOT NULL PRIMARY KEY,
            name TEXT NOT NULL,
            emoji TEXT NOT NULL DEFAULT '👤',
            createdAt INTEGER NOT NULL
          )
          """.trimIndent()
        )
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS journalist_entries (
            id TEXT NOT NULL PRIMARY KEY,
            personId TEXT NOT NULL,
            personName TEXT NOT NULL,
            category TEXT NOT NULL,
            text TEXT NOT NULL,
            timestamp TEXT NOT NULL,
            date TEXT NOT NULL,
            time TEXT NOT NULL,
            context TEXT NOT NULL DEFAULT '',
            intensity TEXT NOT NULL DEFAULT '',
            createdAt INTEGER NOT NULL,
            updatedAt INTEGER NOT NULL
          )
          """.trimIndent()
        )
        // Ensure default self person exists
        val now = System.currentTimeMillis()
        db.execSQL(
          "INSERT OR IGNORE INTO journalist_persons (id, name, emoji, createdAt) VALUES ('self', 'मैं (स्वयं)', '👤', $now)"
        )
      }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_settings ADD COLUMN alarmSoundType TEXT NOT NULL DEFAULT 'DEFAULT'")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN customSoundUri TEXT")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN customSoundTitle TEXT NOT NULL DEFAULT 'High-Tone Digital Alarm'")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN alarmVolume REAL NOT NULL DEFAULT 1.0")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN isVibrationEnabled INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN alarmRepeatMode TEXT NOT NULL DEFAULT 'DAILY'")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN alarmCustomDaysMask INTEGER NOT NULL DEFAULT 127")
      }
    }

    fun getDatabase(context: Context): LifeTrackerDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          LifeTrackerDatabase::class.java,
          "life_tracker_db"
        )
          .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
