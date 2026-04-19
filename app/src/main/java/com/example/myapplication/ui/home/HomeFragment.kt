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
import java.util.Calendar

class HomeFragment : Fragment() {

    // View binding reference for accessing UI elements
    private var _binding: FragmentHomeBinding? = null
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

        // Set greeting badge based on time of day
        setTimeBadge()

        val repository = TaskRepository()
        val rewardRepository = RewardRepository()

        // Navigate to profile page
        binding.btnProfile.setOnClickListener {
            val intent = Intent(requireContext(), Profile::class.java)
            startActivity(intent)
        }

        // Adapter for displaying tasks
        val adapter = TaskAdapter(mutableListOf()) { task ->
            repository.toggleTask(task) // Toggle completion state
        }

        binding.recyclerViewTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewTasks.adapter = adapter

        // Listen for task updates and refresh UI
        repository.listenToTasks { tasks ->
            adapter.updateTasks(tasks)
        }

        // Update reward progress based on completed tasks
        repository.listenToCompletedTaskCount { completedCount ->
            rewardRepository.updateRewardProgress(completedCount) { unlockedSeed ->
                if (!unlockedSeed.isNullOrBlank()) {
                    showRewardPopup("Task Reward Unlocked", "You unlocked: $unlockedSeed")
                }
            }
        }

        // Display bloom points currency
        rewardRepository.listenToBloomPoints { points ->
            binding.txtBloomPoints.text = "Bloom Points: $points"
        }

        // Add new task
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
        _binding = null
    }
}