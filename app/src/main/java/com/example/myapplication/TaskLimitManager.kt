package com.example.myapplication

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskLimitManager {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        const val MAX_REGENS = 1
        const val MAX_SWAPS = 2
        const val MAX_QUICK_TASKS = 3
    }

    private val today get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    private fun userDoc() = db.collection("users").document(auth.currentUser!!.uid)

    fun getLimits(onResult: (regenLeft: Int, swapLeft: Int, quickLeft: Int) -> Unit) {
        userDoc().get().addOnSuccessListener { doc ->
            val regenLeft = MAX_REGENS - if (doc.getString("regenDate") == today)
                doc.getLong("regenCount")?.toInt() ?: 0 else 0

            val swapLeft = MAX_SWAPS - if (doc.getString("swapDate") == today)
                doc.getLong("swapCount")?.toInt() ?: 0 else 0

            val quickLeft = MAX_QUICK_TASKS - if (doc.getString("quickTaskDate") == today)
                doc.getLong("quickTaskCount")?.toInt() ?: 0 else 0

            onResult(
                regenLeft.coerceAtLeast(0),
                swapLeft.coerceAtLeast(0),
                quickLeft.coerceAtLeast(0)
            )
        }.addOnFailureListener {
            // Default to full limits if fetch fails
            onResult(MAX_REGENS, MAX_SWAPS, MAX_QUICK_TASKS)
        }
    }

    fun useRegen(onSuccess: () -> Unit, onLimitReached: () -> Unit) {
        checkAndUse("regenDate", "regenCount", MAX_REGENS, onSuccess, onLimitReached)
    }

    fun useSwap(onSuccess: () -> Unit, onLimitReached: () -> Unit) {
        checkAndUse("swapDate", "swapCount", MAX_SWAPS, onSuccess, onLimitReached)
    }

    fun useQuickTask(onSuccess: () -> Unit, onLimitReached: () -> Unit) {
        checkAndUse("quickTaskDate", "quickTaskCount", MAX_QUICK_TASKS, onSuccess, onLimitReached)
    }

    private fun checkAndUse(
        dateField: String,
        countField: String,
        max: Int,
        onSuccess: () -> Unit,
        onLimitReached: () -> Unit
    ) {
        userDoc().get().addOnSuccessListener { doc ->
            val currentCount = if (doc.getString(dateField) == today)
                doc.getLong(countField)?.toInt() ?: 0 else 0

            if (currentCount >= max) {
                onLimitReached()
                return@addOnSuccessListener
            }

            // Use set with merge so it works even if the document
            // or fields don't exist yet
            userDoc().set(
                mapOf(dateField to today, countField to currentCount + 1),
                SetOptions.merge()
            ).addOnSuccessListener {
                onSuccess()
            }.addOnFailureListener {
                // Log so you can see if writes are failing
                android.util.Log.e("TaskLimitManager", "Failed to write: ${it.message}")
            }

        }.addOnFailureListener {
            android.util.Log.e("TaskLimitManager", "Failed to read: ${it.message}")
        }
    }
}