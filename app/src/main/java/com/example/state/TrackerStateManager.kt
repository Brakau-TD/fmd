package com.example.state

import android.location.Location
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

sealed interface TrackerEvent {
    data class DisplayMessage(val message: String) : TrackerEvent
    object TriggerAlarm : TrackerEvent
    object FlashAlerts : TrackerEvent
    object StopAlerts : TrackerEvent
}

object TrackerStateManager {
    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning = _isServiceRunning.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()

    private val _trackerStatus = MutableStateFlow("Inactive") // "Active", "Stationary (Power-Saving)", "Inactive"
    val trackerStatus = _trackerStatus.asStateFlow()

    private val _lastLocation = MutableStateFlow<Location?>(null)
    val lastLocation = _lastLocation.asStateFlow()

    private val _isPowerSaving = MutableStateFlow(false)
    val isPowerSaving = _isPowerSaving.asStateFlow()

    private val _logsUpdated = MutableSharedFlow<Unit>(replay = 1)
    val logsUpdated = _logsUpdated.asSharedFlow()

    // Flow of incoming commands for UI to consume
    private val _commandEvents = MutableSharedFlow<TrackerEvent>(extraBufferCapacity = 64)
    val commandEvents = _commandEvents.asSharedFlow()

    // Alert states
    val isAlarmRunning = MutableStateFlow(false)
    val isFlashingRunning = MutableStateFlow(false)
    val activeMessage = MutableStateFlow<String?>(null)

    fun setServiceRunning(running: Boolean) {
        _isServiceRunning.value = running
    }

    fun setConnectionState(state: ConnectionState) {
        _connectionState.value = state
    }

    fun setTrackerStatus(status: String) {
        _trackerStatus.value = status
    }

    fun setLastLocation(location: Location?) {
        _lastLocation.value = location
    }

    fun setPowerSaving(enabled: Boolean) {
        _isPowerSaving.value = enabled
    }

    suspend fun triggerLogsUpdate() {
        _logsUpdated.emit(Unit)
    }

    suspend fun emitCommandEvent(event: TrackerEvent) {
        _commandEvents.emit(event)
        when (event) {
            is TrackerEvent.DisplayMessage -> activeMessage.value = event.message
            TrackerEvent.TriggerAlarm -> isAlarmRunning.value = true
            TrackerEvent.FlashAlerts -> isFlashingRunning.value = true
            TrackerEvent.StopAlerts -> {
                isAlarmRunning.value = false
                isFlashingRunning.value = false
                activeMessage.value = null
            }
        }
    }
}
