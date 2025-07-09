package com.example.progetto_tosa.ui.home

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.os.bundleOf
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.FragmentMyTrainerScheduleBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.*

class MyTrainerSchedule : Fragment(R.layout.fragment_my_trainer_schedule) {

    // binding della view
    private var _binding: FragmentMyTrainerScheduleBinding? = null
    private val binding get() = _binding!!

    // istanze di firestore e auth
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // listener attivi da rimuovere in onDestroyView
    private val activeListeners = mutableListOf<ListenerRegistration>()

    // argomenti passati al fragment
    private lateinit var dateId: String
    private lateinit var selectedUserId: String

    // contatore esercizi rimanenti
    private var remainingExercises = 0

    // costanti per notifiche
    private val CHANNEL_ID = "trainer_channel"
    private val notificationId = 2001
    private val NOTIF_PERMISSION_REQUEST = 101

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // inizializza il binding e crea il canale notifiche
        _binding = FragmentMyTrainerScheduleBinding.inflate(inflater, container, false)
        createNotificationChannel()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // recupera gli argomenti selectedUser e selectedDate
        selectedUserId = requireArguments().getString("selectedUser")
            ?: error("selectedUser mancante")
        dateId = requireArguments().getString("selectedDate")
            ?: error("selectedDate mancante")

        // formatta la data per il display
        val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dispDate = outFmt.format(inFmt.parse(dateId)!!)
        val today = outFmt.format(Date())

        // imposta il sottotitolo in base alla data
        binding.subtitlePPPPPPPPROOOOOVA.apply {
            text = if (dispDate == today)
                "oggi il pt ha preparato per me questa scheda:"
            else
                "la scheda che mi ha preparato il pt del: $dispDate"
            visibility = VISIBLE
        }

        // di default nascondi il cronometro finché non verifichi il ruolo
        binding.chrono.visibility = GONE

        // navigazione al cronotimer
        binding.chrono.setOnClickListener {
            findNavController().navigate(
                R.id.action_fragment_my_trainer_schedule_to_navigation_cronotimer
            )
        }

        // nasconde per default il bottone di riempimento scheda
        binding.btnFillSchedule.visibility = GONE

        // verifica se l'utente è personal trainer
        auth.currentUser?.uid?.let { uid ->
            // primo controllo nella collezione users
            db.collection("users").document(uid)
                .get()
                .addOnSuccessListener { userDoc ->
                    val isUserPT = userDoc.getBoolean("isPersonalTrainer") == true
                    if (isUserPT) {
                        // è pt: mostra il bottone di fill e lascia nascosto il cronometro
                        showFillButton()
                    } else {
                        // non è in users, controllo nella collezione personal_trainers
                        db.collection("personal_trainers").document(uid)
                            .get()
                            .addOnSuccessListener { ptDoc ->
                                val isProfilePT = ptDoc.getBoolean("isPersonalTrainer") == true
                                if (isProfilePT) {
                                    // è pt: mostra fill, cronometro rimane nascosto
                                    showFillButton()
                                } else {
                                    // non è pt: mostra il cronometro
                                    binding.chrono.visibility = VISIBLE
                                }
                            }
                            .addOnFailureListener {
                                // in caso di errore, mostro il cronometro per sicurezza
                                binding.chrono.visibility = VISIBLE
                            }
                    }
                }
                .addOnFailureListener {
                    // se fetch fallisce, mostro il cronometro
                    binding.chrono.visibility = VISIBLE
                }
        }

        // popola la lista unificata di esercizi
        populateUnifiedExerciseList()
    }

    private fun showFillButton() {
        // mostra il bottone di fill e gestisce il click
        binding.btnFillSchedule.apply {
            visibility = VISIBLE
            setOnClickListener {
                findNavController().navigate(
                    R.id.action_fragment_my_trainer_schedule_to_fragment_workout,
                    bundleOf(
                        "selectedUser" to selectedUserId,
                        "selectedDate" to dateId
                    )
                )
            }
        }
    }

    private fun populateUnifiedExerciseList() {
        // container interno della card
        val container = binding.exerciseTitle.parent as LinearLayout
        val unifiedList = mutableListOf<Triple<String, String, String>>()
        val categories = listOf("bodybuilding", "cardio", "corpo-libero", "stretching")
        var completedFetches = 0

        // per ogni categoria recupera gli esercizi da firestore
        for (cat in categories) {
            db.collection("schede_del_pt")
                .document(selectedUserId)
                .collection(dateId)
                .document(cat)
                .collection("esercizi")
                .get()
                .addOnSuccessListener { snap ->
                    for (doc in snap.documents) {
                        val nome = doc.getString("nomeEsercizio") ?: doc.id
                        unifiedList.add(Triple(nome, cat, doc.id))
                    }
                }
                .addOnCompleteListener {
                    completedFetches++
                    if (completedFetches == categories.size) {
                        showUnifiedList(container, unifiedList, selectedUserId)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(
                        requireContext(),
                        "errore caricamento categoria $cat",
                        Toast.LENGTH_SHORT
                    ).show()
                    completedFetches++
                    if (completedFetches == categories.size) {
                        showUnifiedList(container, unifiedList, selectedUserId)
                    }
                }
        }
    }

    private fun showUnifiedList(
        container: LinearLayout,
        esercizi: List<Triple<String, String, String>>,
        user: String
    ) {
        // pulisci il container e inizializza il contatore
        container.removeAllViews()
        remainingExercises = esercizi.size

        // raggruppa gli esercizi per categoria
        val eserciziPerCategoria = esercizi.groupBy { it.second }
        val categorieOrdinate = listOf("bodybuilding", "cardio", "corpo-libero", "stretching")

        // per ogni categoria crea le view corrispondenti
        for (categoria in categorieOrdinate) {
            val listCat = eserciziPerCategoria[categoria] ?: continue

            // titolo categoria
            val titoloCategoria = TextView(requireContext()).apply {
                text = categoria.uppercase()
                setTextColor(ContextCompat.getColor(requireContext(), R.color.orange))
                textSize = 18f
                setTypeface(typeface, Typeface.BOLD)
                setPadding(36, 24, 8, 8)
            }
            container.addView(titoloCategoria)

            // riga e dettaglio per ogni esercizio
            for ((nome, _, docId) in listCat) {
                val row = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(36, 4, 8, 16)
                }
                val text = TextView(requireContext()).apply {
                    this.text = "○ $nome"
                    setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                val infoBtn = ImageButton(requireContext()).apply {
                    setImageResource(R.drawable.info)
                    background = null
                    layoutParams = LinearLayout.LayoutParams(100, 100)
                }
                val detailCard = LayoutInflater.from(requireContext())
                    .inflate(R.layout.exercise_info_card, container, false) as CardView
                detailCard.visibility = GONE

                // campi all'interno della card dettaglio
                val titleTv = detailCard.findViewById<TextView>(R.id.cardExerciseTitle)
                val setsRepsTv = detailCard.findViewById<TextView>(R.id.cardSetsReps)
                val weightInput = detailCard.findViewById<EditText>(R.id.cardWeightInput)
                val saveBtn = detailCard.findViewById<Button>(R.id.cardSaveButton)
                titleTv.text = nome

                // carica dati di serie, ripetizioni e peso
                db.collection("schede_del_pt")
                    .document(user)
                    .collection(dateId)
                    .document(categoria)
                    .collection("esercizi")
                    .document(docId)
                    .get()
                    .addOnSuccessListener { d ->
                        val serie = d.getLong("numeroSerie")?.toString() ?: "-"
                        val rip = d.getLong("numeroRipetizioni")?.toString() ?: "-"
                        setsRepsTv.text = "serie: $serie  |  ripetizioni: $rip"
                        setsRepsTv.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.tick, 0)
                        setsRepsTv.compoundDrawablePadding = 16

                        // click sul tick per eliminare
                        setsRepsTv.setOnClickListener {
                            db.collection("schede_del_pt")
                                .document(user)
                                .collection(dateId)
                                .document(categoria)
                                .collection("esercizi")
                                .document(docId)
                                .delete()
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        requireContext(),
                                        "esercizio eliminato",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    container.removeView(row)
                                    container.removeView(detailCard)
                                    remainingExercises--
                                    if (remainingExercises == 0) {
                                        sendCompletionNotification()
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(
                                        requireContext(),
                                        "errore eliminazione",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }

                        d.getDouble("peso")?.let { weightInput.setText(it.toString()) }
                    }

                // inizializza stato della card dettaglio (chiusa)
                infoBtn.tag = false
                infoBtn.setImageResource(R.drawable.down)

                // toggle di apertura/chiusura dettaglio
                infoBtn.setOnClickListener { view ->
                    val btn = view as ImageButton
                    val isOpen = btn.tag as Boolean
                    if (!isOpen) {
                        detailCard.visibility = VISIBLE
                        btn.setImageResource(R.drawable.up)
                    } else {
                        detailCard.visibility = GONE
                        btn.setImageResource(R.drawable.down)
                    }
                    btn.tag = !isOpen
                }

                // click per salvare il peso
                saveBtn.setOnClickListener {
                    val peso = weightInput.text.toString().toFloatOrNull()
                    if (peso == null) {
                        Toast.makeText(requireContext(), "inserisci un peso valido", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    db.collection("schede_del_pt")
                        .document(user)
                        .collection(dateId)
                        .document(categoria)
                        .collection("esercizi")
                        .document(docId)
                        .set(mapOf("peso" to peso), SetOptions.merge())
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "peso salvato", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "errore salvataggio", Toast.LENGTH_SHORT).show()
                        }
                }

                // aggiungi riga e dettaglio al container
                row.addView(text)
                row.addView(infoBtn)
                container.addView(row)
                container.addView(detailCard)
            }
        }
    }

    private fun createNotificationChannel() {
        // crea il notification channel per android o+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "trainer"
            val descriptionText = "notifiche pt"
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
        // verifica i permessi per android 13+
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
        // invia la notifica di completamento
        val builder = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("scheda pt")
            .setContentText("hai completato la scheda del pt!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        NotificationManagerCompat.from(requireContext())
            .notify(notificationId, builder.build())
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // se il permesso è stato concesso, reinvia la notifica
        if (requestCode == NOTIF_PERMISSION_REQUEST &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        ) {
            sendCompletionNotification()
        }
    }

    override fun onDestroyView() {
        // rimuovi listener e svuota binding
        activeListeners.forEach { it.remove() }
        activeListeners.clear()
        _binding = null
        super.onDestroyView()
    }
}
