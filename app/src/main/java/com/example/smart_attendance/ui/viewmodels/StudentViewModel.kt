package com.example.smart_attendance.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.smart_attendance.data.AppDatabase
import com.example.smart_attendance.data.Student
import com.example.smart_attendance.data.StudentRepository
import com.example.smart_attendance.data.StudentSummary
import kotlinx.coroutines.launch

class StudentViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: StudentRepository
    val allStudents: LiveData<List<StudentSummary>>

    init {
        val db = AppDatabase.getInstance(application)
        repository = StudentRepository(db.studentDao())
        allStudents = repository.allStudents
    }

    fun insert(student: Student) = viewModelScope.launch {
        repository.insert(student)
    }

    fun delete(student: Student) = viewModelScope.launch {
        repository.delete(student)
    }

    suspend fun getStudentByEnrollment(enrollmentNumber: String): Student? {
        return repository.getStudentByEnrollment(enrollmentNumber)
    }
}
