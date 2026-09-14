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
        val layoutEmpty = view.findViewById<View?>(R.id.layoutEmptyCharts)
        val cardDistance = view.findViewById<View?>(R.id.cardDistanceChart)
        val cardLaps = view.findViewById<View?>(R.id.cardLapsChart)

        viewLifecycleOwner.lifecycleScope.launch {
            val sessions = repository.getAllSessionsForExport()
                .filter { it.endTimeMs != null }
                .takeLast(10)

            if (sessions.isEmpty()) {
                layoutEmpty?.visibility = View.VISIBLE
                cardDistance?.visibility = View.GONE
                cardLaps?.visibility = View.GONE
                barDistance.setNoDataText("Complete a run to see charts!")
                barLaps.setNoDataText("Complete a run to see charts!")
                barDistance.setNoDataTextColor(Color.parseColor("#94A3B8"))
                barLaps.setNoDataTextColor(Color.parseColor("#94A3B8"))
                barDistance.invalidate()
                barLaps.invalidate()
                return@launch
            } else {
                layoutEmpty?.visibility = View.GONE
                cardDistance?.visibility = View.VISIBLE
                cardLaps?.visibility = View.VISIBLE
            }

            val sdf = SimpleDateFormat("MM/dd", Locale.getDefault())
            val labels = sessions.map { sdf.format(Date(it.startTimeMs)) }

            // Distance chart
            val distEntries = sessions.mapIndexed { i, s ->
                BarEntry(i.toFloat(), (s.totalDistanceMeters / 1000.0).toFloat())
            }
            val distSet = BarDataSet(distEntries, "km").apply {
                color = Color.parseColor("#F97316")
                valueTextColor = Color.WHITE
                valueTextSize = 11f
            }
            styleChart(barDistance, labels)
            barDistance.data = BarData(distSet).apply { barWidth = 0.65f }
            barDistance.invalidate()

            // Laps chart
            val lapEntries = sessions.mapIndexed { i, s ->
                BarEntry(i.toFloat(), s.totalLaps.toFloat())
            }
            val lapSet = BarDataSet(lapEntries, "laps").apply {
                color = Color.parseColor("#06B6D4")
                valueTextColor = Color.WHITE
                valueTextSize = 11f
            }
            styleChart(barLaps, labels)
            barLaps.data = BarData(lapSet).apply { barWidth = 0.65f }
            barLaps.invalidate()
        }
    }

    private fun styleChart(chart: BarChart, labels: List<String>) {
        chart.apply {
            setBackgroundColor(Color.TRANSPARENT)
            description.isEnabled = false
            legend.isEnabled = false
            setDrawGridBackground(false)
            setDrawBorders(false)
            setFitBars(true)
            axisRight.isEnabled = false
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#1FFFFFFF")
                setDrawAxisLine(false)
                textColor = Color.parseColor("#94A3B8")
                textSize = 10f
                axisMinimum = 0f
            }
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawAxisLine(false)
                textColor = Color.parseColor("#94A3B8")
                textSize = 10f
                granularity = 1f
                valueFormatter = IndexAxisValueFormatter(labels)
                labelRotationAngle = -35f
            }
            animateY(600)
        }
    }
}
