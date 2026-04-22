package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class Mood : AppCompatActivity() {

    // Firebase instances for authentication and database
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

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
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Bind UI elements
        val backButton = findViewById<Button>(R.id.btnBack)
        btnHappy = findViewById(R.id.happyButton)
        btnOkay = findViewById(R.id.okayButton)
        btnMad = findViewById(R.id.madButton)
        btnSubmit = findViewById(R.id.SubmitMood)
        tvDate = findViewById(R.id.date)

        // Display today's date
        setTodaysDate()

        // Navigate back to previous screen
        backButton.setOnClickListener { finish() }

        // Mood selection listeners
        btnHappy.setOnClickListener { selectMood("happy") }
        btnOkay.setOnClickListener { selectMood("okay") }
        btnMad.setOnClickListener { selectMood("mad") }

        // Submit mood to database
        btnSubmit.setOnClickListener {
            if (selectedMood != null) {
                saveMood()
            } else {
                Toast.makeText(this, "Please select a mood first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Updates selected mood and highlights the chosen button
    private fun selectMood(mood: String) {
        selectedMood = mood

        // Highlight selected mood visually
        btnHappy.isSelected = (mood == "happy")
        btnOkay.isSelected = (mood == "okay")
        btnMad.isSelected = (mood == "mad")
    }

    // Formats and displays the current date
    private fun setTodaysDate() {
        val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
        tvDate.text = "Log mood for ${dateFormat.format(Date())}"
    }

    // Saves the selected mood to Firestore under the current user
    private fun saveMood() {

        // Ensure user is logged in
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "You must be logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Create mood entry object
        val entry = hashMapOf(
            "mood" to selectedMood,
            "createdAt" to System.currentTimeMillis() // timestamp for sorting and analytics
        )

        // Store mood entry in Firestore
        db.collection("users")
            .document(uid)
            .collection("moodEntries")
            .add(entry)
            .addOnSuccessListener {
                Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show()
            }
    }
}