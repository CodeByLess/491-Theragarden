package com.example.myapplication

// Import Android and Firebase libraries needed for the activity
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

// Activity responsible for allowing users to submit bug reports or issues
class SubmitIssueActivity : AppCompatActivity() {

    // UI components
    private lateinit var etTitle: EditText          // Input field for issue title
    private lateinit var etDescription: EditText    // Input field for issue description
    private lateinit var spCategory: Spinner        // Dropdown for issue category
    private lateinit var spPriority: Spinner        // Dropdown for issue priority
    private lateinit var btnSubmit: Button          // Button to submit issue
    private lateinit var btnBack: Button            // Button to go back to previous screen
    private lateinit var progress: ProgressBar      // Loading indicator while submitting
    private lateinit var tvStatus: TextView         // Text view to display status messages

    // Firebase Firestore instance (database)
    private val db by lazy { FirebaseFirestore.getInstance() }

    // Firebase Authentication instance (for identifying logged-in users)
    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the UI layout for this activity
        setContentView(R.layout.activity_submit_issue)

        // Initialize the Back button
        btnBack = findViewById(R.id.btnBack)

        // When clicked, close the activity and return to previous screen
        btnBack.setOnClickListener {
            finish()
        }

        // Initialize UI input fields and components
        etTitle = findViewById(R.id.etTitle)
        etDescription = findViewById(R.id.etDescription)
        spCategory = findViewById(R.id.spCategory)
        spPriority = findViewById(R.id.spPriority)
        btnSubmit = findViewById(R.id.btnSubmit)
        progress = findViewById(R.id.progress)
        tvStatus = findViewById(R.id.tvStatus)

        // Populate the dropdown menus with values
        setupSpinners()

        // When the submit button is clicked, call submitIssue()
        btnSubmit.setOnClickListener {
            submitIssue()
        }
    }

    // Setup dropdown menus for issue category and priority
    private fun setupSpinners() {

        // Category spinner setup
        ArrayAdapter.createFromResource(
            this,
            R.array.issue_categories,                      // categories defined in arrays.xml
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )
            spCategory.adapter = adapter
        }

        // Priority spinner setup
        ArrayAdapter.createFromResource(
            this,
            R.array.issue_priorities,                      // priorities defined in arrays.xml
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
            )
            spPriority.adapter = adapter
        }
    }

    // Function responsible for submitting the issue to Firestore
    private fun submitIssue() {

        // Get user input values
        val title = etTitle.text.toString().trim()
        val description = etDescription.text.toString().trim()

        // Validate that title is not empty
        if (title.isEmpty()) {
            etTitle.error = "Title is required"
            etTitle.requestFocus()
            return
        }

        // Validate that description is not empty
        if (description.isEmpty()) {
            etDescription.error = "Description is required"
            etDescription.requestFocus()
            return
        }

        // Show loading spinner and disable inputs
        setLoading(true)
        tvStatus.text = ""

        // Check if the user is logged in
        val user = auth.currentUser
        if (user == null) {
            setLoading(false)
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            return
        }

        // Get the user's unique Firebase ID
        val uid = user.uid

        // Get selected category and priority from dropdowns
        val category = spCategory.selectedItem.toString()
        val priority = spPriority.selectedItem.toString()

        // Create a map containing the issue data
        val issue = hashMapOf(
            "userId" to uid,
            "title" to title,
            "description" to description,
            "category" to category,
            "priority" to priority,
            "status" to "open",                          // default issue status
            "createdAt" to FieldValue.serverTimestamp(), // timestamp from Firebase server
            "deviceModel" to Build.MODEL,                // device used when submitting
            "osVersion" to "Android ${Build.VERSION.RELEASE}"
        )

        // Save the issue inside Firestore under the user's document
        db.collection("users")
            .document(uid)
            .collection("issues")
            .add(issue)

            // If successful
            .addOnSuccessListener {

                // Notify user
                Toast.makeText(this, "Issue submitted. Thanks!", Toast.LENGTH_SHORT).show()

                // Clear form fields
                etTitle.text.clear()
                etDescription.text.clear()
                spCategory.setSelection(0)
                spPriority.setSelection(0)

                // Display success message
                tvStatus.text = "Submitted successfully."

                // Re-enable UI
                setLoading(false)
            }

            // If submission fails
            .addOnFailureListener { e ->
                tvStatus.text = "Submit failed: ${e.message}"
                setLoading(false)
            }
    }

    // Controls loading state by enabling/disabling UI components
    private fun setLoading(loading: Boolean) {

        // Show or hide progress spinner
        progress.visibility = if (loading) View.VISIBLE else View.GONE

        // Disable UI while loading
        btnSubmit.isEnabled = !loading
        btnBack.isEnabled = !loading
        etTitle.isEnabled = !loading
        etDescription.isEnabled = !loading
        spCategory.isEnabled = !loading
        spPriority.isEnabled = !loading
    }
}