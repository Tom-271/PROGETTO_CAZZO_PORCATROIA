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

/**
 * Fragment che mostra un calendario settimanale generato automaticamente.
 * Permette di navigare tra le settimane e selezionare un giorno per vedere la propria pianificazione.
 */
class MyAutoCalendar : Fragment() {

    // Binding generato per il layout fragment_myautocalendar.xml
    private var _binding: FragmentMyautocalendarBinding? = null
    private val binding get() = _binding!!

    // ViewModel che gestisce la logica e i dati del calendario
    private lateinit var viewModel: MyAutoCalendarViewModel

    // Memorizza l'ID della data selezionata per passarla in navigazione
    private var selectedDateId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inizializza il binding e il ViewModel
        _binding = FragmentMyautocalendarBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(MyAutoCalendarViewModel::class.java)
        // Collega il ViewModel al layout per DataBinding
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configura i pulsanti per navigare tra le settimane
        binding.btnPrevWeek.setOnClickListener {
            viewModel.onPrevWeek()   // Mostra la settimana precedente
            resetSelection()         // Resetta eventuali selezioni di giorni
        }
        binding.btnNextWeek.setOnClickListener {
            viewModel.onNextWeek()   // Mostra la settimana successiva
            resetSelection()         // Resetta eventuali selezioni di giorni
        }

        // Lista degli ID dei ToggleButton in ordine da Lunedì a Domenica
        val dayBtns = listOf(
            binding.btnMonday.id,
            binding.btnTuesday.id,
            binding.btnWednesday.id,
            binding.btnThursday.id,
            binding.btnFriday.id,
            binding.btnSaturday.id,
            binding.btnSunday.id
        )

        // Listener sul gruppo di toggle: quando si seleziona un giorno,
        // mostra il pulsante "fancy" e memorizza la data corrispondente
        binding.daysToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked && checkedId != View.NO_ID) {
                // Trova l'indice del giorno selezionato
                val idx = dayBtns.indexOf(checkedId)
                // Ottiene l'ID della data dal ViewModel
                selectedDateId = viewModel.dateIds.value?.getOrNull(idx)
                // Rende visibile il pulsante per confermare la selezione
                binding.btnFancy.visibility = View.VISIBLE
            }
        }

        // Click sul pulsante "fancy": naviga verso il Fragment di pianificazione,
        // passando la data selezionata tramite Bundle
        binding.btnFancy.setOnClickListener {
            selectedDateId?.let { date ->
                findNavController().navigate(
                    R.id.action_navigation_myautocalendar_to_fragment_my_auto_schedule,
                    bundleOf("selectedDate" to date)
                )
            }
        }
    }

    // Resetta la selezione di un giorno nel ToggleGroup, nasconde il pulsante "fancy" e pulisce la variabile selectedDateId

    private fun resetSelection() {
        binding.daysToggleGroup.clearChecked()
        selectedDateId = null
        binding.btnFancy.visibility = View.INVISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Libera il binding per evitare memory leaks
        _binding = null
    }
}
