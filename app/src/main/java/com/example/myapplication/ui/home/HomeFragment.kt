package com.example.myapplication.ui.home

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.Profile
import com.example.myapplication.QuizTaskPool
import com.example.myapplication.Task
import com.example.myapplication.TaskAdapter
import com.example.myapplication.TaskRepository
import com.example.myapplication.TaskLimitManager
import com.example.myapplication.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var repository: TaskRepository
    private lateinit var adapter: TaskAdapter
    private lateinit var limitManager: TaskLimitManager
    private var currentTasks = mutableListOf<Task>()

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
        repository = TaskRepository()
        limitManager = TaskLimitManager()


        // Profile button stays the same
        binding.btnProfile.setOnClickListener {
            val intent = Intent(requireContext(), Profile::class.java)
            startActivity(intent)
        }

        adapter = TaskAdapter(mutableListOf()) { task ->
            repository.toggleTask(task)
        }

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

        refreshLimitButtons()
        setupRegenButton()
        setupSwapButton()
        setupQuickTaskButton()
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
            AlertDialog.Builder(requireContext())
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
    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}