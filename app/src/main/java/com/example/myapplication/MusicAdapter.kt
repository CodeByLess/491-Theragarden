package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.TaskAdapter.TaskViewHolder

class MusicAdapter (
    private val musicList: List<MusicTitle>,
    private val onPlayPauseClick: (Int) -> Unit
) : RecyclerView.Adapter<MusicAdapter.MusicViewHolder>()
{
    class MusicViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMusicName: TextView = view.findViewById(R.id.tvMusicName)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        val btnPlayPause: ImageButton = view.findViewById(R.id.btnPlayPause)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_music, parent, false)
        return MusicViewHolder(view)
    }

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        val music = musicList[position]

        holder.tvMusicName.text = music.name

        if (music.isPlaying) {
            holder.progressBar.visibility = View.VISIBLE
            holder.progressBar.progress = music.progress
            holder.btnPlayPause.setImageResource(android.R.drawable.ic_media_pause)
        } else {
            holder.progressBar.visibility = View.GONE
            holder.btnPlayPause.setImageResource(android.R.drawable.ic_media_play)
        }

        holder.btnPlayPause.setOnClickListener {
            onPlayPauseClick(position)
        }
    }

    override fun getItemCount() = musicList.size

}