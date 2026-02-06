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

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private var goalsListener: ListenerRegistration? = null

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

        // completed Goals
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            binding.txtCompletedGoals.text = "Goals completed: 0"
        } else {
            val userRef = FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)

            goalsListener = userRef.addSnapshotListener { snapshot, _ ->
                if (_binding == null) return@addSnapshotListener

                val count = snapshot?.getLong("completedGoals") ?: 0L
                binding.txtCompletedGoals.text = "Goals completed: $count"
            }
        }
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
        goalsListener?.remove()
        goalsListener = null
        _binding = null
    }
}