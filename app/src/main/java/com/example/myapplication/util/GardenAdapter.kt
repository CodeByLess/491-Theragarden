package com.example.myapplication.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemGardenPlantBinding
import java.util.Collections

// Added by Lesley Del Cid
class GardenAdapter(
    private val plantList: MutableList<GardenPlant>
) : RecyclerView.Adapter<GardenAdapter.GardenViewHolder>() {

    // Added by Lesley Del Cid:
    // Stores the current card and text colors for the Garden theme.
    private var cardColor: Int = Color.WHITE
    private var textColor: Int = Color.parseColor("#2B1B10")

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

        holder.binding.txtPlantName.text = currentPlant.seedName
        holder.binding.imgPlant.setImageResource(currentPlant.imageResId)

        // Added by Lesley Del Cid:
        // Applies the selected theme colors to each plant card.
        holder.binding.root.setCardBackgroundColor(cardColor)
        holder.binding.txtPlantName.setTextColor(textColor)
    }

    override fun getItemCount(): Int = plantList.size

    // Added by Lesley Del Cid
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

    // Added by Lesley Del Cid
    fun updateTheme(cardColor: Int, textColor: Int) {
        this.cardColor = cardColor
        this.textColor = textColor
        notifyDataSetChanged()
    }
}