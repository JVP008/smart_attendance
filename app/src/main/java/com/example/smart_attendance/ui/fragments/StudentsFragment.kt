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
import com.example.smart_attendance.databinding.FragmentStudentsBinding
import com.example.smart_attendance.ui.adapters.StudentAdapter
import com.example.smart_attendance.ui.viewmodels.StudentViewModel
import com.example.smart_attendance.data.StudentSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StudentsFragment : Fragment() {
    private var _binding: FragmentStudentsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: StudentViewModel
    private lateinit var adapter: StudentAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentStudentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel = ViewModelProvider(this).get(StudentViewModel::class.java)
        adapter = StudentAdapter(emptyList<StudentSummary>()) { studentSummary ->
            // Handle delete click here
            lifecycleScope.launch(Dispatchers.IO) {
                val studentToDelete = viewModel.getStudentByEnrollment(studentSummary.enrollmentNumber)
                if (studentToDelete != null) {
                    viewModel.delete(studentToDelete)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Student ${studentSummary.name} (Enrollment: ${studentSummary.enrollmentNumber}) deleted.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error: Student not found for deletion.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        binding.rvStudents.layoutManager = LinearLayoutManager(context)
        binding.rvStudents.adapter = adapter

        viewModel.allStudents.observe(viewLifecycleOwner) { students ->
            adapter.updateData(students)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
