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

    data class ParsedEndpoint(
        val host: String,
        val port: Int,
        val websocketUrl: String
    )

    companion object {
        fun parseEndpoint(
            rawHostInput: String,
            rawPortInput: String,
            explicitWebsocketUrl: String = ""
        ): ParsedEndpoint {
            val trimmedInput = rawHostInput.trim()
            val trimmedExplicitWs = explicitWebsocketUrl.trim()
            val fallbackPort = rawPortInput.trim().toIntOrNull() ?: 8080

            if (trimmedInput.isEmpty()) {
                return ParsedEndpoint("127.0.0.1", fallbackPort, trimmedExplicitWs)
            }

            val lower = trimmedInput.lowercase()
            val hasWsScheme = lower.startsWith("ws://") || lower.startsWith("wss://")
            val hasHttpScheme = lower.startsWith("http://") || lower.startsWith("https://")
            val isSecureScheme = lower.startsWith("wss://") || lower.startsWith("https://")

            val withoutScheme = trimmedInput
                .removePrefix("https://")
                .removePrefix("HTTPS://")
                .removePrefix("http://")
                .removePrefix("HTTP://")
                .removePrefix("wss://")
                .removePrefix("WSS://")
                .removePrefix("ws://")
                .removePrefix("WS://")

            val slashIndex = withoutScheme.indexOf('/')
            val authority = if (slashIndex >= 0) withoutScheme.substring(0, slashIndex) else withoutScheme
            val pathPart = if (slashIndex >= 0) withoutScheme.substring(slashIndex) else ""

            var parsedHost = authority
            var parsedPort = fallbackPort

            val colonIndex = authority.lastIndexOf(':')
            if (colonIndex > 0 && !authority.endsWith("]")) {
                val potentialPort = authority.substring(colonIndex + 1).toIntOrNull()
                if (potentialPort != null) {
                    parsedHost = authority.substring(0, colonIndex)
                    parsedPort = potentialPort
                }
            } else if (hasWsScheme || hasHttpScheme) {
                if (rawPortInput.trim().isEmpty() || (isSecureScheme && fallbackPort == 8080)) {
                    parsedPort = if (isSecureScheme) 443 else 80
                }
            }

            val resolvedWsUrl = when {
                trimmedExplicitWs.isNotEmpty() -> trimmedExplicitWs
                hasWsScheme && pathPart.isNotEmpty() && pathPart != "/" -> trimmedInput
                hasHttpScheme && pathPart.isNotEmpty() && pathPart != "/" -> {
                    val wsScheme = if (isSecureScheme) "wss://" else "ws://"
                    "$wsScheme$authority$pathPart"
                }
                else -> ""
            }

            return ParsedEndpoint(
                host = parsedHost.trim().ifEmpty { "127.0.0.1" },
                port = parsedPort,
                websocketUrl = resolvedWsUrl
            )
        }
    }

    private val repository: TrackerRepository

    // State bindings
    val pairingConfig = MutableStateFlow<com.example.data.PairingConfig?>(null)
    val logs: StateFlow<List<TrackerLog>>
    val serverTargets: StateFlow<List<com.example.data.ServerTarget>>
    
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

        serverTargets = repository.serverTargets.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch {
            repository.pairingConfig.collect { config ->
                pairingConfig.value = config
                if (config != null && config.serverHost.isNotEmpty()) {
                    inputHost.value = config.serverHost
                    inputPort.value = config.serverPort.toString()
                    // Ensure active configured server is present in saved server targets
                    val existingTargets = repository.getServerTargetsDirect()
                    val alreadySaved = existingTargets.any {
                        it.serverHost == config.serverHost &&
                        it.serverPort == config.serverPort &&
                        it.websocketUrl == config.websocketUrl
                    }
                    if (!alreadySaved) {
                        repository.upsertServerTarget(
                            name = config.serverHost,
                            host = config.serverHost,
                            port = config.serverPort,
                            websocketUrl = config.websocketUrl,
                            clientId = config.clientId,
                            secretToken = config.secretToken
                        )
                    }
                }
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
        val parsed = parseEndpoint(host, portStr)
        isConnectingForPairing.value = true
        
        viewModelScope.launch {
            val current = repository.getPairingConfigDirect()
            val retainPaired = current?.isPaired == true && current.secretToken.isNotEmpty()
            val cId = current?.clientId?.takeIf { it.isNotEmpty() } ?: "T-${(1000..9999).random()}"
            val token = if (retainPaired) current!!.secretToken else ""

            repository.addLog("CONNECTION", "Initializing socket handshake with ${parsed.host}:${parsed.port}...", "INFO")
            repository.savePairing(
                host = parsed.host,
                port = parsed.port,
                clientId = cId,
                token = token,
                isPaired = retainPaired,
                websocketUrl = parsed.websocketUrl
            )
            repository.upsertServerTarget(
                name = parsed.host,
                host = parsed.host,
                port = parsed.port,
                websocketUrl = parsed.websocketUrl,
                clientId = cId,
                secretToken = token
            )
            inputHost.value = parsed.host
            inputPort.value = parsed.port.toString()
            isConnectingForPairing.value = false
            
            // Start the service to open the socket
            startService(context)
        }
    }

    fun saveAndActivateServerTarget(
        name: String,
        rawHost: String,
        rawPort: String,
        explicitWsUrl: String = "",
        clientIdOverride: String = "",
        secretTokenOverride: String = "",
        targetId: Long = 0,
        context: Context
    ) {
        val parsed = parseEndpoint(rawHost, rawPort, explicitWsUrl)
        viewModelScope.launch {
            val current = repository.getPairingConfigDirect()
            val resolvedClientId = clientIdOverride.trim().ifEmpty {
                current?.clientId?.takeIf { it.isNotEmpty() } ?: "DEVICE-${(1000..9999).random()}"
            }
            val resolvedToken = secretTokenOverride.trim().ifEmpty {
                current?.secretToken ?: ""
            }
            val isPaired = resolvedToken.isNotEmpty() || (current?.isPaired == true)

            val savedTarget = repository.upsertServerTarget(
                id = targetId,
                name = name.trim().ifEmpty { parsed.host },
                host = parsed.host,
                port = parsed.port,
                websocketUrl = parsed.websocketUrl,
                clientId = resolvedClientId,
                secretToken = resolvedToken
            )

            repository.savePairing(
                host = savedTarget.serverHost,
                port = savedTarget.serverPort,
                clientId = resolvedClientId,
                token = resolvedToken,
                isPaired = isPaired,
                websocketUrl = savedTarget.websocketUrl
            )

            inputHost.value = savedTarget.serverHost
            inputPort.value = savedTarget.serverPort.toString()

            val endpointLabel = if (savedTarget.websocketUrl.isNotEmpty()) {
                savedTarget.websocketUrl
            } else {
                "${savedTarget.serverHost}:${savedTarget.serverPort}"
            }
            repository.addLog(
                "CONNECTION",
                "Switched target server to '${savedTarget.name}' [$endpointLabel]",
                "SUCCESS"
            )

            startService(context)
        }
    }

    fun saveServerTargetOnly(
        name: String,
        rawHost: String,
        rawPort: String,
        explicitWsUrl: String = "",
        clientIdOverride: String = "",
        secretTokenOverride: String = "",
        targetId: Long = 0
    ) {
        val parsed = parseEndpoint(rawHost, rawPort, explicitWsUrl)
        viewModelScope.launch {
            val current = repository.getPairingConfigDirect()
            val resolvedClientId = clientIdOverride.trim().ifEmpty { current?.clientId ?: "" }
            val resolvedToken = secretTokenOverride.trim().ifEmpty { current?.secretToken ?: "" }

            val saved = repository.upsertServerTarget(
                id = targetId,
                name = name.trim().ifEmpty { parsed.host },
                host = parsed.host,
                port = parsed.port,
                websocketUrl = parsed.websocketUrl,
                clientId = resolvedClientId,
                secretToken = resolvedToken
            )
            repository.addLog(
                "SYSTEM",
                "Saved server target '${saved.name}' (${saved.serverHost}:${saved.serverPort})",
                "INFO"
            )
        }
    }

    fun switchToServerTarget(target: com.example.data.ServerTarget, context: Context) {
        viewModelScope.launch {
            val current = repository.getPairingConfigDirect()
            val resolvedClientId = target.clientId.ifEmpty {
                current?.clientId?.takeIf { it.isNotEmpty() } ?: "DEVICE-${(1000..9999).random()}"
            }
            val resolvedToken = target.secretToken.ifEmpty {
                current?.secretToken ?: ""
            }
            val isPaired = resolvedToken.isNotEmpty() || (current?.isPaired == true)

            repository.upsertServerTarget(
                id = target.id,
                name = target.name,
                host = target.serverHost,
                port = target.serverPort,
                websocketUrl = target.websocketUrl,
                clientId = resolvedClientId,
                secretToken = resolvedToken
            )

            repository.savePairing(
                host = target.serverHost,
                port = target.serverPort,
                clientId = resolvedClientId,
                token = resolvedToken,
                isPaired = isPaired,
                websocketUrl = target.websocketUrl
            )

            inputHost.value = target.serverHost
            inputPort.value = target.serverPort.toString()

            val endpointLabel = if (target.websocketUrl.isNotEmpty()) {
                target.websocketUrl
            } else {
                "${target.serverHost}:${target.serverPort}"
            }
            repository.addLog(
                "CONNECTION",
                "Switched active target server to '${target.name}' [$endpointLabel]",
                "SUCCESS"
            )

            startService(context)
        }
    }

    fun deleteServerTarget(target: com.example.data.ServerTarget) {
        viewModelScope.launch {
            repository.deleteServerTarget(target.id)
            repository.addLog(
                "SYSTEM",
                "Removed saved server target '${target.name}' (${target.serverHost}:${target.serverPort})",
                "INFO"
            )
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

                val fallbackEndpoint = parseEndpoint(
                    rawHostInput = inputHost.value.ifBlank { pairingConfig.value?.serverHost ?: "127.0.0.1" },
                    rawPortInput = inputPort.value.ifBlank { (pairingConfig.value?.serverPort ?: 8080).toString() },
                    explicitWebsocketUrl = pairingConfig.value?.websocketUrl ?: ""
                )
                var currentHost = fallbackEndpoint.host
                var currentPort = fallbackEndpoint.port
                var websocketUrl = fallbackEndpoint.websocketUrl

                if (scannedData.startsWith("{")) {
                    val json = JSONObject(scannedData)
                    cId = json.optString("clientId", "")
                    token = json.optString("token", "")
                    if (token.isEmpty()) {
                        token = json.optString("secret", "")
                    }
                    
                    if (json.has("serverHost") && json.optString("serverHost").isNotBlank()) {
                        currentHost = json.optString("serverHost").trim()
                        inputHost.value = currentHost
                    }
                    if (json.has("serverPort")) {
                        currentPort = json.optInt("serverPort", currentPort)
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
                    uri.getQueryParameter("serverHost")?.takeIf { it.isNotBlank() }?.let {
                        currentHost = it.trim()
                        inputHost.value = currentHost
                    }
                    uri.getQueryParameter("serverPort")?.toIntOrNull()?.let {
                        currentPort = it
                        inputPort.value = currentPort.toString()
                    }
                    val uriWs = uri.getQueryParameter("websocketUrl") ?: uri.getQueryParameter("wsUrl") ?: ""
                    if (uriWs.isNotEmpty()) {
                        websocketUrl = uriWs
                    }
                } else {
                    // Treat direct string as raw secret key, auto-generate client ID
                    cId = pairingConfig.value?.clientId?.takeIf { it.isNotEmpty() } ?: "DEVICE-${(1000..9999).random()}"
                    token = scannedData.trim()
                }

                if (token.isEmpty()) {
                    repository.addLog("SECURITY", "Pairing rejected: Secret Token is blank", "ERROR")
                    return@launch
                }

                val parsedFinal = parseEndpoint(currentHost, currentPort.toString(), websocketUrl)

                repository.savePairing(
                    host = parsedFinal.host,
                    port = parsedFinal.port,
                    clientId = cId,
                    token = token,
                    isPaired = true,
                    websocketUrl = parsedFinal.websocketUrl
                )

                repository.upsertServerTarget(
                    name = parsedFinal.host,
                    host = parsedFinal.host,
                    port = parsedFinal.port,
                    websocketUrl = parsedFinal.websocketUrl,
                    clientId = cId,
                    secretToken = token
                )

                repository.addLog("SECURITY", "Successfully paired with ${parsedFinal.host}:${parsedFinal.port}! Secured with HMAC-SHA256 Token.", "SUCCESS")
                
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
