package com.example.progetto_tosa.ui.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.FragmentPtCalendarBinding

class PTcalendar : Fragment() {

    private var _binding: FragmentPtCalendarBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: PTCalendarViewModel

    // dettagli utente e ruolo
    private var selectedUser: String? = null
    private var isPT: Boolean = false

    // data selezionata per navigare
    private var selectedDateId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPtCalendarBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(PTCalendarViewModel::class.java)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // recupera args e prefs
        selectedUser = arguments?.getString("selectedUser")
        val prefs = requireActivity()
            .getSharedPreferences("user_data", Context.MODE_PRIVATE)
        isPT = prefs.getBoolean("is_trainer", false)

        // imposta titolo e testo del bottone
        binding.textUserTitle.text = selectedUser ?: getString(R.string.app_name)
        binding.btnFancy.text = if (isPT)
            "Compila la scheda al tuo allievo!"
        else
            "Visualizza la scheda che il PT ha fatto per te!"
        binding.btnFancy.visibility = View.INVISIBLE

        // setta listener navigazione settimana
        binding.btnPrevWeek.setOnClickListener {
            viewModel.onPrevWeek()
            resetSelection()
        }
        binding.btnNextWeek.setOnClickListener {
            viewModel.onNextWeek()
            resetSelection()
        }

        // lista ID dei toggle button
        val dayBtns = listOf(
            binding.btnMonday.id,
            binding.btnTuesday.id,
            binding.btnWednesday.id,
            binding.btnThursday.id,
            binding.btnFriday.id,
            binding.btnSaturday.id,
            binding.btnSunday.id
        )

        // listener di selezione giorno
        binding.daysToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked && checkedId != View.NO_ID) {
                val idx = dayBtns.indexOf(checkedId)
                selectedDateId = viewModel.dateIds.value?.getOrNull(idx)
                binding.btnFancy.visibility = View.VISIBLE
            }
        }

        // click finale per navigazione
        binding.btnFancy.setOnClickListener {
            selectedDateId?.let { date ->
                findNavController().navigate(
                    R.id.action_navigation_ptSchedule_to_fragment_my_trainer_schedule,
                    bundleOf(
                        "selectedUser" to selectedUser,
                        "selectedDate" to date,
                        "isPT" to isPT
                    )
                )
            } ?: Toast.makeText(
                requireContext(),
                "Seleziona prima una data",
                Toast.LENGTH_SHORT
            ).show()
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
