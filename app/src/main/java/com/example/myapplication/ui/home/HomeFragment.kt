package com.example.myapplication.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.appcompat.app.AlertDialog
import com.example.myapplication.Profile
import com.example.myapplication.TaskAdapter
import com.example.myapplication.TaskRepository
import com.example.myapplication.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.example.myapplication.R

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textHome
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Profile button stays the same
        binding.btnProfile.setOnClickListener {
            val intent = Intent(requireContext(), Profile::class.java)
            startActivity(intent)
        }

        /*Added by Lesley Del Cid:
          GOAL + PLANT INITIALIZATION
        */
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            binding.txtCompletedGoals.text = "Goals completed: 0"
        } else {

            val userRef = FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)

            goalsListener = userRef.addSnapshotListener { snapshot, _ ->
                if (_binding == null) return@addSnapshotListener

                // GOALS
                val count = snapshot?.getLong("completedGoals") ?: 0L
                binding.txtCompletedGoals.text = "Goals completed: $count"

                // PLANT DATA
                val currentSeedId = snapshot?.getString("currentSeedId") ?: ""
                val plantProgress = snapshot?.getLong("plantProgress")?.toInt() ?: 0
                val plantCompleted = snapshot?.getBoolean("plantCompleted") ?: false
                val plantStage = snapshot?.getString("plantStage") ?: "dirt"

                // CURRENT PLANT DISPLAY
                binding.txtCurrentPlant.text =
                    if (currentSeedId.isBlank())
                        "Current plant: None"
                    else
                        "Current plant: $currentSeedId"

                // STAGE DISPLAY
                binding.txtPlantStage.text = "Stage: $plantStage"

                /*Added by Lesley Del Cid:
                  PLANT IMAGE DISPLAY
                */
                val imageRes = when (plantStage.lowercase()) {
                    "dirt" -> R.drawable.dirt
                    "sprout" -> R.drawable.sprout
                    "bloom" -> {
                        when (currentSeedId) {
                            "Sunflower Seed" -> R.drawable.sunflower
                            "Strawberry Seed" -> R.drawable.strawberry
                            "Lavender Seed" -> R.drawable.lavender
                            else -> R.drawable.sprout
                        }
                    }
                    else -> R.drawable.dirt
                }

                binding.imgPlantStage.setImageResource(imageRes)

                // PROGRESS BAR
                binding.plantProgressBar.progress = plantProgress

                /*Added by Lesley Del Cid:
                  SEED BUTTON VISIBILITY:
                  - User can pick a new seed when:
                      • plantStage is bloom  OR
                      • plantCompleted is true
                */
                binding.btnChooseNewSeed.visibility =
                    if (plantCompleted || plantStage.lowercase() == "bloom") View.VISIBLE else View.GONE

                /*Added by Lesley Del Cid:
                  Update button text depending on state
                */
                binding.btnChooseNewSeed.text =
                    if (plantCompleted) "Choose New Seed" else "Pick a New Seed"
            }
        }

        /*Added by Lesley Del Cid:
          SEED SELECTION FLOW
          - Manual seed picker from Home page.
          - Milestone popups are handled globally in MainActivity.
        */
        binding.btnChooseNewSeed.setOnClickListener {
            val intent = Intent(requireContext(), com.example.myapplication.Seeds::class.java)
            startActivity(intent)
        }

        // TASK SYSTEM
        val repository = TaskRepository()
        val adapter = TaskAdapter(mutableListOf()) { task ->
            repository.toggleTask(task)
        }

        binding.taskRecyclerView.layoutManager =
            LinearLayoutManager(requireContext())
        binding.taskRecyclerView.adapter = adapter

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
    }

    /*Added by Lesley Del Cid:
      showSeedPickerPopup
      - Allows user to pick a new seed from Home page.
    */
    private fun showSeedPickerPopup() {

        val seeds = arrayOf(
            "Sunflower Seed",
            "Strawberry Seed",
            "Lavender Seed"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Choose a seed to grow")
            .setItems(seeds) { _, which ->

                val chosenSeed = seeds[which]
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@setItems
                val userRef = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)

                val updates = hashMapOf<String, Any>(
                    "currentSeedId" to chosenSeed,
                    "plantProgress" to 0,
                    "plantCompleted" to false,
                    "plantStage" to "dirt",
                    "plantSubmits" to 0,

                    // Added by Lesley Del Cid:
                    // Reset bloomReached so global bloom popup can happen again next cycle
                    "bloomReached" to false
                )

                userRef.update(updates)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        goalsListener?.remove()
        goalsListener = null
        _binding = null
    }
}