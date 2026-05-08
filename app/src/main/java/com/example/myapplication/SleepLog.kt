package com.example.myapplication

import com.google.firebase.Timestamp

// Data model representing a single sleep log entry in Firestore
data class SleepLog(

    // Timestamp when the user started sleeping
    // Nullable to support Firestore deserialization
    val startTime: Timestamp? = null,

    // Timestamp when the user ended sleeping
    // Can remain null if sleep session is still active
    val endTime: Timestamp? = null,

    // Total sleep duration stored in minutes
    // Default value prevents null mapping issues
    val durationMinutes: Long = 0,

    // Sleep quality rating (typically 1–5 scale)
    // Default set to 3 as a neutral baseline
    val quality: Int = 3,

    // Optional note written by the user about their sleep
    val note: String = ""
)
