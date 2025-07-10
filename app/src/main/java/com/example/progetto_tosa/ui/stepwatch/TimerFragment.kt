// Fragment per gestire il timer con conto alla rovescia personalizzabile
package com.example.progetto_tosa.ui.stepwatch

import android.app.AlertDialog
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.NumberPicker
import androidx.fragment.app.Fragment
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.FragmentTimerBinding
import com.google.android.material.button.MaterialButton

class TimerFragment : Fragment() {

    // Binding per accedere in modo sicuro alle view del layout
    private var _binding: FragmentTimerBinding? = null
    private val binding get() = _binding!!

    // Timer e handler per gestire il countdown e operazioni post-finish
    private var countDownTimer: CountDownTimer? = null
    private val handler = Handler(Looper.getMainLooper())

    // Tempo totale impostato (in secondi) e tempo rimanente
    private var totalSecs = 30L
    private var timeLeft = totalSecs
    private var isRunning = false

    // Inflating del layout tramite view binding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Disabilita click sul quadrante per evitare interferenze
        binding.cardClock.setOnClickListener { }

        // Toggle start/pause del timer al click del pulsante
        (binding.btnPlayPause as MaterialButton).setOnClickListener { toggleTimer() }

        // Permette di modificare il tempo solo quando il timer è fermo
        binding.textStepwatch.setOnClickListener {
            if (!isRunning) showTimePicker()
        }

        // Inizializza display del tempo e progress bar
        updateDisplay(totalSecs)
        binding.progressTimer.max = totalSecs.toInt()
        binding.progressTimer.progress = totalSecs.toInt()
    }

    // Alterna tra avvia e pausa
    private fun toggleTimer() {
        if (isRunning) pauseTimer() else startCountdown(timeLeft)
    }

    // Avvia il CountDownTimer a partire da startSecs
    private fun startCountdown(startSecs: Long) {
        // Annulla eventuale timer esistente
        countDownTimer?.cancel()
        isRunning = true
        // Cambia icona del pulsante in pausa
        (binding.btnPlayPause as MaterialButton).icon =
            resources.getDrawable(R.drawable.ic_pause, null)
        // Avvia animazione di pulsazione
        animatePulse(true)

        countDownTimer = object : CountDownTimer(startSecs * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Aggiorna tempo rimanente e UI ogni secondo
                timeLeft = millisUntilFinished / 1000
                updateDisplay(timeLeft)
                binding.progressTimer.progress = timeLeft.toInt()
            }

            override fun onFinish() {
                // Al termine: reset stato, icona play, stop animazione
                isRunning = false
                (binding.btnPlayPause as MaterialButton).icon =
                    resources.getDrawable(R.drawable.ic_play, null)
                animatePulse(false)
                // Mostro testo "Fine"
                binding.textStepwatch.text = getString(R.string.fine)
                binding.progressTimer.progress = 0
                // Dopo 2 secondi ripristino il timer al valore iniziale
                handler.postDelayed({
                    timeLeft = totalSecs
                    updateDisplay(timeLeft)
                    binding.progressTimer.max = totalSecs.toInt()
                    binding.progressTimer.progress = totalSecs.toInt()
                }, 2000)
            }
        }.start()
    }

    // Metodi per mettere in pausa il timer
    private fun pauseTimer() {
        countDownTimer?.cancel()
        isRunning = false
        (binding.btnPlayPause as MaterialButton).icon =
            resources.getDrawable(R.drawable.ic_play, null)
        animatePulse(false)
    }

    // Aggiorna il TextView con formato mm:ss
    private fun updateDisplay(seconds: Long) {
        val mm = seconds / 60
        val ss = seconds % 60
        binding.textStepwatch.text = String.format("%02d:%02d", mm, ss)
    }

    // Animazione di pulsazione del quadrante quando il timer è attivo
    private fun animatePulse(start: Boolean) {
        binding.cardClock.animate()
            .scaleX(if (start) 1.05f else 1f)
            .scaleY(if (start) 1.05f else 1f)
            .setDuration(500)
            .withEndAction { if (isRunning && start) animatePulse(true) }
            .start()
    }

    // Mostra un dialog con NumberPicker per impostare minuti e secondi
    private fun showTimePicker() {
        val pickerMin = NumberPicker(requireContext()).apply {
            minValue = 0; maxValue = 59; value = (totalSecs / 60).toInt()
        }
        val pickerSec = NumberPicker(requireContext()).apply {
            minValue = 0; maxValue = 59; value = (totalSecs % 60).toInt()
        }
        // Layout orizzontale per contenere i picker
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
            addView(pickerMin)
            addView(pickerSec)
        }
        // Costruisci e mostra AlertDialog per confermare o annullare
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.imposta_tempo)
            .setView(layout)
            .setPositiveButton(R.string.ok) { _, _ ->
                totalSecs = pickerMin.value * 60L + pickerSec.value
                timeLeft = totalSecs
                binding.progressTimer.max = totalSecs.toInt()
                updateDisplay(timeLeft)
            }
            .setNegativeButton(R.string.annulla, null)
            .show()
    }

    // Pulizia binding e timer al termine della view
    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }
}
