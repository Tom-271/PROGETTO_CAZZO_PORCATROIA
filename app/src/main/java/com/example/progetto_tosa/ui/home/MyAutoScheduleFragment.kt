package com.example.progetto_tosa.ui.home

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
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

    // recupera il nome e cognome utente loggato
    private val currentUserName: String?
        get() = requireActivity()
            .getSharedPreferences("user_data", android.content.Context.MODE_PRIVATE)
            .getString("saved_display_name", null)

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

        selectedDateId = requireArguments().getString("selectedDate")
            ?: error("selectedDate mancante")
        Log.d("MyAutoSchedule", "selectedDateId = $selectedDateId")

        // giorno della settimana
        val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .parse(selectedDateId)!!
        val dayOfWeek = Calendar.getInstance().apply { time = parsedDate }
            .get(Calendar.DAY_OF_WEEK)
        val dayDisplayName = when (dayOfWeek) {
            Calendar.MONDAY -> "LUNEDÌ"
            Calendar.TUESDAY -> "MARTEDÌ"
            Calendar.WEDNESDAY -> "MERCOLEDÌ"
            Calendar.THURSDAY -> "GIOVEDÌ"
            Calendar.FRIDAY -> "VENERDÌ"
            Calendar.SATURDAY -> "SABATO"
            Calendar.SUNDAY -> "DOMENICA"
            else -> ""
        }
        binding.subtitlePPPPPPPPROOOOOVA.apply {
            visibility = VISIBLE
            text = "SCHEDA DI $dayDisplayName"
        }

        // bottone per aggiungere esercizi
        binding.btnFillSchedule.apply {
            visibility = VISIBLE
            setOnClickListener {
                findNavController().navigate(
                    R.id.action_fragment_my_auto_schedule_to_fragment_workout,
                    Bundle().apply { putString("selectedDate", selectedDateId) }
                )
            }
        }

        // categorie da verificare
        val categories = listOf("bodybuilding", "cardio", "corpo_libero", "stretching")
        categories.forEach { category ->
            val (subtitleView, container) = when (category) {
                "bodybuilding" -> binding.subtitleBodyBuilding to binding.bodybuildingDetailsContainer
                "cardio" -> binding.subtitleCardio to binding.cardioDetailsContainer
                "corpo_libero" -> binding.subtitleCorpoLibero to binding.corpoliberoDetailsContainer
                "stretching" -> binding.subtitleStretching to binding.stretchingDetailsContainer
                else -> null to null
            }
            if (subtitleView != null && container != null) {
                initCountAndAutoPopulate(category, subtitleView, container)
                val toggleBtn = when (category) {
                    "bodybuilding" -> binding.btnBodybuilding
                    "cardio" -> binding.btnCardio
                    "corpo_libero" -> binding.btnCorpoLibero
                    "stretching" -> binding.btnStretching
                    else -> null
                }
                toggleBtn?.setOnClickListener {
                    toggleAndPopulate(category, container)
                }
            }
        }
    }

    private fun initCountAndAutoPopulate(
        category: String,
        subtitleView: TextView,
        container: LinearLayout
    ) {
        val user = currentUserName ?: return
        val listener = db.collection("schede_giornaliere")
            .document(user)
            .collection(selectedDateId)
            .document(category)
            .collection("esercizi")
            .addSnapshotListener { snap, err ->
                val total = if (err != null) 0 else (snap?.documents?.size ?: 0)
                subtitleView.text = if (total == 1) "$total esercizio" else "$total esercizi"
                if (total > 0 && container.visibility == GONE) {
                    toggleAndPopulate(category, container)
                }
            }
        activeListeners.add(listener)
    }

    private fun toggleAndPopulate(
        category: String,
        container: LinearLayout
    ) {
        val user = currentUserName ?: return
        if (container.visibility == GONE) {
            container.visibility = VISIBLE
            container.removeAllViews()

            db.collection("schede_giornaliere")
                .document(user)
                .collection(selectedDateId)
                .document(category)
                .collection("esercizi")
                .get()
                .addOnSuccessListener { snap ->
                    if (snap.isEmpty) return@addOnSuccessListener

                    val byMuscle = snap.documents.groupBy {
                        it.getString("muscoloPrincipale") ?: "Altro"
                    }
                    byMuscle.forEach { (muscle, exercises) ->
                        val header = TextView(requireContext()).apply {
                            text = muscle.uppercase()
                            typeface = Typeface.DEFAULT_BOLD
                            setTextColor(ContextCompat.getColor(requireContext(), R.color.sky))
                            setPadding(40, 30, 0, 0)
                        }
                        container.addView(header)

                        container.addView(View(requireContext()).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, 3
                            ).apply { setMargins(40, 16, 40, 16) }
                            setBackgroundColor(
                                ContextCompat.getColor(requireContext(), R.color.dark_gray)
                            )
                        })

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
                    Toast.makeText(
                        requireContext(),
                        "Errore nel caricamento degli esercizi",
                        Toast.LENGTH_SHORT
                    ).show()
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
