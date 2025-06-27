package com.example.progetto_tosa.ui.home

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
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

    // “yyyy-MM-dd” dalla selezione nel calendario
    private val selectedDateId: String by lazy {
        requireArguments().getString("selectedDate")
            ?: error("MyAutoScheduleFragment: selectedDate mancante")
    }

    // View refs
    private lateinit var subtitlePPPPPPPPROOOOOVA: TextView
    private lateinit var btnFillSchedule: Button
    private lateinit var subtitleBodyBuilding: TextView
    private lateinit var subtitleCardio: TextView
    private lateinit var subtitleCorpoLibero: TextView
    private lateinit var subtitleStretching: TextView
    private lateinit var btnBodybuilding: Button
    private lateinit var btnCardio: Button
    private lateinit var btnCorpoLibero: Button
    private lateinit var btnStretching: Button
    private lateinit var bodybuildingDetailsContainer: LinearLayout
    private lateinit var cardioDetailsContainer: LinearLayout
    private lateinit var corpoLiberoDetailsContainer: LinearLayout
    private lateinit var stretchingDetailsContainer: LinearLayout

    // Firestore
    private val db = FirebaseFirestore.getInstance()
    private val activeListeners = mutableListOf<ListenerRegistration>()

    // “Muscoli” per categoria
    private val bodybuildingMuscoli = listOf("petto", "gambe", "spalle", "dorso", "bicipiti", "tricipiti")
    private val cardioMuscoli = listOf("cardio1", "cardio2")
    private val corpoLiberoMuscoli = listOf("libero1", "libero2")
    private val stretchingMuscoli = listOf("stretch1", "stretch2")

    // Conteggi dinamici
    private val countsMap = mutableMapOf<TextView, MutableMap<String, Int>>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyAutoScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind delle view
        subtitlePPPPPPPPROOOOOVA = binding.subtitlePPPPPPPPROOOOOVA
        btnFillSchedule = binding.btnFillSchedule
        subtitleBodyBuilding = binding.subtitleBodyBuilding
        subtitleCardio = binding.subtitleCardio
        subtitleCorpoLibero = binding.subtitleCorpoLibero
        subtitleStretching = binding.subtitleStretching
        btnBodybuilding = binding.btnBodybuilding
        btnCardio = binding.btnCardio
        btnCorpoLibero = binding.btnCorpoLibero
        btnStretching = binding.btnStretching
        bodybuildingDetailsContainer = binding.bodybuildingDetailsContainer
        cardioDetailsContainer = binding.cardioDetailsContainer
        corpoLiberoDetailsContainer = binding.corpoliberoDetailsContainer
        stretchingDetailsContainer = binding.stretchingDetailsContainer

        // Formatta la data selezionata
        val displayDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            .format(
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .parse(selectedDateId)!!
            )


        // Imposta il tuo sottotitolo con la data dal calendario
        subtitlePPPPPPPPROOOOOVA.visibility = VISIBLE
        val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        if(displayDate == today)
        {
            subtitlePPPPPPPPROOOOOVA.text = "LA MIA SCHEDA di oggi"
        }
        else
        {
            subtitlePPPPPPPPROOOOOVA.text = "LA MIA SCHEDA DEL: $displayDate"
        }
        // Crea documento giorno se non esiste
        db.collection("schede_giornaliere")
            .document(selectedDateId)
            .get()
            .addOnSuccessListener { snap ->
                if (!snap.exists()) {
                    snap.reference.set(mapOf("date" to Timestamp.now()))
                }
            }

        // “Riempila ora!” — sempre visibile
        btnFillSchedule.visibility = VISIBLE
        btnFillSchedule.setOnClickListener {
            findNavController().navigate(
                R.id.action_fragment_my_auto_schedule_to_fragment_workout,
                bundleOf("selectedDate" to selectedDateId)
            )
        }

        // Espansioni + popolazioni
        btnBodybuilding.setOnClickListener {
            findNavController().navigate(
                R.id.action_fragment_my_auto_schedule_to_navigation_bodybuilding,
                bundleOf("selectedDate" to selectedDateId)
            )
        }
        btnCardio.setOnClickListener {
            toggleContainer(cardioDetailsContainer) {
                listenAndPopulate("cardio", cardioMuscoli, cardioDetailsContainer)
            }
        }
        btnCorpoLibero.setOnClickListener {
            toggleContainer(corpoLiberoDetailsContainer) {
                listenAndPopulate("corpo_libero", corpoLiberoMuscoli, corpoLiberoDetailsContainer)
            }
        }
        btnStretching.setOnClickListener {
            toggleContainer(stretchingDetailsContainer) {
                listenAndPopulate("stretching", stretchingMuscoli, stretchingDetailsContainer)
            }
        }

        // Conteggi dinamici
        listenToExerciseCount("bodybuilding", bodybuildingMuscoli, subtitleBodyBuilding)
        listenToExerciseCount("cardio", cardioMuscoli, subtitleCardio)
        listenToExerciseCount("corpo_libero", corpoLiberoMuscoli, subtitleCorpoLibero)
        listenToExerciseCount("stretching", stretchingMuscoli, subtitleStretching)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activeListeners.forEach { it.remove() }
        activeListeners.clear()
        _binding = null
    }

    private inline fun toggleContainer(container: LinearLayout, onOpen: () -> Unit) {
        if (container.visibility == GONE) {
            container.visibility = VISIBLE
            onOpen()
        } else {
            container.visibility = GONE
        }
    }

    private fun listenToExerciseCount(
        category: String,
        muscoli: List<String>,
        subtitleView: TextView
    ) {
        muscoli.forEach { muscolo ->
            val listener = db
                .collection("schede_giornaliere")
                .document(selectedDateId)
                .collection(category)
                .document(muscolo)
                .collection("esercizi")
                .addSnapshotListener { snap, err ->
                    if (err != null) {
                        subtitleView.text = "0 exercises"
                        return@addSnapshotListener
                    }
                    val map = countsMap.getOrPut(subtitleView) { mutableMapOf() }
                    map[muscolo] = snap?.size() ?: 0
                    subtitleView.text = "${map.values.sum()} exercises"
                }
            activeListeners.add(listener)
        }
    }

    private fun listenAndPopulate(
        category: String,
        listaElementi: List<String>,
        container: LinearLayout
    ) {
        container.removeAllViews()
        listaElementi.forEach { muscolo ->
            // Header
            val header = TextView(requireContext()).apply {
                text = muscolo.uppercase(Locale.getDefault())
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
                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.dark_gray))
            }
            container.addView(divider)

            // Listener su esercizi
            val esListener = db
                .collection("schede_giornaliere")
                .document(selectedDateId)
                .collection(category)
                .document(muscolo)
                .collection("esercizi")
                .addSnapshotListener { snap, _ ->
                    val startIdx = container.indexOfChild(divider) + 1
                    while (container.childCount > startIdx) {
                        container.removeViewAt(startIdx)
                    }
                    snap?.documents?.forEach { doc ->
                        val nome = doc.getString("nomeEsercizio") ?: doc.id
                        val serie = doc.getLong("numeroSerie")?.toString() ?: "0"
                        val rep = doc.getLong("numeroRipetizioni")?.toString() ?: "0"

                        val tv = TextView(requireContext()).apply {
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
                        val row = LinearLayout(requireContext()).apply {
                            orientation = LinearLayout.HORIZONTAL
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.WRAP_CONTENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { setMargins(80, 24, 8, 30) }
                        }
                        val detail = TextView(requireContext()).apply {
                            text = "○ Ripetizioni: $rep, Serie: $serie"
                            setTextColor(
                                ContextCompat.getColor(
                                    requireContext(),
                                    R.color.light_gray
                                )
                            )
                            textSize = 16f
                        }
                        val tick = Button(requireContext()).apply {
                            background = ContextCompat.getDrawable(context, R.drawable.tick)
                            layoutParams = LinearLayout.LayoutParams(50, 50).apply { setMargins(90, 0, 0, 24) }
                            setOnClickListener { doc.reference.delete() }
                        }
                        row.addView(detail)
                        row.addView(tick)
                        container.addView(tv)
                        container.addView(row)
                    }
                }
            activeListeners.add(esListener)
        }
    }
}
