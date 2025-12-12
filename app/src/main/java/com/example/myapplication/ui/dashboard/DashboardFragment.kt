package com.example.myapplication.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.Affirmations
import com.example.myapplication.Breathing
import com.example.myapplication.Doodle
import com.example.myapplication.Hydration
import com.example.myapplication.Journal
import com.example.myapplication.Links
import com.example.myapplication.Mood
import com.example.myapplication.Music
import com.example.myapplication.Photo
import com.example.myapplication.Quote
import com.example.myapplication.Seeds
import com.example.myapplication.Sleep
import com.example.myapplication.Stretches
import com.example.myapplication.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textDashboard
        dashboardViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLinks.setOnClickListener {
            val intent = Intent(requireContext(), Links::class.java)
            startActivity(intent)
        }
        binding.btnMood.setOnClickListener {
            val intent = Intent(requireContext(), Mood::class.java)
            startActivity(intent)
        }
        binding.btnSleep.setOnClickListener {
            val intent = Intent(requireContext(), Sleep::class.java)
            startActivity(intent)
        }
        binding.btnHydration.setOnClickListener {
            val intent = Intent(requireContext(), Hydration::class.java)
            startActivity(intent)
        }
        binding.btnBreathing.setOnClickListener {
            val intent = Intent(requireContext(), Breathing::class.java)
            startActivity(intent)
        }
        binding.btnStretches.setOnClickListener {
            val intent = Intent(requireContext(), Stretches::class.java)
            startActivity(intent)
        }
        binding.btnAffirmations.setOnClickListener {
            val intent = Intent(requireContext(), Affirmations::class.java)
            startActivity(intent)
        }
        binding.btnMusic.setOnClickListener {
            val intent = Intent(requireContext(), Music::class.java)
            startActivity(intent)
        }
        binding.btnDoodle.setOnClickListener {
            val intent = Intent(requireContext(), Doodle::class.java)
            startActivity(intent)
        }
        binding.btnPhoto.setOnClickListener {
            val intent = Intent(requireContext(), Photo::class.java)
            startActivity(intent)
        }
        binding.btnQuote.setOnClickListener {
            val intent = Intent(requireContext(), Quote::class.java)
            startActivity(intent)
        }
        binding.btnJournal.setOnClickListener {
            val intent = Intent(requireContext(), Journal::class.java)
            startActivity(intent)
        }



    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}