package com.example.data.remote

import android.util.Log
import com.example.data.model.AttendanceRecord
import com.example.data.model.School
import com.example.data.model.Teacher
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class FirestoreManager {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            val db = FirebaseFirestore.getInstance()
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            db.firestoreSettings = settings
            db
        } catch (e: Exception) {
            Log.w("FirestoreManager", "Firestore not configured or initialized: ${e.message}")
            null
        }
    }

    private val auth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.w("FirestoreManager", "FirebaseAuth not configured: ${e.message}")
            null
        }
    }

    // Default Seed Schools for Karnataka Districts
    val defaultSchools = listOf(
        School(
            schoolId = "KA_BLR_001",
            name = "Govt. High School Malleshwaram, Bengaluru",
            nameKn = "ಸರ್ಕಾರಿ ಪ್ರೌಢಶಾಲೆ ಮಲ್ಲೇಶ್ವರಂ, ಬೆಂಗಳೂರು",
            district = "Bengaluru Urban",
            lat = 13.0031,
            lng = 77.5645,
            radiusM = 200,
            checkinStart = "08:30",
            checkinEnd = "09:30",
            checkoutStart = "16:00",
            checkoutEnd = "17:30"
        ),
        School(
            schoolId = "KA_MYS_002",
            name = "Govt. PU College Saraswathipuram, Mysuru",
            nameKn = "ಸರ್ಕಾರಿ ಪಿಯು ಕಾಲೇಜು ಸರಸ್ವತಿಪುರಂ, ಮೈಸೂರು",
            district = "Mysuru",
            lat = 12.3051,
            lng = 76.6346,
            radiusM = 250,
            checkinStart = "08:30",
            checkinEnd = "09:30",
            checkoutStart = "16:00",
            checkoutEnd = "17:30"
        ),
        School(
            schoolId = "KA_DWD_003",
            name = "Govt. Higher Primary School, Dharwad",
            nameKn = "ಸರ್ಕಾರಿ ಹಿರಿಯ ಪ್ರಾಥಮಿಕ ಶಾಲೆ, ಧಾರವಾಡ",
            district = "Dharwad",
            lat = 15.4589,
            lng = 75.0078,
            radiusM = 180,
            checkinStart = "08:30",
            checkinEnd = "09:30",
            checkoutStart = "16:00",
            checkoutEnd = "17:30"
        ),
        School(
            schoolId = "KA_MNG_004",
            name = "Govt. High School Hampankatta, Mangaluru",
            nameKn = "ಸರ್ಕಾರಿ ಪ್ರೌಢಶಾಲೆ ಹಂಪನಕಟ್ಟೆ, ಮಂಗಳೂರು",
            district = "Dakshina Kannada",
            lat = 12.8698,
            lng = 74.8430,
            radiusM = 200,
            checkinStart = "08:30",
            checkinEnd = "09:30",
            checkoutStart = "16:00",
            checkoutEnd = "17:30"
        )
    )

    val defaultTeachers = listOf(
        Teacher(
            teacherId = "TCH_KA_101",
            name = "Dr. Ramesh Patil",
            nameKn = "ಡಾ. ರಮೇಶ್ ಪಾಟೀಲ್",
            phone = "9845012345",
            schoolId = "KA_BLR_001",
            subject = "Mathematics & Science",
            isFaceEnrolled = false
        ),
        Teacher(
            teacherId = "TCH_KA_102",
            name = "Smt. Shailaja Gowda",
            nameKn = "ಶ್ರೀಮತಿ ಶೈಲಜಾ ಗೌಡ",
            phone = "9880198765",
            schoolId = "KA_BLR_001",
            subject = "Kannada Literature",
            isFaceEnrolled = false
        ),
        Teacher(
            teacherId = "TCH_KA_103",
            name = "Sri. Basavaraj Naik",
            nameKn = "ಶ್ರೀ ಬಸವರಾಜ್ ನಾಯಕ್",
            phone = "9448054321",
            schoolId = "KA_MYS_002",
            subject = "Social Studies",
            isFaceEnrolled = false
        ),
        Teacher(
            teacherId = "TCH_KA_104",
            name = "Kum. Anitha Shenoy",
            nameKn = "ಕು. ಅನಿತಾ ಶೆಣೈ",
            phone = "9740122334",
            schoolId = "KA_DWD_003",
            subject = "English Language",
            isFaceEnrolled = false
        )
    )

    suspend fun fetchSchool(schoolId: String): School? {
        val db = firestore ?: return defaultSchools.find { it.schoolId == schoolId }
        return try {
            val doc = db.collection("schools").document(schoolId).get().await()
            if (doc.exists()) {
                School(
                    schoolId = doc.getString("school_id") ?: schoolId,
                    name = doc.getString("name") ?: "",
                    nameKn = doc.getString("name_kn") ?: "",
                    district = doc.getString("district") ?: "",
                    lat = doc.getDouble("lat") ?: 12.9716,
                    lng = doc.getDouble("lng") ?: 77.5946,
                    radiusM = doc.getLong("radius_m")?.toInt() ?: 150,
                    checkinStart = doc.getString("checkin_start") ?: "08:30",
                    checkinEnd = doc.getString("checkin_end") ?: "09:30",
                    checkoutStart = doc.getString("checkout_start") ?: "16:00",
                    checkoutEnd = doc.getString("checkout_end") ?: "17:30"
                )
            } else {
                defaultSchools.find { it.schoolId == schoolId }
            }
        } catch (e: Exception) {
            Log.w("FirestoreManager", "fetchSchool error: ${e.message}")
            defaultSchools.find { it.schoolId == schoolId }
        }
    }

    suspend fun fetchAllSchools(): List<School> {
        val db = firestore ?: return defaultSchools
        return try {
            val snapshot = db.collection("schools").get().await()
            if (!snapshot.isEmpty) {
                snapshot.documents.mapNotNull { doc ->
                    School(
                        schoolId = doc.getString("school_id") ?: doc.id,
                        name = doc.getString("name") ?: "",
                        nameKn = doc.getString("name_kn") ?: "",
                        district = doc.getString("district") ?: "",
                        lat = doc.getDouble("lat") ?: 12.9716,
                        lng = doc.getDouble("lng") ?: 77.5946,
                        radiusM = doc.getLong("radius_m")?.toInt() ?: 150,
                        checkinStart = doc.getString("checkin_start") ?: "08:30",
                        checkinEnd = doc.getString("checkin_end") ?: "09:30",
                        checkoutStart = doc.getString("checkout_start") ?: "16:00",
                        checkoutEnd = doc.getString("checkout_end") ?: "17:30"
                    )
                }
            } else {
                defaultSchools
            }
        } catch (e: Exception) {
            Log.w("FirestoreManager", "fetchAllSchools error: ${e.message}")
            defaultSchools
        }
    }

    suspend fun saveTeacherProfile(teacher: Teacher): Boolean {
        val db = firestore ?: return true
        return try {
            val data = hashMapOf(
                "teacher_id" to teacher.teacherId,
                "name" to teacher.name,
                "name_kn" to teacher.nameKn,
                "phone" to teacher.phone,
                "school_id" to teacher.schoolId,
                "subject" to teacher.subject,
                "embedding" to teacher.embedding,
                "enrolled_date" to teacher.enrolledDate,
                "is_face_enrolled" to teacher.isFaceEnrolled,
                "sample_count" to teacher.sampleCount,
                "updated_at" to FieldValue.serverTimestamp()
            )
            db.collection("teachers").document(teacher.teacherId)
                .set(data, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            Log.w("FirestoreManager", "saveTeacherProfile error: ${e.message}")
            false
        }
    }

    suspend fun syncAttendanceRecord(record: AttendanceRecord): Boolean {
        val db = firestore ?: return false
        return try {
            val docId = if (record.recordId.isNotBlank()) record.recordId else "${record.teacherId}_${record.date}"
            val data = hashMapOf(
                "record_id" to docId,
                "teacher_id" to record.teacherId,
                "teacher_name" to record.teacherName,
                "school_id" to record.schoolId,
                "date" to record.date,
                "check_in_time" to record.checkInTime,
                "check_out_time" to record.checkOutTime,
                "lat" to record.lat,
                "lng" to record.lng,
                "distance_meters" to record.distanceMeters,
                "status" to record.status,
                "device_id" to record.deviceId,
                "similarity_score" to record.similarityScore,
                "liveness_passed" to record.livenessPassed,
                "is_unverified_time" to record.isUnverifiedTime,
                "created_at" to FieldValue.serverTimestamp()
            )
            db.collection("attendance").document(docId)
                .set(data, SetOptions.merge())
                .await()
            true
        } catch (e: Exception) {
            Log.w("FirestoreManager", "syncAttendanceRecord error: ${e.message}")
            false
        }
    }
}
