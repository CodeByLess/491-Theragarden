package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.myapplication.databinding.FragmentTaskResultsBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.recyclerview.widget.LinearLayoutManager

class TaskResultsBottomSheet : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentTaskResultsBinding
    private lateinit var suggestedTaskAdapter: SuggestedTaskAdapter
    private val taskRepository = TaskRepository()

    companion object {
        private const val ARG_TASKS = "tasks"

        fun newInstance(tasks: List<String>): TaskResultsBottomSheet {
            return TaskResultsBottomSheet().apply {
                arguments = Bundle().apply {
                    putStringArrayList(ARG_TASKS, ArrayList(tasks))
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTaskResultsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get the recommended tasks passed from QuizBottomSheet
        val tasks = arguments?.getStringArrayList(ARG_TASKS) ?: arrayListOf()
        Log.d("QuizDebug", "Tasks received in results sheet: $tasks")

        setupBottomSheet()
        setupRecyclerView(tasks)
        setupButtons(tasks)
    }

    private fun setupBottomSheet() {
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    private fun setupRecyclerView(tasks: List<String>) {
        suggestedTaskAdapter = SuggestedTaskAdapter(tasks)
        binding.rvSuggestedTasks.apply {
            adapter = suggestedTaskAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupButtons(tasks: List<String>) {
        binding.btnAddTasks.setOnClickListener {
            val selected = suggestedTaskAdapter.selectedTasks

            if (selected.isEmpty()) {
                Toast.makeText(context, "Please select at least one task", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save each selected task to Firebase using existing TaskRepository
            selected.forEach { taskTitle ->
                taskRepository.addTask(taskTitle)
                Log.d("QuizDebug", "Adding task to Firebase: $taskTitle")
            }

            Toast.makeText(context, "${selected.size} task(s) added to your list!", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        binding.btnRetakeQuiz.setOnClickListener {
            // Close results and reopen quiz
            dismiss()
            QuizBottomSheet.newInstance()
                .show(parentFragmentManager, "QuizBottomSheet")
        }

        binding.btnExit.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}