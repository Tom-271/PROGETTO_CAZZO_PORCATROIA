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

    private var _binding: FragmentTimerBinding? = null
    private val binding get() = _binding!!

    private var countDownTimer: CountDownTimer? = null
    private val handler = Handler(Looper.getMainLooper())

    private var totalSecs = 30L
    private var timeLeft = totalSecs
    private var isRunning = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTimerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Ignora click sul quadrante
        binding.cardClock.setOnClickListener { }

        // Toggle start/pause
        (binding.btnPlayPause as MaterialButton).setOnClickListener { toggleTimer() }

        // Modifica tempo quando fermo
        binding.textStepwatch.setOnClickListener {
            if (!isRunning) showTimePicker()
        }

        // Inizializza display e progress
        updateDisplay(totalSecs)
        binding.progressTimer.max = totalSecs.toInt()
        binding.progressTimer.progress = totalSecs.toInt()
    }

    private fun toggleTimer() {
        if (isRunning) pauseTimer() else startCountdown(timeLeft)
    }

    private fun startCountdown(startSecs: Long) {
        countDownTimer?.cancel()
        isRunning = true
        (binding.btnPlayPause as MaterialButton).icon =
            resources.getDrawable(R.drawable.ic_pause, null)
        animatePulse(true)

        countDownTimer = object : CountDownTimer(startSecs * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeft = millisUntilFinished / 1000
                updateDisplay(timeLeft)
                binding.progressTimer.progress = timeLeft.toInt()
            }

            override fun onFinish() {
                isRunning = false
                (binding.btnPlayPause as MaterialButton).icon =
                    resources.getDrawable(R.drawable.ic_play, null)
                animatePulse(false)
                binding.textStepwatch.text = getString(R.string.fine)
                binding.progressTimer.progress = 0
                handler.postDelayed({
                    timeLeft = totalSecs
                    updateDisplay(timeLeft)
                    binding.progressTimer.max = totalSecs.toInt()
                    binding.progressTimer.progress = totalSecs.toInt()
                }, 2000)
            }
        }.start()
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        isRunning = false
        (binding.btnPlayPause as MaterialButton).icon =
            resources.getDrawable(R.drawable.ic_play, null)
        animatePulse(false)
    }

    private fun updateDisplay(seconds: Long) {
        val mm = seconds / 60
        val ss = seconds % 60
        binding.textStepwatch.text = String.format("%02d:%02d", mm, ss)
    }

    private fun animatePulse(start: Boolean) {
        binding.cardClock.animate()
            .scaleX(if (start) 1.05f else 1f)
            .scaleY(if (start) 1.05f else 1f)
            .setDuration(500)
            .withEndAction { if (isRunning && start) animatePulse(true) }
            .start()
    }

    private fun showTimePicker() {
        val pickerMin = NumberPicker(requireContext()).apply {
            minValue = 0; maxValue = 59; value = (totalSecs / 60).toInt()
        }
        val pickerSec = NumberPicker(requireContext()).apply {
            minValue = 0; maxValue = 59; value = (totalSecs % 60).toInt()
        }
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            val pad = (16 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
            addView(pickerMin)
            addView(pickerSec)
        }
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

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }
}
