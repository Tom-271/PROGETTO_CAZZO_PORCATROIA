// Fragment per gestire il programma automatico giornaliero di esercizi
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

// Definizione del Fragment che utilizza il layout fragment_my_auto_schedule
class MyAutoScheduleFragment : Fragment(R.layout.fragment_my_auto_schedule) {

    // Binding generato per accedere ai view binding del layout
    private var _binding: FragmentMyAutoScheduleBinding? = null
    private val binding get() = _binding!!

    // ViewModel associato per gestire la logica dei dati
    private lateinit var viewModel: MyAutoScheduleViewModel

    // Launcher per richiedere il permesso di notifica su Android 13 e successivi
    private val notifLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Se il permesso è stato concesso, invia la notifica di completamento
        if (granted) sendNotification()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflating del binding e creazione del canale di notifica
        _binding = FragmentMyAutoScheduleBinding.inflate(inflater, container, false)
        createNotificationChannel()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Estraggo l'ID della data selezionata dagli argomenti del Fragment
        val dateId = requireArguments().getString("selectedDate")
            ?: error("selectedDate missing")

        // Factory personalizzata per inizializzare il ViewModel con l'ID della data
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

        // Associazione del ViewModel al layout e setting del lifecycleOwner
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        // Osservo il nome del giorno e mostro il sottotitolo quando disponibile
        viewModel.dayName.observe(viewLifecycleOwner) {
            binding.subtitleAllExercises.visibility = VISIBLE
        }

        // Ricostruisco la lista di esercizi ogni volta che cambia
        viewModel.exercises.observe(viewLifecycleOwner) { renderExercises(it) }

        // Inizializziamo previousRemaining col valore corrente (che potrebbe essere 0)
           var previousRemaining = viewModel.remaining.value ?: 0
           viewModel.remaining.observe(viewLifecycleOwner) { rem ->
                    // mando notifica solo se prima c'erano esercizi (>0) e ora non ce ne sono più (==0)
                    if (previousRemaining > 0 && rem == 0) {
                            sendNotification()
                        }
                    previousRemaining = rem
                }

        // Navigazione al timer cronometro
        binding.chrono.setOnClickListener {
            findNavController().navigate(
                R.id.action_fragment_my_auto_schedule_to_navigation_cronotimer
            )
        }

        // Navigazione al fragment di compilazione scheda
        binding.btnFillSchedule.setOnClickListener {
            findNavController().navigate(
                R.id.action_fragment_my_auto_schedule_to_fragment_workout,
                Bundle().apply { putString("selectedDate", dateId) }
            )
        }
    }

    // Funzione per generare dinamicamente le view degli esercizi
    private fun renderExercises(exs: List<ScheduledExercise>) {
        val container = binding.allExercisesContainer
        container.removeAllViews()

        // Raggruppo gli esercizi per categoria e ordino secondo un criterio personalizzato
        exs.groupBy { it.categoria ?: "Altro" }
            .toSortedMap(compareBy<String> {
                listOf("bodybuilding", "cardio", "corpo-libero", "stretching").indexOf(it)
            })

            .forEach { (cat, list) ->
                // Header di sezione per categoria
                val header = TextView(requireContext()).apply {
                    text = cat.uppercase()
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.orange))
                    textSize = 18f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    setPadding(36, 24, 8, 8)
                }
                container.addView(header)

                // Aggiungo una riga per ogni esercizio della categoria
                list.forEach { exercise ->
                    addExerciseRow(container, exercise)
                }
            }
    }

    // Costruisce la riga di dettaglio di un singolo esercizio
    private fun addExerciseRow(container: LinearLayout, exercise: ScheduledExercise) {
        val nome = exercise.nome
        val categoria = exercise.categoria
        val docId = exercise.docId
        val sets = exercise.sets
        val reps = exercise.reps

        // Inflating della CardView nascosta di default
        val cardView = layoutInflater.inflate(
            R.layout.exercise_info_card, container, false
        ) as CardView
        cardView.visibility = GONE

        // Layout orizzontale per titolo e pulsante info
        val itemLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(36, 4, 8, 16)
        }

        // TextView che mostra il nome esercizio con punto elenco
        val text = TextView(requireContext()).apply {
            this.text = "○ $nome"
            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        // Pulsante per espandere/nascondere i dettagli nella CardView
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

        // Imposto sets e reps nella CardView con icona di check
        val setsRepsTv = cardView.findViewById<TextView>(R.id.cardSetsReps)
        setsRepsTv.text = "$sets set x $reps ripetizioni"
        setsRepsTv.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.tick, 0)
        setsRepsTv.compoundDrawablePadding = 16

        // Click su setsReps per segnare esercizio come completato
        setsRepsTv.setOnClickListener {
            viewModel.markExerciseDone(categoria, docId, {
                Toast.makeText(requireContext(), "Esercizio terminato!", Toast.LENGTH_SHORT).show()
                // Rimuovo la riga e la card dal container
                container.removeView(itemLayout)
                container.removeView(cardView)
            }, {
                Toast.makeText(requireContext(), "Errore eliminazione", Toast.LENGTH_SHORT).show()
            })
        }

        // Gestione del salvataggio del peso inserito dall'utente
        val weightInput = cardView.findViewById<EditText>(R.id.cardWeightInput)
        val saveButton = cardView.findViewById<Button>(R.id.cardSaveButton)

        exercise.peso?.let {
            // Se presente, precompilo il campo peso
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
                // Avviso se l'input è vuoto
                Toast.makeText(requireContext(), "Inserisci un valore", Toast.LENGTH_SHORT).show()
            }
        }

        // Aggiungo la CardView al container sotto la riga
        container.addView(cardView)
    }

    // Creazione del canale di notifica (necessario su API 26+)
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

    // Invia la notifica di completamento scheda
    private fun sendNotification() {
        // Richiesta permesso POST_NOTIFICATIONS su Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        // Costruzione e invio della notifica
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
        // Ripulisco il binding per evitare memory leak
        _binding = null
    }
}
