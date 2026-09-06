package com.shame.tracker.ui.summary

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.shame.tracker.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SessionSummaryFragment : Fragment(R.layout.fragment_session_summary) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Button>(R.id.btnDone).setOnClickListener {
            findNavController().popBackStack(R.id.homeFragment, false)
        }
    }
}
