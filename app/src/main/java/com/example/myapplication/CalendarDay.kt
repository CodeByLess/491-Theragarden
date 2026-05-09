package com.example.myapplication.ui.util

// Added by Lesley Del Cid:
// Represents one cell in the custom Stats calendar.
// Blank cells are used so the first day of the month starts on the correct weekday.
data class CalendarDay(
    val dayNumber: Int,
    val dateString: String,
    val isBlank: Boolean = false,
    val isSelected: Boolean = false,
    val isToday: Boolean = false,
    val hasTask: Boolean = false,
    val hasPlant: Boolean = false
)