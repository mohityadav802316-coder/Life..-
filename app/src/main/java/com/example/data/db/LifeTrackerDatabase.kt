package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.DailyChallengeEntity
import com.example.data.model.DailyNoteEntity
import com.example.data.model.DailySnapshotEntity
import com.example.data.model.DayTaskEntity
import com.example.data.model.GoalEntity
import com.example.data.model.JournalistConverters
import com.example.data.model.JournalistEntryEntity
import com.example.data.model.JournalistPersonEntity
import com.example.data.model.LifeTrackerConverters
import com.example.data.model.MeditationSessionEntity
import com.example.data.model.PlaylistEntity
import com.example.data.model.PlaylistRuleEntity
import com.example.data.model.PlaylistSongCrossRef
import com.example.data.model.ReflectionEntity
import com.example.data.model.RoutineTemplateEntity
import com.example.data.model.SongEntity
import com.example.data.model.ShortContentRecordEntity
import com.example.data.model.ShortContentDailySummaryEntity
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
    JournalistEntryEntity::class,
    DailyNoteEntity::class,
    MeditationSessionEntity::class,
    SongEntity::class,
    PlaylistEntity::class,
    PlaylistSongCrossRef::class,
    PlaylistRuleEntity::class,
    DailyChallengeEntity::class,
    ShortContentRecordEntity::class,
    ShortContentDailySummaryEntity::class
  ],
  version = 14,
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
  abstract fun dailyNoteDao(): DailyNoteDao
  abstract fun meditationDao(): MeditationDao
  abstract fun musicDao(): MusicDao
  abstract fun dailyChallengeDao(): DailyChallengeDao
  abstract fun shortContentDao(): ShortContentDao

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

    val MIGRATION_5_6 = object : Migration(5, 6) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_settings ADD COLUMN isHapticFeedbackEnabled INTEGER NOT NULL DEFAULT 1")
      }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS daily_notes (
            date TEXT NOT NULL PRIMARY KEY,
            note TEXT NOT NULL DEFAULT '',
            mood TEXT NOT NULL DEFAULT 'NORMAL',
            updatedAt INTEGER NOT NULL
          )
          """.trimIndent()
        )
        db.execSQL("ALTER TABLE day_tasks ADD COLUMN priority TEXT NOT NULL DEFAULT 'NORMAL'")
        db.execSQL("ALTER TABLE routine_templates ADD COLUMN priority TEXT NOT NULL DEFAULT 'NORMAL'")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN smartReminderMinutes INTEGER NOT NULL DEFAULT 0")
      }
    }

    val MIGRATION_7_8 = object : Migration(7, 8) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_settings ADD COLUMN userName TEXT NOT NULL DEFAULT 'मोहित'")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN meditationChimeEnabled INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN meditationVoiceLanguage TEXT NOT NULL DEFAULT 'HI'")
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS meditation_sessions (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            date TEXT NOT NULL,
            type TEXT NOT NULL,
            durationMinutes INTEGER NOT NULL,
            completedSeconds INTEGER NOT NULL,
            isCompleted INTEGER NOT NULL,
            timestamp INTEGER NOT NULL
          )
          """.trimIndent()
        )
      }
    }

    val MIGRATION_8_9 = object : Migration(8, 9) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_settings ADD COLUMN enableQuickAdd INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN enableMorningBrief INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN enableLifeTimeline INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN enablePersonalInsights INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN enableRecoveryMode INTEGER NOT NULL DEFAULT 1")
      }
    }

    val MIGRATION_9_10 = object : Migration(9, 10) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_settings ADD COLUMN dailyEnergyMode TEXT NOT NULL DEFAULT 'NORMAL'")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN dailyEnergyModeDate TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN dailyChallengeEnabled INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN musicAutoRoutineEnabled INTEGER NOT NULL DEFAULT 1")

        db.execSQL("""
          CREATE TABLE IF NOT EXISTS songs (
            id TEXT NOT NULL PRIMARY KEY,
            title TEXT NOT NULL,
            artist TEXT NOT NULL,
            album TEXT NOT NULL,
            durationMs INTEGER NOT NULL,
            contentUri TEXT NOT NULL,
            albumArtUri TEXT,
            isFavorite INTEGER NOT NULL DEFAULT 0,
            lastPlayedAt INTEGER NOT NULL DEFAULT 0,
            playCount INTEGER NOT NULL DEFAULT 0,
            isBuiltIn INTEGER NOT NULL DEFAULT 0,
            mood TEXT NOT NULL DEFAULT 'CALM'
          )
        """.trimIndent())

        db.execSQL("""
          CREATE TABLE IF NOT EXISTS playlists (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            name TEXT NOT NULL,
            description TEXT NOT NULL,
            colorHex TEXT NOT NULL,
            icon TEXT NOT NULL,
            createdAt INTEGER NOT NULL
          )
        """.trimIndent())

        db.execSQL("""
          CREATE TABLE IF NOT EXISTS playlist_songs (
            playlistId INTEGER NOT NULL,
            songId TEXT NOT NULL,
            orderIndex INTEGER NOT NULL DEFAULT 0,
            addedAt INTEGER NOT NULL,
            PRIMARY KEY(playlistId, songId)
          )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_songs_playlistId ON playlist_songs(playlistId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_songs_songId ON playlist_songs(songId)")

        db.execSQL("""
          CREATE TABLE IF NOT EXISTS playlist_rules (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            playlistId INTEGER NOT NULL,
            startMinutes INTEGER NOT NULL,
            endMinutes INTEGER NOT NULL,
            mood TEXT NOT NULL,
            situation TEXT NOT NULL,
            linkedCategory TEXT,
            autoPlay INTEGER NOT NULL DEFAULT 0
          )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_playlist_rules_playlistId ON playlist_rules(playlistId)")

        db.execSQL("""
          CREATE TABLE IF NOT EXISTS daily_challenges (
            date TEXT NOT NULL PRIMARY KEY,
            category TEXT NOT NULL,
            title TEXT NOT NULL,
            description TEXT NOT NULL,
            targetMinutes INTEGER NOT NULL DEFAULT 15,
            isCompleted INTEGER NOT NULL DEFAULT 0,
            completedAt INTEGER NOT NULL DEFAULT 0,
            isSkipped INTEGER NOT NULL DEFAULT 0
          )
        """.trimIndent())
      }
    }

    val MIGRATION_10_11 = object : Migration(10, 11) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_settings ADD COLUMN shortDailyLimit INTEGER NOT NULL DEFAULT 20")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN shortWarning50Enabled INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN shortWarning80Enabled INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN shortWarning100Enabled INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN shortFocusLockIntegration INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN shortTrackingEnabled INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN dashboardPreset TEXT NOT NULL DEFAULT 'CUSTOM'")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN dashboardSectionsOrder TEXT NOT NULL DEFAULT 'NEXT_ACTIVITY,PRIORITY_TASK,TODAYS_ROUTINE,DAILY_PROGRESS,STREAK,ENERGY_MODE,SHORT_CONTENT_TRACKER,MEDITATION,MUSIC,DAILY_CHALLENGE,NEXT_ALARM,RECOVERY_DAY,WEEKLY_SUMMARY'")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN dashboardDisabledSections TEXT NOT NULL DEFAULT ''")

        db.execSQL("""
          CREATE TABLE IF NOT EXISTS short_content_records (
            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            date TEXT NOT NULL,
            appPackage TEXT NOT NULL,
            appName TEXT NOT NULL,
            videoSignature TEXT NOT NULL,
            timestamp INTEGER NOT NULL,
            durationSeconds INTEGER NOT NULL
          )
        """.trimIndent())
        db.execSQL("CREATE INDEX IF NOT EXISTS index_short_content_records_date ON short_content_records(date)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_short_content_records_videoSignature ON short_content_records(videoSignature)")

        db.execSQL("""
          CREATE TABLE IF NOT EXISTS short_content_daily_summaries (
            date TEXT NOT NULL PRIMARY KEY,
            instagramCount INTEGER NOT NULL DEFAULT 0,
            youtubeCount INTEGER NOT NULL DEFAULT 0,
            facebookCount INTEGER NOT NULL DEFAULT 0,
            otherCount INTEGER NOT NULL DEFAULT 0,
            totalCount INTEGER NOT NULL DEFAULT 0,
            totalTimeSeconds INTEGER NOT NULL DEFAULT 0,
            dailyLimit INTEGER NOT NULL DEFAULT 20,
            lastUpdated INTEGER NOT NULL DEFAULT 0
          )
        """.trimIndent())
      }
    }

    val MIGRATION_11_12 = object : Migration(11, 12) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_settings ADD COLUMN isFocusModeActive INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN isFocusScheduleEnabled INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN focusStartTimeMinutes INTEGER NOT NULL DEFAULT 1260")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN focusEndTimeMinutes INTEGER NOT NULL DEFAULT 360")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN focusSessionEndTimestamp INTEGER NOT NULL DEFAULT 0")
      }
    }

    val MIGRATION_12_13 = object : Migration(12, 13) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE user_settings ADD COLUMN isBlackScreenEnabled INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN isBlackScreenOverlayActive INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN isFloatingDotEnabled INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN blackScreenDotX INTEGER NOT NULL DEFAULT 80")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN blackScreenDotY INTEGER NOT NULL DEFAULT 260")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN blackScreenDotSize INTEGER NOT NULL DEFAULT 36")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN blackScreenDotOpacity REAL NOT NULL DEFAULT 0.45")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN blackScreenActivationMethod TEXT NOT NULL DEFAULT 'DOUBLE_TAP'")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN blackScreenExitGesture TEXT NOT NULL DEFAULT 'THREE_FINGER_TAP'")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN blackScreenGestureSensitivity REAL NOT NULL DEFAULT 1.0")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN blackScreenShowClock INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN blackScreenClockFormat24 INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN blackScreenRestoreAfterUnlock INTEGER NOT NULL DEFAULT 1")
        db.execSQL("ALTER TABLE user_settings ADD COLUMN blackScreenRestoreAfterReboot INTEGER NOT NULL DEFAULT 1")
      }
    }

    val MIGRATION_13_14 = object : Migration(13, 14) {
      override fun migrate(db: SupportSQLiteDatabase) {
        // High-performance database query indices for large historical datasets
        db.execSQL("CREATE INDEX IF NOT EXISTS index_day_tasks_date ON day_tasks(date)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_day_tasks_templateId ON day_tasks(templateId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_reflections_date ON reflections(date)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_meditation_sessions_date ON meditation_sessions(date)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_journalist_entries_date ON journalist_entries(date)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_journalist_entries_personId ON journalist_entries(personId)")
      }
    }

    fun getDatabase(context: Context): LifeTrackerDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          LifeTrackerDatabase::class.java,
          "life_tracker_db"
        )
          .addMigrations(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11,
            MIGRATION_11_12,
            MIGRATION_12_13,
            MIGRATION_13_14
          )
          .fallbackToDestructiveMigrationOnDowngrade()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
