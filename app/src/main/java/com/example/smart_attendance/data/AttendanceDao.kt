package com.example.smart_attendance.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AttendanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attendance: Attendance): Long

    @Query("SELECT * FROM attendance ORDER BY timestamp ASC")
    fun getAllAttendance(): LiveData<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE studentEnrollmentNumber = :enrollmentNumber")
    fun getAttendanceForStudent(enrollmentNumber: String): LiveData<List<Attendance>>

    @Query("DELETE FROM attendance")
    suspend fun deleteAllAttendance(): Int

    @Query("SELECT * FROM attendance ORDER BY timestamp ASC")
    suspend fun getAllAttendanceList(): List<Attendance>

    @Query("SELECT COUNT(DISTINCT strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch')) FROM attendance WHERE studentEnrollmentNumber = :enrollmentNumber AND timestamp BETWEEN :startDate AND :endDate")
    suspend fun getAttendanceCountForStudentByDateRange(enrollmentNumber: String, startDate: Long, endDate: Long): Int

    @Query("SELECT COUNT(DISTINCT strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch')) FROM attendance WHERE timestamp BETWEEN :startDate AND :endDate")
    suspend fun getTotalAttendanceDatesByDateRange(startDate: Long, endDate: Long): Int

    @Query("UPDATE attendance SET status = :status WHERE studentEnrollmentNumber = :enrollmentNumber AND strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch') = strftime('%Y-%m-%d', :timestamp / 1000, 'unixepoch')")
    suspend fun updateAttendanceStatus(enrollmentNumber: String, timestamp: Long, status: String): Int

    @Query("SELECT * FROM attendance WHERE studentEnrollmentNumber = :enrollmentNumber AND strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch') = strftime('%Y-%m-%d', :date / 1000, 'unixepoch') LIMIT 1")
    suspend fun getAttendanceRecordForStudentAndDate(enrollmentNumber: String, date: Long): Attendance?

    @Query("SELECT * FROM attendance WHERE studentEnrollmentNumber = :enrollmentNumber AND strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch') = strftime('%Y-%m-%d', :date / 1000, 'unixepoch') AND status = 'Not Present' LIMIT 1")
    suspend fun getNotPresentAttendanceRecordForStudentAndDate(enrollmentNumber: String, date: Long): Attendance?

    @Query("SELECT * FROM attendance WHERE studentEnrollmentNumber = :enrollmentNumber AND strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch') = strftime('%Y-%m-%d', :date / 1000, 'unixepoch') AND status = 'Present' LIMIT 1")
    suspend fun getPresentAttendanceRecordForStudentAndDate(enrollmentNumber: String, date: Long): Attendance?

    @Query("SELECT * FROM attendance WHERE studentEnrollmentNumber = :enrollmentNumber AND timestamp >= :startOfDay AND timestamp < :endOfDay LIMIT 1")
    suspend fun getAttendanceRecordForStudentAndDateRange(enrollmentNumber: String, startOfDay: Long, endOfDay: Long): Attendance?

    @Query("UPDATE attendance SET status = :status, timestamp = :newTimestamp WHERE studentEnrollmentNumber = :enrollmentNumber AND strftime('%Y-%m-%d', timestamp / 1000, 'unixepoch') = strftime('%Y-%m-%d', :date / 1000, 'unixepoch')")
    suspend fun updateAttendanceRecord(enrollmentNumber: String, date: Long, status: String, newTimestamp: Long): Int
}
