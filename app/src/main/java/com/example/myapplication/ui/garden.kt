package com.example.myapplication.ui

import android.content.Intent
import androidx.fragment.app.viewModels
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager // added by Les
import androidx.recyclerview.widget.ItemTouchHelper // added by Les
import androidx.recyclerview.widget.RecyclerView // added by Les
import com.example.myapplication.Profile
import com.example.myapplication.R
import com.example.myapplication.Seeds
import com.example.myapplication.Share
import com.example.myapplication.databinding.FragmentDashboardBinding
import com.example.myapplication.databinding.FragmentGardenBinding
import com.example.myapplication.ui.dashboard.DashboardViewModel
import com.google.firebase.auth.FirebaseAuth // added by Les
import com.google.firebase.firestore.FirebaseFirestore // added by Les
import com.google.firebase.firestore.WriteBatch // added by Les

class garden : Fragment() {

    private var _binding: FragmentGardenBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    // list that will store all plants from Firestore // added by Lesley
    private val gardenPlantList = mutableListOf<GardenPlant>() // added by Lesley

    // adapter for RecyclerView // added by Les
    private lateinit var gardenAdapter: GardenAdapter // added by Lesley

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val GardenViewModel=
            ViewModelProvider(this).get(GardenViewModel::class.java)

        _binding = FragmentGardenBinding.inflate(inflater, container, false)
        val root: View = binding.root


        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // setup RecyclerView as grid (2 columns) // added by Lesley
        gardenAdapter = GardenAdapter(gardenPlantList) // added by Lesley
        binding.recyclerGarden.layoutManager = GridLayoutManager(requireContext(), 2) // added by Les
        binding.recyclerGarden.adapter = gardenAdapter // added by Les

        // enables press-and-hold drag reordering in the garden grid // added by Lesley
        enableDragAndDrop() // added by Les

        binding.btnSeeds.setOnClickListener {
            val intent = Intent(requireContext(), Seeds::class.java)
            startActivity(intent)
        }

        binding.btnShare.setOnClickListener {
            val intent = Intent(requireContext(), Share::class.java)
            startActivity(intent)
        }

        loadGardenPlants() // load data from Firestore // added by Lesley
    }

    // function to get all saved plants from Firestore // added by Lesley
    private fun loadGardenPlants() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("garden")
            .orderBy("order") // added by Les
            .get()
            .addOnSuccessListener { documents ->

                gardenPlantList.clear() // clear old list // added by Lesley

                for (document in documents) {
                    val seedName = document.getString("seedName") ?: "Unknown Plant"

                    // match plant name to correct image // added by Lesley
                    val imageResId = getPlantImage(seedName)

                    gardenPlantList.add(
                        GardenPlant(
                            documentId = document.id, // added by Lesley
                            seedName = seedName,
                            imageResId = imageResId,
                            order = document.getLong("order")?.toInt() ?: 0 // added by Lesley
                        )
                    )
                }

                gardenAdapter.notifyDataSetChanged() // refresh UI // added by Lesley
            }
    }

    // maps plant name to drawable image // added by Lesley
    private fun getPlantImage(seedName: String): Int {
        return when (seedName) {
            "Sunflower Seed" -> R.drawable.sunflower
            "Strawberry Seed" -> R.drawable.strawberry
            "Lavender Seed" -> R.drawable.lavender
            "Tulip Seed" -> R.drawable.tulip
            "Cactus Seed" -> R.drawable.cactus
            "Monstera Seed" -> R.drawable.monstera
            else -> R.drawable.dirt
        }
    }

    // attaches drag-and-drop behavior to the RecyclerView // added by Lesley
    private fun enableDragAndDrop() {
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN or
                    ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPosition = viewHolder.adapterPosition
                val toPosition = target.adapterPosition

                gardenAdapter.moveItem(fromPosition, toPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                // no swipe action // added by Lesley
            }

            override fun isLongPressDragEnabled(): Boolean {
                return true
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {
                super.clearView(recyclerView, viewHolder)

                // save new plant order after the drag finishes // added by Lesley
                saveGardenOrder()
            }
        }

        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(binding.recyclerGarden)
    }

    // saves the new dragged order back to Firestore // added by Lesley
    private fun saveGardenOrder() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val batch: WriteBatch = db.batch()

        gardenPlantList.forEachIndexed { index, plant ->
            if (plant.documentId.isNotBlank()) {
                val docRef = db.collection("users")
                    .document(uid)
                    .collection("garden")
                    .document(plant.documentId)

                batch.update(docRef, "order", index)
            }
        }

        batch.commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}