package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class SchoolDto(
    @Json(name = "school_id") val schoolId: String,
    @Json(name = "name") val name: String,
    @Json(name = "name_kn") val nameKn: String? = null,
    @Json(name = "district") val district: String? = null,
    @Json(name = "lat") val lat: Double = 12.9716,
    @Json(name = "lng") val lng: Double = 77.5946,
    @Json(name = "radius_m") val radiusM: Int = 200,
    @Json(name = "checkin_start") val checkinStart: String? = "08:30",
    @Json(name = "checkin_end") val checkinEnd: String? = "09:30",
    @Json(name = "checkout_start") val checkoutStart: String? = null,
    @Json(name = "checkout_end") val checkoutEnd: String? = null,
    @Json(name = "updated_at") val updatedAt: Long? = null
)

@JsonClass(generateAdapter = true)
data class TeacherDto(
    @Json(name = "teacher_id") val teacherId: String,
    @Json(name = "name") val name: String,
    @Json(name = "name_kn") val nameKn: String? = null,
    @Json(name = "phone") val phone: String,
    @Json(name = "school_id") val schoolId: String? = null,
    @Json(name = "subject") val subject: String? = null,
    @Json(name = "is_face_enrolled") val isFaceEnrolled: Boolean = false,
    @Json(name = "face_embedding") val faceEmbedding: List<Float>? = null,
    @Json(name = "updated_at") val updatedAt: Long? = null
)

@JsonClass(generateAdapter = true)
data class AttendanceRecordDto(
    @Json(name = "record_id") val recordId: String,
    @Json(name = "teacher_id") val teacherId: String,
    @Json(name = "school_id") val schoolId: String,
    @Json(name = "date") val date: String,
    @Json(name = "check_in_time") val checkInTime: String? = null,
    @Json(name = "check_out_time") val checkOutTime: String? = null,
    @Json(name = "lat") val lat: Double? = null,
    @Json(name = "lng") val lng: Double? = null,
    @Json(name = "distance_meters") val distanceMeters: Int? = null,
    @Json(name = "status") val status: String = "ON_TIME",
    @Json(name = "similarity_score") val similarityScore: Float? = null,
    @Json(name = "liveness_passed") val livenessPassed: Boolean? = true,
    @Json(name = "device_id") val deviceId: String? = null
)

@JsonClass(generateAdapter = true)
data class SingleAttendanceResponse(
    @Json(name = "success") val success: Boolean = false,
    @Json(name = "record_id") val recordId: String? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class SyncRecordResult(
    @Json(name = "record_id") val recordId: String,
    @Json(name = "success") val success: Boolean,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class SyncResponse(
    @Json(name = "synced") val synced: Int = 0,
    @Json(name = "failed") val failed: Int = 0,
    @Json(name = "results") val results: List<SyncRecordResult> = emptyList(),
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class BulkSyncRequest(
    @Json(name = "records") val records: List<AttendanceRecordDto>
)

interface AttendanceApiService {

    @GET("schools")
    suspend fun getSchools(): List<SchoolDto>

    @GET("schools/{schoolId}")
    suspend fun getSchool(@Path("schoolId") schoolId: String): SchoolDto

    @GET("teachers")
    suspend fun getTeachers(@Query("school_id") schoolId: String? = null): List<TeacherDto>

    @GET("teachers/{teacherId}")
    suspend fun getTeacher(@Path("teacherId") teacherId: String): TeacherDto

    @POST("attendance")
    suspend fun submitAttendance(@Body record: AttendanceRecordDto): Response<SingleAttendanceResponse>

    @POST("attendance/sync")
    suspend fun syncOfflineRecords(@Body request: BulkSyncRequest): Response<SyncResponse>

    @GET("attendance")
    suspend fun getAttendance(
        @Query("date") date: String? = null,
        @Query("school_id") schoolId: String? = null,
        @Query("teacher_id") teacherId: String? = null
    ): List<AttendanceRecordDto>
}

object ApiClient {
    private const val BASE_URL = "http://13.206.244.237/attendance/mobile-api/"
    private const val API_KEY = "1094f6d1939e1d2a4a4d2ca1701dc6a5210506df8abc0eb7e321c590c847050c"

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .addHeader("x-api-key", API_KEY)
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val apiService: AttendanceApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(AttendanceApiService::class.java)
    }
}
