package com.shame.tracker.ui.bests

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.shame.tracker.R
import com.shame.tracker.data.repository.SessionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class PersonalBestsFragment : Fragment(R.layout.fragment_personal_bests) {

    @Inject
    lateinit var repository: SessionRepository

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvDistance = view.findViewById<TextView>(R.id.tvBestDistance)
        val tvLaps = view.findViewById<TextView>(R.id.tvBestLaps)
        val tvPace = view.findViewById<TextView>(R.id.tvBestPace)
        val tvTotal = view.findViewById<TextView>(R.id.tvTotalRuns)

        viewLifecycleOwner.lifecycleScope.launch {
            val longestDist = repository.getLongestDistanceSession()
            if (longestDist != null && longestDist.totalDistanceMeters > 0) {
                tvDistance.text = String.format(Locale.getDefault(), "%.2f km", longestDist.totalDistanceMeters / 1000.0)
            } else {
                tvDistance.text = "-- km"
            }

            val mostLaps = repository.getMostLapsSession()
            if (mostLaps != null && mostLaps.totalLaps > 0) {
                tvLaps.text = "${mostLaps.totalLaps} laps"
            } else {
                tvLaps.text = "-- laps"
            }

            val bestPace = repository.getBestPaceSession()
            if (bestPace != null && bestPace.totalDistanceMeters > 0) {
                val paceMin = (bestPace.totalDurationMs / 1000.0 / 60.0) / (bestPace.totalDistanceMeters / 1000.0)
                val m = paceMin.toInt()
                val s = ((paceMin - m) * 60).toInt()
                tvPace.text = String.format(Locale.getDefault(), "%d:%02d /km", m, s)
            } else {
                tvPace.text = "-- /km"
            }

            val count = repository.getCompletedSessionCount()
            tvTotal.text = "$count runs"
        }
    }
}
