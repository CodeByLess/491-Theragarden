package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.*

class Breathing : AppCompatActivity() {

    private lateinit var btnBack: Button
    private lateinit var btnStartStop: Button
    private lateinit var tvPhase: TextView
    private lateinit var tvPrompt: TextView
    private lateinit var tvRound: TextView
    private lateinit var imgBreath: ImageView

    private var isRunning = false
    private var breathingJob: Job? = null

    // MVP timings (you can tweak later)
    private val inhaleMs = 4000L
    private val holdMs = 2000L
    private val exhaleMs = 6000L
    private val totalRounds = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_breathing)

        btnBack = findViewById(R.id.btnBack)
        btnStartStop = findViewById(R.id.btnStartStop)
        tvPhase = findViewById(R.id.tvPhase)
        tvPrompt = findViewById(R.id.tvPrompt)
        tvRound = findViewById(R.id.tvRound)
        imgBreath = findViewById(R.id.imgBreath)

        btnBack.setOnClickListener { finish() }

        btnStartStop.setOnClickListener {
            if (!isRunning) startBreathing() else stopBreathing()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun startBreathing() {
        isRunning = true
        btnStartStop.text = "Stop"
        tvPrompt.text = "Get comfortable. We’ll do $totalRounds rounds."

        breathingJob = CoroutineScope(Dispatchers.Main).launch {
            for (round in 1..totalRounds) {
                if (!isRunning) break
                tvRound.text = "Round $round / $totalRounds"

                // INHALE
                setPhase("INHALE", "Breathe in through your nose…")
                animateBreath(expand = true, duration = inhaleMs)
                delay(inhaleMs)

                if (!isRunning) break

                // HOLD
                setPhase("HOLD", "Hold gently…")
                delay(holdMs)

                if (!isRunning) break

                // EXHALE
                setPhase("EXHALE", "Slowly breathe out…")
                animateBreath(expand = false, duration = exhaleMs)
                delay(exhaleMs)
            }

            // finished
            if (isRunning) {
                setPhase("Done", "Nice work. Want to go again?")
                tvRound.text = "Round $totalRounds / $totalRounds"
            }
            isRunning = false
            btnStartStop.text = "Start Breathing"
        }
    }

    private fun stopBreathing() {
        isRunning = false
        breathingJob?.cancel()
        breathingJob = null

        setPhase("Ready", "Stopped. Press Start when you’re ready.")
        tvRound.text = "Round 0 / $totalRounds"
        btnStartStop.text = "Start Breathing"

        // reset animation size
        imgBreath.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
    }

    private fun setPhase(phase: String, prompt: String) {
        tvPhase.text = phase
        tvPrompt.text = prompt
    }

    private fun animateBreath(expand: Boolean, duration: Long) {
        val target = if (expand) 1.35f else 0.85f
        imgBreath.animate()
            .scaleX(target)
            .scaleY(target)
            .setDuration(duration)
            .start()
    }

    override fun onDestroy() {
        super.onDestroy()
        breathingJob?.cancel()
    }
}
