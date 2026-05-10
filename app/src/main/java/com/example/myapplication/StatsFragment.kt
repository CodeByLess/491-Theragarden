package com.example.myapplication.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

data class MoodEntry(
    val id: String = "",
    val mood: String = "",
    val createdAt: Long = 0L
)

class StatsFragment : Fragment() {

    // TextViews for displaying mood statistics
    private lateinit var tvLatestMood: TextView
    private lateinit var tvAverageMood: TextView
    private lateinit var tvHighestMood: TextView
    private lateinit var tvLowestMood: TextView
    private lateinit var tvTotalEntries: TextView
    private lateinit var tvStatus: TextView

    // Firebase instances for authentication and database access
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Inflate layout for stats page
        val root = inflater.inflate(R.layout.fragment_stats, container, false)

        // Bind UI components
        tvLatestMood = root.findViewById(R.id.tvLatestMood)
        tvAverageMood = root.findViewById(R.id.tvAverageMood)
        tvHighestMood = root.findViewById(R.id.tvHighestMood)
        tvLowestMood = root.findViewById(R.id.tvLowestMood)
        tvTotalEntries = root.findViewById(R.id.tvTotalEntries)
        tvStatus = root.findViewById(R.id.tvStatus)

        // Load mood data from Firestore
        loadMoodStats()

        return root
    }

    override fun onResume() {
        super.onResume()

        // Refresh data whenever user returns to this screen
        loadMoodStats()
    }

    // Retrieves mood data from Firestore and computes statistics
    private fun loadMoodStats() {

        // Ensure user is logged in
        val uid = auth.currentUser?.uid ?: run {
            tvStatus.text = "You must be logged in."
            return
        }

        tvStatus.text = "Loading mood statistics..."

        // Query user's mood entries ordered by timestamp
        db.collection("users")
            .document(uid)
            .collection("moodEntries")
            .orderBy("createdAt")
            .get()
            .addOnSuccessListener { result ->

                // Handle case where no data exists
                if (result.isEmpty) {
                    tvLatestMood.text = "Latest Mood: --"
                    tvAverageMood.text = "Average Mood: --"
                    tvHighestMood.text = "Highest Mood: --"
                    tvLowestMood.text = "Lowest Mood: --"
                    tvTotalEntries.text = "Total Entries: 0"
                    tvStatus.text = "No mood entries found yet."
                    return@addOnSuccessListener
                }

                val entries = mutableListOf<MoodEntry>()

                // Convert Firestore documents into MoodEntry objects
                for (doc in result.documents) {
                    val mood = doc.getString("mood") ?: continue
                    val createdAt = doc.getLong("createdAt") ?: 0L

                    entries.add(
                        MoodEntry(
                            id = doc.id,
                            mood = mood,
                            createdAt = createdAt
                        )
                    )
                }

                // Handle invalid data case
                if (entries.isEmpty()) {
                    tvStatus.text = "No valid mood data found."
                    return@addOnSuccessListener
                }

                // Convert moods into numeric scores for analysis
                val moodScores = entries.map { moodToScore(it.mood) }

                // Find latest entry based on timestamp
                val latestEntry = entries.maxByOrNull { it.createdAt }

                // Calculate statistics
                val average = moodScores.average()
                val highestScore = moodScores.maxOrNull() ?: 0
                val lowestScore = moodScores.minOrNull() ?: 0

                // Update UI with computed values
                tvLatestMood.text =
                    "Latest Mood: ${latestEntry?.mood?.replaceFirstChar { it.uppercase() } ?: "--"}"
                tvAverageMood.text = "Average Mood: ${"%.2f".format(average)} / 3"
                tvHighestMood.text = "Highest Mood: ${scoreToMood(highestScore)}"
                tvLowestMood.text = "Lowest Mood: ${scoreToMood(lowestScore)}"
                tvTotalEntries.text = "Total Entries: ${entries.size}"
                tvStatus.text = "Mood statistics loaded."
            }
            .addOnFailureListener { e ->
                // Handle database errors
                tvStatus.text = "Failed to load mood statistics: ${e.message}"
            }
    }

    // Converts mood string into numeric score for calculations
    private fun moodToScore(mood: String): Int {
        return when (mood.lowercase()) {
            "happy" -> 3
            "okay" -> 2
            "mad" -> 1
            else -> 0
        }
    }

    // Converts numeric score back into readable mood label
    private fun scoreToMood(score: Int): String {
        return when (score) {
            3 -> "Happy"
            2 -> "Okay"
            1 -> "Mad"
            else -> "Unknown"
        }
    }
}