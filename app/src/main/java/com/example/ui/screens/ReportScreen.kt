package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TaskStatus
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
import com.example.util.TimeUtils
import kotlin.math.roundToInt

@Composable
fun ReportScreen(
  viewModel: LifeTrackerViewModel,
  modifier: Modifier = Modifier
) {
  val allTasks by viewModel.allTasks.collectAsState()
  val anchorDate by viewModel.anchorDate.collectAsState()

  // Daily performance breakdown
  val dailySummaries = remember(allTasks, anchorDate) {
    val grouped = allTasks.groupBy { it.date }
    grouped.toSortedMap().map { (date, tasks) ->
      val total = tasks.size
      val c = tasks.count { it.status == TaskStatus.COMPLETE }
      val p = tasks.count { it.status == TaskStatus.PARTIAL }
      val m = tasks.count { it.status == TaskStatus.MISSED }
      val score = c + (p * 0.5f)
      val pct = if (total > 0) ((score / total) * 100f).roundToInt() else 0
      val cycleDay = TimeUtils.calculateCycleDay(anchorDate, date)
      DailyPerf(date, cycleDay, total, c, p, m, score, pct)
    }
  }

  val totalDays = dailySummaries.size
  val totalTasks = allTasks.size
  val totalComplete = allTasks.count { it.status == TaskStatus.COMPLETE }
  val totalPartial = allTasks.count { it.status == TaskStatus.PARTIAL }
  val totalMissed = allTasks.count { it.status == TaskStatus.MISSED }
  val totalScore = totalComplete + (totalPartial * 0.5f)
  val overallAdherence = if (totalTasks > 0) ((totalScore / totalTasks) * 100f).roundToInt() else 0

  // Category adherence breakdown
  val categoryMetrics = remember(allTasks) {
    allTasks.groupBy { it.category }
      .filter { it.key.isNotBlank() }
      .map { (cat, list) ->
        val c = list.count { it.status == TaskStatus.COMPLETE }
        val p = list.count { it.status == TaskStatus.PARTIAL }
        val score = c + (p * 0.5f)
        val rate = if (list.isNotEmpty()) ((score / list.size) * 100f).roundToInt() else 0
        CategoryMetric(cat, list.size, rate)
      }
      .sortedByDescending { it.adherenceRate }
  }

  // 7-day Cycle Performance Breakdown
  val cycleDayAverages = remember(dailySummaries) {
    (1..7).map { cycleDay ->
      val days = dailySummaries.filter { it.cycleDay == cycleDay }
      val avgPct = if (days.isNotEmpty()) days.map { it.percentage }.average().roundToInt() else 0
      CycleDayMetric(cycleDay, avgPct, days.size)
    }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBackground),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Header
    item {
      Column {
        Text(
          text = "EXECUTIVE METRICS & INTELLIGENCE",
          color = CyanNeon,
          fontSize = 11.sp,
          fontWeight = FontWeight.Black,
          letterSpacing = 1.sp
        )
        Text(
          text = "Adherence & Long-Term Trends",
          color = TextPrimary,
          fontSize = 22.sp,
          fontWeight = FontWeight.Black
        )
      }
    }

    // 1. KPI Cards Grid
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        LuxuryKpiCard(
          title = "Overall Adherence",
          value = "$overallAdherence%",
          subtitle = "All historical days",
          accentColor = CyanNeon,
          icon = Icons.Default.Insights,
          modifier = Modifier.weight(1f)
        )
        LuxuryKpiCard(
          title = "Active History",
          value = "$totalDays days",
          subtitle = "Recorded logs",
          accentColor = VioletNeon,
          icon = Icons.Default.DateRange,
          modifier = Modifier.weight(1f)
        )
      }
      Spacer(modifier = Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        LuxuryKpiCard(
          title = "Completed Tasks",
          value = totalComplete.toString(),
          subtitle = "Full completion (1.0)",
          accentColor = StatusComplete,
          icon = Icons.Default.CheckCircle,
          modifier = Modifier.weight(1f)
        )
        LuxuryKpiCard(
          title = "Total Scored",
          value = String.format(java.util.Locale.US, "%.1f", totalScore),
          subtitle = "Points accumulated",
          accentColor = VioletGlow,
          icon = Icons.Default.AutoGraph,
          modifier = Modifier.weight(1f)
        )
      }
    }

    // 2. Status Ratio Multi-Segment Breakdown
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .background(GlassGradient)
          .border(1.dp, CardBorderGradient, RoundedCornerShape(20.dp))
          .padding(18.dp)
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Status Distribution", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("$totalTasks total tasks", color = TextSecondary, fontSize = 11.sp)
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Multi-color segmented progress bar
          if (totalTasks > 0) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
            ) {
              val completeWeight = (totalComplete.toFloat() / totalTasks).coerceAtLeast(0.01f)
              val partialWeight = (totalPartial.toFloat() / totalTasks).coerceAtLeast(0.01f)
              val missedWeight = (totalMissed.toFloat() / totalTasks).coerceAtLeast(0.01f)

              Box(modifier = Modifier.weight(completeWeight).fillMaxSize().background(StatusComplete))
              Box(modifier = Modifier.weight(partialWeight).fillMaxSize().background(StatusPartial))
              Box(modifier = Modifier.weight(missedWeight).fillMaxSize().background(StatusMissed))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("🟢 Complete: $totalComplete (${if (totalTasks > 0) (totalComplete * 100 / totalTasks) else 0}%)", color = StatusComplete, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
              Text("🟡 Partial: $totalPartial", color = StatusPartial, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
              Text("🔴 Missed: $totalMissed", color = StatusMissed, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }
    }

    // 3. Daily Adherence Trend Canvas Chart (Recent Days)
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .background(GlassGradient)
          .border(1.dp, CardBorderGradient, RoundedCornerShape(20.dp))
          .padding(18.dp)
          .testTag("daily_trend_chart")
      ) {
        Column {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = "Daily Performance Trend",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "Recent history adherence",
                color = TextSecondary,
                fontSize = 11.sp
              )
            }

            // Target Badge
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(StatusComplete.copy(alpha = 0.15f))
                .border(1.dp, StatusComplete.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                text = "70% TARGET",
                color = StatusComplete,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          val recentDays = dailySummaries.takeLast(10)
          if (recentDays.isEmpty()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(140.dp),
              contentAlignment = Alignment.Center
            ) {
              Text("No historical records logged yet.", color = TextMuted, fontSize = 12.sp)
            }
          } else {
            Canvas(
              modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
            ) {
              val width = size.width
              val height = size.height
              val barCount = recentDays.size
              val barSpacing = 10.dp.toPx()
              val availableWidth = width - (barSpacing * (barCount - 1))
              val barWidth = (availableWidth / barCount).coerceAtMost(36.dp.toPx())

              // 70% Target benchmark line
              val benchmarkY = height * (1f - 0.70f)
              drawLine(
                color = StatusComplete.copy(alpha = 0.35f),
                start = Offset(0f, benchmarkY),
                end = Offset(width, benchmarkY),
                strokeWidth = 1.dp.toPx()
              )

              recentDays.forEachIndexed { index, day ->
                val barHeight = (height * (day.percentage / 100f)).coerceAtLeast(4.dp.toPx())
                val x = index * (barWidth + barSpacing)
                val y = height - barHeight

                val barColor = when {
                  day.percentage >= 70 -> Brush.verticalGradient(listOf(CyanNeon, VioletNeon))
                  day.percentage > 0 -> Brush.verticalGradient(listOf(StatusPartial, StatusPartial.copy(alpha = 0.6f)))
                  else -> Brush.verticalGradient(listOf(DarkSurfaceBorder, DarkSurfaceBorder))
                }

                drawRoundRect(
                  brush = barColor,
                  topLeft = Offset(x, y),
                  size = Size(barWidth, barHeight),
                  cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              recentDays.forEach { d ->
                Text(
                  text = TimeUtils.formatDateShort(d.date),
                  color = TextMuted,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Medium
                )
              }
            }
          }
        }
      }
    }

    // 4. Continuous 7-Day Cycle Performance (Day 1..Day 7 averages)
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .background(GlassGradient)
          .border(1.dp, CardBorderGradient, RoundedCornerShape(20.dp))
          .padding(18.dp)
      ) {
        Column {
          Text(
            text = "7-Day Cycle Performance Curve",
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Average adherence by cycle position (Day 1 to 7)",
            color = TextSecondary,
            fontSize = 12.sp
          )

          Spacer(modifier = Modifier.height(14.dp))

          cycleDayAverages.forEach { metric ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Day ${metric.day}",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(54.dp)
              )
              LinearProgressIndicator(
                progress = { (metric.averagePercentage / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                  .weight(1f)
                  .height(7.dp)
                  .clip(RoundedCornerShape(4.dp)),
                color = if (metric.averagePercentage >= 70) CyanNeon else VioletNeon,
                trackColor = DarkSurface
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "${metric.averagePercentage}%",
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(36.dp)
              )
            }
          }
        }
      }
    }

    // 5. Category Adherence
    if (categoryMetrics.isNotEmpty()) {
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GlassGradient)
            .border(1.dp, CardBorderGradient, RoundedCornerShape(20.dp))
            .padding(18.dp)
        ) {
          Column {
            Text(
              text = "Category Adherence Breakdown",
              color = TextPrimary,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            categoryMetrics.forEach { cat ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Text(cat.category, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("${cat.adherenceRate}% (${cat.totalTasks})", color = CyanNeon, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  LinearProgressIndicator(
                    progress = { (cat.adherenceRate / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                      .fillMaxWidth()
                      .height(6.dp)
                      .clip(RoundedCornerShape(3.dp)),
                    color = CyanNeon,
                    trackColor = DarkSurface
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun LuxuryKpiCard(
  title: String,
  value: String,
  subtitle: String,
  accentColor: Color,
  icon: ImageVector,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(18.dp))
      .background(GlassGradient)
      .border(1.dp, CardBorderGradient, RoundedCornerShape(18.dp))
      .padding(14.dp)
  ) {
    Column {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(text = title, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Box(
          modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = 0.15f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(15.dp))
        }
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(text = value, color = accentColor, fontSize = 22.sp, fontWeight = FontWeight.Black)
      Spacer(modifier = Modifier.height(2.dp))
      Text(text = subtitle, color = TextMuted, fontSize = 10.sp)
    }
  }
}

private data class DailyPerf(
  val date: String,
  val cycleDay: Int,
  val totalTasks: Int,
  val completed: Int,
  val partial: Int,
  val missed: Int,
  val score: Float,
  val percentage: Int
)

private data class CategoryMetric(
  val category: String,
  val totalTasks: Int,
  val adherenceRate: Int
)

private data class CycleDayMetric(
  val day: Int,
  val averagePercentage: Int,
  val samplesCount: Int
)
