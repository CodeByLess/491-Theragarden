package com.example.myapplication

/*
  PlantData Data Class
  - Represents the plant-related state stored for a user in Firestore.
  - This model is used to track:
      1) Which seed is currently active
      2) The current progress of the plant
      3) Whether the plant cycle has been completed
  - Stored inside the users/{uid} document (merged with other user data).
*/
data class PlantData(

    // ID or name of the currently selected seed.
    // Example: "sunflower", "rose", etc.
    // Empty string means no plant has been selected yet.
    val currentSeedId: String = "",

    // Numeric progress value representing plant growth.
    // Can represent percentage (0–100) or progress toward a goal.
    // Default is 0 for a new plant.
    val plantProgress: Int = 0,

    // Indicates whether the plant growth cycle is complete.
    // When true, UI can show "Choose New Seed" button.
    val plantCompleted: Boolean = false
)
