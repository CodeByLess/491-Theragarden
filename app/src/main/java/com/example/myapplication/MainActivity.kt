package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.myapplication.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    /*Added by Lesley Del Cid:
      Global plant listener
      - Allows plant milestone popups to appear on ANY screen.
    */
    private var plantListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_garden,
                R.id.navigation_dashboard,
                R.id.navigation_stats,
                R.id.navigation_notifications
            )
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        handleOpenTab(intent, navView)
    }
    /*Added by Lesley Del Cid:
          setup the plant listener to be able to update the plant stage and know what stage we are on
        */
    override fun onStart() {
        super.onStart()
        startGlobalPlantListener()
    }

    override fun onStop() {
        super.onStop()
        plantListener?.remove()
        plantListener = null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOpenTab(intent, binding.navView)
    }

    private fun handleOpenTab(intent: Intent?, navView: BottomNavigationView) {
        when (intent?.getStringExtra("OPEN_TAB")) {
            "DASHBOARD" -> navView.selectedItemId = R.id.navigation_dashboard
        }
    }

    /*Added by Lesley Del Cid:
      startGlobalPlantListener
      - Watches Firestore plant fields globally.
      - Shows popups when stage changes to sprout or bloom.
      - Bloom popup allows user to immediately pick a new seed.
      - Uses SharedPreferences to prevent duplicate popups.
    */
    private fun startGlobalPlantListener() {

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val userRef = FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)

        val prefs = getSharedPreferences("plant_popups", Context.MODE_PRIVATE)

        plantListener?.remove()
        plantListener = null

        plantListener = userRef.addSnapshotListener { snapshot, _ ->
            if (snapshot == null || isFinishing) return@addSnapshotListener

            val plantStage = snapshot.getString("plantStage") ?: "dirt"
            val plantCompleted = snapshot.getBoolean("plantCompleted") ?: false

            val lastStageShown = prefs.getString("lastStageShown", null)
            val lastCompletedShown = prefs.getBoolean("lastCompletedShown", false)

            // First load baseline (no popup)
            if (lastStageShown == null) {
                prefs.edit()
                    .putString("lastStageShown", plantStage)
                    .putBoolean("lastCompletedShown", plantCompleted)
                    .apply()
                return@addSnapshotListener
            }

            // Stage changed
            if (plantStage != lastStageShown) {

                when (plantStage.lowercase()) {
                    "sprout" -> showMessagePopup(
                        "Sprouted!",
                        "Your plant just sprouted. Keep going!"
                    )

                    "bloom" -> showBloomPopup()
                }

                prefs.edit().putString("lastStageShown", plantStage).apply()
            }

            // Completed
            if (plantCompleted != lastCompletedShown) {
                if (plantCompleted) {
                    showMessagePopup(
                        "Plant Complete!",
                        "Your plant is fully grown. You can pick a new seed now."
                    )
                }
                prefs.edit().putBoolean("lastCompletedShown", plantCompleted).apply()
            }
        }
    }

    private fun showMessagePopup(title: String, message: String) {
        if (isFinishing) return

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    /*Added by Lesley Del Cid:
      showBloomPopup
      - Appears when plant hits bloom.
      - Allows user to pick a new seed immediately.
    */
    private fun showBloomPopup() {
        if (isFinishing) return

        AlertDialog.Builder(this)
            .setTitle("Bloomed!")
            .setMessage("Your plant bloomed! Would you like to pick a new seed?")
            .setPositiveButton("Pick New Seed") { _, _ ->
                val intent = Intent(this, Seeds::class.java)
                startActivity(intent)
            }
            .setNegativeButton("Not yet", null)
            .show()
    }

    private fun showSeedPickerPopup() {

        val seeds = arrayOf(
            "Sunflower Seed",
            "Strawberry Seed",
            "Lavender Seed"
        )

        AlertDialog.Builder(this)
            .setTitle("Choose a seed to grow")
            .setItems(seeds) { _, which ->
                val chosenSeed = seeds[which]
                startNewPlantCycle(chosenSeed)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startNewPlantCycle(chosenSeed: String) {

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userRef = FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)

        val updates = hashMapOf<String, Any>(
            "currentSeedId" to chosenSeed,
            "plantProgress" to 0,
            "plantCompleted" to false,
            "plantStage" to "dirt",
            "plantSubmits" to 0,
            "bloomReached" to false
        )

        userRef.update(updates)

        val prefs = getSharedPreferences("plant_popups", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("lastStageShown", "dirt")
            .putBoolean("lastCompletedShown", false)
            .apply()
    }
}