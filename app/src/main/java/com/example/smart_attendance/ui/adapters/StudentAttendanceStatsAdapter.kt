package com.example.smart_attendance.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smart_attendance.R
import com.example.smart_attendance.data.StudentAttendanceStats

class StudentAttendanceStatsAdapter(private var statsList: List<StudentAttendanceStats>) :
    RecyclerView.Adapter<StudentAttendanceStatsAdapter.AttendanceStatsViewHolder>() {

    class AttendanceStatsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvStudentNameRoll: TextView = itemView.findViewById(R.id.tvStudentNameRoll)
        val tvAttendancePercentage: TextView = itemView.findViewById(R.id.tvAttendancePercentage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceStatsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_attendance_stats, parent, false)
        return AttendanceStatsViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttendanceStatsViewHolder, position: Int) {
        val currentItem = statsList[position]
        holder.tvStudentNameRoll.text = "${currentItem.studentName} (${currentItem.studentEnrollmentNumber})"
        // Decide which percentage to display based on the context (weekly/monthly adapter)
        // For simplicity, let's assume this adapter will be used for both, and the calling fragment
        // will decide which value to pass if needed. Or we can make separate adapters.
        // For now, let's display both, or just the weekly/monthly if it's a dedicated adapter.
        // Let's display weekly for weekly adapter and monthly for monthly adapter, so we pass one.
        // For this generic adapter, let's display the monthly by default, or just use it for below 60%.
        // Given the prompt, let's make it flexible and display a combined percentage if needed, or specific.
        // For now, let's display monthly, and the fragment will specify.
        holder.tvAttendancePercentage.text = "Attendance: ${currentItem.monthlyAttendancePercentage}%"

    }

    override fun getItemCount(): Int = statsList.size

    fun updateData(newStatsList: List<StudentAttendanceStats>) {
        statsList = newStatsList
        notifyDataSetChanged()
    }
} 