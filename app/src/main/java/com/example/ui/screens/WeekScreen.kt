package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DaySummary
import com.example.ui.theme.ActiveCardBorderGradient
import com.example.ui.theme.CardBorderGradient
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.GlassGradient
import com.example.ui.theme.StatusComplete
import com.example.ui.theme.StatusMissed
import com.example.ui.theme.StatusPartial
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletGlow
import com.example.ui.theme.VioletNeon
import com.example.ui.viewmodel.LifeTrackerViewModel
import com.example.ui.viewmodel.MainTab
import com.example.util.TimeUtils
import kotlin.math.roundToInt

@Composable
fun WeekScreen(
  viewModel: LifeTrackerViewModel,
  modifier: Modifier = Modifier
) {
  val cycleSummaries by viewModel.cycleSummaries.collectAsState()
  val selectedDate by viewModel.selectedDate.collectAsState()
  val todayDate = viewModel.todayDate

  // Overall Cycle Stats
  val totalTasks = cycleSummaries.sumOf { it.totalTasks }
  val totalScore = cycleSummaries.sumOf { it.totalScore.toDouble() }.toFloat()
  val avgPercentage = if (totalTasks > 0) ((totalScore / totalTasks.toFloat()) * 100f).roundToInt() else 0

  val startDate = cycleSummaries.firstOrNull()?.date ?: ""
  val endDate = cycleSummaries.lastOrNull()?.date ?: ""

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBackground),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // 1. Cycle Dashboard Banner & Historical Archive Navigation
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(24.dp))
          .background(GlassGradient)
          .border(
            width = 1.dp,
            brush = Brush.linearGradient(
              listOf(CyanNeon.copy(alpha = 0.4f), VioletNeon.copy(alpha = 0.3f), DarkSurfaceBorder)
            ),
            shape = RoundedCornerShape(24.dp)
          )
          .padding(20.dp)
          .testTag("week_overview_banner")
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "CONTINUOUS 7-DAY CYCLE ARCHIVE",
                color = CyanNeon,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
              )
              Spacer(modifier = Modifier.height(3.dp))
              Text(
                text = "${TimeUtils.formatDateShort(startDate)} – ${TimeUtils.formatDateShort(endDate)}",
                color = TextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black
              )
            }

            // Cycle Navigation
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
              IconButton(
                onClick = { viewModel.navigateCycle(-1) },
                modifier = Modifier
                  .size(34.dp)
                  .clip(CircleShape)
                  .background(DarkSurface)
                  .border(1.dp, DarkSurfaceBorder, CircleShape)
                  .testTag("prev_cycle_button")
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = "Previous Cycle",
                  tint = TextSecondary,
                  modifier = Modifier.size(16.dp)
                )
              }

              IconButton(
                onClick = { viewModel.navigateCycle(1) },
                modifier = Modifier
                  .size(34.dp)
                  .clip(CircleShape)
                  .background(DarkSurface)
                  .border(1.dp, DarkSurfaceBorder, CircleShape)
                  .testTag("next_cycle_button")
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                  contentDescription = "Next Cycle",
                  tint = TextSecondary,
                  modifier = Modifier.size(16.dp)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Key Cycle Metric Indicators
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(DarkSurfaceElevated)
              .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(14.dp))
              .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text("Cycle Adherence", color = TextSecondary, fontSize = 11.sp)
              Text("$avgPercentage%", color = CyanNeon, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
            Column {
              Text("Score Accumulated", color = TextSecondary, fontSize = 11.sp)
              Text(
                String.format(java.util.Locale.US, "%.1f / %d pts", totalScore, totalTasks),
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }

    // 2. 7-Day Cards with Circular/Arc Progress
    items(cycleSummaries, key = { it.date }) { daySummary ->
      val isToday = daySummary.date == todayDate
      val isSelected = daySummary.date == selectedDate

      WeekDayArcCard(
        summary = daySummary,
        isToday = isToday,
        isSelected = isSelected,
        onClick = {
          viewModel.selectDate(daySummary.date)
          viewModel.setTab(MainTab.TODAY)
        }
      )
    }
  }
}

@Composable
private fun WeekDayArcCard(
  summary: DaySummary,
  isToday: Boolean,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val animatedProgress by animateFloatAsState(
    targetValue = (summary.completionPercentage / 100f).coerceIn(0f, 1f),
    animationSpec = tween(700),
    label = "progress"
  )

  val cardBorderBrush = when {
    isToday -> ActiveCardBorderGradient
    isSelected -> Brush.linearGradient(listOf(CyanNeon.copy(alpha = 0.6f), DarkSurfaceBorder))
    else -> CardBorderGradient
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(if (isToday) DarkSurfaceElevated else DarkSurface)
      .border(if (isToday) 1.5.dp else 1.dp, cardBorderBrush, RoundedCornerShape(18.dp))
      .clickable { onClick() }
      .padding(14.dp)
      .testTag("week_day_${summary.dayOfCycle}")
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // 1. Mini Circular Arc Progress Donut
      Box(
        modifier = Modifier.size(54.dp),
        contentAlignment = Alignment.Center
      ) {
        Canvas(modifier = Modifier.size(48.dp)) {
          // Track
          drawArc(
            color = DarkSurfaceBorder,
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
          )
          // Progress Arc
          if (animatedProgress > 0f) {
            drawArc(
              brush = Brush.sweepGradient(
                listOf(CyanNeon, VioletGlow, CyanNeon)
              ),
              startAngle = 135f,
              sweepAngle = 270f * animatedProgress,
              useCenter = false,
              style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
            )
          }
        }
        Text(
          text = "${summary.completionPercentage}%",
          color = TextPrimary,
          fontSize = 11.sp,
          fontWeight = FontWeight.Black
        )
      }

      Spacer(modifier = Modifier.width(14.dp))

      // 2. Day Info + Micro Completion Indicators
      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // Day Badge: "DAY 1"
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(if (isToday) CyanNeon else DarkSurfaceBorder)
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = "DAY ${summary.dayOfCycle}",
              color = if (isToday) Color(0xFF00363D) else TextPrimary,
              fontSize = 10.sp,
              fontWeight = FontWeight.Black
            )
          }

          Text(
            text = TimeUtils.formatDateDisplay(summary.date),
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
          )

          if (isToday) {
            Text(
              text = "TODAY",
              color = CyanNeon,
              fontSize = 9.sp,
              fontWeight = FontWeight.Black,
              modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(CyanNeon.copy(alpha = 0.15f))
                .padding(horizontal = 5.dp, vertical = 2.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Small completion indicators
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🟢 ${summary.completedCount}", color = StatusComplete, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("🟡 ${summary.partialCount}", color = StatusPartial, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("🔴 ${summary.missedCount}", color = StatusMissed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }

          Text(
            text = String.format(java.util.Locale.US, "%.1f / %d pts", summary.totalScore, summary.totalTasks),
            color = TextSecondary,
            fontSize = 11.sp
          )
        }
      }

      Spacer(modifier = Modifier.width(6.dp))

      Icon(
        imageVector = Icons.Default.ChevronRight,
        contentDescription = "View Day",
        tint = TextMuted,
        modifier = Modifier.size(18.dp)
      )
    }
  }
}
