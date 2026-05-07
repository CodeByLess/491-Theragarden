package com.example.myapplication

data class Task(
    val id: String = "",
    val title: String = "",
    var completed: Boolean = false,

    // Added by Lesley:
    // Stores the date the task was created (yyyy-MM-dd format).
    // Used for grouping tasks by day in the calendar.
    val createdDate: String = "",

    // Added by Lesley:
    // Stores the date the task was completed.
    // Null means the task is not completed.
    // Used for streak tracking and progress analysis.
    val completedDate: String? = null
)