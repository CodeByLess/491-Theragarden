package com.example.myapplication

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Date

class JournalAdapter(
    private val items: List<JournalEntry>,
    private val onItemClick: (JournalEntry) -> Unit
) : RecyclerView.Adapter<JournalAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDate: TextView = itemView.findViewById(R.id.tvEntryDate)
        val tvText: TextView = itemView.findViewById(R.id.tvEntryText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_journal_entry, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        val dateStr = DateFormat.format("MMM dd, yyyy • h:mm a", Date(item.createdAt)).toString()

        holder.tvDate.text = dateStr
        holder.tvText.text = item.text

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size
}
