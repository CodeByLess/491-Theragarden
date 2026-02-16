package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class SleepAdapter : RecyclerView.Adapter<SleepAdapter.ViewHolder>() {

    // List that holds all sleep entries
    private var items: List<SleepEntry> = emptyList()

    // Called when Firestore data changes
    fun submitList(list: List<SleepEntry>?) {
        // If list is null, use empty list to prevent crashes
        items = list ?: emptyList()

        // Refresh RecyclerView
        notifyDataSetChanged()
    }

    // Holds references to views inside sleep_item.xml
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val date: TextView = view.findViewById(R.id.tvDate)       // Sleep date
        val duration: TextView = view.findViewById(R.id.tvDuration) // Sleep duration
        val quality: TextView = view.findViewById(R.id.tvQuality)   // Sleep quality rating
    }

    // Inflates the layout for each row
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.sleep_item, parent, false)
        return ViewHolder(view)
    }

    // Binds data to each row
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val entry = items[position] // Get sleep entry for this position

        val formatter = SimpleDateFormat("MMM dd", Locale.getDefault()) // Format date

        // Convert Firestore Timestamp to readable date
        // !! assumes sleepStart is not null
        holder.date.text = formatter.format(entry.sleepStart!!.toDate())

        // Convert total minutes into hours and minutes
        holder.duration.text =
            "Duration: ${entry.durationMinutes / 60}h ${entry.durationMinutes % 60}m"

        // Display quality rating out of 5
        holder.quality.text =
            "Quality: ${entry.quality}/5"
    }

    // Returns total number of items
    override fun getItemCount() = items.size
}
