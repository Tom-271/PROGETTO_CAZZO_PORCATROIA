package com.example.progetto_tosa.ui.progression

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.progetto_tosa.R
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.recyclerview.widget.ItemTouchHelper
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

    // Views
    private lateinit var cardInsertBody: CardView
    private lateinit var cardInsertWeight: CardView
    private lateinit var cardInsertLean: CardView

    private lateinit var tvCurrentBfValue: TextView
    private lateinit var tvCurrentBfDate : TextView
    private lateinit var tvCurrentWeightValue: TextView
    private lateinit var tvCurrentWeightDate : TextView
    private lateinit var tvCurrentLeanValue: TextView
    private lateinit var tvCurrentLeanDate : TextView

    private lateinit var rvNumbersBf: RecyclerView
    private lateinit var rvNumbersWeight: RecyclerView
    private lateinit var rvNumbersLean: RecyclerView

    private lateinit var btnAggiungi: ExtendedFloatingActionButton
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var tvDescBody: TextView
    private lateinit var tvDescWeight: TextView
    private lateinit var tvDescLean: TextView

    private lateinit var tvDateBF: TextView
    private lateinit var etBF: EditText
    private lateinit var btnPickBF: ImageButton
    private lateinit var btnSaveBF: ImageButton

    private lateinit var tvDateWeight: TextView
    private lateinit var etWeight: EditText
    private lateinit var btnPickWeight: ImageButton
    private lateinit var btnSaveWeight: ImageButton

    private lateinit var tvDateLean: TextView
    private lateinit var etLean: EditText
    private lateinit var btnPickLean: ImageButton
    private lateinit var btnSaveLean: ImageButton

    private lateinit var TitleForRVLeanMass: TextView
    private lateinit var TitleForRVWeight: TextView
    private lateinit var TitleForRVBodyFat: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // ViewModel & UID
        uid = auth.currentUser?.uid ?: throw IllegalStateException("Devi effettuare il login")
        vm  = ViewModelProvider(this, ProgressionVmFactory(requireContext(), uid))
            .get(ProgressionViewModel::class.java)

        // Bind views
        cardInsertBody     = view.findViewById(R.id.cardInsertBodyFat)
        cardInsertWeight   = view.findViewById(R.id.cardInsertWeight)
        cardInsertLean     = view.findViewById(R.id.cardInsertLean)

        tvCurrentBfValue   = view.findViewById(R.id.tvCurrentBfValue)
        tvCurrentBfDate    = view.findViewById(R.id.tvCurrentBfDate)
        tvCurrentWeightValue = view.findViewById(R.id.tvCurrentWeightValue)
        tvCurrentWeightDate  = view.findViewById(R.id.tvCurrentWeightDate)
        tvCurrentLeanValue  = view.findViewById(R.id.tvCurrentLeanValue)
        tvCurrentLeanDate   = view.findViewById(R.id.tvCurrentLeanDate)

        rvNumbersBf        = view.findViewById(R.id.rvNumbersBf)
        rvNumbersWeight    = view.findViewById(R.id.rvNumbersWeight)
        rvNumbersLean      = view.findViewById(R.id.rvNumbersLean)

        btnAggiungi        = view.findViewById(R.id.efabAggiungi)
        tvTitle            = view.findViewById(R.id.titoloForGraphs)
        tvSubtitle         = view.findViewById(R.id.tvSubtitleGraph)
        tvDescBody         = view.findViewById(R.id.tvDescBodyFat)
        tvDescWeight       = view.findViewById(R.id.tvDescWeight)
        tvDescLean         = view.findViewById(R.id.tvDescLean)

        tvDateBF           = view.findViewById(R.id.tvSelectedDate)
        etBF               = view.findViewById(R.id.etBodyFatInput)
        btnPickBF          = view.findViewById(R.id.btnPickDate)
        btnSaveBF          = view.findViewById(R.id.btnSaveBodyFat)

        tvDateWeight       = view.findViewById(R.id.tvSelectedDateWeight)
        etWeight           = view.findViewById(R.id.etWeightInput)
        btnPickWeight      = view.findViewById(R.id.btnPickDateWeight)
        btnSaveWeight      = view.findViewById(R.id.btnSaveWeight)

        tvDateLean         = view.findViewById(R.id.tvSelectedDateLean)
        etLean             = view.findViewById(R.id.etLeanInput)
        btnPickLean        = view.findViewById(R.id.btnPickDateLean)
        btnSaveLean        = view.findViewById(R.id.btnSaveLean)

        TitleForRVLeanMass = view.findViewById(R.id.TitleForRVLeanMass)
        TitleForRVWeight   = view.findViewById(R.id.TitleForRVWeight)
        TitleForRVBodyFat  = view.findViewById(R.id.TitleForRVBodyFat)

        // RecyclerViews vertical
        listOf(rvNumbersBf, rvNumbersWeight, rvNumbersLean).forEach {
            it.layoutManager = LinearLayoutManager(requireContext())
        }
        listOf(rvNumbersBf, rvNumbersWeight, rvNumbersLean).forEach { recyclerView ->
            enableSwipeReveal(recyclerView)
        }
        // Hide all initially
        listOf<View>(
            tvCurrentBfValue, tvCurrentBfDate,
            tvCurrentWeightValue, tvCurrentWeightDate,
            tvCurrentLeanValue, tvCurrentLeanDate,
            tvSubtitle, tvDescBody, tvDescWeight, tvDescLean,
            rvNumbersBf, rvNumbersWeight, rvNumbersLean,
            cardInsertBody, cardInsertWeight, cardInsertLean
        ).forEach { it.visibility = View.GONE }

        // Mode
        when (arguments?.getString("graphType") ?: "bodyfat") {
            "bodyfat" -> setupBodyFatMode()
            "weight"  -> setupWeightMode()
            "lean"    -> setupLeanMode()
        }

        // Insert chart fragment
        val chartFrag = when (arguments?.getString("graphType")) {
            "weight" -> PesoChartFragment()
            "lean"   -> MassaMagraChartFragment()
            else      -> BodyFatChartFragment()
        }
        childFragmentManager.beginTransaction()
            .replace(R.id.fragment_graphs_container, chartFrag)
            .commitNow()

        vm.loadBodyFat()
        vm.loadGoals()
        attachCloudListener()
    }

    private fun setupBodyFatMode() {
        tvTitle.text           = "PERCENTUALE MASSA GRASSA RAGGIUNTA:"
        tvCurrentBfValue.isVisible = true
        tvCurrentBfDate.isVisible  = true
        TitleForRVBodyFat.isVisible = true
        TitleForRVWeight.isVisible = false
        TitleForRVLeanMass.isVisible = false
        tvSubtitle.apply { text = "Cosa è il BF?"; visibility = View.VISIBLE }
        tvDescBody.visibility      = View.VISIBLE
        rvNumbersBf.visibility     = View.VISIBLE
        btnAggiungi.apply {
            text = "Aggiungi"; visibility = View.VISIBLE
            setOnClickListener { toggleForm(cardInsertBody) }
        }
        setupDatePickerBF()
        setupSaveButtonBF()
        loadLastBodyFat()
    }

    private fun setupWeightMode() {
        tvTitle.text               = "PESO CORPOREO RAGGIUNTO:"
        tvCurrentWeightValue.isVisible = true
        tvCurrentWeightDate.isVisible  = true
        TitleForRVWeight.isVisible = true
        TitleForRVLeanMass.isVisible = false
        TitleForRVBodyFat.isVisible = false
        tvSubtitle.apply { text = "Peso corporeo"; visibility = View.VISIBLE }
        tvDescWeight.visibility       = View.VISIBLE
        rvNumbersWeight.visibility    = View.VISIBLE
        btnAggiungi.apply {
            text = "Aggiungi"; visibility = View.VISIBLE
            setOnClickListener { toggleForm(cardInsertWeight) }
        }
        setupDatePickerWeight()
        setupSaveButtonWeight()
        loadLastWeight()
    }

    private fun setupLeanMode() {
        tvTitle.text               = "MASSA MAGRA RAGGIUNTA:"
        tvCurrentLeanValue.isVisible = true
        TitleForRVLeanMass.isVisible = true
        tvCurrentLeanDate.isVisible  = true
        TitleForRVLeanMass.isVisible = true
        TitleForRVWeight.isVisible = false
        TitleForRVBodyFat.isVisible = false
        tvSubtitle.apply { text = "Cos’è la massa magra?"; visibility = View.VISIBLE }
        tvDescLean.visibility       = View.VISIBLE
        rvNumbersLean.visibility    = View.VISIBLE
        btnAggiungi.apply {
            text = "Aggiungi"; visibility = View.VISIBLE
            setOnClickListener { toggleForm(cardInsertLean) }
        }
        setupDatePickerLean()
        setupSaveButtonLean()
        loadLastLean()
    }


    private fun attachCloudListener() {
        listener?.remove()
        listener = db.collection("users").document(uid)
            .collection("bodyFatEntries")
            .orderBy("epochDay", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                val entries = snap.documents.mapNotNull { d ->
                    val epoch = d.getLong("epochDay") ?: return@mapNotNull null
                    val bf    = d.getDouble("bodyFatPercent")?.toFloat()
                    val w     = d.getDouble("bodyWeightKg")?.toFloat()
                    val lm    = d.getDouble("leanMassKg")?.toFloat()
                    BodyFatEntry(0, uid, epoch, bf, w, lm)
                }
                // 1) aggiorna il ViewModel in modo che ChartFragment riceva i dati
                vm.replaceAllFromCloud(entries)

                // 2) aggiorna i RecyclerView
                rvNumbersBf.adapter = NumberAdapter(
                    entries.filter { it.bodyFatPercent != null },
                    NumberAdapter.EntryType.BODYFAT
                )
                rvNumbersWeight.adapter = NumberAdapter(
                    entries.filter { it.bodyWeightKg     != null },
                    NumberAdapter.EntryType.WEIGHT
                )
                rvNumbersLean.adapter = NumberAdapter(
                    entries.filter { it.leanMassKg       != null },
                    NumberAdapter.EntryType.LEAN
                )
            }
    }



    // BodyFat
    private fun setupDatePickerBF() {
        val listener = DatePickerDialog.OnDateSetListener { _, y, m, d ->
            selectedDate = LocalDate.of(y, m + 1, d)
            updateDateLabelBF()
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
            if (bf == null || bf <= 0f || bf > 70f) { toast("BF% non valida"); return@setOnClickListener }
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
            val data = mutableMapOf(
                "epochDay" to epoch,
                "bodyFatPercent" to bfVal,
                "updatedAt" to Timestamp.now()
            ).apply { wtVal?.let { put("bodyWeightKg", it) } }
            db.collection("users").document(uid)
                .collection("bodyFatEntries").document(epoch.toString())
                .set(data, SetOptions.merge())
            db.collection("users").document(uid)
                .set(mutableMapOf(
                    "currentBodyFatPercent" to bfVal,
                    "lastBodyFatEpochDay" to epoch
                ).apply { wtVal?.let { put("currentBodyWeightKg", it) } }, SetOptions.merge())
        }
    }

    // Weight
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
            if (wt == null || wt <= 0f) { toast("Peso non valido"); return@setOnClickListener }
            saveEntryWeight(wt)
            toast("Salvato peso %.1f kg".format(wt))
            loadLastWeight()
        }
    }

    // Weight
    private fun saveEntryWeight(wtVal: Float) {
        val epoch = selectedDate.toEpochDay()
        // 1) aggiorna solo il peso nel documento
        val data = mutableMapOf(
            "epochDay" to epoch,
            "bodyWeightKg" to wtVal,
            "updatedAt" to Timestamp.now()
        )
        db.collection("users").document(uid)
            .collection("bodyFatEntries").document(epoch.toString())
            .set(data, SetOptions.merge())

        // 2) aggiorna solo i campi "current" per il peso
        db.collection("users").document(uid)
            .set(mutableMapOf(
                "currentBodyWeightKg"      to wtVal,
                "lastBodyWeightEpochDay"   to epoch
            ), SetOptions.merge())
    }

    // Lean
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
            if (lean == null || lean <= 0f) { toast("Massa magra non valida"); return@setOnClickListener }
            saveEntryLean(lean)
            toast("Salvata massa magra %.1f kg".format(lean))
            loadLastLean()
        }
    }

    private fun saveEntryLean(leanVal: Float) {
        val epoch = selectedDate.toEpochDay()
        db.collection("users").document(uid)
            .collection("bodyFatEntries").document(epoch.toString())
            .set(mutableMapOf(
                "epochDay" to epoch,
                "leanMassKg" to leanVal,
                "updatedAt" to Timestamp.now()
            ), SetOptions.merge())
        db.collection("users").document(uid)
            .set(mutableMapOf(
                "currentLeanMassKg" to leanVal,
                "lastLeanEpochDay" to epoch
            ), SetOptions.merge())
        vm.loadBodyFat()
    }

    // Load last values
    private fun loadLastBodyFat() {
        db.collection("users").document(uid)
            .collection("bodyFatEntries")
            .orderBy("epochDay", Query.Direction.DESCENDING)
            .limit(1)
            .get().addOnSuccessListener { snaps ->
                val doc = snaps.documents.firstOrNull()
                val bf = doc?.getDouble("bodyFatPercent")?.toFloat() ?: 0f
                val epoch = doc?.getLong("epochDay") ?: 0L
                tvCurrentBfValue.text = String.format("%.1f %%", bf)
                val date = LocalDate.ofEpochDay(epoch)
                tvCurrentBfDate.text = "%02d/%02d/%04d".format(
                    date.dayOfMonth, date.monthValue, date.year)
            }
    }

    private fun loadLastWeight() {
        db.collection("users").document(uid)
            .collection("bodyFatEntries")
            .orderBy("epochDay", Query.Direction.DESCENDING)
            .limit(10)
            .get().addOnSuccessListener { snaps ->
                val doc = snaps.documents.firstOrNull { it.contains("bodyWeightKg") }
                val wt = doc?.getDouble("bodyWeightKg")?.toFloat() ?: 0f
                val epoch = doc?.getLong("epochDay") ?: 0L
                tvCurrentWeightValue.text = String.format("%.1f kg", wt)
                val date = LocalDate.ofEpochDay(epoch)
                tvCurrentWeightDate.text = "%02d/%02d/%04d".format(
                    date.dayOfMonth, date.monthValue, date.year)
            }
    }

    private fun loadLastLean() {
        db.collection("users").document(uid)
            .collection("bodyFatEntries")
            .orderBy("epochDay", Query.Direction.DESCENDING)
            .limit(10)
            .get().addOnSuccessListener { snaps ->
                val doc = snaps.documents.firstOrNull { it.contains("leanMassKg") }
                val lm = doc?.getDouble("leanMassKg")?.toFloat() ?: 0f
                val epoch = doc?.getLong("epochDay") ?: 0L
                tvCurrentLeanValue.text = String.format("%.1f kg", lm)
                val date = LocalDate.ofEpochDay(epoch)
                tvCurrentLeanDate.text = "%02d/%02d/%04d".format(
                    date.dayOfMonth, date.monthValue, date.year)
            }
    }

    private fun updateDateLabelBF() {
        tvDateBF.text = if (selectedDate == LocalDate.now()) "Oggi"
        else "%02d/%02d/%04d".format(
            selectedDate.dayOfMonth, selectedDate.monthValue, selectedDate.year)
    }

    private fun updateDateLabelWeight() {
        tvDateWeight.text = if (selectedDate == LocalDate.now()) "Oggi"
        else "%02d/%02d/%04d".format(
            selectedDate.dayOfMonth, selectedDate.monthValue, selectedDate.year)
    }

    private fun updateDateLabelLean() {
        tvDateLean.text = if (selectedDate == LocalDate.now()) "Oggi"
        else "%02d/%02d/%04d".format(
            selectedDate.dayOfMonth, selectedDate.monthValue, selectedDate.year)
    }

    // Animate show/hide
    private fun View.toggleVisibilityAnimated() {
        if (isVisible) {
            animate().alpha(0f).setDuration(200).withEndAction {
                visibility = View.GONE
                btnAggiungi.text = "Aggiungi"

                alpha = 1f
            }
        } else {
            alpha = 0f
            visibility = View.VISIBLE
            btnAggiungi.text = "Chiudi"
            animate().alpha(1f).setDuration(200).start()
        }
    }

    private fun toggleForm(card: CardView) {
        card.toggleVisibilityAnimated()
    }

    private fun toast(msg: String) {
        android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun enableSwipeReveal(recyclerView: RecyclerView) {
        val swipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val pos = vh.adapterPosition
                val entry = (recyclerView.adapter as NumberAdapter).getItemAt(pos)
                db.collection("users").document(uid)
                    .collection("bodyFatEntries").document(entry.epochDay.toString())
                    .delete()
                    .addOnSuccessListener { toast("Elemento eliminato dal database") }
                    .addOnFailureListener { e ->
                        toast("Errore eliminazione: ${e.message}")
                        recyclerView.adapter?.notifyItemChanged(pos)
                    }
            }
            override fun onChildDraw(c: Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder,
                                     dX: Float, dY: Float, actionState: Int, isActive: Boolean) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    val item = vh.itemView
                    val paint = Paint().apply { color = Color.RED }
                    c.drawRect(item.right + dX, item.top.toFloat(), item.right.toFloat(), item.bottom.toFloat(), paint)
                    paint.color = Color.WHITE
                    paint.textSize = 16 * rv.resources.displayMetrics.scaledDensity
                    paint.textAlign = Paint.Align.CENTER
                    val x = item.right - (80 * rv.resources.displayMetrics.density) / 2
                    val y = item.top + (item.height + paint.textSize) / 2
                    c.drawText("Elimina", x, y, paint)
                }
                super.onChildDraw(c, rv, vh, dX, dY, actionState, isActive)
            }
        }
        ItemTouchHelper(swipeCallback).attachToRecyclerView(recyclerView)
    }

    override fun onDestroyView() {
        listener?.remove()
        super.onDestroyView()
    }

}
