package com.example.progetto_tosa.ui.progression

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.progetto_tosa.R
import com.example.progetto_tosa.data.BodyFatEntry
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import java.time.LocalDate

class ProgressionFragment : Fragment(R.layout.fragment_progression) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var tvWeightGoalValue: TextView
    private lateinit var tvBodyFatGoalValue: TextView
    private lateinit var etBodyFatInput: EditText
    private lateinit var btnPickDate: ImageButton
    private lateinit var btnSaveBodyFat: ImageButton
    private lateinit var tvSelectedDate: TextView
    private lateinit var bodyFatChart: LineChart

    private lateinit var vm: ProgressionViewModel
    private var bodyFatListener: ListenerRegistration? = null
    private var selectedDate: LocalDate = LocalDate.now()
    private var uid: String? = null
    private var targetFatLimitLine: com.github.mikephil.charting.components.LimitLine? = null
    private var targetFatValue: Float? = null


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvWeightGoalValue = view.findViewById(R.id.tvWeightGoalValue)
        tvBodyFatGoalValue = view.findViewById(R.id.tvBodyFatGoalValue)
        etBodyFatInput     = view.findViewById(R.id.etBodyFatInput)
        btnPickDate        = view.findViewById(R.id.btnPickDate)
        btnSaveBodyFat     = view.findViewById(R.id.btnSaveBodyFat)
        tvSelectedDate     = view.findViewById(R.id.tvSelectedDate)
        bodyFatChart       = view.findViewById(R.id.bodyFatChart)

        uid = auth.currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), "Devi effettuare il login", Toast.LENGTH_SHORT).show()
            return
        }

        vm = ViewModelProvider(
            this,
            ProgressionVmFactory(requireContext(), uid!!)
        )[ProgressionViewModel::class.java]

        setupChart()
        observe()
        vm.loadBodyFat()
        vm.loadGoals()
        attachListener()

        updateSelectedDateLabel()
        setupDatePicker()
        setupSave()
    }

    override fun onDestroyView() {
        bodyFatListener?.remove()
        bodyFatListener = null
        super.onDestroyView()
    }

    private fun observe() {
        vm.bodyFatEntries.observe(viewLifecycleOwner) { list ->
            if (list.isEmpty()) {
                bodyFatChart.clear()
            } else updateChart(list)
        }
        vm.goals.observe(viewLifecycleOwner) { g ->
            tvWeightGoalValue.text = g.targetLean?.let { v -> String.format("%.1f kg", v) } ?: "—"
            tvBodyFatGoalValue.text = g.targetFat?.let { v -> String.format("%.1f %%", v) } ?: "—"

            val newTarget = g.targetFat?.toFloat()
            if (newTarget != null && newTarget != targetFatValue) {
                targetFatValue = newTarget
                addOrUpdateTargetLimitLine(newTarget)
            }
        }


    }

    private fun addOrUpdateTargetLimitLine(value: Float) {
        val yAxis = bodyFatChart.axisLeft

        // Rimuovi la precedente se esiste
        targetFatLimitLine?.let { yAxis.removeLimitLine(it) }

        targetFatLimitLine = com.github.mikephil.charting.components.LimitLine(value, "Target")
            .apply {
                lineWidth = 2.5f            // spessore
                enableDashedLine(0f, 0f, 0f) // linea piena (se vuoi tratteggi: es. 10f,5f,0f)
                textSize = 10f
                textColor = Color.RED
                lineColor = Color.RED
                labelPosition = com.github.mikephil.charting.components.LimitLine.LimitLabelPosition.RIGHT_TOP
            }

        yAxis.addLimitLine(targetFatLimitLine)
        // Porta la linea dietro o davanti (opzionale: yAxis.setDrawLimitLinesBehindData(false))
        yAxis.setDrawLimitLinesBehindData(false)

        bodyFatChart.invalidate()
    }


    private fun attachListener() {
        val user = auth.currentUser ?: return
        bodyFatListener?.remove()
        bodyFatListener = db.collection("users")
            .document(user.uid)
            .collection("bodyFatEntries")
            .orderBy("epochDay")
            .addSnapshotListener { snap, err ->
                if (err != null || snap == null) return@addSnapshotListener
                val list = snap.documents.mapNotNull { d ->
                    val epoch = d.getLong("epochDay") ?: return@mapNotNull null
                    val bf = d.getDouble("bodyFatPercent") ?: return@mapNotNull null
                    BodyFatEntry(
                        id = 0,
                        userId = user.uid,
                        epochDay = epoch,
                        bodyFatPercent = bf.toFloat()
                    )
                }
                vm.replaceAllFromCloud(list)
            }
    }

    private fun setupChart() = bodyFatChart.apply {
        description.isEnabled = false
        setNoDataText("Nessun dato")
        setTouchEnabled(true)
        setPinchZoom(true)
        legend.isEnabled = false
        axisRight.isEnabled = false
        xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        axisLeft.setDrawGridLines(true)
        xAxis.textColor = Color.LTGRAY
        axisLeft.textColor = Color.LTGRAY
        setExtraOffsets(8f,16f,8f,16f)
    }

    private fun updateChart(entries: List<BodyFatEntry>) {
        val sorted = entries.sortedBy { it.epochDay }
        val mp = sorted.mapIndexed { i, e -> Entry(i.toFloat(), e.bodyFatPercent) }
        val ds = LineDataSet(mp, "BodyFat").apply {
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawValues(false)
            lineWidth = 2f
            setDrawCircles(true)
            circleRadius = 3f
            color = Color.WHITE
            setCircleColor(Color.WHITE)
            highLightColor = Color.YELLOW
            setDrawFilled(true)
            fillColor = Color.parseColor("#33FFFFFF")
            fillAlpha = 60
        }
        bodyFatChart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(v: Float, axis: AxisBase?): String {
                val i = v.toInt()
                return if (i in sorted.indices) {
                    val d = LocalDate.ofEpochDay(sorted[i].epochDay)
                    "${d.dayOfMonth}/${d.monthValue}"
                } else ""
            }
        }
        bodyFatChart.data = LineData(ds)
        bodyFatChart.animateX(400)
        bodyFatChart.invalidate()
    }

    private fun setupDatePicker() {
        btnPickDate.setOnClickListener {
            val d = selectedDate
            DatePickerDialog(
                requireContext(),
                { _, y, m, day ->
                    selectedDate = LocalDate.of(y, m + 1, day)
                    updateSelectedDateLabel()
                    vm.getMeasurement(selectedDate) { entry ->
                        etBodyFatInput.setText(entry?.bodyFatPercent?.let { String.format("%.1f", it) } ?: "")
                    }
                },
                d.year,
                d.monthValue - 1,
                d.dayOfMonth
            ).show()
        }
    }

    private fun updateSelectedDateLabel() {
        tvSelectedDate.text =
            if (selectedDate == LocalDate.now()) "Oggi"
            else "%02d/%02d/%d".format(selectedDate.dayOfMonth, selectedDate.monthValue, selectedDate.year)
    }

    private fun setupSave() {
        btnSaveBodyFat.setOnClickListener {
            val raw = etBodyFatInput.text.toString().replace(',', '.').trim()
            val value = raw.toFloatOrNull()
            if (value == null || value <= 0f || value > 70f) {
                Toast.makeText(requireContext(), "Valore non valido (0 - 70)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            vm.addBodyFat(value, selectedDate)
            val user = auth.currentUser
            if (user != null) {
                val epoch = selectedDate.toEpochDay()
                db.collection("users").document(user.uid)
                    .collection("bodyFatEntries").document(epoch.toString())
                    .set(
                        mapOf(
                            "epochDay" to epoch,
                            "bodyFatPercent" to value,
                            "updatedAt" to com.google.firebase.Timestamp.now()
                        ),
                        SetOptions.merge()
                    )
                db.collection("users").document(user.uid)
                    .set(
                        mapOf(
                            "currentBodyFatPercent" to value,
                            "lastBodyFatEpochDay" to epoch
                        ),
                        SetOptions.merge()
                    )
            }
            Toast.makeText(requireContext(),
                "Salvato ${String.format("%.1f%%", value)} per ${if (selectedDate==LocalDate.now()) "oggi" else tvSelectedDate.text}",
                Toast.LENGTH_SHORT).show()
        }
    }
}
