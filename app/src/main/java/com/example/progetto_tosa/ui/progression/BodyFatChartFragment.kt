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
import java.time.LocalDate

class BodyFatChartFragment : Fragment(R.layout.fragment_chart_bodyfat) {

    private lateinit var chart: LineChart
    private lateinit var vm: ProgressionViewModel
    private var targetLine: LimitLine? = null
    private var currentEntries: List<BodyFatEntry> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        chart = view.findViewById(R.id.chartBodyFat)
        vm = ViewModelProvider(requireParentFragment())[ProgressionViewModel::class.java]

        setupChart()
        observeData()
        observeGoals()
    }

    private fun setupChart() = chart.apply {
        description.isEnabled = false
        setNoDataText("Nessun dato BodyFat")
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
            // Rimuovi tutte le linee esistenti prima di aggiungere la nuova
            chart.axisLeft.removeAllLimitLines()
            updateTargetLine(g.targetFat?.toFloat())
            updateChart(currentEntries)
        }
    }

    private fun updateTargetLine(target: Float?) {
        target?.let { targetValue ->
            targetLine = LimitLine(targetValue, "Target BF").apply {
                lineWidth = 2f
                textSize = 10f
                lineColor = Color.GREEN
                textColor = Color.GREEN
                labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
            }
            chart.axisLeft.addLimitLine(targetLine)
            chart.invalidate()
        }
    }

    private fun updateChart(entries: List<BodyFatEntry>) {
        if (entries.isEmpty()) {
            chart.clear()
            chart.invalidate()
            return
        }

        val sorted = entries.sortedBy { it.epochDay }
        val pts = sorted.mapIndexed { i, e -> Entry(i.toFloat(), e.bodyFatPercent) }

        val ds = LineDataSet(pts, "BodyFat %").apply {
            axisDependency = YAxis.AxisDependency.LEFT
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
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
                val i = value.toInt()
                return if (i in sorted.indices) {
                    val d = LocalDate.ofEpochDay(sorted[i].epochDay)
                    "${d.dayOfMonth}/${d.monthValue}"
                } else ""
            }
        }

        // Calcola la scala Y considerando anche la linea target
        val maxBf = sorted.maxOf { it.bodyFatPercent }
        val targetBf = vm.goals.value?.targetFat?.toFloat() ?: 0f
        val maxYValue = maxOf(maxBf + 5f, targetBf + 5f).coerceAtLeast(30f)

        chart.axisLeft.apply {
            axisMinimum = 0f
            axisMaximum = maxYValue
        }

        // Riaggiungi la linea target se esiste
        vm.goals.value?.targetFat?.toFloat()?.let { currentTarget ->
            if (targetLine == null || targetLine?.limit != currentTarget) {
                updateTargetLine(currentTarget)
            }
        }

        chart.data = LineData(ds)
        chart.notifyDataSetChanged()
        chart.invalidate()
    }
}