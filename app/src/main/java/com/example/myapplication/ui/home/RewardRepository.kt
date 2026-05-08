package com.example.myapplication.ui.home

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class RewardRepository {

    // Firebase instances for database and authentication
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Reference to the current user's document in Firestore
    private fun userRef() =
        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid)
        }

    // Update reward progress based on completed tasks
    fun updateRewardProgress(progress: Int, onNewUnlock: (String?) -> Unit = {}) {
        val ref = userRef() ?: run {
            onNewUnlock(null)
            return
        }

        ref.set(
            mapOf("rewardProgress" to progress),
            SetOptions.merge()
        ).addOnSuccessListener {
            checkAndUnlockTaskSeeds(progress, onNewUnlock)
        }.addOnFailureListener {
            onNewUnlock(null)
        }
    }

    // Listen for changes in bloom points and update UI
    fun listenToBloomPoints(onResult: (Int) -> Unit) {
        val ref = userRef() ?: run {
            onResult(0)
            return
        }

        ref.addSnapshotListener { snapshot, _ ->
            val bloomPoints = snapshot?.getLong("bloomPoints")?.toInt() ?: 0
            onResult(bloomPoints)
        }
    }

    // Add bloom points when a plant blooms
    fun addBloomPoints(points: Int) {
        val ref = userRef() ?: return

        ref.set(
            mapOf("bloomPoints" to FieldValue.increment(points.toLong())),
            SetOptions.merge()
        )
    }

    // Check if task milestones are reached and unlock seeds
    private fun checkAndUnlockTaskSeeds(progress: Int, onNewUnlock: (String?) -> Unit) {
        val ref = userRef() ?: run {
            onNewUnlock(null)
            return
        }

        // Milestones for unlocking task-based seeds
        val taskMilestones = listOf(
            5 to "Sunflower Seed",
            10 to "Lavender Seed",
            15 to "Strawberry Seed",
            20 to "Tulip Seed"
        )

        ref.get().addOnSuccessListener { document ->
            val taskUnlockedSeeds =
                document.get("taskUnlockedSeeds") as? List<String> ?: emptyList()

            val newReward = taskMilestones.firstOrNull { (milestone, seedName) ->
                progress >= milestone && !taskUnlockedSeeds.contains(seedName)
            }

            // Unlock seed if milestone reached and not already unlocked
            if (newReward != null) {
                ref.update("taskUnlockedSeeds", FieldValue.arrayUnion(newReward.second))
                    .addOnSuccessListener {
                        onNewUnlock(newReward.second)
                    }
                    .addOnFailureListener {
                        ref.set(
                            mapOf("taskUnlockedSeeds" to listOf(newReward.second)),
                            SetOptions.merge()
                        ).addOnSuccessListener {
                            onNewUnlock(newReward.second)
                        }
                    }
            } else {
                onNewUnlock(null)
            }
        }.addOnFailureListener {
            onNewUnlock(null)
        }
    }

    // Get spin-only seeds that the user has NOT unlocked yet
    fun getAvailableSpinSeeds(allSpinSeeds: List<String>, onResult: (List<String>) -> Unit) {
        val ref = userRef() ?: run {
            onResult(emptyList())
            return
        }

        ref.get().addOnSuccessListener { document ->
            val unlocked =
                document.get("spinUnlockedSeeds") as? List<String> ?: emptyList()

            val available = allSpinSeeds.filter { !unlocked.contains(it) }
            onResult(available)
        }.addOnFailureListener {
            onResult(emptyList())
        }
    }

    // Unlock a spin wheel reward and store it in Firestore
    fun unlockSpinSeed(seedName: String, onResult: (Boolean) -> Unit) {
        val ref = userRef() ?: run {
            onResult(false)
            return
        }

        ref.update("spinUnlockedSeeds", FieldValue.arrayUnion(seedName))
            .addOnSuccessListener {
                onResult(true)
            }
            .addOnFailureListener {
                ref.set(
                    mapOf("spinUnlockedSeeds" to listOf(seedName)),
                    SetOptions.merge()
                ).addOnSuccessListener {
                    onResult(true)
                }.addOnFailureListener {
                    onResult(false)
                }
            }
    }

    // Added by Paula Awad:
    // Checks if the user already bought access to the Spin Wheel.
    fun isSpinWheelUnlocked(onResult: (Boolean) -> Unit) {
        val ref = userRef() ?: run {
            onResult(false)
            return
        }

        ref.get().addOnSuccessListener { document ->
            val unlocked = document.getBoolean("spinWheelUnlocked") ?: false
            onResult(unlocked)
        }.addOnFailureListener {
            onResult(false)
        }
    }

    // Added by Paula Awad:
    // Unlocks the Spin Wheel by charging 200 Bloom Points.
    fun buySpinWheelAccess(onResult: (Boolean, String) -> Unit) {
        val ref = userRef() ?: run {
            onResult(false, "User not signed in.")
            return
        }

        val spinWheelPrice = 200

        ref.get().addOnSuccessListener { document ->
            val currentPoints = document.getLong("bloomPoints")?.toInt() ?: 0
            val alreadyUnlocked = document.getBoolean("spinWheelUnlocked") ?: false

            if (alreadyUnlocked) {
                onResult(true, "Spin Wheel is already unlocked.")
                return@addOnSuccessListener
            }

            if (currentPoints < spinWheelPrice) {
                onResult(false, "You need 200 Bloom Points to unlock the Spin Wheel.")
                return@addOnSuccessListener
            }

            ref.set(
                mapOf(
                    "bloomPoints" to currentPoints - spinWheelPrice,
                    "spinWheelUnlocked" to true
                ),
                SetOptions.merge()
            ).addOnSuccessListener {
                onResult(true, "Spin Wheel unlocked!")
            }.addOnFailureListener {
                onResult(false, "Could not unlock Spin Wheel.")
            }
        }.addOnFailureListener {
            onResult(false, "Could not load Bloom Points.")
        }
    }

    // Return shop items and their prices
    fun getShopSeeds(): Map<String, Int> {
        return mapOf(
            "Bonsai Tree" to 20,
            "Cherry Blossom" to 30,
            "Palm Tree" to 40,
            "Venus Flytrap" to 50
        )
    }

    // Handle purchasing a seed from the shop
    fun buyShopSeed(seedName: String, onResult: (Boolean, String) -> Unit) {
        val ref = userRef() ?: run {
            onResult(false, "User not signed in.")
            return
        }

        val shopSeeds = getShopSeeds()
        val price = shopSeeds[seedName]

        if (price == null) {
            onResult(false, "Seed not found in shop.")
            return
        }

        ref.get().addOnSuccessListener { document ->
            val currentPoints = document.getLong("bloomPoints")?.toInt() ?: 0
            val ownedShopSeeds =
                document.get("shopUnlockedSeeds") as? List<String> ?: emptyList()

            // Prevent duplicate purchases
            if (ownedShopSeeds.contains(seedName)) {
                onResult(false, "You already own $seedName.")
                return@addOnSuccessListener
            }

            // Check if user has enough currency
            if (currentPoints < price) {
                onResult(false, "Not enough Bloom Points. $seedName costs $price Bloom Points.")
                return@addOnSuccessListener
            }

            // Deduct points and unlock seed
            ref.set(
                mapOf(
                    "bloomPoints" to currentPoints - price,
                    "shopUnlockedSeeds" to FieldValue.arrayUnion(seedName)
                ),
                SetOptions.merge()
            ).addOnSuccessListener {
                onResult(true, "You bought $seedName for $price Bloom Points.")
            }.addOnFailureListener {
                onResult(false, "Purchase failed.")
            }
        }.addOnFailureListener {
            onResult(false, "Could not load shop data.")
        }
    }
}