package com.example.progetto_tosa.ui.home

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
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
import androidx.core.view.isVisible
import androidx.core.view.isGone


class MyAutoScheduleFragment : Fragment(R.layout.fragment_my_auto_schedule) {

    private var _binding: FragmentMyAutoScheduleBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MyAutoScheduleViewModel

    private var currentDate = Date()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyAutoScheduleBinding.inflate(inflater, container, false)
        createNotificationChannel()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dateId = requireArguments().getString("selectedDate") ?: return

        viewModel = ViewModelProvider(this)[MyAutoScheduleViewModel::class.java]
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        val initialDate = requireArguments().getString("selectedDate") ?: return
        viewModel.selectedDateId.value = initialDate

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        viewModel.dayName.observe(viewLifecycleOwner) {
            binding.subtitleAllExercises.visibility = VISIBLE
        }

        viewModel.exercises.observe(viewLifecycleOwner) { allExercises ->
            renderExercises(allExercises)

            // passa direttamente la lista (copiata) alla card
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
            findNavController().navigate(
                R.id.action_fragment_my_auto_schedule_to_fragment_workout,
                Bundle().apply { putString("selectedDate", dateId) }
            )
        }

        binding.btnStartTraining.setOnClickListener {
            val allExercises = viewModel.exercises.value ?: emptyList()

            // ✅ ordina la lista prima di passarla alla dialog
            val orderedExercises = allExercises.sortedBy { it.ordine }

            val dialog = TrainingQueueDialogFragment.newInstance(orderedExercises, viewModel) { reorderedList ->
                if (reorderedList != allExercises) {
                    viewModel.saveReorderedExercises(reorderedList)
                    viewModel.selectedDateId.value = viewModel.selectedDateId.value
                }
            }

            dialog.show(parentFragmentManager, "training_queue_dialog")
        }


        binding.arrowLeft.setOnClickListener {
            changeDayBy(-1) // giorno precedente
        }

        binding.arrowRight.setOnClickListener {
            changeDayBy(1) // giorno successivo
        }

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
        val dayNameFmt = SimpleDateFormat("EEEE", Locale.getDefault()) // es. lunedì
        val dayNumberFmt = SimpleDateFormat("d", Locale.getDefault())   // es. 19
        val monthFmt = SimpleDateFormat("MMMM", Locale.getDefault())   // es. luglio

        val nomeGiorno = dayNameFmt.format(currentDate).uppercase(Locale.getDefault()) // → LUNEDÌ
        val numeroGiorno = dayNumberFmt.format(currentDate)                            // → 19
        val mese = monthFmt.format(currentDate).uppercase(Locale.getDefault())         // → LUGLIO

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
            recyclerView.visibility =
                if (recyclerView.isVisible) View.GONE else View.VISIBLE

            if (recyclerView.isGone) {
                val reorderedList = adapter.getCurrentList()
                if (reorderedList != scheduledList) {
                    viewModel.saveReorderedExercises(reorderedList)
                    Toast.makeText(requireContext(), "Ordine salvato", Toast.LENGTH_SHORT).show() // ✅ test
                } else {
                    Toast.makeText(requireContext(), "Ordine identico → non salvato", Toast.LENGTH_SHORT).show() // ✅ test
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
                Toast.makeText(requireContext(), "Esercizio terminato!", Toast.LENGTH_SHORT).show()
                container.removeView(itemLayout)
                container.removeView(cardView)
            }, {
                Toast.makeText(requireContext(), "Errore eliminazione", Toast.LENGTH_SHORT).show()
            })
        }

        val weightInput = cardView.findViewById<EditText>(R.id.cardWeightInput)
        val RecoverInput = cardView.findViewById<EditText>(R.id.cardRecoverInput)
        val saveButton = cardView.findViewById<Button>(R.id.cardSaveButton)

        exercise.peso?.let { weightInput.setText(it) }

        saveButton.setOnClickListener {
            val inputText = weightInput.text.toString().trim()
            val inputRecover = RecoverInput.text.toString().trim()
            if (inputText.isNotEmpty() || inputRecover.isNotEmpty()) {
                viewModel.saveExerciseWeight(exercise.categoria, exercise.docId, inputText, {
                    Toast.makeText(requireContext(), "Peso salvato!", Toast.LENGTH_SHORT).show()
                }, {
                    Toast.makeText(requireContext(), "Errore salvataggio peso", Toast.LENGTH_SHORT).show()
                })
                viewModel.saveExerciseRecoverTime(exercise.categoria, exercise.docId, inputText, {
                    Toast.makeText(requireContext(), "Tempo di recupero salvato!", Toast.LENGTH_SHORT).show()
                }, {
                    Toast.makeText(requireContext(), "Errore salvataggio recupero", Toast.LENGTH_SHORT).show()
                })
            } else {
                Toast.makeText(requireContext(), "Inserisci un valore", Toast.LENGTH_SHORT).show()
            }
        }

        container.addView(cardView)
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

    /*private fun sendNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            return
        }

        val notif = NotificationCompat.Builder(requireContext(), "completion_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Bravo!")
            .setContentText("Hai terminato la scheda di oggi!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(requireContext()).notify(1001, notif)
    }*/

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
