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

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_quote)

        val backButton = findViewById<Button>(R.id.btnBack)
        val newQuoteButton = findViewById<Button>(R.id.btnNewQuote)
        val quoteText = findViewById<TextView>(R.id.tvQuote)

        backButton.setOnClickListener { finish() }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fun fetchAffirmation() {
            quoteText.text = "Loading affirmation..."

            val request = Request.Builder()
                .url("https://affirmations.dev/")
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        quoteText.text = "Couldn’t load a quote. Try again."
                        Toast.makeText(this@Quote, "Network error", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        if (!it.isSuccessful) {
                            runOnUiThread {
                                quoteText.text = "Couldn’t load a quote. Try again."
                            }
                            return
                        }

                        val bodyString = it.body?.string().orEmpty()
                        val affirmation = try {
                            JSONObject(bodyString).optString("affirmation", "")
                        } catch (ex: Exception) {
                            ""
                        }

                        runOnUiThread {
                            quoteText.text = if (affirmation.isNotBlank()) affirmation
                            else "Couldn’t load a quote. Try again."
                        }
                    }
                }
            })
        }

        newQuoteButton.setOnClickListener { fetchAffirmation() }

        // Load one immediately
        fetchAffirmation()
    }
}
