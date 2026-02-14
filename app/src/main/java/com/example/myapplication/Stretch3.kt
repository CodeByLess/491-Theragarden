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
      * Submit: records completion by incrementing the user's completedGoals in Firestore
        and then returns the user to the Dashboard tab in MainActivity
*/
class Stretch3 : AppCompatActivity() {

    // Repository that updates the user's "completedGoals" field in Firestore.
    // Keeping DB logic in a repository avoids duplicating Firestore code in the UI.
    private val goalsRepository = GoalsRepository()

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
        // 2) Restart MainActivity and open the Dashboard tab
        //    - Clears the back stack so the user can't navigate back into stretch screens
        val submitButton = findViewById<Button>(R.id.btnSubmit)
        submitButton.setOnClickListener {
            goalsRepository.incrementGoals { success ->
                if (success) {
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