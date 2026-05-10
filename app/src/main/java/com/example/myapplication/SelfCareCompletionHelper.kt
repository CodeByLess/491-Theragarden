package com.example.myapplication

import android.app.Activity
import android.content.Intent

/*
  Added by Lesley Del Cid:
  Helper for completing any self-care activity.

  This prevents us from copying the same submit logic into every self-care hub.
  It updates:
      1) completedGoals
      2) plant growth progress
      3) returns user to Dashboard
*/
class SelfCareCompletionHelper(
    private val activity: Activity
) {
    private val goalsRepository = GoalsRepository()
    private val plantRepository = PlantRepository()

    fun completeSelfCareActivity() {
        goalsRepository.incrementGoals { success ->
            if (success) {

                // Added by Lesley Del Cid:
                // Each completed self-care activity also grows the plant.
                plantRepository.incrementPlantSubmits {
                    // Home page updates through Firestore snapshot listener
                }

                val intent = Intent(activity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("OPEN_TAB", "DASHBOARD")
                }

                activity.startActivity(intent)
                activity.finish()
            }
        }
    }
}