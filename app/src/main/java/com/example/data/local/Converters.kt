package com.example.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromFloatList(list: List<Float>?): String {
        if (list == null || list.isEmpty()) return ""
        return list.joinToString(",")
    }

    @TypeConverter
    fun toFloatList(data: String?): List<Float> {
        if (data.isNullOrBlank()) return emptyList()
        return try {
            data.split(",").mapNotNull { it.trim().toFloatOrNull() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
