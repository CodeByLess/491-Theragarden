package com.example.myapplication

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.example.myapplication.databinding.FragmentQuizQuestionBinding
import com.google.android.material.card.MaterialCardView


class QuizQuestionFragment : Fragment() {

    private lateinit var binding: FragmentQuizQuestionBinding
    private val cardViews = mutableListOf<MaterialCardView>()
    private var selectedAnswer: QuizAnswer? = null
    private var onAnswerSelected: ((Int, QuizAnswer) -> Unit)? = null

    companion object {
        private const val ARG_INDEX = "index"

        fun newInstance(
            index: Int,
            onAnswerSelected: (Int, QuizAnswer) -> Unit
        ): QuizQuestionFragment {
            return QuizQuestionFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_INDEX, index)
                }
                this.onAnswerSelected = onAnswerSelected
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentQuizQuestionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get the index and look up the question from the pool
        val index = arguments?.getInt(ARG_INDEX) ?: 0
        val question = QuizTaskPool.questions[index]

        // Set the question text
        binding.tvQuestion.text = question.question

        // Build an answer card for each answer
        question.answers.forEach { answer ->
            val card = buildAnswerCard(answer, index)
            binding.answersContainer.addView(card)
            cardViews.add(card)
        }
    }
    private fun buildAnswerCard(answer: QuizAnswer, questionIndex: Int): MaterialCardView {
        // Create the card
        val card = MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dpToPx(12) }
            radius = dpToPx(12).toFloat()
            cardElevation = 0f
            strokeWidth = dpToPx(2)
            strokeColor = Color.parseColor("#E0E0E0")
            setCardBackgroundColor(Color.WHITE)
        }
        // Inner layout to hold emoji and text
        val innerLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            gravity = Gravity.CENTER_VERTICAL
        }

        // Emoji
        val emojiView = TextView(requireContext()).apply {
            text = answer.emoji
            textSize = 24f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.marginEnd = dpToPx(12) }
        }

        // Answer text
        val answerText = TextView(requireContext()).apply {
            text = answer.text
            textSize = 16f
            setTextColor(Color.parseColor("#212121"))
        }

        innerLayout.addView(emojiView)
        innerLayout.addView(answerText)
        card.addView(innerLayout)

        // Handle tap
        card.setOnClickListener {
            selectCard(card, answer, questionIndex)
        }

        return card
    }

    private fun selectCard(
        selectedCard: MaterialCardView,
        answer: QuizAnswer,
        questionIndex: Int
    ) {
        // Reset all cards to unselected state
        cardViews.forEach {
            it.strokeColor = Color.parseColor("#E0E0E0")
            it.setCardBackgroundColor(Color.WHITE)
        }

        // Highlight the selected card
        selectedCard.strokeColor = Color.parseColor("#4CAF50")
        selectedCard.setCardBackgroundColor(Color.parseColor("#F1F8F1"))

        // Store the answer and notify the bottom sheet
        selectedAnswer = answer
        onAnswerSelected?.invoke(questionIndex, answer)
    }

    // Called by QuizBottomSheet before allowing Next
    fun hasAnswerSelected(): Boolean = selectedAnswer != null

    // Helper to convert dp to px
    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()
}