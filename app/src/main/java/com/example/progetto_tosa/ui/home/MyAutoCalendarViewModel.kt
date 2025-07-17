package com.example.progetto_tosa.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.text.SimpleDateFormat
import java.util.*

class MyAutoCalendarViewModel : ViewModel() {

    // Offset di settimane rispetto a quella corrente
    private val _weekOffset = MutableLiveData(0)

    // Etichette dei giorni (Lunedì…Domenica)
    private val _dayLabels = MutableLiveData<List<String>>()
    val dayLabels: LiveData<List<String>> = _dayLabels

    // Stringhe delle date in formato yyyy-MM-dd
    private val _dateIds = MutableLiveData<List<String>>()
    val dateIds: LiveData<List<String>> = _dateIds

    init {
        updateDays()
    }

    private fun updateDays() {
        val calendar = Calendar.getInstance()
            .apply {
                set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                add(Calendar.WEEK_OF_YEAR, _weekOffset.value ?: 0)
            }

        val labelFmt = SimpleDateFormat("EEEE d MMMM", Locale("it", "IT"))
        val idFmt    = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val labels = mutableListOf<String>()
        val ids    = mutableListOf<String>()
        repeat(7) { i ->
            val c = calendar.clone() as Calendar
            c.add(Calendar.DAY_OF_YEAR, i)
            labels += labelFmt.format(c.time).replaceFirstChar { it.uppercase() }
            ids    += idFmt.format(c.time)
        }
        _dayLabels.value = labels
        _dateIds.value   = ids
    }

    fun onPrevWeek() {
        _weekOffset.value = (_weekOffset.value ?: 0) - 1
        updateDays()
    }

    fun onNextWeek() {
        _weekOffset.value = (_weekOffset.value ?: 0) + 1
        updateDays()
    }
}