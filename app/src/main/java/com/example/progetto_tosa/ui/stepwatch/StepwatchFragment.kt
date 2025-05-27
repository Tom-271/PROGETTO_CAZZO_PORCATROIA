package com.example.progetto_tosa.ui.stepwatch

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.progetto_tosa.R

class StepwatchFragment : Fragment() {

    private lateinit var timerText: TextView
    private lateinit var startButton: Button
    private lateinit var pauseButton: Button
    private lateinit var resetButton: Button
    private lateinit var roundButton: Button

    private var handler = Handler(Looper.getMainLooper())
    private var startTime = 0L
    private var timeInMillis = 0L
    private var isRunning = false
    private var hasStartedOnce = false  // Indica se il cronometro è mai stato avviato
    private lateinit var lapContainer: LinearLayout
    private var lastLapTime: Long = 0L
    private var lapCount = 0
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isRunning) {
                val now = System.currentTimeMillis()
                timeInMillis = now - startTime
                updateTimerText(timeInMillis)
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_stepwatch, container, false)

        timerText = view.findViewById(R.id.cronometro_text)
        startButton = view.findViewById(R.id.start_button)
        pauseButton = view.findViewById(R.id.pause_button)
        resetButton = view.findViewById(R.id.reset_button)
        roundButton = view.findViewById(R.id.round_button)
        val lapScrollView = view.findViewById<ScrollView>(R.id.lap_scrollview)
        lapContainer = view.findViewById(R.id.lap_container)

        // Visibilità iniziale: solo Start visibile
        startButton.visibility = View.VISIBLE
        pauseButton.visibility = View.GONE
        resetButton.visibility = View.GONE
        roundButton.visibility = View.GONE

        startButton.setOnClickListener {
            if (!isRunning) {
                startTime = System.currentTimeMillis() - timeInMillis
                handler.post(updateRunnable)
                isRunning = true
                hasStartedOnce = true

                // Mostra Pause e Reset, nascondi Start
                startButton.visibility = View.GONE
                pauseButton.visibility = View.VISIBLE
                resetButton.visibility = View.VISIBLE
                roundButton.visibility = View.VISIBLE
            }
        }

        pauseButton.setOnClickListener {
            if (isRunning) {
                handler.removeCallbacks(updateRunnable)
                isRunning = false

                // Mostra di nuovo Start se il cronometro è in pausa
                pauseButton.visibility = View.GONE
                roundButton.visibility = View.VISIBLE
                resetButton.visibility = View.VISIBLE
                startButton.visibility = View.VISIBLE
                // Reset resta visibile
            }
        }

        resetButton.setOnClickListener {
            handler.removeCallbacks(updateRunnable)
            isRunning = false
            timeInMillis = 0L
            updateTimerText(timeInMillis)

            // Torna alla visibilità iniziale
            startButton.visibility = View.VISIBLE
            pauseButton.visibility = View.GONE
            resetButton.visibility = View.GONE
            roundButton.visibility = View.GONE

            hasStartedOnce = true


            lapContainer.removeAllViews()
            lapCount = 0
            lastLapTime = 0L
        }

        roundButton.setOnClickListener {
            if (isRunning) {
                val currentLapTime = System.currentTimeMillis() - startTime
                val lapDuration = currentLapTime - lastLapTime
                lastLapTime = currentLapTime
                lapCount++

                val currentFormatted = formatTime(currentLapTime)
                val diffFormatted = formatTime(lapDuration)

                val lapView = TextView(requireContext()).apply {
                    text = "N$lapCount   $currentFormatted   +$diffFormatted"
                    textSize = 16f
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                lapContainer.addView(lapView)

                lapScrollView.post {
                    lapScrollView.fullScroll(View.FOCUS_DOWN)
                }

            }
        }

        return view
    }

    private fun updateTimerText(millis: Long) {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        timerText.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun formatTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateRunnable)
    }
}
