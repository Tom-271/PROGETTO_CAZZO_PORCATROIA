package com.example.progetto_tosa.ui.workout

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.FragmentWorkoutBinding
import java.text.SimpleDateFormat
import java.util.*

class WorkoutFragment : Fragment() {

    private var _binding: FragmentWorkoutBinding? = null
    private val binding get() = _binding!!

    private val selectedDate: String? by lazy {
        requireArguments().getString("selectedDate") // può essere null
    }
    private val selectedUser: String? by lazy {
        requireArguments().getString("selectedUser")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentWorkoutBinding.inflate(inflater, container, false)
        .also { _binding = it }
        .root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnItem1.setOnClickListener {
            val args = bundleOf().apply {
                selectedDate?.let { putString("selectedDate", it) }
                selectedUser?.let { putString("selectedUser", it) }
            }
            findNavController().navigate(
                R.id.action_fragment_workout_to_navigation_bodybuilding,
                args
            )
        }

        binding.btnItem2.setOnClickListener {
            val args = bundleOf().apply {
                selectedDate?.let { putString("selectedDate", it) }
                selectedUser?.let { putString("selectedUser", it) }
            }
            findNavController().navigate(
                R.id.action_fragment_workout_to_navigation_corpolibero,
                args
            )
        }

        binding.btnItem3.setOnClickListener {
            val args = bundleOf().apply {
                selectedDate?.let { putString("selectedDate", it) }
                selectedUser?.let { putString("selectedUser", it) }
            }
            findNavController().navigate(
                R.id.action_fragment_workout_to_navigation_cardio,
                args
            )
        }

        binding.btnItem4.setOnClickListener {
            val args = bundleOf().apply {
                selectedDate?.let { putString("selectedDate", it) }
                selectedUser?.let { putString("selectedUser", it) }
            }
            findNavController().navigate(
                R.id.action_fragment_workout_to_navigation_stretching,
                args
            )
        }
    }

    private fun getDayNameFromDate(dateString: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("EEEE", Locale("it", "IT"))
        val date = inputFormat.parse(dateString)
        return outputFormat.format(date!!).replaceFirstChar { it.uppercase() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
