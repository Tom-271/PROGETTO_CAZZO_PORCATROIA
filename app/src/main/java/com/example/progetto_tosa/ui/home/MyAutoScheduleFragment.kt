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

/**
 * Fragment che mostra la schedulazione automatica degli esercizi per la data selezionata.
 * Gestisce il rendering degli esercizi, il loro completamento e invia una notifica al termine.
 */
class MyAutoScheduleFragment : Fragment(R.layout.fragment_my_auto_schedule) {

    // Binding per il layout fragment_my_auto_schedule.xml
    private var _binding: FragmentMyAutoScheduleBinding? = null
    private val binding get() = _binding!!

    // ViewModel specifico, inizializzato con la data selezionata
    private lateinit var viewModel: MyAutoScheduleViewModel

    // Launcher per richiedere il permesso di invio notifiche su Android 13+
    private val notifLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Se permesso concesso, invia la notifica di completamento
        if (granted) sendNotification()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inizializzo il binding
        _binding = FragmentMyAutoScheduleBinding.inflate(inflater, container, false)
        // Creo il canale per le notifiche di completamento (Android O+)
        createNotificationChannel()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Recupero l'ID della data passata tramite Bundle
        val dateId = requireArguments().getString("selectedDate")
            ?: error("selectedDate missing")

        // Factory per creare il ViewModel con il parametro dateId
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

        // DataBinding tra ViewModel e layout
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        // Quando il nome del giorno diventa disponibile, mostro il sottotitolo
        viewModel.dayName.observe(viewLifecycleOwner) {
            binding.subtitleAllExercises.visibility = VISIBLE
        }

        // Ricostruisco dinamicamente la lista degli esercizi
        viewModel.exercises.observe(viewLifecycleOwner) { renderExercises(it) }

        // Gestisco l'evento di completamento degli esercizi:
        // ignoro il primo valore, poi se remaining==0 invio la notifica
        var initialized = false
        viewModel.remaining.observe(viewLifecycleOwner) { rem ->
            if (!initialized) {
                initialized = true
            } else if (rem == 0) {
                sendNotification()
            }
        }

        // Click sul cronometro: naviga al fragment CronoTimer
        binding.chrono.setOnClickListener {
            findNavController().navigate(
                R.id.action_fragment_my_auto_schedule_to_navigation_cronotimer
            )
        }

        // Click sul pulsante per riempire la scheda: naviga al fragment Workout
        binding.btnFillSchedule.setOnClickListener {
            findNavController().navigate(
                R.id.action_fragment_my_auto_schedule_to_fragment_workout,
                Bundle().apply { putString("selectedDate", dateId) }
            )
        }
    }

    /**
     * Renderizza la lista degli esercizi raggruppati per categoria.
     * Ogni categoria ha un header e le righe esercizio con toggle per dettagli.
     */
    private fun renderExercises(exs: List<Triple<String, String, String>>) {
        val container = binding.allExercisesContainer
        // Pulisce vista precedente
        container.removeAllViews()

        exs.groupBy { it.second }
            // Ordina categorie secondo un ordine personalizzato
            .toSortedMap(compareBy {
                listOf("bodybuilding", "cardio", "corpo-libero", "stretching").indexOf(it)
            })
            .forEach { (cat, list) ->
                // Header categoria
                val header = TextView(requireContext()).apply {
                    text = cat.uppercase()
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.orange))
                    textSize = 18f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setPadding(36, 24, 8, 8)
                }
                container.addView(header)
                // Righe di ogni esercizio nella categoria
                list.forEach { (nome, categoria, docId) ->
                    addExerciseRow(container, nome, categoria, docId)
                }
            }
    }

    /**
     * Aggiunge una riga esercizio con info collapsabile e azione di completamento.
     */
    private fun addExerciseRow(
        container: LinearLayout,
        nome: String,
        categoria: String,
        docId: String
    ) {
        // 1) Inflate della CardView per dettagli e la nasconde
        val cardView = layoutInflater.inflate(
            R.layout.exercise_info_card, container, false
        ) as CardView
        cardView.visibility = GONE

        // 2) Crea layout orizzontale per nome e pulsante toggle
        val itemLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(36, 4, 8, 16)
        }
        val text = TextView(requireContext()).apply {
            this.text = "○ $nome"
            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        val infoButton = ImageButton(requireContext()).apply {
            setImageResource(R.drawable.down)
            background = null
            tag = false
            // Toggle visibilità card
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

        // 3) Configura il TextView con tick interno nella card
        val setsRepsTv = cardView.findViewById<TextView>(R.id.cardSetsReps).apply {
            setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.tick, 0)
            compoundDrawablePadding = 16
        }

        // 4) Al click sul tick, chiama ViewModel per marcare come fatto
        setsRepsTv.setOnClickListener {
            viewModel.markExerciseDone(categoria, docId, {
                Toast.makeText(requireContext(), "Esercizio terminato!", Toast.LENGTH_SHORT).show()
                container.removeView(itemLayout)
                container.removeView(cardView)
            }, {
                Toast.makeText(requireContext(), "Errore eliminazione", Toast.LENGTH_SHORT).show()
            })
        }

        // 5) Aggiunge la card subito dopo la riga principale
        container.addView(cardView)
    }

    /**
     * Crea il canale di notifica per i messaggi di completamento (Android O+)
     */
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

    /**
     * Invia la notifica di completamento della scheda,
     * richiede permesso su Android 13+ se necessario
     */
    private fun sendNotification() {
        // Controllo permesso POST_NOTIFICATIONS su Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Se non concesso, richiedo permesso
            notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        // Costruisco e invio la notifica
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
        // Pulisce il binding per evitare memory leaks
        _binding = null
    }
}
