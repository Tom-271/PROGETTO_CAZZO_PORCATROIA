package com.example.progetto_tosa.ui.home

import android.content.Context
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.progetto_tosa.databinding.CardTrainingBinding
import com.google.firebase.firestore.FirebaseFirestore

class TrainingStartDialogFragment : DialogFragment() {

    private var _binding: CardTrainingBinding? = null
    private val binding get() = _binding!!

    private var currentExerciseIndex = 0
    private var exercises: List<ScheduledExercise> = emptyList()

    private val db = FirebaseFirestore.getInstance()

    private var selectedDate: String? = null
    private var selectedUser: String? = null
    private var category: String? = null

    private var countDownTimer: CountDownTimer? = null
    private val handler = Handler(Looper.getMainLooper())

    // recovery timer
    private var totalSecs = 30L
    private var timeLeft = totalSecs
    private var isRunning = false
    private var inRecovery = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedDate = arguments?.getString("selectedDate")
        selectedUser = arguments?.getString("selectedUser")
        category     = arguments?.getString("category")
        exercises = (arguments?.getSerializable("exercises") as? ArrayList<ScheduledExercise>) ?: emptyList()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = CardTrainingBinding.inflate(inflater, container, false)

        if (selectedDate.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Data non valida", Toast.LENGTH_SHORT).show()
            dismiss()
            return binding.root
        }

        if (exercises.isNotEmpty()) {
            showExercise(exercises[currentExerciseIndex])
        }

        binding.buttonExit.setOnClickListener { dismiss() }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (selectedDate.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Data non valida", Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }
        setupButtons()
    }

    private fun setupButtons() {
        binding.buttonExit.setOnClickListener { dismiss() }

        binding.EndExercise.setOnClickListener {
            // ensure clock container visible when duration-mode is active
            binding.stepwatchContainer.visibility = VISIBLE

            val current = exercises.getOrNull(currentExerciseIndex) ?: return@setOnClickListener
            val hasDuration = parseDurationSecsStrict(current.durata) > 0

            // show clock area; hide lower card in duration-mode
            binding.cardClock.visibility = VISIBLE
            binding.progressTimer.visibility = VISIBLE
            if (hasDuration) {
                binding.trainingCard.visibility = GONE
                current.sets -= 1
                ensureSetsAbovePost(current.sets)
            } else {
                binding.trainingCard.visibility = GONE
            }

            if (!inRecovery) {
                // end a set -> start recovery
                if (current.sets > 0) {
                    current.sets -= 1
                    if (hasDuration) {
                        ensureSetsAbove(current.sets)
                    } else {
                        binding.exerciseSet.text = current.sets.toString()
                    }
                }
                startRecovery()
                inRecovery = true
            } else {
                // resume set before recovery finishes
                stopRecovery()
                binding.EndExercise.text = "termina serie"
                inRecovery = false

                if (current.sets <= 0) {
                    goToNextExerciseOrFinish()
                } else {
                    if (hasDuration) {
                        showClockWithTrainingSet(current.durata.orEmpty(), current.sets)
                    } else {
                        showFullCard()
                    }
                }
            }
        }
    }

    private fun showExercise(exercise: ScheduledExercise) {
        // base info
        binding.eserciseName.text = exercise.nome
        binding.exerciseSet.text = exercise.sets.toString()
        binding.exerciseReps.text = exercise.reps.toString()
        binding.exerciseRecover.text = exercise.recupero.toString()

        // visibility for the lower card (used only if no duration)
        toggleVisibility(binding.trainingSet, exercise.sets)
        toggleVisibility(binding.trainingReps, exercise.reps)
        toggleVisibility(binding.trainingRecover, exercise.recupero)

        // duration strict
        val durSecs = parseDurationSecsStrict(exercise.durata)
        val hasDuration = durSecs > 0

        // recovery timer
        totalSecs = parseMinSec(exercise.recupero)
        timeLeft = totalSecs
        updateProgressFor(timeLeft, totalSecs)

        if (hasDuration) {
            binding.stepwatchContainer.visibility = VISIBLE
            ensureSetsAbovePost(exercise.sets)
            showClockWithTrainingSet(exercise.durata.orEmpty(), exercise.sets)
        } else {
            showFullCard()
            updateDisplay(timeLeft)
        }

        binding.EndExercise.text = "termina serie"
        inRecovery = false
        isRunning = false

        // exercise image
        loadExerciseImageResId(category ?: "null", exercise.nome) { resId ->
            if (resId != 0) binding.exerciseImage.setImageResource(resId)
        }

        // weight/duration slot only when not in duration-mode
        if (!hasDuration) {
            val weightStr = exercise.peso?.toString()?.trim()
            val weightNum = weightStr?.replace(",", ".")?.toDoubleOrNull()
            val hasWeight = !weightStr.isNullOrEmpty() && (weightNum == null || weightNum != 0.0)

            val durationStr = exercise.durata?.trim()
            val showDurationInsteadOfWeight = !hasWeight && !durationStr.isNullOrEmpty() && durationStr != "00:00"

            if (showDurationInsteadOfWeight) {
                binding.textWeight.text = "DURATION"
                binding.exerciseWeight.text = durationStr
                binding.trainingWeight.visibility = VISIBLE
            } else {
                binding.textWeight.text = "WEIGHT"
                if (hasWeight) {
                    binding.exerciseWeight.text = weightStr
                    binding.trainingWeight.visibility = VISIBLE
                } else {
                    binding.exerciseWeight.text = ""
                    binding.trainingWeight.visibility = GONE
                }
            }
        }
    }

    // === UI helpers ===

    private fun showClockWithTrainingSet(durationStr: String, remainingSets: Int) {
        // only "sets above" + clock in duration-mode
        binding.trainingCard.visibility = GONE
        binding.stepwatchContainer.visibility = VISIBLE

        ensureSetsAbovePost(remainingSets)


        binding.cardClock.visibility = VISIBLE
        binding.progressTimer.visibility = VISIBLE
        binding.EndExercise.visibility = VISIBLE

        binding.textStepwatch.text = durationStr
    }

    private fun showFullCard() {
        binding.cardClock.visibility = GONE
        binding.progressTimer.visibility = GONE
        binding.trainingSetAbove.visibility = GONE
        binding.stepwatchContainer.visibility = VISIBLE
        binding.trainingCard.visibility = VISIBLE
        binding.EndExercise.visibility = VISIBLE
    }

    private fun toggleVisibility(layout: LinearLayout, value: Any?) {
        layout.visibility = if (value == null || value.toString().isEmpty() || value == "00:00") GONE else VISIBLE
    }

    // === recovery timer ===

    private fun startRecovery() {
        Log.d("TrainingStartDialog", "Inizio recupero per esercizio: ${exercises.getOrNull(currentExerciseIndex)?.nome}")

        _binding?.EndExercise?.visibility = GONE
        stopRecovery()
        isRunning = true
        animatePulse(true)

        // setup ring for current recovery
        updateProgressFor(totalSecs, totalSecs)

        countDownTimer = object : CountDownTimer(timeLeft * 1000, 1000) {
            override fun onTick(ms: Long) {
                timeLeft = ms / 1000
                updateDisplay(timeLeft)
                updateProgressFor(timeLeft, totalSecs)
            }

            override fun onFinish() {
                isRunning = false
                inRecovery = false
                updateDisplay(0)
                updateProgressFor(0, totalSecs)

                if (_binding == null) return

                val current = exercises.getOrNull(currentExerciseIndex)
                val lastExercise = isLastExercise()
                val hasDuration = parseDurationSecsStrict(current?.durata) > 0

                if (lastExercise && (current == null || current.sets <= 0)) {
                    timeLeft = totalSecs
                    _binding?.EndExercise?.isClickable = true
                    animatePulse(false)
                    if (isAdded) dismiss()
                    return
                }

                _binding?.EndExercise?.text = "termina serie"

                if (current != null && current.sets <= 0) {
                    goToNextExerciseOrFinish()
                } else {
                    if (hasDuration) {
                        _binding?.let {
                            it.trainingCard.visibility = GONE
                            it.stepwatchContainer.visibility = VISIBLE
                            ensureSetsAbovePost(current!!.sets)

                            it.cardClock.visibility = VISIBLE
                            it.progressTimer.visibility = VISIBLE
                            it.textStepwatch.text = current.durata.orEmpty()
                        }
                    } else {
                        showFullCard()
                    }
                }

                timeLeft = totalSecs
                _binding?.EndExercise?.isClickable = true
                animatePulse(false)
            }
        }.start()
    }

    private fun updateProgressFor(current: Long, total: Long) {
        val b = _binding ?: return
        val t = total.coerceAtLeast(1).toInt()
        b.progressTimer.max = t
        b.progressTimer.progress = current.coerceIn(0, total).toInt()
    }

    private fun isLastExercise(): Boolean = currentExerciseIndex >= (exercises.size - 1)

    private fun goToNextExerciseOrFinish() {
        Log.d("TrainingStartDialog", "Passando al prossimo esercizio. Indice corrente: $currentExerciseIndex")
        if (currentExerciseIndex < exercises.size - 1) {
            currentExerciseIndex++
            showExercise(exercises[currentExerciseIndex])
        } else {
            Toast.makeText(requireContext(), "Allenamento completato!", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    private fun updateDisplay(seconds: Long) {
        val b = _binding ?: return
        val mm = seconds / 60
        val ss = seconds % 60
        b.textStepwatch.text = String.format("%02d:%02d", mm, ss)
    }

    private fun animatePulse(start: Boolean) {
        val b = _binding ?: return
        b.cardClock.animate()
            .scaleX(if (start) 1.05f else 1f)
            .scaleY(if (start) 1.05f else 1f)
            .setDuration(500)
            .withEndAction { if (isRunning && start && _binding != null) animatePulse(true) }
            .start()
    }

    private fun stopRecovery() {
        countDownTimer?.cancel()
        countDownTimer = null
        isRunning = false
        if (_binding == null) return
        animatePulse(false)
    }

    // "MM:SS" -> seconds; 0 if null/invalid
    private fun parseDurationSecsStrict(d: String?): Long {
        if (d.isNullOrBlank()) return 0L
        return try {
            val p = d.split(":")
            val mm = p.getOrNull(0)?.toLongOrNull() ?: 0L
            val ss = p.getOrNull(1)?.toLongOrNull() ?: 0L
            (mm * 60 + ss).coerceAtLeast(0)
        } catch (_: Exception) { 0L }
    }

    // recovery "MM:SS" -> seconds; default 30s
    private fun parseMinSec(s: String?): Long {
        if (s.isNullOrBlank()) return 30L
        return try {
            val p = s.split(":")
            val mm = p.getOrNull(0)?.toLongOrNull() ?: 0L
            val ss = p.getOrNull(1)?.toLongOrNull() ?: 0L
            (mm * 60 + ss).coerceAtLeast(0)
        } catch (_: Exception) { 30L }
    }

    private fun ensureSetsAbove(remaining: Int) {
        binding.trainingSetAbove.visibility = View.VISIBLE
        binding.exerciseSetAbove.text = remaining.toString()
        binding.trainingSetAbove.bringToFront()
        binding.trainingSetAbove.elevation = binding.cardClock.elevation + 2f
        binding.trainingSetAbove.translationZ = binding.cardClock.translationZ + 2f
    }

    private fun ensureSetsAbovePost(remaining: Int) {
        binding.root.post { ensureSetsAbove(remaining) } // forza dopo il primo layout pass
    }

    private fun fmt(seconds: Long): String {
        val mm = seconds / 60
        val ss = seconds % 60
        return String.format("%02d:%02d", mm, ss)
    }

    private fun loadExerciseImageResId(
        category: String,
        exerciseName: String,
        onReady: (Int) -> Unit
    ) {
        val slug = exerciseName.toSlug()
        db.collection("esercizi")
            .document(category)
            .collection("voci")
            .document(slug)
            .get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("descriptionImageName")
                    ?: doc.getString("imageResName")
                    ?: ""
                onReady(nameToResId(name))
            }
            .addOnFailureListener { onReady(0) }
    }

    private fun String.toSlug(): String =
        lowercase().replace(" ", "_").replace("[^a-z0-9_]+".toRegex(), "")

    private fun nameToResId(name: String?): Int {
        if (name.isNullOrBlank()) return 0
        return requireContext().resources.getIdentifier(name, "drawable", requireContext().packageName)
    }

    private fun getPrefsUserName(): String {
        val prefs = requireActivity().getSharedPreferences("user_data", Context.MODE_PRIVATE)
        return prefs.getString("saved_display_name", "") ?: ""
    }

    override fun onDestroyView() {
        stopRecovery()
        _binding = null
        super.onDestroyView()
    }

    companion object {
        @JvmStatic
        fun newInstance(
            selectedDate: String,
            category: String,
            exercises: List<ScheduledExercise>,
            selectedUser: String? = null
        ): TrainingStartDialogFragment {
            return TrainingStartDialogFragment().apply {
                arguments = Bundle().apply {
                    putString("selectedDate", selectedDate)
                    putSerializable("exercises", ArrayList(exercises))
                    putString("selectedUser", selectedUser)
                    putString("category", category)
                }
            }
        }
    }
}
