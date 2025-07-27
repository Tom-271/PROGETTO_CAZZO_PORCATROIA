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
    private val db = FirebaseFirestore.getInstance()

    private var listener: ListenerRegistration? = null
    private var selectedDate: LocalDate = LocalDate.now()

    private lateinit var cardInsertBodyFat: CardView
    private lateinit var tvCurrentBfValue: TextView
    private lateinit var tvCurrentBfDate: TextView

    private lateinit var tvDateBF: TextView
    private lateinit var etBF: EditText
    private lateinit var btnPickBF: ImageButton
    private lateinit var btnSaveBF: ImageButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        uid = auth.currentUser?.uid ?: throw IllegalStateException("Devi effettuare il login")
        vm = ViewModelProvider(this, ProgressionVmFactory(requireContext(), uid))
            .get(ProgressionViewModel::class.java)

        cardInsertBodyFat = view.findViewById(R.id.cardInsertBodyFat)
        tvCurrentBfValue = view.findViewById(R.id.tvCurrentBfValue)
        tvCurrentBfDate = view.findViewById(R.id.tvCurrentBfDate)

        tvDateBF = view.findViewById(R.id.tvSelectedDate)
        etBF = view.findViewById(R.id.etBodyFatInput)
        btnPickBF = view.findViewById(R.id.btnPickDate)
        btnSaveBF = view.findViewById(R.id.btnSaveBodyFat)

        val graphType = requireArguments().getString("graphType")

        val btnAggiungi = view.findViewById<ExtendedFloatingActionButton>(R.id.efabAggiungi)

        if (graphType == "bodyfat") {
            cardInsertBodyFat.visibility = View.GONE
            tvCurrentBfValue.visibility = View.VISIBLE
            tvCurrentBfDate.visibility = View.VISIBLE
            loadLastBodyFat()

            var isFormVisible = false
            btnAggiungi.setOnClickListener {
                isFormVisible = !isFormVisible
                btnAggiungi.text = if (isFormVisible) "Chiudi" else "Aggiungi"
                cardInsertBodyFat.toggleVisibilityAnimated()
            }

            setupDatePicker()
            setupSaveButton()
        } else {
            cardInsertBodyFat.visibility = View.GONE
            tvCurrentBfValue.visibility = View.GONE
            tvCurrentBfDate.visibility = View.GONE
            btnAggiungi.visibility = View.GONE
        }

        val chartFragment = if (graphType == "bodyfat") BodyFatChartFragment() else PesoChartFragment()
        val chartTag = if (graphType == "bodyfat") "BF_CHART" else "PESO_CHART"

        childFragmentManager.beginTransaction()
            .replace(R.id.fragment_graphs_container, chartFragment, chartTag)
            .commitNow()

        vm.loadBodyFat()
        vm.loadGoals()
        updateDateLabel()
        attachCloudListener()
    }

    private fun loadLastBodyFat() {
        db.collection("users").document(uid)
            .collection("bodyFatEntries")
            .orderBy("epochDay", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { snaps ->
                val doc = snaps.documents.firstOrNull()
                if (doc != null) {
                    val bf = doc.getDouble("bodyFatPercent")?.toFloat() ?: 0f
                    val epoch = doc.getLong("epochDay") ?: 0L
                    tvCurrentBfValue.text = String.format("%.1f%%", bf)
                    val date = LocalDate.ofEpochDay(epoch)
                    tvCurrentBfDate.text = "%02d/%02d/%04d".format(date.dayOfMonth, date.monthValue, date.year)
                }
            }
    }

    private fun setupDatePicker() {
        val listener = DatePickerDialog.OnDateSetListener { _, y, m, d ->
            selectedDate = LocalDate.of(y, m + 1, d)
            updateDateLabel()
            vm.getMeasurement(selectedDate) { entry ->
                etBF.setText(entry?.bodyFatPercent?.toString() ?: "")
            }
        }
        btnPickBF.setOnClickListener {
            DatePickerDialog(requireContext(), listener, selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth).show()
        }
    }

    private fun setupSaveButton() {
        btnSaveBF.setOnClickListener {
            val bf = etBF.text.toString().replace(',', '.').toFloatOrNull()
            if (bf == null || bf <= 0f || bf > 70f) {
                toast("BF% non valida (0-70)")
                return@setOnClickListener
            }

            saveEntry(bf)
            toast("Salvato BF %.1f%%".format(bf))
            vm.loadBodyFat()
        }
    }

    private fun saveEntry(bfVal: Float) {
        vm.getMeasurement(selectedDate) { existing ->
            val wtVal = existing?.bodyWeightKg

            vm.addBodyFat(bfVal, wtVal, selectedDate)

            val epoch = selectedDate.toEpochDay()
            val data = mutableMapOf<String, Any>(
                "epochDay" to epoch,
                "bodyFatPercent" to bfVal,
                "updatedAt" to Timestamp.now()
            ).apply {
                wtVal?.let { put("bodyWeightKg", it) }
            }

            db.collection("users").document(uid)
                .collection("bodyFatEntries")
                .document(epoch.toString())
                .set(data, SetOptions.merge())

            val summary = mutableMapOf<String, Any>(
                "currentBodyFatPercent" to bfVal,
                "lastBodyFatEpochDay" to epoch
            ).apply {
                wtVal?.let { put("currentBodyWeightKg", it) }
            }

            db.collection("users").document(uid).set(summary, SetOptions.merge())
        }
    }

    private fun updateDateLabel() {
        val label = if (selectedDate == LocalDate.now()) "Oggi"
        else "%02d/%02d/%04d".format(selectedDate.dayOfMonth, selectedDate.monthValue, selectedDate.year)
        tvDateBF.text = label
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
                    val e = d.getLong("epochDay") ?: return@mapNotNull null
                    val bf = d.getDouble("bodyFatPercent")?.toFloat() ?: return@mapNotNull null
                    val w = d.getDouble("bodyWeightKg")?.toFloat()
                    BodyFatEntry(0, uid, e, bf, w)
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
        android.widget.Toast.makeText(requireContext(), msg, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        listener?.remove()
        super.onDestroyView()
    }
}
