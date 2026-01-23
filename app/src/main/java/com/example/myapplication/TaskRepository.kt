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

    fun toggleTask(task: Task) {
        taskRef()
            .document(task.id)
            .update("completed", !task.completed)
    }

    fun addTask(title: String) {
        taskRef().add(
            mapOf(
                "title" to title,
                "completed" to false
            )
        )
    }
}
