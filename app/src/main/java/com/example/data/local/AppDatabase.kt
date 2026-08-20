package com.example.data.local

import androidx.room.*
import com.example.data.model.AttendanceRecord
import com.example.data.model.School
import com.example.data.model.Teacher
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolDao {
    @Query("SELECT * FROM schools WHERE schoolId = :schoolId LIMIT 1")
    fun getSchoolById(schoolId: String): Flow<School?>

    @Query("SELECT * FROM schools ORDER BY name ASC")
    fun getAllSchools(): Flow<List<School>>

    @Query("SELECT COUNT(*) FROM schools")
    suspend fun getSchoolCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchool(school: School)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchools(schools: List<School>)

    @Query("DELETE FROM schools WHERE schoolId = :schoolId")
    suspend fun deleteSchoolById(schoolId: String)
}

@Dao
interface TeacherDao {
    @Query("SELECT * FROM teachers WHERE teacherId = :teacherId LIMIT 1")
    fun getTeacherById(teacherId: String): Flow<Teacher?>

    @Query("SELECT * FROM teachers WHERE phone = :phone LIMIT 1")
    suspend fun getTeacherByPhone(phone: String): Teacher?

    @Query("SELECT * FROM teachers WHERE phone = :phone LIMIT 1")
    fun observeTeacherByPhone(phone: String): Flow<Teacher?>

    @Query("SELECT * FROM teachers ORDER BY name ASC")
    fun getAllTeachers(): Flow<List<Teacher>>

    @Query("SELECT COUNT(*) FROM teachers")
    suspend fun getTeacherCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacher(teacher: Teacher)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeachers(teachers: List<Teacher>)

    @Update
    suspend fun updateTeacher(teacher: Teacher)

    @Delete
    suspend fun deleteTeacher(teacher: Teacher)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance WHERE teacherId = :teacherId ORDER BY date DESC, createdAt DESC")
    fun getAttendanceForTeacher(teacherId: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance ORDER BY date DESC, createdAt DESC")
    fun getAllAttendance(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance WHERE recordId = :recordId LIMIT 1")
    suspend fun getAttendanceByRecordId(recordId: String): AttendanceRecord?

    @Query("SELECT * FROM attendance WHERE recordId = :recordId LIMIT 1")
    fun observeAttendanceByRecordId(recordId: String): Flow<AttendanceRecord?>

    @Query("SELECT * FROM attendance WHERE date = :date ORDER BY createdAt DESC")
    fun getAttendanceByDate(date: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance WHERE isSynced = 0 ORDER BY createdAt ASC")
    suspend fun getUnsyncedAttendance(): List<AttendanceRecord>

    @Query("SELECT COUNT(*) FROM attendance WHERE isSynced = 0")
    fun getUnsyncedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM attendance WHERE isSynced = 0")
    suspend fun getUnsyncedCountSync(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(record: AttendanceRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceList(records: List<AttendanceRecord>)

    @Query("UPDATE attendance SET isSynced = :synced, updatedAt = :updatedAt WHERE recordId = :recordId")
    suspend fun updateSyncStatus(recordId: String, synced: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM attendance WHERE recordId = :recordId")
    suspend fun deleteAttendanceById(recordId: String)
}

@Database(
    entities = [
        School::class,
        Teacher::class,
        AttendanceRecord::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun schoolDao(): SchoolDao
    abstract fun teacherDao(): TeacherDao
    abstract fun attendanceDao(): AttendanceDao
}
