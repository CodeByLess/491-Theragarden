package com.example.myapplication

// Android and UI-related imports
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.Toast
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

    companion object {
        const val EXTRA_SELECTED_IMAGE_PATH = "selectedImagePath"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        val btnBack = findViewById<Button>(R.id.btnBackGallery)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)

        btnBack.setOnClickListener { finish() }

        recyclerView.layoutManager = GridLayoutManager(this, 3)

        lateinit var adapter: GalleryAdapter

        adapter = GalleryAdapter(
            loadImages(),
            onDeleteRequested = { fileToDelete ->

                val deleted = try {
                    fileToDelete.delete()
                } catch (e: Exception) {
                    false
                }

                if (deleted) {
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                    adapter.updateFiles(loadImages())
                } else {
                    Toast.makeText(this, "Could not delete", Toast.LENGTH_SHORT).show()
                }
            },
            onImageSelected = { selectedFile ->
                val resultIntent = Intent()
                resultIntent.putExtra(EXTRA_SELECTED_IMAGE_PATH, selectedFile.absolutePath)
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        )

        recyclerView.adapter = adapter
    }

    private fun loadImages(): List<File> {

        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val galleryFolder = File(storageDir, "TheragardenGallery")

        if (!galleryFolder.exists()) return emptyList()

        return galleryFolder.listFiles()
            ?.filter { it.isFile }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }
}