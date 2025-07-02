package com.example.progetto_tosa.ui.home

import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.LayoutInflater
import android.view.View
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

        // 1) Leggi la data passata
        selectedDateId = requireArguments().getString("selectedDate")
            ?: error("selectedDate mancante")

        // 2) Ricava giorno della settimana per la label
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
            text = "SCHEDA DEL $dayDisplayName"
        }

        // 3) Pulsante "Aggiungi esercizio"
        binding.btnFillSchedule.apply {
            visibility = VISIBLE
            setOnClickListener {
                findNavController().navigate(
                    R.id.action_fragment_my_auto_schedule_to_fragment_workout,
                    Bundle().apply { putString("selectedDate", selectedDateId) }
                )
            }
        }

        // 4) Inizializza i contatori
        initExerciseCountListener(
            category = "bodybuilding",
            subtitleView = binding.subtitleBodyBuilding
        )
        initExerciseCountListener(
            category = "cardio",
            subtitleView = binding.subtitleCardio
        )
        initExerciseCountListener(
            category = "corpo_libero",
            subtitleView = binding.subtitleCorpoLibero
        )
        initExerciseCountListener(
            category = "stretching",
            subtitleView = binding.subtitleStretching
        )

        // 5) Toggle e dettagli
        binding.btnBodybuilding.setOnClickListener {
            toggleAndPopulate(
                container = binding.bodybuildingDetailsContainer,
                category = "bodybuilding"
            )
        }
        binding.btnCardio.setOnClickListener {
            toggleAndPopulate(
                container = binding.cardioDetailsContainer,
                category = "cardio"
            )
        }
        binding.btnCorpoLibero.setOnClickListener {
            toggleAndPopulate(
                container = binding.corpoliberoDetailsContainer,
                category = "corpo_libero"
            )
        }
        binding.btnStretching.setOnClickListener {
            toggleAndPopulate(
                container = binding.stretchingDetailsContainer,
                category = "stretching"
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
        category: String,
        subtitleView: TextView
    ) {
        val listener = db.collection("schede_giornaliere")
            .document(selectedDateId)
            .collection(category)
            .addSnapshotListener { snap, err ->
                val total = if (err != null) 0 else (snap?.size() ?: 0)
                subtitleView.text = if (total == 1) "$total esercizio" else "$total esercizi"
            }
        activeListeners.add(listener)
    }

    private fun toggleAndPopulate(
        container: LinearLayout,
        category: String
    ) {
        if (container.visibility == GONE) {
            container.visibility = VISIBLE
            container.removeAllViews()

            db.collection("schede_giornaliere")
                .document(selectedDateId)
                .collection(category)
                .get()
                .addOnSuccessListener { snap ->
                    if (snap.isEmpty) return@addOnSuccessListener

                    // Raggruppa esercizi per muscolo
                    val exercisesByMuscle = snap.documents.groupBy {
                        it.getString("muscoloPrincipale") ?: "Altro"
                    }

                    exercisesByMuscle.forEach { (muscle, exercises) ->
                        // Header muscolo
                        val header = TextView(requireContext()).apply {
                            text = muscle.uppercase()
                            typeface = Typeface.DEFAULT_BOLD
                            setTextColor(ContextCompat.getColor(requireContext(), R.color.sky))
                            setPadding(40, 30, 0, 0)
                            textSize = 20f
                        }
                        container.addView(header)

                        // Divider
                        val divider = View(requireContext()).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                3
                            ).apply { setMargins(40, 16, 40, 16) }
                            setBackgroundColor(
                                ContextCompat.getColor(requireContext(), R.color.dark_gray)
                            )
                        }
                        container.addView(divider)

                        // Esercizi
                        exercises.forEach { doc ->
                            val nome = doc.getString("nomeEsercizio") ?: doc.id
                            val serie = doc.getLong("numeroSerie")?.toString() ?: "0"
                            val rep = doc.getLong("numeroRipetizioni")?.toString() ?: "0"

                            val item = TextView(requireContext()).apply {
                                text = "○ $nome  |  Serie: $serie  •  Rep: $rep"
                                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                                setPadding(36, 4, 8, 16)
                                textSize = 14f
                            }
                            container.addView(item)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Errore nel caricamento degli esercizi", Toast.LENGTH_SHORT).show()
                }
        } else {
            container.visibility = GONE
        }
    }
}