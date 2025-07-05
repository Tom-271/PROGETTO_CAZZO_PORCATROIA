package com.example.progetto_tosa.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.FragmentMyautocalendarBinding
import com.google.android.material.button.MaterialButtonToggleGroup
import java.text.SimpleDateFormat
import java.util.*

class MyAutoCalendar : Fragment() {

    private var _binding: FragmentMyautocalendarBinding? = null
    private val binding get() = _binding!!
    private var weekOffset = 0
    private var dateId: String? = null

    // Mappa buttonId → (dayOffset, dateString)
    private val buttonInfo = mutableMapOf<Int, Pair<Int, String>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyautocalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnFancy.visibility = View.INVISIBLE
        setupNavButtons()
        setupToggleDays()
    }

    private fun setupNavButtons() {
        binding.btnPrevWeek.setOnClickListener {
            weekOffset--
            resetSelection()
            setupToggleDays()
        }
        binding.btnNextWeek.setOnClickListener {
            weekOffset++
            resetSelection()
            setupToggleDays()
        }
        binding.btnFancy.setOnClickListener {
            dateId?.let { id ->
                findNavController().navigate(
                    R.id.action_navigation_myautocalendar_to_fragment_my_auto_schedule,
                    bundleOf("selectedDate" to id)
                )
            }
        }
    }

    private fun setupToggleDays() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            add(Calendar.WEEK_OF_YEAR, weekOffset)
        }
        val labelFmt = SimpleDateFormat("EEEE d MMMM", Locale("it", "IT"))
        val idFmt    = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // Tutti i MaterialButton del gruppo
        val buttons = listOf(
            binding.btnMonday,
            binding.btnTuesday,
            binding.btnWednesday,
            binding.btnThursday,
            binding.btnFriday,
            binding.btnSaturday,
            binding.btnSunday
        )

        buttonInfo.clear()
        buttons.forEachIndexed { idx, btn ->
            val dayCal = calendar.clone() as Calendar
            dayCal.add(Calendar.DAY_OF_YEAR, idx)
            val label = labelFmt.format(dayCal.time)
                .replaceFirstChar { it.uppercase() }
            val idStr = idFmt.format(dayCal.time)

            btn.text = label
            buttonInfo[btn.id] = idx to idStr
        }

        // Ripulisci eventuali listener precedenti
        binding.daysToggleGroup.clearOnButtonCheckedListeners()
        binding.daysToggleGroup.addOnButtonCheckedListener { group, _, _ ->
            // se c'è un bottone selezionato, mostra btnFancy e aggiorna dateId
            val selId = group.checkedButtonId
            if (selId != View.NO_ID) {
                dateId = buttonInfo[selId]?.second
                binding.btnFancy.visibility = View.VISIBLE
            } else {
                dateId = null
                binding.btnFancy.visibility = View.INVISIBLE
            }
        }
    }

    private fun resetSelection() {
        binding.daysToggleGroup.clearChecked()
        dateId = null
        binding.btnFancy.visibility = View.INVISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
