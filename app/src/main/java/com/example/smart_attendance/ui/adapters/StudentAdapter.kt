package com.example.smart_attendance.ui.adapters

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_attendance.data.StudentSummary
import com.example.smart_attendance.databinding.ItemStudentBinding

class StudentAdapter(
    private var students: List<StudentSummary>,
    private val onDeleteClickListener: (StudentSummary) -> Unit
) :
    RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    inner class StudentViewHolder(val binding: ItemStudentBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.ivDeleteStudent.setOnClickListener { // 'ivDeleteStudent' is the ID of the delete button
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClickListener(students[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val binding = ItemStudentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StudentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StudentAdapter.StudentViewHolder, position: Int) {
        val student: StudentSummary = students[position]
        holder.binding.tvName.text = student.name
        holder.binding.tvRoll.text = student.enrollmentNumber
        holder.binding.tvBranch.text = student.branch
        holder.binding.tvSemester.text = student.semester
        student.faceImagePath?.let { imagePath ->
            val bitmap = BitmapFactory.decodeFile(imagePath)
            holder.binding.ivStudentFace.setImageBitmap(bitmap)
        } ?: holder.binding.ivStudentFace.setImageBitmap(null)
    }

    override fun getItemCount(): Int = students.size

    fun updateData(newStudents: List<StudentSummary>) {
        students = newStudents
        notifyDataSetChanged()
    }
}
