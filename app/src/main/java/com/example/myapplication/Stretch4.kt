package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/*
  Stretch4 Activity
  - Final screen in the guided stretches flow
  - Provides:
      * Back: returns to the previous screen in the flow
      * Submit:
          1) increments the user's completedGoals in Firestore
          2) increments plant growth submit count (plantSubmits) to update plant stage/progress
          3) returns the user to the Dashboard tab in MainActivity
  - Note: There is no "Next" button here because this is the last stretch screen.
*/
class Stretch4 : AppCompatActivity() {

    // Repository that handles Firestore updates for goal tracking (completedGoals)
    private val goalsRepository = GoalsRepository()

    // Repository responsible for updating plant growth when the user submits a completed activity
    private val plantRepository = PlantRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enables edge-to-edge so the layout can extend behind system bars
        enableEdgeToEdge()

        // Loads the layout for the final stretch screen
        setContentView(R.layout.activity_stretch4)

        // BACK BUTTON:
        // Finishes this Activity and returns to the previous Activity in the stack
        val backButton = findViewById<Button>(R.id.btnBack)
        backButton.setOnClickListener {
            finish() // return to the previous screen
        }

        // SUBMIT BUTTON:
        // 1) Increment completedGoals in Firestore (Goal Tracker)
        // 2) Increment plantSubmits in Firestore (Plant Growth Tracker)
        // 3) If successful, restart MainActivity and open the Dashboard tab
        //    - Clearing the task prevents the user from navigating back into stretch screens
        val submitButton = findViewById<Button>(R.id.btnSubmit)
        submitButton.setOnClickListener {
            goalsRepository.incrementGoals { success ->
                if (success) {

                    // Added by Lesley Del Cid:
                    // Each successful submit also contributes to plant growth.
                    // PlantRepository handles stage thresholds (5 submits → sprout, 10 → bloom, 15 → complete).
                    plantRepository.incrementPlantSubmits { /* UI updates via Home snapshot listener */ }

                    val intent = Intent(this, MainActivity::class.java).apply {
                        // Start MainActivity as a fresh task
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                        // MainActivity reads this extra to switch to a specific tab on launch
                        putExtra("OPEN_TAB", "DASHBOARD")
                    }

                    startActivity(intent)
                    finish() // close Stretch4
                }
            }
        }

        // WINDOW INSETS:
        // Adds padding so the UI doesn't get covered by status/navigation bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}