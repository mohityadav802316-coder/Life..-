package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewTimeline
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewTimeline
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.ReflectionScreen
import com.example.ui.screens.ReportScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TodayScreen
import com.example.ui.screens.WeekScreen
import com.example.ui.theme.CardBorderGradient
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.LifeTrackerTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletGlow
import com.example.ui.theme.VioletNeon
import com.example.ui.viewmodel.LifeTrackerViewModel
import com.example.ui.viewmodel.MainTab

class MainActivity : ComponentActivity() {
  private val viewModel: LifeTrackerViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      LifeTrackerTheme {
        LifeTrackerApp(viewModel = viewModel)
      }
    }
  }

  override fun onResume() {
    super.onResume()
    viewModel.onAppResume()
  }

  override fun onPause() {
    super.onPause()
    viewModel.onAppPause()
  }
}

@Composable
fun LifeTrackerApp(viewModel: LifeTrackerViewModel) {
  val currentTab by viewModel.currentTab.collectAsState()

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBackground),
    containerColor = DarkBackground,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    bottomBar = {
      LifeTrackerBottomBar(
        currentTab = currentTab,
        onTabSelected = { tab -> viewModel.setTab(tab) }
      )
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      Crossfade(
        targetState = currentTab,
        animationSpec = tween(durationMillis = 220),
        label = "tab_crossfade"
      ) { tab ->
        when (tab) {
          MainTab.TODAY -> TodayScreen(viewModel = viewModel)
          MainTab.WEEK -> WeekScreen(viewModel = viewModel)
          MainTab.REPORT -> ReportScreen(viewModel = viewModel)
          MainTab.REFLECTION -> ReflectionScreen(viewModel = viewModel)
          MainTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
        }
      }
    }
  }
}

@Composable
fun LifeTrackerBottomBar(
  currentTab: MainTab,
  onTabSelected: (MainTab) -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .fillMaxWidth()
      .navigationBarsPadding()
      .padding(horizontal = 14.dp, vertical = 6.dp)
  ) {
    // Floating Glassmorphic Pill Container
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(68.dp)
        .clip(RoundedCornerShape(26.dp))
        .background(DarkSurfaceElevated.copy(alpha = 0.94f))
        .border(
          width = 1.dp,
          brush = Brush.linearGradient(
            listOf(
              CyanNeon.copy(alpha = 0.4f),
              DarkSurfaceBorder,
              VioletNeon.copy(alpha = 0.3f)
            )
          ),
          shape = RoundedCornerShape(26.dp)
        )
        .padding(horizontal = 6.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
      ) {
        val items = listOf(
          NavigationItemData(MainTab.TODAY, "Today", Icons.Filled.ViewTimeline, Icons.Outlined.ViewTimeline, "tab_today"),
          NavigationItemData(MainTab.WEEK, "Week", Icons.Filled.DateRange, Icons.Outlined.DateRange, "tab_week"),
          NavigationItemData(MainTab.REPORT, "Report", Icons.Filled.Insights, Icons.Outlined.Insights, "tab_report"),
          NavigationItemData(MainTab.REFLECTION, "Journalist", Icons.Filled.RateReview, Icons.Outlined.RateReview, "tab_reflection"),
          NavigationItemData(MainTab.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "tab_settings")
        )

        items.forEach { item ->
          val isSelected = currentTab == item.tab
          val interactionSource = remember { MutableInteractionSource() }

          Box(
            modifier = Modifier
              .weight(1f)
              .height(52.dp)
              .clip(RoundedCornerShape(18.dp))
              .background(
                if (isSelected) CyanNeon.copy(alpha = 0.12f) else DarkSurface.copy(alpha = 0f)
              )
              .clickable(
                interactionSource = interactionSource,
                indication = null
              ) { onTabSelected(item.tab) }
              .testTag(item.testTag),
            contentAlignment = Alignment.Center
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.label,
                tint = if (isSelected) CyanNeon else TextSecondary,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.height(3.dp))
              Text(
                text = item.label,
                color = if (isSelected) CyanNeon else TextMuted,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
              )
              if (isSelected) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                  modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(CyanNeon)
                )
              }
            }
          }
        }
      }
    }
  }
}

private data class NavigationItemData(
  val tab: MainTab,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String
)
