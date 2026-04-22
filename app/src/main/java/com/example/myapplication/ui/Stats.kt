package com.example.myapplication.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.myapplication.MoodEntry
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentStatsBinding
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Stats : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)

        setupChart()

        binding.btnSelfCareHub.setOnClickListener {
            activity
                ?.findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.nav_view)
                ?.selectedItemId = R.id.navigation_dashboard
        }

        loadMoodStats()
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        loadMoodStats()
    }

    // CHART SETUP
    private fun setupChart() {
        val chart = binding.moodChart

        chart.setBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.dark_gray_brown)
        )

        chart.setNoDataText("No mood data yet.")
        chart.setNoDataTextColor(
            ContextCompat.getColor(requireContext(), R.color.white)
        )

        val description = Description()
        description.text = ""
        chart.description = description

        chart.axisRight.isEnabled = false
        chart.legend.textColor = ContextCompat.getColor(requireContext(), R.color.white)

        // Y Axis (Mood labels)
        chart.axisLeft.apply {
            axisMinimum = 1f
            axisMaximum = 3f
            granularity = 1f
            textColor = ContextCompat.getColor(requireContext(), R.color.white)
            setDrawGridLines(false)

            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return when (value.toInt()) {
                        1 -> "Mad"
                        2 -> "Okay"
                        3 -> "Happy"
                        else -> ""
                    }
                }
            }
        }

        // X Axis
        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            textColor = ContextCompat.getColor(requireContext(), R.color.white)
            setDrawGridLines(false)
            granularity = 1f
        }
    }


    // LOAD DATA
    private fun loadMoodStats() {
        val uid = auth.currentUser?.uid ?: run {
            binding.tvStatus.text = "You must be logged in."
            return
        }

        binding.tvStatus.text = "Loading mood statistics..."

        db.collection("users")
            .document(uid)
            .collection("moodEntries")
            .orderBy("createdAt")
            .get()
            .addOnSuccessListener { result ->

                val entries = result.documents.mapNotNull { doc ->
                    val mood = doc.getString("mood") ?: return@mapNotNull null
                    val createdAt = doc.getLong("createdAt") ?: 0L

                    MoodEntry(
                        id = doc.id,
                        mood = mood,
                        createdAt = createdAt
                    )
                }

                if (entries.isEmpty()) {
                    binding.tvLatestMood.text = "Latest: --"
                    binding.tvAverageMood.text = "Average: --"
                    binding.tvHighestMood.text = "Highest: --"
                    binding.tvLowestMood.text = "Lowest: --"
                    binding.tvTotalEntries.text = "Entries: 0"
                    binding.tvSupportMessage.text = ""
                    binding.btnSelfCareHub.visibility = View.GONE
                    binding.tvStatus.text = "No mood entries found yet."
                    binding.moodChart.clear()
                    return@addOnSuccessListener
                }

                val scores = entries.map { moodToScore(it.mood) }
                val avg = scores.average()

                val latestMood = entries.last().mood
                val highest = scores.maxOrNull() ?: 0
                val lowest = scores.minOrNull() ?: 0

                // Update stats
                binding.tvLatestMood.text = "Latest: ${latestMood.replaceFirstChar { it.uppercase() }}"
                binding.tvAverageMood.text = "Average: ${"%.2f".format(avg)} / 3"
                binding.tvHighestMood.text = "Highest: ${scoreToMood(highest)}"
                binding.tvLowestMood.text = "Lowest: ${scoreToMood(lowest)}"
                binding.tvTotalEntries.text = "Entries: ${entries.size}"
                binding.tvStatus.text = ""

                renderChart(entries)
                showSupport(latestMood, avg)
            }
            .addOnFailureListener { e ->
                binding.tvStatus.text = "Failed to load mood statistics: ${e.message}"
            }
    }

    // RENDER CHART
    private fun renderChart(entries: List<MoodEntry>) {
        val chartEntries = entries.mapIndexed { index, entry ->
            Entry(index.toFloat(), moodToScore(entry.mood).toFloat())
        }

        val dataSet = LineDataSet(chartEntries, "Mood Trend").apply {
            setColor(ContextCompat.getColor(requireContext(), R.color.gray_green))
            setCircleColor(ContextCompat.getColor(requireContext(), R.color.white))
            lineWidth = 3f
            circleRadius = 6f
            setDrawValues(false)
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.white)
        }

        val lineData = LineData(dataSet)
        binding.moodChart.data = lineData

        // X labels
        val xLabels = entries.indices.map { "Entry ${it + 1}" }
        binding.moodChart.xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)
        binding.moodChart.xAxis.labelCount = xLabels.size

        binding.moodChart.invalidate()
    }
    // SUPPORT LOGIC
    private fun showSupport(latest: String, avg: Double) {
        val isLow = latest == "mad" || avg < 2.0

        if (isLow) {
            binding.tvSupportMessage.text =
                "You're not feeling your best. Try the Self-Care Hub."
            binding.btnSelfCareHub.visibility = View.VISIBLE
        } else {
            binding.tvSupportMessage.text =
                "You're doing well. Keep tracking!"
            binding.btnSelfCareHub.visibility = View.GONE
        }
    }
    // HELPERS
    private fun moodToScore(m: String): Int = when (m.lowercase()) {
        "happy" -> 3
        "okay" -> 2
        "mad" -> 1
        else -> 0
    }

    private fun scoreToMood(score: Int): String = when (score) {
        3 -> "Happy"
        2 -> "Okay"
        1 -> "Mad"
        else -> "Unknown"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}