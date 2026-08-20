package com.example.domain.location

import android.location.Location
import android.os.Build
import com.example.data.model.School
import kotlin.math.*

data class GeofenceResult(
    val isInside: Boolean,
    val distanceMeters: Int,
    val radiusMeters: Int,
    val isMockLocation: Boolean,
    val userLat: Double,
    val userLng: Double,
    val message: String
)

class GeofenceValidator {

    /**
     * Calculates geodesic distance in meters using Haversine formula.
     */
    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371000.0 // meters

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    /**
     * Checks if location is from a simulated or mock provider.
     */
    fun isMock(location: Location): Boolean {
        val hasMockProvider = location.provider?.contains("mock", ignoreCase = true) == true
        val isMockFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            location.isMock
        } else {
            @Suppress("DEPRECATION")
            location.isFromMockProvider
        }
        return hasMockProvider || isMockFlag
    }

    /**
     * Validates if the user's current GPS fix is within the assigned school geofence.
     */
    fun validate(location: Location?, school: School): GeofenceResult {
        if (location == null) {
            return GeofenceResult(
                isInside = false,
                distanceMeters = -1,
                radiusMeters = school.radiusM,
                isMockLocation = false,
                userLat = 0.0,
                userLng = 0.0,
                message = "Waiting for GPS fix..."
            )
        }

        val isMocked = isMock(location)
        val distance = calculateDistanceMeters(
            lat1 = location.latitude,
            lon1 = location.longitude,
            lat2 = school.lat,
            lon2 = school.lng
        ).toInt()

        val isInside = !isMocked && distance <= school.radiusM

        val message = when {
            isMocked -> "⚠️ Mock GPS Detected! Attendance blocked."
            isInside -> "Within School Boundary (${distance}m / ${school.radiusM}m)"
            else -> "Outside School (${distance}m away, allowed: ${school.radiusM}m)"
        }

        return GeofenceResult(
            isInside = isInside,
            distanceMeters = distance,
            radiusMeters = school.radiusM,
            isMockLocation = isMocked,
            userLat = location.latitude,
            userLng = location.longitude,
            message = message
        )
    }
}
