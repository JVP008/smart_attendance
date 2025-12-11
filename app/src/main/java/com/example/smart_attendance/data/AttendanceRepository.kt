package com.example.smart_attendance.data

import androidx.lifecycle.LiveData

class AttendanceRepository(private val attendanceDao: AttendanceDao) {
    val allAttendance: LiveData<List<Attendance>> = attendanceDao.getAllAttendance()

    suspend fun insert(attendance: Attendance) {
        attendanceDao.insert(attendance)
    }

    suspend fun clearAllAttendance() {
        attendanceDao.deleteAllAttendance()
    }

    suspend fun getAllAttendanceList(): List<Attendance> {
        return attendanceDao.getAllAttendanceList()
    }

    suspend fun getAttendanceCountForStudentByDateRange(enrollmentNumber: String, startDate: Long, endDate: Long): Int {
        return attendanceDao.getAttendanceCountForStudentByDateRange(enrollmentNumber, startDate, endDate)
    }

    suspend fun getTotalAttendanceDatesByDateRange(startDate: Long, endDate: Long): Int {
        return attendanceDao.getTotalAttendanceDatesByDateRange(startDate, endDate)
    }

    suspend fun updateAttendanceStatus(enrollmentNumber: String, timestamp: Long, status: String) {
        attendanceDao.updateAttendanceStatus(enrollmentNumber, timestamp, status)
    }

    suspend fun getAttendanceRecordForStudentAndDate(enrollmentNumber: String, date: Long): Attendance? {
        return attendanceDao.getAttendanceRecordForStudentAndDate(enrollmentNumber, date)
    }

    suspend fun getNotPresentAttendanceRecordForStudentAndDate(enrollmentNumber: String, date: Long): Attendance? {
        return attendanceDao.getNotPresentAttendanceRecordForStudentAndDate(enrollmentNumber, date)
    }

    suspend fun getPresentAttendanceRecordForStudentAndDate(enrollmentNumber: String, date: Long): Attendance? {
        return attendanceDao.getPresentAttendanceRecordForStudentAndDate(enrollmentNumber, date)
    }

    suspend fun getAttendanceRecordForStudentAndDateRange(enrollmentNumber: String, startOfDay: Long, endOfDay: Long): Attendance? {
        return attendanceDao.getAttendanceRecordForStudentAndDateRange(enrollmentNumber, startOfDay, endOfDay)
    }

    suspend fun updateAttendanceRecord(enrollmentNumber: String, date: Long, status: String, newTimestamp: Long) {
        attendanceDao.updateAttendanceRecord(enrollmentNumber, date, status, newTimestamp)
    }
}
