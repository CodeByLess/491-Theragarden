package com.example.myapplication.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class StatsViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Statistics"
    }
    val text: LiveData<String> = _text

    // Added by Lesley:
    // Stores the user's current overall day streak.
    private val _dayStreak = MutableLiveData<Int>().apply {
        value = 0
    }
    val dayStreak: LiveData<Int> = _dayStreak

    // Added by Lesley:
    // Stores task-specific streaks keyed by habit title.
    // Example: "Sleep 8 hours" -> 1
    private val _habitStreaks = MutableLiveData<Map<String, Int>>().apply {
        value = emptyMap()
    }
    val habitStreaks: LiveData<Map<String, Int>> = _habitStreaks

    // Added by Lesley:
    // Updates the current overall streak.
    fun setDayStreak(streak: Int) {
        _dayStreak.value = streak
    }

    // Added by Lesley:
    // Updates the task-specific streaks shown on the Stats page.
    fun setHabitStreaks(streaks: Map<String, Int>) {
        _habitStreaks.value = streaks
    }
}