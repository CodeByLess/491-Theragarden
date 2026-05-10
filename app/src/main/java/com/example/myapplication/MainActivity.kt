package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
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

        // Added by Lesley Del Cid:
        // Hides the default ActionBar so it does not
        // cover the Home screen UI.
        supportActionBar?.hide()

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

        // Added by Lesley Del Cid:
        // Removed setupActionBarWithNavController because it brings back
        // the top ActionBar on every bottom navigation page.
        // setupActionBarWithNavController(navController, appBarConfiguration)

        navView.setupWithNavController(navController)

        handleOpenTab(intent, navView)

        QuizManager(this).checkAndShowQuiz()
    }

    /*Added by Lesley Del Cid:
      Set up the global plant listener so the app can detect plant stage
      changes and show milestone popups across any screen.
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
      - Shows a popup when the plant changes to sprout.
      - Shows a single bloom/completion popup when the plant reaches bloom.
      - Uses SharedPreferences to prevent duplicate popups.

      Important:
      - In this project, bloom and completion happen at the same threshold.
      - Because of that, the app should show only ONE final popup at bloom
        instead of showing both a bloom popup and a separate completion popup.
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

            /* Added by Lesley Del Cid:
              Stage changed:
              - Show sprout popup when the plant first reaches sprout.
              - Show bloom popup when the plant reaches bloom.
              - Since bloom and completion happen together in this app,
                the bloom popup acts as the final milestone popup.
            */
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

            /*Added by Lesley Del Cid:
              Completed:
              - Update popup memory so completed state stays in sync.
              - Do not show a second completion popup, because bloom already
                serves as the final milestone popup in this design.
            */
            if (plantCompleted != lastCompletedShown) {
                prefs.edit()
                    .putBoolean("lastCompletedShown", plantCompleted)
                    .apply()
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
      - Appears when the plant reaches bloom.
      - In this project, bloom also means the plant is completed.
      - Allows the user to immediately choose a new seed for the next cycle.
    */
    private fun showBloomPopup() {
        if (isFinishing) return

        AlertDialog.Builder(this)
            .setTitle("Bloomed!")
            .setMessage("Your plant bloomed and is fully grown! Would you like to pick a new seed?")
            .setPositiveButton("Pick New Seed") { _, _ ->
                val intent = Intent(this, Seeds::class.java)
                startActivity(intent)
            }
            .setNegativeButton("Not yet", null)
            .show()
    }

    /*Added by Lesley Del Cid:
      startNewPlantCycle
      - Resets only the current plant cycle fields for the newly selected seed.
      - Does not reset completedPlants because that field stores the user's
        lifetime total of completed plants.
      - Also resets popup memory so milestone popups can appear again for
        the next plant cycle.
    */
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