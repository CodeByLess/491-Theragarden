package com.example.myapplication

// Android and UI-related imports
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

// File handling import
import java.io.File

/*
 * GalleryActivity
 *
 * This activity displays all saved gratitude photos
 * inside a RecyclerView using a grid layout.
 *
 * Images are loaded from the app-specific
 * "TheragardenGallery" folder located in the
 * external pictures directory.
 */
class GalleryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        // Initialize UI components
        val btnBack = findViewById<Button>(R.id.btnBackGallery)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        // Back button closes the gallery activity
        btnBack.setOnClickListener {
            finish()
        }

        /*
         * Set RecyclerView layout manager.
         * GridLayoutManager displays images in a grid format.
         * The value "3" represents three columns per row.
         */
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        // Set adapter with list of image files
        recyclerView.adapter = GalleryAdapter(loadImages())
    }

    /*
     * Loads image files from the TheragardenGallery folder.
     *
     * Steps:
     * 1. Access app-specific external pictures directory
     * 2. Locate the custom gallery folder
     * 3. Return only valid image files
     * 4. Sort images by most recent first
     */
    private fun loadImages(): List<File> {

        // Get app-specific external pictures directory
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        // Access TheragardenGallery subfolder
        val galleryFolder = File(storageDir, "TheragardenGallery")

        // If folder does not exist, return empty list
        if (!galleryFolder.exists()) return emptyList()

        /*
         * Retrieve list of files:
         * - Filter to include only files (not directories)
         * - Sort by last modified date in descending order
         * - If null, return empty list
         */
        return galleryFolder.listFiles()
            ?.filter { it.isFile }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }
}
