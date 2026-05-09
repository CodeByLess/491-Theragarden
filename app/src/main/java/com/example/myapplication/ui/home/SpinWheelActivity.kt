package com.example.myapplication.ui.home

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.example.myapplication.databinding.ActivitySpinWheelBinding

class SpinWheelActivity : AppCompatActivity() {

    // View binding for accessing UI elements
    private lateinit var binding: ActivitySpinWheelBinding

    // Repository to handle reward logic and Firestore interactions
    private lateinit var rewardRepository: RewardRepository

    // Spin-exclusive plants (only obtainable through the wheel)
    private val spinSeeds = listOf(
        "Trumpet Flower",
        "Blue Rose",
        "Crystal Lotus",
        "Rainbow Tulip"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate layout
        binding = ActivitySpinWheelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ensure user is signed in before using reward system
        if (FirebaseAuth.getInstance().currentUser == null) {
            finish()
            return
        }

        rewardRepository = RewardRepository()

        // Set the wheel segments (plants shown on the wheel)
        binding.spinWheelView.setSegments(spinSeeds)

        // Back button to return to previous screen
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Spin button logic
        binding.btnSpin.setOnClickListener {

            // Disable button to prevent multiple clicks
            binding.btnSpin.isEnabled = false

            // Added by Lesley:
            // Charges 200 Bloom Points every time the user spins.
            rewardRepository.buySpin { success, message ->

                // If player cannot afford the spin or purchase failed
                if (!success) {

                    binding.btnSpin.isEnabled = true

                    AlertDialog.Builder(this)
                        .setTitle("Spin Wheel")
                        .setMessage(message)
                        .setPositiveButton("OK", null)
                        .show()

                    return@buySpin
                }

                // Get available (not yet unlocked) spin rewards
                rewardRepository.getAvailableSpinSeeds(spinSeeds) { availableSeeds ->

                    // If all rewards are already unlocked
                    if (availableSeeds.isEmpty()) {

                        binding.btnSpin.isEnabled = true

                        AlertDialog.Builder(this)
                            .setTitle("Spin Wheel")
                            .setMessage("You already unlocked all spin-exclusive plants.")
                            .setPositiveButton("OK", null)
                            .show()

                    } else {

                        // Pick a random reward from available seeds
                        val selectedSeed = availableSeeds.random()
                        val selectedIndex = spinSeeds.indexOf(selectedSeed)

                        // Spin the wheel animation and land on selected index
                        binding.spinWheelView.spinToIndex(selectedIndex) {

                            // Save unlocked reward to Firestore
                            rewardRepository.unlockSpinSeed(selectedSeed) { unlockSuccess ->

                                binding.btnSpin.isEnabled = true

                                // Show result popup
                                val resultMessage =
                                    if (unlockSuccess) {
                                        "You spent 200 Bloom Points and won: $selectedSeed"
                                    } else {
                                        "You spent 200 Bloom Points and landed on: $selectedSeed"
                                    }

                                AlertDialog.Builder(this)
                                    .setTitle("Spin Result")
                                    .setMessage(resultMessage)
                                    .setPositiveButton("Awesome", null)
                                    .show()
                            }
                        }
                    }
                }
            }
        }
    }
}