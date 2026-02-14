package com.example.myapplication

/*
  Goals Data Class
  - Represents the goal-tracking data stored in Firestore for a user.
  - Currently contains a single field:
      completedGoals → number of goals the user has finished.
  - This model is used when updating or merging data into the
    users/{uid} document in Firestore.
*/
data class Goals(

    // Total number of completed goals for the current user.
    // Default value = 0 so a new user starts with zero completed goals.
    val completedGoals: Int = 0
)
