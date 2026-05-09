package com.example.myapplication

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SuggestedTaskAdapter (
    private val tasks: List<String>
) : RecyclerView.Adapter<SuggestedTaskAdapter.ViewHolder>() {
    // Tracks which tasks the user has checked
    val selectedTasks = mutableSetOf<String>()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkBox: CheckBox = view.findViewById(R.id.checkBox)
        val title: TextView = view.findViewById(R.id.tvTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.task_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val task = tasks[position]

        holder.title.text = task

        // Set checkbox state without triggering listener
        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.isChecked = selectedTasks.contains(task)

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedTasks.add(task)
                Log.d("QuizDebug", "Task selected: $task")
            } else {
                selectedTasks.remove(task)
                Log.d("QuizDebug", "Task deselected: $task")
            }
        }
    }

    override fun getItemCount() = tasks.size
}