package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class Quote : AppCompatActivity() {

    // HTTP client to make API requests
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_quote)

        // Get references to UI elements
        val backButton = findViewById<Button>(R.id.btnBack)
        val newQuoteButton = findViewById<Button>(R.id.btnNewQuote)
        val quoteText = findViewById<TextView>(R.id.tvQuote)

        // Go back to previous screen
        backButton.setOnClickListener { finish() }

        // Makes layout adjust for status/navigation bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Function that calls the API and loads a new affirmation
        fun fetchAffirmation() {

            // Show loading text while waiting for response
            quoteText.text = "Loading affirmation..."

            // Build the request to the affirmations API
            val request = Request.Builder()
                .url("https://www.affirmations.dev/")
                .build()

            // Send request asynchronously
            client.newCall(request).enqueue(object : Callback {

                // If request fails (no internet, timeout, etc.)
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        quoteText.text = "Couldn’t load a quote. Try again."
                        Toast.makeText(this@Quote, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }

                // If request succeeds
                override fun onResponse(call: Call, response: Response) {
                    response.use {

                        // If server response isn’t successful
                        if (!it.isSuccessful) {
                            runOnUiThread { quoteText.text = "Couldn’t load a quote. Try again." }
                            return
                        }

                        // Get the raw JSON string
                        val bodyString = it.body?.string().orEmpty()

                        // Try to extract the "affirmation" value from JSON
                        val affirmation = try {
                            JSONObject(bodyString).optString("affirmation", "")
                        } catch (ex: Exception) {
                            ""
                        }

                        // Update UI on main thread
                        runOnUiThread {
                            quoteText.text = if (affirmation.isNotBlank()) affirmation
                            else "Couldn’t load a quote. Try again."
                        }
                    }
                }
            })
        }

        // Load a new quote when button is clicked
        newQuoteButton.setOnClickListener { fetchAffirmation() }

        // Automatically load a quote when screen opens
        fetchAffirmation()
    }
}
