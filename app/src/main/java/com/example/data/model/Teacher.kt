package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity representing a Teacher in the attendance system.
 */
@Entity(
    tableName = "teachers",
    indices = [
        Index(value = ["phone"]),
        Index(value = ["schoolId"])
    ]
)
data class Teacher(
    @PrimaryKey
    val teacherId: String = "",
    val name: String = "",
    val nameKn: String = "",
    val phone: String = "",
    val schoolId: String = "",
    val subject: String = "",
    val embedding: List<Float> = emptyList(),
    val enrolledDate: Long = System.currentTimeMillis(),
    val isFaceEnrolled: Boolean = false,
    val sampleCount: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)
