package com.example.smart_attendance.data

import androidx.lifecycle.LiveData

class StudentRepository(private val studentDao: StudentDao) {
    val allStudents: LiveData<List<StudentSummary>> = studentDao.getAllStudents()

    suspend fun insert(student: Student) {
        studentDao.insert(student)
    }

    suspend fun delete(student: Student) {
        studentDao.delete(student)
    }

    fun getStudentByIdLiveData(enrollmentNumber: String): LiveData<Student?> {
        return studentDao.getStudentByIdLiveData(enrollmentNumber)
    }

    fun getStudentSummaryByEnrollmentNumberLiveData(enrollmentNumber: String): LiveData<StudentSummary?> {
        return studentDao.getStudentSummaryByEnrollmentNumberLiveData(enrollmentNumber)
    }

    suspend fun getStudentSummaryByEnrollmentNumber(enrollmentNumber: String): StudentSummary? {
        return studentDao.getStudentSummaryByEnrollmentNumber(enrollmentNumber)
    }

    suspend fun getStudentByEnrollment(enrollmentNumber: String): Student? {
        return studentDao.getStudentByEnrollment(enrollmentNumber)
    }
}
