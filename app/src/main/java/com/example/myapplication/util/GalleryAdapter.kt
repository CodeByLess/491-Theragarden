package com.example.myapplication

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class GalleryAdapter(
    private var images: List<File>,
    private val isFavorite: (File) -> Boolean,
    private val onFavoriteToggle: (File) -> Unit,
    private val onDeleteRequested: (File) -> Unit,
    private val onImageSelected: (File) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageItem)
        val favoriteButton: ImageButton = view.findViewById(R.id.btnFavorite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = images.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = images[position]

        holder.imageView.setImageURI(Uri.fromFile(file))

        val favoriteIcon = if (isFavorite(file)) {
            android.R.drawable.btn_star_big_on
        } else {
            android.R.drawable.btn_star_big_off
        }
        holder.favoriteButton.setImageResource(favoriteIcon)

        holder.favoriteButton.setOnClickListener {
            onFavoriteToggle(file)

            val updatedIcon = if (isFavorite(file)) {
                android.R.drawable.btn_star_big_on
            } else {
                android.R.drawable.btn_star_big_off
            }
            holder.favoriteButton.setImageResource(updatedIcon)
        }

        holder.itemView.setOnClickListener {
            onImageSelected(file)
        }

        holder.itemView.setOnLongClickListener {
            onDeleteRequested(file)
            true
        }
    }

    fun updateFiles(newImages: List<File>) {
        images = newImages
        notifyDataSetChanged()
    }
}