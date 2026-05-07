package com.example.myapplication.ui.home

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.Profile
import com.example.myapplication.TaskAdapter
import com.example.myapplication.TaskRepository
import com.example.myapplication.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/*
  HomeFragment
  - Displays:
      • Completed goals count
      • Current plant information
      • Plant progress bar
      • "Choose New Seed" button (when plant is complete)
      •Add a task
  - Listens in real-time to the user's Firestore document.
  - Reacts automatically when goal or plant data changes.
*/
class HomeFragment : Fragment() {

    // View binding reference for accessing UI elements
    private var _binding: FragmentHomeBinding? = null

    /*Added by Lesley Del Cid:
      goalsListener
      - Real-time Firestore listener attached to users/{uid}.
      - Monitors both:
          • completedGoals (Goal Tracker)
          • PlantData fields (currentSeedId, plantProgress, plantCompleted)
    */
    private var goalsListener: ListenerRegistration? = null

    private val binding get() = _binding!!

    // Inflate the layout and initialize binding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        ViewModelProvider(this)[HomeViewModel::class.java]
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Main UI setup and logic
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Profile button stays the same
        binding.btnProfile.setOnClickListener {
            val intent = Intent(requireContext(), Profile::class.java)
            startActivity(intent)
        }

        /*Added by Lesley Del Cid:
          GOAL + PLANT INITIALIZATION
          - Retrieves the current logged-in user's UID.
          - If no user exists, display safe default values.
        */
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            //Added by Lesley Del Cid:
            // GOALS: Default display when no authenticated user is found
            binding.txtCompletedGoals.text = "Goals completed: 0"
        } else {

            //Added by Lesley Del Cid:
            // Reference to the user's Firestore document
            val userRef = FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)

            /*Added by Lesley Del Cid:
              REAL-TIME LISTENER
              - Updates UI automatically whenever:
                  • completedGoals changes (from stretches or other features)
                  • PlantData changes (new seed, progress updates, completion)
            */
            goalsListener = userRef.addSnapshotListener { snapshot, _ ->
                if (_binding == null) return@addSnapshotListener

                /*Added by Lesley Del Cid:
                  GOALS: Display total completed goals.
                  - Updated when stretches call GoalsRepository.incrementGoals().
                */
                val count = snapshot?.getLong("completedGoals") ?: 0L
                binding.txtCompletedGoals.text = "Goals completed: $count"

                /*Added by Lesley Del Cid:
                  PLANT DATA:
                  - Read plant state fields from the SAME user document.
                */
                val currentSeedId = snapshot?.getString("currentSeedId") ?: ""
                val plantProgress = snapshot?.getLong("plantProgress")?.toInt() ?: 0
                val plantCompleted = snapshot?.getBoolean("plantCompleted") ?: false

                /*Added by Lesley Del Cid:
                  CURRENT PLANT DISPLAY:
                  - Shows selected seed.
                  - If none selected, displays "None".
                */
                binding.txtCurrentPlant.text =
                    if (currentSeedId.isBlank())
                        "Current plant: None"
                    else
                        "Current plant: $currentSeedId"

                /*Added by Lesley Del Cid:
                  PLANT PROGRESS BAR:
                  - Progress value is stored in Firestore (0–100).
                  - Updated in real-time as plantProgress changes.
                */
                binding.plantProgressBar.progress = plantProgress

                /*Added by Lesley Del Cid:
                  PLANT COMPLETION:
                  - When plantCompleted == true,
                    user can start a new plant cycle.
                  - Button remains hidden while plant is still growing.
                */
                binding.btnChooseNewSeed.visibility =
                    if (plantCompleted) View.VISIBLE else View.GONE
            }
        }

        /*Added by Lesley Del Cid:
          SEED SELECTION FLOW:
          - Opens the Seed Pack Activity.
          - Used after a plant has been completed.
        */
        binding.btnChooseNewSeed.setOnClickListener {
            val intent = Intent(requireContext(), com.example.myapplication.Seeds::class.java)
            startActivity(intent)
        }

        //Task

        val repository = TaskRepository()

        val adapter = TaskAdapter(mutableListOf()) { task ->
            repository.toggleTask(task) // Toggle completion state
        }

        binding.recyclerViewTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewTasks.adapter = adapter

        // Listen for task updates and refresh UI
        repository.listenToTasks { tasks ->
            adapter.updateTasks(tasks)
        }

        binding.btnAdd.setOnClickListener {
            val taskText = binding.etTask.text.toString().trim()

            if (taskText.isNotEmpty()) {
                repository.addTask(taskText)
                binding.etTask.text.clear()
            }
        }

        // Open spin wheel activity
        binding.btnSpinWheel.setOnClickListener {
            val intent = Intent(requireContext(), SpinWheelActivity::class.java)
            startActivity(intent)
        }

        // Open shop dialog
        binding.btnOpenShop.setOnClickListener {
            showShopDialog(rewardRepository)
        }
    }

    // Update badge (GM, GA, GE, GN) based on current time
    override fun onResume() {
        super.onResume()
        setTimeBadge()
    }

    private fun setTimeBadge() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        val (badgeText, badgeColor) = when (hour) {
            in 5..11 -> "GM" to "#6A4FBF"   // Morning
            in 12..16 -> "GA" to "#4CAF50"  // Afternoon
            in 17..20 -> "GE" to "#FF9800"  // Evening
            else -> "GN" to "#3F51B5"       // Night
        }

        binding.tvTimeBadge.text = badgeText
        binding.tvTimeBadge.backgroundTintList =
            ColorStateList.valueOf(badgeColor.toColorInt())
    }

    // Generic popup for rewards and shop messages
    private fun showRewardPopup(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    // Display shop items and handle purchases
    private fun showShopDialog(rewardRepository: RewardRepository) {
        val shopItems = rewardRepository.getShopSeeds().toList()
        val itemLabels = shopItems.map { "${it.first} - ${it.second} pts" }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Seed Shop")
            .setItems(itemLabels) { _, which ->
                val selectedSeed = shopItems[which].first

                rewardRepository.buyShopSeed(selectedSeed) { success, message ->
                    showRewardPopup(
                        if (success) "Shop Purchase" else "Shop",
                        message
                    )
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    // Prevent memory leaks by clearing binding
    override fun onDestroyView() {
        super.onDestroyView()

        /*
          CLEANUP:
          - Removes Firestore listener to prevent memory leaks.
          - Important because this listener tracks goal + plant state.
        */
        goalsListener?.remove()
        goalsListener = null

        _binding = null
    }
}