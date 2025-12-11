package com.example.smart_attendance.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.smart_attendance.data.AppDatabase
import com.example.smart_attendance.data.Attendance
import com.example.smart_attendance.data.AttendanceRepository
import com.example.smart_attendance.data.Student
import com.example.smart_attendance.data.StudentRepository
import com.example.smart_attendance.data.StudentSummary
import kotlinx.coroutines.launch

class AttendanceHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val attendanceRepo: AttendanceRepository
    private val studentRepo: StudentRepository
    val allAttendance: LiveData<List<Attendance>>

    init {
        val db = AppDatabase.getInstance(application)
        attendanceRepo = AttendanceRepository(db.attendanceDao())
        studentRepo = StudentRepository(db.studentDao())
        allAttendance = attendanceRepo.allAttendance
    }

    fun getStudentSummaryByEnrollmentNumberLiveData(enrollmentNumber: String): LiveData<com.example.smart_attendance.data.StudentSummary?> {
        return studentRepo.getStudentSummaryByEnrollmentNumberLiveData(enrollmentNumber)
    }

    suspend fun getStudentSummaryByEnrollmentNumber(enrollmentNumber: String): StudentSummary? {
        return studentRepo.getStudentSummaryByEnrollmentNumber(enrollmentNumber)
    }

    fun clearAllAttendance() = viewModelScope.launch {
        attendanceRepo.clearAllAttendance()
    }

    suspend fun getAllAttendanceList(): List<Attendance> {
        return attendanceRepo.getAllAttendanceList()
    }
} 