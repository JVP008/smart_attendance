package com.example.smart_attendance.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students", indices = [androidx.room.Index(value = ["rollNo"], unique = true),
    androidx.room.Index(value = ["enrollmentNumber"], unique = true)])
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val rollNo: String,
    val enrollmentNumber: String,
    val branch: String,
    val semester: String,
    val faceImagePath: String,
    val faceEmbedding: ByteArray,
    val faceImageWidth: Int,
    val faceImageHeight: Int,
    val subjectName: String,
    val attendanceType: String
)
