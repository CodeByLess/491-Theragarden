package com.example.myapplication

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class QuizPagerAdapter (
    fragment: Fragment,
    private val onAnswerSelected: (Int, QuizAnswer) -> Unit
) : FragmentStateAdapter(fragment) {
    // Automatically counts how many questions are in your pool
    override fun getItemCount() = QuizTaskPool.questions.size

    // Creates a new QuizQuestionFragment for each question
    override fun createFragment(position: Int): Fragment {
        return QuizQuestionFragment.newInstance(position, onAnswerSelected)
    }
}