package com.example.myapplication

import com.google.firebase.Timestamp

// Data model representing one sleep session stored in Firestore
data class SleepEntry(

    // Timestamp when user started sleeping
    // Nullable because Firestore requires empty constructor values
    val sleepStart: Timestamp? = null,

    // Timestamp when user ended sleeping
    // May be null if sleep session hasn't been completed yet
    val sleepEnd: Timestamp? = null,

    // Total sleep duration in minutes
    // Default 0 prevents null issues when Firestore maps data
    val durationMinutes: Long = 0,

    // Sleep quality rating (1–5 scale)
    // Default 3 acts as neutral value
    val quality: Int = 3,

    // Optional user note about sleep (dreams, interruptions, etc.)
    val note: String = ""
)
