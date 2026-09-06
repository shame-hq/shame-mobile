package com.shame.tracker.ui.session

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.Button
import com.shame.tracker.data.model.TrackingState
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.shame.tracker.R
import com.shame.tracker.service.TrackingService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class ActiveSessionFragment : Fragment(R.layout.fragment_active_session) {

    private var trackingService: TrackingService? = null
    private var isBound = false
    private lateinit var lapAdapter: LapSplitAdapter

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as TrackingService.LocalBinder
            trackingService = binder.getService()
            isBound = true
            observeState()
        }
        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lapAdapter = LapSplitAdapter()
        val rvLaps = view.findViewById<RecyclerView>(R.id.rvLaps)
        rvLaps.layoutManager = LinearLayoutManager(requireContext())
        rvLaps.adapter = lapAdapter

        val startIntent = Intent(requireContext(), TrackingService::class.java).apply {
            action = TrackingService.ACTION_START
        }
        ContextCompat.startForegroundService(requireContext(), startIntent)

        val bindIntent = Intent(requireContext(), TrackingService::class.java)
        requireContext().bindService(bindIntent, connection, Context.BIND_AUTO_CREATE)

        view.findViewById<Button>(R.id.btnEndLap).setOnClickListener {
            trackingService?.recordLap()
        }

        view.findViewById<Button>(R.id.btnPause).setOnClickListener {
            val srv = trackingService ?: return@setOnClickListener
            if (srv.isPaused()) {
                srv.resumeSession()
            } else {
                srv.pauseSession()
            }
        }

        view.findViewById<Button>(R.id.btnEndSession).setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.confirm_end_session)
                .setMessage(R.string.confirm_end_session_msg)
                .setPositiveButton(R.string.confirm) { _, _ ->
                    val srv = trackingService
                    if (srv != null) {
                        lifecycleScope.launch {
                            val sessionId = srv.endSessionAndSave()
                            requireContext().unbindService(connection)
                            isBound = false
                            val stopIntent = Intent(requireContext(), TrackingService::class.java).apply {
                                action = TrackingService.ACTION_STOP
                            }
                            requireContext().startService(stopIntent)
                            
                            val bundle = Bundle().apply { putLong("sessionId", sessionId) }
                            findNavController().navigate(R.id.action_session_to_summary, bundle)
                        }
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    TrackingService.trackingState.collect { state ->
                        updateUI(state)
                    }
                }
                launch {
                    TrackingService.laps.collect { laps ->
                        lapAdapter.submitList(laps)
                        view?.findViewById<RecyclerView>(R.id.rvLaps)?.scrollToPosition(laps.size - 1)
                    }
                }
            }
        }
    }

    private fun updateUI(state: TrackingState) {
        val view = view ?: return
        val tvLap = view.findViewById<TextView>(R.id.tvLap)
        val tvTime = view.findViewById<TextView>(R.id.tvTime)
        val tvSessionTime = view.findViewById<TextView>(R.id.tvSessionTime)
        val tvDistance = view.findViewById<TextView>(R.id.tvDistance)
        val tvPace = view.findViewById<TextView>(R.id.tvPace)
        val btnPause = view.findViewById<Button>(R.id.btnPause)

        tvLap.text = "Lap ${state.lapCount}"
        
        // Main timer is current lap time
        val lapSecs = state.currentLapElapsedMs / 1000
        tvTime.text = String.format(Locale.getDefault(), "%02d:%02d", lapSecs / 60, lapSecs % 60)
        
        // Session timer
        val sesSecs = state.sessionElapsedMs / 1000
        tvSessionTime.text = String.format(Locale.getDefault(), "Total Time: %02d:%02d", sesSecs / 60, sesSecs % 60)
        
        tvDistance.text = String.format(Locale.getDefault(), "%.2f km", state.sessionDistanceMeters / 1000.0)
        tvPace.text = String.format(Locale.getDefault(), "%.2f min/km", state.currentPaceMinutesPerKm)

        btnPause.text = if (state.isPaused) getString(R.string.resume_session) else getString(R.string.pause_session)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (isBound) {
            requireContext().unbindService(connection)
            isBound = false
        }
    }
}
