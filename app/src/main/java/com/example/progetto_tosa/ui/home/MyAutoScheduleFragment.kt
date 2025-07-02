package com.example.progetto_tosa.ui.home

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.FragmentMyAutoScheduleBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

class MyAutoScheduleFragment : Fragment() {

    private var _binding: FragmentMyAutoScheduleBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val activeListeners = mutableListOf<ListenerRegistration>()

    private lateinit var selectedDateId: String

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

        // Ottieni la data passata
        selectedDateId = requireArguments().getString("selectedDate")
            ?: error("selectedDate mancante")

        // Giorno della settimana
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
        binding.subtitlePPPPPPPPROOOOOVA.apply {
            visibility = VISIBLE
            text = "SCHEDA DI $dayDisplayName"
        }

        // Pulsante "Aggiungi esercizio"
        binding.btnFillSchedule.apply {
            visibility = VISIBLE
            setOnClickListener {
                findNavController().navigate(
                    R.id.action_fragment_my_auto_schedule_to_fragment_workout,
                    Bundle().apply { putString("selectedDate", selectedDateId) }
                )
            }
        }

        // Categorie e muscoli
        val categories = mapOf(
            "bodybuilding" to listOf("petto", "gambe", "spalle", "schiena", "bicipiti", "tricipiti"),
            "cardio" to listOf("cardio1", "cardio2"),
            "corpo_libero" to listOf("libero1", "libero2"),
            "stretching" to listOf("stretch1", "stretch2")
        )

        // Inizializza contatori e click listener
        categories.forEach { (category, muscoli) ->
            val subtitleView = when (category) {
                "bodybuilding" -> binding.subtitleBodyBuilding
                "cardio" -> binding.subtitleCardio
                "corpo_libero" -> binding.subtitleCorpoLibero
                "stretching" -> binding.subtitleStretching
                else -> null
            }
            subtitleView?.let {
                initExerciseCountListener(category, muscoli, it)
            }
            val container = when (category) {
                "bodybuilding" -> binding.bodybuildingDetailsContainer
                "cardio" -> binding.cardioDetailsContainer
                "corpo_libero" -> binding.corpoliberoDetailsContainer
                "stretching" -> binding.stretchingDetailsContainer
                else -> null
            }
            container?.let { cont ->
                when (category) {
                    "bodybuilding" -> binding.btnBodybuilding
                    "cardio" -> binding.btnCardio
                    "corpo_libero" -> binding.btnCorpoLibero
                    "stretching" -> binding.btnStretching
                    else -> null
                }?.setOnClickListener {
                    toggleAndPopulate(cont, category, muscoli)
                }
            }
        }
    }

    private fun initExerciseCountListener(
        category: String,
        muscoli: List<String>,
        subtitleView: TextView
    ) {
        val counts = muscoli.associateWith { 0 }.toMutableMap()
        muscoli.forEach { m ->
            val listener = db.collection("schede_giornaliere")
                .document(selectedDateId)
                .collection(category)
                .document(m)
                .collection("esercizi")
                .addSnapshotListener { snap, err ->
                    counts[m] = if (err != null) 0 else (snap?.size() ?: 0)
                    val total = counts.values.sum()
                    subtitleView.text = if (total == 1) "$total esercizio" else "$total esercizi"
                }
            activeListeners.add(listener)
        }
    }

    private fun toggleAndPopulate(
        container: LinearLayout,
        category: String,
        muscoli: List<String>
    ) {
        if (container.visibility == GONE) {
            container.visibility = VISIBLE
            container.removeAllViews()
            muscoli.forEach { m ->
                db.collection("schede_giornaliere")
                    .document(selectedDateId)
                    .collection(category)
                    .document(m)
                    .collection("esercizi")
                    .get()
                    .addOnSuccessListener { snap ->
                        if (snap.isEmpty) return@addOnSuccessListener
                        val byMuscle = snap.documents.groupBy {
                            it.getString("muscoloPrincipale") ?: "Altro"
                        }
                        byMuscle.forEach { (muscle, exercises) ->
                            // Header muscolo
                            val header = TextView(requireContext()).apply {
                                text = muscle.uppercase()
                                typeface = Typeface.DEFAULT_BOLD
                                setTextColor(ContextCompat.getColor(requireContext(), R.color.sky))
                                setPadding(40, 30, 0, 0)
                            }
                            container.addView(header)
                            // Divider
                            container.addView(View(requireContext()).apply {
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT, 3
                                ).apply { setMargins(40, 16, 40, 16) }
                                setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.dark_gray))
                            })
                            // Esercizi
                            exercises.forEach { doc ->
                                val nome = doc.getString("nomeEsercizio") ?: doc.id
                                val serie = doc.getLong("numeroSerie")?.toString() ?: "0"
                                val rep = doc.getLong("numeroRipetizioni")?.toString() ?: "0"
                                val item = TextView(requireContext()).apply {
                                    text = "○ $nome  |  Serie: $serie  •  Rep: $rep"
                                    setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                                    setPadding(36, 4, 8, 16)
                                }
                                container.addView(item)
                            }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Errore nel caricamento degli esercizi", Toast.LENGTH_SHORT).show()
                    }
            }
        } else {
            container.visibility = GONE
        }
    }

    override fun onDestroyView() {
        activeListeners.forEach { it.remove() }
        activeListeners.clear()
        _binding = null
        super.onDestroyView()
    }
}
