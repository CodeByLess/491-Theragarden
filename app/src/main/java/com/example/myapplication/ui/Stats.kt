package com.example.myapplication.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.myapplication.ui.CalendarAdapter
import com.example.myapplication.ui.CalendarDay
import com.example.myapplication.R
import com.example.myapplication.TaskRepository
import com.example.myapplication.databinding.FragmentStatsBinding
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Added by merge fix:
// MoodEntry data class used for mood records from Firestore.
data class MoodEntry(
    val id: String = "",
    val mood: String = "",
    val createdAt: Long = 0L
)

// Added by Lesley Del Cid:
// Stats Fragment handles the interactive statistics screen.
// It displays a custom calendar, daily streaks, habit streaks,
// selected-day task/plant details, weekly/monthly summaries,
// and plant images for plants grown on a selected day.
class Stats : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    // Added by Lesley Del Cid:
    // Stores the currently selected date in yyyy-MM-dd format.
    private var selectedDateString: String = ""

    // Added by Lesley Del Cid:
    // Stores dates that have at least one completed task.
    // These dates show the task dot on the calendar.
    private val taskDates = mutableSetOf<String>()

    // Added by Lesley Del Cid:
    // Stores dates that have at least one plant grown.
    // These dates show the plant dot on the calendar.
    private val plantDates = mutableSetOf<String>()

    // Added by Lesley Del Cid:
    // Adapter used to display the custom calendar days.
    private lateinit var calendarAdapter: CalendarAdapter

    // Added by Lesley Del Cid:
    // Tracks the month currently displayed on the calendar.
    private val visibleMonthCalendar: Calendar = Calendar.getInstance()

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repository = TaskRepository()

        selectedDateString = getTodayString()

        setupCalendarRecyclerView()

        binding.btnPreviousMonth.setOnClickListener {
            visibleMonthCalendar.add(Calendar.MONTH, -1)
            refreshCalendarGrid()
        }

        binding.btnNextMonth.setOnClickListener {
            visibleMonthCalendar.add(Calendar.MONTH, 1)
            refreshCalendarGrid()
        }

        loadCalendarMarkers()

        repository.listenToCurrentStreak { streak ->
            if (_binding != null) {
                binding.txtDayStreak.text = "Day streak: $streak"
            }
        }

        repository.listenToHabitStreaks { streaks ->
            if (_binding != null) {
                binding.txtHabitStreaks.text =
                    if (streaks.isEmpty()) {
                        "No habit streaks yet."
                    } else {
                        streaks.entries.joinToString("\n") {
                            "• ${it.key}: ${it.value} day streak"
                        }
                    }
            }
        }

        loadSelectedDate(selectedDateString)
    }

    override fun onResume() {
        super.onResume()
        loadMoodStats()
    }

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

        chart.xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            textColor = ContextCompat.getColor(requireContext(), R.color.white)
            setDrawGridLines(false)
            granularity = 1f
        }
    }

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

                binding.tvLatestMood.text =
                    "Latest: ${latestMood.replaceFirstChar { it.uppercase() }}"

                binding.tvAverageMood.text =
                    "Average: ${"%.2f".format(avg)} / 3"

                binding.tvHighestMood.text =
                    "Highest: ${scoreToMood(highest)}"

                binding.tvLowestMood.text =
                    "Lowest: ${scoreToMood(lowest)}"

                binding.tvTotalEntries.text =
                    "Entries: ${entries.size}"

                binding.tvStatus.text = ""

                renderChart(entries)
                showSupport(latestMood, avg)
            }
            .addOnFailureListener { e ->
                binding.tvStatus.text = "Failed to load mood statistics: ${e.message}"
            }
    }

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

        val xLabels = entries.indices.map { "Entry ${it + 1}" }
        binding.moodChart.xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)
        binding.moodChart.xAxis.labelCount = xLabels.size

        binding.moodChart.invalidate()
    }

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

    private fun setupCalendarRecyclerView() {
        calendarAdapter = CalendarAdapter(emptyList()) { selectedDay ->
            selectedDateString = selectedDay.dateString

            refreshCalendarGrid()
            loadSelectedDate(selectedDateString)
        }

        binding.calendarRecyclerView.layoutManager =
            GridLayoutManager(requireContext(), 7)

        binding.calendarRecyclerView.adapter = calendarAdapter

        refreshCalendarGrid()
    }

    private fun loadCalendarMarkers() {
        val uid = auth.currentUser?.uid ?: return

        taskDates.clear()
        plantDates.clear()

        db.collection("users")
            .document(uid)
            .collection("dailyLogs")
            .get()
            .addOnSuccessListener { logs ->
                if (_binding == null) return@addOnSuccessListener

                for (doc in logs) {
                    val date = doc.getString("date") ?: doc.id
                    val count = doc.getLong("completedCount") ?: 0L

                    if (count > 0 && date.isNotBlank()) {
                        taskDates.add(date)
                    }
                }

                db.collection("users")
                    .document(uid)
                    .collection("garden")
                    .get()
                    .addOnSuccessListener { plants ->
                        if (_binding == null) return@addOnSuccessListener

                        for (doc in plants) {
                            val completedAt = doc.getTimestamp("completedAt")

                            if (completedAt != null) {
                                plantDates.add(timestampToDateString(completedAt))
                            }
                        }

                        refreshCalendarGrid()
                    }
            }
    }

    private fun refreshCalendarGrid() {
        val formatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        binding.txtMonthTitle.text = formatter.format(visibleMonthCalendar.time)

        val year = visibleMonthCalendar.get(Calendar.YEAR)
        val month = visibleMonthCalendar.get(Calendar.MONTH)

        val tempCalendar = Calendar.getInstance()
        tempCalendar.set(year, month, 1)

        val daysInMonth = tempCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK)

        val calendarDays = mutableListOf<CalendarDay>()
        val todayString = getTodayString()

        for (i in 1 until firstDayOfWeek) {
            calendarDays.add(
                CalendarDay(
                    dayNumber = 0,
                    dateString = "",
                    isBlank = true
                )
            )
        }

        for (day in 1..daysInMonth) {
            val dateString = String.format(
                Locale.getDefault(),
                "%04d-%02d-%02d",
                year,
                month + 1,
                day
            )

            calendarDays.add(
                CalendarDay(
                    dayNumber = day,
                    dateString = dateString,
                    isSelected = dateString == selectedDateString,
                    isToday = dateString == todayString,
                    hasTask = taskDates.contains(dateString),
                    hasPlant = plantDates.contains(dateString)
                )
            )
        }

        calendarAdapter.updateDays(calendarDays)
    }

    private fun getTodayString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Calendar.getInstance().time)
    }

    private fun timestampToDateString(timestamp: Timestamp): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(timestamp.toDate())
    }

    private fun dateStringToCalendar(date: String): Calendar {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.time = sdf.parse(date)!!
        return calendar
    }

    private fun isSameWeek(date: String, selectedDate: String): Boolean {
        val dateCal = dateStringToCalendar(date)
        val selectedCal = dateStringToCalendar(selectedDate)

        return dateCal.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) &&
                dateCal.get(Calendar.WEEK_OF_YEAR) == selectedCal.get(Calendar.WEEK_OF_YEAR)
    }

    private fun isSameMonth(date: String, selectedDate: String): Boolean {
        val dateCal = dateStringToCalendar(date)
        val selectedCal = dateStringToCalendar(selectedDate)

        return dateCal.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) &&
                dateCal.get(Calendar.MONTH) == selectedCal.get(Calendar.MONTH)
    }

    private fun loadSelectedDate(date: String) {
        val uid = auth.currentUser?.uid ?: return

        binding.txtSelectedDateTitle.text = "Selected Day: $date"
        binding.txtSelectedDayDetails.text = "Loading..."
        binding.txtWeeklySummary.text = "Weekly Summary\nLoading..."
        binding.txtMonthlySummary.text = "Monthly Summary\nLoading..."

        binding.plantPreviewContainer.removeAllViews()
        binding.plantPreviewContainer.visibility = View.GONE

        loadSelectedDayDetails(uid, date)
        loadSummaryTotals(uid, date)
    }

    private fun loadSelectedDayDetails(uid: String, date: String) {
        db.collection("users")
            .document(uid)
            .collection("dailyLogs")
            .document(date)
            .get()
            .addOnSuccessListener { doc ->
                if (_binding == null) return@addOnSuccessListener

                val tasks = doc.get("completedTasks") as? List<String> ?: emptyList()
                val count = doc.getLong("completedCount") ?: tasks.size.toLong()

                db.collection("users")
                    .document(uid)
                    .collection("garden")
                    .get()
                    .addOnSuccessListener { plantsDocs ->
                        if (_binding == null) return@addOnSuccessListener

                        val plants = mutableListOf<String>()

                        for (p in plantsDocs) {
                            val completedAt = p.getTimestamp("completedAt")
                            val seedName = p.getString("seedName") ?: "Unknown"

                            if (completedAt != null && timestampToDateString(completedAt) == date) {
                                plants.add(seedName)
                            }
                        }

                        val taskText =
                            if (tasks.isEmpty()) {
                                "Completed tasks: 0\nNo tasks completed."
                            } else {
                                "Completed tasks: $count\n${tasks.joinToString("\n") { "• $it" }}"
                            }

                        val plantText =
                            if (plants.isEmpty()) {
                                "Plants grown: 0\nNo plants grown."
                            } else {
                                "Plants grown: ${plants.size}\n${plants.joinToString("\n") { "• $it" }}"
                            }

                        binding.txtSelectedDayDetails.text = "$taskText\n\n$plantText"

                        showPlantImages(plants)
                    }
            }
    }

    private fun showPlantImages(plants: List<String>) {
        if (plants.isEmpty()) {
            binding.plantPreviewContainer.visibility = View.GONE
            return
        }

        binding.plantPreviewContainer.visibility = View.VISIBLE

        var currentRow: LinearLayout? = null

        plants.forEachIndexed { index, seedName ->

            if (index % 3 == 0) {
                currentRow = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                binding.plantPreviewContainer.addView(currentRow)
            }

            val imageRes = getPlantImageResource(seedName)

            if (imageRes != null) {
                val img = ImageView(requireContext())

                img.setImageResource(imageRes)
                img.scaleType = ImageView.ScaleType.FIT_CENTER
                img.layoutParams = ViewGroup.MarginLayoutParams(150, 150).apply {
                    setMargins(8, 8, 8, 8)
                }

                currentRow?.addView(img)
            }
        }
    }

    // Added by Lesley:
    // Returns the correct plant image for starter seeds,
    // shop seeds, and spin wheel exclusive plants
    // inside the Stats calendar preview.
    private fun getPlantImageResource(seedName: String): Int? {
        return when (seedName) {

            // Starter seeds
            "Sunflower Seed" -> R.drawable.sunflower
            "Strawberry Seed" -> R.drawable.strawberry
            "Lavender Seed" -> R.drawable.lavender
            "Tulip Seed" -> R.drawable.tulip
            "Cactus Seed" -> R.drawable.cactus
            "Monstera Seed" -> R.drawable.monstera

            // Shop seeds
            "Bonsai Tree" -> R.drawable.bonsai
            "Cherry Blossom" -> R.drawable.cherryblossoms
            "Palm Tree" -> R.drawable.palmtree
            "Venus Flytrap" -> R.drawable.venusflytrap

            // Spin wheel plants
            "Trumpet Flower" -> R.drawable.trumpetflower
            "Blue Rose" -> R.drawable.bluerose
            "Crystal Lotus" -> R.drawable.crystallotus
            "Rainbow Tulip" -> R.drawable.rainbowtulip

            else -> null
        }
    }

    private fun loadSummaryTotals(uid: String, selectedDate: String) {
        var weeklyTasks = 0
        var monthlyTasks = 0
        var weeklyPlants = 0
        var monthlyPlants = 0

        db.collection("users")
            .document(uid)
            .collection("dailyLogs")
            .get()
            .addOnSuccessListener { logs ->
                if (_binding == null) return@addOnSuccessListener

                for (doc in logs) {
                    val date = doc.getString("date") ?: doc.id
                    val count = doc.getLong("completedCount")?.toInt() ?: 0

                    if (isSameWeek(date, selectedDate)) {
                        weeklyTasks += count
                    }

                    if (isSameMonth(date, selectedDate)) {
                        monthlyTasks += count
                    }
                }

                db.collection("users")
                    .document(uid)
                    .collection("garden")
                    .get()
                    .addOnSuccessListener { plants ->
                        if (_binding == null) return@addOnSuccessListener

                        for (doc in plants) {
                            val completedAt = doc.getTimestamp("completedAt")

                            if (completedAt != null) {
                                val plantDate = timestampToDateString(completedAt)

                                if (isSameWeek(plantDate, selectedDate)) {
                                    weeklyPlants++
                                }

                                if (isSameMonth(plantDate, selectedDate)) {
                                    monthlyPlants++
                                }
                            }
                        }

                        binding.txtWeeklySummary.text =
                            "Weekly Summary\n• Tasks completed: $weeklyTasks\n• Plants grown: $weeklyPlants"

                        binding.txtMonthlySummary.text =
                            "Monthly Summary\n• Tasks completed: $monthlyTasks\n• Plants grown: $monthlyPlants"
                    }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}