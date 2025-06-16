package com.example.progetto_tosa.ui.home

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.progetto_tosa.R
import com.prolificinteractive.materialcalendarview.MaterialCalendarView
import com.prolificinteractive.materialcalendarview.CalendarDay

class PTscheduleFragment : Fragment(R.layout.fragment_pt_schedule) {

    // runs after the layout is inflated
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // grab calendar view from the layout
        val calendarView = view.findViewById<MaterialCalendarView>(R.id.calendarView)

        // pre-select today's date
        calendarView.selectedDate = CalendarDay.today()

        // listener for date selection
        calendarView.setOnDateChangedListener { _, date, _ ->
            // toast to show the chosen day
            val msg = "Selected: ${date.day}/${date.month}/${date.year}"
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }
}

