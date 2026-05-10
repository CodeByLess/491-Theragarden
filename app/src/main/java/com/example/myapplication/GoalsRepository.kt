package com.example.myapplication

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/*
  GoalsRepository
  - Handles all Firestore operations related to goal tracking.
  - Separates database logic from UI (Activities/Fragments),
    following a simple Repository pattern.
  - Currently responsible for incrementing the user's completedGoals field.
*/
class GoalsRepository {

    // Reference to Firestore database instance
    private val db = FirebaseFirestore.getInstance()

    // Reference to Firebase Authentication (used to get current user's UID)
    private val auth = FirebaseAuth.getInstance()

    /*
      incrementGoals
      - Increments the "completedGoals" field in the current user's Firestore document.
      - Uses a Firestore transaction to ensure the value updates safely
        even if multiple writes happen at the same time.
      - onDone callback returns:
          true  → if the update succeeded
          false → if the update failed
    */
    fun incrementGoals(onDone: (Boolean) -> Unit) {

        // Get the currently logged-in user's UID.
        // Added by Lesley Del Cid:
        // If no user is logged in, return false through the callback
        // instead of silently exiting.
        val uid = auth.currentUser?.uid
        if (uid == null) {
            onDone(false)
            return
        }

        // Reference to users/{uid} document
        val docRef = db.collection("users").document(uid)

        // Run a Firestore transaction to safely increment the counter
        db.runTransaction { transaction ->

            // Retrieve the current snapshot of the user document
            val snapshot = transaction.get(docRef)

            // Get existing completedGoals value (default to 0 if not found)
            val current = snapshot.getLong("completedGoals") ?: 0

            // Update the document using SetOptions.merge()
            // so we do NOT overwrite other fields in the user document
            transaction.set(
                docRef,
                Goals(current.toInt() + 1), // increment by 1
                SetOptions.merge()
            )
        }
            // If transaction succeeds
            .addOnSuccessListener {
                onDone(true)
            }
            // If transaction fails
            .addOnFailureListener {
                onDone(false)
            }
    }
}