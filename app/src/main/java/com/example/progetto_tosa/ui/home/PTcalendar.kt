package com.example.progetto_tosa.ui.home

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.FragmentPtCalendarBinding
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class PTcalendar : Fragment() {

    private var _binding: FragmentPtCalendarBinding? = null
    private val binding get() = _binding!!

    private var selectedUser: String? = null
    private var weekOffset = 0
    private var dateId: String? = null
    private var isPT: Boolean = false

    // tengono traccia della selezione corrente
    private var selectedCard: MaterialCardView? = null
    private var selectedText: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPtCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1) recupera l’utente selezionato
        selectedUser = arguments?.getString("selectedUser")
        // 2) recupera il ruolo
        val prefs = requireActivity()
            .getSharedPreferences("user_data", Context.MODE_PRIVATE)
        isPT = prefs.getBoolean("is_trainer", false)

        // 3) imposta il titolo
        binding.textUserTitle.text = selectedUser ?: getString(R.string.app_name)

        // 4) stato iniziale
        dateId = null
        binding.btnFancy.visibility = View.INVISIBLE
        binding.btnFancy.text = if (isPT)
            "Compila la scheda al tuo allievo!"
        else
            "Visualizza la scheda che il PT ha fatto per te!"

        // 5) popola i giorni
        setupDays()

        // 6) prev/next settimana
        binding.btnPrevWeek.setOnClickListener {
            weekOffset--
            resetSelection()
        }
        binding.btnNextWeek.setOnClickListener {
            weekOffset++
            resetSelection()
        }

        // 7) click sul bottone
        binding.btnFancy.setOnClickListener {
            if (dateId == null) {
                Toast.makeText(
                    requireContext(),
                    "Seleziona prima una data",
                    Toast.LENGTH_SHORT
                ).show()
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

    private fun resetSelection() {
        // deseleziona visivamente
        selectedCard?.setCardBackgroundColor(
            ContextCompat.getColor(requireContext(), R.color.light_gray)
        )
        selectedText?.setTextColor(
            ContextCompat.getColor(requireContext(), R.color.black)
        )
        selectedCard = null
        selectedText = null

        dateId = null
        binding.btnFancy.visibility = View.INVISIBLE
        setupDays()
    }

    private fun setupDays() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            add(Calendar.WEEK_OF_YEAR, weekOffset)
        }
        val labelFmt = SimpleDateFormat("EEEE d MMMM", Locale("it", "IT"))
        val idFmt    = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // colori
        val defaultBg = ContextCompat.getColor(requireContext(), R.color.light_gray)
        val defaultText = ContextCompat.getColor(requireContext(), R.color.black)
        val selBg = ContextCompat.getColor(requireContext(), R.color.navy_blue)
        val selText = ContextCompat.getColor(requireContext(), R.color.sky)

        listOf(
            Triple(binding.cardMonday,    binding.textMonday,    0),
            Triple(binding.cardTuesday,   binding.textTuesday,   1),
            Triple(binding.cardWednesday, binding.textWednesday, 2),
            Triple(binding.cardThursday,  binding.textThursday,  3),
            Triple(binding.cardFriday,    binding.textFriday,    4),
            Triple(binding.cardSaturday,  binding.textSaturday,  5),
            Triple(binding.cardSunday,    binding.textSunday,    6)
        ).forEach { (card, text, offset) ->
            // calcolo data
            val d = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, offset) }
            text.text = labelFmt.format(d.time).replaceFirstChar { it.uppercase() }

            card.setOnClickListener {
                // deseleziona precedente
                selectedCard?.setCardBackgroundColor(defaultBg)
                selectedText?.setTextColor(defaultText)

                // seleziona questo
                card.setCardBackgroundColor(selBg)
                text.setTextColor(selText)
                selectedCard = card
                selectedText = text

                // salva ID e mostra bottone
                dateId = idFmt.format(d.time)
                binding.btnFancy.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        resetSelection()
    }
}
