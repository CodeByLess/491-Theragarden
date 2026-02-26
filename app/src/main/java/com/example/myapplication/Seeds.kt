package com.example.myapplication

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/*
 Seeds (Seedpack / Inventory Page)
 - Displays at least 3 starter seeds
 - When user picks a seed:
     • saves it to Firestore users/{uid}
     • resets plant progress data
     • closes this screen (returns to Home)
*/
class Seeds : AppCompatActivity() {

    // Starter seeds the user can choose from (inventory)
    private val starterSeeds = listOf(
        "Sunflower Seed",
        "Strawberry Seed",
        "Lavender Seed"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_seeds)

        // Back button (kept)
        val backButton = findViewById<Button>(R.id.btnBack)
        backButton.setOnClickListener {
            finish() // return to Home
        }

        // Hook up the 3 seed buttons (inventory items)
        val seed1 = findViewById<Button>(R.id.btnSeed1)
        val seed2 = findViewById<Button>(R.id.btnSeed2)
        val seed3 = findViewById<Button>(R.id.btnSeed3)

        /*
          Set button labels from our starter seed list
          (optional, but keeps it consistent if we ever rename seeds)
        */
        seed1.text = starterSeeds[0]
        seed2.text = starterSeeds[1]
        seed3.text = starterSeeds[2]

        /*
          When a seed is clicked, save it + reset plant data
          NOTE: Only assign ONE click listener per button
        */
        seed1.setOnClickListener { chooseSeed(starterSeeds[0]) }
        seed2.setOnClickListener { chooseSeed(starterSeeds[1]) }
        seed3.setOnClickListener { chooseSeed(starterSeeds[2]) }

        // Window insets (kept)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /*
      chooseSeed(seedName)
      - Writes plant state to Firestore so HomeFragment updates automatically.
      - Resets bloomReached + popup memory so sprout/bloom popups work correctly next cycle.
    */
    private fun chooseSeed(seedName: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            Toast.makeText(this, "No user signed in.", Toast.LENGTH_SHORT).show()
            return
        }

        val userRef = FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)

        // Reset plant cycle when selecting a new seed
        val updates = hashMapOf<String, Any>(
            "currentSeedId" to seedName,   // Home shows this as "Current plant: ___"
            "plantProgress" to 0,          // reset progress bar
            "plantCompleted" to false,     // hide "Choose New Seed" button again
            "plantStage" to "dirt",        // starting stage
            "plantSubmits" to 0,           // restart submit count

            //
            // Reset bloom flag so bloom popup can happen again for the new plant
            "bloomReached" to false
        )

        userRef.update(updates)
            .addOnSuccessListener {

                /*
                  Reset popup memory so global popups don't get stuck/skipped
                */
                val prefs = getSharedPreferences("plant_popups", Context.MODE_PRIVATE)
                prefs.edit()
                    .putString("lastStageShown", "dirt")
                    .putBoolean("lastCompletedShown", false)
                    .putBoolean("lastBloomPopupShown", false)
                    .apply()

                Toast.makeText(this, "You chose: $seedName", Toast.LENGTH_SHORT).show()
                finish() // go back to Home
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to choose seed.", Toast.LENGTH_SHORT).show()
            }
    }
}