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

class MyAutoCalendar : Fragment() {

    private var _binding: FragmentMyautocalendarBinding? = null
    private val binding get() = _binding!!

    // verrà valorizzato alla selezione di un giorno
    private var dateId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyautocalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // salva in dateId la stringa "yyyy-MM-dd" quando l’utente sceglie un giorno
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            dateId = "%04d-%02d-%02d".format(year, month + 1, dayOfMonth)
            binding.btnFancy.visibility = View.VISIBLE
        }

        // recupera il button via binding e imposta il click
        binding.btnFancy.setOnClickListener {
            dateId?.let { id ->
                findNavController().navigate(
                    R.id.action_navigation_myautocalendar_to_fragment_my_auto_schedule,
                    bundleOf("selectedDate" to id)
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
