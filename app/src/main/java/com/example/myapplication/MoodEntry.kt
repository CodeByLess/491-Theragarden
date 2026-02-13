package com.example.myapplication

data class MoodEntry(
    val id: String = "",
    val mood: String,
    val createdAt: Long = 0L
)