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

    // timer di recupero
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
            val current = exercises.getOrNull(currentExerciseIndex) ?: return@setOnClickListener
            val hasDuration = parseDurationSecsStrict(current.durata) > 0

            // quando premi, entri nello stato di recupero (UI recupero)
            binding.cardClock.visibility = VISIBLE
            binding.progressTimer.visibility = VISIBLE
            // in modalità durata mostriamo trainingSet sopra all'orologio
            if (hasDuration) {
                keepOnlyTrainingSetVisible(current.sets)
                placeTrainingSetAboveClock()
            } else {
                binding.trainingCard.visibility = GONE
            }

            if (!inRecovery) {
                // finita una serie -> avvia recupero
                if (current.sets > 0) {
                    current.sets -= 1
                    binding.exerciseSet.text = current.sets.toString()
                }
                startRecovery()
                inRecovery = true
            } else {
                // inizia la serie prima della fine del recupero
                stopRecovery()
                binding.EndExercise.text = "termina serie"
                inRecovery = false

                if (current.sets <= 0) {
                    goToNextExerciseOrFinish()
                } else {
                    // ripristina UI in base alla durata
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
        // titoli/valori base
        binding.eserciseName.text = exercise.nome
        binding.exerciseSet.text = exercise.sets.toString()
        binding.exerciseReps.text = exercise.reps.toString()
        binding.exerciseRecover.text = exercise.recupero.toString()

        // visibilità base (verrà eventualmente sovrascritta in modalità durata)
        toggleVisibility(binding.trainingSet, exercise.sets)
        toggleVisibility(binding.trainingReps, exercise.reps)
        toggleVisibility(binding.trainingRecover, exercise.recupero)

        // durata strict
        val durSecs = parseDurationSecsStrict(exercise.durata)
        val hasDuration = durSecs > 0

        // timer recupero
        totalSecs = parseMinSec(exercise.recupero)
        timeLeft = totalSecs

        if (hasDuration) {
            // mostra orologio e SOPRA solo trainingSet
            showClockWithTrainingSet(exercise.durata.orEmpty(), exercise.sets)
        } else {
            // UI completa come prima
            showFullCard()
            updateDisplay(timeLeft) // mm:ss del recupero
        }

        binding.EndExercise.text = "termina serie"
        inRecovery = false
        isRunning = false

        // immagini esercizio
        loadExerciseImageResId(category ?: "null", exercise.nome) { resId ->
            if (resId != 0) binding.exerciseImage.setImageResource(resId)
        }

        // SLOT WEIGHT/DURATION (solo quando UI completa)
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

    // mostra SOLO trainingSet (sopra) + orologio (sotto)
    private fun showClockWithTrainingSet(durationStr: String, remainingSets: Int) {
        // mostra solo trainingSet all'interno della card
        binding.trainingCard.visibility = VISIBLE
        keepOnlyTrainingSetVisible(remainingSets)
        placeTrainingSetAboveClock()

        // mostra orologio; se è figlio di progressTimer tieni visibile il parent
        binding.cardClock.visibility = VISIBLE
        val isChildOfProgress = (binding.cardClock.parent === binding.progressTimer)
        binding.progressTimer.visibility = if (isChildOfProgress) VISIBLE else GONE

        binding.EndExercise.visibility = VISIBLE

        // testo orologio = SOLO durata (i set sono nel blocco sopra)
        binding.textStepwatch.text = durationStr
    }

    // rende visibile solo trainingSet nella card e aggiorna il numero
    private fun keepOnlyTrainingSetVisible(remainingSets: Int) {
        binding.trainingSet.visibility = VISIBLE
        binding.exerciseSet.text = remainingSets.toString()

        binding.trainingReps.visibility = GONE
        binding.trainingRecover.visibility = GONE
        binding.trainingWeight.visibility = GONE
    }

    // UI completa (come prima)
    private fun showFullCard() {
        binding.cardClock.visibility = GONE
        binding.progressTimer.visibility = GONE

        binding.trainingCard.visibility = VISIBLE
        // riabilita visibilità base (gli specifici toggle restano in showExercise)
        // qui NON forziamo i singoli layout, per non sovrascrivere i toggle di showExercise
        binding.EndExercise.visibility = VISIBLE
    }

    // === fine UI helpers ===

    private fun toggleVisibility(layout: LinearLayout, value: Any?) {
        layout.visibility = if (value == null || value.toString().isEmpty() || value == "00:00") GONE else VISIBLE
    }

    private fun placeTrainingSetAboveClock() {
        val clockParent = binding.cardClock.parent as? ViewGroup ?: return
        // se trainingSet non è già nello stesso parent, spostalo
        if (binding.exerciseSetAbove.parent != clockParent) {
            (binding.exerciseSetAbove.parent as? ViewGroup)?.removeView(binding.exerciseSetAbove)
            // inseriscilo subito PRIMA di cardClock
            val clockIndex = clockParent.indexOfChild(binding.cardClock)
            clockParent.addView(binding.exerciseSetAbove, clockIndex)
        } else {
            // è già nel parent: assicurati che stia prima di cardClock
            val clockIndex = clockParent.indexOfChild(binding.cardClock)
            val setIndex = clockParent.indexOfChild(binding.exerciseSetAbove)
            if (setIndex > clockIndex) {
                clockParent.removeView(binding.exerciseSetAbove)
                clockParent.addView(binding.exerciseSetAbove, clockIndex)
            }
        }
        binding.trainingSet.visibility = VISIBLE
    }

    private fun startRecovery() {
        Log.d("TrainingStartDialog", "Inizio recupero per esercizio: ${exercises.getOrNull(currentExerciseIndex)?.nome}")

        _binding?.EndExercise?.visibility = GONE
        stopRecovery()
        isRunning = true
        animatePulse(true)

        countDownTimer = object : CountDownTimer(timeLeft * 1000, 1000) {
            override fun onTick(ms: Long) {
                timeLeft = ms / 1000
                updateDisplay(timeLeft)
            }

            override fun onFinish() {
                isRunning = false
                inRecovery = false
                updateDisplay(0)

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
                            // torna alla vista "trainingSet sopra + orologio"
                            it.trainingCard.visibility = VISIBLE
                            keepOnlyTrainingSetVisible(current!!.sets)
                            placeTrainingSetAboveClock()

                            it.cardClock.visibility = VISIBLE
                            val isChildOfProgress = (it.cardClock.parent === it.progressTimer)
                            it.progressTimer.visibility = if (isChildOfProgress) VISIBLE else GONE

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

    // "MM:SS" -> secondi; 0 se null/vuota/formato errato
    private fun parseDurationSecsStrict(d: String?): Long {
        if (d.isNullOrBlank()) return 0L
        return try {
            val p = d.split(":")
            val mm = p.getOrNull(0)?.toLongOrNull() ?: 0L
            val ss = p.getOrNull(1)?.toLongOrNull() ?: 0L
            (mm * 60 + ss).coerceAtLeast(0)
        } catch (_: Exception) { 0L }
    }

    // recupero "MM:SS" -> secondi; default 30s
    private fun parseMinSec(s: String?): Long {
        if (s.isNullOrBlank()) return 30L
        return try {
            val p = s.split(":")
            val mm = p.getOrNull(0)?.toLongOrNull() ?: 0L
            val ss = p.getOrNull(1)?.toLongOrNull() ?: 0L
            (mm * 60 + ss).coerceAtLeast(0)
        } catch (_: Exception) { 30L }
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
