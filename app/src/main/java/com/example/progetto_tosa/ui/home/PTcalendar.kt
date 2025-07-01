package com.example.progetto_tosa.ui.home

import android.os.Bundle
import android.view.View
import android.widget.Toast
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.progetto_tosa.R
import com.google.android.material.button.MaterialButton
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.CalendarDay
import androidx.core.os.bundleOf

class PTcalendar : Fragment(R.layout.fragment_pt_calendar) {

    private var selectedUser: String? = null
    private var dateId: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1) recupera l’utente selezionato
        selectedUser = arguments?.getString("selectedUser")

        // 2) imposta il titolo
        val titleView = view.findViewById<TextView>(R.id.textUserTitle)
        titleView.text = selectedUser ?: getString(R.string.app_name)

        // 3) trova il calendario e il bottone
        val calendarView = view.findViewById<MaterialCalendarView>(R.id.calendarView)
        val btnFancy     = view.findViewById<MaterialButton>(R.id.btnFancy)
        btnFancy.visibility = View.INVISIBLE

        // 4) listener data
        calendarView.setOnDateChangedListener { _, date, _ ->
            dateId = "%04d-%02d-%02d".format(date.year, date.month + 1, date.day)
            btnFancy.visibility = View.VISIBLE
        }

        // 5) click bottone
        btnFancy.setOnClickListener {
            if (dateId == null) {
                Toast.makeText(requireContext(),
                    "Seleziona prima una data", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val args = bundleOf(
                "selectedUser" to selectedUser,
                "selectedDate" to dateId
            )
            findNavController().navigate(
                R.id.action_navigation_ptSchedule_to_fragment_my_trainer_schedule,
                args
            )
        }
    }
}
