package com.example.myapplication

import android.text.InputType
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class Hydration : AppCompatActivity() {

    // Added by Lesley Del Cid:
    // SharedPreferences saves hydration data so it does not reset when the user exits the activity.
    private val prefsName = "HydrationPrefs"

    // Added by Lesley Del Cid:
    // Helper that updates completedGoals, plant progress, and returns to Dashboard
    private lateinit var completionHelper: SelfCareCompletionHelper

    // Class-level properties (NOT inside onCreate)
    private var bottleCount = 0
    private var goalCount = 8
    private var goalsMet = 0

    // Views declared at class level so all functions can access them
    private lateinit var tvBottleCount: TextView
    private lateinit var tvGoalProgress: TextView
    private lateinit var tvGoalPercent: TextView
    private lateinit var tvGoalsMet: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_hydration)

        // Added by Lesley Del Cid:
        // Initialize reusable self-care completion helper
        completionHelper = SelfCareCompletionHelper(this)

        // Added by Lesley Del Cid:
        // Load saved hydration values before updating the screen.
        loadHydrationData()

        // Wire up views
        tvBottleCount = findViewById(R.id.tvBottleCount)
        tvGoalProgress = findViewById(R.id.tvGoalProgress)
        tvGoalPercent = findViewById(R.id.tvGoalPercent)
        tvGoalsMet = findViewById(R.id.tvGoalsMet)

        // Back button
        val backButton = findViewById<Button>(R.id.btnBack)
        backButton.setOnClickListener { finish() }

        // Increment button
        val btnIncrement = findViewById<Button>(R.id.btnIncrement)
        btnIncrement.setOnClickListener { onIncrement() }

        // Decrement button
        val btnDecrement = findViewById<Button>(R.id.btnDecrement)
        btnDecrement.setOnClickListener {
            if (bottleCount > 0) {
                bottleCount--
                saveHydrationData()
                updateUI()
            }
        }

        // Edit goal button
        val btnEditGoal = findViewById<Button>(R.id.btnEditGoal)
        btnEditGoal.setOnClickListener {
            val input = EditText(this).apply {
                inputType = InputType.TYPE_CLASS_NUMBER
                setText(goalCount.toString())
                hint = "Enter bottle goal"
            }

            MaterialAlertDialogBuilder(this)
                .setTitle("Set Daily Goal")
                .setView(input)
                .setPositiveButton("Save") { _, _ ->
                    val newGoal = input.text.toString().toIntOrNull()
                    if (newGoal != null && newGoal > 0) {
                        goalCount = newGoal

                        // Added by Lesley Del Cid:
                        // Save edited goal so it stays after leaving the activity.
                        saveHydrationData()

                        updateUI()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Set initial UI state
        updateUI()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun updateUI() {
        tvBottleCount.text = bottleCount.toString()
        tvGoalProgress.text = "$bottleCount / $goalCount bottles"

        val percent = ((bottleCount.toFloat() / goalCount) * 100).toInt().coerceAtMost(100)
        tvGoalPercent.text = "$percent%"

        // Added by Lesley Del Cid:
        // Keeps goals met text updated when data is loaded again.
        tvGoalsMet.text = goalsMet.toString()
    }

    private fun onIncrement() {
        bottleCount++

        // Added by Lesley Del Cid:
        // Save bottle count every time it changes.
        saveHydrationData()

        updateUI()

        if (bottleCount == goalCount) {
            showGoalMetDialog()
        }
    }

    private fun showGoalMetDialog() {
        goalsMet++

        // Added by Lesley Del Cid:
        // Save goalsMet so it does not reset when leaving the activity.
        saveHydrationData()

        tvGoalsMet.text = goalsMet.toString()

        MaterialAlertDialogBuilder(this)
            .setTitle("🎉 Goal Met!")
            .setMessage("Amazing! You hit your goal of $goalCount bottles today!")
            .setPositiveButton("Awesome!") { dialog, _ ->
                dialog.dismiss()

                // Added by Lesley Del Cid:
                // Since the hydration goal was completed, count it as a completed self-care activity.
                // This updates completedGoals, updates plant progress, and returns to Dashboard.
                completionHelper.completeSelfCareActivity()
            }
            .show()
    }

    // Added by Lesley Del Cid:
    // Loads saved hydration values from SharedPreferences.
    private fun loadHydrationData() {
        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)

        bottleCount = prefs.getInt("bottleCount", 0)
        goalCount = prefs.getInt("goalCount", 8)
        goalsMet = prefs.getInt("goalsMet", 0)
    }

    // Added by Lesley Del Cid:
    // Saves hydration values so they remain after leaving and reopening the activity.
    private fun saveHydrationData() {
        val prefs = getSharedPreferences(prefsName, MODE_PRIVATE)

        prefs.edit()
            .putInt("bottleCount", bottleCount)
            .putInt("goalCount", goalCount)
            .putInt("goalsMet", goalsMet)
            .apply()
    }
}