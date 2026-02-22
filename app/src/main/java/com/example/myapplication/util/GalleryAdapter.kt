package com.example.myapplication

// Android UI and RecyclerView imports
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

// File handling import
import java.io.File

/*
 * GalleryAdapter
 *
 * RecyclerView adapter responsible for displaying
 * image files inside the GalleryActivity.
 * Each image is displayed using item_image layout.
 */
class GalleryAdapter(
    private var images: List<File>,
    private val onDeleteRequested: (File) -> Unit,
    private val onImageSelected: (File) -> Unit
) : RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    /*
     * ViewHolder class holds reference to ImageView
     * for each grid item in the RecyclerView.
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageItem)
    }

    /*
     * Called when RecyclerView needs a new ViewHolder.
     * Inflates item_image layout.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image, parent, false)
        return ViewHolder(view)
    }

    // Returns total number of images
    override fun getItemCount(): Int = images.size

    /*
     * Binds image file to ImageView.
     * Converts File into URI for display.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = images[position]

        holder.imageView.setImageURI(Uri.fromFile(file))

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