package com.example.myapplication

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * TaskRepository
 *
 * Manages:
 * - Task CRUD operations
 * - Daily log tracking (dailyLogs collection)
 * - Streak calculations (overall and per habit)
 * - Synchronization between task completion and daily logs
 */
class TaskRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    /**
     * Returns a reference to the current user's task collection.
     */
    private fun taskRef() =
        db.collection("users")
            .document(auth.currentUser!!.uid)
            .collection("tasks")

    /**
     * Returns today's date formatted as yyyy-MM-dd.
     * This format is used consistently across Firestore documents.
     */
    private fun getTodayString(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    /**
     * Adds a completed task to today's daily log.
     *
     * - Prevents duplicate task entries
     * - Updates completedCount based on unique tasks
     * - Uses a transaction to ensure data consistency
     */
    private fun saveDailyLog(task: Task) {
        val uid = auth.currentUser?.uid ?: return
        val today = getTodayString()

        val dailyLogRef = db.collection("users")
            .document(uid)
            .collection("dailyLogs")
            .document(today)

        db.runTransaction { transaction ->

            val snapshot = transaction.get(dailyLogRef)

            val existingTasks =
                snapshot.get("completedTasks") as? List<String> ?: emptyList()

            val updatedTasks =
                if (existingTasks.contains(task.title)) {
                    existingTasks
                } else {
                    existingTasks + task.title
                }

            val update = mapOf(
                "date" to today,
                "completedTasks" to updatedTasks,
                "completedCount" to updatedTasks.size,
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            transaction.set(dailyLogRef, update, SetOptions.merge())
        }
    }

    /**
     * Removes a task from today's daily log.
     *
     * - Ensures completedCount is updated correctly
     * - Uses a transaction for consistency
     */
    private fun removeFromDailyLog(task: Task) {
        val uid = auth.currentUser?.uid ?: return
        val today = getTodayString()

        val dailyLogRef = db.collection("users")
            .document(uid)
            .collection("dailyLogs")
            .document(today)

        db.runTransaction { transaction ->

            val snapshot = transaction.get(dailyLogRef)

            val existingTasks =
                snapshot.get("completedTasks") as? List<String> ?: emptyList()

            val updatedTasks =
                existingTasks.filter { it != task.title }

            val update = mapOf(
                "completedTasks" to updatedTasks,
                "completedCount" to updatedTasks.size,
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            transaction.set(dailyLogRef, update, SetOptions.merge())
        }
    }

    /**
     * Listens for real-time updates to the user's task list.
     * Used by the Home screen to display tasks.
     */
    fun listenToTasks(onResult: (List<Task>) -> Unit) {
        taskRef().addSnapshotListener { snapshot, _ ->
            val tasks = snapshot?.documents?.map {
                Task(
                    id = it.id,
                    title = it.getString("title") ?: "",
                    completed = it.getBoolean("completed") ?: false,
                    createdDate = it.getString("createdDate") ?: "",
                    completedDate = it.getString("completedDate")
                )
            } ?: emptyList()

            onResult(tasks)
        }
    }

    /**
     * Toggles a task's completion state.
     *
     * - Updates Firestore task document
     * - Syncs changes with dailyLogs collection
     */
    fun toggleTask(task: Task) {
        val newCompleted = !task.completed

        val updates = if (newCompleted) {
            saveDailyLog(task)

            mapOf(
                "completed" to true,
                "completedDate" to getTodayString()
            )
        } else {
            removeFromDailyLog(task)

            mapOf(
                "completed" to false,
                "completedDate" to null
            )
        }

        taskRef()
            .document(task.id)
            .update(updates)
    }

    /**
     * Creates a new task.
     */
    fun addTask(title: String) {
        taskRef().add(
            mapOf(
                "title" to title,
                "completed" to false,
                "createdDate" to getTodayString(),
                "completedDate" to null
            )
        )
    }

    // NEW (3.1) – Count completed tasks
    fun listenToCompletedTaskCount(onResult: (Int) -> Unit) {
        taskRef().addSnapshotListener { snapshot, _ ->

            val completedCount = snapshot?.documents?.count { document ->
                document.getBoolean("completed") == true
            } ?: 0

            onResult(completedCount)
        }
    }

    /**
     * Deletes a task permanently.
     */
    fun deleteTask(task: Task) {
        taskRef().document(task.id).delete()
    }

    /**
     * Listens for changes in dailyLogs and calculates
     * the current overall streak.
     *
     * A day counts toward the streak only if completedCount > 0.
     */
    fun listenToCurrentStreak(onResult: (Int) -> Unit) {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(uid)
            .collection("dailyLogs")
            .addSnapshotListener { snapshot, _ ->

                val completedDates = snapshot?.documents
                    ?.mapNotNull { doc ->
                        val count = doc.getLong("completedCount") ?: 0
                        val date = doc.getString("date") ?: doc.id

                        if (count > 0 && date.isNotBlank()) {
                            date
                        } else {
                            null
                        }
                    }
                    ?.toSet()
                    ?: emptySet()

                onResult(calculateCurrentStreak(completedDates))
            }
    }

    /**
     * Calculates streaks for each individual habit (task title).
     *
     * Returns a map of:
     * task name → current streak length
     */
    fun listenToHabitStreaks(onResult: (Map<String, Int>) -> Unit) {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(uid)
            .collection("dailyLogs")
            .addSnapshotListener { snapshot, _ ->

                val habitMap = mutableMapOf<String, MutableSet<String>>()

                snapshot?.documents?.forEach { doc ->
                    val date = doc.getString("date") ?: doc.id

                    val tasks =
                        doc.get("completedTasks") as? List<String> ?: emptyList()

                    for (task in tasks) {
                        val key = task.lowercase()

                        habitMap.getOrPut(key) { mutableSetOf() }
                            .add(date)
                    }
                }

                val result = mutableMapOf<String, Int>()

                for ((task, dates) in habitMap) {
                    val streak = calculateCurrentStreak(dates)

                    if (streak > 0) {
                        result[task] = streak
                    }
                }

                onResult(
                    result.toList()
                        .sortedByDescending { it.second }
                        .toMap()
                )
            }
    }

    /**
     * Core streak calculation logic.
     *
     * Starts from today and counts backward until a missing day is found.
     */
    private fun calculateCurrentStreak(dates: Set<String>): Int {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()

        var streak = 0

        while (true) {
            val currentDate = sdf.format(calendar.time)

            if (dates.contains(currentDate)) {
                streak++
                calendar.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }

        return streak
    }
}