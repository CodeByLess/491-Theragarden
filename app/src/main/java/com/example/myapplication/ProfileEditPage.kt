package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.util.Collection.USER_COLLECTION
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileEditPage : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var btnBack: Button
    private lateinit var btnChangeName: Button
    private lateinit var btnChangePassword: Button
    private lateinit var btnSave: Button
    private lateinit var btnDelete: Button
    private lateinit var btnEditAvatar: Button

    private var newFirstName: String? = null
    private var newLastName: String? = null
    private var newPassword: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_edit_page)

        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        btnBack = findViewById(R.id.backProfile)
        btnChangeName = findViewById(R.id.Change_name)
        btnChangePassword = findViewById(R.id.ChangePassword)
        btnSave = findViewById(R.id.SaveProfile)
        btnDelete = findViewById(R.id.DeleteAccount)
        btnEditAvatar = findViewById(R.id.EditAvatar)


        btnBack.setOnClickListener { finish() }

        btnChangeName.setOnClickListener { showChangeNameDialog() }

        btnChangePassword.setOnClickListener { showChangePasswordDialog() }

        btnSave.setOnClickListener { saveChanges() }

        btnDelete.setOnClickListener { deleteAccount() }

        btnEditAvatar.setOnClickListener { showAvatarDialog() }
    }


    private fun showChangeNameDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val etFirst = EditText(this).apply {
            hint = "First name"
            inputType = InputType.TYPE_CLASS_TEXT
        }

        val etLast = EditText(this).apply {
            hint = "Last name"
            inputType = InputType.TYPE_CLASS_TEXT
        }

        layout.addView(etFirst)
        layout.addView(etLast)

        AlertDialog.Builder(this)
            .setTitle("Change Name")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val first = etFirst.text.toString().trim()
                val last = etLast.text.toString().trim()

                if (first.isEmpty() || last.isEmpty()) {
                    Toast.makeText(this, "Both fields are required", Toast.LENGTH_SHORT).show()
                } else {
                    newFirstName = first
                    newLastName = last
                    Toast.makeText(this, "Name stored. Press SAVE to apply.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showChangePasswordDialog() {
        val input = EditText(this).apply {
            hint = "New password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val value = input.text.toString().trim()
                if (value.length < 6) {
                    Toast.makeText(this, "Password must be 6+ characters", Toast.LENGTH_SHORT).show()
                } else {
                    newPassword = value
                    Toast.makeText(this, "Password stored. Press SAVE to apply.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun saveChanges() {
        val user = firebaseAuth.currentUser ?: return
        val uid = user.uid


        val updates = hashMapOf<String, Any>()

        if (!newFirstName.isNullOrEmpty()) {
            updates["firstName"] = newFirstName!!
        }
        if (!newLastName.isNullOrEmpty()) {
            updates["lastName"] = newLastName!!
        }

        if (updates.isNotEmpty()) {
            db.collection(USER_COLLECTION).document(uid)
                .update(updates as Map<String, Any>)
                .addOnSuccessListener {
                    Toast.makeText(this, "Name updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error updating name: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }


        if (!newPassword.isNullOrEmpty()) {
            user.updatePassword(newPassword!!)
                .addOnSuccessListener {
                    Toast.makeText(this, "Password updated", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error updating password: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

        Toast.makeText(this, "Changes saved", Toast.LENGTH_SHORT).show()
    }
    private fun deleteAccount() {
        val user = firebaseAuth.currentUser
        val uid = user?.uid ?: return

        db.collection(USER_COLLECTION).document(uid)
            .delete()
            .addOnSuccessListener {
                user.delete().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Account deleted", Toast.LENGTH_LONG).show()
                        val intent = Intent(this, Registration::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    } else {
                        Toast.makeText(
                            this,
                            "Error deleting auth: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error deleting Firestore: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun showAvatarDialog() {
        // The labels the user sees
        val avatars = arrayOf("Dog", "Rabbit", "Cat", "Butterfly")

        AlertDialog.Builder(this)
            .setTitle("Choose Your Avatar")
            .setItems(avatars) { _, which ->
                val selectedKey = when (which) {
                    0 -> "dog"
                    1 -> "rabbit"
                    2 -> "cat"
                    else -> "butterfly"
                }
                saveAvatarSelection(selectedKey)
            }
            .show()
    }
    private fun saveAvatarSelection(avatarKey: String) {
        val uid = firebaseAuth.currentUser?.uid ?: return

        db.collection(USER_COLLECTION).document(uid)
            .update("avatar", avatarKey)
            .addOnSuccessListener {
                Toast.makeText(this, "Avatar updated!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update avatar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}


