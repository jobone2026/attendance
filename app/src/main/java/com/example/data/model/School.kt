package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity representing a School / Examination Centre with Geofence Parameters.
 */
@Entity(
    tableName = "schools",
    indices = [
        Index(value = ["district"])
    ]
)
data class School(
    @PrimaryKey
    val schoolId: String = "",
    val name: String = "",
    val nameKn: String = "",
    val district: String = "",
    val lat: Double = 12.9716,
    val lng: Double = 77.5946,
    val radiusM: Int = 150, // Geofence radius in meters (e.g. 150-250m)
    val checkinStart: String = "08:30",
    val checkinEnd: String = "09:30",
    val checkoutStart: String = "16:00",
    val checkoutEnd: String = "17:30"
)
