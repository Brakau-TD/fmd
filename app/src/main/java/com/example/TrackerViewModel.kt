package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.TrackerDatabase
import com.example.data.TrackerLog
import com.example.data.TrackerRepository
import com.example.security.SecurityUtils
import com.example.state.ConnectionState
import com.example.state.TrackerEvent
import com.example.state.TrackerStateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject

class TrackerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TrackerRepository

    // State bindings
    val pairingConfig = MutableStateFlow<com.example.data.PairingConfig?>(null)
    val logs: StateFlow<List<TrackerLog>>
    
    val isServiceRunning = TrackerStateManager.isServiceRunning
    val connectionState = TrackerStateManager.connectionState
    val trackerStatus = TrackerStateManager.trackerStatus
    val lastLocation = TrackerStateManager.lastLocation
    val isPowerSaving = TrackerStateManager.isPowerSaving
    
    val isAlarmRunning = TrackerStateManager.isAlarmRunning
    val isFlashingRunning = TrackerStateManager.isFlashingRunning
    val activeMessage = TrackerStateManager.activeMessage

    // Pairing entry fields
    val inputHost = MutableStateFlow("192.168.1.100")
    val inputPort = MutableStateFlow("8080")
    val isConnectingForPairing = MutableStateFlow(false)

    init {
        val db = TrackerDatabase.getDatabase(application)
        repository = TrackerRepository(db.trackerDao())
        
        logs = repository.trackerLogs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch {
            repository.pairingConfig.collect { config ->
                pairingConfig.value = config
            }
        }
    }

    fun startService(context: Context) {
        val intent = Intent(context, TrackingService::class.java).apply {
            action = TrackingService.ACTION_START
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopService(context: Context) {
        val intent = Intent(context, TrackingService::class.java).apply {
            action = TrackingService.ACTION_STOP
        }
        context.stopService(intent)
    }

    fun triggerReconnect(context: Context) {
        val intent = Intent(context, TrackingService::class.java).apply {
            action = TrackingService.ACTION_RECONNECT
        }
        context.startService(intent)
    }

    fun initializePairingConnection(host: String, portStr: String, context: Context) {
        val port = portStr.toIntOrNull() ?: 8080
        isConnectingForPairing.value = true
        
        viewModelScope.launch {
            repository.addLog("CONNECTION", "Initializing socket handshake with $host:$port...", "INFO")
            // Temporarily save pairing details as unpaired
            repository.savePairing(
                host = host,
                port = port,
                clientId = "T-${(1000..9999).random()}", // Pre-generate temporary client ID
                token = "", // Unpaired
                isPaired = false
            )
            isConnectingForPairing.value = false
            
            // Start the service to open the socket
            startService(context)
        }
    }

    /**
     * Completes the pairing handshake when the app scans the QR code from the client portal.
     * QR format expected:
     * - JSON: {"clientId":"device_abc","token":"secret_key_123"}
     * - Query URL: tracker://pair?clientId=device_abc&token=secret_key_123
     */
    fun processPairingQR(scannedData: String, context: Context) {
        viewModelScope.launch {
            try {
                var cId = ""
                var token = ""

                var currentHost = pairingConfig.value?.serverHost ?: "127.0.0.1"
                var currentPort = pairingConfig.value?.serverPort ?: 8080
                var websocketUrl = ""

                if (scannedData.startsWith("{")) {
                    val json = JSONObject(scannedData)
                    cId = json.optString("clientId", "")
                    token = json.optString("token", "")
                    if (token.isEmpty()) {
                        token = json.optString("secret", "")
                    }
                    
                    if (json.has("serverHost")) {
                        currentHost = json.optString("serverHost")
                        inputHost.value = currentHost
                    }
                    if (json.has("serverPort")) {
                        currentPort = json.optInt("serverPort")
                        inputPort.value = currentPort.toString()
                    }
                    if (json.has("websocketUrl")) {
                        websocketUrl = json.optString("websocketUrl")
                    } else if (json.has("wsUrl")) {
                        websocketUrl = json.optString("wsUrl")
                    }
                } else if (scannedData.startsWith("tracker://pair")) {
                    val uri = android.net.Uri.parse(scannedData)
                    cId = uri.getQueryParameter("clientId") ?: ""
                    token = uri.getQueryParameter("token") ?: ""
                    websocketUrl = uri.getQueryParameter("websocketUrl") ?: uri.getQueryParameter("wsUrl") ?: ""
                } else {
                    // Treat direct string as raw secret key, auto-generate client ID
                    cId = pairingConfig.value?.clientId ?: "DEVICE-${(1000..9999).random()}"
                    token = scannedData.trim()
                }

                if (token.isEmpty()) {
                    repository.addLog("SECURITY", "Pairing rejected: Secret Token is blank", "ERROR")
                    return@launch
                }

                repository.savePairing(
                    host = currentHost,
                    port = currentPort,
                    clientId = cId,
                    token = token,
                    isPaired = true,
                    websocketUrl = websocketUrl
                )

                repository.addLog("SECURITY", "Successfully paired! Secured with HMAC-SHA256 Token.", "SUCCESS")
                
                // Restart service to connect securely
                startService(context)

            } catch (e: Exception) {
                repository.addLog("SECURITY", "Failed to parse pairing QR: ${e.localizedMessage}", "ERROR")
            }
        }
    }

    fun disconnect(context: Context) {
        viewModelScope.launch {
            stopService(context)
            repository.disconnect()
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    fun updateLocationIntervals(activeSec: Int, stationarySec: Int) {
        viewModelScope.launch {
            val current = repository.getPairingConfigDirect() ?: com.example.data.PairingConfig()
            repository.savePairing(
                host = current.serverHost,
                port = current.serverPort,
                clientId = current.clientId,
                token = current.secretToken,
                isPaired = current.isPaired,
                websocketUrl = current.websocketUrl,
                locationIntervalSec = activeSec,
                stationaryIntervalSec = stationarySec
            )
            repository.addLog("SYSTEM", "Tracking frequencies saved: Active=${activeSec}s, Stationary=${stationarySec}s", "SUCCESS")
        }
    }

    // --- Simulation Engines (For Developer Verification and Portal emulation) ---

    fun simulateIncomingCommand(command: String, parameter: String = "") {
        viewModelScope.launch {
            val config = pairingConfig.value ?: return@launch
            
            // Log command inception
            repository.addLog("COMMAND", "Simulating Portal Command execution: $command", "INFO")
            
            // Build command payload JSON
            val payload = JSONObject().apply {
                put("command", command)
                if (parameter.isNotEmpty()) {
                    put("message", parameter)
                }
            }
            
            // Generate valid HMAC signature to simulate actual secured network message
            val timestamp = System.currentTimeMillis()
            val dataToSign = "${config.clientId}|$timestamp|${payload}"
            val signature = SecurityUtils.generateHmacSignature(dataToSign, config.secretToken)
            
            val fullPayloadJson = JSONObject().apply {
                put("clientId", config.clientId)
                put("timestamp", timestamp)
                put("signature", signature)
                put("payload", payload.toString())
            }

            repository.addLog("SECURITY", "Encrypted Tunnel Authenticated [Signature: ${signature.take(8)}...]", "SUCCESS")
            
            // Send into the state receiver
            when (command) {
                "get_current_location" -> {
                    // Fake gps reply
                    val fakeLocation = android.location.Location("GPS").apply {
                        latitude = 37.4220 + ((-50..50).random().toDouble() / 10000.0)
                        longitude = -122.0841 + ((-50..50).random().toDouble() / 10000.0)
                        accuracy = 12.4f
                        speed = 1.5f
                        time = System.currentTimeMillis()
                    }
                    TrackerStateManager.setLastLocation(fakeLocation)
                    repository.addLog("LOCATION", "Synced position: [${fakeLocation.latitude}, ${fakeLocation.longitude}] with server", "INFO")
                }
                "flash_flashlight_and_screen" -> {
                    TrackerStateManager.emitCommandEvent(TrackerEvent.FlashAlerts)
                }
                "display_message_on_screen" -> {
                    val msg = if (parameter.isNotEmpty()) parameter else "Emergency message: Return device immediately!"
                    TrackerStateManager.emitCommandEvent(TrackerEvent.DisplayMessage(msg))
                }
                "trigger_emergency_alarm" -> {
                    TrackerStateManager.emitCommandEvent(TrackerEvent.TriggerAlarm)
                }
            }
        }
    }

    fun simulateSensorMotion() {
        viewModelScope.launch {
            repository.addLog("SENSOR", "Simulating sensor movement... Triggering active mode", "INFO")
            TrackerStateManager.setPowerSaving(false)
            TrackerStateManager.setTrackerStatus("Active")
            repository.addLog("SENSOR", "Motion detected! Resumed high-frequency GPS tracking", "SUCCESS")
        }
    }

    fun simulateSensorStationary() {
        viewModelScope.launch {
            repository.addLog("SENSOR", "Simulating stationary timeout (5-mins idleness)...", "INFO")
            TrackerStateManager.setPowerSaving(true)
            TrackerStateManager.setTrackerStatus("Stationary (Power-Saving)")
            repository.addLog("SENSOR", "Device stationary. Entered low-battery power-saving mode automatically.", "WARNING")
        }
    }

    fun stopSimulatedAlerts() {
        viewModelScope.launch {
            TrackerStateManager.emitCommandEvent(TrackerEvent.StopAlerts)
            repository.addLog("COMMAND", "All active remote alerts silenced and dismissed.", "INFO")
        }
    }
}
