package com.example.myapplication

import android.os.Bundle
import android.content.Intent
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.databinding.ActivityRegistrationBinding
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.example.myapplication.util.Collection.USER_COLLECTION
import com.google.firebase.firestore.FirebaseFirestore


class Registration : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrationBinding
    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistrationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.textView.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }

        binding.button.setOnClickListener {

            val email = binding.email.text.toString().trim()
            val pass = binding.password.text.toString().trim()

            val first = binding.first.text.toString().trim()
            val last = binding.last.text.toString().trim()
            val dob = binding.DOB.text.toString().trim()
            val country = binding.Country.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty() || first.isEmpty() || last.isEmpty() || dob.isEmpty() || country.isEmpty()) {
                Toast.makeText(this, "Empty Fields not allowed", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            firebaseAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener { task ->

                    if (!task.isSuccessful) {
                        Toast.makeText(this, task.exception?.message ?: "Sign up failed", Toast.LENGTH_SHORT).show()
                        return@addOnCompleteListener
                    }

                    val uid = firebaseAuth.currentUser?.uid
                    if (uid == null) {
                        Toast.makeText(this, "Error: UID not found", Toast.LENGTH_SHORT).show()
                        return@addOnCompleteListener
                    }

                    val userData = hashMapOf(
                        "firstName" to first,
                        "lastName" to last,
                        "dateOfBirth" to dob,
                        "country" to country,
                        "avatar" to "dog"
                    )

                    db.collection(USER_COLLECTION).document(uid)
                        .set(userData)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, Login::class.java)
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Firestore save failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
        }

        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}