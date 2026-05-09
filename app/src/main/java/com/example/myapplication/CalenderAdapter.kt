package com.example.myapplication.ui.util

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R

// Added by Lesley Del Cid:
// Adapter for the custom Stats calendar.
// Uses findViewById instead of ViewBinding, so no Gradle changes are needed.
class CalendarAdapter(
    private var days: List<CalendarDay>,
    private val onDayClicked: (CalendarDay) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    inner class CalendarViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtDayNumber: TextView = view.findViewById(R.id.txtDayNumber)
        val taskDot: View = view.findViewById(R.id.taskDot)
        val plantDot: View = view.findViewById(R.id.plantDot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_calendar_day, parent, false)

        return CalendarViewHolder(view)
    }


    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        val day = days[position]

        if (day.isBlank) {
            holder.txtDayNumber.text = ""
            holder.txtDayNumber.background = null
            holder.taskDot.visibility = View.GONE
            holder.plantDot.visibility = View.GONE
            holder.itemView.setOnClickListener(null)
            return
        }

        holder.txtDayNumber.text = day.dayNumber.toString()

        if (day.isSelected) {
            holder.txtDayNumber.setBackgroundResource(R.drawable.bg_selected_day)
            holder.txtDayNumber.setTextColor(Color.WHITE)
        } else if (day.isToday) {
            holder.txtDayNumber.setBackgroundResource(R.drawable.bg_today_outline)
            holder.txtDayNumber.setTextColor(Color.parseColor("#2B1B10"))
        } else {
            holder.txtDayNumber.background = null
            holder.txtDayNumber.setTextColor(Color.parseColor("#2B1B10"))
        }

        holder.taskDot.visibility =
            if (day.hasTask) View.VISIBLE else View.GONE

        holder.plantDot.visibility =
            if (day.hasPlant) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            onDayClicked(day)
        }
    }

    override fun getItemCount(): Int = days.size

    fun updateDays(newDays: List<CalendarDay>) {
        days = newDays
        notifyDataSetChanged()
    }
}