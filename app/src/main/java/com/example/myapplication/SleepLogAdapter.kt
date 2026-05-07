package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Locale

class SleepLogAdapter : RecyclerView.Adapter<SleepLogAdapter.VH>() {

    // Holds the list of sleep logs displayed in RecyclerView
    private var items: List<SleepLog> = emptyList()

    // Updates adapter data when new sleep logs are loaded
    fun submitList(newItems: List<SleepLog>) {
        items = newItems
        notifyDataSetChanged() // Refresh entire list
    }

    // ViewHolder holds references to each view inside sleep_item.xml
    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)         // Sleep start date
        val tvDuration: TextView = itemView.findViewById(R.id.tvDuration) // Sleep duration
        val tvQuality: TextView = itemView.findViewById(R.id.tvQuality)   // Sleep quality rating
        val tvNote: TextView = itemView.findViewById(R.id.tvNote)         // Optional user note
    }

    // Inflates the layout for each row in the list
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.sleep_item, parent, false)
        return VH(v)
    }

    // Binds SleepLog data to each row
    override fun onBindViewHolder(holder: VH, position: Int) {

        val entry = items[position] // Get current sleep log
        val fmt = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault())

        // Convert Firestore Timestamp to formatted date string
        val start = entry.startTime?.toDate()
        holder.tvDate.text = if (start != null)
            fmt.format(start)
        else
            "Unknown date"

        // Convert total minutes into hours and minutes
        val h = entry.durationMinutes / 60
        val m = entry.durationMinutes % 60
        holder.tvDuration.text = "Duration: ${h}h ${m}m"

        // Display quality rating
        holder.tvQuality.text = "Quality: ${entry.quality}/5"

        // Only show note if it exists
        holder.tvNote.text =
            if (entry.note.isBlank()) ""
            else "Note: ${entry.note}"
    }

    // Returns total number of sleep logs
    override fun getItemCount(): Int = items.size
}
