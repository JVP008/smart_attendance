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
import kotlinx.coroutines.launch
import java.util.Calendar

class AttendanceViewModel(application: Application) : AndroidViewModel(application) {
    private val studentRepo: StudentRepository
    private val attendanceRepo: AttendanceRepository
    val allAttendance: LiveData<List<Attendance>>

    init {
        val db = AppDatabase.getInstance(application)
        studentRepo = StudentRepository(db.studentDao())
        attendanceRepo = AttendanceRepository(db.attendanceDao())
        allAttendance = attendanceRepo.allAttendance
    }

    fun markAttendance(attendance: Attendance) = viewModelScope.launch {
        attendanceRepo.insert(attendance)
    }

    fun updateAttendanceStatus(enrollmentNumber: String, timestamp: Long, status: String) = viewModelScope.launch {
        attendanceRepo.updateAttendanceStatus(enrollmentNumber, timestamp, status)
    }

    suspend fun getAttendanceForStudentAndDate(enrollmentNumber: String, date: Long): Attendance? {
        return attendanceRepo.getAttendanceRecordForStudentAndDate(enrollmentNumber, date)
    }

    fun getStudentSummaryByEnrollmentNumberLiveData(enrollmentNumber: String): LiveData<com.example.smart_attendance.data.StudentSummary?> {
        return studentRepo.getStudentSummaryByEnrollmentNumberLiveData(enrollmentNumber)
    }

    fun prePopulateAttendanceForNewStudent(enrollmentNumber: String, startDate: Long) = viewModelScope.launch {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = startDate

        val currentCalendar = Calendar.getInstance()
        currentCalendar.timeInMillis = System.currentTimeMillis()
        currentCalendar.set(Calendar.HOUR_OF_DAY, 0)
        currentCalendar.set(Calendar.MINUTE, 0)
        currentCalendar.set(Calendar.SECOND, 0)
        currentCalendar.set(Calendar.MILLISECOND, 0)

        while (calendar.timeInMillis <= currentCalendar.timeInMillis) {
            val attendanceDate = calendar.timeInMillis
            // Check if a record already exists for this student on this date
            val existingRecord = attendanceRepo.getAttendanceRecordForStudentAndDate(enrollmentNumber, attendanceDate)
            if (existingRecord == null) {
                val attendance = Attendance(
                    studentEnrollmentNumber = enrollmentNumber,
                    timestamp = attendanceDate,
                    status = "Not Present"
                )
                attendanceRepo.insert(attendance)
    }
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    suspend fun getPresentAttendanceRecordForStudentAndDate(enrollmentNumber: String, date: Long): Attendance? {
        return attendanceRepo.getPresentAttendanceRecordForStudentAndDate(enrollmentNumber, date)
    }

    suspend fun markAttendanceAsPresent(enrollmentNumber: String, timestamp: Long, studentName: String, studentFaceImageWidth: Int, studentFaceImageHeight: Int) {
        // Calculate the start and end of the current day for the given timestamp
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        val startOfDayTimestamp = calendar.timeInMillis

        calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        val endOfDayTimestamp = calendar.timeInMillis // Start of next day

        val existingAttendanceForDay = attendanceRepo.getAttendanceRecordForStudentAndDateRange(enrollmentNumber, startOfDayTimestamp, endOfDayTimestamp)

        if (existingAttendanceForDay == null) {
            // This case should ideally not happen if a 'Not Present' entry is created on registration.
            // However, as a fallback, if no record exists for the day, insert a new 'Present' record.
            val newAttendance = Attendance(
                studentEnrollmentNumber = enrollmentNumber,
                timestamp = timestamp, // Use the exact recognition timestamp
                status = "Present"
            )
            attendanceRepo.insert(newAttendance)
        } else {
            // Record for today exists, update its status to 'Present' and timestamp to recognition time
            attendanceRepo.updateAttendanceRecord(enrollmentNumber, startOfDayTimestamp, "Present", timestamp)
        }
    }
}
