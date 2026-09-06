package com.shame.tracker.ui.home

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.shame.tracker.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    private val viewModel: HomeViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnStart: Button = view.findViewById(R.id.btnStartSession)
        val btnHistory: Button = view.findViewById(R.id.btnHistory)
        val btnCharts: Button = view.findViewById(R.id.btnCharts)
        val btnBests: Button = view.findViewById(R.id.btnBests)
        val btnSettings: Button = view.findViewById(R.id.btnSettings)
        
        val tvLastRunInfo: TextView = view.findViewById(R.id.tvLastRunInfo)

        btnStart.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_active_session)
        }
        btnHistory.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_history)
        }
        btnCharts.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_charts)
        }
        btnBests.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_bests)
        }
        btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_settings)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.lastSession.collect { session ->
                    if (session != null) {
                        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        val dateStr = sdf.format(Date(session.startTimeMs))
                        val distanceStr = String.format(Locale.getDefault(), "%.2f km", session.totalDistanceMeters / 1000.0)
                        tvLastRunInfo.text = "Last Run: $dateStr - $distanceStr (${session.totalLaps} laps)"
                    } else {
                        tvLastRunInfo.text = getString(R.string.no_recent_run)
                    }
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.loadLastSession()
    }
}
