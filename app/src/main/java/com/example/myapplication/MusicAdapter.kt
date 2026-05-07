package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.TaskAdapter.TaskViewHolder


/** * Adapter for displaying a list of music tracks in a RecyclerView.
 * * Handles play/pause state, progress display, and user interactions.
 * * * @param musicList List of MusicTitle objects to display
 * * @param onPlayPauseClick Callback triggered when the play/pause button is clicked, passes the item position */
class MusicAdapter (
    private val musicList: List<MusicTitle>,
    private val onPlayPauseClick: (Int) -> Unit
) : RecyclerView.Adapter<MusicAdapter.MusicViewHolder>()
{
    /** * ViewHolder that holds references to the UI elements for each music item. * Avoids repeated findViewById calls during scrolling for better performance. */

    class MusicViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMusicName: TextView = view.findViewById(R.id.tvMusicName)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        val btnPlayPause: ImageButton = view.findViewById(R.id.btnPlayPause)

    }
    /** * Inflates the item layout and creates a new ViewHolder for each music item. */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_music, parent, false)
        return MusicViewHolder(view)
    }
    /** * Binds data from the music list to the ViewHolder at the given position. * Updates the UI based on whether the track is currently playing or paused. */

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        val music = musicList[position]
// Set the track name
        holder.tvMusicName.text = music.name
//show progress bar and buttons when music is playing
        if (music.isPlaying) {
            holder.progressBar.visibility = View.VISIBLE
            holder.progressBar.progress = music.progress
            holder.btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
        } else {
            holder.progressBar.visibility = View.GONE
            holder.btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
        }  // Hide progress bar and show play icon when track is paused

        holder.btnPlayPause.setOnClickListener {
            onPlayPauseClick(position)
        } // Notify the parent via callback when play/pause is clicked
    }

    override fun getItemCount() = musicList.size

}