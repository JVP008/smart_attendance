package com.example.smart_attendance.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance")
data class Attendance(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentEnrollmentNumber: String,
    val timestamp: Long,
    val status: String = "Not Present"
)
