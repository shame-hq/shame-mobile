package com.shame.tracker.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shame.tracker.data.db.entity.GpsPoint
import com.shame.tracker.data.db.entity.Lap
import com.shame.tracker.data.db.entity.Session
import com.shame.tracker.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    private val repository: SessionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sessionId: Long = savedStateHandle.get<Long>("sessionId") ?: -1L

    private val _session = MutableStateFlow<Session?>(null)
    val session = _session.asStateFlow()

    private val _laps = MutableStateFlow<List<Lap>>(emptyList())
    val laps = _laps.asStateFlow()

    private val _points = MutableStateFlow<List<GpsPoint>>(emptyList())
    val points = _points.asStateFlow()

    init {
        if (sessionId != -1L) {
            viewModelScope.launch {
                _session.value = repository.getSession(sessionId)
            }
            viewModelScope.launch {
                repository.observeLaps(sessionId).collect { _laps.value = it }
            }
            viewModelScope.launch {
                _points.value = repository.getGpsPoints(sessionId)
            }
        }
    }
}
