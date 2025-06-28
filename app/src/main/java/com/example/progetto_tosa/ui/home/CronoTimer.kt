package com.example.progetto_tosa.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.progetto_tosa.R
import com.example.progetto_tosa.ui.stepwatch.StepwatchFragment
import com.example.progetto_tosa.ui.stepwatch.TimerFragment
import com.google.android.material.button.MaterialButtonToggleGroup

class CronoTimer : Fragment() {

    private val cronoTag = "CRONO"
    private val timerTag = "TIMER"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_cronotimer, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val toggle = view.findViewById<MaterialButtonToggleGroup>(R.id.toggleCronoTimer)

        //trova o crea i fragment solo la prima volta
        val fm = childFragmentManager
        var crono = fm.findFragmentByTag(cronoTag) as? StepwatchFragment        //al posto di avere un mattone di codice, ne abbiamo 3, uno che è quello definito nella navbar e da lui richiamo il cronometro e timer, separatamente
        var timer = fm.findFragmentByTag(timerTag) as? TimerFragment

        if (crono == null) {
            crono = StepwatchFragment()
            fm.beginTransaction()
                .add(R.id.fragment_container, crono, cronoTag)
                .commitNow()
        }
        if (timer == null) {
            timer = TimerFragment()
            fm.beginTransaction()
                .add(R.id.fragment_container, timer, timerTag)
                .hide(timer)
                .commitNow()
        }

        // Imposta selezione iniziale
        if (savedInstanceState == null) {
            toggle.check(R.id.btnCronometro)
        }

        // Al cambio toggle show/hide senza replace
        toggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            fm.beginTransaction().apply {
                when (checkedId) {
                    R.id.btnCronometro -> {
                        show(crono)
                        hide(timer)
                    }
                    R.id.btnTimer -> {
                        show(timer)
                        hide(crono)
                    }
                }
            }.commit()
        }
    }
}
