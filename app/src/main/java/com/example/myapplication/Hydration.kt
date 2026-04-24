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
    }

    private fun onIncrement() {
        bottleCount++
        updateUI()
        if (bottleCount == goalCount) {
            showGoalMetDialog()
        }
    }

    private fun showGoalMetDialog() {
        goalsMet++
        tvGoalsMet.text = goalsMet.toString()

        MaterialAlertDialogBuilder(this)
            .setTitle("🎉 Goal Met!")
            .setMessage("Amazing! You hit your goal of $goalCount bottles today!")
            .setPositiveButton("Awesome!") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}