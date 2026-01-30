package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Mood : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var btnSubmit: Button
    private lateinit var btnHappy: ImageButton
    private lateinit var btnOkay: ImageButton
    private lateinit var btnMad: ImageButton
    private lateinit var tvDate : TextView

    private var selectedMood: String? = null

    // For the calendar mood tracker just add the adapter and recycler view to this file

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mood)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val backButton = findViewById<Button>(R.id.btnBack)
        btnHappy = findViewById(R.id.happyButton)
        btnOkay = findViewById(R.id.okayButton)
        btnMad = findViewById(R.id.madButton)
        btnSubmit = findViewById(R.id.SubmitMood)
        tvDate = findViewById(R.id.date)

        setTodaysDate()

        backButton.setOnClickListener {
            finish() // return to Home
        }

        btnHappy.setOnClickListener {
            selectMood("happy")
        }
        btnOkay.setOnClickListener {
            selectMood("okay")
        }
        btnMad.setOnClickListener {
            selectMood("mad")
        }

        btnSubmit.setOnClickListener {
            if (selectedMood != null) {
                saveMood()
            }
            else{
                Toast.makeText(this, "Please select a mood first", Toast.LENGTH_SHORT).show()
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun selectMood (mood: String) {
        selectedMood = mood

        btnHappy.isSelected = (mood == "happy")
        btnOkay.isSelected = (mood == "okay")
        btnMad.isSelected = (mood == "mad")

    }

    private fun setTodaysDate() {
        val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        tvDate.text = "Log mood for $currentDate"
    }

    private fun saveMood() {
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "You must be logged in", Toast.LENGTH_SHORT).show()
            return
        }
        val entry = hashMapOf(
            "mood" to selectedMood,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users")
            .document(uid)
            .collection("moodEntries")
            .add(entry)
            .addOnSuccessListener {
                Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()

            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}