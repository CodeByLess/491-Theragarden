package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class Mood : AppCompatActivity() {

    // Firebase instances for authentication and database
    // Variable declarations for firebase and features
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    // Added by Lesley Del Cid:
    // Helper that updates completedGoals, plant progress, and returns to Dashboard
    private lateinit var completionHelper: SelfCareCompletionHelper

    // UI components
    private lateinit var btnSubmit: Button
    private lateinit var btnHappy: ImageButton
    private lateinit var btnOkay: ImageButton
    private lateinit var btnMad: ImageButton
    private lateinit var tvDate: TextView

    // Stores the currently selected mood
    private var selectedMood: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mood)

        // Initialize Firebase
        // Connection to firebase and firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Added by Lesley Del Cid:
        // Initialize reusable self-care completion helper
        completionHelper = SelfCareCompletionHelper(this)

        // Bind UI elements
        // Connect to UI elements by their ids
        val backButton = findViewById<Button>(R.id.btnBack)
        btnHappy = findViewById(R.id.happyButton)
        btnOkay = findViewById(R.id.okayButton)
        btnMad = findViewById(R.id.madButton)
        btnSubmit = findViewById(R.id.SubmitMood)
        tvDate = findViewById(R.id.date)

        // Display today's date
        setTodaysDate()

        // Navigate back to previous screen
        backButton.setOnClickListener {
            finish() // return to Home
        }

        // Mood selection listeners
        // Mood button click listeners
        btnHappy.setOnClickListener {
            selectMood("happy")
        }

        btnOkay.setOnClickListener {
            selectMood("okay")
        }

        btnMad.setOnClickListener {
            selectMood("mad")
        }

        // Submit mood to database
        btnSubmit.setOnClickListener {

            // Check if user selected mood and save if yes, if no send message
            if (selectedMood != null) {
                saveMood()
            } else {
                Toast.makeText(this, "Please select a mood first", Toast.LENGTH_SHORT).show()
            }
        }

        // System UI padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }
    }

    // Updates selected mood and highlights the chosen button
    // Mood selection, updates mood when selected
    private fun selectMood(mood: String) {

        selectedMood = mood

        // Highlight selected mood visually
        btnHappy.isSelected = (mood == "happy")
        btnOkay.isSelected = (mood == "okay")
        btnMad.isSelected = (mood == "mad")
    }

    // Formats and displays the current date
    // Updates date in format
    private fun setTodaysDate() {

        val dateFormat =
            SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())

        tvDate.text =
            "Log mood for ${dateFormat.format(Date())}"
    }

    // Saves the selected mood to Firestore under the current user
    // save moods in firebase to its corresponding user as long as its is logged in
    private fun saveMood() {

        // Ensure user is logged in
        val uid = auth.currentUser?.uid ?: run {

            Toast.makeText(
                this,
                "You must be logged in",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // Create mood entry object
        // mood and time is chose at saved to database under its user
        val entry = hashMapOf(
            "mood" to selectedMood,
            "createdAt" to System.currentTimeMillis() // timestamp for sorting and analytics
        )

        // Store mood entry in Firestore
        // Database collections created under users and mood entries
        db.collection("users")
            .document(uid)
            .collection("moodEntries")
            .add(entry)

            .addOnSuccessListener {
                Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()

                // Added by Lesley Del Cid:
                // After the mood is successfully saved, count this as a completed self-care activity.
                // This updates completedGoals, updates plant progress, and returns to Dashboard.
                completionHelper.completeSelfCareActivity()
            }

            .addOnFailureListener {
                Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show()
            }
    }
}