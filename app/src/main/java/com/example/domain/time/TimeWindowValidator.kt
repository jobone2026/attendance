package com.example.domain.time

import com.example.data.model.School
import java.text.SimpleDateFormat
import java.util.*

enum class AttendanceTimeStatus {
    ON_TIME,
    LATE,
    OUTSIDE_WINDOW,
    CHECKOUT_OPEN
}

data class TimeValidationResult(
    val status: AttendanceTimeStatus,
    val isWithinWindow: Boolean,
    val calculatedTag: String, // "ON_TIME", "LATE"
    val formattedCurrentTime: String,
    val windowDescription: String,
    val message: String
)

class TimeWindowValidator {

    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val displayTimeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun getCurrentDateString(date: Date = Date()): String = dateFormat.format(date)
    fun getCurrentTimeString(date: Date = Date()): String = displayTimeFormat.format(date)

    /**
     * Converts "HH:mm" to minutes from start of day.
     */
    private fun toMinutes(timeStr: String): Int {
        val parts = timeStr.split(":")
        if (parts.size != 2) return 0
        val h = parts[0].trim().toIntOrNull() ?: 0
        val m = parts[1].trim().toIntOrNull() ?: 0
        return h * 60 + m
    }

    /**
     * Validates if current time is within check-in / check-out window.
     */
    fun validateCheckIn(school: School, now: Date = Date()): TimeValidationResult {
        val calendar = Calendar.getInstance().apply { time = now }
        val currentMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)

        val checkinStartMin = toMinutes(school.checkinStart) // e.g. 08:30 -> 510
        val checkinEndMin = toMinutes(school.checkinEnd)     // e.g. 09:30 -> 570
        val lateBufferMin = checkinEndMin + 45               // e.g. up to 10:15 allowed as Late

        val currentStr = displayTimeFormat.format(now)
        val windowDesc = "${school.checkinStart} - ${school.checkinEnd}"

        return when {
            currentMinutes in checkinStartMin..checkinEndMin -> {
                TimeValidationResult(
                    status = AttendanceTimeStatus.ON_TIME,
                    isWithinWindow = true,
                    calculatedTag = "ON_TIME",
                    formattedCurrentTime = currentStr,
                    windowDescription = windowDesc,
                    message = "Within standard morning check-in window (On-Time)"
                )
            }
            currentMinutes in (checkinEndMin + 1)..lateBufferMin -> {
                TimeValidationResult(
                    status = AttendanceTimeStatus.LATE,
                    isWithinWindow = true,
                    calculatedTag = "LATE",
                    formattedCurrentTime = currentStr,
                    windowDescription = windowDesc,
                    message = "After scheduled check-in cut-off (Marked Late)"
                )
            }
            else -> {
                // Allows flexible check-in throughout the day marked as LATE if after morning window
                val tag = if (currentMinutes < checkinStartMin) "ON_TIME" else "LATE"
                TimeValidationResult(
                    status = if (currentMinutes < checkinStartMin) AttendanceTimeStatus.ON_TIME else AttendanceTimeStatus.LATE,
                    isWithinWindow = true,
                    calculatedTag = tag,
                    formattedCurrentTime = currentStr,
                    windowDescription = windowDesc,
                    message = if (currentMinutes < checkinStartMin) "Early check-in" else "Checked in outside regular window (Late)"
                )
            }
        }
    }

    fun validateCheckOut(school: School, now: Date = Date()): TimeValidationResult {
        val currentStr = displayTimeFormat.format(now)
        val windowDesc = "${school.checkoutStart} - ${school.checkoutEnd}"

        return TimeValidationResult(
            status = AttendanceTimeStatus.CHECKOUT_OPEN,
            isWithinWindow = true,
            calculatedTag = "ON_TIME",
            formattedCurrentTime = currentStr,
            windowDescription = windowDesc,
            message = "Check-out window ($windowDesc)"
        )
    }
}
