package com.example.smart_attendance.data

data class StudentAttendanceStats(
    val studentEnrollmentNumber: String,
    val studentName: String,
    val weeklyAttendanceCount: Int,
    val weeklyAttendancePercentage: Double,
    val monthlyAttendanceCount: Int,
    val monthlyAttendancePercentage: Double
) 