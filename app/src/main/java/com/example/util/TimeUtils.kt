package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object TimeUtils {
  private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
  private val displayDateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US)
  private val shortDateFormat = SimpleDateFormat("MMM d", Locale.US)
  private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.US)
  private val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.US)

  /**
   * Formats minutes from midnight (0..1439) into strictly 12-hour AM/PM format.
   * e.g., 300 -> "5:00 AM", 780 -> "1:00 PM", 1320 -> "10:00 PM"
   */
  fun minutesTo12Hour(totalMinutes: Int): String {
    val normalizedMinutes = ((totalMinutes % 1440) + 1440) % 1440
    val hour24 = normalizedMinutes / 60
    val minute = normalizedMinutes % 60
    val isPm = hour24 >= 12
    val hour12 = when {
      hour24 == 0 -> 12
      hour24 > 12 -> hour24 - 12
      else -> hour24
    }
    return String.format(Locale.US, "%d:%02d %s", hour12, minute, if (isPm) "PM" else "AM")
  }

  /** Alias for minutesTo12Hour ensuring 12-hour format */
  fun formatTime12Hour(totalMinutes: Int): String = minutesTo12Hour(totalMinutes)

  /**
   * Returns natural Hindi time period label and appropriate contextual emoji:
   * 🌅 सुबह (4:00 AM - 6:59 AM)
   * ☀️ सुबह (7:00 AM - 11:59 AM)
   * 🌤️ दोपहर (12:00 PM - 3:59 PM)
   * 🌆 शाम (4:00 PM - 7:59 PM)
   * 🌙 रात (8:00 PM - 3:59 AM)
   */
  fun getHindiTimePeriod(hour24: Int): Pair<String, String> {
    val normHour = ((hour24 % 24) + 24) % 24
    return when (normHour) {
      in 4..6 -> "🌅" to "सुबह"
      in 7..11 -> "☀️" to "सुबह"
      in 12..15 -> "🌤️" to "दोपहर"
      in 16..19 -> "🌆" to "शाम"
      else -> "🌙" to "रात"
    }
  }

  /**
   * Formats minutes into natural Hindi time display:
   * e.g. 300 -> "🌅 सुबह 5:00 बजे", 540 -> "☀️ सुबह 9:00 बजे",
   * 780 -> "🌤️ दोपहर 1:00 बजे", 1020 -> "🌆 शाम 5:00 बजे", 1320 -> "🌙 रात 10:00 बजे"
   */
  fun minutesToHindiTime(totalMinutes: Int): String {
    val normalizedMinutes = ((totalMinutes % 1440) + 1440) % 1440
    val hour24 = normalizedMinutes / 60
    val minute = normalizedMinutes % 60
    val (emoji, period) = getHindiTimePeriod(hour24)
    val hour12 = when {
      hour24 == 0 -> 12
      hour24 > 12 -> hour24 - 12
      else -> hour24
    }
    return String.format(Locale.US, "%s %s %d:%02d बजे", emoji, period, hour12, minute)
  }

  /**
   * Formats a time range in Hindi:
   * e.g. (300, 360) -> "🌅 सुबह 5:00–6:00 बजे"
   */
  fun minutesToHindiTimeRange(startMinutes: Int, endMinutes: Int): String {
    val normStart = ((startMinutes % 1440) + 1440) % 1440
    val normEnd = ((endMinutes % 1440) + 1440) % 1440
    val startH24 = normStart / 60
    val startM = normStart % 60
    val endH24 = normEnd / 60
    val endM = normEnd % 60

    val (startEmoji, startPeriod) = getHindiTimePeriod(startH24)
    val (endEmoji, endPeriod) = getHindiTimePeriod(endH24)

    val startH12 = when {
      startH24 == 0 -> 12
      startH24 > 12 -> startH24 - 12
      else -> startH24
    }
    val endH12 = when {
      endH24 == 0 -> 12
      endH24 > 12 -> endH24 - 12
      else -> endH24
    }

    return if (startPeriod == endPeriod && startEmoji == endEmoji) {
      String.format(Locale.US, "%s %s %d:%02d–%d:%02d बजे", startEmoji, startPeriod, startH12, startM, endH12, endM)
    } else {
      String.format(Locale.US, "%s %s %d:%02d – %s %s %d:%02d बजे", startEmoji, startPeriod, startH12, startM, endEmoji, endPeriod, endH12, endM)
    }
  }

  /**
   * Short Hindi time representation: "5:15 बजे"
   */
  fun minutesToShortHindiTime(totalMinutes: Int): String {
    val normalizedMinutes = ((totalMinutes % 1440) + 1440) % 1440
    val hour24 = normalizedMinutes / 60
    val minute = normalizedMinutes % 60
    val hour12 = when {
      hour24 == 0 -> 12
      hour24 > 12 -> hour24 - 12
      else -> hour24
    }
    return String.format(Locale.US, "%d:%02d बजे", hour12, minute)
  }

  fun parse12HourToMinutes(hour12: Int, minute: Int, isPm: Boolean): Int {
    val h = when {
      isPm && hour12 < 12 -> hour12 + 12
      !isPm && hour12 == 12 -> 0
      else -> hour12
    }
    return (h * 60 + minute) % 1440
  }

  fun getTodayDateString(): String {
    return isoDateFormat.format(Date())
  }

  fun getTodayIsoDate(): String = getTodayDateString()

  fun formatDateDisplay(dateStr: String): String {
    return try {
      val date = isoDateFormat.parse(dateStr) ?: return dateStr
      displayDateFormat.format(date)
    } catch (_: Exception) {
      dateStr
    }
  }

  fun formatDateShort(dateStr: String): String {
    return try {
      val date = isoDateFormat.parse(dateStr) ?: return dateStr
      shortDateFormat.format(date)
    } catch (_: Exception) {
      dateStr
    }
  }

  fun getDayOfWeek(dateStr: String): String {
    return try {
      val date = isoDateFormat.parse(dateStr) ?: return ""
      dayOfWeekFormat.format(date)
    } catch (_: Exception) {
      ""
    }
  }

  fun formatMonthYear(dateStr: String): String {
    return try {
      val date = isoDateFormat.parse(dateStr) ?: return dateStr
      monthYearFormat.format(date)
    } catch (_: Exception) {
      dateStr
    }
  }

  fun shiftDate(dateStr: String, days: Int): String {
    return try {
      val date = isoDateFormat.parse(dateStr) ?: return dateStr
      val cal = Calendar.getInstance()
      cal.time = date
      cal.add(Calendar.DAY_OF_YEAR, days)
      isoDateFormat.format(cal.time)
    } catch (_: Exception) {
      dateStr
    }
  }

  fun daysBetween(startDateStr: String, targetDateStr: String): Long {
    return try {
      val start = isoDateFormat.parse(startDateStr) ?: return 0
      val target = isoDateFormat.parse(targetDateStr) ?: return 0
      val diffMs = target.time - start.time
      TimeUnit.DAYS.convert(diffMs, TimeUnit.MILLISECONDS)
    } catch (_: Exception) {
      0
    }
  }

  /**
   * Calculates continuous 7-day cycle: Day 1, Day 2, ..., Day 7.
   * Cycle continues indefinitely forward and backward.
   */
  fun calculateCycleDay(anchorDateStr: String, targetDateStr: String): Int {
    val diffDays = daysBetween(anchorDateStr, targetDateStr)
    val mod = ((diffDays % 7) + 7) % 7
    return (mod + 1).toInt() // returns 1..7
  }

  /**
   * Returns the list of 7 dates corresponding to the 7-day cycle that contains targetDateStr.
   * Day 1 is index 0, Day 7 is index 6.
   */
  fun getCycleDates(anchorDateStr: String, targetDateStr: String): List<String> {
    val cycleDay = calculateCycleDay(anchorDateStr, targetDateStr)
    val day1Offset = 1 - cycleDay // offset to reach Day 1 of this cycle
    val day1Date = shiftDate(targetDateStr, day1Offset)
    return (0..6).map { shiftDate(day1Date, it) }
  }

  fun getCurrentTimeString(): String {
    val cal = Calendar.getInstance()
    val h = cal.get(Calendar.HOUR_OF_DAY)
    val m = cal.get(Calendar.MINUTE)
    return String.format(Locale.US, "%02d:%02d", h, m)
  }

  fun getCurrentMinutes(): Int {
    val cal = Calendar.getInstance()
    return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
  }

  fun getCurrentMinutesFromMidnight(): Int = getCurrentMinutes()

  fun getNowTimeShort(): String {
    val sdf = SimpleDateFormat("h:mm a", Locale.US)
    return sdf.format(Date())
  }

  fun isFutureDate(dateStr: String, todayStr: String = getTodayDateString()): Boolean {
    return dateStr > todayStr
  }

  fun isFutureTime(timeMinutes: Int, currentMinutes: Int = getCurrentMinutes()): Boolean {
    return timeMinutes > currentMinutes
  }

  fun getCurrentIsoTimestamp(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    return sdf.format(Date())
  }

  /**
   * Formats "HH:mm" (24-hour time string) to natural Hindi time:
   * e.g. "05:00" -> "🌅 सुबह 5:00 बजे", "09:00" -> "☀️ सुबह 9:00 बजे",
   * "13:00" -> "🌤️ दोपहर 1:00 बजे", "17:00" -> "🌆 शाम 5:00 बजे", "22:00" -> "🌙 रात 10:00 बजे"
   */
  fun timeStringToHindi(timeStr: String): String {
    return try {
      val parts = timeStr.trim().split(":")
      if (parts.size >= 2) {
        val h = parts[0].toInt()
        val m = parts[1].toInt()
        val (emoji, period) = getHindiTimePeriod(h)
        val h12 = when {
          h == 0 -> 12
          h > 12 -> h - 12
          else -> h
        }
        String.format(Locale.US, "%s %s %d:%02d बजे", emoji, period, h12, m)
      } else {
        timeStr
      }
    } catch (_: Exception) {
      timeStr
    }
  }

  /**
   * Formats date + time to natural Hindi display:
   * e.g. "2026-09-21", "10:30" -> "21 Sep 2026 • ☀️ सुबह 10:30 बजे"
   */
  fun formatDateTimeHindi(dateStr: String, timeStr: String): String {
    val dateDisplay = formatDateDisplay(dateStr)
    val timeHindi = timeStringToHindi(timeStr)
    return "$dateDisplay • $timeHindi"
  }

  fun formatSecondsToMmSs(totalSeconds: Int): String {
    val mins = (totalSeconds / 60).coerceAtLeast(0)
    val secs = (totalSeconds % 60).coerceAtLeast(0)
    return String.format(Locale.US, "%02d:%02d", mins, secs)
  }
}
