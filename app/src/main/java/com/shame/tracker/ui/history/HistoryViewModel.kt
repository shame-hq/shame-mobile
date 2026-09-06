package com.shame.tracker.ui.history

import androidx.lifecycle.ViewModel
import com.shame.tracker.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: SessionRepository
) : ViewModel() {
    val sessions = repository.observeAllSessions()
}
