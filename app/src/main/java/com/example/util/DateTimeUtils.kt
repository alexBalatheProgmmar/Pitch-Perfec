package com.example.util

import com.example.data.model.ReminderTiming
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateTimeUtils {

    private val standardDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val standardTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private val displayTimeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    fun getTodayDateString(): String {
        return standardDateFormat.format(Date())
    }

    fun formatDisplayDate(dateStr: String?): String {
        if (dateStr == null) return "No date"
        return try {
            val date = standardDateFormat.parse(dateStr)
            if (date != null) displayDateFormat.format(date) else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    fun formatRelativeTime(dateStr: String?, timeStr: String?): String {
        if (dateStr == null) return "No deadline"
        val lower = dateStr.lowercase()
        if (lower.contains("tomorrow") || lower.contains("next week") || lower.contains("friday")) {
            return dateStr
        }

        return try {
            val parsedDate = standardDateFormat.parse(dateStr) ?: return dateStr
            val now = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val dueTime = parsedDate.time
            val diffDays = TimeUnit.MILLISECONDS.toDays(dueTime - now)

            when {
                diffDays < 0 -> "Overdue by ${-diffDays} day(s)"
                diffDays == 0L -> "Due today"
                diffDays == 1L -> "Due tomorrow"
                diffDays in 2..7 -> "Due in $diffDays days"
                else -> "Due on ${displayDateFormat.format(parsedDate)}"
            }
        } catch (e: Exception) {
            dateStr
        }
    }

    fun calculateTimestamp(dateStr: String?, timeStr: String?): Long? {
        if (dateStr == null) return null
        return try {
            val cal = Calendar.getInstance()
            if (dateStr.equals("today", ignoreCase = true)) {
                // keep today
            } else if (dateStr.equals("tomorrow", ignoreCase = true)) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            } else {
                val parsed = standardDateFormat.parse(dateStr)
                if (parsed != null) {
                    cal.time = parsed
                }
            }

            if (timeStr != null) {
                val parts = timeStr.split(":", " ")
                if (parts.isNotEmpty()) {
                    val hour = parts[0].toIntOrNull() ?: 9
                    val minute = if (parts.size > 1) parts[1].toIntOrNull() ?: 0 else 0
                    cal.set(Calendar.HOUR_OF_DAY, hour)
                    cal.set(Calendar.MINUTE, minute)
                }
            } else {
                cal.set(Calendar.HOUR_OF_DAY, 9)
                cal.set(Calendar.MINUTE, 0)
            }

            cal.timeInMillis
        } catch (e: Exception) {
            null
        }
    }

    fun calculateReminderTimestamp(dueTimestamp: Long?, timing: ReminderTiming): Long? {
        if (dueTimestamp == null || timing == ReminderTiming.NONE) return null
        val cal = Calendar.getInstance().apply { timeInMillis = dueTimestamp }

        return when (timing) {
            ReminderTiming.SAME_DAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 9)
                cal.set(Calendar.MINUTE, 0)
                cal.timeInMillis
            }
            ReminderTiming.ONE_DAY_BEFORE -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                cal.set(Calendar.HOUR_OF_DAY, 9)
                cal.set(Calendar.MINUTE, 0)
                cal.timeInMillis
            }
            ReminderTiming.THREE_DAYS_BEFORE -> {
                cal.add(Calendar.DAY_OF_YEAR, -3)
                cal.set(Calendar.HOUR_OF_DAY, 9)
                cal.set(Calendar.MINUTE, 0)
                cal.timeInMillis
            }
            ReminderTiming.SEVEN_DAYS_BEFORE -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                cal.set(Calendar.HOUR_OF_DAY, 9)
                cal.set(Calendar.MINUTE, 0)
                cal.timeInMillis
            }
            ReminderTiming.CUSTOM -> dueTimestamp
            ReminderTiming.NONE -> null
        }
    }

    fun getGreeting(): Int {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 4..11 -> com.example.R.string.greeting_morning
            in 12..17 -> com.example.R.string.greeting_afternoon
            else -> com.example.R.string.greeting_evening
        }
    }
}
