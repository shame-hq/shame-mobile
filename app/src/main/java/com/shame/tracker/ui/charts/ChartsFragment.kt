package com.shame.tracker.ui.charts

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.shame.tracker.R
import com.shame.tracker.data.repository.SessionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class ChartsFragment : Fragment(R.layout.fragment_charts) {

    @Inject
    lateinit var repository: SessionRepository

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val barDistance = view.findViewById<BarChart>(R.id.barChartDistance)
        val barLaps = view.findViewById<BarChart>(R.id.barChartLaps)

        viewLifecycleOwner.lifecycleScope.launch {
            val sessions = repository.getAllSessionsForExport()
                .filter { it.endTimeMs != null }
                .takeLast(10)

            if (sessions.isEmpty()) {
                barDistance.setNoDataText("Complete a run to see charts!")
                barLaps.setNoDataText("Complete a run to see charts!")
                barDistance.invalidate()
                barLaps.invalidate()
                return@launch
            }

            val sdf = SimpleDateFormat("MM/dd", Locale.getDefault())
            val labels = sessions.map { sdf.format(Date(it.startTimeMs)) }

            // Distance chart
            val distEntries = sessions.mapIndexed { i, s ->
                BarEntry(i.toFloat(), (s.totalDistanceMeters / 1000.0).toFloat())
            }
            val distSet = BarDataSet(distEntries, "km").apply {
                color = Color.parseColor("#F97316")
                valueTextColor = Color.GRAY
                valueTextSize = 10f
            }
            styleChart(barDistance, labels)
            barDistance.data = BarData(distSet).apply { barWidth = 0.7f }
            barDistance.invalidate()

            // Laps chart
            val lapEntries = sessions.mapIndexed { i, s ->
                BarEntry(i.toFloat(), s.totalLaps.toFloat())
            }
            val lapSet = BarDataSet(lapEntries, "laps").apply {
                color = Color.parseColor("#1E3A5F")
                valueTextColor = Color.GRAY
                valueTextSize = 10f
            }
            styleChart(barLaps, labels)
            barLaps.data = BarData(lapSet).apply { barWidth = 0.7f }
            barLaps.invalidate()
        }
    }

    private fun styleChart(chart: BarChart, labels: List<String>) {
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setDrawGridBackground(false)
            setDrawBorders(false)
            axisRight.isEnabled = false
            axisLeft.apply {
                setDrawGridLines(false)
                axisMinimum = 0f
            }
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(labels)
                labelRotationAngle = -45f
            }
            animateY(600)
        }
    }
}
