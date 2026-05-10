package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.time.LocalDate

class Affirmations : AppCompatActivity() {

    // SharedPreferences file name
    private val PREFS = "affirmation_prefs"

    // Keys for storing text and date
    private val KEY_TEXT = "daily_text"
    private val KEY_DATE = "daily_date"

    // Added by Lesley Del Cid:
    // Helper that updates completedGoals, plant progress, and returns to Dashboard
    private lateinit var completionHelper: SelfCareCompletionHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_affirmations)

        // Added by Lesley Del Cid:
        // Initialize reusable self-care completion helper
        completionHelper = SelfCareCompletionHelper(this)

        // Get UI elements
        val backButton = findViewById<Button>(R.id.btnBack)
        val saveButton = findViewById<Button>(R.id.btnSave)
        val editText = findViewById<EditText>(R.id.etAffirmation)

        // Go back to previous screen
        backButton.setOnClickListener { finish() }

        // Get SharedPreferences
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)

        // Get today's date
        val today = LocalDate.now().toString()

        // Get saved date from storage
        val savedDate = prefs.getString(KEY_DATE, "")

        // If it's the same day, load saved text
        // If it's a new day, clear old data
        if (savedDate == today) {
            editText.setText(prefs.getString(KEY_TEXT, ""))
        } else {
            prefs.edit().clear().apply()
        }

        // Save affirmation when button is clicked
        saveButton.setOnClickListener {

            prefs.edit()
                .putString(KEY_TEXT, editText.text.toString()) // save the text
                .putString(KEY_DATE, today) // save today's date
                .apply()

            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()

            // Added by Lesley Del Cid:
            // Saving an affirmation counts as completing a self-care activity.
            // This updates completedGoals, updates plant progress, and returns user to Dashboard.
            completionHelper.completeSelfCareActivity()
        }

        // Makes layout adjust for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}