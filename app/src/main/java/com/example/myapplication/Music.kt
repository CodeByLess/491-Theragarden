package com.example.myapplication

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager

// Music feature will display different music options to choose from, chosen song will
// play and display progress bar

class Music : AppCompatActivity() {


// Initialize lists and var

    private lateinit var rvMusicList: RecyclerView
    private lateinit var musicAdapter: MusicAdapter
    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingIndex: Int = -1

    // Added by Lesley Del Cid:
    // Helper that updates completedGoals, plant progress, and returns to Dashboard
    private lateinit var completionHelper: SelfCareCompletionHelper

    private val handler = Handler(Looper.getMainLooper())
    private var progressRunnable: Runnable? = null

    //list of music mp3 files
    private val musicList = mutableListOf(
        MusicTitle("Bird Chirping", R.raw.bird_chirping),
        MusicTitle("Ocean Waves", R.raw.ocean_waves),
        MusicTitle("Soft Piano", R.raw.soft_piano),
        MusicTitle("Uplifting", R.raw.uplifting),
        MusicTitle("White Noise", R.raw.white_noise),
        MusicTitle("Wind Chimes", R.raw.wind_chimes)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music)

        // Added by Lesley Del Cid:
        // Initialize reusable self-care completion helper
        completionHelper = SelfCareCompletionHelper(this)

        val backButton = findViewById<Button>(R.id.btnBack)
        rvMusicList = findViewById(R.id.rvMusicList)

        backButton.setOnClickListener {
            finish() // return to Home
        }

        musicAdapter = MusicAdapter(musicList) { position ->
            handlePlayPause(position)
        }

        rvMusicList.layoutManager = LinearLayoutManager(this)
        rvMusicList.adapter = musicAdapter

    }

    //handles the play and pause buttons-uses list and checks position
    private fun handlePlayPause(position: Int) {
        if (currentPlayingIndex == position) {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                musicList[position].isPlaying = false
                stopProgressUpdate()
            } else {
                mediaPlayer?.start()
                musicList[position].isPlaying = true
                startProgressUpdate(position)
            }
        } else {
            stopCurrentTrack()
            playTrack(position)
        }

        musicAdapter.notifyDataSetChanged()
    }

    //handles play, checks position and updates progress bar if play button is clicked
    private fun playTrack(position: Int) {
        try {
            mediaPlayer = MediaPlayer.create(this, musicList[position].audioResId)
            mediaPlayer?.start()
            musicList[position].isPlaying = true
            currentPlayingIndex = position

            startProgressUpdate(position)

            mediaPlayer?.setOnCompletionListener {
                musicList[position].isPlaying = false
                musicList[position].progress = 0
                currentPlayingIndex = -1
                musicAdapter.notifyItemChanged(position)

                // Added by Lesley Del Cid:
                // When the user finishes listening to a full track, count it as a completed self-care activity.
                // This updates completedGoals, updates plant progress, and returns user to Dashboard.
                completionHelper.completeSelfCareActivity()
            }

        } catch (e: Exception) {
            e.printStackTrace()

            android.widget.Toast.makeText(
                this,
                "Error playing audio: ${e.message}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    //Checks if pause button pressed and updates progress bar
    private fun stopCurrentTrack() {
        if (currentPlayingIndex >= 0) {
            musicList[currentPlayingIndex].isPlaying = false
            musicList[currentPlayingIndex].progress = 0
        }

        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        stopProgressUpdate()
    }

    //changes the position of the progress bar
    private fun startProgressUpdate(position: Int) {
        progressRunnable = object : Runnable {
            override fun run() {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        val progress = (player.currentPosition * 100) / player.duration
                        musicList[position].progress = progress
                        musicAdapter.notifyItemChanged(position)
                        handler.postDelayed(this, 100)
                    }
                }
            }
        }

        handler.post(progressRunnable!!)
    }

    private fun stopProgressUpdate() {
        progressRunnable?.let { handler.removeCallbacks(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCurrentTrack()
    }
}