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

        val factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return MyAutoScheduleViewModel(requireActivity().application, dateId) as T
            }
        }
        viewModel = ViewModelProvider(this, factory)
            .get(MyAutoScheduleViewModel::class.java)

        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        viewModel.dayName.observe(viewLifecycleOwner) {
            binding.subtitleAllExercises.visibility = VISIBLE
        }
        viewModel.exercises.observe(viewLifecycleOwner) { renderExercises(it) }

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

    private fun renderExercises(exs: List<Triple<String, String, String>>) {
        val container = binding.allExercisesContainer
        container.removeAllViews()
        exs.groupBy { it.second }
            .toSortedMap(compareBy {
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
                list.forEach { (nome, categoria, docId) ->
                    addExerciseRow(container, nome, categoria, docId)
                }
            }
    }

    private fun addExerciseRow(
        container: LinearLayout,
        nome: String,
        categoria: String,
        docId: String
    ) {
        // 1) Card dei dettagli (inizialmente nascosta)
        val cardView = layoutInflater.inflate(
            R.layout.exercise_info_card, container, false
        ) as CardView
        cardView.visibility = GONE

        // 2) Riga principale: solo nome + freccia
        val itemLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(36, 4, 8, 16)
        }
        val text = TextView(requireContext()).apply {
            this.text = "○ $nome"
            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            )
        }
        val infoButton = ImageButton(requireContext()).apply {
            setImageResource(R.drawable.down)
            background = null
            tag = false
            setOnClickListener { v ->
                val btn = v as ImageButton
                val open = btn.tag as Boolean
                cardView.visibility = if (open) GONE else VISIBLE
                btn.setImageResource(if (open) R.drawable.down else R.drawable.up)
                btn.tag = !open
            }
        }
        itemLayout.addView(text)
        itemLayout.addView(infoButton)
        container.addView(itemLayout)

        // 3) All’interno della card, trova il TextView delle serie/ripetizioni
        val setsRepsTv = cardView.findViewById<TextView>(R.id.cardSetsReps)
        setsRepsTv.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.tick, 0)
        setsRepsTv.compoundDrawablePadding = 16

        // 4) Al click sul tick dentro i dettagli
        setsRepsTv.setOnClickListener {
            viewModel.markExerciseDone(categoria, docId, {
                Toast.makeText(requireContext(),
                    "Esercizio terminato!", Toast.LENGTH_SHORT).show()
                container.removeView(itemLayout)
                container.removeView(cardView)
                // se era l'ultimo, notifico qui
                if (viewModel.remaining.value == 0) {
                    sendNotification()
                }
            }, {
                Toast.makeText(requireContext(),
                    "Errore eliminazione", Toast.LENGTH_SHORT).show()
            })
        }

        // 5) Aggiungo la card sotto la riga principale
        container.addView(cardView)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                "completion_channel", "Completion", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Notifica fine scheda" }
            requireContext().getSystemService(NotificationManager::class.java)
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
        val notif = NotificationCompat.Builder(
            requireContext(), "completion_channel"
        )
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
