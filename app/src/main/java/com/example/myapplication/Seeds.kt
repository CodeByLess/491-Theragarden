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

    // Added by Lesley:
    // Starter seeds always available to the player.
    private val starterSeeds = mutableListOf(
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

        // Hook up the seed buttons (inventory items)
        val seed1 = findViewById<Button>(R.id.btnSeed1)
        val seed2 = findViewById<Button>(R.id.btnSeed2)
        val seed3 = findViewById<Button>(R.id.btnSeed3)
        val seed4 = findViewById<Button>(R.id.btnSeed4)
        val seed5 = findViewById<Button>(R.id.btnSeed5)
        val seed6 = findViewById<Button>(R.id.btnSeed6)
        val seed7 = findViewById<Button>(R.id.btnSeed7)
        val seed8 = findViewById<Button>(R.id.btnSeed8)
        val seed9 = findViewById<Button>(R.id.btnSeed9)
        val seed10 = findViewById<Button>(R.id.btnSeed10)

        // Added by Lesley:
        // Load purchased shop seeds and spin wheel seeds into inventory.
        loadShopSeeds()

        // Window insets (kept)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    /*
      loadShopSeeds()
      - Loads purchased shop seeds from Firestore.
      - Adds them into the inventory list so they appear
        in the Seedpack screen after purchase.
      - Also loads Spin Wheel exclusive seeds.
    */
    private fun loadShopSeeds() {

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { document ->

                val shopSeeds =
                    document.get("shopUnlockedSeeds") as? List<String> ?: emptyList()

                // Added by Lesley:
                // Loads spin wheel reward seeds.
                val spinSeeds =
                    document.get("spinUnlockedSeeds") as? List<String> ?: emptyList()

                // Add purchased shop seeds into inventory
                for (seed in shopSeeds) {
                    if (!starterSeeds.contains(seed)) {
                        starterSeeds.add(seed)
                    }
                }

                // Add spin wheel seeds into inventory
                for (seed in spinSeeds) {
                    if (!starterSeeds.contains(seed)) {
                        starterSeeds.add(seed)
                    }
                }

                // Added by Lesley:
                // Includes extra buttons for purchased shop seeds
                // and spin wheel reward seeds.
                val buttons = listOf(
                    findViewById<Button>(R.id.btnSeed1),
                    findViewById<Button>(R.id.btnSeed2),
                    findViewById<Button>(R.id.btnSeed3),
                    findViewById<Button>(R.id.btnSeed4),
                    findViewById<Button>(R.id.btnSeed5),
                    findViewById<Button>(R.id.btnSeed6),
                    findViewById<Button>(R.id.btnSeed7),
                    findViewById<Button>(R.id.btnSeed8),
                    findViewById<Button>(R.id.btnSeed9),
                    findViewById<Button>(R.id.btnSeed10),

                    // Added by Lesley:
                    // Spin Wheel exclusive seed buttons.
                    findViewById<Button>(R.id.btnSeed11),
                    findViewById<Button>(R.id.btnSeed12),
                    findViewById<Button>(R.id.btnSeed13),
                    findViewById<Button>(R.id.btnSeed14)
                )

                for (i in buttons.indices) {

                    if (i < starterSeeds.size) {

                        buttons[i].visibility = Button.VISIBLE
                        buttons[i].text = starterSeeds[i]

                        buttons[i].setOnClickListener {
                            chooseSeed(starterSeeds[i])
                        }

                    } else {

                        // Hide unused buttons if fewer than available button slots
                        buttons[i].visibility = Button.GONE
                    }
                }
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