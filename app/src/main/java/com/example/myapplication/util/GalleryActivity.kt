package com.example.myapplication

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GalleryActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SELECTED_IMAGE_PATH = "selectedImagePath"
    }

    private val PREFS_NAME = "TheraGardenPrefs"
    private val KEY_FAVORITE_IMAGES = "favorite_images"

    private lateinit var highlightImage: ImageView
    private lateinit var highlightDateText: TextView

    private val slideshowHandler = Handler(Looper.getMainLooper())
    private var slideshowFiles: List<File> = emptyList()
    private var currentSlideIndex = 0

    private val slideshowRunnable = object : Runnable {
        override fun run() {
            if (slideshowFiles.isNotEmpty()) {
                currentSlideIndex = (currentSlideIndex + 1) % slideshowFiles.size
                showHighlightImage(slideshowFiles[currentSlideIndex])
                slideshowHandler.postDelayed(this, 3000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        val btnBack = findViewById<Button>(R.id.btnBackGallery)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        highlightImage = findViewById(R.id.highlightImage)
        highlightDateText = findViewById(R.id.highlightDateText)

        btnBack.setOnClickListener { finish() }
        recyclerView.layoutManager = GridLayoutManager(this, 3)

        lateinit var adapter: GalleryAdapter
        val imageFiles = loadImages()

        adapter = GalleryAdapter(
            images = imageFiles,
            isFavorite = { file -> isFavorite(file.absolutePath) },
            onFavoriteToggle = { file ->
                toggleFavorite(file.absolutePath)
                refreshHighlightReel()
                adapter.notifyDataSetChanged()

                val message = if (isFavorite(file.absolutePath)) {
                    "Added to favorites"
                } else {
                    "Removed from favorites"
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            },
            onDeleteRequested = { fileToDelete ->
                val deleted = try {
                    fileToDelete.delete()
                } catch (e: Exception) {
                    false
                }

                if (deleted) {
                    removeFromFavorites(fileToDelete.absolutePath)
                    Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
                    adapter.updateFiles(loadImages())
                    refreshHighlightReel()
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
        refreshHighlightReel()
    }

    override fun onDestroy() {
        super.onDestroy()
        slideshowHandler.removeCallbacks(slideshowRunnable)
    }

    private fun refreshHighlightReel() {
        slideshowHandler.removeCallbacks(slideshowRunnable)
        slideshowFiles = loadFavoriteImages()

        if (slideshowFiles.isNotEmpty()) {
            currentSlideIndex = 0
            showHighlightImage(slideshowFiles[currentSlideIndex])

            if (slideshowFiles.size > 1) {
                slideshowHandler.postDelayed(slideshowRunnable, 3000)
            }
        } else {
            highlightImage.setImageDrawable(null)
            highlightDateText.text = "No favorite photos yet"
        }
    }

    private fun loadFavoriteImages(): List<File> {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val favoritePaths = prefs.getStringSet(KEY_FAVORITE_IMAGES, emptySet()) ?: emptySet()

        return favoritePaths
            .map { File(it) }
            .filter { it.exists() && it.isFile }
            .sortedByDescending { it.lastModified() }
    }

    private fun isFavorite(imagePath: String): Boolean {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val favorites = prefs.getStringSet(KEY_FAVORITE_IMAGES, emptySet()) ?: emptySet()
        return favorites.contains(imagePath)
    }

    private fun toggleFavorite(imagePath: String) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val favorites = prefs.getStringSet(KEY_FAVORITE_IMAGES, emptySet())?.toMutableSet()
            ?: mutableSetOf()

        if (favorites.contains(imagePath)) {
            favorites.remove(imagePath)
        } else {
            favorites.add(imagePath)
        }

        prefs.edit().putStringSet(KEY_FAVORITE_IMAGES, favorites).apply()
    }

    private fun removeFromFavorites(imagePath: String) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val favorites = prefs.getStringSet(KEY_FAVORITE_IMAGES, emptySet())?.toMutableSet()
            ?: mutableSetOf()

        favorites.remove(imagePath)
        prefs.edit().putStringSet(KEY_FAVORITE_IMAGES, favorites).apply()
    }

    private fun showHighlightImage(file: File) {
        if (file.exists()) {
            highlightImage.setImageURI(Uri.fromFile(file))
            highlightDateText.text = formatDateTime(file.lastModified())
        }
    }

    private fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMMM d, yyyy - h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
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