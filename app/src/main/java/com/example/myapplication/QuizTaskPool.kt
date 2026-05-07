package com.example.myapplication

object QuizTaskPool {
    // Task Pools
    val lowMoodTasks = listOf(
        "Take 5 deep breaths",
        "Write down 3 things you are grateful for",
        "Step outside for 5 minutes",
        "Drink a full glass of water",
        "Listen to a song that comforts you",
        "Give yourself permission to rest today",
        "Write a kind note to yourself",
        "Watch something that makes you laugh",
        "Do a gentle 5 minute stretch",
        "Light a candle or make your space cozy"
    )

    val anxietyTasks = listOf(
        "Try a 4-7-8 breathing exercise",
        "Write down what is worrying you",
        "Go for a short walk outside",
        "Do a 5 minute body scan meditation",
        "Call or text someone you trust",
        "Make a cup of herbal tea and sit quietly",
        "Write down 3 things in your control right now",
        "Do a quick tidy of your immediate space",
        "Watch something lighthearted for 15 minutes",
        "Put your phone down and sit outside for 10 minutes"
    )

    val motivationTasks = listOf(
        "Set one small goal for today",
        "Clean one small area of your space",
        "Reach out to a friend",
        "Write down one thing you are proud of",
        "Make your bed",
        "Write a to do list for today with just 3 things",
        "Play an upbeat playlist while you do a small task",
        "Take a shower and get dressed",
        "Do one thing you have been putting off for 5 minutes",
        "Reward yourself after completing one task"
    )

    val sleepTasks = listOf(
        "Avoid screens 30 minutes before bed",
        "Write a short journal entry before sleeping",
        "Do a 5 minute breathing exercise before bed",
        "Make a calming herbal tea",
        "Set a consistent bedtime alarm",
        "Dim your lights an hour before bed",
        "Write down any thoughts keeping you up",
        "Do a short guided sleep meditation",
        "Keep your phone outside the bedroom tonight",
        "Stretch gently before getting into bed"
    )

    val generalTasks = listOf(
        "Journal for 5 minutes",
        "Do 10 minutes of light exercise",
        "Prepare a healthy snack",
        "Take a short social media break",
        "Spend 10 minutes in nature",
        "Read something enjoyable for 15 minutes",
        "Drink more water today",
        "Check in with a friend or family member",
        "Do something creative for 10 minutes",
        "Write down your mood at the end of the day"
    )

    //Questions pool
    val questions = listOf(
        QuizQuestion(                               // index 0
            question = "How are you feeling today? 🌱",
            answers = listOf(
                QuizAnswer("😔", "Not great", "low_mood"),
                QuizAnswer("😐", "Just okay", "neutral"),
                QuizAnswer("😊", "Pretty good", "good_mood")
            )
        ),
        QuizQuestion(                               // index 1
            question = "How is your energy level?",
            answers = listOf(
                QuizAnswer("🪫", "Drained", "low_energy"),
                QuizAnswer("⚡", "Moderate", "medium_energy"),
                QuizAnswer("🔥", "High energy", "high_energy")
            )
        ),
        QuizQuestion(                               // index 2
            question = "What are you struggling with most?",
            answers = listOf(
                QuizAnswer("😰", "Anxiety", "anxiety"),
                QuizAnswer("😴", "Low motivation", "motivation"),
                QuizAnswer("💤", "Sleep", "sleep"),
                QuizAnswer("🌀", "General stress", "stress")
            )
        )
    )
}