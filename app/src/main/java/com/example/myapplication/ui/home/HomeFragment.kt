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
            repository.toggleTask(task)
        }

        binding.taskRecyclerView.layoutManager = LinearLayoutManager(requireContext())
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