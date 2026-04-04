package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent

class Doodle : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doodle)

        val btnBack = findViewById<Button>(R.id.btnBack)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnLibrary = findViewById<Button>(R.id.btnLibrary)



        btnBack.setOnClickListener {
            finish()
        }
        // Placeholder actions (for now)
        btnSave.setOnClickListener {
            Toast.makeText(this, "Save coming soon", Toast.LENGTH_SHORT).show()
        }

        btnLibrary.setOnClickListener {
            startActivity(Intent(this, LibraryDoodle::class.java))
        }


    }
}