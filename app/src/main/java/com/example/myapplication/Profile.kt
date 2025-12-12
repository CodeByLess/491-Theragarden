package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.util.Collection.USER_COLLECTION // This must match your Registration import
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Profile : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tvFullName: TextView
    private lateinit var tvLastName : TextView
    private lateinit var tvDob: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvCountry: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // --- 1. SETUP BUTTONS ---
        // These R.id names MUST match your activity_profile.xml IDs
        val btnBack = findViewById<Button>(R.id.btnBack)
        val btnEdit = findViewById<Button>(R.id.edit)
        val btnDelete = findViewById<Button>(R.id.delete)

        // --- 2. SETUP TEXT FIELDS ---
        tvFullName = findViewById(R.id.first)
        tvLastName = findViewById(R.id.last)
        tvDob = findViewById(R.id.DOB)
        tvEmail = findViewById(R.id.email)
        tvCountry = findViewById(R.id.country)

        // Load data on start
        loadUserInfo()

        // --- 3. BUTTON ACTIONS ---

        btnBack.setOnClickListener { finish() }

        btnEdit.setOnClickListener {
            val intent = Intent(this, ProfileEditPage::class.java)
            startActivity(intent)
        }


        btnDelete.setOnClickListener {
            // DEBUG MSG: If you don't see this pop up, your button ID is wrong!
            Toast.makeText(this, "Attempting to delete...", Toast.LENGTH_SHORT).show()

            deleteAccount()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // --- 4. LOAD DATA FUNCTION ---
    private fun loadUserInfo() {
        val user = firebaseAuth.currentUser
        val uid = user?.uid // We get the UID from the logged-in user

        if (uid != null) {
            // We use that UID to find the document you created in Registration
            db.collection(USER_COLLECTION).document(uid).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val first = document.getString("firstName")
                        val last = document.getString("lastName")
                        val dob = document.getString("dateOfBirth")
                        val country = document.getString("country")

                        tvFullName.text = first
                        tvLastName.text = last
                        tvDob.text = dob
                        tvEmail.text = user.email
                        tvCountry.text = country
                    }
                }
        }
    }

    // --- 5. DELETE FUNCTION (Matches your Registration logic) ---
    private fun deleteAccount() {
        val user = firebaseAuth.currentUser
        val uid = user?.uid

        if (uid != null) {
            // Step 1: Delete the database entry (created in Registration)
            db.collection(USER_COLLECTION).document(uid)
                .delete()
                .addOnSuccessListener {
                    // Step 2: Delete the Login Account
                    user.delete().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Account Deleted Successfully", Toast.LENGTH_LONG).show()

                            // Step 3: Send them back to Registration/Login
                            val intent = Intent(this, Registration::class.java)
                            // Clear the back stack so they can't go back to Profile
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                        } else {
                            Toast.makeText(this, "Error deleting login: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error deleting data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "No user found to delete", Toast.LENGTH_SHORT).show()
        }
    }
}