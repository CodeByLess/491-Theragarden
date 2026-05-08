package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class Sleep : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var startTimestamp: Timestamp? = null

    private lateinit var adapter: SleepLogAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sleep)

        // Back
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        val btnStart = findViewById<Button>(R.id.btnStartSleep)
        val btnEnd = findViewById<Button>(R.id.btnEndSleep)
        val ratingBar = findViewById<RatingBar>(R.id.ratingBar)
        val etNote = findViewById<EditText>(R.id.etNote)

        val recycler = findViewById<RecyclerView>(R.id.recyclerSleep)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = SleepLogAdapter()
        recycler.adapter = adapter

        // Load history now + live updates
        listenForSleepLogs()

        // Start Sleep
        btnStart.setOnClickListener {
            startTimestamp = Timestamp.now()
            btnStart.isEnabled = false
            btnEnd.isEnabled = true
            Toast.makeText(this, "Sleep started", Toast.LENGTH_SHORT).show()
        }

        // End Sleep -> save log to Firestore
        btnEnd.setOnClickListener {
            val user = auth.currentUser
            if (user == null) {
                Toast.makeText(this, "You must be logged in.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val start = startTimestamp
            if (start == null) {
                Toast.makeText(this, "Press Start Sleep first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val end = Timestamp.now()
            val durationMinutes = (end.seconds - start.seconds) / 60

            val log = hashMapOf(
                "startTime" to start,
                "endTime" to end,
                "durationMinutes" to durationMinutes,
                "quality" to ratingBar.rating.toInt(),
                "note" to etNote.text.toString()
            )

            db.collection("users")
                .document(user.uid)
                .collection("sleepLogs")
                .add(log)
                .addOnSuccessListener {
                    Toast.makeText(this, "Saved sleep log", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_LONG).show()
                }

            // Reset UI
            startTimestamp = null
            btnStart.isEnabled = true
            btnEnd.isEnabled = false
            etNote.text.clear()
        }
    }

    private fun listenForSleepLogs() {
        val user = auth.currentUser ?: return

        db.collection("users")
            .document(user.uid)
            .collection("sleepLogs")
            .orderBy("startTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                val list = snap?.toObjects(SleepLog::class.java) ?: emptyList()
                adapter.submitList(list)
            }
    }
}
