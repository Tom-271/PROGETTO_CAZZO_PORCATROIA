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

class PesoChartFragment : Fragment(R.layout.fragment_chart_peso) {

    private lateinit var chart: LineChart
    private lateinit var vm: ProgressionViewModel
    private var targetLine: LimitLine? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        chart = view.findViewById(R.id.chartPeso)
        vm = ViewModelProvider(requireParentFragment())[ProgressionViewModel::class.java]

        setupChart()
        observeData()
        observeGoals()
    }

    private fun setupChart() = chart.apply {
        description.isEnabled = false
        setNoDataText("Nessun dato Peso")
        setTouchEnabled(true)
        setPinchZoom(true)
        legend.isEnabled = false
        axisRight.isEnabled = false
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        axisLeft.setDrawGridLines(true)
        xAxis.textColor = Color.LTGRAY
        axisLeft.textColor = Color.LTGRAY
        setExtraOffsets(8f,16f,8f,16f)
    }

    private fun observeData() {
        vm.bodyFatEntries.observe(viewLifecycleOwner) { list ->
            updateChart(list)
        }
    }

    private fun observeGoals() {
        vm.goals.observe(viewLifecycleOwner) { g ->
            val target = g.targetLean?.toFloat() ?: return@observe
            val yAxis = chart.axisLeft
            targetLine?.let { yAxis.removeLimitLine(it) }
            targetLine = LimitLine(target, "Target Weight").apply {
                lineWidth = 2f
                textSize = 10f
                lineColor = Color.GREEN
                textColor = Color.GREEN
                labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
            }
            yAxis.addLimitLine(targetLine)
            chart.invalidate()
        }
    }

    private fun applyTargetLine() {
        val target = vm.goals.value?.targetLean?.toFloat() ?: return
        val yAxis = chart.axisLeft
        // se fuori range axis verrà corretto in updateChart, ma se lo chiami dopo updateChart è ok
        targetLine?.let { yAxis.removeLimitLine(it) }
        targetLine = LimitLine(target, "Target Peso").apply {
            lineWidth = 2f
            textSize = 10f
            lineColor = Color.GREEN
            textColor = Color.GREEN
            labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
        }
        yAxis.addLimitLine(targetLine)
    }

    private fun updateChart(entries: List<BodyFatEntry>) {
        val valid = entries.filter { it.bodyWeightKg != null }
        if (valid.isEmpty()) {
            chart.clear()
            return
        }
        val sorted = valid.sortedBy { it.epochDay }

        val pts = sorted.mapIndexed { i, e -> Entry(i.toFloat(), e.bodyWeightKg!!) }

        val ds = LineDataSet(pts, "Peso (kg)").apply {
            axisDependency = YAxis.AxisDependency.LEFT
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
            setDrawValues(false)
            lineWidth = 2f
            setDrawCircles(true)
            circleRadius = 3f
            color = Color.CYAN
            setCircleColor(Color.CYAN)
            highLightColor = Color.MAGENTA
            setDrawFilled(true)
            fillColor = Color.parseColor("#3324B5FF")
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

        val minW = sorted.minOf { it.bodyWeightKg!! }
        val maxW = sorted.maxOf { it.bodyWeightKg!! }
        val target = vm.goals.value?.targetLean?.toFloat()

        var axisMin = (minW - 2f).coerceAtLeast(0f)
        var axisMax = maxW + 2f
        if (target != null) {
            if (target < axisMin) axisMin = target - 1f
            if (target > axisMax) axisMax = target + 1f
        }

        chart.axisLeft.axisMinimum = axisMin
        chart.axisLeft.axisMaximum = axisMax

        chart.data = LineData(ds)

        applyTargetLine()

        chart.animateX(300)
        chart.invalidate()
    }
}
