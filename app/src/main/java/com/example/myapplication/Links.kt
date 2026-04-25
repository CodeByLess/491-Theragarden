package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Links : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_links)

        // Back button
        val backButton = findViewById<Button>(R.id.btnBack)
        backButton.setOnClickListener {
            finish()
        }

        // Buttons
        val btn988 = findViewById<Button>(R.id.btn988)
        val btnCrisisText = findViewById<Button>(R.id.btnCrisisText)
        val btnNami = findViewById<Button>(R.id.btnNami)
        val btnMHA = findViewById<Button>(R.id.btnMHA)
        val btn7Cups = findViewById<Button>(R.id.btn7Cups)
        val btnLAHelp = findViewById<Button>(R.id.btnLAHelp)
        val btnMentalHealth = findViewById<Button>(R.id.btnMentalHealth)

        // website links
        btn988.setOnClickListener { openLink("https://988lifeline.org/") }
        btnCrisisText.setOnClickListener { openLink("https://www.crisistextline.org/") }
        btnNami.setOnClickListener { openLink("https://www.nami.org/") }
        btnMHA.setOnClickListener { openLink("https://mhanational.org/get-help/") }
        btn7Cups.setOnClickListener { openLink("https://www.7cups.com/") }
        btnLAHelp.setOnClickListener { openLink("https://dmh.lacounty.gov/") }
        btnMentalHealth.setOnClickListener { openLink("https://www.mentalhealth.com/") }

        // edge to edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun openLink(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        startActivity(intent)
    }
}