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
      • Completed plants count
      • Current plant information
      • Plant progress bar
      • "Choose New Seed" button (when plant is complete)
      • Add a task
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
          • completedPlants (Plant Tracker)
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
          - Reads both goal progress and plant progress from the user's
            Firestore document in real time.
        */
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            binding.txtCompletedGoals.text = "Goals completed: 0"
            binding.txtCompletedPlants.text = "Plants completed: 0"
        } else {

            val userRef = FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)

            goalsListener = userRef.addSnapshotListener { snapshot, _ ->
                if (_binding == null) return@addSnapshotListener

                // Added by Lesley Del Cid: GOALS
                val count = snapshot?.getLong("completedGoals") ?: 0L
                binding.txtCompletedGoals.text = "Goals completed: $count"

                /*Added by Lesley Del Cid:
                  COMPLETED PLANTS
                  - Reads the user's lifetime total of completed plants.
                  - Defaults to 0 if the field does not exist yet.
                */
                val completedPlants = snapshot?.getLong("completedPlants") ?: 0L
                binding.txtCompletedPlants.text = "Plants completed: $completedPlants"

                // PLANT DATA
                val currentSeedId = snapshot?.getString("currentSeedId") ?: ""
                val plantProgress = snapshot?.getLong("plantProgress")?.toInt() ?: 0
                val plantCompleted = snapshot?.getBoolean("plantCompleted") ?: false
                val plantStage = snapshot?.getString("plantStage") ?: "dirt"

                // Added by Lesley Del Cid: CURRENT PLANT DISPLAY
                binding.txtCurrentPlant.text =
                    if (currentSeedId.isBlank())
                        "Current plant: None"
                    else
                        "Current plant: $currentSeedId"

                // Added by Lesley Del Cid: STAGE DISPLAY
                binding.txtPlantStage.text = "Stage: $plantStage"

                /*Added by Lesley Del Cid:
                  PLANT IMAGE DISPLAY
                  - Dirt and sprout use shared stage images.
                  - Bloom uses a seed-specific flower image depending on
                    the selected seed.
                */
                val imageRes = when (plantStage.lowercase()) {
                    "dirt" -> R.drawable.dirt
                    "sprout" -> R.drawable.sprout
                    "bloom" -> {
                        when (currentSeedId) {
                            "Sunflower Seed" -> R.drawable.sunflower
                            "Strawberry Seed" -> R.drawable.strawberry
                            "Lavender Seed" -> R.drawable.lavender
                            "Tulip Seed" -> R.drawable.tulip
                            "Cactus Seed" -> R.drawable.cactus
                            "Monstera Seed" -> R.drawable.monstera
                            else -> R.drawable.sprout
                        }
                    }
                    else -> R.drawable.dirt
                }

                binding.imgPlantStage.setImageResource(imageRes)

                // Added by Lesley Del Cid: PROGRESS BAR
                binding.plantProgressBar.progress = plantProgress

                /*Added by Lesley Del Cid:
                  SEED BUTTON VISIBILITY:
                  - User can pick a new seed when:
                      • plantStage is bloom OR
                      • plantCompleted is true
                */
                binding.btnChooseNewSeed.visibility =
                    if (plantCompleted || plantStage.lowercase() == "bloom") {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

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

    override fun onDestroyView() {
        super.onDestroyView()

        goalsListener?.remove()
        goalsListener = null
        _binding = null
    }
}