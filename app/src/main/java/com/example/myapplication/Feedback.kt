package com.example.myapplication

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class Feedback : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)

        val ratingBar = findViewById<RatingBar>(R.id.ratingBar)
        val feedbackBox = findViewById<EditText>(R.id.etFeedback)
        val submitButton = findViewById<Button>(R.id.btnSubmit)
        val backButton = findViewById<Button>(R.id.btnBack)

        // Back button
        backButton.setOnClickListener {
            finish()
        }

        // Temporary submit behavior
        submitButton.setOnClickListener {

            val rating = ratingBar.rating
            val message = feedbackBox.text.toString()

            if (message.isEmpty()) {
                Toast.makeText(this, "Please enter feedback", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(
                this,
                "Thanks for your feedback!",
                Toast.LENGTH_SHORT
            ).show()

            // Clear fields
            ratingBar.rating = 0f
            feedbackBox.text.clear()
        }
    }
}