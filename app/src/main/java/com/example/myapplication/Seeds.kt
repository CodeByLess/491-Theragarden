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
  - Displays available starter seeds for the user to choose from.
  - Allows the user to begin a new plant cycle by selecting a seed.
  - When the user picks a seed:
      • saves the selected seed to Firestore users/{uid}
      • resets only the current plant cycle fields
      • preserves lifetime plant data such as completedPlants
      • closes this screen (returns to Home)
*/
class Seeds : AppCompatActivity() {

    /*
      Starter seed inventory
      - These seeds appear as selectable options in the Seedpack screen.
      - Each seed corresponds to a bloom image shown later in HomeFragment.
    */
    private val starterSeeds = listOf(
        "Sunflower Seed",
        "Strawberry Seed",
        "Lavender Seed",
        "Tulip Seed",
        "Cactus Seed",
        "Monstera Seed"
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

        // Hook up the 6 seed buttons (inventory items)
        val seed1 = findViewById<Button>(R.id.btnSeed1)
        val seed2 = findViewById<Button>(R.id.btnSeed2)
        val seed3 = findViewById<Button>(R.id.btnSeed3)
        val seed4 = findViewById<Button>(R.id.btnSeed4)
        val seed5 = findViewById<Button>(R.id.btnSeed5)
        val seed6 = findViewById<Button>(R.id.btnSeed6)

        /*
          Set button labels from our starter seed list.
          This keeps the UI text consistent with the seed names stored in code.
        */
        seed1.text = starterSeeds[0]
        seed2.text = starterSeeds[1]
        seed3.text = starterSeeds[2]
        seed4.text = starterSeeds[3]
        seed5.text = starterSeeds[4]
        seed6.text = starterSeeds[5]

        /*
          When a seed is clicked, save it and reset the current plant cycle.

          NOTE:
          - Only assign one click listener per button.
          - The completedPlants total should remain unchanged.
        */
        seed1.setOnClickListener { chooseSeed(starterSeeds[0]) }
        seed2.setOnClickListener { chooseSeed(starterSeeds[1]) }
        seed3.setOnClickListener { chooseSeed(starterSeeds[2]) }
        seed4.setOnClickListener { chooseSeed(starterSeeds[3]) }
        seed5.setOnClickListener { chooseSeed(starterSeeds[4]) }
        seed6.setOnClickListener { chooseSeed(starterSeeds[5]) }

        // Window insets (kept)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /*
      chooseSeed(seedName)
      - Writes the newly selected seed to Firestore so HomeFragment updates automatically.
      - Resets only the active plant cycle fields for the next growth cycle.
      - Does NOT reset completedPlants, because that field stores the user's
        lifetime total of completed plants.
      - Resets bloomReached and popup memory so stage popups work correctly
        for the newly selected plant.
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

        /*
          Reset only the current plant cycle fields when selecting a new seed.
          This keeps long-term progress fields, such as completedPlants, intact.
        */
        val updates = hashMapOf<String, Any>(
            "currentSeedId" to seedName,   // Home shows this as "Current plant: ___"
            "plantProgress" to 0,          // reset progress bar
            "plantCompleted" to false,     // hide "Choose New Seed" button again
            "plantStage" to "dirt",        // starting stage for the new plant
            "plantSubmits" to 0,           // restart submit count for the new plant

            // Reset bloom flag so the bloom popup can happen again for the new plant
            "bloomReached" to false
        )

        userRef.update(updates)
            .addOnSuccessListener {

                /*
                  Reset popup memory so global popups do not get stuck or skipped
                  when the user starts a new plant cycle.
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