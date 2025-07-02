package com.example.progetto_tosa.ui.home

import android.graphics.Typeface
import android.os.Bundle
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
import com.example.progetto_tosa.databinding.FragmentMyTrainerScheduleBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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
            text = if (dispDate == today) "Oggi il PT ha preparato per me questa scheda:"
            else "LA sCHEDA che mi ha preparato il pt DEL: $dispDate"
            visibility = View.VISIBLE
        }

        // 3) Mostro btnFillSchedule solo se l'utente è PT
        binding.btnFillSchedule.visibility = View.GONE
        auth.currentUser?.uid?.let { uid ->
            // Controllo in users
            db.collection("users").document(uid)
                .get()
                .addOnSuccessListener { userDoc ->
                    val isPT = userDoc.getBoolean("isPersonalTrainer") == true
                    if (isPT) {
                        showFillButton()
                    } else {
                        // Fallback su personal_trainers
                        db.collection("personal_trainers").document(uid)
                            .get()
                            .addOnSuccessListener { ptDoc ->
                                if (ptDoc.getBoolean("isPersonalTrainer") == true) {
                                    showFillButton()
                                }
                            }
                    }
                }
                .addOnFailureListener {
                    // in caso di errore mantengo nascosto
                }
        }

        // 4) Imposto i listener per mostrare i dettagli delle categorie
        val dateCol: CollectionReference = db
            .collection("schede_del_pt")
            .document(selectedUserId)
            .collection(dateId)

        initCount("bodybuilding", binding.subtitleBodyBuilding, dateCol)
        initCount("cardio",       binding.subtitleCardio,       dateCol)
        initCount("corpo_libero", binding.subtitleCorpoLibero, dateCol)
        initCount("stretching",   binding.subtitleStretching,   dateCol)

        binding.btnBodybuilding.setOnClickListener {
            toggleAndPopulate(binding.bodybuildingDetailsContainer, "bodybuilding", dateCol)
        }
        binding.btnCardio.setOnClickListener {
            toggleAndPopulate(binding.cardioDetailsContainer, "cardio", dateCol)
        }
        binding.btnCorpoLibero.setOnClickListener {
            toggleAndPopulate(binding.corpoliberoDetailsContainer, "corpo_libero", dateCol)
        }
        binding.btnStretching.setOnClickListener {
            toggleAndPopulate(binding.stretchingDetailsContainer, "stretching", dateCol)
        }
    }

    private fun showFillButton() {
        binding.btnFillSchedule.apply {
            visibility = View.VISIBLE
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

    private fun initCount(
        category: String,
        subtitleView: TextView,
        dateCol: CollectionReference
    ) {
        val reg = dateCol.document(category).collection("esercizi")
            .addSnapshotListener { snap, err ->
                val total = if (err != null) 0 else (snap?.size() ?: 0)
                subtitleView.text = if (total == 1) "$total esercizio" else "$total esercizi"
            }
        activeListeners += reg
    }

    private fun toggleAndPopulate(
        container: LinearLayout,
        category: String,
        dateCol: CollectionReference
    ) {
        if (container.visibility == View.GONE) {
            container.visibility = View.VISIBLE
            container.removeAllViews()
            dateCol.document(category).collection("esercizi")
                .get()
                .addOnSuccessListener { snap ->
                    if (snap.isEmpty) return@addOnSuccessListener
                    val byMuscle = snap.documents.groupBy {
                        it.getString("muscoloPrincipale")?.uppercase(Locale.getDefault()) ?: "ALTRO"
                    }
                    byMuscle.forEach { (muscle, docs) ->
                        container.addView(TextView(requireContext()).apply {
                            text = muscle
                            typeface = Typeface.DEFAULT_BOLD
                            setTextColor(ContextCompat.getColor(requireContext(), R.color.sky))
                            setPadding(40, 24, 0, 8)
                        })
                        container.addView(View(requireContext()).apply {
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, 3
                            ).apply { setMargins(40, 4, 40, 12) }
                            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.dark_gray))
                        })
                        docs.forEach { doc ->
                            val nome  = doc.getString("nomeEsercizio") ?: doc.id
                            val serie = doc.getLong("numeroSerie")?.toString() ?: "0"
                            val rep   = doc.getLong("numeroRipetizioni")?.toString() ?: "0"
                            container.addView(TextView(requireContext()).apply {
                                text = "○ $nome   •   Serie: $serie   •   Rep: $rep"
                                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                                setPadding(56, 4, 8, 8)
                            })
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(
                        requireContext(),
                        "Errore caricamento esercizi in $category",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } else {
            container.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        activeListeners.forEach { it.remove() }
        activeListeners.clear()
        _binding = null
        super.onDestroyView()
    }
}
