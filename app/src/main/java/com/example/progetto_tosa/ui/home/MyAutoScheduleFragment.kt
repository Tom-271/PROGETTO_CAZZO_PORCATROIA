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
import androidx.navigation.fragment.findNavController
import androidx.fragment.app.Fragment
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.FragmentMyAutoScheduleBinding
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

class MyAutoScheduleFragment : Fragment() {

    private var _binding: FragmentMyAutoScheduleBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val activeListeners = mutableListOf<ListenerRegistration>()

    private val selectedDateId: String by lazy {
        requireArguments().getString("selectedDate")
            ?: error("selectedDate mancante")
    }

    // mappa per conteggi
    private val countsMap = mutableMapOf<String, MutableMap<String, Int>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyAutoScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val b = binding

        // imposta data
        val displayDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            .format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDateId)!!)
        val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        b.subtitlePPPPPPPPROOOOOVA.apply {
            visibility = VISIBLE
            text =
                if (displayDate == today) "LA MIA SCHEDA di oggi" else "LA MIA SCHEDA DEL: $displayDate"
        }

        // assicurati documento
        db.collection("schede_giornaliere").document(selectedDateId)
            .get()
            .addOnSuccessListener { snap -> if (!snap.exists()) snap.reference.set(mapOf("date" to Timestamp.now())) }

        // bottone aggiungi generico
        b.btnFillSchedule.apply {
            visibility = VISIBLE
            setOnClickListener {
                findNavController().navigate(
                    R.id.action_fragment_my_auto_schedule_to_fragment_workout,
                    Bundle().apply { putString("selectedDate", selectedDateId) }
                )
            }
        }

        // inizializza contatori
        initExerciseCountListener(
            "bodybuilding",
            listOf("petto", "gambe", "spalle", "dorso", "bicipiti", "tricipiti"),
            b.subtitleBodyBuilding
        )
        initExerciseCountListener("cardio", listOf("cardio1", "cardio2"), b.subtitleCardio)
        initExerciseCountListener(
            "corpo_libero",
            listOf("libero1", "libero2"),
            b.subtitleCorpoLibero
        )
        initExerciseCountListener(
            "stretching",
            listOf("stretch1", "stretch2"),
            b.subtitleStretching
        )

        // setup toggle e popolamento al click
        b.btnBodybuilding.setOnClickListener {
            toggleAndPopulate(
                b.bodybuildingDetailsContainer,
                "bodybuilding",
                listOf("petto", "gambe", "spalle", "dorso", "bicipiti", "tricipiti")
            )
        }
        b.btnCardio.setOnClickListener {
            toggleAndPopulate(b.cardioDetailsContainer, "cardio", listOf("cardio1", "cardio2"))
        }
        b.btnCorpoLibero.setOnClickListener {
            toggleAndPopulate(
                b.corpoliberoDetailsContainer,
                "corpo_libero",
                listOf("libero1", "libero2")
            )
        }
        b.btnStretching.setOnClickListener {
            toggleAndPopulate(
                b.stretchingDetailsContainer,
                "stretching",
                listOf("stretch1", "stretch2")
            )
        }

        // cronotimer
        b.fabAdd.setOnClickListener {
            findNavController().navigate(R.id.action_fragment_my_auto_schedule_to_navigation_cronotimer)
        }
    }

    override fun onDestroyView() {
        activeListeners.forEach { it.remove() }
        activeListeners.clear()
        _binding = null
        super.onDestroyView()
    }

    private fun initExerciseCountListener(
        category: String,
        muscoli: List<String>,
        subtitleView: TextView
    ) {
        // inizializza mappe contatori
        val counts = muscoli.associateWith { 0 }.toMutableMap()
        countsMap[category] = counts
        muscoli.forEach { m ->
            val listener = db.collection("schede_giornaliere")
                .document(selectedDateId)
                .collection(category)
                .document(m)
                .collection("esercizi")
                .addSnapshotListener { snap, err ->
                    counts[m] = if (err != null) 0 else (snap?.size() ?: 0)
                    val total = counts.values.sum()
                    subtitleView.text =
                        if (total == 1) "${total} esercizio" else "${total} esercizi"
                }
            activeListeners.add(listener)
        }
    }

    private fun toggleAndPopulate(
        container: LinearLayout,
        category: String,
        lista: List<String>
    ) {
        if (container.visibility == GONE) {
            container.visibility = VISIBLE
            container.removeAllViews()
            // Popola con fetch una tantum
            lista.forEach { m ->
                val colRef = db.collection("schede_giornaliere")
                    .document(selectedDateId)
                    .collection(category)
                    .document(m)
                    .collection("esercizi")
                colRef.get().addOnSuccessListener { snap ->
                    if (snap.isEmpty) return@addOnSuccessListener
                    // Header muscolo
                    val header = TextView(requireContext()).apply {
                        text = m.uppercase(Locale.getDefault())
                        typeface = Typeface.DEFAULT_BOLD
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.sky))
                        setPadding(40, 30, 0, 0)
                        textSize = 20f
                    }
                    container.addView(header)
                    // Divider
                    val divider = View(requireContext()).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 3
                        ).apply { setMargins(40, 16, 40, 16) }
                        setBackgroundColor(
                            ContextCompat.getColor(
                                requireContext(),
                                R.color.dark_gray
                            )
                        )
                    }
                    container.addView(divider)
                    // Esercizi
                    snap.documents.forEach { doc ->
                        val nome = doc.getString("nomeEsercizio") ?: doc.id
                        val serie = doc.getLong("numeroSerie")?.toString() ?: "0"
                        val rep = doc.getLong("numeroRipetizioni")?.toString() ?: "0"
                        // Nome esercizio
                        val nomeView = TextView(requireContext()).apply {
                            text = nome
                            setTextColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    android.R.color.white
                                )
                            )
                            setPadding(18, 0, 8, 4)
                            textSize = 16f
                        }
                        container.addView(nomeView)
                        // Dettagli
                        val detail = TextView(requireContext()).apply {
                            text = "○ Ripetizioni: $rep, Serie: $serie"
                            setTextColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.light_gray
                                )
                            )
                            textSize = 14f
                            setPadding(36, 0, 8, 16)
                        }
                        container.addView(detail)
                    }
                }
            }
        } else {
            container.visibility = GONE
        }
    }
}