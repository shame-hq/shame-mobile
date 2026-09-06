package com.shame.tracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shame.tracker.data.db.entity.Session
import com.shame.tracker.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: SessionRepository
) : ViewModel() {

    private val _lastSession = MutableStateFlow<Session?>(null)
    val lastSession: StateFlow<Session?> = _lastSession.asStateFlow()

    init {
        loadLastSession()
    }

    fun loadLastSession() {
        viewModelScope.launch {
            _lastSession.value = repository.getLatestSession()
        }
    }
}
