package com.example.progetto_tosa.ui.progression

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.progetto_tosa.R
import com.example.progetto_tosa.data.BodyFatEntry
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import java.time.LocalDate

/**
 * Fragment padre: gestisce input obiettivi e bodyfat/weight con pull-to-refresh
 */
class ProgressionFragment : Fragment(R.layout.fragment_progression) {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    // Pull-to-refresh
    private lateinit var swipeRefresh: SwipeRefreshLayout

    // UI obiettivi
    private lateinit var etWeightGoalValue: EditText
    private lateinit var etBodyFatGoalValue: EditText
    private lateinit var btnConfirm: Button

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

    // Stato data selezionata
    private var selectedDate: LocalDate = LocalDate.now()
    private var uid: String? = null

    // PT flag & editing state
    private var isPtUser = false
    private var editingGoals = false

    // Tag fragment grafici
    private val pesoTag = "PESO_CHART"
    private val bfTag   = "BF_CHART"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)

        // Configura pull-to-refresh
        swipeRefresh = view.findViewById(R.id.swipeRefresh)
        swipeRefresh.setOnRefreshListener {
            reloadAllData()
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

        // Prima ottengo obiettivi direttamente da Firestore, così ho già i valori
        fetchGoalsFromFirestore()

        // Caricamento iniziale degli altri dati
        reloadAllData()
        observeCurrentMeasurements()
        attachCloudListener()

        // Controllo ruolo PT - abilito il button di modifica
        db.collection("personal_trainers").document(uid!!)
            .get().addOnSuccessListener { snap ->
                isPtUser = snap.getBoolean("isPersonalTrainer") == true
                if (isPtUser) {
                    btnConfirm.visibility = View.VISIBLE
                    setupConfirmButton()
                }
            }

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

    private fun bindViews(v: View) {
        etWeightGoalValue    = v.findViewById(R.id.tvWeightGoalValue)
        etBodyFatGoalValue   = v.findViewById(R.id.tvBodyFatGoalValue)
        btnConfirm           = v.findViewById(R.id.buttonConfirm)

        etBodyFatInput       = v.findViewById(R.id.etBodyFatInput)
        btnPickDate          = v.findViewById(R.id.btnPickDate)
        btnSaveBodyFat       = v.findViewById(R.id.btnSaveBodyFat)
        tvSelectedDate       = v.findViewById(R.id.tvSelectedDate)
        panelInsertBodyFat   = v.findViewById(R.id.panelInsertBodyFat)

        etBodyWeightInput    = v.findViewById(R.id.etBodyWeightInput)
        btnPickDateWeight    = v.findViewById(R.id.btnPickDateWeight)
        btnSaveBodyWeight    = v.findViewById(R.id.btnSaveBodyWeight)
        tvSelectedDateWeight = v.findViewById(R.id.tvSelectedDateWeight)
        panelInsertWeight    = v.findViewById(R.id.panelInsertWeight)

        toggle               = v.findViewById(R.id.toggleForGraphs)

        // Disabilito editing obiettivi di default
        etWeightGoalValue.isEnabled = false
        etBodyFatGoalValue.isEnabled = false
        btnConfirm.visibility = View.GONE
    }

    // Ricarica dati bodyfat e obiettivi
    private fun reloadAllData() {
        vm.loadBodyFat()
        vm.loadGoals()
        observeGoals()
        swipeRefresh.isRefreshing = false
    }

    // Prendo obiettivi dal documento corrispondente in Firestore
    private fun fetchGoalsFromFirestore() {
        // Provo da PT
        db.collection("personal_trainers").document(uid!!)
            .get().addOnSuccessListener { ptSnap ->
                if (ptSnap.getBoolean("isPersonalTrainer") == true) {
                    ptSnap.getDouble("targetLeanMass")?.let { etWeightGoalValue.setText(String.format("%.1f", it)) }
                    ptSnap.getDouble("targetFatMass")?.let { etBodyFatGoalValue.setText(String.format("%.1f", it)) }
                } else {
                    // Altrimenti da users
                    db.collection("users").document(uid!!)
                        .get().addOnSuccessListener { uSnap ->
                            uSnap.getDouble("targetLeanMass")?.let { etWeightGoalValue.setText(String.format("%.1f", it)) }
                            uSnap.getDouble("targetFatMass")?.let { etBodyFatGoalValue.setText(String.format("%.1f", it)) }
                        }
                }
            }
    }

    private fun observeGoals() {
        vm.goals.observe(viewLifecycleOwner) { g ->
            g.targetLean?.let { etWeightGoalValue.setText(String.format("%.1f", it)) }
            g.targetFat?.let  { etBodyFatGoalValue.setText(String.format("%.1f", it)) }
        }
    }

    private fun observeCurrentMeasurements() {
        vm.bodyFatEntries.observe(viewLifecycleOwner) { entries ->
            entries.maxByOrNull { it.epochDay }?.let { latest ->
                latest.bodyFatPercent?.let { etBodyFatInput.setText(String.format("%.1f", it)) }
                latest.bodyWeightKg?.let { etBodyWeightInput.setText(String.format("%.1f", it)) }
            }
        }
    }

    private fun setupConfirmButton() {
        btnConfirm.text = "modifica parametri"
        btnConfirm.setOnClickListener {
            if (!editingGoals) {
                editingGoals = true
                btnConfirm.text = "salva obiettivi"
                etWeightGoalValue.apply {
                    isEnabled = true; isFocusable = true; isFocusableInTouchMode = true; isCursorVisible = true; imeOptions = EditorInfo.IME_ACTION_DONE
                }
                etBodyFatGoalValue.apply {
                    isEnabled = true; isFocusable = true; isFocusableInTouchMode = true; isCursorVisible = true; imeOptions = EditorInfo.IME_ACTION_DONE
                }
            } else {
                val lean = etWeightGoalValue.text.toString().replace(',', '.').toDoubleOrNull()
                val fat  = etBodyFatGoalValue.text.toString().replace(',', '.').toDoubleOrNull()
                when {
                    lean == null -> toast("Kg obiettivo non valido")
                    fat  == null -> toast("% grasso obiettivo non valido")
                    else -> {
                        vm.updateGoals(newLean = lean, newFat = fat)
                        // Aggiorno Firestore manualmente per immediatezza
                        val targetRef = if (isPtUser)
                            db.collection("personal_trainers").document(uid!!)
                        else db.collection("users").document(uid!!)
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

    private fun attachCloudListener() {
        val user = auth.currentUser ?: return
        bodyFatListener?.remove()
        bodyFatListener = db.collection("users").document(user.uid)
            .collection("bodyFatEntries").orderBy("epochDay")
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                val list = snap.documents.mapNotNull { d ->
                    val epoch  = d.getLong("epochDay") ?: return@mapNotNull null
                    val bf     = d.getDouble("bodyFatPercent") ?: return@mapNotNull null
                    val weight = d.getDouble("bodyWeightKg")?.toFloat()
                    BodyFatEntry(0, user.uid, epoch, bf.toFloat(), weight)
                }
                vm.replaceAllFromCloud(list)
            }
    }

    private fun setupToggleWithFragments(savedState: Bundle?) {
        val fm = childFragmentManager
        var pesoFrag = fm.findFragmentByTag(pesoTag)
        var bfFrag   = fm.findFragmentByTag(bfTag)
        if (pesoFrag == null) {
            pesoFrag = PesoChartFragment()
            fm.beginTransaction().add(R.id.chartContainer, pesoFrag, pesoTag).commitNow()
        }
        if (bfFrag == null) {
            bfFrag = BodyFatChartFragment()
            fm.beginTransaction().add(R.id.chartContainer, bfFrag, bfTag).hide(bfFrag).commitNow()
        }
        if (savedState == null) {
            toggle.check(R.id.btnCronometro)
            panelInsertWeight.visibility = View.VISIBLE
            panelInsertBodyFat.visibility = View.GONE
        } else applyPanelVisibility(toggle.checkedButtonId)
        toggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            fm.beginTransaction().apply {
                if (checkedId == R.id.btnCronometro) { show(pesoFrag!!); hide(bfFrag!!) }
                else                               { show(bfFrag!!); hide(pesoFrag!!) }
            }.commit()
            applyPanelVisibility(checkedId)
        }
    }

    private fun applyPanelVisibility(checkedId: Int) {
        if (checkedId == R.id.btnCronometro) {
            panelInsertWeight.visibility  = View.VISIBLE
            panelInsertBodyFat.visibility = View.GONE
        } else {
            panelInsertWeight.visibility  = View.GONE
            panelInsertBodyFat.visibility = View.VISIBLE
        }
    }

    private fun setupDatePickers() {
        val pickerListener = DatePickerDialog.OnDateSetListener { _, y, m, d ->
            selectedDate = LocalDate.of(y, m + 1, d)
            updateSelectedDateLabels()
            vm.getMeasurement(selectedDate) { entry ->
                etBodyFatInput.setText(entry?.bodyFatPercent?.let { String.format("%.1f", it) } ?: "")
                etBodyWeightInput.setText(entry?.bodyWeightKg?.let { String.format("%.1f", it) } ?: "")
            }
        }
        val openPicker = {
            val d = selectedDate
            DatePickerDialog(requireContext(), pickerListener, d.year, d.monthValue - 1, d.dayOfMonth).show()
        }
        btnPickDate.setOnClickListener { openPicker() }
        btnPickDateWeight.setOnClickListener { openPicker() }
    }

    private fun updateSelectedDateLabels() {
        val label = if (selectedDate == LocalDate.now()) "Oggi"
        else String.format("%02d/%02d/%d", selectedDate.dayOfMonth, selectedDate.monthValue, selectedDate.year)
        tvSelectedDate.text       = label
        tvSelectedDateWeight.text = label
    }

    private fun setupSaveButtons() {
        btnSaveBodyFat.setOnClickListener {
            val bf = etBodyFatInput.text.toString().replace(',', '.').toFloatOrNull()
            if (bf == null || bf <= 0f || bf > 70f) { toast("BF% non valida (0-70)"); return@setOnClickListener }
            vm.getMeasurement(selectedDate) { existing ->
                val weight = existing?.bodyWeightKg
                saveDay(bf, weight)
                toast(String.format("Salvato BF %.1f%%", bf))
            }
        }
        btnSaveBodyWeight.setOnClickListener {
            val w = etBodyWeightInput.text.toString().replace(',', '.').toFloatOrNull()
            if (w == null || w < 25f || w > 400f) { toast("Peso non valido (25-400)"); return@setOnClickListener }
            vm.getMeasurement(selectedDate) { existing ->
                val bf = existing?.bodyFatPercent ?: 0f
                saveDay(bf, w)
                toast(String.format("Salvato Peso %.1f kg", w))
            }
        }
    }

    private fun saveDay(bf: Float, weight: Float?) {
        vm.addBodyFat(bf, weight, selectedDate)
        saveToFirestoreMirror(bf, weight)
        reloadAllData()
    }

    private fun saveToFirestoreMirror(bf: Float, weight: Float?) {
        val user = auth.currentUser ?: return
        val epoch = selectedDate.toEpochDay()
        val data = mutableMapOf<String, Any>(
            "epochDay" to epoch,
            "bodyFatPercent" to bf,
            "updatedAt" to Timestamp.now()
        ).apply { weight?.let { put("bodyWeightKg", it) } }
        db.collection("users").document(user.uid)
            .collection("bodyFatEntries").document(epoch.toString())
            .set(data, SetOptions.merge())

        val summary = mutableMapOf<String, Any>(
            "currentBodyFatPercent" to bf,
            "lastBodyFatEpochDay" to epoch
        ).apply { weight?.let { put("currentBodyWeightKg", it) } }
        db.collection("users").document(user.uid)
            .set(summary, SetOptions.merge())
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}