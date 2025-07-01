package com.example.progetto_tosa.ui.home

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.FragmentMyTrainerScheduleBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MyTrainerSchedule : Fragment() {

    private var _binding: FragmentMyTrainerScheduleBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val activeListeners = mutableListOf<ListenerRegistration>()

    private val selectedUser: String by lazy {
        requireArguments().getString("selectedUser")
            ?: error("selectedUser mancante")
    }
    private val selectedDateId: String by lazy {
        requireArguments().getString("selectedDate")
            ?: error("selectedDate mancante")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyTrainerScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val b = binding

        // Imposta il titolo con la data selezionata
        val displayDate = android.icu.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            .format(android.icu.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                .parse(selectedDateId)!!)
        val today = android.icu.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            .format(java.util.Date())
        b.subtitlePPPPPPPPROOOOOVA.apply {
            visibility = VISIBLE
            text = if (displayDate == today)
                "LA MIA SCHEDA di oggi"
            else
                "LA MIA SCHEDA DEL: $displayDate"
        }

        // 0) Documento utente sotto schede_del_pt
        val userDocRef: DocumentReference = db
            .collection("schede_del_pt")
            .document(selectedUser)

        // 1) Subcollection “dates” → doc(selectedDateId)
        val dateDocRef: DocumentReference = userDocRef
            .collection("dates")
            .document(selectedDateId)

        // 2) (opzionale) crea un timestamp se il doc non esiste
        dateDocRef
            .get()
            .addOnSuccessListener { snap ->
                if (!snap.exists()) {
                    dateDocRef.set(mapOf("date" to Timestamp.now()))
                }
            }

        // Bottone “Aggiungi esercizio” passando entrambi gli argomenti
        b.btnFillSchedule.apply {
            visibility = VISIBLE
            setOnClickListener {
                findNavController().navigate(
                    R.id.action_fragment_my_trainer_schedule_to_fragment_workout,
                    Bundle().apply {
                        putString("selectedUser", selectedUser)
                        putString("selectedDate", selectedDateId)
                    }
                )
            }
        }

        // Inizializza i contatori sulle varie categorie
        initExerciseCountListener(
            dateDocRef,
            "bodybuilding",
            listOf("petto","gambe","spalle","dorso","bicipiti","tricipiti"),
            b.subtitleBodyBuilding
        )
        initExerciseCountListener(
            dateDocRef,
            "cardio",
            listOf("cardio1","cardio2"),
            b.subtitleCardio
        )
        initExerciseCountListener(
            dateDocRef,
            "corpo_libero",
            listOf("libero1","libero2"),
            b.subtitleCorpoLibero
        )
        initExerciseCountListener(
            dateDocRef,
            "stretching",
            listOf("stretch1","stretch2"),
            b.subtitleStretching
        )

        // Toggle + popolamento dettagli al click
        b.btnBodybuilding.setOnClickListener {
            toggleAndPopulate(
                dateDocRef,
                b.bodybuildingDetailsContainer,
                "bodybuilding",
                listOf("petto","gambe","spalle","dorso","bicipiti","tricipiti")
            )
        }
        b.btnCardio.setOnClickListener {
            toggleAndPopulate(
                dateDocRef,
                b.cardioDetailsContainer,
                "cardio",
                listOf("cardio1","cardio2")
            )
        }
        b.btnCorpoLibero.setOnClickListener {
            toggleAndPopulate(
                dateDocRef,
                b.corpoliberoDetailsContainer,
                "corpo_libero",
                listOf("libero1","libero2")
            )
        }
        b.btnStretching.setOnClickListener {
            toggleAndPopulate(
                dateDocRef,
                b.stretchingDetailsContainer,
                "stretching",
                listOf("stretch1","stretch2")
            )
        }
    }

    override fun onDestroyView() {
        activeListeners.forEach { it.remove() }
        activeListeners.clear()
        _binding = null
        super.onDestroyView()
    }

    private fun initExerciseCountListener(
        dateRef: DocumentReference,
        category: String,
        muscoli: List<String>,
        subtitleView: TextView
    ) {
        val counts = muscoli.associateWith { 0 }.toMutableMap()
        muscoli.forEach { m ->
            val listener = dateRef
                .collection(category)
                .document(m)
                .collection("esercizi")
                .addSnapshotListener { snap, err ->
                    counts[m] = if (err != null) 0 else (snap?.documents?.size ?: 0)
                    val total = counts.values.sum()
                    subtitleView.text =
                        if (total == 1) "$total esercizio" else "$total esercizi"
                }
            activeListeners.add(listener)
        }
    }

    private fun toggleAndPopulate(
        dateRef: DocumentReference,
        container: LinearLayout,
        category: String,
        lista: List<String>
    ) {
        if (container.visibility == GONE) {
            container.visibility = VISIBLE
            container.removeAllViews()

            lista.forEach { m ->
                dateRef
                    .collection(category)
                    .document(m)
                    .collection("esercizi")
                    .get()
                    .addOnSuccessListener { snap ->
                        if (snap.isEmpty) return@addOnSuccessListener

                        val header = TextView(requireContext()).apply {
                            text = m.uppercase()
                            typeface = Typeface.DEFAULT_BOLD
                            setTextColor(ContextCompat.getColor(requireContext(), R.color.sky))
                            setPadding(40, 30, 0, 0)
                            textSize = 20f
                        }
                        container.addView(header)

                        val divider = View(requireContext()).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, 3
                            ).apply { setMargins(40, 16, 40, 16) }
                            setBackgroundColor(
                                ContextCompat.getColor(requireContext(), R.color.dark_gray)
                            )
                        }
                        container.addView(divider)

                        snap.documents.forEach { doc ->
                            val nome = doc.getString("nomeEsercizio") ?: doc.id
                            val serie = doc.getLong("numeroSerie")?.toString() ?: "0"
                            val rep = doc.getLong("numeroRipetizioni")?.toString() ?: "0"

                            TextView(requireContext()).apply {
                                text = nome
                                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                                setPadding(18, 0, 8, 4)
                                textSize = 16f
                            }.also { container.addView(it) }

                            TextView(requireContext()).apply {
                                text = "○ Ripetizioni: $rep, Serie: $serie"
                                setTextColor(ContextCompat.getColor(requireContext(), R.color.light_gray))
                                setPadding(36, 0, 8, 16)
                                textSize = 14f
                            }.also { container.addView(it) }
                        }
                    }
            }
        } else {
            container.visibility = GONE
        }
    }
}
