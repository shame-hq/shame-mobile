package com.shame.tracker.ui.session

import androidx.lifecycle.ViewModel
import com.shame.tracker.service.TrackingService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ActiveSessionViewModel @Inject constructor() : ViewModel() {
    val trackingState = TrackingService.trackingState
}
