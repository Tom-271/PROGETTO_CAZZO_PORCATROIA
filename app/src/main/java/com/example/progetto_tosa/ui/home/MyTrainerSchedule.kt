package com.example.progetto_tosa.ui.home

import android.graphics.Typeface
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyTrainerScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1) Leggo argomenti
        selectedUserId = requireArguments().getString("selectedUser")
            ?: error("selectedUser mancante")
        dateId = requireArguments().getString("selectedDate")
            ?: error("selectedDate mancante")

        // 2) Titolo con data
        val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dispDate = outFmt.format(inFmt.parse(dateId)!!)
        val today = outFmt.format(Date())
        binding.subtitlePPPPPPPPROOOOOVA.apply {
            text = if (dispDate == today)
                "Oggi il PT ha preparato per me questa scheda:"
            else
                "La scheda che mi ha preparato il PT del: $dispDate"
            visibility = VISIBLE
        }
        binding.chrono.setOnClickListener {

        findNavController().navigate(
            R.id.action_fragment_my_trainer_schedule_to_navigation_cronotimer,
        )
        }
        // 3) Mostro btnFillSchedule solo se l'utente è PT
        binding.btnFillSchedule.visibility = GONE
        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    val isPT = doc.getBoolean("isPersonalTrainer") == true
                    if (isPT)
                    {
                        showFillButton()
                        binding.chrono.visibility = GONE
                    }
                    else {
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

        // 4) Popolo **automaticamente** tutti gli esercizi del PT
        populateUnifiedExerciseList()
    }

    private fun showFillButton() {
        binding.btnFillSchedule.apply {
            visibility = VISIBLE
            setOnClickListener {
                findNavController().navigate(
                    R.id.action_fragment_my_trainer_schedule_to_fragment_workout,
                    Bundle().apply {
                        putString("selectedUser", selectedUserId)
                        putString("selectedDate", dateId)
                    }
                )
            }
        }
    }

    private fun populateUnifiedExerciseList() {
        val user = selectedUserId
        // Trovo il LinearLayout interno al CardView dove mettere gli esercizi
        val cardInner = binding.exerciseTitle.parent as LinearLayout

        val unifiedList = mutableListOf<Triple<String, String, String>>()  // nome, categoria, docId
        val categories = listOf("bodybuilding", "cardio", "corpo_libero", "stretching")
        var completedFetches = 0

        for (cat in categories) {
            db.collection("schede_del_pt")
                .document(user)
                .collection(dateId)
                .document(cat)
                .collection("esercizi")
                .orderBy("createdAt")
                .get()
                .addOnSuccessListener { snap ->
                    for (doc in snap.documents) {
                        val nome = doc.getString("nomeEsercizio") ?: doc.id
                        unifiedList.add(Triple(nome, cat, doc.id))
                    }
                    completedFetches++
                    if (completedFetches == categories.size) {
                        showUnifiedList(cardInner, unifiedList)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(
                        requireContext(),
                        "Errore caricamento categoria $cat",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun showUnifiedList(
        container: LinearLayout,
        esercizi: List<Triple<String, String, String>>
    ) {
        val user = selectedUserId
        container.removeAllViews()

        for ((nome, categoria, docId) in esercizi) {
            // Riga principale
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(36, 4, 8, 16)
            }
            val text = TextView(requireContext()).apply {
                text = "○ $nome"
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            }
            val infoBtn = ImageButton(requireContext()).apply {
                setImageResource(R.drawable.info)
                background = null
                layoutParams = LinearLayout.LayoutParams(100, 100)
            }

            // Card di dettaglio nascosta
            val detailCard = LayoutInflater.from(requireContext())
                .inflate(R.layout.exercise_info_card, container, false) as CardView
            detailCard.visibility = GONE

            val titleTv = detailCard.findViewById<TextView>(R.id.cardExerciseTitle)
            val setsRepsTv = detailCard.findViewById<TextView>(R.id.cardSetsReps)
            val weightInput = detailCard.findViewById<EditText>(R.id.cardWeightInput)
            val saveBtn = detailCard.findViewById<Button>(R.id.cardSaveButton)

            titleTv.text = nome

            // Carico serie/rip e peso
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
                    d.getDouble("peso")?.let { weightInput.setText(it.toString()) }
                }

            // Toggle visibilità del dettaglio
            infoBtn.setOnClickListener {
                detailCard.visibility = if (detailCard.visibility == GONE) VISIBLE else GONE
            }

            // Salvataggio peso
            saveBtn.setOnClickListener {
                val peso = weightInput.text.toString().toFloatOrNull()
                if (peso == null) {
                    Toast.makeText(requireContext(), "Inserisci un peso valido", Toast.LENGTH_SHORT)
                        .show()
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
                        Toast.makeText(requireContext(), "Peso salvato", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Errore salvataggio", Toast.LENGTH_SHORT)
                            .show()
                    }
            }

            row.addView(text)
            row.addView(infoBtn)
            container.addView(row)
            container.addView(detailCard)
        }
    }

    override fun onDestroyView() {
        activeListeners.forEach { it.remove() }
        activeListeners.clear()
        _binding = null
        super.onDestroyView()
    }
}
