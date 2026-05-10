package com.example.myapplication

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class PuzzleActivity : AppCompatActivity() {

    private lateinit var grid: GridLayout
    private val tiles = mutableListOf<Int>() // numbers (0 = empty)
    private val buttons = mutableListOf<Button>() // buttons on screen

    // Added by Lesley Del Cid:
    // Helper that updates completedGoals, plant progress, and returns to Dashboard
    private lateinit var completionHelper: SelfCareCompletionHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_puzzle)

        // Added by Lesley Del Cid:
        // Initialize reusable self-care completion helper
        completionHelper = SelfCareCompletionHelper(this)

        grid = findViewById(R.id.grid)

        val btnBack = findViewById<Button>(R.id.btnBack)
        btnBack.setOnClickListener {
            finish() // go back
        }

        val btnReset = findViewById<Button>(R.id.btnReset)
        btnReset.setOnClickListener {
            setupGame() // reshuffle
        }

        setupGame()
    }

    private fun setupGame() {
        tiles.clear()
        tiles.addAll(listOf(1, 2, 3, 4, 5, 6, 7, 8, 0)) // 0 is empty space

        // shuffle until it's solvable
        do {
            tiles.shuffle()
        } while (!isSolvable())

        grid.removeAllViews()
        buttons.clear()

        for (i in tiles.indices) {
            val btn = Button(this)
            val value = tiles[i]

            btn.text = if (value == 0) "" else value.toString()
            btn.textSize = 24f
            btn.setBackgroundColor(if (value == 0) Color.TRANSPARENT else Color.WHITE)

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = 0
                columnSpec = GridLayout.spec(i % 3, 1f)
                rowSpec = GridLayout.spec(i / 3, 1f)
                setMargins(8, 8, 8, 8)
            }

            btn.layoutParams = params
            btn.gravity = Gravity.CENTER

            btn.setOnClickListener {
                moveTile(i) // try to move tile
            }

            buttons.add(btn)
            grid.addView(btn)
        }
    }

    private fun moveTile(index: Int) {
        val emptyIndex = tiles.indexOf(0)

        val row = index / 3
        val col = index % 3

        val emptyRow = emptyIndex / 3
        val emptyCol = emptyIndex % 3

        // only move if next to empty space
        val isAdjacent =
            (row == emptyRow && Math.abs(col - emptyCol) == 1) ||
                    (col == emptyCol && Math.abs(row - emptyRow) == 1)

        if (isAdjacent) {
            tiles[emptyIndex] = tiles[index]
            tiles[index] = 0

            updateUI()
            checkWin()
        }
    }

    private fun updateUI() {
        for (i in tiles.indices) {
            val value = tiles[i]
            val btn = buttons[i]

            btn.text = if (value == 0) "" else value.toString()
            btn.setBackgroundColor(if (value == 0) Color.TRANSPARENT else Color.WHITE)
        }
    }

    private fun checkWin() {
        val correct = listOf(1, 2, 3, 4, 5, 6, 7, 8, 0)

        if (tiles == correct) {
            Toast.makeText(this, "You solved it! 🎉", Toast.LENGTH_SHORT).show()

            // Added by Lesley Del Cid:
            // Solving the puzzle counts as completing a self-care activity.
            // This updates completedGoals, updates plant progress, and returns user to Dashboard.
            completionHelper.completeSelfCareActivity()
        }
    }

    private fun isSolvable(): Boolean {
        var inversions = 0
        val list = tiles.filter { it != 0 }

        for (i in list.indices) {
            for (j in i + 1 until list.size) {
                if (list[i] > list[j]) inversions++
            }
        }

        return inversions % 2 == 0 // even = solvable
    }
}