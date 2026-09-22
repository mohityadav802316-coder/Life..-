package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Life Tracker", appName)
  }

  @Test
  fun `verify 12 hour time format and cycle calculation`() {
    assertEquals("5:00 AM", com.example.util.TimeUtils.minutesTo12Hour(300))
    assertEquals("12:00 PM", com.example.util.TimeUtils.minutesTo12Hour(720))
    assertEquals("10:00 PM", com.example.util.TimeUtils.minutesTo12Hour(1320))
    
    // Continuous cycle 1..7 test
    val cycleDay = com.example.util.TimeUtils.calculateCycleDay("2026-09-21", "2026-09-21")
    assertEquals(1, cycleDay)
  }

  @Test
  fun `verify Hindi natural time labels`() {
    assertEquals("🌅 सुबह 5:00 बजे", com.example.util.TimeUtils.minutesToHindiTime(300))
    assertEquals("☀️ सुबह 9:00 बजे", com.example.util.TimeUtils.minutesToHindiTime(540))
    assertEquals("🌤️ दोपहर 1:00 बजे", com.example.util.TimeUtils.minutesToHindiTime(780))
    assertEquals("🌆 शाम 5:00 बजे", com.example.util.TimeUtils.minutesToHindiTime(1020))
    assertEquals("🌙 रात 10:00 बजे", com.example.util.TimeUtils.minutesToHindiTime(1320))

    // Range test
    assertEquals("🌅 सुबह 5:00–6:00 बजे", com.example.util.TimeUtils.minutesToHindiTimeRange(300, 360))
  }

  @Test
  fun `verify Room persistence and date-wise isolation`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(context, com.example.data.db.LifeTrackerDatabase::class.java).build()
    val repo = com.example.data.repository.LifeTrackerRepository(db)

    // Day 1: Initialize defaults and day 1
    repo.initializeDefaultsIfNeeded("2026-09-21")
    val day1Tasks = repo.taskDao.getTasksForDateSync("2026-09-21")
    org.junit.Assert.assertTrue(day1Tasks.isNotEmpty())

    // Update first task on Day 1 to COMPLETE
    val taskToComplete = day1Tasks.first()
    repo.updateTaskStatus(taskToComplete, com.example.data.model.TaskStatus.COMPLETE)

    val updatedDay1 = repo.taskDao.getTasksForDateSync("2026-09-21")
    assertEquals(com.example.data.model.TaskStatus.COMPLETE, updatedDay1.first { it.id == taskToComplete.id }.status)

    // Verify snapshot was created automatically
    val snapshotDay1 = repo.dailySnapshotDao.getSnapshotForDateSync("2026-09-21")
    org.junit.Assert.assertNotNull(snapshotDay1)
    assertEquals(1, snapshotDay1?.completedCount)

    // Day 2 initialization should NOT overwrite Day 1
    repo.ensureDayInitialized("2026-09-22", 2)
    val day1AfterDay2 = repo.taskDao.getTasksForDateSync("2026-09-21")
    assertEquals(com.example.data.model.TaskStatus.COMPLETE, day1AfterDay2.first { it.id == taskToComplete.id }.status)

    db.close()
  }
}
