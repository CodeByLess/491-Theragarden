package com.example.myapplication

object TaskRecommendationEngine {

    fun recommendTasks(answers: Map<Int, String>): List<String> {
        val mood = answers[0]      // "low_mood" / "neutral" / "good_mood"
        // answers[1] is energy — ignored for now
        val struggle = answers[2]  // "anxiety" / "motivation" / "sleep" / "stress"

        // Struggle is the primary signal
        val primaryPool = when (struggle) {
            "anxiety"    -> QuizTaskPool.anxietyTasks
            "motivation" -> QuizTaskPool.motivationTasks
            "sleep"      -> QuizTaskPool.sleepTasks
            "stress"     -> QuizTaskPool.generalTasks
            else         -> QuizTaskPool.generalTasks
        }

        // Low mood adds extra tasks from the low mood pool
        val secondaryPool = when (mood) {
            "low_mood" -> QuizTaskPool.lowMoodTasks
            else       -> emptyList()
        }

        // Combine, remove duplicates, shuffle, return 5
        return (primaryPool + secondaryPool)
            .distinct()
            .shuffled()
            .take(5)
    }
}