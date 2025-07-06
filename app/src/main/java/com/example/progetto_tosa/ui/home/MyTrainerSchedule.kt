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
import androidx.core.os.bundleOf
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

    private var _binding: FragmentMyTrainerScheduleBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val activeListeners = mutableListOf<ListenerRegistration>()

    private lateinit var dateId: String
    private lateinit var selectedUserId: String

    private var remainingExercises = 0

    private val CHANNEL_ID = "trainer_channel"
    private val notificationId = 2001
    private val NOTIF_PERMISSION_REQUEST = 101

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        //init binding e crea canale notifiche
        _binding = FragmentMyTrainerScheduleBinding.inflate(inflater, container, false)
        createNotificationChannel()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //recupera selectedUser e selectedDate
        selectedUserId = requireArguments().getString("selectedUser")
            ?: error("selectedUser mancante")
        dateId = requireArguments().getString("selectedDate")
            ?: error("selectedDate mancante")
        //formatta la data per il display
        val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dispDate = outFmt.format(inFmt.parse(dateId)!!)
        val today = outFmt.format(Date())
        //imposta il sottotitolo
        binding.subtitlePPPPPPPPROOOOOVA.apply {
            text = if (dispDate == today)
                "Oggi il PT ha preparato per me questa scheda:"
            else
                "La scheda che mi ha preparato il PT del: $dispDate"
            visibility = VISIBLE
        }
        //navigazione al cronotimer
        binding.chrono.setOnClickListener {
            findNavController().navigate(R.id.action_fragment_my_trainer_schedule_to_navigation_cronotimer)
        }
        //nasconde il btnFill per default
        binding.btnFillSchedule.visibility = GONE
        //controlla se utente è PT e mostra il bottone
        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    val isPT = doc.getBoolean("isPersonalTrainer") == true
                    if (isPT) {
                        showFillButton()
                        binding.chrono.visibility = GONE
                    } else {
                        db.collection("personal_trainers").document(uid)
                            .get()
                            .addOnSuccessListener { ptDoc ->
                                if (ptDoc.getBoolean("isPersonalTrainer") == true) {
                                    showFillButton()
                                }
                            }
                        binding.chrono.visibility = VISIBLE
                    }
                }
        }
        //carica gli esercizi
        populateUnifiedExerciseList()
    }

    private fun showFillButton() {
        //mostra e gestisci click del btnFillSchedule
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
        //recupera container inner della card
        val user = selectedUserId
        val cardInner = binding.exerciseTitle.parent as LinearLayout
        val unifiedList = mutableListOf<Triple<String, String, String>>()
        val categories = listOf("bodybuilding", "cardio", "corpo-libero", "stretching")
        var completedFetches = 0
        //per ogni categoria fetch da Firestore
        for (cat in categories) {
            db.collection("schede_del_pt")
                .document(user)
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
                        showUnifiedList(cardInner, unifiedList, user)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(
                        requireContext(),
                        "Errore caricamento categoria $cat",
                        Toast.LENGTH_SHORT
                    ).show()
                    completedFetches++
                    if (completedFetches == categories.size) {
                        showUnifiedList(cardInner, unifiedList, user)
                    }
                }
        }
    }

    private fun showUnifiedList(
        container: LinearLayout,
        esercizi: List<Triple<String, String, String>>,
        user: String
    ) {
        //pulisci il container e init contatore
        container.removeAllViews()
        remainingExercises = esercizi.size

        val eserciziPerCategoria = esercizi.groupBy { it.second }
        val categorieOrdinate = listOf("bodybuilding", "cardio", "corpo-libero", "stretching")

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

            for ((nome, _, docId) in listCat) {
                // riga esercizio + infoBtn
                val row = LinearLayout(requireContext()).apply {
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
                val infoBtn = ImageButton(requireContext()).apply {
                    setImageResource(R.drawable.info)
                    background = null
                    layoutParams = LinearLayout.LayoutParams(100, 100)
                }

                // card dettaglio
                val detailCard = LayoutInflater.from(requireContext())
                    .inflate(R.layout.exercise_info_card, container, false) as CardView
                detailCard.visibility = GONE

                val titleTv = detailCard.findViewById<TextView>(R.id.cardExerciseTitle)
                val setsRepsTv = detailCard.findViewById<TextView>(R.id.cardSetsReps)
                val weightInput = detailCard.findViewById<EditText>(R.id.cardWeightInput)
                val saveBtn = detailCard.findViewById<Button>(R.id.cardSaveButton)

                titleTv.text = nome

                // carica dati e aggiungi tick su setsRepsTv
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
                        setsRepsTv.text = "Serie: $serie  |  Ripetizioni: $rip"
                        setsRepsTv.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.tick, 0)
                        setsRepsTv.compoundDrawablePadding = 16

                        // elimina al click sul tick
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
                                        "Esercizio eliminato",
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
                                        "Errore eliminazione",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }

                        d.getDouble("peso")?.let { weightInput.setText(it.toString()) }
                    }

                // toggle dettaglio
                infoBtn.setOnClickListener {
                    detailCard.visibility = if (detailCard.visibility == GONE) VISIBLE else GONE
                }

                // salva peso
                saveBtn.setOnClickListener {
                    val peso = weightInput.text.toString().toFloatOrNull()
                    if (peso == null) {
                        Toast.makeText(
                            requireContext(),
                            "Inserisci un peso valido",
                            Toast.LENGTH_SHORT
                        ).show()
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
                            Toast.makeText(requireContext(), "Peso salvato", Toast.LENGTH_SHORT)
                                .show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(
                                requireContext(),
                                "Errore salvataggio",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                // aggiungi viste
                row.addView(text)
                row.addView(infoBtn)
                container.addView(row)
                container.addView(detailCard)
            }
        }
    }

    private fun createNotificationChannel() {
        //crea NotificationChannel per Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Trainer"
            val descriptionText = "Notifiche PT"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val manager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun sendCompletionNotification() {
        //verifica permission su Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                NOTIF_PERMISSION_REQUEST
            )
            return
        }
        //mostra notifica
        val builder = NotificationCompat.Builder(requireContext(), CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Scheda PT")
            .setContentText("Hai completato la scheda del PT!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        NotificationManagerCompat.from(requireContext()).notify(notificationId, builder.build())
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIF_PERMISSION_REQUEST && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            //ritenta la notifica
            sendCompletionNotification()
        }
    }

    override fun onDestroyView() {
        //rimuovi listener e pulisci binding
        activeListeners.forEach { it.remove() }
        activeListeners.clear()
        _binding = null
        super.onDestroyView()
    }
}
