package com.example.smart_attendance.data

data class StudentSummary(
    val name: String,
    val enrollmentNumber: String,
    val branch: String,
    val semester: String,
    val faceImagePath: String,
    val subjectName: String?,
    val attendanceType: String?
) 