package com.example.domain.repository

import android.content.Context
import android.location.Location
import android.provider.Settings
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.model.AttendanceRecord
import com.example.data.model.School
import com.example.data.model.Teacher
import com.example.data.remote.*
import com.example.domain.location.GeofenceValidator
import com.example.domain.time.TimeWindowValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AttendanceRepository:
 * Implements an offline-first Repository Pattern abstracting data operations between
 * local Room Database (Single Source of Truth), the MySQL REST API backend
 * (http://13.206.244.237/attendance/mobile-api/), and Firebase Firestore.
 */
class AttendanceRepository(
    private val context: Context,
    private val database: AppDatabase,
    private val apiService: AttendanceApiService = ApiClient.apiService,
    private val firestoreManager: FirestoreManager = FirestoreManager(),
    private val geofenceValidator: GeofenceValidator = GeofenceValidator(),
    private val timeValidator: TimeWindowValidator = TimeWindowValidator()
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _currentTeacherId = MutableStateFlow<String>("TCH_KA_101")
    val currentTeacherId: StateFlow<String> = _currentTeacherId.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // 1. Reactive Streams directly from Room Database (SSOT)
    val currentTeacher: Flow<Teacher?> = _currentTeacherId.flatMapLatest { id ->
        database.teacherDao().getTeacherById(id)
    }

    val currentSchool: Flow<School?> = currentTeacher.flatMapLatest { teacher ->
        val schoolId = teacher?.schoolId ?: "KA_BLR_001"
        database.schoolDao().getSchoolById(schoolId)
    }

    val allTeachers: Flow<List<Teacher>> = database.teacherDao().getAllTeachers()

    val allSchools: Flow<List<School>> = database.schoolDao().getAllSchools()

    val attendanceHistory: Flow<List<AttendanceRecord>> = _currentTeacherId.flatMapLatest { id ->
        database.attendanceDao().getAttendanceForTeacher(id)
    }

    val todayRecord: Flow<AttendanceRecord?> = _currentTeacherId.flatMapLatest { id ->
        val todayStr = timeValidator.getCurrentDateString()
        val recordId = "${id}_$todayStr"
        database.attendanceDao().observeAttendanceByRecordId(recordId)
    }

    val unsyncedCount: Flow<Int> = database.attendanceDao().getUnsyncedCount()

    init {
        scope.launch {
            seedInitialData()
            refreshRemoteData()
            syncPendingRecords()
        }
    }

    /**
     * Seeds initial schools and teacher data into Room if database is empty.
     */
    private suspend fun seedInitialData() = withContext(Dispatchers.IO) {
        val teacherCount = database.teacherDao().getTeacherCount()
        if (teacherCount == 0) {
            database.schoolDao().insertSchools(firestoreManager.defaultSchools)
            database.teacherDao().insertTeachers(firestoreManager.defaultTeachers)
        }
    }

    /**
     * Refreshes Schools and Teachers catalog from the MySQL backend server.
     */
    suspend fun refreshRemoteData() = withContext(Dispatchers.IO) {
        try {
            // Fetch schools from MySQL Mobile API
            val remoteSchools = apiService.getSchools()
            if (remoteSchools.isNotEmpty()) {
                val schools = remoteSchools.map { dto ->
                    School(
                        schoolId = dto.schoolId,
                        name = dto.name,
                        nameKn = dto.nameKn ?: "",
                        district = dto.district ?: "",
                        lat = dto.lat,
                        lng = dto.lng,
                        radiusM = dto.radiusM,
                        checkinStart = dto.checkinStart ?: "08:30",
                        checkinEnd = dto.checkinEnd ?: "09:30",
                        checkoutStart = dto.checkoutStart ?: "16:00",
                        checkoutEnd = dto.checkoutEnd ?: "17:30"
                    )
                }
                database.schoolDao().insertSchools(schools)
            }
        } catch (e: Exception) {
            Log.w("AttendanceRepo", "Failed to fetch schools from MySQL backend: ${e.message}")
        }

        try {
            // Fetch teachers from MySQL Mobile API
            val remoteTeachers = apiService.getTeachers()
            if (remoteTeachers.isNotEmpty()) {
                val teachers = remoteTeachers.map { dto ->
                    Teacher(
                        teacherId = dto.teacherId,
                        name = dto.name,
                        nameKn = dto.nameKn ?: "",
                        phone = dto.phone,
                        schoolId = dto.schoolId ?: "",
                        subject = dto.subject ?: "",
                        embedding = dto.faceEmbedding ?: emptyList(),
                        isFaceEnrolled = dto.isFaceEnrolled,
                        updatedAt = dto.updatedAt ?: System.currentTimeMillis()
                    )
                }
                database.teacherDao().insertTeachers(teachers)
            }
        } catch (e: Exception) {
            Log.w("AttendanceRepo", "Failed to fetch teachers from MySQL backend: ${e.message}")
        }
    }

    /**
     * Select active teacher profile.
     */
    suspend fun selectTeacher(teacherId: String) {
        _currentTeacherId.value = teacherId
    }

    /**
     * Login lookup using mobile number from local Room storage.
     */
    suspend fun loginWithPhone(phone: String): Teacher? = withContext(Dispatchers.IO) {
        val clean = phone.trim().replace(" ", "").replace("-", "")
        val formattedWithCountry = if (clean.startsWith("+91")) clean else "+91$clean"
        val plain = clean.replace("+91", "")

        val teacher = database.teacherDao().getTeacherByPhone(formattedWithCountry)
            ?: database.teacherDao().getTeacherByPhone(plain)
            ?: database.teacherDao().getTeacherByPhone(clean)

        if (teacher != null) {
            _currentTeacherId.value = teacher.teacherId
        }
        teacher
    }

    /**
     * Save biometric face enrollment embedding locally in Room, then sync to server.
     */
    suspend fun saveFaceEnrollment(
        teacherId: String,
        masterEmbedding: List<Float>,
        sampleCount: Int
    ): Boolean = withContext(Dispatchers.IO) {
        val teacherFlow = database.teacherDao().getTeacherById(teacherId)
        val teacher = teacherFlow.firstOrNull() ?: return@withContext false

        val updatedTeacher = teacher.copy(
            embedding = masterEmbedding,
            isFaceEnrolled = true,
            sampleCount = sampleCount,
            enrolledDate = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // 1. Save in local Room DB for instant offline matching
        database.teacherDao().insertTeacher(updatedTeacher)

        // 2. Sync to Firestore in background
        scope.launch {
            firestoreManager.saveTeacherProfile(updatedTeacher)
        }
        true
    }

    /**
     * Records Check-In attendance:
     * 1. Validates geofencing and anti-spoof checks.
     * 2. Writes record immediately to local Room DB (offline-first).
     * 3. Syncs record with MySQL REST API backend and Firestore.
     */
    suspend fun markCheckIn(
        teacher: Teacher,
        school: School,
        location: Location?,
        similarityScore: Float,
        livenessPassed: Boolean
    ): Result<AttendanceRecord> = withContext(Dispatchers.IO) {
        val dateStr = timeValidator.getCurrentDateString()
        val recordId = "${teacher.teacherId}_$dateStr"
        val timeValidation = timeValidator.validateCheckIn(school)
        val geofence = geofenceValidator.validate(location, school)

        if (geofence.isMockLocation) {
            return@withContext Result.failure(Exception("Mock GPS location detected. Attendance cannot be recorded."))
        }

        val deviceId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "device_id"
        } catch (e: Exception) {
            "device_id"
        }

        val record = AttendanceRecord(
            recordId = recordId,
            teacherId = teacher.teacherId,
            schoolId = school.schoolId,
            teacherName = teacher.name,
            date = dateStr,
            checkInTime = timeValidation.formattedCurrentTime,
            checkOutTime = null,
            lat = location?.latitude ?: school.lat,
            lng = location?.longitude ?: school.lng,
            distanceMeters = geofence.distanceMeters.coerceAtLeast(0),
            status = timeValidation.calculatedTag,
            deviceId = deviceId,
            isSynced = false,
            isUnverifiedTime = false,
            similarityScore = similarityScore,
            livenessPassed = livenessPassed,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // 1. Insert into local Room DB immediately (ensures 100% offline capability)
        database.attendanceDao().insertAttendance(record)

        // 2. Attempt push to MySQL REST API backend
        scope.launch {
            try {
                val dto = record.toDto()
                val response = apiService.submitAttendance(dto)
                if (response.isSuccessful && response.body()?.success == true) {
                    database.attendanceDao().updateSyncStatus(recordId, true)
                } else {
                    // Secondary fallback to Firestore
                    val synced = firestoreManager.syncAttendanceRecord(record)
                    if (synced) {
                        database.attendanceDao().updateSyncStatus(recordId, true)
                    }
                }
            } catch (e: Exception) {
                Log.w("AttendanceRepo", "Push to MySQL failed, queuing for offline sync: ${e.message}")
                val synced = firestoreManager.syncAttendanceRecord(record)
                if (synced) {
                    database.attendanceDao().updateSyncStatus(recordId, true)
                }
            }
        }

        Result.success(record)
    }

    /**
     * Records Check-Out attendance with local-first persistence and server sync.
     */
    suspend fun markCheckOut(
        existingRecord: AttendanceRecord,
        location: Location?
    ): Result<AttendanceRecord> = withContext(Dispatchers.IO) {
        val timeStr = timeValidator.getCurrentTimeString()
        val updated = existingRecord.copy(
            checkOutTime = timeStr,
            isSynced = false,
            updatedAt = System.currentTimeMillis()
        )

        // 1. Update in local Room DB
        database.attendanceDao().insertAttendance(updated)

        // 2. Sync to MySQL API and Firestore
        scope.launch {
            try {
                val dto = updated.toDto()
                val response = apiService.submitAttendance(dto)
                if (response.isSuccessful && response.body()?.success == true) {
                    database.attendanceDao().updateSyncStatus(updated.recordId, true)
                } else {
                    val synced = firestoreManager.syncAttendanceRecord(updated)
                    if (synced) {
                        database.attendanceDao().updateSyncStatus(updated.recordId, true)
                    }
                }
            } catch (e: Exception) {
                Log.w("AttendanceRepo", "Checkout push failed: ${e.message}")
                val synced = firestoreManager.syncAttendanceRecord(updated)
                if (synced) {
                    database.attendanceDao().updateSyncStatus(updated.recordId, true)
                }
            }
        }

        Result.success(updated)
    }

    /**
     * Scans and syncs all pending offline records to MySQL backend with bulk sync.
     * Failed records remain unsynced and are re-queued cleanly.
     */
    suspend fun syncPendingRecords(): Int = withContext(Dispatchers.IO) {
        if (_isSyncing.value) return@withContext 0
        _isSyncing.value = true

        try {
            val unsynced = database.attendanceDao().getUnsyncedAttendance()
            if (unsynced.isEmpty()) return@withContext 0

            var successCount = 0
            val dtoList = unsynced.map { it.toDto() }

            try {
                // Call bulk sync endpoint POST /attendance/sync
                val response = apiService.syncOfflineRecords(BulkSyncRequest(dtoList))
                if (response.isSuccessful && response.body() != null) {
                    val syncResult = response.body()!!
                    for (res in syncResult.results) {
                        if (res.success) {
                            database.attendanceDao().updateSyncStatus(res.recordId, true)
                            successCount++
                        } else {
                            Log.w("AttendanceRepo", "Record ${res.recordId} failed server sync: ${res.error}")
                        }
                    }
                } else {
                    // Fallback to per-record Firestore sync
                    for (record in unsynced) {
                        val ok = firestoreManager.syncAttendanceRecord(record)
                        if (ok) {
                            database.attendanceDao().updateSyncStatus(record.recordId, true)
                            successCount++
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("AttendanceRepo", "Bulk sync endpoint error: ${e.message}")
                for (record in unsynced) {
                    val ok = firestoreManager.syncAttendanceRecord(record)
                    if (ok) {
                        database.attendanceDao().updateSyncStatus(record.recordId, true)
                        successCount++
                    }
                }
            }
            successCount
        } finally {
            _isSyncing.value = false
        }
    }

    /**
     * Trigger manual full sync (uploads pending offline attendance and refreshes remote data).
     */
    suspend fun triggerSync(): Int {
        refreshRemoteData()
        return syncPendingRecords()
    }

    /**
     * Export attendance history as formatted CSV.
     */
    suspend fun exportAttendanceCsv(): String = withContext(Dispatchers.IO) {
        val teacherId = _currentTeacherId.value
        val records = database.attendanceDao().getAttendanceForTeacher(teacherId).first()
        val sb = StringBuilder()
        sb.append("Record ID,Teacher Name,Date,Check-In,Check-Out,Status,Distance (m),Synced\n")
        for (r in records) {
            sb.append("${r.recordId},\"${r.teacherName}\",${r.date},${r.checkInTime ?: "-"},${r.checkOutTime ?: "-"},${r.status},${r.distanceMeters},${if (r.isSynced) "YES" else "PENDING"}\n")
        }
        sb.toString()
    }

    private fun AttendanceRecord.toDto(): AttendanceRecordDto {
        return AttendanceRecordDto(
            recordId = recordId,
            teacherId = teacherId,
            schoolId = schoolId,
            date = date,
            checkInTime = checkInTime,
            checkOutTime = checkOutTime,
            lat = lat,
            lng = lng,
            distanceMeters = distanceMeters,
            status = status,
            similarityScore = similarityScore,
            livenessPassed = livenessPassed,
            deviceId = deviceId
        )
    }
}

