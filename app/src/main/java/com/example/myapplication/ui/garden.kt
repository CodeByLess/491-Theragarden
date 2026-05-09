package com.example.myapplication.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.Seeds
import com.example.myapplication.Share
import com.example.myapplication.databinding.FragmentGardenBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch

class garden : Fragment() {

    private var _binding: FragmentGardenBinding? = null
    private val binding get() = _binding!!

    private val gardenPlantList = mutableListOf<GardenPlant>()
    private lateinit var gardenAdapter: GardenAdapter

    // Added by Lesley Del Cid:
    // Holds color values for each Garden theme option.
    data class GardenTheme(
        val name: String,
        val backgroundColor: Int,
        val cardColor: Int,
        val buttonColor: Int,
        val textColor: Int
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        ViewModelProvider(this).get(GardenViewModel::class.java)

        _binding = FragmentGardenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Added by Lesley Del Cid:
        // Sets up RecyclerView as a 2-column plant grid.
        gardenAdapter = GardenAdapter(gardenPlantList)

        binding.recyclerGarden.layoutManager =
            GridLayoutManager(requireContext(), 2)

        binding.recyclerGarden.adapter = gardenAdapter

        enableDragAndDrop()

        binding.btnSeeds.setOnClickListener {
            val intent = Intent(requireContext(), Seeds::class.java)
            startActivity(intent)
        }

        binding.btnShare.setOnClickListener {
            val intent = Intent(requireContext(), Share::class.java)
            startActivity(intent)
        }

        // Added by Lesley Del Cid:
        // Opens theme selection dialog.
        binding.btnTheme.setOnClickListener {
            showThemeDialog()
        }

        loadSavedTheme()
        loadGardenPlants()
    }

    // Added by Lesley Del Cid:
    // Shows preset theme options for the Garden screen.
    private fun showThemeDialog() {

        // Added by Lesley Del Cid:
        // Includes preset themes and a custom theme option.
        val themeNames = arrayOf(
            "Forest Theme",
            "Lavender Theme",
            "Sunset Theme",
            "Custom Theme"
        )

        val themeKeys = arrayOf(
            "forest",
            "lavender",
            "sunset",
            "custom"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Choose Garden Theme")

            .setItems(themeNames) { _, which ->

                val selectedThemeKey = themeKeys[which]

                // Added by Lesley Del Cid:
                // Opens custom color selection dialog.
                if (selectedThemeKey == "custom") {

                    showCustomThemeDialog()

                } else {

                    val selectedTheme =
                        getGardenTheme(selectedThemeKey)

                    applyGardenTheme(selectedTheme)

                    saveSelectedTheme(selectedThemeKey)
                }
            }
            .show()
    }

    // Added by Lesley Del Cid:
    // Allows users to create a custom Garden theme
    // by choosing their own background color.
    private fun showCustomThemeDialog() {

        val colorNames = arrayOf(
            "Pink",
            "Blue",
            "Purple",
            "Dark Green"
        )

        val colorValues = arrayOf(
            "#F8BBD0",
            "#BBDEFB",
            "#D1C4E9",
            "#355E3B"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Choose Background Color")

            .setItems(colorNames) { _, which ->

                val selectedColor =
                    Color.parseColor(colorValues[which])

                val customTheme = GardenTheme(
                    name = "custom",
                    backgroundColor = selectedColor,
                    cardColor = Color.WHITE,
                    buttonColor = Color.parseColor("#6B4BB8"),
                    textColor = Color.parseColor("#2B1B10")
                )

                applyGardenTheme(customTheme)

                // Added by Lesley Del Cid:
                // Saves custom theme colors to Firestore.
                saveCustomTheme(colorValues[which])
            }
            .show()
    }

    // Added by Lesley Del Cid:
    // Returns theme colors based on the selected theme name.
    private fun getGardenTheme(themeKey: String): GardenTheme {

        return when (themeKey) {

            "lavender" -> GardenTheme(
                name = "lavender",
                backgroundColor = Color.parseColor("#2B1B3A"),
                cardColor = Color.parseColor("#F3E8FF"),
                buttonColor = Color.parseColor("#8E7DBE"),
                textColor = Color.parseColor("#2B1B3A")
            )

            "sunset" -> GardenTheme(
                name = "sunset",
                backgroundColor = Color.parseColor("#3A1F12"),
                cardColor = Color.parseColor("#FFE0B2"),
                buttonColor = Color.parseColor("#D96C3B"),
                textColor = Color.parseColor("#2B1B10")
            )

            else -> GardenTheme(
                name = "forest",
                backgroundColor = Color.parseColor("#2B1B10"),
                cardColor = Color.parseColor("#FFFFFF"),
                buttonColor = Color.parseColor("#6B4BB8"),
                textColor = Color.parseColor("#2B1B10")
            )
        }
    }

    // Added by Lesley Del Cid:
    // Applies the selected theme colors to the Garden screen.
    private fun applyGardenTheme(theme: GardenTheme) {

        binding.gardenRoot.setBackgroundColor(theme.backgroundColor)

        binding.btnSeeds.backgroundTintList =
            ColorStateList.valueOf(theme.buttonColor)

        binding.btnShare.backgroundTintList =
            ColorStateList.valueOf(theme.buttonColor)

        binding.btnTheme.backgroundTintList =
            ColorStateList.valueOf(theme.buttonColor)

        binding.btnSeeds.setTextColor(Color.WHITE)
        binding.btnShare.setTextColor(Color.WHITE)
        binding.btnTheme.setTextColor(Color.WHITE)

        gardenAdapter.updateTheme(
            theme.cardColor,
            theme.textColor
        )
    }

    // Added by Lesley Del Cid:
    // Saves selected Garden theme to the user's Firestore document.
    private fun saveSelectedTheme(themeKey: String) {

        val uid =
            FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .set(
                mapOf(
                    "gardenTheme" to themeKey
                ),
                SetOptions.merge()
            )
    }

    // Added by Lesley Del Cid:
    // Saves custom theme background color to Firestore.
    private fun saveCustomTheme(backgroundColor: String) {

        val uid =
            FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .set(
                mapOf(
                    "gardenTheme" to "custom",
                    "customBackgroundColor" to backgroundColor
                ),
                SetOptions.merge()
            )
    }

    // Added by Lesley Del Cid:
    // Loads saved theme from Firestore and applies it when Garden opens.
    private fun loadSavedTheme() {

        val uid =
            FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()

            .addOnSuccessListener { document ->

                val savedTheme =
                    document.getString("gardenTheme")
                        ?: "forest"

                // Added by Lesley Del Cid:
                // Loads saved custom theme colors.
                if (savedTheme == "custom") {

                    val customColor =
                        document.getString("customBackgroundColor")
                            ?: "#2B1B10"

                    val customTheme = GardenTheme(
                        name = "custom",
                        backgroundColor =
                            Color.parseColor(customColor),

                        cardColor = Color.WHITE,

                        buttonColor =
                            Color.parseColor("#6B4BB8"),

                        textColor =
                            Color.parseColor("#2B1B10")
                    )

                    applyGardenTheme(customTheme)

                } else {

                    applyGardenTheme(
                        getGardenTheme(savedTheme)
                    )
                }
            }
    }

    // Added by Lesley Del Cid:
    // Gets all saved plants from Firestore
    // and displays them in the garden grid.
    private fun loadGardenPlants() {

        val uid =
            FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("garden")
            .orderBy("order")
            .get()

            .addOnSuccessListener { documents ->

                gardenPlantList.clear()

                for (document in documents) {

                    val seedName =
                        document.getString("seedName")
                            ?: "Unknown Plant"

                    val imageResId =
                        getPlantImage(seedName)

                    gardenPlantList.add(
                        GardenPlant(
                            documentId = document.id,
                            seedName = seedName,
                            imageResId = imageResId,
                            order =
                                document.getLong("order")
                                    ?.toInt() ?: 0
                        )
                    )
                }

                gardenAdapter.notifyDataSetChanged()
            }
    }

    // Added by Lesley Del Cid:
    // Maps plant names from Firestore to their drawable images.
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

    // Added by Lesley Del Cid:
    // Enables press-and-hold drag reordering in the garden grid.
    private fun enableDragAndDrop() {

        val callback = object : ItemTouchHelper.SimpleCallback(

            ItemTouchHelper.UP or
                    ItemTouchHelper.DOWN or
                    ItemTouchHelper.LEFT or
                    ItemTouchHelper.RIGHT,

            0
        ) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {

                val fromPosition =
                    viewHolder.adapterPosition

                val toPosition =
                    target.adapterPosition

                gardenAdapter.moveItem(
                    fromPosition,
                    toPosition
                )

                return true
            }

            override fun onSwiped(
                viewHolder: RecyclerView.ViewHolder,
                direction: Int
            ) {
                // No swipe action is needed for garden plants.
            }

            override fun isLongPressDragEnabled(): Boolean {
                return true
            }

            override fun clearView(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ) {

                super.clearView(recyclerView, viewHolder)

                saveGardenOrder()
            }
        }

        val itemTouchHelper =
            ItemTouchHelper(callback)

        itemTouchHelper.attachToRecyclerView(
            binding.recyclerGarden
        )
    }

    // Added by Lesley Del Cid:
    // Saves the new dragged plant order back to Firestore.
    private fun saveGardenOrder() {

        val uid =
            FirebaseAuth.getInstance().currentUser?.uid ?: return

        val db = FirebaseFirestore.getInstance()

        val batch: WriteBatch = db.batch()

        gardenPlantList.forEachIndexed { index, plant ->

            if (plant.documentId.isNotBlank()) {

                val docRef =
                    db.collection("users")
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