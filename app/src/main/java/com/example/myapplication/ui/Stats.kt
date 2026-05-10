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

// Added by Lesley Del Cid:
// Represents one saved mood entry from Firestore.
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

        // Added by Lesley Del Cid:
        // Start the Stats page by selecting today's date.
        selectedDateString = getTodayString()

        setupCalendarRecyclerView()

        // Added by Lesley Del Cid:
        // Moves the calendar to the previous month.
        binding.btnPreviousMonth.setOnClickListener {
            visibleMonthCalendar.add(Calendar.MONTH, -1)
            refreshCalendarGrid()
        }

        // Added by Lesley Del Cid:
        // Moves the calendar to the next month.
        binding.btnNextMonth.setOnClickListener {
            visibleMonthCalendar.add(Calendar.MONTH, 1)
            refreshCalendarGrid()
        }

        // Added by Lesley Del Cid:
        // Loads all dates that need task/plant dots on the calendar.
        loadCalendarMarkers()

        // Added by Lesley Del Cid:
        // Displays the user's overall day streak.
        repository.listenToCurrentStreak { streak ->
            if (_binding != null) {
                binding.txtDayStreak.text = "Day streak: $streak"
            }
        }

        // Added by Lesley Del Cid:
        // Displays each individual habit streak.
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

        // Added by Lesley Del Cid:
        // Loads today's selected-day details and summaries when the page opens.
        loadSelectedDate(selectedDateString)
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

    // Added by Lesley Del Cid:
    // Sets up the custom calendar RecyclerView using 7 columns for the week.
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

    // Added by Lesley Del Cid:
    // Reads Firestore to find which dates should show task dots and plant dots.
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

    // Added by Lesley Del Cid:
    // Builds the calendar grid for the visible month.
    // Blank cells are added before day 1 so dates line up under the correct weekday.
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

        // Added by Lesley Del Cid:
        // Add blank cells before the first day of the month.
        for (i in 1 until firstDayOfWeek) {
            calendarDays.add(
                CalendarDay(
                    dayNumber = 0,
                    dateString = "",
                    isBlank = true
                )
            )
        }

        // Added by Lesley Del Cid:
        // Add the real calendar days with selected/today states and activity dots.
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

    // Added by Lesley Del Cid:
    // Returns today's date in yyyy-MM-dd format.
    private fun getTodayString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Calendar.getInstance().time)
    }

    // Added by Lesley Del Cid:
    // Converts a Firebase Timestamp into yyyy-MM-dd format.
    private fun timestampToDateString(timestamp: Timestamp): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(timestamp.toDate())
    }

    // Added by Lesley Del Cid:
    // Converts a yyyy-MM-dd string into a Calendar object for week/month checks.
    private fun dateStringToCalendar(date: String): Calendar {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.time = sdf.parse(date)!!
        return calendar
    }

    // Added by Lesley Del Cid:
    // Checks if a date belongs to the same week as the selected date.
    private fun isSameWeek(date: String, selectedDate: String): Boolean {
        val dateCal = dateStringToCalendar(date)
        val selectedCal = dateStringToCalendar(selectedDate)

        return dateCal.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) &&
                dateCal.get(Calendar.WEEK_OF_YEAR) == selectedCal.get(Calendar.WEEK_OF_YEAR)
    }

    // Added by Lesley Del Cid:
    // Checks if a date belongs to the same month as the selected date.
    private fun isSameMonth(date: String, selectedDate: String): Boolean {
        val dateCal = dateStringToCalendar(date)
        val selectedCal = dateStringToCalendar(selectedDate)

        return dateCal.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR) &&
                dateCal.get(Calendar.MONTH) == selectedCal.get(Calendar.MONTH)
    }

    // Added by Lesley Del Cid:
    // Loads selected-day details and summary information.
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

    // Added by Lesley Del Cid:
    // Loads completed tasks and plants grown on the selected date.
    // Combines data from dailyLogs and garden Firestore collections.
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

    // Added by Lesley Del Cid:
    // Displays plant images in rows of 3.
    // This prevents overflow when multiple plants were grown on the same day.
    private fun showPlantImages(plants: List<String>) {
        if (plants.isEmpty()) {
            binding.plantPreviewContainer.visibility = View.GONE
            return
        }

        binding.plantPreviewContainer.visibility = View.VISIBLE

        var currentRow: LinearLayout? = null

        plants.forEachIndexed { index, seedName ->

            // Added by Lesley Del Cid:
            // Create a new horizontal row after every 3 plant images.
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

    // Added by Lesley Del Cid:
    // Matches a saved seed name from Firestore to its drawable image.
    private fun getPlantImageResource(seedName: String): Int? {
        return when (seedName) {
            "Sunflower Seed" -> R.drawable.sunflower
            "Strawberry Seed" -> R.drawable.strawberry
            "Lavender Seed" -> R.drawable.lavender
            "Tulip Seed" -> R.drawable.tulip
            "Cactus Seed" -> R.drawable.cactus
            "Monstera Seed" -> R.drawable.monstera
            else -> null
        }
    }

    // Added by Lesley Del Cid:
    // Calculates weekly and monthly totals for completed tasks and grown plants.
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

