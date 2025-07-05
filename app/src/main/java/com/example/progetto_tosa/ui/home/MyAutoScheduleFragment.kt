package com.example.progetto_tosa.ui.home

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.FragmentMyAutoScheduleBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.*

class MyAutoScheduleFragment : Fragment() {

    private var _binding: FragmentMyAutoScheduleBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private lateinit var selectedDateId: String

    private val currentUserName: String?
        get() = requireActivity()
            .getSharedPreferences("user_data", Context.MODE_PRIVATE)
            .getString("saved_display_name", null)

    private val CHANNEL_ID = "completion_channel"
    private val notificationId = 1001

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyAutoScheduleBinding.inflate(inflater, container, false)
        createNotificationChannel()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectedDateId = requireArguments().getString("selectedDate")
            ?: error("selectedDate mancante")
        Log.d("MyAutoSchedule", "selectedDateId = $selectedDateId")

        displayDayOfWeek()

        binding.chrono.setOnClickListener {
            findNavController()
                .navigate(R.id.action_fragment_my_auto_schedule_to_navigation_cronotimer)
        }
        binding.btnFillSchedule.apply {
            visibility = VISIBLE
            setOnClickListener {
                findNavController().navigate(
                    R.id.action_fragment_my_auto_schedule_to_fragment_workout,
                    bundleOf("selectedDate" to selectedDateId)
                )
            }
        }

        populateUnifiedExerciseList()
    }

    private fun displayDayOfWeek() {
        val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .parse(selectedDateId)!!
        val dayOfWeek = Calendar.getInstance().apply { time = parsedDate }
            .get(Calendar.DAY_OF_WEEK)
        val dayDisplayName = when (dayOfWeek) {
            Calendar.MONDAY    -> "LUNEDÌ"
            Calendar.TUESDAY   -> "MARTEDÌ"
            Calendar.WEDNESDAY -> "MERCOLEDÌ"
            Calendar.THURSDAY  -> "GIOVEDÌ"
            Calendar.FRIDAY    -> "VENERDÌ"
            Calendar.SATURDAY  -> "SABATO"
            Calendar.SUNDAY    -> "DOMENICA"
            else               -> ""
        }
        binding.subtitleAllExercises.apply {
            visibility = VISIBLE
            text = "SCHEDA DI $dayDisplayName"
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Completion"
            val descriptionText = "Canale per notifica fine scheda"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val manager =
                requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun sendCompletionNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                0
            )
            return
        }

        val builder = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Bravo!")
            .setContentText("Hai terminato la scheda di oggi!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        NotificationManagerCompat.from(requireContext())
            .notify(notificationId, builder.build())
    }

    private fun populateUnifiedExerciseList() {
        val user = currentUserName ?: return
        val container = binding.allExercisesContainer
        container.removeAllViews()

        val categories = listOf("bodybuilding", "cardio", "corpo-libero", "stretching")
        val unifiedList = mutableListOf<Triple<String, String, String>>()
        var completedFetches = 0

        for (category in categories) {
            db.collection("schede_giornaliere")
                .document(user)
                .collection(selectedDateId)
                .document(category)
                .collection("esercizi")
                .get()
                .addOnSuccessListener { snap ->
                    for (doc in snap.documents) {
                        val nome = doc.getString("nomeEsercizio") ?: doc.id
                        unifiedList.add(Triple(nome, category, doc.id))
                    }
                }
                .addOnCompleteListener {
                    completedFetches++
                    if (completedFetches == categories.size) {
                        showUnifiedList(container, unifiedList)
                        if (unifiedList.isEmpty()) sendCompletionNotification()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(
                        requireContext(),
                        "Errore nel caricamento degli esercizi di $category",
                        Toast.LENGTH_SHORT
                    ).show()
                    // contiamo comunque il fetch fallito per non bloccare l’UI
                    completedFetches++
                    if (completedFetches == categories.size) {
                        showUnifiedList(container, unifiedList)
                        if (unifiedList.isEmpty()) sendCompletionNotification()
                    }
                }
        }
    }

    private fun showUnifiedList(
        container: LinearLayout,
        esercizi: List<Triple<String, String, String>>
    ) {
        val user = currentUserName ?: return
        container.removeAllViews()

        for ((nome, categoria, docId) in esercizi) {
            // riga principale
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
                setImageResource(R.drawable.info)
                background = null
                layoutParams = LinearLayout.LayoutParams(100, 100)
            }

            // card espandibile
            val cardView = LayoutInflater.from(requireContext())
                .inflate(R.layout.exercise_info_card, container, false) as CardView
            cardView.visibility = GONE

            val titleText    = cardView.findViewById<TextView>(R.id.cardExerciseTitle)
            val setsRepsText = cardView.findViewById<TextView>(R.id.cardSetsReps)
            val pesoInput    = cardView.findViewById<EditText>(R.id.cardWeightInput)
            val saveButton   = cardView.findViewById<Button>(R.id.cardSaveButton)

            titleText.text = nome

            // carica set/rip/peso
            db.collection("schede_giornaliere")
                .document(user)
                .collection(selectedDateId)
                .document(categoria)
                .collection("esercizi")
                .document(docId)
                .get()
                .addOnSuccessListener { doc ->
                    val serie = doc.getLong("numeroSerie")?.toString() ?: "-"
                    val rip   = doc.getLong("numeroRipetizioni")?.toString() ?: "-"
                    val peso  = doc.getDouble("peso")
                    setsRepsText.text = "Serie: $serie  |  Ripetizioni: $rip"
                    setsRepsText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.tick, 0)
                    setsRepsText.compoundDrawablePadding = 16

                    setsRepsText.setOnClickListener {
                        // elimina esercizio
                        db.collection("schede_giornaliere")
                            .document(user)
                            .collection(selectedDateId)
                            .document(categoria)
                            .collection("esercizi")
                            .document(docId)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(
                                    requireContext(),
                                    "Esercizio eliminato",
                                    Toast.LENGTH_SHORT
                                ).show()
                                container.removeView(itemLayout)
                                container.removeView(cardView)
                                if (container.childCount == 0) sendCompletionNotification()
                            }
                            .addOnFailureListener {
                                Toast.makeText(
                                    requireContext(),
                                    "Errore eliminazione",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                    if (peso != null) pesoInput.setText(peso.toString())
                }

            infoButton.setOnClickListener {
                cardView.visibility = if (cardView.visibility == GONE) VISIBLE else GONE
            }
            saveButton.setOnClickListener {
                val pesoVal = pesoInput.text.toString().toFloatOrNull()
                if (pesoVal == null) {
                    Toast.makeText(requireContext(), "Inserisci un peso valido", Toast.LENGTH_SHORT)
                        .show()
                    return@setOnClickListener
                }
                db.collection("schede_giornaliere")
                    .document(user)
                    .collection(selectedDateId)
                    .document(categoria)
                    .collection("esercizi")
                    .document(docId)
                    .set(mapOf("peso" to pesoVal), SetOptions.merge())
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Peso salvato", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Errore salvataggio", Toast.LENGTH_SHORT)
                            .show()
                    }
            }

            itemLayout.addView(text)
            itemLayout.addView(infoButton)
            container.addView(itemLayout)
            container.addView(cardView)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
