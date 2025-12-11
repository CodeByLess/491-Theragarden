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
import com.example.myapplication.Profile
import com.example.myapplication.R
import com.example.myapplication.Seeds
import com.example.myapplication.Share
import com.example.myapplication.databinding.FragmentDashboardBinding
import com.example.myapplication.databinding.FragmentGardenBinding
import com.example.myapplication.ui.dashboard.DashboardViewModel

class garden : Fragment() {

    private var _binding: FragmentGardenBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val GardenViewModel=
            ViewModelProvider(this).get(GardenViewModel::class.java)

        _binding = FragmentGardenBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textGarden
        GardenViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSeeds.setOnClickListener {
            val intent = Intent(requireContext(), Seeds::class.java)
            startActivity(intent)

        }
        binding.btnShare.setOnClickListener {
            val intent = Intent(requireContext(), Share::class.java)
            startActivity(intent)

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}