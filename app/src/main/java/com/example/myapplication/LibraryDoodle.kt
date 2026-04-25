package com.example.myapplication

import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class LibraryDoodle : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_library_doodle)

        val btnBack = findViewById<Button>(R.id.btnBack)
        val container = findViewById<LinearLayout>(R.id.imageContainer)

        btnBack.setOnClickListener {
            finish() // go back
        }

        loadImages(container) // load saved doodles
    }

    private fun loadImages(container: LinearLayout) {

        // only grab images from our doodle folder
        val projection = arrayOf(MediaStore.Images.Media._ID)

        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf("%Pictures/Doodles%")

        val cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Images.Media.DATE_ADDED} DESC" // newest first
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)

                val uri = Uri.withAppendedPath(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )

                val imageView = ImageView(this)

                val size = 650 // image size

                val params = LinearLayout.LayoutParams(size, size)
                params.setMargins(0, 24, 0, 24)
                params.gravity = android.view.Gravity.CENTER

                imageView.layoutParams = params
                imageView.setImageURI(uri)
                imageView.scaleType = ImageView.ScaleType.CENTER_CROP

                container.addView(imageView) // add to screen
            }
        }
    }
}