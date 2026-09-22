package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.DaySummary
import com.example.ui.components.SummaryHeaderCard
import com.example.ui.theme.LifeTrackerTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      LifeTrackerTheme {
        SummaryHeaderCard(
          summary = DaySummary(
            date = "2026-09-21",
            dayOfCycle = 1,
            totalTasks = 12,
            completedCount = 8,
            partialCount = 2,
            missedCount = 2,
            totalScore = 9.0f,
            completionPercentage = 75
          ),
          selectedDate = "2026-09-21",
          isToday = true,
          onPrevDay = {},
          onNextDay = {},
          onJumpToday = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
