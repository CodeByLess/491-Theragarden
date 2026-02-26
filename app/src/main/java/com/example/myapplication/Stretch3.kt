package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/*
  Stretch3 Activity
  - Third screen in the guided stretches
  - Provides:
      * Back: returns to the previous stretch screen
      * Next: advances to Stretch4
      * Submit:
          1) increments the user's completedGoals in Firestore
          2) increments plant growth submit count (plantSubmits) to update plant stage/progress
          3) returns the user to the Dashboard tab in MainActivity
*/
class Stretch3 : AppCompatActivity() {

    // Repository that updates the user's "completedGoals" field in Firestore.
    // Keeping DB logic in a repository avoids duplicating Firestore code in the UI.
    private val goalsRepository = GoalsRepository()

    // Repository responsible for updating plant growth when the user submits a completed activity
    private val plantRepository = PlantRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enables edge-to-edge layout so UI can draw behind system bars
        enableEdgeToEdge()

        // Loads the layout for this Activity
        setContentView(R.layout.activity_stretch3)

        // BACK BUTTON:
        // Finishes this Activity and returns to the previous Activity in the stack
        val backButton = findViewById<Button>(R.id.btnBack)
        backButton.setOnClickListener {
            finish() // return to the previous stretch screen
        }

        // NEXT BUTTON:
        // Advances the user forward in the stretches sequence to Stretch4
        val nextButton = findViewById<Button>(R.id.btnNext)
        nextButton.setOnClickListener {
            val intent = Intent(this, Stretch4::class.java)
            startActivity(intent)
        }

        // SUBMIT BUTTON:
        // When the user finishes the stretch, we:
        // 1) Increment completedGoals in Firestore
        // 2) Increment plantSubmits in Firestore (Plant Growth Tracker)
        // 3) Restart MainActivity and open the Dashboard tab
        //    - Clears the back stack so the user can't navigate back into stretch screens
        val submitButton = findViewById<Button>(R.id.btnSubmit)
        submitButton.setOnClickListener {
            goalsRepository.incrementGoals { success ->
                if (success) {

                    // Added by Lesley Del Cid:
                    // Each successful submit also contributes to plant growth.
                    // PlantRepository handles stage thresholds (5 submits → sprout, 10 → bloom, 15 → complete).
                    plantRepository.incrementPlantSubmits { /* UI updates via Home snapshot listener */ }

                    val intent = Intent(this, MainActivity::class.java).apply {
                        // Clears existing Activities from the back stack
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                        // Custom flag read in MainActivity to open a specific bottom-nav tab
                        putExtra("OPEN_TAB", "DASHBOARD")
                    }

                    startActivity(intent)
                    finish() // close this Activity
                }
            }
        }

        // WINDOW INSETS:
        // Adds padding so content isn't hidden behind status/navigation bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}