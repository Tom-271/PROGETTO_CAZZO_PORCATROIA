package com.example.progetto_tosa.ui.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.FragmentPtCalendarBinding
import com.google.android.material.button.MaterialButtonToggleGroup
import java.text.SimpleDateFormat
import java.util.*

class PTcalendar : Fragment() {

    private var _binding: FragmentPtCalendarBinding? = null
    private val binding get() = _binding!!

    private var selectedUser: String? = null
    private var weekOffset = 0
    private var dateId: String? = null
    private var isPT: Boolean = false

    // Mappa buttonId → dayOffset
    private val buttonInfo = mutableMapOf<Int, Int>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPtCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // setup utente e ruolo
        selectedUser = arguments?.getString("selectedUser")
        val prefs = requireActivity()
            .getSharedPreferences("user_data", Context.MODE_PRIVATE)
        isPT = prefs.getBoolean("is_trainer", false)

        binding.textUserTitle.text = selectedUser ?: getString(R.string.app_name)
        binding.btnFancy.visibility = View.INVISIBLE
        binding.btnFancy.text = if (isPT)
            "Compila la scheda al tuo allievo!"
        else
            "Visualizza la scheda che il PT ha fatto per te!"

        setupDays()
        setupNavButtons()
    }

    private fun setupNavButtons() {
        binding.btnPrevWeek.setOnClickListener {
            weekOffset--
            resetSelection()
            setupDays()
        }
        binding.btnNextWeek.setOnClickListener {
            weekOffset++
            resetSelection()
            setupDays()
        }
        binding.btnFancy.setOnClickListener {
            if (dateId == null) {
                Toast.makeText(requireContext(), "Seleziona prima una data", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            findNavController().navigate(
                R.id.action_navigation_ptSchedule_to_fragment_my_trainer_schedule,
                bundleOf(
                    "selectedUser" to selectedUser,
                    "selectedDate" to dateId,
                    "isPT" to isPT
                )
            )
        }
    }

    private fun setupDays() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            add(Calendar.WEEK_OF_YEAR, weekOffset)
        }
        val labelFmt = SimpleDateFormat("EEEE d MMMM", Locale("it", "IT"))
        val idFmt    = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // I 7 MaterialButton all'interno del ToggleGroup
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
            val d = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, idx) }
            btn.text = labelFmt.format(d.time).replaceFirstChar { it.uppercase() }
            buttonInfo[btn.id] = idx
        }

        // Listener sul ToggleGroup
        binding.daysToggleGroup.clearOnButtonCheckedListeners()
        binding.daysToggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                // calcola dataId dal dayOffset
                val offset = buttonInfo[checkedId]!!
                val d = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
                dateId = idFmt.format(d.time)
                binding.btnFancy.visibility = View.VISIBLE
            } else if (group.checkedButtonId == View.NO_ID) {
                // nessuna selezione
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
