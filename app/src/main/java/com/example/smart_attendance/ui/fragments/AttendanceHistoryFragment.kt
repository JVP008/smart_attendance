package com.example.smart_attendance.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smart_attendance.data.AttendanceRecordDisplay
import com.example.smart_attendance.databinding.FragmentAttendanceHistoryBinding
import com.example.smart_attendance.ui.adapters.AttendanceAdapter
import com.example.smart_attendance.ui.viewmodels.AttendanceHistoryViewModel
import com.example.smart_attendance.ui.viewmodels.StudentViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AttendanceHistoryFragment : Fragment() {
    private var _binding: FragmentAttendanceHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var attendanceHistoryViewModel: AttendanceHistoryViewModel
    private lateinit var studentViewModel: StudentViewModel
    private lateinit var attendanceAdapter: AttendanceAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAttendanceHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        attendanceHistoryViewModel = ViewModelProvider(this)[AttendanceHistoryViewModel::class.java]
        studentViewModel = ViewModelProvider(this)[StudentViewModel::class.java]
        attendanceAdapter = AttendanceAdapter(emptyList())

        binding.rvAttendanceHistory.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = attendanceAdapter
            setHasFixedSize(true)
        }

        // Combine student data and attendance data efficiently
        attendanceHistoryViewModel.allAttendance.observe(viewLifecycleOwner) { attendanceList ->
            lifecycleScope.launch(Dispatchers.IO) {
                // Fetch all students once to avoid N+1 queries
                val studentSummaries = studentViewModel.allStudents.value ?: emptyList()
                val studentMap = studentSummaries.associateBy { it.enrollmentNumber }

                val displayList = attendanceList.map { attendance ->
                    val student = studentMap[attendance.studentEnrollmentNumber]
                    AttendanceRecordDisplay(
                        attendance.id,
                        student?.name ?: "Unknown Student",
                        student?.enrollmentNumber ?: attendance.studentEnrollmentNumber,
                        attendance.timestamp,
                        student?.subjectName ?: "N/A",
                        student?.attendanceType ?: "N/A",
                        attendance.status
                    )
                }
                withContext(Dispatchers.Main) {
                    attendanceAdapter.updateData(displayList)
                }
            }
        }

        binding.btnExportClear.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val attendanceList = attendanceHistoryViewModel.allAttendance.value ?: emptyList()
                if (attendanceList.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "No attendance records to export.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                
                val studentSummaries = studentViewModel.allStudents.value ?: emptyList()
                val studentMap = studentSummaries.associateBy { it.enrollmentNumber }
                
                val displayList = attendanceList.map { attendance ->
                    val student = studentMap[attendance.studentEnrollmentNumber]
                    AttendanceRecordDisplay(
                        attendance.id,
                        student?.name ?: "Unknown Student",
                        student?.enrollmentNumber ?: attendance.studentEnrollmentNumber,
                        attendance.timestamp,
                        student?.subjectName ?: "N/A",
                        student?.attendanceType ?: "N/A",
                        attendance.status
                    )
                }
                try {
                    val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    val fileName = "attendance_${sdf.format(Date())}.csv"
                    val file = File(requireContext().getExternalFilesDir(null), fileName)

                    val dateFormat = SimpleDateFormat("MM-dd", Locale.getDefault())
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                    FileWriter(file).use { writer ->
                        writer.append("Enrollment,Name,Date,Time,Status\n")
                        for (record in displayList) {
                            val formattedDate = dateFormat.format(Date(record.timestamp))
                            val formattedTime = timeFormat.format(Date(record.timestamp))
                            writer.append("${record.studentEnrollmentNumber},${record.studentName},${formattedDate},${formattedTime},${record.status}\n")
                        }
                    }
                    attendanceHistoryViewModel.clearAllAttendance()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Attendance exported to ${file.absolutePath} and cleared!", Toast.LENGTH_LONG).show()
                        attendanceAdapter.updateData(emptyList())
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Failed to export: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
