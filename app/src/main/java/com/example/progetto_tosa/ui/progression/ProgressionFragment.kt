package com.example.progetto_tosa.ui.progression

import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.core.os.bundleOf
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.progetto_tosa.R
import com.example.progetto_tosa.ui.progression.ProgressionVmFactory
import com.example.progetto_tosa.ui.progression.ProgressionViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions

class ProgressionFragment : Fragment(R.layout.fragment_progression) {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    // Pull-to-refresh
    private lateinit var swipeRefresh: SwipeRefreshLayout

    // UI obiettivi
    private lateinit var etWeightGoalValue: EditText
    private lateinit var etBodyFatGoalValue: EditText
    private lateinit var btnConfirm: TextView

    // Subtitle dinamici: ultima misura registrata per giorno (anche futura)
    private lateinit var bfSubtitle: TextView
    private lateinit var pesoSubtitle: TextView

    // Bottoni per mostrare grafici
    private lateinit var buttonForBF: CardView
    private lateinit var buttonForWEIGHT: CardView

    // ViewModel
    private lateinit var vm: ProgressionViewModel

    private var uid: String? = null
    private var isPtUser = false
    private var editingGoals = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)

        swipeRefresh.setOnRefreshListener {
            loadGoals()
            loadLatestMeasurements()
        }

        uid = auth.currentUser?.uid
        if (uid.isNullOrEmpty()) {
            toast("Devi effettuare il login")
            return
        }

        vm = ViewModelProvider(
            this,
            ProgressionVmFactory(requireContext(), uid!!)
        )[ProgressionViewModel::class.java]

        loadGoals()
        loadLatestMeasurements()
        setupConfirmButton()
        setupGraphButtons()
    }

    private fun bindViews(v: View) {
        swipeRefresh         = v.findViewById(R.id.swipeRefresh)
        etWeightGoalValue    = v.findViewById(R.id.tvWeightGoalValue)
        etBodyFatGoalValue   = v.findViewById(R.id.tvBodyFatGoalValue)
        btnConfirm           = v.findViewById(R.id.buttonConfirm)
        bfSubtitle           = v.findViewById(R.id.bfSubtitle)
        pesoSubtitle         = v.findViewById(R.id.pesoSubtitle)
        buttonForBF          = v.findViewById(R.id.buttonForBF)
        buttonForWEIGHT      = v.findViewById(R.id.buttonForWEIGHT)

        // Disabilito editing di default
        etWeightGoalValue.isEnabled = false
        etBodyFatGoalValue.isEnabled = false
        btnConfirm.visibility = View.GONE
    }

    /**
     * Carica l'ultima misura (bodyFat% e peso) ordinata per giorno (epochDay) in ordine decrescente,
     * prendendo il documento con il giorno più recente, anche se futuro.
     * Per il peso, effettua una query limitata e trova client-side la prima entry con peso non nullo.
     */
    private fun loadLatestMeasurements() {
        val userDoc = db.collection("users").document(uid!!)

        // BODYFAT: ordina per epochDay discendente, prendi il più recente
        userDoc.collection("bodyFatEntries")
            .orderBy("epochDay", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { snaps ->
                val doc = snaps.documents.firstOrNull()
                val bf  = doc?.getDouble("bodyFatPercent")?.toFloat()
                bfSubtitle.text = bf?.let { String.format("%.1f%%", it) } ?: "—"
            }

        // PESO: ordina per epochDay discendente, limito a 10 e cerco la prima entry con bodyWeightKg presente
        userDoc.collection("bodyFatEntries")
            .orderBy("epochDay", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { snaps ->
                val doc = snaps.documents.firstOrNull { it.contains("bodyWeightKg") }
                val w   = doc?.getDouble("bodyWeightKg")?.toFloat()
                pesoSubtitle.text = w?.let { String.format("%.1f kg", it) } ?: "—"
            }
    }

    private fun loadGoals() {
        db.collection("personal_trainers").document(uid!!)
            .get().addOnSuccessListener { snap ->
                isPtUser = snap.getBoolean("isPersonalTrainer") == true
                if (isPtUser) {
                    snap.getDouble("targetLeanMass")
                        ?.let { etWeightGoalValue.setText(String.format("%.1f", it)) }
                    snap.getDouble("targetFatMass")
                        ?.let { etBodyFatGoalValue.setText(String.format("%.1f", it)) }
                } else {
                    db.collection("users").document(uid!!)
                        .get().addOnSuccessListener { uSnap ->
                            uSnap.getDouble("targetLeanMass")
                                ?.let { etWeightGoalValue.setText(String.format("%.1f", it)) }
                            uSnap.getDouble("targetFatMass")
                                ?.let { etBodyFatGoalValue.setText(String.format("%.1f", it)) }
                        }
                }
                swipeRefresh.isRefreshing = false
            }
    }

    private fun setupConfirmButton() {
        btnConfirm.text = if (!editingGoals) "modifica parametri" else "salva obiettivi"
        btnConfirm.setOnClickListener {
            if (!editingGoals) {
                editingGoals = true
                btnConfirm.text = "salva obiettivi"
                etWeightGoalValue.apply {
                    isEnabled = true; isFocusable = true; isFocusableInTouchMode = true; imeOptions = EditorInfo.IME_ACTION_DONE
                }
                etBodyFatGoalValue.apply {
                    isEnabled = true; isFocusable = true; isFocusableInTouchMode = true; imeOptions = EditorInfo.IME_ACTION_DONE
                }
            } else {
                val lean = etWeightGoalValue.text.toString().replace(',', '.').toDoubleOrNull()
                val fat  = etBodyFatGoalValue.text.toString().replace(',', '.').toDoubleOrNull()
                when {
                    lean == null -> toast("Kg obiettivo non valido")
                    fat  == null -> toast("% grasso obiettivo non valido")
                    else -> {
                        vm.updateGoals(newLean = lean, newFat = fat)
                        val targetRef = if (isPtUser)
                            db.collection("personal_trainers").document(uid!!)
                        else
                            db.collection("users").document(uid!!)
                        targetRef.set(mapOf(
                            "targetLeanMass" to lean,
                            "targetFatMass" to fat
                        ), SetOptions.merge())

                        toast("Obiettivi salvati")
                        etWeightGoalValue.isEnabled = false
                        etBodyFatGoalValue.isEnabled = false
                        editingGoals = false
                        btnConfirm.text = "modifica parametri"
                    }
                }
            }
        }
    }

    private fun setupGraphButtons() {
        val nav = findNavController()
        buttonForBF.setOnClickListener {
            nav.navigate(
                R.id.action_progressionFragment_to_graphsFragment,
                bundleOf("graphType" to "bodyfat")
            )
        }
        buttonForWEIGHT.setOnClickListener {
            nav.navigate(
                R.id.action_progressionFragment_to_graphsFragment,
                bundleOf("graphType" to "weight")
            )
        }
    }

    private fun toast(msg: String) =
        android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_SHORT).show()
}
