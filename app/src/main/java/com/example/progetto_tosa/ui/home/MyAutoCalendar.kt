package com.example.progetto_tosa.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.FragmentMyautocalendarBinding

class MyAutoCalendar : Fragment() {

    private var _binding: FragmentMyautocalendarBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MyAutoCalendarViewModel

    // Memorizza la data selezionata per la navigazione
    private var selectedDateId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyautocalendarBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(MyAutoCalendarViewModel::class.java)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Pulsanti di navigazione settimana
        binding.btnPrevWeek.setOnClickListener {
            viewModel.onPrevWeek()
            resetSelection()
        }
        binding.btnNextWeek.setOnClickListener {
            viewModel.onNextWeek()
            resetSelection()
        }

        // Lista degli ID dei toggle button (ordine L-...-D)
        val dayBtns = listOf(
            binding.btnMonday.id,
            binding.btnTuesday.id,
            binding.btnWednesday.id,
            binding.btnThursday.id,
            binding.btnFriday.id,
            binding.btnSaturday.id,
            binding.btnSunday.id
        )

        // Listener sul toggle group: mostra sempre il btnFancy ogni volta che si seleziona un giorno
        binding.daysToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked && checkedId != View.NO_ID) {
                val idx = dayBtns.indexOf(checkedId)
                selectedDateId = viewModel.dateIds.value?.getOrNull(idx)
                binding.btnFancy.visibility = View.VISIBLE
            }
        }

        // Click su btnFancy: naviga passando il bundle
        binding.btnFancy.setOnClickListener {
            selectedDateId?.let { date ->
                findNavController().navigate(
                    R.id.action_navigation_myautocalendar_to_fragment_my_auto_schedule,
                    bundleOf("selectedDate" to date)
                )
            }
        }
    }

    private fun resetSelection() {
        binding.daysToggleGroup.clearChecked()
        selectedDateId = null
        binding.btnFancy.visibility = View.INVISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
