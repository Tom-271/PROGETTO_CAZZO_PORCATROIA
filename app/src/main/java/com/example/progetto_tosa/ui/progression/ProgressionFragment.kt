package com.example.progetto_tosa.ui.progression

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import java.util.Locale
import com.example.progetto_tosa.R
import com.example.progetto_tosa.data.BodyFatEntry
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import java.time.LocalDate

/** Fragment padre: gestisce input + toggle + caricamento dati; i grafici sono nei figli */
class ProgressionFragment : Fragment(R.layout.fragment_progression) {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    // UI obiettivi
    private lateinit var tvWeightGoalValue: TextView
    private lateinit var tvBodyFatGoalValue: TextView

    // UI input BodyFat
    private lateinit var etBodyFatInput: EditText
    private lateinit var btnPickDate: ImageButton
    private lateinit var btnSaveBodyFat: ImageButton
    private lateinit var tvSelectedDate: TextView
    private lateinit var panelInsertBodyFat: View

    // UI input Peso
    private lateinit var etBodyWeightInput: EditText
    private lateinit var btnPickDateWeight: ImageButton
    private lateinit var btnSaveBodyWeight: ImageButton
    private lateinit var tvSelectedDateWeight: TextView
    private lateinit var panelInsertWeight: View

    // Toggle + titolo
    private lateinit var toggle: MaterialButtonToggleGroup

    // ViewModel
    private lateinit var vm: ProgressionViewModel

    // Firestore listener
    private var bodyFatListener: ListenerRegistration? = null

    private var selectedDate: LocalDate = LocalDate.now()
    private var uid: String? = null

    // Tag fragment grafici
    private val pesoTag = "PESO_CHART"
    private val bfTag   = "BF_CHART"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindViews(view)

        uid = auth.currentUser?.uid
        if (uid == null) {
            toast("Devi effettuare il login")
            return
        }

        vm = ViewModelProvider(
            this,
            ProgressionVmFactory(requireContext(), uid!!)
        )[ProgressionViewModel::class.java]

        observeVm()
        vm.loadBodyFat()
        vm.loadGoals()
        attachCloudListener()

        updateSelectedDateLabels()
        setupDatePickers()
        setupSaveButtons()
        setupToggleWithFragments(savedInstanceState)
    }

    override fun onDestroyView() {
        bodyFatListener?.remove()
        bodyFatListener = null
        super.onDestroyView()
    }

    /* -------------------- UI BIND -------------------- */

    private fun bindViews(v: View) {
        tvWeightGoalValue     = v.findViewById(R.id.tvWeightGoalValue)
        tvBodyFatGoalValue    = v.findViewById(R.id.tvBodyFatGoalValue)

        etBodyFatInput        = v.findViewById(R.id.etBodyFatInput)
        btnPickDate           = v.findViewById(R.id.btnPickDate)
        btnSaveBodyFat        = v.findViewById(R.id.btnSaveBodyFat)
        tvSelectedDate        = v.findViewById(R.id.tvSelectedDate)
        panelInsertBodyFat    = v.findViewById(R.id.panelInsertBodyFat)

        etBodyWeightInput     = v.findViewById(R.id.etBodyWeightInput)
        btnPickDateWeight     = v.findViewById(R.id.btnPickDateWeight)
        btnSaveBodyWeight     = v.findViewById(R.id.btnSaveBodyWeight)
        tvSelectedDateWeight  = v.findViewById(R.id.tvSelectedDateWeight)
        panelInsertWeight     = v.findViewById(R.id.panelInsertWeight)

        toggle                = v.findViewById(R.id.toggleForGraphs)
    }

    /* -------------------- OBSERVE -------------------- */

    private fun observeVm() {
        vm.goals.observe(viewLifecycleOwner) { g ->
            tvWeightGoalValue.text  = g.targetLean?.let { String.format(Locale.getDefault(), "%.1f kg", it) } ?: "—"
            tvBodyFatGoalValue.text = g.targetFat ?.let { String.format(Locale.getDefault(), "%.1f%%", it) } ?: "—"
        }
    }

    /* -------------------- FIRESTORE LISTENER -------------------- */

    private fun attachCloudListener() {
        val user = auth.currentUser ?: return
        bodyFatListener?.remove()
        bodyFatListener = db.collection("users")
            .document(user.uid)
            .collection("bodyFatEntries")
            .orderBy("epochDay")
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                val list = snap.documents.mapNotNull { d ->
                    val epoch  = d.getLong("epochDay") ?: return@mapNotNull null
                    val bf     = d.getDouble("bodyFatPercent") ?: return@mapNotNull null
                    val weight = d.getDouble("bodyWeightKg")?.toFloat()
                    BodyFatEntry(
                        id = 0,
                        userId = user.uid,
                        epochDay = epoch,
                        bodyFatPercent = bf.toFloat(),
                        bodyWeightKg = weight
                    )
                }
                vm.replaceAllFromCloud(list)
            }
    }

    /* -------------------- TOGGLE & CHILD FRAGMENTS -------------------- */

    private fun setupToggleWithFragments(savedState: Bundle?) {
        val fm = childFragmentManager

        var pesoFrag = fm.findFragmentByTag(pesoTag)
        var bfFrag   = fm.findFragmentByTag(bfTag)

        if (pesoFrag == null) {
            pesoFrag = PesoChartFragment()
            fm.beginTransaction()
                .add(R.id.chartContainer, pesoFrag, pesoTag)
                .commitNow()
        }
        if (bfFrag == null) {
            bfFrag = BodyFatChartFragment()
            fm.beginTransaction()
                .add(R.id.chartContainer, bfFrag, bfTag)
                .hide(bfFrag)
                .commitNow()
        }

        if (savedState == null) {
            toggle.check(R.id.btnCronometro) // Peso default
            panelInsertWeight.visibility = View.VISIBLE
            panelInsertBodyFat.visibility = View.GONE
        } else {
            // Se serve, potresti ripristinare la visibilità in base al bottone selezionato
            val currentId = toggle.checkedButtonId
            applyPanelVisibility(currentId)
        }

        toggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            fm.beginTransaction().apply {
                when (checkedId) {
                    R.id.btnCronometro -> { // Peso
                        show(pesoFrag!!)
                        hide(bfFrag!!)
                    }
                    R.id.btnTimer -> {      // BodyFat
                        show(bfFrag!!)
                        hide(pesoFrag!!)
                    }
                }
            }.commit()
            applyPanelVisibility(checkedId)
        }
    }

    private fun applyPanelVisibility(checkedId: Int) {
        when (checkedId) {
            R.id.btnCronometro -> {
                panelInsertWeight.visibility  = View.VISIBLE
                panelInsertBodyFat.visibility = View.GONE
            }
            R.id.btnTimer -> {
                panelInsertWeight.visibility  = View.GONE
                panelInsertBodyFat.visibility = View.VISIBLE
            }
        }
    }

    /* -------------------- DATE PICKER -------------------- */

    private fun setupDatePickers() {
        val pickerListener = DatePickerDialog.OnDateSetListener { _, y, m, d ->
            selectedDate = LocalDate.of(y, m + 1, d)
            updateSelectedDateLabels()
            vm.getMeasurement(selectedDate) { entry ->
                etBodyFatInput.setText(entry?.bodyFatPercent?.let { "%.1f".format(it) } ?: "")
                etBodyWeightInput.setText(entry?.bodyWeightKg?.let { "%.1f".format(it) } ?: "")
            }
        }

        val openPicker = {
            val d = selectedDate
            DatePickerDialog(
                requireContext(),
                pickerListener,
                d.year,
                d.monthValue - 1,
                d.dayOfMonth
            ).show()
        }

        btnPickDate.setOnClickListener { openPicker() }
        btnPickDateWeight.setOnClickListener { openPicker() }
    }

    private fun updateSelectedDateLabels() {
        val label = if (selectedDate == LocalDate.now()) "Oggi"
        else "%02d/%02d/%d".format(selectedDate.dayOfMonth, selectedDate.monthValue, selectedDate.year)
        tvSelectedDate.text = label
        tvSelectedDateWeight.text = label
    }

    /* -------------------- SAVE BUTTONS -------------------- */

    private fun setupSaveButtons() {
        btnSaveBodyFat.setOnClickListener {
            val bf = etBodyFatInput.text.toString().replace(',', '.').trim().toFloatOrNull()
            if (bf == null || bf <= 0f || bf > 70f) {
                toast("BF% non valida (0-70)")
                return@setOnClickListener
            }
            vm.getMeasurement(selectedDate) { existing ->
                val weight = existing?.bodyWeightKg
                saveDay(bf, weight)
                toast("Salvato BF ${"%.1f".format(bf)}%")
            }
        }

        btnSaveBodyWeight.setOnClickListener {
            val w = etBodyWeightInput.text.toString().replace(',', '.').trim().toFloatOrNull()
            if (w == null || w < 25f || w > 400f) {
                toast("Peso non valido (25-400)")
                return@setOnClickListener
            }
            vm.getMeasurement(selectedDate) { existing ->
                val bf = existing?.bodyFatPercent ?: 0f
                saveDay(bf, w)
                toast("Salvato Peso ${"%.1f".format(w)} kg")
            }
        }
    }

    private fun saveDay(bf: Float, weight: Float?) {
        vm.addBodyFat(bf, weight, selectedDate)
        saveToFirestoreMirror(bf, weight)
    }

    private fun saveToFirestoreMirror(bf: Float, weight: Float?) {
        val user = auth.currentUser ?: return
        val epoch = selectedDate.toEpochDay()
        val data = mutableMapOf<String, Any>(
            "epochDay" to epoch,
            "bodyFatPercent" to bf,
            "updatedAt" to com.google.firebase.Timestamp.now()
        )
        if (weight != null) data["bodyWeightKg"] = weight

        db.collection("users").document(user.uid)
            .collection("bodyFatEntries").document(epoch.toString())
            .set(data, SetOptions.merge())

        val summary = mutableMapOf<String, Any>(
            "currentBodyFatPercent" to bf,
            "lastBodyFatEpochDay" to epoch
        )
        if (weight != null) summary["currentBodyWeightKg"] = weight

        db.collection("users").document(user.uid)
            .set(summary, SetOptions.merge())
    }

    /* -------------------- UTILS -------------------- */

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}
