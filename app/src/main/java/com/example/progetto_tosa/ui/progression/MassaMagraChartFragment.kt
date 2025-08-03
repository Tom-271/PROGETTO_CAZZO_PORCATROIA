package com.example.progetto_tosa.ui.progression

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.progetto_tosa.R
import com.example.progetto_tosa.data.BodyFatEntry
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate

class MassaMagraChartFragment : Fragment(R.layout.fragment_chart_massamagra) {

    private lateinit var chart: LineChart
    private lateinit var vm: ProgressionViewModel
    private var targetLine: LimitLine? = null
    private var currentEntries: List<BodyFatEntry> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chart = view.findViewById(R.id.chartMassaMagra)

        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("Devi effettuare il login")
        vm = ViewModelProvider(
            requireParentFragment(),
            ProgressionVmFactory(requireContext(), uid)
        ).get(ProgressionViewModel::class.java)

        setupChart()
        observeData()
        observeGoals()
    }

    private fun setupChart() = chart.apply {
        description.isEnabled = false
        setNoDataText("Nessun dato Massa Magra")
        setTouchEnabled(true)
        setPinchZoom(true)
        legend.isEnabled = false
        axisRight.isEnabled = false

        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        axisLeft.setDrawGridLines(true)
        xAxis.textColor = Color.LTGRAY
        axisLeft.textColor = Color.LTGRAY
        setExtraOffsets(8f, 16f, 8f, 16f)
    }

    private fun observeData() {
        vm.bodyFatEntries.observe(viewLifecycleOwner) { list ->
            currentEntries = list
            updateChart(list)
        }
    }

    private fun observeGoals() {
        vm.goals.observe(viewLifecycleOwner) { g ->
            chart.axisLeft.removeAllLimitLines()
            g.targetLean?.toFloat()?.let { updateTargetLine(it) }
            updateChart(currentEntries)
        }
    }

    private fun updateTargetLine(target: Float) {
        targetLine?.let { chart.axisLeft.removeLimitLine(it) }
        targetLine = LimitLine(target, "Target Massa Magra").apply {
            lineWidth = 2f
            textSize = 10f
            lineColor = Color.CYAN
            textColor = Color.CYAN
            labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
        }
        chart.axisLeft.addLimitLine(targetLine)
    }

    private fun updateChart(entries: List<BodyFatEntry>) {
        // solo leanMass espliciti
        val sorted = entries.sortedBy { it.epochDay }
        val pts = sorted.mapIndexedNotNull { i, e ->
            e.leanMassKg?.let { Entry(i.toFloat(), it) }
        }

        if (pts.isEmpty()) {
            chart.clear()
            chart.invalidate()
            return
        }

        val ds = LineDataSet(pts, "Massa Magra (kg)").apply {
            axisDependency = YAxis.AxisDependency.LEFT
            mode = LineDataSet.Mode.LINEAR  // LINEAR per linea spezzata
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

        chart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                val idx = value.toInt()
                return sorted.getOrNull(idx)?.let { e ->
                    val d = LocalDate.ofEpochDay(e.epochDay)
                    "${d.dayOfMonth}/${d.monthValue}"
                } ?: ""
            }
        }

        val maxLean = pts.maxOf { it.y }
        val tgt = vm.goals.value?.targetLean?.toFloat() ?: 0f
        val maxY = maxOf(maxLean, tgt) + 5f
        chart.axisLeft.apply {
            axisMinimum = 0f
            axisMaximum = maxY.coerceAtLeast(10f)
        }

        chart.data = LineData(ds)
        chart.notifyDataSetChanged()
        chart.invalidate()
    }
}
