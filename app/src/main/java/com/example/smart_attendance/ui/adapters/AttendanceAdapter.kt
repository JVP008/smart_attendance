package com.example.smart_attendance.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_attendance.R
import com.example.smart_attendance.data.AttendanceRecordDisplay
import java.text.SimpleDateFormat
import java.util.*

class AttendanceAdapter(private var attendanceList: List<AttendanceRecordDisplay>) : RecyclerView.Adapter<AttendanceAdapter.AttendanceViewHolder>() {

    class AttendanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvStudentName: TextView = itemView.findViewById(R.id.tvStudentName)
        val tvRollNumber: TextView = itemView.findViewById(R.id.tvRollNumber)
        val tvAttendanceDate: TextView = itemView.findViewById(R.id.tvAttendanceDate)
        val tvAttendanceStatus: TextView = itemView.findViewById(R.id.tvAttendanceStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_attendance_record, parent, false)
        return AttendanceViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        val currentItem = attendanceList[position]
        holder.tvStudentName.text = "Name: ${currentItem.studentName}"
        holder.tvRollNumber.text = "Enrollment: ${currentItem.studentEnrollmentNumber}"
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        holder.tvAttendanceDate.text = "Date: ${sdf.format(Date(currentItem.timestamp))}"
        holder.tvAttendanceStatus.text = "Status: ${currentItem.status}"
    }

    override fun getItemCount() = attendanceList.size

    fun updateData(newAttendanceList: List<AttendanceRecordDisplay>) {
        attendanceList = newAttendanceList
        notifyDataSetChanged()
    }
} 