package com.example.progetto_tosa.ui.workout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.progetto_tosa.databinding.FragmentWorkoutBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.progetto_tosa.R
import androidx.navigation.fragment.findNavController

class WorkoutFragment : Fragment() {

    private var _binding: FragmentWorkoutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val workoutViewModel = ViewModelProvider(this)[WorkoutViewModel::class.java]

        _binding = FragmentWorkoutBinding.inflate(inflater, container, false)
        val root = binding.root

        // Navigazione ai fragment
        binding.btnItem1.setOnClickListener {
            findNavController().navigate(R.id.action_workout_to_bodybuilding)
        }

        binding.btnItem2.setOnClickListener {
            findNavController().navigate(R.id.action_workout_to_corpolibero)
        }

        binding.btnItem3.setOnClickListener {
            findNavController().navigate(R.id.action_workout_to_cardio)
        }

        binding.btnItem4.setOnClickListener {
            findNavController().navigate(R.id.action_workout_to_stretching)
        }

        // Immagini animate
        val imagesCorpoLibero = arrayOf(
            R.drawable.stretch,
            R.drawable.crunches,
            R.drawable.plank,
            R.drawable.no_so_esercizio
        )
        val imagesPesi = arrayOf(
            R.drawable.staccoooo,
            R.drawable.donna_qualcosa,
            R.drawable.military,
            R.drawable.bicips
        )

        var currentIndex = 0
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
