package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity representing a Teacher's Daily Attendance Record.
 * Indexed by teacherId, date, and sync status for instant local querying and offline sync queues.
 */
@Entity(
    tableName = "attendance",
    indices = [
        Index(value = ["teacherId"]),
        Index(value = ["date"]),
        Index(value = ["isSynced"])
    ]
)
data class AttendanceRecord(
    @PrimaryKey
    val recordId: String = "", // Format: "{teacherId}_{date}"
    val teacherId: String = "",
    val schoolId: String = "",
    val teacherName: String = "",
    val date: String = "", // "YYYY-MM-DD"
    val checkInTime: String? = null, // e.g., "08:42 AM"
    val checkOutTime: String? = null, // e.g., "04:35 PM"
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val distanceMeters: Int = 0,
    val status: String = "ON_TIME", // "ON_TIME", "LATE", "EARLY_CHECKOUT", "ABSENT"
    val deviceId: String = "",
    val isSynced: Boolean = false,
    val isUnverifiedTime: Boolean = false,
    val similarityScore: Float = 0.0f,
    val livenessPassed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
