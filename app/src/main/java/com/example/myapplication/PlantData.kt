package com.example.myapplication

/*
  PlantData Data Class
  - Represents the plant-related state stored for a user in Firestore.
  - This model is used to track:
      1) Which seed is currently active
      2) The current progress of the plant
      3) Whether the plant cycle has been completed
      4) How many completed activities (submits) have contributed to plant growth
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
    val plantCompleted: Boolean = false,

    // Start at the dirt phase of your plant.
    // Three phases are dirt, sprout, bloom.
    // This is automatically updated based on plantSubmits thresholds.
    val plantStage: String = "dirt",

    /*
      plantSubmits
      - Tracks how many completed self-care activities (ex: stretch submits)
        have contributed to plant growth.
      - Used to determine stage thresholds:
            0–4 submits   → dirt
            5–9 submits   → sprout
            10 submits    → bloom
            10 submits    → completed
      - Reset to 0 when a new plant is started.
    */
    val plantSubmits: Int = 0,
    /*
      completedPlants
      - Tracks the total number of plants the user has fully grown.
      - This value increases every time a plant reaches the completed stage.
      - Used for statistics, achievements, or displaying plant history.
      - Default is 0 for a new user.
    */
    val completedPlants: Int = 0
)