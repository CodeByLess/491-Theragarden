package com.example.myapplication

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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class Photo : AppCompatActivity() {

    private val CAMERA_REQUEST = 100
    private val CAMERA_PERMISSION = 101
    private val GALLERY_REQUEST = 102

    private val PREFS_NAME = "TheraGardenPrefs"
    private val KEY_FAVORITE_BANNER = "favorite_banner"
    private val KEY_FAVORITE_IMAGES = "favorite_images"

    // Added by Lesley Del Cid:
    // Helper that updates completedGoals, plant progress, and returns to Dashboard
    private lateinit var completionHelper: SelfCareCompletionHelper

    private var photoFile: File? = null
    private lateinit var imagePreview: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo)

        // Added by Lesley Del Cid:
        // Initialize reusable self-care completion helper
        completionHelper = SelfCareCompletionHelper(this)

        val btnBack = findViewById<Button>(R.id.btnBack)
        val btnCapture = findViewById<Button>(R.id.btnCapture)
        val btnGallery = findViewById<Button>(R.id.btnGallery)
        imagePreview = findViewById(R.id.imagePreview)

        btnBack.setOnClickListener { finish() }

        btnGallery.setOnClickListener {
            val intent = Intent(this, GalleryActivity::class.java)
            startActivityForResult(intent, GALLERY_REQUEST)
        }

        btnCapture.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION
                )

            } else {
                openCamera()
            }
        }
    }

    private fun openCamera() {

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        if (intent.resolveActivity(packageManager) != null) {

            photoFile = createImageFile()

            val uri: Uri = FileProvider.getUriForFile(
                this,
                "${packageName}.provider",
                photoFile!!
            )

            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)

            startActivityForResult(intent, CAMERA_REQUEST)

        } else {

            Toast.makeText(
                this,
                "No camera app available.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun createImageFile(): File {

        val timeStamp =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(System.currentTimeMillis())

        val storageDir =
            getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        val galleryFolder =
            File(storageDir, "TheragardenGallery")

        if (!galleryFolder.exists()) {
            galleryFolder.mkdirs()
        }

        return File(galleryFolder, "gratitude_$timeStamp.jpg")
    }

    private fun saveFavoriteBanner(imagePath: String) {

        val prefs =
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        prefs.edit()
            .putString(KEY_FAVORITE_BANNER, imagePath)
            .apply()
    }

    private fun addToFavorites(imagePath: String) {

        val prefs =
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        val currentFavorites =
            prefs.getStringSet(KEY_FAVORITE_IMAGES, emptySet())
                ?.toMutableSet()
                ?: mutableSetOf()

        currentFavorites.add(imagePath)

        prefs.edit()
            .putStringSet(KEY_FAVORITE_IMAGES, currentFavorites)
            .apply()
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CAMERA_REQUEST &&
            resultCode == Activity.RESULT_OK
        ) {

            val file = photoFile

            if (file != null && file.exists()) {

                imagePreview.setImageURI(Uri.fromFile(file))

                saveFavoriteBanner(file.absolutePath)
                addToFavorites(file.absolutePath)

                Toast.makeText(
                    this,
                    "Saved, set as homepage banner, and added to favorites.",
                    Toast.LENGTH_SHORT
                ).show()

                // Added by Lesley Del Cid:
                // Taking and saving a gratitude photo counts as completing
                // a self-care activity.
                // This updates completedGoals, updates plant progress,
                // and returns user to Dashboard.
                completionHelper.completeSelfCareActivity()

            } else {

                Toast.makeText(
                    this,
                    "Save failed. Try again.",
                    Toast.LENGTH_SHORT
                ).show()
            }

        } else if (requestCode == GALLERY_REQUEST &&
            resultCode == Activity.RESULT_OK
        ) {

            val selectedPath =
                data?.getStringExtra(
                    GalleryActivity.EXTRA_SELECTED_IMAGE_PATH
                )

            if (!selectedPath.isNullOrBlank()) {

                val selectedFile = File(selectedPath)

                if (selectedFile.exists()) {

                    imagePreview.setImageURI(Uri.fromFile(selectedFile))

                    saveFavoriteBanner(selectedFile.absolutePath)
                    addToFavorites(selectedFile.absolutePath)

                    Toast.makeText(
                        this,
                        "Selected image set as homepage banner and added to favorites.",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Added by Lesley Del Cid:
                    // Selecting a gratitude photo from gallery counts as
                    // completing a self-care activity.
                    // This updates completedGoals, updates plant progress,
                    // and returns user to Dashboard.
                    completionHelper.completeSelfCareActivity()

                } else {

                    Toast.makeText(
                        this,
                        "Selected image not found.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        } else {

            Toast.makeText(
                this,
                "Canceled.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )

        if (requestCode == CAMERA_PERMISSION &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {

            openCamera()

        } else {

            Toast.makeText(
                this,
                "Camera permission required.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}