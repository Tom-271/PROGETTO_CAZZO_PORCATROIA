package com.example.progetto_tosa.ui.home

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.FragmentMyAutoScheduleBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MyAutoScheduleFragment : Fragment(R.layout.fragment_my_auto_schedule) {

    private var _binding: FragmentMyAutoScheduleBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MyAutoScheduleViewModel

    private var currentDate = Date()

    //timer generico (se usato altrove)
    private var totalSecs = 30L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyAutoScheduleBinding.inflate(inflater, container, false)
        createNotificationChannel()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val argDateId = requireArguments().getString("selectedDate") ?: return
        currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(argDateId) ?: Date()

        viewModel = ViewModelProvider(this)[MyAutoScheduleViewModel::class.java]
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        findNavController().currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<String>("returnDateId")
            ?.observe(viewLifecycleOwner) { returnedDateId ->
                if (!returnedDateId.isNullOrBlank()) {
                    currentDate = sdf.parse(returnedDateId) ?: Date()   // aggiorna la data corrente
                    viewModel.selectedDateId.value = returnedDateId     // vincola i salvataggi a quella scheda
                    setupBannerDate()                                   // aggiorna il banner
                }
            }


        val initialDate = argDateId  //commento: usa la stessa data già letta
        viewModel.selectedDateId.value = initialDate

        viewModel.dayName.observe(viewLifecycleOwner) {
            binding.subtitleAllExercises.visibility = VISIBLE
        }

        viewModel.exercises.observe(viewLifecycleOwner) { allExercises ->
            renderExercises(allExercises)

            //passa direttamente la lista (copiata) alla card
            val trainingQueueList = allExercises.toMutableList()

            setupTrainingQueueCard(
                trainingQueueCardLayout = binding.trainingQueueCard.trainingQueueCard,
                recyclerView = binding.recyclerTrainingQueue,
                scheduledList = trainingQueueList,
                viewModel = viewModel
            )
        }

        viewModel.isLoadingExercises.observe(viewLifecycleOwner) { isLoading ->
            binding.recyclerTrainingQueue.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
            binding.trainingQueueCard.trainingQueueCard.isEnabled = !isLoading
        }

        binding.chrono.setOnClickListener {
            findNavController().navigate(R.id.action_fragment_my_auto_schedule_to_navigation_cronotimer)
        }

        binding.btnFillSchedule.setOnClickListener {
            val bannerText = binding.subtitleAllExercises.text?.toString().orEmpty()
            val currentDateId = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentDate)

            viewModel.selectedDateId.value = currentDateId

            // ⬇️ chiave usata dall’observer al ritorno
            findNavController().currentBackStackEntry
                ?.savedStateHandle
                ?.set("returnDateId", currentDateId)

            findNavController().navigate(
                R.id.action_fragment_my_auto_schedule_to_fragment_workout,
                Bundle().apply {
                    putString("selectedDate", currentDateId)
                    putString("bannerTitle", bannerText)
                }
            )
        }

        binding.btnSetOrder.setOnClickListener {
            val allExercises = viewModel.exercises.value ?: emptyList()
            val orderedExercises = allExercises.sortedBy { it.ordine }

            val dialog = TrainingQueueDialogFragment.newInstance(orderedExercises, viewModel) { reorderedList ->
                if (reorderedList != allExercises) {
                    viewModel.saveReorderedExercises(reorderedList)
                    viewModel.selectedDateId.value = viewModel.selectedDateId.value
                } else {
                    Toast.makeText(requireContext(), "Ordine identico → non salvato", Toast.LENGTH_SHORT).show()
                }
            }

            dialog.show(parentFragmentManager, "training_queue_dialog")
        }

        binding.arrowLeft.setOnClickListener { changeDayBy(-1) }
        binding.arrowRight.setOnClickListener { changeDayBy(1) }

        setupBannerDate()
    }

    private fun changeDayBy(days: Int) {
        val calendar = java.util.Calendar.getInstance()
        calendar.time = currentDate
        calendar.add(java.util.Calendar.DATE, days)
        currentDate = calendar.time

        val newDateId = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentDate)
        viewModel.selectedDateId.value = newDateId

        setupBannerDate()
    }

    private fun setupBannerDate() {
        val dayNameFmt = SimpleDateFormat("EEEE", Locale.getDefault())
        val dayNumberFmt = SimpleDateFormat("d", Locale.getDefault())
        val monthFmt = SimpleDateFormat("MMMM", Locale.getDefault())

        val nomeGiorno = dayNameFmt.format(currentDate).uppercase(Locale.getDefault())
        val numeroGiorno = dayNumberFmt.format(currentDate)
        val mese = monthFmt.format(currentDate).uppercase(Locale.getDefault())

        val testo = "SCHEDA DI $nomeGiorno $numeroGiorno $mese"
        binding.subtitleAllExercises.text = testo
    }

    private fun setupTrainingQueueCard(
        trainingQueueCardLayout: LinearLayout,
        recyclerView: RecyclerView,
        scheduledList: MutableList<ScheduledExercise>,
        viewModel: MyAutoScheduleViewModel
    ) {
        lateinit var itemTouchHelper: ItemTouchHelper

        val adapter = TrainingQueueAdapter(scheduledList) { viewHolder ->
            itemTouchHelper.startDrag(viewHolder)
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = adapter
        }

        val callback = ItemMoveCallback(adapter)
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        trainingQueueCardLayout.setOnClickListener {
            recyclerView.visibility = if (recyclerView.isVisible) View.GONE else View.VISIBLE

            if (recyclerView.isGone) {
                val reorderedList = adapter.getCurrentList()
                if (reorderedList != scheduledList) {
                    viewModel.saveReorderedExercises(reorderedList)
                    Toast.makeText(requireContext(), "Ordine salvato", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Ordine identico → non salvato", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun renderExercises(exs: List<ScheduledExercise>) {
        val container = binding.allExercisesContainer
        container.removeAllViews()

        exs.groupBy { it.categoria ?: "Altro" }
            .toSortedMap(compareBy<String> {
                listOf("bodybuilding", "cardio", "corpo-libero", "stretching").indexOf(it)
            })
            .forEach { (cat, list) ->
                val header = TextView(requireContext()).apply {
                    text = cat.uppercase()
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.orange))
                    textSize = 18f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setPadding(36, 24, 8, 8)
                }
                container.addView(header)
                list.forEach { addExerciseRow(container, it) }
            }
    }

    private fun addExerciseRow(container: LinearLayout, exercise: ScheduledExercise) {
        val cardView = layoutInflater.inflate(R.layout.exercise_info_card, container, false) as CardView
        cardView.visibility = GONE

        val itemLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(36, 4, 8, 16)
        }

        val text = TextView(requireContext()).apply {
            text = "○ ${exercise.nome}"
            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val infoButton = ImageButton(requireContext()).apply {
            setImageResource(R.drawable.down)
            background = null
            tag = false
            setOnClickListener { v ->
                val isOpen = v.tag as Boolean
                cardView.visibility = if (isOpen) GONE else VISIBLE
                (v as ImageButton).setImageResource(if (isOpen) R.drawable.down else R.drawable.up)
                v.tag = !isOpen
            }
        }

        itemLayout.addView(text)
        itemLayout.addView(infoButton)
        container.addView(itemLayout)

        val setsRepsTv = cardView.findViewById<TextView>(R.id.cardSetsReps)
        setsRepsTv.text = "${exercise.sets} set x ${exercise.reps} ripetizioni"
        setsRepsTv.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.tick, 0)
        setsRepsTv.compoundDrawablePadding = 16

        setsRepsTv.setOnClickListener {
            viewModel.markExerciseDone(exercise.categoria, exercise.docId, {
                Toast.makeText(requireContext(), "Esercizio rimosso!", Toast.LENGTH_SHORT).show()
                container.removeView(itemLayout)
                container.removeView(cardView)
            }, {
                Toast.makeText(requireContext(), "Errore eliminazione", Toast.LENGTH_SHORT).show()
            })
        }

        //==== nuovi riferimenti ui ====

        val weightSpinner = cardView.findViewById<Spinner>(R.id.cardWeightInput)
        val recoverInput = cardView.findViewById<TextView>(R.id.cardRecoverInput)
        val durationInput = cardView.findViewById<TextView>(R.id.cardDurationInput)
        val saveButton = cardView.findViewById<Button>(R.id.cardSaveButton)
        val recoverCard = cardView.findViewById<LinearLayout>(R.id.trainingRecover)
        val weightCard = cardView.findViewById<LinearLayout>(R.id.trainingWeight)
        val durationCard = cardView.findViewById<LinearLayout>(R.id.trainingDuration)


        var pendingWeight: String? = null
        var pendingRecover: String? = null

        var changedWeight = false
        var changedRecover = false


        weightSpinner.setSelection(0)  // To reset spinner selection
        recoverInput.text = "Aggiungi"


        //==== spinner peso 0..150 con salvataggio immediato ====
        // ==== spinner peso 0..150 senza salvataggio immediato ====
        val weights = (0..150).map { it.toString() }
        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            weights
        ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        weightSpinner.adapter = spinnerAdapter

        exercise.peso?.toIntOrNull()?.let { saved ->
            if (saved in 0..150) weightSpinner.setSelection(saved)
        }

        var skipFirstSelection = true
        weightSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                if (skipFirstSelection) { skipFirstSelection = false; return }
                pendingWeight = weights[pos]
                changedWeight = true     // <- segna modifica, ma NON salva
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        //==== recupero: time picker + salvataggio immediato ====
        exercise.recupero?.let { recoverInput.text = it }
        recoverInput.isFocusable = false
        recoverInput.isClickable = true
        recoverInput.setOnClickListener {
            showTimePicker { formatted ->
                recoverInput.text = formatted
                pendingRecover = formatted
                changedRecover = true   // <- segna modifica, ma NON salva
            }
        }

        // ---- DURATA: init + UI ----
        val initialDuration = exercise.durata?.trim()
        val hasDuration = !initialDuration.isNullOrEmpty() && initialDuration != "00:00"

        var pendingDuration: String? = if (hasDuration) initialDuration else null
        var changedDuration = hasDuration

// mostra subito il valore nella TextView
        durationInput.text = if (hasDuration) initialDuration else "Aggiungi"
// mostra subito la card della durata se presente
        durationCard.visibility = if (hasDuration) VISIBLE else GONE

// non editabile da tastiera, ma cliccabile per aprire il picker
        durationInput.isFocusable = false
        durationInput.isClickable = true
        durationInput.setOnClickListener {
            showTimePicker { formatted ->
                durationInput.text = formatted
                pendingDuration = formatted
                changedDuration = true    // segna modifica, ma NON salva ancora
                durationCard.visibility = VISIBLE
            }
        }


        // ==== modalità modifica: avvio sempre in lettura, edit solo temporaneo ====
        exercise.modificaAbilitata = false

        var enterEditMode: (() -> Unit)? = null
        var returnToViewMode: (() -> Unit)? = null

        enterEditMode = {
            exercise.modificaAbilitata = true

            // abilita campi e mostra sezioni
            weightSpinner.isEnabled = true
            recoverInput.isEnabled = true
            durationInput.isEnabled = true
            recoverCard.visibility = VISIBLE
            durationCard.visibility = VISIBLE
            weightCard.visibility = VISIBLE

            saveButton.text = "salva"
            saveButton.setOnClickListener {
                var didSomething = false

                if (changedWeight && !pendingWeight.isNullOrBlank()) {
                    viewModel.saveExerciseWeight(
                        exercise.categoria, exercise.docId, pendingWeight!!,
                        { Toast.makeText(requireContext(),"Peso salvato!",Toast.LENGTH_SHORT).show() },
                        { Toast.makeText(requireContext(),"Errore salvataggio peso",Toast.LENGTH_SHORT).show() }
                    )
                    didSomething = true
                }
                if (changedRecover && !pendingRecover.isNullOrBlank()) {
                    viewModel.saveExerciseRecoverTime(
                        exercise.categoria, exercise.docId, pendingRecover!!,
                        { Toast.makeText(requireContext(),"Recupero salvato!",Toast.LENGTH_SHORT).show() },
                        { Toast.makeText(requireContext(),"Errore salvataggio recupero",Toast.LENGTH_SHORT).show() }
                    )
                    didSomething = true
                }
                if (changedDuration && !pendingDuration.isNullOrBlank()) {
                    viewModel.saveExerciseDuration(
                        exercise.categoria, exercise.docId, pendingDuration!!,
                        { Toast.makeText(requireContext(),"Durata salvata!",Toast.LENGTH_SHORT).show() },
                        { Toast.makeText(requireContext(),"Errore salvataggio durata",Toast.LENGTH_SHORT).show() }
                    )
                    didSomething = true
                }

                if (!didSomething) {
                    Toast.makeText(requireContext(), "Nessuna modifica da salvare", Toast.LENGTH_SHORT).show()
                    // reset e ritorno allo stato iniziale
                    changedWeight = false; changedRecover = false; changedDuration = false
                    pendingWeight = null;  pendingRecover = null;  pendingDuration = null
                    returnToViewMode?.invoke()
                } else {
                    // reset flag (resti in modifica; se vuoi uscire, invoca returnToViewMode?.invoke())
                    changedWeight = false; changedRecover = false; changedDuration = false
                    pendingWeight = null;  pendingRecover = null;  pendingDuration = null
                }
            }
        }

        returnToViewMode = {
            exercise.modificaAbilitata = false

            // disabilita campi
            weightSpinner.isEnabled = false
            recoverInput.isEnabled = false
            durationInput.isEnabled = false

            // nascondi sezioni se valori "vuoti"
            recoverCard.visibility  = if (recoverInput.text == "Aggiungi") GONE else VISIBLE
            durationCard.visibility = if (durationInput.text == "Aggiungi" || durationInput.text == "00:00") GONE else VISIBLE
            weightCard.visibility   = if (weightSpinner.selectedItemPosition == 0) GONE else VISIBLE

            // bottone -> "modifica" per rientrare in edit
            saveButton.text = "modifica"
            saveButton.setOnClickListener { enterEditMode?.invoke() }
        }

// stato iniziale
        returnToViewMode?.invoke()


        container.addView(cardView)
    }

    private fun showTimePicker(onTimeSelected: (String) -> Unit) {
        val pickerMin = NumberPicker(requireContext()).apply { minValue = 0; maxValue = 59; value = (totalSecs / 60).toInt() }
        val pickerSec = NumberPicker(requireContext()).apply { minValue = 0; maxValue = 59; value = (totalSecs % 60).toInt() }

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
                val total = pickerMin.value * 60 + pickerSec.value
                val mm = total / 60
                val ss = total % 60
                val formatted = String.format("%02d:%02d", mm, ss)
                onTimeSelected(formatted)
            }
            .setNegativeButton(R.string.annulla, null)
            .show()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                "completion_channel", "Completion", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Notifica fine scheda" }

            val manager = requireContext().getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(chan)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
