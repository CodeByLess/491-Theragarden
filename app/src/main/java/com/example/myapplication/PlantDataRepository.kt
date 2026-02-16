package com.example.myapplication

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/*
  PlantRepository
  - Handles all Firestore operations related to plant growth.
  - Separates database logic from UI (Activities/Fragments),
    following a Repository pattern.
  - Currently responsible for starting a new plant cycle.
*/
class PlantRepository {

    // Firestore database reference
    private val db = FirebaseFirestore.getInstance()

    // Firebase Authentication reference (used to get current user's UID)
    private val auth = FirebaseAuth.getInstance()

    /*
      startNewPlant
      - Starts a new plant growth cycle for the current user.
      - Resets plant progress to 0.
      - Marks plantCompleted as false.
      - Stores the selected seed ID in Firestore.
      - Uses SetOptions.merge() to avoid overwriting other user data.

      Parameters:
      seedId → The ID/name of the selected seed (e.g., "sunflower").
      onDone → Callback returning:
                true  if update succeeds
                false if update fails
    */
    fun startNewPlant(seedId: String, onDone: (Boolean) -> Unit) {

        // Get currently logged-in user's UID.
        // If no user is logged in, exit early.
        val uid = auth.currentUser?.uid ?: return

        // Reference to users/{uid} document
        val docRef = db.collection("users").document(uid)

        // Create a new PlantData object with reset progress
        val newPlant = PlantData(
            currentSeedId = seedId,  // store selected seed
            plantProgress = 0,      // reset growth progress
            plantCompleted = false  // mark as active (not completed)
        )

        // Update Firestore using merge so other user fields remain intact
        docRef.set(newPlant, SetOptions.merge())
            .addOnSuccessListener {
                onDone(true)  // Notify caller that update succeeded
            }
            .addOnFailureListener {
                onDone(false) // Notify caller that update failed
            }
    }
}
