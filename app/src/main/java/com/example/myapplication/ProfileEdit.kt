package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.util.Collection.USER_COLLECTION
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileEdit : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile_edit)

        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // 1. Find the views
        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)

        // 2. Pre-fill text boxes from intent extras (sent from Profile screen)
        etFirstName.setText(intent.getStringExtra("firstName") ?: "")
        etLastName.setText(intent.getStringExtra("lastName") ?: "")

        // 3. Save button
        btnSave.setOnClickListener {
            val newFirst = etFirstName.text.toString().trim()
            val newLast = etLastName.text.toString().trim()

            if (newFirst.isEmpty() || newLast.isEmpty()) {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show()
            } else {
                updateProfile(newFirst, newLast)
            }
        }

        // 4. Cancel button
        btnCancel.setOnClickListener {
            finish()
        }

        // Optional: if your root view has id "main"
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun updateProfile(firstName: String, lastName: String) {
        val uid = firebaseAuth.currentUser?.uid ?: return

        val updates = mapOf(
            "firstName" to firstName,
            "lastName" to lastName
        )

        db.collection(USER_COLLECTION).document(uid)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile Updated", Toast.LENGTH_SHORT).show()

                // Go back to Profile screen
                val intent = Intent(this, Profile::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Update Failed", Toast.LENGTH_SHORT).show()
            }
    }
}
