package com.example.myapplication

// Android and lifecycle imports
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope

// Lottie animation import
import com.airbnb.lottie.LottieAnimationView

// Firebase imports
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

// Kotlin coroutine imports
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Math utility
import kotlin.math.ceil

/*
 * Breathing Activity
 * This activity controls a guided breathing session using coroutines.
 * It manages inhale, hold, and exhale phases across multiple rounds.
 * Firebase is used to store completion data.
 */
class Breathing : AppCompatActivity() {

    // UI components
    private lateinit var btnBack: Button
    private lateinit var btnStartStop: Button
    private lateinit var tvPhase: TextView
    private lateinit var tvPrompt: TextView
    private lateinit var tvRound: TextView
    private lateinit var imgBreath: LottieAnimationView

    // Enum to track session state
    private enum class RunState { IDLE, RUNNING, PAUSED }

    // Enum to track breathing phase
    private enum class Step { INHALE, HOLD, EXHALE }

    // Current state of session
    private var state = RunState.IDLE

    // Coroutine job for breathing session
    private var breathingJob: Job? = null

    // Duration values for each phase (in milliseconds)
    private val inhaleMs = 4000L
    private val holdMs = 7000L
    private val exhaleMs = 8000L

    // Total number of breathing rounds
    private val totalRounds = 5

    // Tracking current round and step
    private var currentRound = 0
    private var currentStep = Step.INHALE
    private var remainingMs = inhaleMs

    // Firebase authentication and Firestore references
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    // Added by Lesley Del Cid:
    // Helper that updates completedGoals, plant progress, and returns to Dashboard
    private lateinit var completionHelper: SelfCareCompletionHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enables edge-to-edge display layout
        enableEdgeToEdge()
        setContentView(R.layout.activity_breathing)

        // Added by Lesley Del Cid:
        // Initialize reusable self-care completion helper
        completionHelper = SelfCareCompletionHelper(this)

        // Connect UI elements to layout
        btnBack = findViewById(R.id.btnBack)
        btnStartStop = findViewById(R.id.btnStartStop)
        tvPhase = findViewById(R.id.tvPhase)
        tvPrompt = findViewById(R.id.tvPrompt)
        tvRound = findViewById(R.id.tvRound)
        imgBreath = findViewById(R.id.imgBreath)

        // Back button stops session and closes activity
        btnBack.setOnClickListener {
            stopAndReset(showStoppedMessage = false)
            finish()
        }

        // Start/Pause/Resume button logic
        btnStartStop.setOnClickListener {
            when (state) {
                RunState.IDLE -> startBreathing()
                RunState.RUNNING -> pauseBreathing()
                RunState.PAUSED -> resumeBreathing()
            }
        }

        // Adjust layout for system bars (status/navigation)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize UI
        setPhase("Ready", "Press Start when you’re ready.")
        tvRound.text = "Round 0 / $totalRounds"
        updateButton()

        // Start looping animation
        imgBreath.playAnimation()
    }

    /*
     * Starts a new breathing session.
     * Initializes state values and launches coroutine.
     */
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
                // Ignore cancellation
            } finally {
                if (state != RunState.IDLE && currentRound == 0) {
                    stopAndReset()
                }
            }
        }
    }

    // Pauses the breathing session
    private fun pauseBreathing() {
        if (state != RunState.RUNNING) return
        state = RunState.PAUSED
        setPhase(phaseName(), "Paused. Tap Resume to continue.")
        updateButton()
        imgBreath.pauseAnimation()
    }

    // Resumes the breathing session
    private fun resumeBreathing() {
        if (state != RunState.PAUSED) return
        state = RunState.RUNNING
        updateButton()
        imgBreath.resumeAnimation()
    }

    /*
     * Core breathing loop.
     * Iterates through rounds and steps.
     */
    private suspend fun runSession() {
        while (currentRound in 1..totalRounds) {
            when (currentStep) {
                Step.INHALE -> {
                    setPhase("INHALE", "Breathe in through your nose…")
                    animateBreath(true, remainingMs)
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
                    animateBreath(false, remainingMs)
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

    /*
     * Countdown timer that updates UI every 100ms.
     * Pauses if session is paused.
     */
    private suspend fun tickDownOrReturn() {
        val tick = 100L

        while (remainingMs > 0) {

            while (state == RunState.PAUSED) {
                delay(150L)
            }

            if (state != RunState.RUNNING) return

            delay(tick)

            remainingMs -= tick

            val secs =
                ceil(remainingMs / 1000.0).toInt().coerceAtLeast(0)

            tvPhase.text = "${phaseName()} • $secs"
        }
    }

    // Called when session completes successfully
    private fun finishSession() {

        breathingJob = null
        state = RunState.IDLE

        // Save breathing completion stats to Firestore
        saveBreathingCompletionToFirestore()

        // Added by Lesley Del Cid:
        // Count completed breathing session as a completed self-care activity.
        // This updates completedGoals, updates plant progress,
        // and returns user to Dashboard.
        completionHelper.completeSelfCareActivity()

        setPhase("Done", "Nice work. Want to go again?")
        tvRound.text = "Round $totalRounds / $totalRounds"
        updateButton()

        imgBreath.playAnimation()
    }

    // Stops session and resets values
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

        imgBreath.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(150)
            .start()

        imgBreath.playAnimation()
    }

    // Updates round display
    private fun updateRound() {
        tvRound.text = "Round $currentRound / $totalRounds"
    }

    // Updates button text based on current state
    private fun updateButton() {

        btnStartStop.text =
            when (state) {
                RunState.IDLE -> "Start Breathing"
                RunState.RUNNING -> "Pause"
                RunState.PAUSED -> "Resume"
            }
    }

    // Updates phase label and prompt text
    private fun setPhase(phase: String, prompt: String) {
        tvPhase.text = phase
        tvPrompt.text = prompt
    }

    // Returns current phase name
    private fun phaseName(): String {

        return when (currentStep) {
            Step.INHALE -> "INHALE"
            Step.HOLD -> "HOLD"
            Step.EXHALE -> "EXHALE"
        }
    }

    // Scales animation for inhale/exhale effect
    private fun animateBreath(expand: Boolean, duration: Long) {

        val scale =
            if (expand) 1.20f else 0.95f

        imgBreath.animate()
            .scaleX(scale)
            .scaleY(scale)
            .setDuration(duration)
            .start()
    }

    override fun onPause() {
        super.onPause()

        if (state == RunState.RUNNING) {
            pauseBreathing()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        breathingJob?.cancel()
    }

    /*
     * Saves completion data to Firestore.
     * Increments totalCompleted and updates lastCompleted timestamp.
     */
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