package com.example.myapplication

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class Doodle : AppCompatActivity() {

    // Added by Lesley Del Cid:
    // Helper that updates completedGoals, plant progress, and returns to Dashboard
    private lateinit var completionHelper: SelfCareCompletionHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doodle)

        // Added by Lesley Del Cid:
        // Initialize reusable self-care completion helper
        completionHelper = SelfCareCompletionHelper(this)

        // buttons
        val btnBack = findViewById<Button>(R.id.btnBack)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnLibrary = findViewById<Button>(R.id.btnLibrary)
        val btnClear = findViewById<Button>(R.id.btnClear)
        val btnUndo = findViewById<Button>(R.id.btnUndo)

        // drawing view
        val drawingView = findViewById<DrawingView>(R.id.drawingView)

        // color options
        val colorBlack = findViewById<View>(R.id.colorBlack)
        val colorRed = findViewById<View>(R.id.colorRed)
        val colorBlue = findViewById<View>(R.id.colorBlue)
        val colorGreen = findViewById<View>(R.id.colorGreen)
        val colorBrown = findViewById<View>(R.id.colorBrown)
        val colorYellow = findViewById<View>(R.id.colorYellow)

        btnBack.setOnClickListener {
            finish() // go back
        }

        btnSave.setOnClickListener {
            val bitmap = drawingView.getBitmap()
            saveToGallery(bitmap) // save drawing
        }

        btnLibrary.setOnClickListener {
            startActivity(Intent(this, LibraryDoodle::class.java)) // open library
        }

        btnClear.setOnClickListener {
            drawingView.clearCanvas() // clear drawing
        }

        btnUndo.setOnClickListener {
            drawingView.undo() // undo last line
        }

        // change colors
        colorBlack.setOnClickListener { drawingView.setColor(Color.BLACK) }
        colorRed.setOnClickListener { drawingView.setColor(Color.RED) }
        colorBlue.setOnClickListener { drawingView.setColor(Color.BLUE) }
        colorGreen.setOnClickListener { drawingView.setColor(Color.GREEN) }
        colorBrown.setOnClickListener { drawingView.setColor(Color.parseColor("#795548")) }
        colorYellow.setOnClickListener { drawingView.setColor(Color.parseColor("#FFC107")) }
    }

    // save image to phone
    private fun saveToGallery(bitmap: Bitmap) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "doodle_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Doodles")
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        uri?.let {
            contentResolver.openOutputStream(it)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                Toast.makeText(this, "Saved to Gallery!", Toast.LENGTH_SHORT).show()

                // Added by Lesley Del Cid:
                // Saving a doodle counts as completing a self-care activity.
                // This updates completedGoals, updates plant progress, and returns user to Dashboard.
                completionHelper.completeSelfCareActivity()
            }
        }
    }
}