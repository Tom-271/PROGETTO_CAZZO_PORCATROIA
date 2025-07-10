package com.example.progetto_tosa.ui.home

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.FragmentMyAutoScheduleBinding

class MyAutoScheduleFragment : Fragment(R.layout.fragment_my_auto_schedule) {

    private var _binding: FragmentMyAutoScheduleBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MyAutoScheduleViewModel

    // Launcher per permesso di notifica su Android 13+
    private val notifLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) sendNotification()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyAutoScheduleBinding.inflate(inflater, container, false)
        createNotificationChannel()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dateId = requireArguments().getString("selectedDate")
            ?: error("selectedDate missing")

        // ViewModelFactory per passare selectedDateId
        val factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return MyAutoScheduleViewModel(
                    requireActivity().application,
                    dateId
                ) as T
            }
        }
        viewModel = ViewModelProvider(this, factory)
            .get(MyAutoScheduleViewModel::class.java)

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        // Mostro il sottotitolo quando il giorno è pronto
        viewModel.dayName.observe(viewLifecycleOwner) {
            binding.subtitleAllExercises.visibility = VISIBLE
        }

        // Ricostruisco la lista quando cambia
        viewModel.exercises.observe(viewLifecycleOwner) { renderExercises(it) }

        // Flag per ignorare il primo valore di remaining all'apertura
        var initialized = false
        viewModel.remaining.observe(viewLifecycleOwner) { rem ->
            if (!initialized) {
                initialized = true
            } else if (rem == 0) {
                sendNotification()
            }
        }

        binding.chrono.setOnClickListener {
            findNavController().navigate(
                R.id.action_fragment_my_auto_schedule_to_navigation_cronotimer
            )
        }

        binding.btnFillSchedule.setOnClickListener {
            findNavController().navigate(
                R.id.action_fragment_my_auto_schedule_to_fragment_workout,
                Bundle().apply { putString("selectedDate", dateId) }
            )
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

                list.forEach { exercise ->
                    addExerciseRow(container, exercise)
                }
            }
    }


    private fun addExerciseRow(container: LinearLayout, exercise: ScheduledExercise) {
        val nome = exercise.nome
        val categoria = exercise.categoria
        val docId = exercise.docId
        val sets = exercise.sets
        val reps = exercise.reps

        val cardView = layoutInflater.inflate(
            R.layout.exercise_info_card, container, false
        ) as CardView
        cardView.visibility = GONE

        val itemLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(36, 4, 8, 16)
        }

        val text = TextView(requireContext()).apply {
            this.text = "○ $nome"
            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val infoButton = ImageButton(requireContext()).apply {
            setImageResource(R.drawable.down)
            background = null
            tag = false
            setOnClickListener { v ->
                val open = v.tag as Boolean
                cardView.visibility = if (open) GONE else VISIBLE
                (v as ImageButton).setImageResource(if (open) R.drawable.down else R.drawable.up)
                v.tag = !open
            }
        }

        itemLayout.addView(text)
        itemLayout.addView(infoButton)
        container.addView(itemLayout)

        val setsRepsTv = cardView.findViewById<TextView>(R.id.cardSetsReps)
        setsRepsTv.text = "$sets set x $reps ripetizioni"
        setsRepsTv.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.tick, 0)
        setsRepsTv.compoundDrawablePadding = 16


        setsRepsTv.setOnClickListener {
            viewModel.markExerciseDone(categoria, docId, {
                Toast.makeText(requireContext(), "Esercizio terminato!", Toast.LENGTH_SHORT).show()
                container.removeView(itemLayout)
                container.removeView(cardView)
            }, {
                Toast.makeText(requireContext(), "Errore eliminazione", Toast.LENGTH_SHORT).show()
            })
        }

        val weightInput = cardView.findViewById<EditText>(R.id.cardWeightInput)
        val saveButton = cardView.findViewById<Button>(R.id.cardSaveButton)

        exercise.peso?.let {
            weightInput.setText(it)
        }

        saveButton.setOnClickListener {
            val inputText = weightInput.text.toString().trim()
            if (inputText.isNotEmpty()) {
                viewModel.saveExerciseWeight(categoria, docId, inputText, {
                    Toast.makeText(requireContext(), "Peso salvato!", Toast.LENGTH_SHORT).show()
                }, {
                    Toast.makeText(requireContext(), "Errore salvataggio peso", Toast.LENGTH_SHORT).show()
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
                "completion_channel",
                "Completion",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Notifica fine scheda" }
            requireContext()
                .getSystemService(NotificationManager::class.java)
                .createNotificationChannel(chan)
        }
    }

    private fun sendNotification() {
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}