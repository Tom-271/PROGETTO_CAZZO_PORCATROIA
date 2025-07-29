package com.example.progetto_tosa.ui.progression

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.progetto_tosa.R
import com.example.progetto_tosa.data.BodyFatEntry
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import java.time.LocalDate

class GraphsFragment : Fragment(R.layout.fragment_graphs) {

    private lateinit var vm: ProgressionViewModel
    private lateinit var uid: String

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    private var listener: ListenerRegistration? = null
    private var selectedDate: LocalDate = LocalDate.now()

    // UI BodyFat
    private lateinit var cardInsertBodyFat: CardView
    private lateinit var tvCurrentBfValue: TextView
    private lateinit var tvCurrentBfDate: TextView

    // UI Weight
    private lateinit var cardInsertWeight: CardView
    private lateinit var tvCurrentWeightValue: TextView
    private lateinit var tvCurrentWeightDate: TextView

    // UI Lean mass
    private lateinit var cardInsertLean: CardView
    private lateinit var tvCurrentLeanValue: TextView
    private lateinit var tvCurrentLeanDate: TextView

    // Titolo principale
    private lateinit var titoloforgraphs: TextView

    // Grafico: sottotitolo e descrizioni
    private lateinit var tvSubtitleGraph: TextView
    private lateinit var tvDescBodyFat: TextView
    private lateinit var tvDescWeight: TextView
    private lateinit var tvDescLean: TextView

    // Form BodyFat
    private lateinit var tvDateBF: TextView
    private lateinit var etBF: EditText
    private lateinit var btnPickBF: ImageButton
    private lateinit var btnSaveBF: ImageButton

    // Form Weight
    private lateinit var tvDateWeight: TextView
    private lateinit var etWeight: EditText
    private lateinit var btnPickWeight: ImageButton
    private lateinit var btnSaveWeight: ImageButton

    // Form Lean mass
    private lateinit var tvDateLean: TextView
    private lateinit var etLean: EditText
    private lateinit var btnPickLean: ImageButton
    private lateinit var btnSaveLean: ImageButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // inizializza ViewModel e UID
        uid = auth.currentUser?.uid ?: throw IllegalStateException("Devi effettuare il login")
        vm  = ViewModelProvider(this, ProgressionVmFactory(requireContext(), uid))
            .get(ProgressionViewModel::class.java)

        // bind di tutte le view
        cardInsertBodyFat    = view.findViewById(R.id.cardInsertBodyFat)
        tvCurrentBfValue     = view.findViewById(R.id.tvCurrentBfValue)
        tvCurrentBfDate      = view.findViewById(R.id.tvCurrentBfDate)

        cardInsertWeight         = view.findViewById(R.id.cardInsertWeight)
        tvCurrentWeightValue     = view.findViewById(R.id.tvCurrentWeightValue)
        tvCurrentWeightDate      = view.findViewById(R.id.tvCurrentWeightDate)

        cardInsertLean        = view.findViewById(R.id.cardInsertLean)
        tvCurrentLeanValue    = view.findViewById(R.id.tvCurrentLeanValue)
        tvCurrentLeanDate     = view.findViewById(R.id.tvCurrentLeanDate)

        titoloforgraphs       = view.findViewById(R.id.titoloForGraphs)

        tvSubtitleGraph = view.findViewById(R.id.tvSubtitleGraph)
        tvDescBodyFat   = view.findViewById(R.id.tvDescBodyFat)
        tvDescWeight    = view.findViewById(R.id.tvDescWeight)
        tvDescLean      = view.findViewById(R.id.tvDescLean)

        tvDateBF   = view.findViewById(R.id.tvSelectedDate)
        etBF       = view.findViewById(R.id.etBodyFatInput)
        btnPickBF  = view.findViewById(R.id.btnPickDate)
        btnSaveBF  = view.findViewById(R.id.btnSaveBodyFat)

        tvDateWeight  = view.findViewById(R.id.tvSelectedDateWeight)
        etWeight      = view.findViewById(R.id.etWeightInput)
        btnPickWeight = view.findViewById(R.id.btnPickDateWeight)
        btnSaveWeight = view.findViewById(R.id.btnSaveWeight)

        tvDateLean   = view.findViewById(R.id.tvSelectedDateLean)
        etLean       = view.findViewById(R.id.etLeanInput)
        btnPickLean  = view.findViewById(R.id.btnPickDateLean)
        btnSaveLean  = view.findViewById(R.id.btnSaveLean)

        val btnAggiungi = view.findViewById<ExtendedFloatingActionButton>(R.id.efabAggiungi)
        val graphType   = requireArguments().getString("graphType") ?: "bodyfat"

        // flag di stato per mostrare/nascondere i form
        var isBfFormVisible = false
        var isWeightFormVisible = false
        var isLeanFormVisible = false

        when (graphType) {
            "bodyfat" -> {
                // --- BODYFAT MODE ---
                titoloforgraphs.text    = "PERCENTUALE MASSA GRASSA RAGGIUNTA:"
                cardInsertBodyFat.visibility    = View.GONE
                cardInsertWeight.visibility     = View.GONE
                cardInsertLean.visibility       = View.GONE

                tvCurrentBfValue.visibility     = View.VISIBLE
                tvCurrentBfDate.visibility      = View.VISIBLE
                tvCurrentWeightValue.visibility = View.GONE
                tvCurrentWeightDate.visibility  = View.GONE
                tvCurrentLeanValue.visibility   = View.GONE
                tvCurrentLeanDate.visibility    = View.GONE

                tvSubtitleGraph.apply {
                    text       = "Cosa è il BF?"
                    visibility = View.VISIBLE
                }
                tvDescBodyFat.visibility = View.VISIBLE
                tvDescWeight.visibility  = View.GONE
                tvDescLean.visibility    = View.GONE

                loadLastBodyFat()

                btnAggiungi.visibility = View.VISIBLE
                btnAggiungi.text       = "Aggiungi"
                btnAggiungi.setOnClickListener {
                    isBfFormVisible = !isBfFormVisible
                    btnAggiungi.text = if (isBfFormVisible) "Chiudi" else "Aggiungi"
                    cardInsertBodyFat.toggleVisibilityAnimated()
                }
                setupDatePickerBF()
                setupSaveButtonBF()
            }
            "weight" -> {
                // --- WEIGHT MODE ---
                titoloforgraphs.text    = "PESO CORPOREO RAGGIUNTO:"
                cardInsertBodyFat.visibility    = View.GONE
                cardInsertWeight.visibility     = View.GONE
                cardInsertLean.visibility       = View.GONE

                tvCurrentBfValue.visibility     = View.GONE
                tvCurrentBfDate.visibility      = View.GONE
                tvCurrentWeightValue.visibility = View.VISIBLE
                tvCurrentWeightDate.visibility  = View.VISIBLE
                tvCurrentLeanValue.visibility   = View.GONE
                tvCurrentLeanDate.visibility    = View.GONE

                tvSubtitleGraph.apply {
                    text       = "Peso corporeo"
                    visibility = View.VISIBLE
                }
                tvDescBodyFat.visibility = View.GONE
                tvDescWeight.visibility  = View.VISIBLE
                tvDescLean.visibility    = View.GONE

                loadLastWeight()

                btnAggiungi.visibility = View.VISIBLE
                btnAggiungi.text       = "Aggiungi"
                btnAggiungi.setOnClickListener {
                    isWeightFormVisible = !isWeightFormVisible
                    btnAggiungi.text = if (isWeightFormVisible) "Chiudi" else "Aggiungi"
                    cardInsertWeight.toggleVisibilityAnimated()
                }
                setupDatePickerWeight()
                setupSaveButtonWeight()
            }
            "lean" -> {
                // --- LEAN MASS MODE ---
                titoloforgraphs.text    = "MASSA MAGRA RAGGIUNTA:"
                cardInsertBodyFat.visibility    = View.GONE
                cardInsertWeight.visibility     = View.GONE
                cardInsertLean.visibility       = View.GONE

                tvCurrentBfValue.visibility     = View.GONE
                tvCurrentBfDate.visibility      = View.GONE
                tvCurrentWeightValue.visibility = View.GONE
                tvCurrentWeightDate.visibility  = View.GONE
                tvCurrentLeanValue.visibility   = View.VISIBLE
                tvCurrentLeanDate.visibility    = View.VISIBLE

                tvSubtitleGraph.apply {
                    text       = "Cos’è la massa magra?"
                    visibility = View.VISIBLE
                }
                tvDescBodyFat.visibility = View.GONE
                tvDescWeight.visibility  = View.GONE
                tvDescLean.visibility    = View.VISIBLE

                loadLastLean()

                btnAggiungi.visibility = View.VISIBLE
                btnAggiungi.text       = "Aggiungi"
                btnAggiungi.setOnClickListener {
                    isLeanFormVisible = !isLeanFormVisible
                    btnAggiungi.text = if (isLeanFormVisible) "Chiudi" else "Aggiungi"
                    cardInsertLean.toggleVisibilityAnimated()
                }
                setupDatePickerLean()
                setupSaveButtonLean()
            }
        }

        // inserisci Fragment grafico
        val chartFrag = when (graphType) {
            "bodyfat" -> BodyFatChartFragment()
            "weight"  -> PesoChartFragment()
            else      -> MassaMagraChartFragment()
        }
        val chartTag = when (graphType) {
            "bodyfat" -> "BF_CHART"
            "weight"  -> "PESO_CHART"
            else      -> "LEAN_CHART"
        }
        childFragmentManager.beginTransaction()
            .replace(R.id.fragment_graphs_container, chartFrag, chartTag)
            .commitNow()

        // carica dati iniziali
        vm.loadBodyFat()
        vm.loadGoals()
        updateDateLabelBF()
        attachCloudListener()
    }

    // --- BODYFAT ---
    private fun setupDatePickerBF() {
        val listener = DatePickerDialog.OnDateSetListener { _, y, m, d ->
            selectedDate = LocalDate.of(y, m + 1, d)
            updateDateLabelBF()
            vm.getMeasurement(selectedDate) { entry ->
                etBF.setText(entry?.bodyFatPercent?.toString() ?: "")
            }
        }
        btnPickBF.setOnClickListener {
            DatePickerDialog(requireContext(), listener,
                selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth
            ).show()
        }
    }
    private fun setupSaveButtonBF() {
        btnSaveBF.setOnClickListener {
            val bf = etBF.text.toString().replace(',', '.').toFloatOrNull()
            if (bf == null || bf <= 0f || bf > 70f) {
                toast("BF% non valida"); return@setOnClickListener
            }
            saveEntryBF(bf)
            toast("Salvato BF %.1f %%".format(bf))
            loadLastBodyFat()
        }
    }
    private fun saveEntryBF(bfVal: Float) {
        vm.getMeasurement(selectedDate) { existing ->
            val wtVal = existing?.bodyWeightKg
            vm.addBodyFat(bfVal, wtVal, selectedDate)

            val epoch = selectedDate.toEpochDay()
            val data = mutableMapOf<String, Any>(
                "epochDay" to epoch,
                "bodyFatPercent" to bfVal,
                "updatedAt" to Timestamp.now()
            ).apply { wtVal?.let { put("bodyWeightKg", it) } }

            db.collection("users").document(uid)
                .collection("bodyFatEntries").document(epoch.toString())
                .set(data, SetOptions.merge())

            val summary = mutableMapOf<String, Any>(
                "currentBodyFatPercent" to bfVal,
                "lastBodyFatEpochDay" to epoch
            ).apply { wtVal?.let { put("currentBodyWeightKg", it) } }

            db.collection("users").document(uid)
                .set(summary, SetOptions.merge())
        }
    }

    // --- WEIGHT ---
    private fun setupDatePickerWeight() {
        val listener = DatePickerDialog.OnDateSetListener { _, y, m, d ->
            selectedDate = LocalDate.of(y, m + 1, d)
            updateDateLabelWeight()
        }
        btnPickWeight.setOnClickListener {
            DatePickerDialog(requireContext(), listener,
                selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth
            ).show()
        }
    }
    private fun setupSaveButtonWeight() {
        btnSaveWeight.setOnClickListener {
            val wt = etWeight.text.toString().replace(',', '.').toFloatOrNull()
            if (wt == null || wt <= 0f) {
                toast("Peso non valido"); return@setOnClickListener
            }
            saveEntryWeight(wt)
            toast("Salvato peso %.1f kg".format(wt))
            loadLastWeight()
        }
    }
    private fun saveEntryWeight(wtVal: Float) {
        vm.getMeasurement(selectedDate) { existing ->
            val bfPrev = existing?.bodyFatPercent ?: 0f
            vm.addBodyFat(bfPrev, wtVal, selectedDate)

            val epoch = selectedDate.toEpochDay()
            val data = mutableMapOf<String, Any>(
                "epochDay" to epoch,
                "bodyWeightKg" to wtVal,
                "updatedAt" to Timestamp.now()
            ).apply { put("bodyFatPercent", bfPrev) }

            db.collection("users").document(uid)
                .collection("bodyFatEntries").document(epoch.toString())
                .set(data, SetOptions.merge())

            val summary = mutableMapOf<String, Any>(
                "currentBodyWeightKg" to wtVal,
                "lastBodyWeightEpochDay" to epoch
            ).apply { put("currentBodyFatPercent", bfPrev) }

            db.collection("users").document(uid)
                .set(summary, SetOptions.merge())
        }
    }

    // --- LEAN MASS ---
    private fun setupDatePickerLean() {
        val listener = DatePickerDialog.OnDateSetListener { _, y, m, d ->
            selectedDate = LocalDate.of(y, m + 1, d)
            updateDateLabelLean()
        }
        btnPickLean.setOnClickListener {
            DatePickerDialog(requireContext(), listener,
                selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth
            ).show()
        }
    }
    private fun setupSaveButtonLean() {
        btnSaveLean.setOnClickListener {
            val lean = etLean.text.toString().replace(',', '.').toFloatOrNull()
            if (lean == null || lean <= 0f) {
                toast("Massa magra non valida"); return@setOnClickListener
            }
            saveEntryLean(lean)
            toast("Salvata massa magra %.1f kg".format(lean))
            loadLastLean()
        }
    }
    private fun saveEntryLean(leanVal: Float) {
        val epoch = selectedDate.toEpochDay()
        db.collection("users").document(uid)
            .collection("bodyFatEntries").document(epoch.toString())
            .set(mapOf(
                "epochDay" to epoch,
                "leanMassKg" to leanVal,
                "updatedAt" to Timestamp.now()
            ), SetOptions.merge())
        db.collection("users").document(uid)
            .set(mapOf(
                "currentLeanMassKg" to leanVal,
                "lastLeanEpochDay" to epoch
            ), SetOptions.merge())
        vm.loadBodyFat()
    }

    // --- LOAD LAST VALUES ---
    private fun loadLastBodyFat() {
        db.collection("users").document(uid)
            .collection("bodyFatEntries")
            .orderBy("epochDay", Query.Direction.DESCENDING)
            .limit(1)
            .get().addOnSuccessListener { snaps ->
                val doc   = snaps.documents.firstOrNull()
                val bf    = doc?.getDouble("bodyFatPercent")?.toFloat() ?: 0f
                val epoch = doc?.getLong("epochDay") ?: 0L
                tvCurrentBfValue.text = String.format("%.1f %%", bf)
                val date = LocalDate.ofEpochDay(epoch)
                tvCurrentBfDate.text = "%02d/%02d/%04d".format(
                    date.dayOfMonth, date.monthValue, date.year
                )
            }
    }
    private fun loadLastWeight() {
        db.collection("users").document(uid)
            .collection("bodyFatEntries")
            .orderBy("epochDay", Query.Direction.DESCENDING)
            .limit(10)
            .get().addOnSuccessListener { snaps ->
                val doc   = snaps.documents.firstOrNull { it.contains("bodyWeightKg") }
                val wt    = doc?.getDouble("bodyWeightKg")?.toFloat() ?: 0f
                val epoch = doc?.getLong("epochDay") ?: 0L
                tvCurrentWeightValue.text = String.format("%.1f kg", wt)
                val date = LocalDate.ofEpochDay(epoch)
                tvCurrentWeightDate.text = "%02d/%02d/%04d".format(
                    date.dayOfMonth, date.monthValue, date.year
                )
            }
    }
    private fun loadLastLean() {
        db.collection("users").document(uid)
            .collection("bodyFatEntries")
            .orderBy("epochDay", Query.Direction.DESCENDING)
            .limit(10)
            .get().addOnSuccessListener { snaps ->
                val doc   = snaps.documents.firstOrNull { it.contains("leanMassKg") }
                val lean  = doc?.getDouble("leanMassKg")?.toFloat() ?: 0f
                val epoch = doc?.getLong("epochDay") ?: 0L
                tvCurrentLeanValue.text = String.format("%.1f kg", lean)
                val date = LocalDate.ofEpochDay(epoch)
                tvCurrentLeanDate.text = "%02d/%02d/%04d".format(
                    date.dayOfMonth, date.monthValue, date.year
                )
            }
    }

    // --- DATE LABELS ---
    private fun updateDateLabelBF() {
        tvDateBF.text = if (selectedDate == LocalDate.now()) "Oggi"
        else "%02d/%02d/%04d".format(
            selectedDate.dayOfMonth, selectedDate.monthValue, selectedDate.year
        )
    }
    private fun updateDateLabelWeight() {
        tvDateWeight.text = if (selectedDate == LocalDate.now()) "Oggi"
        else "%02d/%02d/%04d".format(
            selectedDate.dayOfMonth, selectedDate.monthValue, selectedDate.year
        )
    }
    private fun updateDateLabelLean() {
        tvDateLean.text = if (selectedDate == LocalDate.now()) "Oggi"
        else "%02d/%02d/%04d".format(
            selectedDate.dayOfMonth, selectedDate.monthValue, selectedDate.year
        )
    }

    private fun attachCloudListener() {
        listener?.remove()
        listener = db.collection("users")
            .document(uid)
            .collection("bodyFatEntries")
            .orderBy("epochDay")
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                val list = snap.documents.mapNotNull { d ->
                    val e  = d.getLong("epochDay") ?: return@mapNotNull null
                    val bf = d.getDouble("bodyFatPercent")?.toFloat() ?: return@mapNotNull null
                    val w  = d.getDouble("bodyWeightKg")?.toFloat()
                    val lm = d.getDouble("leanMassKg")?.toFloat()
                    BodyFatEntry(0, uid, e, bf, w, lm)
                }
                vm.replaceAllFromCloud(list)
            }
    }

    private fun View.toggleVisibilityAnimated() {
        if (visibility == View.VISIBLE) {
            animate().alpha(0f).setDuration(200).withEndAction {
                visibility = View.GONE
                alpha = 1f
            }
        } else {
            alpha = 0f
            visibility = View.VISIBLE
            animate().alpha(1f).setDuration(200).start()
        }
    }

    private fun toast(msg: String) {
        android.widget.Toast
            .makeText(requireContext(), msg, android.widget.Toast.LENGTH_SHORT)
            .show()
    }

    override fun onDestroyView() {
        listener?.remove()
        super.onDestroyView()
    }
}
