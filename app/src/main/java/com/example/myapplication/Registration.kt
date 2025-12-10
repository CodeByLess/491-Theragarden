package com.example.myapplication

import android.os.Bundle
import android.content.Intent
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.databinding.ActivityRegistrationBinding
import com.example.myapplication.util.Collection.USER_COLLECTION
import com.example.myapplication.User
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class Registration : AppCompatActivity() {

    private lateinit var binding: ActivityRegistrationBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db : FirebaseFirestore

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

        binding.button.setOnClickListener{
            val email = binding.email.text.toString()
            val pass = binding.password.text.toString()
            val firstName = binding.first.text.toString()
            val lastName = binding.last.text.toString()
            val dateOfBirth = binding.DOB.text.toString()
            val country = binding.Country.text.toString()
            val profileImageUrl = "dog.png"

            if (email.isNotEmpty() && pass.isNotEmpty()){
                firebaseAuth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener{ task ->
                    if (task.isSuccessful){
                        val uid = task.result?.user?.uid ?: return@addOnCompleteListener
                        val user = User(firstName, lastName, dateOfBirth, country, profileImageUrl)
                        saveUserInfo(uid, user)
                        val intent = Intent(this, Login::class.java)
                        startActivity(intent)
                    }else{
                        Toast.makeText(this, task.exception.toString(), Toast.LENGTH_SHORT).show()
                    }
                }
                }
            else{
                Toast.makeText(this, "Empty Fields not allowed", Toast.LENGTH_SHORT).show()
            }
        }
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun saveUserInfo(userUid: String, user: User) {
        db.collection(USER_COLLECTION)
            .document(userUid)
            .set(user)
            .addOnSuccessListener {
                Toast.makeText(this, "User info saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error saving info", Toast.LENGTH_SHORT).show()
            }
    }
}

