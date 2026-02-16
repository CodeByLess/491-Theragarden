package com.example.myapplication

data class MusicTitle(
    val name: String,
    val audioResId: Int,
    var isPlaying: Boolean = false,
    var progress: Int = 0
)
