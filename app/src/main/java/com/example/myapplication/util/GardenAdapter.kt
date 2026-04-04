package com.example.myapplication.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemGardenPlantBinding
import java.util.Collections // added by Les

// adapter to display garden plants in RecyclerView // added by Les
class GardenAdapter(
    private val plantList: MutableList<GardenPlant> // added by Les
) : RecyclerView.Adapter<GardenAdapter.GardenViewHolder>() {

    // view holder for each plant item // added by Les
    class GardenViewHolder(val binding: ItemGardenPlantBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GardenViewHolder {
        val binding = ItemGardenPlantBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GardenViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GardenViewHolder, position: Int) {
        val currentPlant = plantList[position]

        // set plant name and image // added by Les
        holder.binding.txtPlantName.text = currentPlant.seedName
        holder.binding.imgPlant.setImageResource(currentPlant.imageResId)
    }

    override fun getItemCount(): Int = plantList.size

    // moves a garden item to a new position after dragging // added by Les
    fun moveItem(fromPosition: Int, toPosition: Int) {
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(plantList, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(plantList, i, i - 1)
            }
        }
        notifyItemMoved(fromPosition, toPosition)
    }
}