package com.example.progetto_tosa.ui.home

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Typeface
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
import java.text.SimpleDateFormat
import java.util.*

class MyAutoScheduleFragment : Fragment() {

    private var _binding: FragmentMyAutoScheduleBinding? = null
    private val binding get() = _binding!!

    private var isInitialLoad = true
    private var remainingExercises = 0

    private val db = FirebaseFirestore.getInstance()
    private lateinit var selectedDateId: String

    private val currentUserName: String?
        get() = requireActivity()
            .getSharedPreferences("user_data", Context.MODE_PRIVATE)
            .getString("saved_display_name", null)

    private val CHANNEL_ID = "completion_channel"
    private val notificationId = 1001
    private val NOTIF_PERMISSION_REQUEST = 100

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //init binding e crea canale notifiche
        _binding = FragmentMyAutoScheduleBinding.inflate(inflater, container, false)
        createNotificationChannel()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //recupera selectedDateId dagli argomenti
        selectedDateId = requireArguments().getString("selectedDate")
            ?: error("selectedDate mancante")
        Log.d("MyAutoSchedule", "selectedDateId = $selectedDateId")
        //mostra giorno della settimana
        displayDayOfWeek()
        //navigazione al cronotimer
        binding.chrono.setOnClickListener {
            findNavController().navigate(R.id.action_fragment_my_auto_schedule_to_navigation_cronotimer)
        }
        //navigazione a workout se vuoto
        binding.btnFillSchedule.apply {
            visibility = VISIBLE
            setOnClickListener {
                findNavController().navigate(
                    R.id.action_fragment_my_auto_schedule_to_fragment_workout,
                    bundleOf("selectedDate" to selectedDateId)
                )
            }
        }
        //carica esercizi
        populateUnifiedExerciseList()
    }

    override fun onDestroyView() {
        //pulisci binding
        _binding = null
        super.onDestroyView()
    }

    private fun displayDayOfWeek() {
        //parsa la data e calcola il giorno
        val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .parse(selectedDateId)!!
        val calendar = Calendar.getInstance().apply { time = parsedDate }
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val dayName = when (dayOfWeek) {
            Calendar.MONDAY    -> "LUNEDÌ"
            Calendar.TUESDAY   -> "MARTEDÌ"
            Calendar.WEDNESDAY -> "MERCOLEDÌ"
            Calendar.THURSDAY  -> "GIOVEDÌ"
            Calendar.FRIDAY    -> "VENERDÌ"
            Calendar.SATURDAY  -> "SABATO"
            Calendar.SUNDAY    -> "DOMENICA"
            else               -> ""
        }
        //imposta testo subtitle
        binding.subtitleAllExercises.apply {
            visibility = VISIBLE
            text = "SCHEDA DI $dayName"
        }
    }

    private fun createNotificationChannel() {
        //crea NotificationChannel per Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Completion"
            val descriptionText = "Canale per notifica fine scheda"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val manager = requireContext()
                .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun sendCompletionNotification() {
        //verifica e richiedi permission su Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                NOTIF_PERMISSION_REQUEST
            )
            return
        }
        //costruisci e mostra notifica
        val builder = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Bravo!")
            .setContentText("Hai terminato la scheda di oggi!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        NotificationManagerCompat.from(requireContext())
            .notify(notificationId, builder.build())
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIF_PERMISSION_REQUEST &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        ) {
            //ritenta la notifica
            sendCompletionNotification()
        }
    }

    private fun populateUnifiedExerciseList() {
        //recupera utente e container
        val user = currentUserName ?: return
        val container = binding.allExercisesContainer
        container.removeAllViews()

        val categories = listOf("bodybuilding", "cardio", "corpo-libero", "stretching")
        val unifiedList = mutableListOf<Triple<String, String, String>>()
        var completedFetches = 0

        //per ogni categoria fetch da Firestore
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
                        showUnifiedList(container, unifiedList, user)
                        isInitialLoad = false
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(
                        requireContext(),
                        "Errore caricamento esercizi di $category",
                        Toast.LENGTH_SHORT
                    ).show()
                    completedFetches++
                    if (completedFetches == categories.size) {
                        showUnifiedList(container, unifiedList, user)
                        isInitialLoad = false
                    }
                }
        }
    }

    private fun showUnifiedList(
        container: LinearLayout,
        esercizi: List<Triple<String, String, String>>,
        user: String
    ) {
        //reset UI e init contatore
        container.removeAllViews()
        remainingExercises = esercizi.size

        val eserciziPerCategoria = esercizi.groupBy { it.second }
        val categorieOrdinate = listOf("bodybuilding", "cardio", "corpo-libero", "stretching")

        for (categoria in categorieOrdinate) {
            val listCategoria = eserciziPerCategoria[categoria] ?: continue
            //titolo categoria
            val titoloCategoria = TextView(requireContext()).apply {
                text = categoria.uppercase()
                setTextColor(ContextCompat.getColor(requireContext(), R.color.orange))
                textSize = 18f
                setTypeface(typeface, Typeface.BOLD)
                setPadding(36, 24, 8, 8)
            }
            container.addView(titoloCategoria)

            for ((nome, _, docId) in listCategoria) {
                //crea riga esercizio e bottone info
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
                    layoutParams = LinearLayout.LayoutParams(100, 100)
                }
                //influisce card dettagli
                val cardView = LayoutInflater.from(requireContext())
                    .inflate(R.layout.exercise_info_card, container, false) as CardView
                cardView.visibility = GONE

                val titleText    = cardView.findViewById<TextView>(R.id.cardExerciseTitle)
                val setsRepsText = cardView.findViewById<TextView>(R.id.cardSetsReps)
                val pesoInput    = cardView.findViewById<EditText>(R.id.cardWeightInput)
                val saveButton   = cardView.findViewById<Button>(R.id.cardSaveButton)

                titleText.text = nome

                //popola serie, ripetizioni e peso
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

                        //elimino esercizio al tap
                        setsRepsText.setOnClickListener {
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
                                        "Esercizio terminato!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    container.removeView(itemLayout)
                                    container.removeView(cardView)
                                    remainingExercises--
                                    if (remainingExercises == 0 && !isInitialLoad) {
                                        sendCompletionNotification()
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(
                                        requireContext(),
                                        "Errore eliminazione",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                        peso?.let { pesoInput.setText(it.toString()) }
                    }

                //mostra/nascondi card
                infoButton.tag = false  // false = card chiusa, true = card aperta
                infoButton.setOnClickListener {
                    // recupera lo stato corrente
                    val isOpen = it.tag as Boolean
                    if (!isOpen) {
                        // apro la card
                        cardView.visibility = VISIBLE
                        infoButton.setImageResource(R.drawable.up)
                    } else {
                        // chiudo la card
                        cardView.visibility = GONE
                        infoButton.setImageResource(R.drawable.down)
                    }
                    // inverto lo stato
                    it.tag = !isOpen
                }

                //salva peso su Firestore
                saveButton.setOnClickListener {
                    val pesoVal = pesoInput.text.toString().toFloatOrNull()
                    if (pesoVal == null) {
                        Toast.makeText(
                            requireContext(),
                            "Inserisci un peso valido",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }
                    db.collection("schede_giornaliere")
                        .document(user)
                        .collection(selectedDateId)
                        .document(categoria)
                        .collection("esercizi")
                        .document(docId)
                        .set(mapOf("peso" to pesoVal), com.google.firebase.firestore.SetOptions.merge())
                        .addOnSuccessListener {
                            Toast.makeText(
                                requireContext(),
                                "Peso salvato",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                requireContext(),
                                "Errore salvataggio",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }

                //aggiungi view al container
                itemLayout.addView(text)
                itemLayout.addView(infoButton)
                container.addView(itemLayout)
                container.addView(cardView)
            }
        }
    }
}
