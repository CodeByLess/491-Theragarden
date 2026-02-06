package com.example.myapplication

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions


class GoalsRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    fun incrementGoals(onDone: (Boolean) -> Unit) {
        val uid = auth.currentUser?.uid ?: return
        val docRef = db.collection("users").document(uid)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val current = snapshot.getLong("completedGoals") ?: 0
            transaction.set(
                docRef,
                Goals(current.toInt() + 1),
                SetOptions.merge()
            )
        }.addOnSuccessListener {
            onDone(true)
        }.addOnFailureListener {
            onDone(false)
        }
    }
}
