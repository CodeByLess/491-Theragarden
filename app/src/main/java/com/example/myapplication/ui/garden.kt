package com.example.myapplication.ui

import android.os.Environment
import android.content.ContentValues
import android.provider.MediaStore
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.Seeds
import com.example.myapplication.databinding.FragmentGardenBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.WriteBatch
import java.io.File
import java.io.FileOutputStream

class garden : Fragment() {

    private var _binding: FragmentGardenBinding? = null
    private val binding get() = _binding!!

    // Added by Lesley Del Cid:
    // Stores all plants currently displayed in the garden.
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

        _binding = FragmentGardenBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {

        super.onViewCreated(view, savedInstanceState)

        // Added by Lesley Del Cid:
        // Sets up RecyclerView as a 2-column plant grid.
        gardenAdapter = GardenAdapter(gardenPlantList)

        binding.recyclerGarden.layoutManager =
            GridLayoutManager(requireContext(), 2)

        binding.recyclerGarden.adapter =
            gardenAdapter

        // Added by Lesley Del Cid:
        // Enables drag-and-drop rearranging.
        enableDragAndDrop()

        // Added by Lesley Del Cid:
        // Opens Seeds activity.
        binding.btnSeeds.setOnClickListener {

            val intent =
                Intent(requireContext(), Seeds::class.java)

            startActivity(intent)
        }

// Added by Al Jayson Mendoza:
// Opens menu for sharing or saving garden image.
        binding.btnShare.setOnClickListener {

            val options = arrayOf(
                "Share Garden",
                "Save to Downloads"
            )

            AlertDialog.Builder(requireContext())
                .setTitle("Garden Options")

                .setItems(options) { _, which ->

                    when (which) {

                        0 -> {
                            shareGarden(binding.recyclerGarden)
                        }

                        1 -> {
                            saveGardenToFiles(binding.recyclerGarden)
                        }
                    }
                }
                .show()
        }

        // Added by Lesley Del Cid:
        // Opens Garden theme selector.
        binding.btnTheme.setOnClickListener {

            showThemeDialog()
        }

        // Added by Lesley Del Cid:
        // Loads saved user theme.
        loadSavedTheme()

        // Added by Lesley Del Cid:
        // Loads saved plants from Firestore.
        loadGardenPlants()
    }

    // Added by Al Jayson Mendoza:
    // Creates shareable image and opens Android share menu.
    private fun shareGarden(view: View) {

        try {

            val bitmap =
                createGardenBitmap(view)

            // Added by Al Jayson Mendoza:
            // Creates temporary cache folder.
            val imagesFolder =
                File(requireContext().cacheDir, "images")

            imagesFolder.mkdirs()

            val file =
                File(imagesFolder, "garden.png")

            FileOutputStream(file).use { stream ->

                bitmap.compress(
                    Bitmap.CompressFormat.PNG,
                    100,
                    stream
                )
            }

            // Added by Al Jayson Mendoza:
            // Creates secure FileProvider URI.
            val uri =
                FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.provider",
                    file
                )

            // Added by Al Jayson Mendoza:
            // Opens Android share menu.
            val shareIntent =
                Intent(Intent.ACTION_SEND).apply {

                    type = "image/png"

                    putExtra(
                        Intent.EXTRA_STREAM,
                        uri
                    )

                    putExtra(
                        Intent.EXTRA_TEXT,
                        "Check out my Theragarden™"
                    )

                    addFlags(
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }

            startActivity(
                Intent.createChooser(
                    shareIntent,
                    "Share your garden"
                )
            )

        } catch (e: Exception) {

            Toast.makeText(
                requireContext(),
                "Share failed: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Added by Al Jayson Mendoza:
    // Converts RecyclerView into bitmap image.
    private fun createGardenBitmap(view: View): Bitmap {

        val bitmap =
            Bitmap.createBitmap(
                view.width,
                view.height,
                Bitmap.Config.ARGB_8888
            )

        val canvas = Canvas(bitmap)

        view.draw(canvas)

        // Added by Al Jayson Mendoza:
        // Adds Theragarden watermark.
        addWatermark(canvas, bitmap)

        return bitmap
    }

    // Added by Al Jayson Mendoza:
    // Draws watermark on exported image.
    private fun addWatermark(
        canvas: Canvas,
        bitmap: Bitmap
    ) {

        val text = "Theragarden™"

        val textPaint = Paint().apply {

            color = Color.WHITE

            textSize = 32f

            alpha = 180

            isAntiAlias = true
        }

        val x = 20f
        val y = bitmap.height - 30f

        canvas.drawText(
            text,
            x,
            y,
            textPaint
        )
    }
    // Added by Al Jayson Mendoza:
    // Saves garden image to Downloads folder.
    private fun saveGardenToFiles(view: View) {
        try {
            val bitmap = createGardenBitmap(view)
            val filename = "theragarden_${System.currentTimeMillis()}.png"

            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, filename)
                put(MediaStore.Downloads.MIME_TYPE, "image/png")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val resolver = requireContext().contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)

            if (uri != null) {
                resolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }

                Toast.makeText(
                    requireContext(),
                    "Saved to Downloads",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Save failed",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Save failed: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    // Added by Lesley Del Cid:
    // Shows list of preset themes.
    private fun showThemeDialog() {

        val themeNames = arrayOf(
            "Forest Theme",
            "Lavender Theme",
            "Sunset Theme",
            "Mix & Match Theme"
        )

        val themeKeys = arrayOf(
            "forest",
            "lavender",
            "sunset",
            "mix"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Choose Garden Theme")

            .setItems(themeNames) { _, which ->

                val selectedThemeKey =
                    themeKeys[which]

                // Added by Lesley Del Cid:
                // Opens preset mix-and-match theme builder.
                if (selectedThemeKey == "mix") {

                    showMixAndMatchThemeDialog()
                    return@setItems
                }

                val selectedTheme =
                    getGardenTheme(selectedThemeKey)

                applyGardenTheme(selectedTheme)

                saveSelectedTheme(selectedThemeKey)
            }
            .show()
    }

    // Added by Lesley Del Cid:
    // Lets the user mix preset background, card, and button colors.
    private fun showMixAndMatchThemeDialog() {

        val backgroundNames = arrayOf(
            "Forest Brown",
            "Lavender Purple",
            "Sunset Brown"
        )

        val backgroundColors = arrayOf(
            "#2B1B10",
            "#2B1B3A",
            "#3A1F12"
        )

        val cardNames = arrayOf(
            "White",
            "Soft Pink",
            "Cream"
        )

        val cardColors = arrayOf(
            "#FFFFFF",
            "#F8BBD0",
            "#FFE0B2"
        )

        val buttonNames = arrayOf(
            "Purple",
            "Green",
            "Orange"
        )

        val buttonColors = arrayOf(
            "#6B4BB8",
            "#455C34",
            "#D96C3B"
        )

        var selectedBackground = backgroundColors[0]
        var selectedCard = cardColors[0]
        var selectedButton = buttonColors[0]

        AlertDialog.Builder(requireContext())
            .setTitle("Choose Background Color")

            .setItems(backgroundNames) { _, backgroundIndex ->

                selectedBackground =
                    backgroundColors[backgroundIndex]

                AlertDialog.Builder(requireContext())
                    .setTitle("Choose Plant Card Color")

                    .setItems(cardNames) { _, cardIndex ->

                        selectedCard =
                            cardColors[cardIndex]

                        AlertDialog.Builder(requireContext())
                            .setTitle("Choose Button Color")

                            .setItems(buttonNames) { _, buttonIndex ->

                                selectedButton =
                                    buttonColors[buttonIndex]

                                val textColor =
                                    "#2B1B10"

                                val mixedTheme = GardenTheme(
                                    name = "mix",
                                    backgroundColor =
                                        Color.parseColor(selectedBackground),

                                    cardColor =
                                        Color.parseColor(selectedCard),

                                    buttonColor =
                                        Color.parseColor(selectedButton),

                                    textColor =
                                        Color.parseColor(textColor)
                                )

                                applyGardenTheme(mixedTheme)

                                saveMixedTheme(
                                    selectedBackground,
                                    selectedCard,
                                    selectedButton,
                                    textColor
                                )
                            }
                            .show()
                    }
                    .show()
            }
            .show()
    }

    // Added by Lesley Del Cid:
    // Returns theme colors based on selected theme name.
    private fun getGardenTheme(
        themeKey: String
    ): GardenTheme {

        return when (themeKey) {

            "lavender" -> GardenTheme(
                name = "lavender",
                backgroundColor =
                    Color.parseColor("#2B1B3A"),

                cardColor =
                    Color.parseColor("#F3E8FF"),

                buttonColor =
                    Color.parseColor("#8E7DBE"),

                textColor =
                    Color.parseColor("#2B1B3A")
            )

            "sunset" -> GardenTheme(
                name = "sunset",
                backgroundColor =
                    Color.parseColor("#3A1F12"),

                cardColor =
                    Color.parseColor("#FFE0B2"),

                buttonColor =
                    Color.parseColor("#D96C3B"),

                textColor =
                    Color.parseColor("#2B1B10")
            )

            else -> GardenTheme(
                name = "forest",
                backgroundColor =
                    Color.parseColor("#2B1B10"),

                cardColor =
                    Color.parseColor("#FFFFFF"),

                buttonColor =
                    Color.parseColor("#6B4BB8"),

                textColor =
                    Color.parseColor("#2B1B10")
            )
        }
    }

    // Added by Lesley Del Cid:
    // Applies selected theme colors to Garden UI.
    private fun applyGardenTheme(
        theme: GardenTheme
    ) {

        binding.gardenRoot.setBackgroundColor(
            theme.backgroundColor
        )

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
    // Saves selected Garden theme to Firestore.
    private fun saveSelectedTheme(
        themeKey: String
    ) {

        val uid =
            FirebaseAuth.getInstance()
                .currentUser?.uid ?: return

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
    // Saves mixed theme colors to Firestore.
    private fun saveMixedTheme(
        backgroundHex: String,
        cardHex: String,
        buttonHex: String,
        textHex: String
    ) {

        val uid =
            FirebaseAuth.getInstance()
                .currentUser?.uid ?: return

        val mixedThemeData = mapOf(
            "gardenTheme" to "mix",
            "customBackgroundColor" to backgroundHex,
            "customCardColor" to cardHex,
            "customButtonColor" to buttonHex,
            "customTextColor" to textHex
        )

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .set(
                mixedThemeData,
                SetOptions.merge()
            )
    }

    // Added by Lesley Del Cid:
    // Loads saved theme from Firestore.
    private fun loadSavedTheme() {

        val uid =
            FirebaseAuth.getInstance()
                .currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()

            .addOnSuccessListener { document ->

                val savedTheme =
                    document.getString("gardenTheme")
                        ?: "forest"

                if (savedTheme == "mix") {

                    val backgroundHex =
                        document.getString("customBackgroundColor")
                            ?: "#2B1B10"

                    val cardHex =
                        document.getString("customCardColor")
                            ?: "#FFFFFF"

                    val buttonHex =
                        document.getString("customButtonColor")
                            ?: "#6B4BB8"

                    val textHex =
                        document.getString("customTextColor")
                            ?: "#2B1B10"

                    val mixedTheme = GardenTheme(
                        name = "mix",
                        backgroundColor =
                            Color.parseColor(backgroundHex),

                        cardColor =
                            Color.parseColor(cardHex),

                        buttonColor =
                            Color.parseColor(buttonHex),

                        textColor =
                            Color.parseColor(textHex)
                    )

                    applyGardenTheme(mixedTheme)

                } else {

                    applyGardenTheme(
                        getGardenTheme(savedTheme)
                    )
                }
            }
    }

    // Added by Lesley Del Cid:
    // Loads all saved plants from Firestore.
    private fun loadGardenPlants() {

        val uid =
            FirebaseAuth.getInstance()
                .currentUser?.uid ?: return

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
    // Matches plant names with drawable images.
    private fun getPlantImage(
        seedName: String
    ): Int {

        return when (seedName) {

            "Sunflower Seed" ->
                R.drawable.sunflower

            "Strawberry Seed" ->
                R.drawable.strawberry

            "Lavender Seed" ->
                R.drawable.lavender

            "Tulip Seed" ->
                R.drawable.tulip

            "Cactus Seed" ->
                R.drawable.cactus

            "Monstera Seed" ->
                R.drawable.monstera

            "Bonsai Tree" ->
                R.drawable.bonsai

            "Cherry Blossom" ->
                R.drawable.cherryblossoms

            "Palm Tree" ->
                R.drawable.palmtree

            "Venus Flytrap" ->
                R.drawable.venusflytrap

            "Trumpet Flower" ->
                R.drawable.trumpetflower

            "Blue Rose" ->
                R.drawable.bluerose

            "Crystal Lotus" ->
                R.drawable.crystallotus

            "Rainbow Tulip" ->
                R.drawable.rainbowtulip

            else ->
                R.drawable.dirt
        }
    }

    // Added by Lesley Del Cid:
    // Enables drag-and-drop plant rearranging.
    private fun enableDragAndDrop() {

        val callback =
            object : ItemTouchHelper.SimpleCallback(

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

                    // No swipe action needed.
                }

                override fun isLongPressDragEnabled(): Boolean {

                    return true
                }

                override fun clearView(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder
                ) {

                    super.clearView(
                        recyclerView,
                        viewHolder
                    )

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
    // Saves dragged plant order to Firestore.
    private fun saveGardenOrder() {

        val uid =
            FirebaseAuth.getInstance()
                .currentUser?.uid ?: return

        val db =
            FirebaseFirestore.getInstance()

        val batch: WriteBatch =
            db.batch()

        gardenPlantList.forEachIndexed { index, plant ->

            if (plant.documentId.isNotBlank()) {

                val docRef =
                    db.collection("users")
                        .document(uid)
                        .collection("garden")
                        .document(plant.documentId)

                batch.update(
                    docRef,
                    "order",
                    index
                )
            }
        }

        batch.commit()
    }

    override fun onDestroyView() {

        super.onDestroyView()

        _binding = null
    }
}