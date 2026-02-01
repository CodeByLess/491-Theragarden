package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieAnimationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil

class Breathing : AppCompatActivity() {

    private lateinit var btnBack: Button
    private lateinit var btnStartStop: Button
    private lateinit var tvPhase: TextView
    private lateinit var tvPrompt: TextView
    private lateinit var tvRound: TextView
    private lateinit var imgBreath: LottieAnimationView

    private enum class RunState { IDLE, RUNNING, PAUSED }
    private enum class Step { INHALE, HOLD, EXHALE }

    private var state = RunState.IDLE
    private var breathingJob: Job? = null

    private val inhaleMs = 4000L
    private val holdMs = 7000L
    private val exhaleMs = 8000L
    private val totalRounds = 5

    private var currentRound = 0
    private var currentStep = Step.INHALE
    private var remainingMs = inhaleMs

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

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

        btnBack.setOnClickListener {
            stopAndReset(showStoppedMessage = false)
            finish()
        }

        btnStartStop.setOnClickListener {
            when (state) {
                RunState.IDLE -> startBreathing()
                RunState.RUNNING -> pauseBreathing()
                RunState.PAUSED -> resumeBreathing()
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setPhase("Ready", "Press Start when you’re ready.")
        tvRound.text = "Round 0 / $totalRounds"
        updateButton()

        imgBreath.playAnimation()
    }

    private fun startBreathing() {
        if (breathingJob?.isActive == true) return

        state = RunState.RUNNING
        currentRound = 1
        currentStep = Step.INHALE
        remainingMs = inhaleMs

        tvPrompt.text = "Get comfortable. We’ll do $totalRounds rounds."
        updateRound()
        updateButton()

        imgBreath.playAnimation()

        breathingJob = lifecycleScope.launch {
            try {
                runSession()
            } catch (_: CancellationException) {
            } finally {
                if (state != RunState.IDLE && currentRound == 0) {
                    stopAndReset()
                }
            }
        }
    }

    private fun pauseBreathing() {
        if (state != RunState.RUNNING) return
        state = RunState.PAUSED
        setPhase(phaseName(), "Paused. Tap Resume to continue.")
        updateButton()
        imgBreath.pauseAnimation()
    }

    private fun resumeBreathing() {
        if (state != RunState.PAUSED) return
        state = RunState.RUNNING
        updateButton()
        imgBreath.resumeAnimation()
    }

    private suspend fun runSession() {
        while (currentRound in 1..totalRounds) {
            when (currentStep) {
                Step.INHALE -> {
                    setPhase("INHALE", "Breathe in through your nose…")
                    animateBreath(expand = true, duration = remainingMs)
                    tickDownOrReturn()
                    currentStep = Step.HOLD
                    remainingMs = holdMs
                }
                Step.HOLD -> {
                    setPhase("HOLD", "Hold gently…")
                    tickDownOrReturn()
                    currentStep = Step.EXHALE
                    remainingMs = exhaleMs
                }
                Step.EXHALE -> {
                    setPhase("EXHALE", "Slowly breathe out…")
                    animateBreath(expand = false, duration = remainingMs)
                    tickDownOrReturn()

                    if (currentRound == totalRounds) break

                    currentRound++
                    currentStep = Step.INHALE
                    remainingMs = inhaleMs
                    updateRound()
                }
            }
            if (state == RunState.IDLE) return
        }
        finishSession()
    }

    private suspend fun tickDownOrReturn() {
        val tick = 100L
        while (remainingMs > 0) {
            while (state == RunState.PAUSED) {
                delay(150L)
            }
            if (state != RunState.RUNNING) return
            delay(tick)
            remainingMs -= tick
            val secs = ceil(remainingMs / 1000.0).toInt().coerceAtLeast(0)
            tvPhase.text = "${phaseName()} • $secs"
        }
    }

    private fun finishSession() {
        breathingJob = null
        state = RunState.IDLE
        saveBreathingCompletionToFirestore()
        setPhase("Done", "Nice work. Want to go again?")
        tvRound.text = "Round $totalRounds / $totalRounds"
        updateButton()
        imgBreath.playAnimation()
    }

    private fun stopAndReset(showStoppedMessage: Boolean = true) {
        breathingJob?.cancel()
        breathingJob = null
        state = RunState.IDLE
        currentRound = 0
        currentStep = Step.INHALE
        remainingMs = inhaleMs
        if (showStoppedMessage) {
            setPhase("Ready", "Stopped. Press Start when you’re ready.")
            tvRound.text = "Round 0 / $totalRounds"
        }
        updateButton()
        imgBreath.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
        imgBreath.playAnimation()
    }

    private fun updateRound() {
        tvRound.text = "Round $currentRound / $totalRounds"
    }

    private fun updateButton() {
        btnStartStop.text = when (state) {
            RunState.IDLE -> "Start Breathing"
            RunState.RUNNING -> "Pause"
            RunState.PAUSED -> "Resume"
        }
    }

    private fun setPhase(phase: String, prompt: String) {
        tvPhase.text = phase
        tvPrompt.text = prompt
    }

    private fun phaseName(): String {
        return when (currentStep) {
            Step.INHALE -> "INHALE"
            Step.HOLD -> "HOLD"
            Step.EXHALE -> "EXHALE"
        }
    }

    private fun animateBreath(expand: Boolean, duration: Long) {
        val scale = if (expand) 1.20f else 0.95f
        imgBreath.animate()
            .scaleX(scale)
            .scaleY(scale)
            .setDuration(duration)
            .start()
    }

    override fun onPause() {
        super.onPause()
        if (state == RunState.RUNNING) pauseBreathing()
    }

    override fun onDestroy() {
        super.onDestroy()
        breathingJob?.cancel()
    }

    private fun saveBreathingCompletionToFirestore() {
        val uid = auth.currentUser?.uid ?: return
        val data = hashMapOf(
            "lastCompleted" to FieldValue.serverTimestamp(),
            "totalCompleted" to FieldValue.increment(1)
        )
        db.collection("users")
            .document(uid)
            .collection("activities")
            .document("breathing")
            .set(data, SetOptions.merge())
    }
}