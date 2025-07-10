// Fragment per gestire la selezione del tipo di allenamento
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

    // Binding per accedere in modo sicuro alle view del layout fragment_workout
    private var _binding: FragmentWorkoutBinding? = null
    private val binding get() = _binding!!

    // Recupero lazy delle argomentazioni passate: data selezionata e utente selezionato (possono essere null)
    private val selectedDate: String? by lazy {
        requireArguments().getString("selectedDate")
    }
    private val selectedUser: String? by lazy {
        requireArguments().getString("selectedUser")
    }

    // Inflating del layout tramite view binding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = FragmentWorkoutBinding.inflate(inflater, container, false)
        .also { _binding = it }
        .root

    // Configurazione dei listener per i quattro pulsanti di selezione allenamento
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bodybuilding
        binding.btnItem1.setOnClickListener {
            // Preparo un bundle con eventuali argomenti da passare
            val args = bundleOf().apply {
                selectedDate?.let { putString("selectedDate", it) }
                selectedUser?.let { putString("selectedUser", it) }
            }
            // Navigo al fragment di allenamento bodybuilding
            findNavController().navigate(
                R.id.action_fragment_workout_to_navigation_bodybuilding,
                args
            )
        }

        // Corpo libero
        binding.btnItem2.setOnClickListener {
            val args = bundleOf().apply {
                selectedDate?.let { putString("selectedDate", it) }
                selectedUser?.let { putString("selectedUser", it) }
            }
            // Navigo al fragment di allenamento a corpo libero
            findNavController().navigate(
                R.id.action_fragment_workout_to_navigation_corpolibero,
                args
            )
        }

        // Cardio
        binding.btnItem3.setOnClickListener {
            val args = bundleOf().apply {
                selectedDate?.let { putString("selectedDate", it) }
                selectedUser?.let { putString("selectedUser", it) }
            }
            // Navigo al fragment di allenamento cardio
            findNavController().navigate(
                R.id.action_fragment_workout_to_navigation_cardio,
                args
            )
        }

        // Stretching
        binding.btnItem4.setOnClickListener {
            val args = bundleOf().apply {
                selectedDate?.let { putString("selectedDate", it) }
                selectedUser?.let { putString("selectedUser", it) }
            }
            // Navigo al fragment di stretching
            findNavController().navigate(
                R.id.action_fragment_workout_to_navigation_stretching,
                args
            )
        }
    }

    // Pulizia del binding per evitare memory leak
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
