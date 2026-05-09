package com.example.myapplication

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

class QuizManager (private val activity: AppCompatActivity) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun checkAndShowQuiz() {
        val userId = auth.currentUser?.uid ?: return
        val userDoc = db.collection("users").document(userId)

        userDoc.get().addOnSuccessListener { doc ->
            val lastDate = doc.getString("lastQuizDate") ?: ""
            val count = doc.getLong("quizTakenCount")?.toInt() ?: 0
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            Log.d("QuizDebug", "Last quiz date: $lastDate Today: $today Count: $count")

            when {
                lastDate != today -> {
                    userDoc.update(
                        mapOf(
                            "lastQuizDate" to today,
                            "quizTakenCount" to 1
                        )
                    )
                    showQuiz()
                }
                count < 2 -> {
                    userDoc.update("quizTakenCount", FieldValue.increment(1))
                    showQuiz()
                }
                else -> {
                    Log.d("QuizDebug", "Quiz limit reached for today")
                }
            }
        }.addOnFailureListener {
            Log.e("QuizDebug", "Failed to check quiz status: ${it.message}")
        }
    }

    private fun showQuiz() {
        QuizBottomSheet.newInstance()
            .show(activity.supportFragmentManager, "QuizBottomSheet")
    }
}