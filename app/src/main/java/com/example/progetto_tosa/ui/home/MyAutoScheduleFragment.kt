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
import androidx.navigation.fragment.findNavController
import com.example.progetto_tosa.R
import com.example.progetto_tosa.databinding.FragmentMyAutoScheduleBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
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

        // mostra giorno della settimana
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
        binding.subtitleAllExercises.apply {
            visibility = VISIBLE
            text = "SCHEDA DI $dayDisplayName"
        }

        binding.chrono.setOnClickListener {
            findNavController().navigate(
                R.id.action_fragment_my_auto_schedule_to_navigation_cronotimer,
            )
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

        // chiama nuova funzione unificata
        populateUnifiedExerciseList()
    }

    private fun populateUnifiedExerciseList() {
        val user = currentUserName ?: return
        val container = binding.allExercisesContainer
        container.removeAllViews()

        val categories = listOf("bodybuilding", "cardio", "corpo_libero", "stretching")
        val unifiedList = mutableListOf<Triple<String, String, String>>() // nome, categoria, id
        var completedFetches = 0

        for (category in categories) {
            db.collection("schede_giornaliere")
                .document(user)
                .collection(selectedDateId)
                .document(category)
                .collection("esercizi")
                .orderBy("createdAt")
                .get()
                .addOnSuccessListener { snap ->
                    for (doc in snap.documents) {
                        val nome = doc.getString("nomeEsercizio") ?: doc.id
                        unifiedList.add(Triple(nome, category, doc.id))
                    }
                    completedFetches++
                    if (completedFetches == categories.size) {
                        showUnifiedList(container, unifiedList)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Errore nel caricamento degli esercizi", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showUnifiedList(
        container: LinearLayout,
        esercizi: List<Triple<String, String, String>>
    ) {
        val user = currentUserName ?: return

        for ((nome, categoria, docId) in esercizi) {
            val itemLayout = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(36, 4, 8, 16)
            }

            val text = TextView(requireContext()).apply {
                text = "○ $nome" // solo il nome visibile fuori dalla card
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val infoButton = ImageButton(requireContext()).apply {
                setImageResource(R.drawable.info)
                background = null
                layoutParams = LinearLayout.LayoutParams(100, 100)
            }

            val cardView = LayoutInflater.from(requireContext())
                .inflate(R.layout.exercise_info_card, container, false) as CardView
            cardView.visibility = View.GONE

            val titleText = cardView.findViewById<TextView>(R.id.cardExerciseTitle)
            val setsRepsText = cardView.findViewById<TextView>(R.id.cardSetsReps)
            val pesoInput = cardView.findViewById<EditText>(R.id.cardWeightInput)
            val saveButton = cardView.findViewById<Button>(R.id.cardSaveButton)

            titleText.text = nome

            // 🔥 Recupera set, rep, peso dalla categoria corretta
            db.collection("schede_giornaliere")
                .document(user)
                .collection(selectedDateId)
                .document(categoria)
                .collection("esercizi")
                .document(docId)
                .get()
                .addOnSuccessListener { doc ->
                    val serie = doc.getLong("numeroSerie")?.toString() ?: "-"
                    val rip = doc.getLong("numeroRipetizioni")?.toString() ?: "-"
                    val peso = doc.getDouble("peso")
                    setsRepsText.text = "Serie: $serie  |  Ripetizioni: $rip"
                    if (peso != null) pesoInput.setText(peso.toString())
                }

            infoButton.setOnClickListener {
                cardView.visibility = if (cardView.visibility == View.GONE) View.VISIBLE else View.GONE
            }

            saveButton.setOnClickListener {
                val peso = pesoInput.text.toString().toFloatOrNull()
                if (peso == null) {
                    Toast.makeText(requireContext(), "Inserisci un peso valido", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val data = mapOf("peso" to peso)

                db.collection("schede_giornaliere")
                    .document(user)
                    .collection(selectedDateId)
                    .document(categoria)
                    .collection("esercizi")
                    .document(docId)
                    .set(data, SetOptions.merge())
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Peso salvato", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Errore salvataggio", Toast.LENGTH_SHORT).show()
                    }
            }

            itemLayout.addView(text)
            itemLayout.addView(infoButton)

            container.addView(itemLayout)
            container.addView(cardView)
        }
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
