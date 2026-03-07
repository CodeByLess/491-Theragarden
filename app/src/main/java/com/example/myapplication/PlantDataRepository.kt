package com.example.myapplication

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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
      • Tracking the total number of plants the user has completed
      • Saving completed plant entries into the user's garden collection
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

      IMPORTANT:
      - Only the CURRENT plant fields should reset here.
      - The completedPlants field should not reset, because it stores
        the user's lifetime total of completed plants.

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

        /*
          Reset only the active plant fields.
          Do not include completedPlants here, or it would reset back to 0
          every time the user chooses a new seed.
        */
        val newPlantData = mapOf(
            "currentSeedId" to seedId,   // store selected seed
            "plantProgress" to 0,        // reset growth progress
            "plantCompleted" to false,   // mark as active (not completed)
            "plantStage" to "dirt",      // start stage of all plants
            "plantSubmits" to 0,         // reset submit count for the new plant
            "bloomReached" to false      // reset bloom popup flag for new cycle
        )

        // Update Firestore using merge so other user fields remain intact
        docRef.set(newPlantData, SetOptions.merge())
            .addOnSuccessListener {
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
      - Also updates completedPlants when a plant reaches the completed stage.
      - Saves the completed plant name into the user's garden collection
        the first time that plant reaches completion.

      Stage Thresholds (based on submits):
      - 0–4 submits   → dirt
      - 5–9 submits   → sprout
      - 10+ submits   → bloom + completed

      Progress Bar:
      - Converts plantSubmits into a percentage so the Home progress bar can still show growth.
      - Example: 10 submits = 100%

      completedPlants:
      - Stores the total number of plants the user has fully completed.
      - Increases only once when the plant changes from not completed
        to completed.
      - This prevents duplicate counting if the user somehow submits again
        after the plant is already complete.

      garden:
      - Saves a completed plant entry into users/{uid}/garden.
      - Each entry stores the completed seed name and a completion timestamp.
      - A new garden entry is added only once per completed plant cycle.
    */
    fun incrementPlantSubmits(onDone: (Boolean) -> Unit) {

        // Get currently logged-in user's UID.
        // If no user is logged in, exit early.
        val uid = auth.currentUser?.uid ?: return

        // Reference to users/{uid} document
        val docRef = db.collection("users").document(uid)

        /*
          Used after the transaction completes successfully.
          If a plant finishes for the first time, we save its name into
          the user's garden collection.
        */
        var shouldSaveToGarden = false
        var completedSeedName = ""

        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)

            // Read current submit count (default to 0 if missing)
            val currentSubmits = snapshot.getLong("plantSubmits")?.toInt() ?: 0
            val newSubmits = currentSubmits + 1

            /*
              Read whether the plant had already been completed before
              this new submit.
              This is used to prevent completedPlants from increasing more
              than once for the same plant.
            */
            val wasAlreadyCompleted = snapshot.getBoolean("plantCompleted") ?: false

            /*
              Read the user's current total number of completed plants.
              If the field does not exist yet, start from 0.
            */
            val currentCompletedPlants = snapshot.getLong("completedPlants")?.toInt() ?: 0

            /*
              Read the currently active seed name.
              This value will be saved into the user's garden collection
              if the plant becomes completed for the first time.
            */
            val currentSeedId = snapshot.getString("currentSeedId") ?: ""

            // Determine stage from submit thresholds
            val stage = when {
                newSubmits >= 10 -> "bloom"
                newSubmits >= 5 -> "sprout"
                else -> "dirt"
            }

            // bloomReached becomes true once the plant hits bloom (10+ submits)
            // This is used later to trigger a bloom popup on any screen
            val bloomReached = newSubmits >= 10

            // Plant becomes completed at 10 submits
            val completed = newSubmits >= 10

            /*
              Increase completedPlants only the FIRST time the current plant
              reaches completion.
              Example:
              - 9 → 10 submits = increment completedPlants
              - 10 → 11 submits = do NOT increment again
            */
            val updatedCompletedPlants =
                if (completed && !wasAlreadyCompleted) {
                    currentCompletedPlants + 1
                } else {
                    currentCompletedPlants
                }

            /*
              Mark this plant to be saved into the user's garden only once,
              when it changes from not completed to completed.
            */
            if (completed && !wasAlreadyCompleted && currentSeedId.isNotBlank()) {
                shouldSaveToGarden = true
                completedSeedName = currentSeedId
            }

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
                    "plantProgress" to progress,

                    // Stores the user's lifetime total of completed plants
                    "completedPlants" to updatedCompletedPlants
                ),
                SetOptions.merge()
            )
        }.addOnSuccessListener {

            /*
              Save the completed plant into the user's garden collection
              after the transaction succeeds.
              This creates a new garden document for each completed plant.
            */
            if (shouldSaveToGarden) {
                val gardenEntry = mapOf(
                    "seedName" to completedSeedName,
                    "completedAt" to FieldValue.serverTimestamp()
                )

                db.collection("users")
                    .document(uid)
                    .collection("garden")
                    .add(gardenEntry)
                    .addOnSuccessListener {
                        onDone(true)
                    }
                    .addOnFailureListener {
                        onDone(false)
                    }
            } else {
                onDone(true)
            }

        }.addOnFailureListener {
            onDone(false)
        }
    }
}