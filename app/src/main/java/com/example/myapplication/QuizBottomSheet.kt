package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.viewpager2.widget.ViewPager2
import com.example.myapplication.databinding.FragmentQuizBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
class QuizBottomSheet : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentQuizBottomSheetBinding
    private lateinit var adapter: QuizPagerAdapter
    private val selectedAnswers = mutableMapOf<Int, String>()

    companion object {
        fun newInstance() = QuizBottomSheet()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentQuizBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBottomSheet()
        setupViewPager()
        setupButtons()
    }

    private fun setupBottomSheet() {
        // Make it fully expanded and not collapsible
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    private fun setupViewPager() {
        // Callback to collect answers as user selects them
        val onAnswerSelected: (Int, QuizAnswer) -> Unit = { questionIndex, answer ->
            selectedAnswers[questionIndex] = answer.value
            Log.d("QuizDebug", "Answer stored - question: $questionIndex value: ${answer.value}")
        }

        adapter = QuizPagerAdapter(this, onAnswerSelected)
        binding.viewPager.adapter = adapter

        // Disable swiping — user must use Next/Back buttons
        binding.viewPager.isUserInputEnabled = false

        // Update progress bar and buttons when page changes
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateProgress(position)
            }
        })

        // Set initial progress
        updateProgress(0)
    }

    private fun setupButtons() {
        binding.btnNext.setOnClickListener {
            val current = binding.viewPager.currentItem
            val currentFragment = getCurrentFragment()

            // Check if user has selected an answer
            if (currentFragment?.hasAnswerSelected() == false) {
                Toast.makeText(context, "Please select an answer to continue", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (current < QuizTaskPool.questions.size - 1) {
                // Go to next question
                binding.viewPager.currentItem = current + 1
            } else {
                // Last question — generate tasks
                Log.d("QuizDebug", "All answers collected: $selectedAnswers")
                generateAndShowTasks()
            }
        }

        binding.btnBack.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current > 0) {
                binding.viewPager.currentItem = current - 1
            }
        }

        binding.btnSkip.setOnClickListener {
            dismiss()
        }
    }

    private fun updateProgress(currentIndex: Int) {
        val total = QuizTaskPool.questions.size

        // Update progress bar
        val progress = ((currentIndex + 1).toFloat() / total * 100).toInt()
        binding.progressBar.progress = progress

        // Update step counter text
        binding.tvStepCounter.text = "Question ${currentIndex + 1} of $total"

        // Show or hide back button
        binding.btnBack.visibility = if (currentIndex == 0) View.GONE else View.VISIBLE

        // Change Next button text on last question
        binding.btnNext.text = if (currentIndex == total - 1) "Get My Tasks" else "Next"
    }

    private fun generateAndShowTasks() {
        // Make sure all questions are answered
        if (selectedAnswers.size < QuizTaskPool.questions.size) {
            Toast.makeText(context, "Please answer all questions", Toast.LENGTH_SHORT).show()
            return
        }

        // Get recommended tasks
        val recommendedTasks = TaskRecommendationEngine.recommendTasks(selectedAnswers)
        Log.d("QuizDebug", "Recommended tasks: $recommendedTasks")

        // Show results bottom sheet
        TaskResultsBottomSheet.newInstance(recommendedTasks)
            .show(parentFragmentManager, "TaskResults")

        // Close quiz
        dismiss()
    }

    // Gets the currently visible question fragment
    private fun getCurrentFragment(): QuizQuestionFragment? {
        val tag = "f${binding.viewPager.currentItem}"
        return childFragmentManager.findFragmentByTag(tag) as? QuizQuestionFragment
    }

    override fun onStart() {
        super.onStart()
        // Make the bottom sheet full width
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}