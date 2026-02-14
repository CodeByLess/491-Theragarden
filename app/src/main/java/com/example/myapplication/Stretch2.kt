package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/*
  Stretch2 Activity
  - Second screen in the guided stretches
  - Provides:
      * Back: returns to the previous stretch screen (Stretch1 / Stretches.kt)
      * Next: advances to Stretch3
      * Submit: records completion by incrementing the user's completedGoals in Firestore
        and then returns to the Dashboard tab in MainActivity
*/
class Stretch2 : AppCompatActivity() {

    // Repository that updates the user's completedGoals in Firestore.
    // Using a repository keeps Firestore logic out of the UI layer.
    private val goalsRepository = GoalsRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enables edge-to-edge layout (content can draw under status/navigation bars)
        enableEdgeToEdge()

        // Loads the layout XML for this stretch screen
        setContentView(R.layout.activity_stretch2)

        // BACK BUTTON:
        // Closes this Activity and returns to the previous Activity in the stack
        val backButton = findViewById<Button>(R.id.btnBack)
        backButton.setOnClickListener {
            finish() // return to the previous screen
        }

        // NEXT BUTTON:
        // Advances to the next guided stretch screen (Stretch3)
        val nextButton = findViewById<Button>(R.id.btnNext)
        nextButton.setOnClickListener {
            val intent = Intent(this, Stretch3::class.java)
            startActivity(intent)
        }

        // SUBMIT BUTTON:
        // 1) Increments completedGoals in Firestore (Goal Tracker)
        // 2) If successful, restarts MainActivity and opens the Dashboard tab
        //    - Clears back stack so user can't navigate back into the guided stretch
        val submitButton = findViewById<Button>(R.id.btnSubmit)
        submitButton.setOnClickListener {
            goalsRepository.incrementGoals { success ->
                if (success) {
                    val intent = Intent(this, MainActivity::class.java).apply {
                        // Start fresh so Back won't return to Stretch2/Stretch3 screens
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK

                        // Used by MainActivity to navigate to the dashboard tab
                        putExtra("OPEN_TAB", "DASHBOARD")
                    }

                    startActivity(intent)
                    finish() // close this Activity
                }
            }
        }

        // WINDOW INSETS:
        // Apply padding so UI isn't covered by system bars (status/navigation)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}