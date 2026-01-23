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

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
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
        _binding = null
    }
}