package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
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
    private lateinit var btnChangeEmail: Button
    private lateinit var btnChangePassword: Button
    private lateinit var btnSave: Button
    private lateinit var btnDelete: Button
    private lateinit var btnEditAvatar: Button

    private var newEmail: String? = null
    private var newPassword: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_edit_page)

        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        btnBack = findViewById(R.id.backProfile)
        btnChangeEmail = findViewById(R.id.ChangeEmail)
        btnChangePassword = findViewById(R.id.ChangePassword)
        btnSave = findViewById(R.id.SaveProfile)
        btnDelete = findViewById(R.id.DeleteAccount)
        btnEditAvatar = findViewById(R.id.EditAvatar)


        btnBack.setOnClickListener { finish() }

        btnChangeEmail.setOnClickListener { showChangeEmailDialog() }

        btnChangePassword.setOnClickListener { showChangePasswordDialog() }

        btnSave.setOnClickListener { saveChanges() }

        btnDelete.setOnClickListener { deleteAccount() }

        btnEditAvatar.setOnClickListener { showAvatarDialog() }
    }

    private fun showChangeEmailDialog() {
        val input = EditText(this).apply {
            hint = "New email"
            setText(firebaseAuth.currentUser?.email ?: "")
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }

        AlertDialog.Builder(this)
            .setTitle("Change Email")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                newEmail = input.text.toString().trim()
                if (newEmail.isNullOrEmpty()) {
                    Toast.makeText(this, "Email cannot be empty", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Email stored. Press SAVE to apply.", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showChangePasswordDialog() {
        val input = EditText(this).apply {
            hint = "New password (6+ characters)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        AlertDialog.Builder(this)
            .setTitle("Change Password")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newPassword = input.text.toString().trim()
                if (newPassword.length < 6) {
                    Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val user = firebaseAuth.currentUser
                if (user != null) {
                    user.updatePassword(newPassword)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Password updated successfully", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                "Error updating password: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                } else {
                    Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveChanges() {
        val user = firebaseAuth.currentUser ?: return
        val uid = user.uid

        // update email if changed
        if (!newEmail.isNullOrEmpty()) {
            user.updateEmail(newEmail!!).addOnSuccessListener {
                db.collection(USER_COLLECTION).document(uid)
                    .update("email", newEmail)
                Toast.makeText(this, "Email updated", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                Toast.makeText(this, "Error updating email: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // update password if changed
        if (!newPassword.isNullOrEmpty()) {
            user.updatePassword(newPassword!!).addOnSuccessListener {
                Toast.makeText(this, "Password updated", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                Toast.makeText(this, "Error updating password: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }

        Toast.makeText(this, "Changes saved", Toast.LENGTH_SHORT).show()
        finish()
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
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "Error deleting auth: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
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


