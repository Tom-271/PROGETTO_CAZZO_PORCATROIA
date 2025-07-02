package com.example.progetto_tosa.ui.home

import android.graphics.Typeface
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.view.ViewGroup
import android.widget.Button
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

    private lateinit var dayName: String
    private lateinit var dayDisplayName: String

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

        // ottieni la data passata
        val selectedDateId = requireArguments().getString("selectedDate")
            ?: error("selectedDate mancante")

        // ricava giorno della settimana
        val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(selectedDateId)
        val calendar = Calendar.getInstance()
        calendar.time = parsedDate!!
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        dayName = when (dayOfWeek) {
            Calendar.MONDAY -> "monday"
            Calendar.TUESDAY -> "tuesday"
            Calendar.WEDNESDAY -> "wednesday"
            Calendar.THURSDAY -> "thursday"
            Calendar.FRIDAY -> "friday"
            Calendar.SATURDAY -> "saturday"
            Calendar.SUNDAY -> "sunday"
            else -> "unknown"
        }

        dayDisplayName = when (dayName) {
            "monday" -> "LUNEDÌ"
            "tuesday" -> "MARTEDÌ"
            "wednesday" -> "MERCOLEDÌ"
            "thursday" -> "GIOVEDÌ"
            "friday" -> "VENERDÌ"
            "saturday" -> "SABATO"
            "sunday" -> "DOMENICA"
            else -> "GIORNO"
        }

        // mostra intestazione
        b.subtitlePPPPPPPPROOOOOVA.visibility = VISIBLE
        b.subtitlePPPPPPPPROOOOOVA.text = "SCHEDA DEL $dayDisplayName"

        // bottone aggiungi
        b.btnFillSchedule.visibility = VISIBLE
        b.btnFillSchedule.setOnClickListener {
            findNavController().navigate(
                R.id.action_fragment_my_auto_schedule_to_fragment_workout,
                Bundle().apply { putString("selectedDate", selectedDateId) }
            )
        }

        // ascoltatori contatori
        initExerciseCountListener("bodybuilding", listOf("petto", "gambe", "spalle", "dorso", "bicipiti", "tricipiti"), b.subtitleBodyBuilding)
        initExerciseCountListener("cardio", listOf("cardio1", "cardio2"), b.subtitleCardio)
        initExerciseCountListener("corpo_libero", listOf("libero1", "libero2"), b.subtitleCorpoLibero)
        initExerciseCountListener("stretching", listOf("stretch1", "stretch2"), b.subtitleStretching)

        // toggle e mostra dettagli
        b.btnBodybuilding.setOnClickListener {
            toggleAndPopulate(b.bodybuildingDetailsContainer, "bodybuilding", listOf("petto", "gambe", "spalle", "dorso", "bicipiti", "tricipiti"))
        }
        b.btnCardio.setOnClickListener {
            toggleAndPopulate(b.cardioDetailsContainer, "cardio", listOf("cardio1", "cardio2"))
        }
        b.btnCorpoLibero.setOnClickListener {
            toggleAndPopulate(b.corpoliberoDetailsContainer, "corpo_libero", listOf("libero1", "libero2"))
        }
        b.btnStretching.setOnClickListener {
            toggleAndPopulate(b.stretchingDetailsContainer, "stretching", listOf("stretch1", "stretch2"))
        }

        // bottone cronotimer
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
        val counts = muscoli.associateWith { 0 }.toMutableMap()
        muscoli.forEach { m ->
            val listener = db.collection("schede_giornaliere")
                .document(dayName)
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
        lista: List<String>
    ) {
        if (container.visibility == GONE) {
            container.visibility = VISIBLE
            container.removeAllViews()
            lista.forEach { m ->
                val colRef = db.collection("schede_giornaliere")
                    .document(dayName)
                    .collection(category)
                    .document(m)
                    .collection("esercizi")

                colRef.get().addOnSuccessListener { snap ->
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
                        setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.dark_gray))
                    }
                    container.addView(divider)

                    snap.documents.forEach { doc ->
                        val nome = doc.getString("nomeEsercizio") ?: doc.id
                        val serie = doc.getLong("numeroSerie")?.toString() ?: "0"
                        val rep = doc.getLong("numeroRipetizioni")?.toString() ?: "0"

                        val itemLayout = LinearLayout(requireContext()).apply {
                            orientation = LinearLayout.HORIZONTAL
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply { setMargins(36, 8, 8, 8) }
                            gravity = Gravity.CENTER_VERTICAL
                        }

                        val infoView = TextView(requireContext()).apply {
                            text = "$nome  ○ Rep: $rep  •  Serie: $serie"
                            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                            textSize = 16f
                            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                        }

                        val sizeDp = 32
                        val sizePx = TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            sizeDp.toFloat(),
                            resources.displayMetrics
                        ).toInt()

                        val btnDelete = Button(requireContext()).apply {
                            text = "✕"
                            textSize = 12f
                            minimumWidth = 0
                            minimumHeight = 0
                            layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                                marginStart = TypedValue.applyDimension(
                                    TypedValue.COMPLEX_UNIT_DIP,
                                    8f,
                                    resources.displayMetrics
                                ).toInt()
                            }
                            background = ContextCompat.getDrawable(requireContext(), R.drawable.rounded_delete_button)
                            setOnClickListener {
                                colRef.document(doc.id)
                                    .delete()
                                    .addOnSuccessListener { container.removeView(itemLayout) }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(requireContext(), "Errore: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }

                        itemLayout.addView(infoView)
                        itemLayout.addView(btnDelete)
                        container.addView(itemLayout)
                    }
                }
            }
        } else {
            container.visibility = GONE
        }
    }
}
