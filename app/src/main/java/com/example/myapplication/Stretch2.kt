package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Stretch2 : AppCompatActivity() {
    private val goalsRepository = GoalsRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_stretch2)

        val backButton = findViewById<Button>(R.id.btnBack)
        backButton.setOnClickListener {
            finish() // return to Home
        }

        val nextButton = findViewById<Button>(R.id.btnNext)
        nextButton.setOnClickListener {
            val intent = Intent(this, Stretch3::class.java)
            startActivity(intent)
        }
        val submitButton = findViewById<Button>(R.id.btnSubmit)
        submitButton.setOnClickListener {
            goalsRepository.incrementGoals { success ->
                if (success) {
                    val intent = Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtra("OPEN_TAB", "DASHBOARD")
                    }
                    startActivity(intent)
                    finish()
                }
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}