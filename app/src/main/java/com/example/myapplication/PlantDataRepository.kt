package com.example.myapplication

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/*
  PlantRepository
  - Handles all Firestore operations related to plant growth.
  - Separates database logic from UI (Activities/Fragments),
    following a Repository pattern.
  - Responsible for:
      • Starting a new plant cycle (selecting a seed)
      • Updating plant progress and automatically assigning plant stages
        (dirt → sprout → bloom) using threshold rules
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
      - Initializes plantStage as "dirt" (first stage).
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
            currentSeedId = seedId,   // store selected seed
            plantProgress = 0,        // reset growth progress
            plantCompleted = false,   // mark as active (not completed)
            plantStage = "dirt",      // start stage of all plants


            // Reset submit count when the user starts a brand new plant
            plantSubmits = 0
        )

        // Update Firestore using merge so other user fields remain intact
        docRef.set(newPlant, SetOptions.merge())
            .addOnSuccessListener {


                // Reset bloomReached flag when starting a new plant cycle
                // This is used later for showing bloom popups anywhere in the app
                docRef.set(mapOf("bloomReached" to false), SetOptions.merge())

                onDone(true)  // Notify caller that update succeeded
            }
            .addOnFailureListener {
                onDone(false) // Notify caller that update failed
            }
    }

    /*
      updatePlantProgress
      - Updates the user's plant progress and automatically sets the plant stage
        using threshold rules (Use Case 4.2).
      - Also determines whether the plant is completed.

      Threshold Rules:
      - 0  to 33  → dirt
      - 34 to 66  → sprout
      - 67 to 99  → bloom
      - 100+      → bloom + completed

      Why this exists:
      - Keeps the stage logic in ONE place (repository),
        so Activities/Fragments only pass in the new progress value.
      - Ensures Firestore fields stay consistent:
          plantProgress, plantStage, and plantCompleted update together.

      Parameters:
      newProgress → New progress value (expected 0–100).
      onDone      → Callback returning:
                     true  if update succeeds
                     false if update fails
    */
    fun updatePlantProgress(newProgress: Int, onDone: (Boolean) -> Unit) {

        // Get currently logged-in user's UID.
        // If no user is logged in, exit early.
        val uid = auth.currentUser?.uid ?: return

        // Reference to users/{uid} document
        val docRef = db.collection("users").document(uid)

        // Clamp progress to keep it within a safe range (0–100)
        val safeProgress = newProgress.coerceIn(0, 100)

        // Determine plant stage based on progress thresholds
        val stage = when {
            safeProgress >= 67 -> "bloom"
            safeProgress >= 34 -> "sprout"
            else -> "dirt"
        }

        // Plant is considered complete when progress reaches 100
        val completed = safeProgress >= 100

        // Update only the plant-related fields using merge
        val update = mapOf(
            "plantProgress" to safeProgress,
            "plantStage" to stage,
            "plantCompleted" to completed
        )

        // Save updates to Firestore (merge prevents overwriting other user fields)
        docRef.set(update, SetOptions.merge())
            .addOnSuccessListener {
                onDone(true)  // Notify caller that update succeeded
            }
            .addOnFailureListener {
                onDone(false) // Notify caller that update failed
            }
    }

    /*
      incrementPlantSubmits
      - Called when the user submits a completed self-care activity (ex: Stretch Submit).
      - Increments plantSubmits by 1 and updates plantStage/plantProgress automatically.
      - Uses a Firestore transaction so the submit count updates safely (no race conditions).

      Stage Thresholds (based on submits):
      - 0–4 submits   → dirt
      - 5–9 submits   → sprout
      - 10–14 submits → bloom
      - 15 submits    → completed (shows "Choose New Seed" on Home)

      Progress Bar:
      - Converts plantSubmits into a percentage so the Home progress bar can still show growth.
      - Example: 15 submits = 100%
    */
    fun incrementPlantSubmits(onDone: (Boolean) -> Unit) {

        // Get currently logged-in user's UID.
        // If no user is logged in, exit early.
        val uid = auth.currentUser?.uid ?: return

        // Reference to users/{uid} document
        val docRef = db.collection("users").document(uid)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)

            // Read current submit count (default to 0 if missing)
            val currentSubmits = snapshot.getLong("plantSubmits")?.toInt() ?: 0
            val newSubmits = currentSubmits + 1

            // Determine stage from submit thresholds
            val stage = when {
                newSubmits >= 10 -> "bloom"
                newSubmits >= 5 -> "sprout"
                else -> "dirt"
            }

            // bloomReached becomes true once the plant hits bloom (10+ submits)
            // This is used later to trigger a bloom popup on any screen
            val bloomReached = newSubmits >= 10

            // Completed after 15 submits
            val completed = newSubmits >= 15


            // Make progress bar look "full" once bloom is reached (10+ submits)
            val progress = if (newSubmits >= 10) 100
            else ((newSubmits / 10.0) * 100).toInt().coerceIn(0, 100)

            // Save only plant-related fields using merge
            transaction.set(
                docRef,
                mapOf(
                    "plantSubmits" to newSubmits,
                    "plantStage" to stage,
                    "bloomReached" to bloomReached,
                    "plantCompleted" to completed,
                    "plantProgress" to progress
                ),
                SetOptions.merge()
            )
        }.addOnSuccessListener {
            onDone(true)
        }.addOnFailureListener {
            onDone(false)
        }
    }
}