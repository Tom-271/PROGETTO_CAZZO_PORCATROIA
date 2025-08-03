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
    private lateinit var etLeanGoalValue: EditText
    private lateinit var etBodyFatGoalValue: EditText
    private lateinit var btnConfirm: TextView

    // Subtitle dinamici
    private lateinit var bfSubtitle: TextView
    private lateinit var pesoSubtitle: TextView
    private lateinit var leanSubtitle: TextView

    // Bottoni per grafici
    private lateinit var buttonForBF: CardView
    private lateinit var buttonForWEIGHT: CardView
    private lateinit var buttonForMassaMagra: CardView

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
        swipeRefresh        = v.findViewById(R.id.swipeRefresh)

        etWeightGoalValue   = v.findViewById(R.id.etWeightGoalValue)
        etLeanGoalValue     = v.findViewById(R.id.etLeanGoalValue)
        etBodyFatGoalValue  = v.findViewById(R.id.etBodyFatGoalValue)
        btnConfirm          = v.findViewById(R.id.buttonConfirm)

        bfSubtitle          = v.findViewById(R.id.bfSubtitle)
        pesoSubtitle        = v.findViewById(R.id.pesoSubtitle)
        leanSubtitle        = v.findViewById(R.id.weightSubtitle)

        buttonForBF         = v.findViewById(R.id.buttonForBF)
        buttonForWEIGHT     = v.findViewById(R.id.buttonForWEIGHT)
        buttonForMassaMagra = v.findViewById(R.id.buttonForMassaMagra)

        etWeightGoalValue.isEnabled   = false
        etLeanGoalValue.isEnabled     = false
        etBodyFatGoalValue.isEnabled  = false
        btnConfirm.visibility         = View.GONE
    }

    private fun loadLatestMeasurements() {
        val entriesRef = db
            .collection("users")
            .document(uid!!)
            .collection("bodyFatEntries")

        // Body‐fat
        entriesRef
            .orderBy("epochDay", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { snaps ->
                val bf = snaps.documents
                    .firstOrNull { it.contains("bodyFatPercent") }
                    ?.getDouble("bodyFatPercent")?.toFloat()
                bfSubtitle.text = bf?.let { "%.1f %%".format(it) } ?: "—"
            }

        // Peso
        entriesRef
            .orderBy("epochDay", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { snaps ->
                val w = snaps.documents
                    .firstOrNull { it.contains("bodyWeightKg") }
                    ?.getDouble("bodyWeightKg")?.toFloat()
                pesoSubtitle.text = w?.let { "%.1f kg".format(it) } ?: "—"
            }

        // Massa magra
        entriesRef
            .orderBy("epochDay", Query.Direction.DESCENDING)
            .limit(10)
            .get()
            .addOnSuccessListener { snaps ->
                val lm = snaps.documents
                    .firstOrNull { it.contains("leanMassKg") }
                    ?.getDouble("leanMassKg")?.toFloat()
                leanSubtitle.text = lm?.let { "%.1f kg".format(it) } ?: "—"
                swipeRefresh.isRefreshing = false
            }
    }




    private fun loadGoals() {
        db.collection("personal_trainers").document(uid!!)
            .get().addOnSuccessListener { snap ->
                isPtUser = snap.getBoolean("isPersonalTrainer") == true

                // 1) Mostra/nascondi il pulsante in base a isPtUser
                btnConfirm.visibility = if (isPtUser) View.VISIBLE else View.GONE

                if (isPtUser) {
                    // da PT: legge direttamente da personal_trainers/doc
                    snap.getDouble("targetWeight")
                        ?.let { etWeightGoalValue.setText("%.1f".format(it)) }
                    snap.getDouble("targetLeanMass")
                        ?.let { etLeanGoalValue.setText("%.1f".format(it)) }
                    snap.getDouble("targetFatMass")
                        ?.let { etBodyFatGoalValue.setText("%.1f".format(it)) }
                } else {
                    // da USER: legge da users/doc
                    db.collection("users").document(uid!!)
                        .get().addOnSuccessListener { uSnap ->
                            uSnap.getDouble("targetWeight")
                                ?.let { etWeightGoalValue.setText("%.1f".format(it)) }
                            uSnap.getDouble("targetLeanMass")
                                ?.let { etLeanGoalValue.setText("%.1f".format(it)) }
                            uSnap.getDouble("targetFatMass")
                                ?.let { etBodyFatGoalValue.setText("%.1f".format(it)) }
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
                etWeightGoalValue.isEnabled   = true
                etLeanGoalValue.isEnabled     = true
                etBodyFatGoalValue.isEnabled  = true
                etWeightGoalValue.imeOptions  = EditorInfo.IME_ACTION_DONE
                etLeanGoalValue.imeOptions    = EditorInfo.IME_ACTION_DONE
                etBodyFatGoalValue.imeOptions = EditorInfo.IME_ACTION_DONE
            } else {
                val w    = etWeightGoalValue.text
                    .toString().replace(',', '.').toDoubleOrNull()
                val lean = etLeanGoalValue.text
                    .toString().replace(',', '.').toDoubleOrNull()
                val fat  = etBodyFatGoalValue.text
                    .toString().replace(',', '.').toDoubleOrNull()
                when {
                    w    == null -> toast("Peso obiettivo non valido")
                    lean == null -> toast("M. magra obiettivo non valida")
                    fat  == null -> toast("% grasso obiettivo non valido")
                    else -> {
                        vm.updateGoals(newWeight = w, newLean = lean, newFat = fat)
                        val targetRef = if (isPtUser)
                            db.collection("personal_trainers").document(uid!!)
                        else
                            db.collection("users").document(uid!!)
                        targetRef.set(mapOf(
                            "targetWeight"     to w,
                            "targetLeanMass"   to lean,
                            "targetFatMass"    to fat
                        ), SetOptions.merge())
                        toast("Obiettivi salvati")
                        etWeightGoalValue.isEnabled   = false
                        etLeanGoalValue.isEnabled     = false
                        etBodyFatGoalValue.isEnabled  = false
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
        buttonForMassaMagra.setOnClickListener {
            nav.navigate(
                R.id.action_progressionFragment_to_graphsFragment,
                bundleOf("graphType" to "lean")
            )
        }
    }

    private fun toast(msg: String) =
        android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_SHORT).show()
}
