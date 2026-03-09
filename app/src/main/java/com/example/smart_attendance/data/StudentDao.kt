package com.example.smart_attendance.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface StudentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(student: Student): Long

    @Delete
    suspend fun delete(student: Student): Int

    @Query("SELECT name, enrollmentNumber, branch, semester, faceImagePath, subjectName, attendanceType FROM students")
    fun getAllStudents(): LiveData<List<StudentSummary>>

    @Query("SELECT * FROM students WHERE enrollmentNumber = :enrollmentNumber")
    fun getStudentByIdLiveData(enrollmentNumber: String): LiveData<Student?>

    @Query("SELECT name, enrollmentNumber, branch, semester, faceImagePath, subjectName, attendanceType FROM students WHERE enrollmentNumber = :enrollmentNumber")
    fun getStudentSummaryByEnrollmentNumberLiveData(enrollmentNumber: String): LiveData<StudentSummary?>

    @Query("SELECT * FROM students WHERE enrollmentNumber = :enrollmentNumber")
    suspend fun getStudentByEnrollment(enrollmentNumber: String): Student?

    @Query("SELECT name, enrollmentNumber, branch, semester, faceImagePath, subjectName, attendanceType FROM students WHERE enrollmentNumber = :enrollmentNumber")
    suspend fun getStudentSummaryByEnrollmentNumber(enrollmentNumber: String): StudentSummary?
}
