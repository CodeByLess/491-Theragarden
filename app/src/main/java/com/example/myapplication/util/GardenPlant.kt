package com.example.myapplication.ui

// data class representing one plant in the user's garden // added by Les
data class GardenPlant(
    val documentId: String = "", // added by Les
    val seedName: String = "",
    val imageResId: Int = 0,
    val order: Int = 0 // added by Les
)