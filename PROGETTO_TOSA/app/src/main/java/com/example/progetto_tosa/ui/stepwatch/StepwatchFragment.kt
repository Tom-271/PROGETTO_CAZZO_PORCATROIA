package com.example.progetto_tosa.ui.stepwatch

import android.animation.ValueAnimator
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.progetto_tosa.databinding.FragmentStepwatchBinding

class StepwatchFragment : Fragment() {

    private var _binding: FragmentStepwatchBinding? = null
    private val binding get() = _binding!!

    private var countDownTimer: CountDownTimer? = null
    private lateinit var heartbeat: ValueAnimator
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStepwatchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Heartbeat: two quick bumps then a pause, total 960ms
        heartbeat = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 960L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            addUpdateListener { anim ->
                val t = anim.animatedFraction * duration
                val scale = when {
                    t < 100 -> 1f + (t / 100f) * 0.05f        // up1
                    t < 200 -> 1.05f - ((t - 100) / 100f) * 0.05f // down1
                    t < 280 -> 1f + ((t - 200) / 80f) * 0.05f   // up2
                    t < 360 -> 1.05f - ((t - 280) / 80f) * 0.05f  // down2
                    else     -> 1f                             // pause
                }
                binding.cardClock.scaleX = scale
                binding.cardClock.scaleY = scale
                binding.progressTimer.scaleX = scale
                binding.progressTimer.scaleY = scale
            }
        }
        heartbeat.start() // pulsazione in idle

        binding.cardClock.setOnClickListener {
            // ferma battito
            heartbeat.cancel()
            resetScale()

            val totalSecs = binding.editSeconds.text.toString().toLongOrNull() ?: 30L
            binding.progressTimer.max = totalSecs.toInt()
            binding.progressTimer.progress = totalSecs.toInt()
            binding.textStepwatch.text = "${totalSecs}s"

            startCountdown(totalSecs)
        }
    }

    private fun resetScale() {
        binding.cardClock.scaleX = 1f
        binding.cardClock.scaleY = 1f
        binding.progressTimer.scaleX = 1f
        binding.progressTimer.scaleY = 1f
    }

    private fun startCountdown(totalSecs: Long) {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(totalSecs * 1000, 1000) {
            override fun onTick(millis: Long) {
                val left = (millis / 1000).toInt()
                binding.textStepwatch.text = "${left}s"
                binding.progressTimer.progress = left
            }

            override fun onFinish() {
                binding.textStepwatch.text = "Fine!"
                binding.progressTimer.progress = 0
                handler.postDelayed({
                    binding.textStepwatch.text = "30s"
                    binding.progressTimer.progress = binding.progressTimer.max
                    heartbeat.start() // riprende battito
                }, 2000)
            }
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        heartbeat.cancel()
        handler.removeCallbacksAndMessages(null)
        _binding = null
    }
}
