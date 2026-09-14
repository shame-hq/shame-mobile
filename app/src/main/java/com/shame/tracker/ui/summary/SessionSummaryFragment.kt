package com.shame.tracker.ui.summary

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.shame.tracker.R
import com.shame.tracker.data.repository.SessionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class SessionSummaryFragment : Fragment(R.layout.fragment_session_summary) {

    @Inject
    lateinit var repository: SessionRepository

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnDone = view.findViewById<Button>(R.id.btnDone)
        val tvDistance = view.findViewById<TextView>(R.id.tvSummaryDistance)
        val tvDuration = view.findViewById<TextView>(R.id.tvSummaryDuration)
        val tvPace = view.findViewById<TextView>(R.id.tvSummaryPace)
        val tvLaps = view.findViewById<TextView>(R.id.tvSummaryLaps)
        val tvDate = view.findViewById<TextView>(R.id.tvSummaryDate)

        btnDone.setOnClickListener {
            findNavController().popBackStack(R.id.homeFragment, false)
        }

        val sessionId = arguments?.getLong("sessionId") ?: -1L
        if (sessionId > 0) {
            viewLifecycleOwner.lifecycleScope.launch {
                val session = repository.getSession(sessionId)
                if (session != null) {
                    val distKm = session.totalDistanceMeters / 1000.0
                    tvDistance?.text = String.format(Locale.getDefault(), "%.2f", distKm)

                    val sec = session.totalDurationMs / 1000
                    tvDuration?.text = String.format(Locale.getDefault(), "%02d:%02d", sec / 60, sec % 60)

                    val paceSec = if (distKm > 0) ((sec / distKm)).toLong() else 0L
                    tvPace?.text = if (paceSec > 0)
                        String.format(Locale.getDefault(), "%d:%02d /km", paceSec / 60, paceSec % 60)
                    else "-- /km"

                    tvLaps?.text = "${session.totalLaps}"

                    val sdf = SimpleDateFormat("EEEE, MMM dd · HH:mm", Locale.getDefault())
                    tvDate?.text = sdf.format(Date(session.startTimeMs))
                }
            }
        }
    }
}
