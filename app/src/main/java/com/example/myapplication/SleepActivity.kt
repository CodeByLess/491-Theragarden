package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SleepActivity : AppCompatActivity() {

    // Firestore database instance
    private val db = FirebaseFirestore.getInstance()

    // Current logged-in user's UID
    // NOTE: Using !! assumes user is logged in.
    // If not authenticated, this will crash.
    private val userId = FirebaseAuth.getInstance().currentUser!!.uid

    // Holds the ID of the currently active sleep entry
    private var activeEntryId: String? = null

    // Stores when the user pressed "Start Sleep"
    private var startTimestamp: Timestamp? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sleep)

        // UI References
        val btnStart = findViewById<Button>(R.id.btnStartSleep)
        val btnEnd = findViewById<Button>(R.id.btnEndSleep)
        val ratingBar = findViewById<RatingBar>(R.id.ratingBar)
        val noteInput = findViewById<EditText>(R.id.etNote)

        // RecyclerView setup for displaying sleep history
        val recycler = findViewById<RecyclerView>(R.id.recyclerSleep)
        recycler.layoutManager = LinearLayoutManager(this)

        val adapter = SleepAdapter()
        recycler.adapter = adapter

        // =============================
        // START SLEEP BUTTON
        // =============================
        btnStart.setOnClickListener {

            // Capture current timestamp
            startTimestamp = Timestamp.now()

            // Create initial sleep entry with start time only
            val data = hashMapOf(
                "sleepStart" to startTimestamp
            )

            // Add new sleep document under:
            // users/{userId}/sleepEntries/{entryId}
            db.collection("users")
                .document(userId)
                .collection("sleepEntries")
                .add(data)
                .addOnSuccessListener {

                    // Save document ID so we can update it later
                    activeEntryId = it.id

                    // Disable start button and enable end button
                    btnStart.isEnabled = false
                    btnEnd.isEnabled = true
                }
        }

        // =============================
        // END SLEEP BUTTON
        // =============================
        btnEnd.setOnClickListener {

            // Get current timestamp as sleep end time
            val endTime = Timestamp.now()

            // Calculate sleep duration in minutes
            // NOTE: startTimestamp!! assumes start was pressed first
            val durationMinutes =
                (endTime.seconds - startTimestamp!!.seconds) / 60

            // Prepare fields to update in Firestore
            val update = mapOf(
                "sleepEnd" to endTime,
                "durationMinutes" to durationMinutes,
                "quality" to ratingBar.rating.toInt(),
                "note" to noteInput.text.toString()
            )

            // Update existing sleep document
            db.collection("users")
                .document(userId)
                .collection("sleepEntries")
                .document(activeEntryId!!)
                .update(update)

            // Reset UI state
            btnStart.isEnabled = true
            btnEnd.isEnabled = false
            noteInput.text.clear()
        }

        // =============================
        // LOAD SLEEP HISTORY (REAL-TIME)
        // =============================
        db.collection("users")
            .document(userId)
            .collection("sleepEntries")
            .orderBy("sleepStart")
            .addSnapshotListener { snapshot, _ ->

                // Convert Firestore documents into SleepEntry objects
                val list = snapshot?.toObjects(SleepEntry::class.java)

                // Submit list to RecyclerView adapter
                adapter.submitList(list)
            }
    }
}

