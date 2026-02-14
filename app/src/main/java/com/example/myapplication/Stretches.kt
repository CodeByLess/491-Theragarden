package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/*
  Stretches Activity
  - Screen for a guided stretch flow
  - Handles navigation (Back / Next)
  - On Submit, records progress by incrementing the user's completedGoals in Firestore
  - Then returns the user to the Dashboard tab in MainActivity
*/
class Stretches : AppCompatActivity() {

    // Repository responsible for updating the user's "completedGoals" field in Firestore
    private val goalsRepository = GoalsRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enables edge-to-edge drawing so the layout can extend behind system bars
        enableEdgeToEdge()

        // Loads the UI layout for this Activity
        setContentView(R.layout.activity_stretches)

        // BACK BUTTON:
        // Finishes this Activity and returns to the previous screen in the back stack
        val backButton = findViewById<Button>(R.id.btnBack)
        backButton.setOnClickListener {
            finish() // return to the previous screen
        }

        // NEXT BUTTON:
        // Moves the user forward to the next screen (Stretch2)
        val nextButton = findViewById<Button>(R.id.btnNext)
        nextButton.setOnClickListener {
            val intent = Intent(this, Stretch2::class.java)
            startActivity(intent)
        }

        // SUBMIT BUTTON:
        // 1) Updates the user's "completedGoals" in Firestore (Goal Tracker)
        // 2) If successful, navigates back to MainActivity and opens the Dashboard tab
        val submitButton = findViewById<Button>(R.id.btnSubmit)
        submitButton.setOnClickListener {

            // Increment goals using a Firestore transaction in GoalsRepository
            goalsRepository.incrementGoals { success ->
                if (success) {

                    // Relaunch MainActivity as a fresh task, and tell it to open the Dashboard tab
                    val intent = Intent(this, MainActivity::class.java).apply {
                        // Clear old Activities so the back button won't return to this stretch screen
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                        // Custom extra that MainActivity checks to navigate to a specific tab
                        putExtra("OPEN_TAB", "DASHBOARD")
                    }

                    startActivity(intent)
                    finish() // close this Activity so it is removed from memory / back stack
                }
            }
        }

        // WINDOW INSETS:
        // Adds padding so content doesn't get hidden behind the status bar / navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}