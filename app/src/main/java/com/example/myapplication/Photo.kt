package com.example.myapplication

// Android and permission-related imports
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider

// File and date handling imports
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/*
 * Photo Activity
 *
 * This activity allows the user to:
 * - Capture a photo using the device camera
 * - Save the image into a custom TheragardenGallery folder
 * - Preview the captured image
 * - Navigate to a gallery screen
 *
 * It also handles runtime camera permission requests.
 */
class Photo : AppCompatActivity() {

    // Request codes for camera intent and permission handling
    private val CAMERA_REQUEST = 100
    private val CAMERA_PERMISSION = 101

    // File reference for the captured image
    private var photoFile: File? = null

    // ImageView used to preview the captured image
    private lateinit var imagePreview: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo)

        // Initialize UI elements
        val btnBack = findViewById<Button>(R.id.btnBack)
        val btnCapture = findViewById<Button>(R.id.btnCapture)
        val btnGallery = findViewById<Button>(R.id.btnGallery)
        imagePreview = findViewById(R.id.imagePreview)

        // Back button closes the activity
        btnBack.setOnClickListener { finish() }

        // Opens gallery activity to view saved images
        btnGallery.setOnClickListener {
            startActivity(Intent(this, GalleryActivity::class.java))
        }

        // Capture button checks camera permission before opening camera
        btnCapture.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                // Request camera permission if not already granted
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION
                )
            } else {
                // Open camera if permission is already granted
                openCamera()
            }
        }
    }

    /*
     * Opens the device camera using an intent.
     * Creates a file location where the captured image will be stored.
     */
    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        // Ensure there is a camera app available
        if (intent.resolveActivity(packageManager) != null) {

            // Create file to store captured image
            photoFile = createImageFile()

            // Generate secure URI using FileProvider
            val uri: Uri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                photoFile!!
            )

            // Specify file output location for full-resolution image
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)

            // Launch camera activity
            startActivityForResult(intent, CAMERA_REQUEST)

        } else {
            Toast.makeText(this, "No camera app available.", Toast.LENGTH_LONG).show()
        }
    }

    /*
     * Creates a uniquely named image file using a timestamp.
     * Stores images inside TheragardenGallery folder within app's
     * external pictures directory.
     */
    private fun createImageFile(): File {

        // Generate timestamp for unique file naming
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            .format(System.currentTimeMillis())

        // Get app-specific external pictures directory
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        // Create custom gallery folder if it does not exist
        val galleryFolder = File(storageDir, "TheragardenGallery")
        if (!galleryFolder.exists()) galleryFolder.mkdirs()

        // Return new image file reference
        return File(galleryFolder, "gratitude_$timeStamp.jpg")
    }

    /*
     * Handles result returned from camera activity.
     * Displays preview if photo was successfully saved.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {

            val file = photoFile

            // Verify file exists before previewing
            if (file != null && file.exists()) {

                // Display captured image in ImageView
                imagePreview.setImageURI(Uri.fromFile(file))

                Toast.makeText(this, "Saved to TheragardenGallery.", Toast.LENGTH_SHORT).show()

            } else {
                Toast.makeText(this, "Save failed. Try again.", Toast.LENGTH_SHORT).show()
            }

        } else {
            Toast.makeText(this, "Photo canceled.", Toast.LENGTH_SHORT).show()
        }
    }

    /*
     * Handles runtime permission result for camera access.
     * If granted, opens camera automatically.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission granted → open camera
            openCamera()
        } else {
            Toast.makeText(this, "Camera permission required.", Toast.LENGTH_SHORT).show()
        }
    }
}
