package com.example.myapplication

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class TaskRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun taskRef() =
        db.collection("users")
            .document(auth.currentUser!!.uid)
            .collection("tasks")

    // Listen to all tasks --Paula
    fun listenToTasks(onResult: (List<Task>) -> Unit) {
        taskRef().addSnapshotListener { snapshot, _ ->
            val tasks = snapshot?.documents?.map {
                Task(
                    id = it.id,
                    title = it.getString("title") ?: "",
                    completed = it.getBoolean("completed") ?: false
                )
            } ?: emptyList()

            onResult(tasks)
        }
    }

    // Toggle task completion
    fun toggleTask(task: Task) {
        taskRef()
            .document(task.id)
            .update("completed", !task.completed)
    }

    // Add new task
    fun addTask(title: String) {
        taskRef().add(
            mapOf(
                "title" to title,
                "completed" to false
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
}