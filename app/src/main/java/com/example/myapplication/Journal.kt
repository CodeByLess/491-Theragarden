package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class Journal : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var journalAdapter: JournalAdapter
    private lateinit var etEntry: EditText
    private lateinit var btnSave: Button
    private lateinit var rvEntries: RecyclerView

    private val entries = mutableListOf<JournalEntry>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_journal)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val backButton = findViewById<Button>(R.id.btnBack)
        etEntry = findViewById(R.id.etJournalEntry)
        btnSave = findViewById(R.id.btnSaveEntry)
        rvEntries = findViewById(R.id.rvJournalEntries)

        journalAdapter = JournalAdapter(entries) { entry ->
            showEditDialog(entry)
        }
        rvEntries.layoutManager = LinearLayoutManager(this)
        rvEntries.adapter = journalAdapter

        backButton.setOnClickListener { finish() }

        btnSave.setOnClickListener {
            val text = etEntry.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(this, "Write something first :)", Toast.LENGTH_SHORT).show()
            } else {
                saveEntry(text)
            }
        }

        loadEntries()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun saveEntry(text: String) {
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "You must be logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val entry = hashMapOf(
            "text" to text,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users")
            .document(uid)
            .collection("journalEntries")
            .add(entry)
            .addOnSuccessListener {
                etEntry.setText("")
                Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show()
                loadEntries()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Save failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadEntries() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(uid)
            .collection("journalEntries")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                entries.clear()
                for (doc in snapshot.documents) {
                    val text = doc.getString("text") ?: ""
                    val createdAt = doc.getLong("createdAt") ?: 0L
                    entries.add(
                        JournalEntry(
                            id = doc.id,
                            text = text,
                            createdAt = createdAt
                        )
                    )
                }
                journalAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Load failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showEditDialog(entry: JournalEntry) {
        val input = EditText(this)
        input.setText(entry.text)
        input.setSelection(input.text.length)

        AlertDialog.Builder(this)
            .setTitle("Edit entry")
            .setView(input)
            .setPositiveButton("Update") { _, _ ->
                val newText = input.text.toString().trim()
                if (newText.isEmpty()) {
                    Toast.makeText(this, "Entry cannot be empty", Toast.LENGTH_SHORT).show()
                } else {
                    updateEntry(entry.id, newText)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateEntry(entryId: String, newText: String) {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(uid)
            .collection("journalEntries")
            .document(entryId)
            .update(
                mapOf(
                    "text" to newText,
                    "updatedAt" to System.currentTimeMillis() // optional field
                )
            )
            .addOnSuccessListener {
                Toast.makeText(this, "Updated!", Toast.LENGTH_SHORT).show()
                loadEntries()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
