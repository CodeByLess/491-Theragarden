package com.example.myapplication.ui.home

import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.Profile
import com.example.myapplication.QuizTaskPool
import com.example.myapplication.R
import com.example.myapplication.RewardRepository
import com.example.myapplication.SpinWheelActivity
import com.example.myapplication.Task
import com.example.myapplication.TaskAdapter
import com.example.myapplication.TaskRepository
import com.example.myapplication.TaskLimitManager
import com.example.myapplication.databinding.FragmentHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Calendar

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
    private val binding get() = _binding!!

    /*Added by Lesley Del Cid:
      goalsListener
      - Real-time Firestore listener attached to users/{uid}.
      - Monitors both:
          • completedGoals (Goal Tracker)
          • completedPlants (Plant Tracker)
          • PlantData fields (currentSeedId, plantProgress, plantCompleted)
    */
    private var goalsListener: ListenerRegistration? = null

    private lateinit var repository: TaskRepository
    private lateinit var adapter: TaskAdapter
    private lateinit var limitManager: TaskLimitManager
    private var currentTasks = mutableListOf<Task>()

    // Inflate the layout and initialize binding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this)[HomeViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textHome
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        return root
    }

    // Added by Lesley Del Cid:
    // Updates the custom vertical growth meter based on the plant's progress
    // percentage from Firestore. The fill grows from bottom to top.
    private fun updateVerticalPlantMeter(progress: Int) {
        val safeProgress = progress.coerceIn(0, 100)

        binding.plantMeterContainer.post {
            val totalHeight = binding.plantMeterContainer.height
            val topCapHeight = binding.plantMeterTopCap.height
            val usableHeight = totalHeight - topCapHeight

            val fillHeight = (usableHeight * (safeProgress / 100f)).toInt()

            val params = binding.plantMeterFill.layoutParams
            params.height = fillHeight
            binding.plantMeterFill.layoutParams = params
        }
    }

    // Main UI setup and logic
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = TaskRepository()
        limitManager = TaskLimitManager()
        val rewardRepository = RewardRepository()

        // Profile button
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
                binding.plantProgressBar.progress = plantProgress
                updateVerticalPlantMeter(plantProgress)

                val plantCompleted = snapshot?.getBoolean("plantCompleted") ?: false
                val plantStage = snapshot?.getString("plantStage") ?: "dirt"

                // Added by Lesley Del Cid: CURRENT PLANT DISPLAY
                binding.txtCurrentPlant.text =
                    if (currentSeedId.isBlank()) "Current plant: None"
                    else "Current plant: $currentSeedId"

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
                    if (plantCompleted) "Choose New Seed"
                    else "Pick a New Seed"
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
        // Added by Lesley:
        // The adapter now supports both checking tasks and deleting tasks.
        // Checking a task toggles its completion status.
        // Long pressing a task opens a confirmation dialog before deletion.
        adapter = TaskAdapter(
            mutableListOf(),
            { task ->
                repository.toggleTask(task)
            },
            { task ->
                // Added by Lesley:
                // Confirmation dialog prevents accidental deletion of tasks.
                androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Delete Task")
                    .setMessage("Are you sure you want to delete this task?")
                    .setPositiveButton("Yes") { _, _ ->
                        repository.deleteTask(task)
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        )

        binding.taskRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.taskRecyclerView.adapter = adapter

        repository.listenToTasks { tasks ->
            currentTasks = tasks.toMutableList()
            adapter.updateTasks(tasks)
        }

        binding.btnAdd.setOnClickListener {
            val taskText = binding.etTask.text.toString().trim()
            if (taskText.isNotEmpty()) {
                repository.addTask(taskText)
                binding.etTask.text.clear()
            }
        }

        // Opens Spin Wheel feature to unlock rare plants -paula
        binding.btnSpinWheel.setOnClickListener {
            val intent = Intent(requireContext(), SpinWheelActivity::class.java)
            startActivity(intent)
        }

        // Opens shop to purchase plants using Bloom Points -paula
        binding.btnOpenShop.setOnClickListener {
            showShopDialog(rewardRepository)
        }

        refreshLimitButtons()
        setupRegenButton()
        setupSwapButton()
        setupQuickTaskButton()
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

    // Deletes all incomplete tasks and adds 3 random ones
    private fun setupRegenButton() {
        binding.btnRegen.setOnClickListener {
            limitManager.useRegen(
                onSuccess = {
                    repository.deleteAllIncompleteTasks {
                        randomTasksFromPool(3).forEach { repository.addTask(it) }
                        refreshLimitButtons()
                        toast("Tasks regenerated! 🔄")
                    }
                },
                onLimitReached = { toast("No regenerations left for today") }
            )
        }
    }

    // Shows a picker of incomplete tasks, replaces the chosen one
    private fun setupSwapButton() {
        binding.btnSwap.setOnClickListener {
            val incomplete = currentTasks.filter { !it.completed }
            if (incomplete.isEmpty()) {
                toast("No incomplete tasks to swap")
                return@setOnClickListener
            }
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Which task to swap? 🔀")
                .setItems(incomplete.map { it.title }.toTypedArray()) { _, index ->
                    limitManager.useSwap(
                        onSuccess = {
                            repository.deleteTask(incomplete[index].id)
                            repository.addTask(randomTasksFromPool(1).first())
                            refreshLimitButtons()
                            toast("Task swapped!")
                        },
                        onLimitReached = { toast("No swaps left for today") }
                    )
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    // Instantly adds one random task from the general pool
    private fun setupQuickTaskButton() {
        binding.btnQuickTask.setOnClickListener {
            limitManager.useQuickTask(
                onSuccess = {
                    repository.addTask(QuizTaskPool.generalTasks.random())
                    refreshLimitButtons()
                    toast("Quick task added! ⚡")
                },
                onLimitReached = { toast("No quick tasks left for today") }
            )
        }
    }

    // Updates button labels and disables them when limit is 0
    private fun refreshLimitButtons() {
        limitManager.getLimits { regenLeft, swapLeft, quickLeft ->
            activity?.runOnUiThread {
                binding.btnRegen.text = "🔄 Regen ($regenLeft)"
                binding.btnSwap.text = "🔀 Swap ($swapLeft)"
                binding.btnQuickTask.text = "⚡ Quick ($quickLeft)"
                binding.btnRegen.isEnabled = regenLeft > 0
                binding.btnSwap.isEnabled = swapLeft > 0
                binding.btnQuickTask.isEnabled = quickLeft > 0
            }
        }
    }

    private fun randomTasksFromPool(count: Int): List<String> {
        return (QuizTaskPool.generalTasks + QuizTaskPool.motivationTasks +
                QuizTaskPool.lowMoodTasks + QuizTaskPool.anxietyTasks)
            .shuffled().take(count)
    }

    // Generic popup for rewards and shop messages
    private fun showRewardPopup(title: String, message: String) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    // Display shop items and handle purchases
    private fun showShopDialog(rewardRepository: RewardRepository) {
        val shopItems = rewardRepository.getShopSeeds().toList()

        val itemLabels = shopItems.map {
            "${it.first} - ${it.second} pts"
        }.toTypedArray()

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
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

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    // Prevent memory leaks by clearing binding
    override fun onDestroyView() {
        super.onDestroyView()
        goalsListener?.remove()
        goalsListener = null
        _binding = null
    }
}