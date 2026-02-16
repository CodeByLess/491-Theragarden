package com.example.myapplication

import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.time.LocalDate

class Affirmations : AppCompatActivity() {

    private val PREFS = "affirmation_prefs"
    private val KEY_TEXT = "daily_text"
    private val KEY_DATE = "daily_date"

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_affirmations)

        val backButton = findViewById<Button>(R.id.btnBack)
        val saveButton = findViewById<Button>(R.id.btnSave)
        val editText = findViewById<EditText>(R.id.etAffirmation)

        backButton.setOnClickListener {
            finish()
        }

        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        val today = LocalDate.now().toString()
        val savedDate = prefs.getString(KEY_DATE, "")

        // Reset if it's a new day
        if (savedDate == today) {
            editText.setText(prefs.getString(KEY_TEXT, ""))
        } else {
            prefs.edit().clear().apply()
        }

        saveButton.setOnClickListener {
            prefs.edit()
                .putString(KEY_TEXT, editText.text.toString())
                .putString(KEY_DATE, today)
                .apply()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}
