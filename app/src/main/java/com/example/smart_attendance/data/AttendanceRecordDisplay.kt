package com.example.smart_attendance.data

data class AttendanceRecordDisplay(
    val attendanceId: Int,
    val studentName: String,
    val studentEnrollmentNumber: String,
    val timestamp: Long,
    val subjectName: String?,
    val subjectType: String?,
    val status: String
) 