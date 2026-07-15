package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.data.TrackerDatabase
import com.example.data.TrackerRepository
import com.example.security.SecurityUtils
import com.example.state.ConnectionState
import com.example.state.TrackerEvent
import com.example.state.TrackerStateManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.sqrt

class TrackingService : Service(), SensorEventListener {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_RECONNECT = "ACTION_RECONNECT"
        
        private const val NOTIFICATION_ID = 404
        private const val CHANNEL_ID = "tracker_service_channel"
        private const val STATIONARY_THRESHOLD_MS = 5 * 60 * 1000L // 5 minutes standard
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var repository: TrackerRepository
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    
    private var webSocket: WebSocket? = null
    private val okHttpClient = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private var lastMotionTime = System.currentTimeMillis()
    private var isCurrentlyStationary = false
    private var lastSavedConfig: com.example.data.PairingConfig? = null

    // Location request parameters
    private var locationCallback: LocationCallback? = null
    private var isLocationCallbackRegistered = false
    private val locationHandler = Handler(Looper.getMainLooper())

    // Alarm ringtone
    private var ringtone: Ringtone? = null
    private var alarmTimeoutJob: kotlinx.coroutines.Job? = null

    // Flashlight flashing fields
    private var isFlashingActive = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var pairingConfigJob: kotlinx.coroutines.Job? = null
    private var reconnectDelayMs = 1000L

    override fun onCreate() {
        super.onCreate()
        
        val db = TrackerDatabase.getDatabase(this)
        repository = TrackerRepository(db.trackerDao())
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        // Setup motion sensor
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // Setup wake lock to hold connection when screen goes off (deferred to onStartCommand)
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            //wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FindMyDevice:TrackerWakeLock")
        } catch (e: Exception) {
            serviceScope.launch {
                repository.addLog("SYSTEM", "Wake lock initialization bypassed: ${e.localizedMessage}", "WARNING")
            }
        }

        createNotificationChannel()

        // Monitor alert state cancellations
        serviceScope.launch {
            TrackerStateManager.isAlarmRunning.collectLatest { running ->
                if (running) {
                    playAlarm()
                } else {
                    stopAlarm()
                }
            }
        }

        serviceScope.launch {
            TrackerStateManager.isFlashingRunning.collectLatest { running ->
                if (running) {
                    startFlashingFlashlight()
                } else {
                    stopFlashingFlashlight()
                }
            }
        }

        // Auto launch/bring MainActivity to front on emergency commands
        serviceScope.launch {
            TrackerStateManager.commandEvents.collect { event ->
                if (event is TrackerEvent.TriggerAlarm || event is TrackerEvent.FlashAlerts || event is TrackerEvent.DisplayMessage) {
                    try {
                        val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        }
                        startActivity(launchIntent)
                    } catch (e: Exception) {
                        repository.addLog("SYSTEM", "Auto foreground launch failed: ${e.localizedMessage}", "WARNING")
                    }
                }
            }
        }

        // Periodic stationary check loop
        serviceScope.launch {
            while (true) {
                delay(10000) // Check every 10 seconds
                checkStationaryState()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        
        when (action) {
            ACTION_START -> {
                reconnectDelayMs = 1000L
                startForegroundServiceCompat()
                
                // Acquire wake lock after becoming a foreground service
                try {
                    wakeLock?.let {
                        if (!it.isHeld) it.acquire(10 * 60 * 1000L /*10 minutes default safety*/)
                    }
                } catch (e: Exception) {
                    serviceScope.launch {
                        repository.addLog("SYSTEM", "Wake lock acquisition bypassed: ${e.localizedMessage}", "WARNING")
                    }
                }
                
                TrackerStateManager.setServiceRunning(true)
                
                // Safely listen to pairing changes and state once foreground service is active
                pairingConfigJob?.cancel()
                pairingConfigJob = serviceScope.launch {
                    repository.pairingConfig.collectLatest { config ->
                        val prevConfig = lastSavedConfig
                        lastSavedConfig = config
                        if (config != null && config.isPaired) {
                            reconnectDelayMs = 1000L
                            connectWebSocket(config)
                            if (prevConfig != null && 
                                (prevConfig.locationIntervalSec != config.locationIntervalSec || 
                                 prevConfig.stationaryIntervalSec != config.stationaryIntervalSec)) {
                                serviceScope.launch {
                                    repository.addLog("SYSTEM", "Tracking intervals updated. Applying changes...", "INFO")
                                }
                                startTracking()
                            }
                        } else {
                            disconnectWebSocket()
                        }
                    }
                }
                
                serviceScope.launch {
                    delay(1000)
                    startTracking()
                }
                serviceScope.launch {
                    repository.addLog("CONNECTION", "Background tracking service started", "INFO")
                }
            }
            ACTION_STOP -> {
                pairingConfigJob?.cancel()
                pairingConfigJob = null
                serviceScope.launch {
                    repository.addLog("CONNECTION", "Background tracking service stopped", "WARNING")
                    stopSelf()
                }
            }
            ACTION_RECONNECT -> {
                reconnectDelayMs = 1000L
                serviceScope.launch {
                    lastSavedConfig?.let {
                        repository.addLog("CONNECTION", "Manual reconnection triggered", "INFO")
                        connectWebSocket(it)
                    }
                }
            }
        }
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        stopTracking()
        disconnectWebSocket()
        stopAlarm()
        stopFlashingFlashlight()
        
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        
        TrackerStateManager.setServiceRunning(false)
        TrackerStateManager.setConnectionState(ConnectionState.DISCONNECTED)
        TrackerStateManager.setTrackerStatus("Inactive")
        serviceScope.cancel()
        super.onDestroy()
    }

    // --- Foreground Service Compatibility ---

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Device Tracker Active Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps tracking connection active in background"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceCompat() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Find My Device Active")
            .setContentText("Continuously tracking location and syncing status...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID, 
                    notification, 
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            serviceScope.launch {
                repository.addLog("SYSTEM", "Failed starting location foreground service: ${e.localizedMessage}. Trying generic...", "ERROR")
            }
            try {
                startForeground(NOTIFICATION_ID, notification)
            } catch (fallbackEx: Exception) {
                serviceScope.launch {
                    repository.addLog("SYSTEM", "All startForeground attempts failed: ${fallbackEx.localizedMessage}", "ERROR")
                }
                stopSelf()
            }
        }
    }

    // --- WebSocket Network Sync & Secure Commands ---

    private fun isPublicHostname(host: String): Boolean {
        val h = host.trim().lowercase()
        if (h == "localhost" || h == "127.0.0.1" || h == "::1") {
            return false
        }
        if (h.startsWith("192.168.") || h.startsWith("10.")) {
            return false
        }
        if (h.startsWith("172.")) {
            val parts = h.split(".")
            if (parts.size >= 2) {
                val secondOctet = parts[1].toIntOrNull()
                if (secondOctet != null && secondOctet in 16..31) {
                    return false
                }
            }
        }
        if (h.endsWith(".local")) {
            return false
        }
        return true
    }

    private fun connectWebSocket(config: com.example.data.PairingConfig) {
        if (TrackerStateManager.connectionState.value == ConnectionState.CONNECTED) {
            return // Already connected
        }
        
        TrackerStateManager.setConnectionState(ConnectionState.CONNECTING)
        
        val isDebug = com.example.BuildConfig.DEBUG
        val wsUrl: String
        val rawHost = config.serverHost.trim()
        val explicitWebsocketUrl = config.websocketUrl.trim()

        if (explicitWebsocketUrl.isNotEmpty()) {
            wsUrl = explicitWebsocketUrl
        } else {
            val scheme: String
            val host = rawHost
                .removePrefix("https://")
                .removePrefix("http://")
                .removePrefix("wss://")
                .removePrefix("ws://")

            val port = config.serverPort

            if (port == 443) {
                scheme = "wss://"
            } else if (isPublicHostname(host)) {
                scheme = "wss://"
            } else {
                if (isDebug) {
                    scheme = "ws://"
                } else {
                    scheme = "wss://"
                }
            }
            wsUrl = "$scheme$host:$port/ws/tracker"
        }
        
        serviceScope.launch {
            repository.addLog("CONNECTION", "Attempting connection to $wsUrl...", "INFO")
        }

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                reconnectDelayMs = 1000L
                TrackerStateManager.setConnectionState(ConnectionState.CONNECTED)
                serviceScope.launch {
                    repository.addLog("CONNECTION", "Web portal connection established successfully!", "SUCCESS")
                    TrackerStateManager.triggerLogsUpdate()
                    sendCurrentLocationImmediate()
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleIncomingMessage(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                TrackerStateManager.setConnectionState(ConnectionState.DISCONNECTED)
                
                val errorMessage = t.localizedMessage ?: t.toString()
                val isCleartextError = errorMessage.contains("cleartext", ignoreCase = true) || 
                                       (t.cause?.localizedMessage?.contains("cleartext", ignoreCase = true) == true)
                                       
                 val displayMessage = if (isCleartextError) {
                    "Server requires TLS. Please use HTTPS/WSS endpoint."
                } else {
                    "Connection failed: $errorMessage. Retrying soon..."
                }

                serviceScope.launch {
                    repository.addLog("CONNECTION", displayMessage, "ERROR")
                    TrackerStateManager.triggerLogsUpdate()
                }
                // Schedule reconnection
                val currentDelay = reconnectDelayMs
                reconnectDelayMs = (reconnectDelayMs * 2).coerceAtMost(30000L)
                locationHandler.postDelayed({
                    lastSavedConfig?.let { connectWebSocket(it) }
                }, currentDelay)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                TrackerStateManager.setConnectionState(ConnectionState.DISCONNECTED)
                serviceScope.launch {
                    repository.addLog("CONNECTION", "Connection closed by portal: $reason", "WARNING")
                    TrackerStateManager.triggerLogsUpdate()
                }
            }
        })
    }

    private fun disconnectWebSocket() {
        webSocket?.close(1000, "Service stopped")
        webSocket = null
        TrackerStateManager.setConnectionState(ConnectionState.DISCONNECTED)
    }

    private fun getBatteryLevel(): Int {
        return try {
            val intent = registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) (level * 100 / scale) else 100
        } catch (e: Exception) {
            100
        }
    }

    private fun handleIncomingMessage(jsonText: String) {
        val config = lastSavedConfig ?: return
        try {
            val root = JSONObject(jsonText)
            
            // Check security details
            val clientId = root.optString("clientId")
            val timestamp = root.optLong("timestamp")
            val signature = root.optString("signature")
            val payloadStr = root.optString("payload")
            
            if (clientId != config.clientId) {
                serviceScope.launch {
                    repository.addLog("SECURITY", "Rejected message: client ID mismatch ($clientId vs ${config.clientId})", "ERROR")
                }
                return
            }

            // Enforce a ±5 minute replay window on incoming command timestamp
            val currentTime = System.currentTimeMillis()
            val timeDifference = if (currentTime > timestamp) currentTime - timestamp else timestamp - currentTime
            if (timeDifference > 5 * 60 * 1000L) {
                serviceScope.launch {
                    repository.addLog("SECURITY", "Rejected message: timestamp replay check failed (diff: ${timeDifference / 1000}s)", "ERROR")
                }
                return
            }

            // Verify signature using the secret token
            val isValid = SecurityUtils.verifySignature(
                clientId = clientId,
                timestamp = timestamp,
                payload = payloadStr,
                signature = signature,
                secretToken = config.secretToken,
                originalJson = jsonText
            )

            if (!isValid) {
                serviceScope.launch {
                    repository.addLog("SECURITY", "Rejected payload: Invalid HMAC signature detected!", "ERROR")
                    TrackerStateManager.triggerLogsUpdate()
                }
                return
            }

            // Parse payload
            val payload = JSONObject(payloadStr)
            val command = payload.optString("command")
            
            serviceScope.launch {
                repository.addLog("COMMAND", "Received secure command: $command", "SUCCESS")
                TrackerStateManager.triggerLogsUpdate()
            }

            when (command) {
                "get_current_location" -> {
                    sendCurrentLocationImmediate()
                }
                "flash_flashlight_and_screen" -> {
                    serviceScope.launch {
                        TrackerStateManager.emitCommandEvent(TrackerEvent.FlashAlerts)
                        bringAppToForeground()
                    }
                }
                "display_message_on_screen" -> {
                    val message = payload.optString("message", "Emergency Alert!")
                    serviceScope.launch {
                        TrackerStateManager.emitCommandEvent(TrackerEvent.DisplayMessage(message))
                        bringAppToForeground()
                    }
                }
                "trigger_emergency_alarm" -> {
                    serviceScope.launch {
                        TrackerStateManager.emitCommandEvent(TrackerEvent.TriggerAlarm)
                        bringAppToForeground()
                    }
                }
                else -> {
                    serviceScope.launch {
                        repository.addLog("COMMAND", "Unsupported command: $command", "WARNING")
                    }
                }
            }

        } catch (e: Exception) {
            serviceScope.launch {
                repository.addLog("COMMAND", "Failed to parse command payload: ${e.localizedMessage}", "ERROR")
                TrackerStateManager.triggerLogsUpdate()
            }
        }
    }

    private fun bringAppToForeground() {
        try {
            val launchIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(launchIntent)
        } catch (e: Exception) {
            // Ignored in cases where system restricts launch from background
        }
    }

    // --- GPS Location Tracking ---

    private fun startTracking() {
        if (!TrackerStateManager.isServiceRunning.value) {
            serviceScope.launch {
                repository.addLog("LOCATION", "Bypassing tracking initiation: Service is not running in the foreground.", "WARNING")
            }
            return
        }
        stopTracking() // clear any existing

        val config = lastSavedConfig
        val movingIntervalMs = (config?.locationIntervalSec ?: 10) * 1000L
        val stationaryIntervalMs = (config?.stationaryIntervalSec ?: 300) * 1000L

        val intervalMs = if (isCurrentlyStationary) stationaryIntervalMs else movingIntervalMs
        val priority = if (isCurrentlyStationary) Priority.PRIORITY_BALANCED_POWER_ACCURACY else Priority.PRIORITY_HIGH_ACCURACY

        val locationRequest = LocationRequest.Builder(priority, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val lastLoc = locationResult.lastLocation ?: return
                handleNewLocation(lastLoc)
            }
        }

        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED && 
            androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            serviceScope.launch {
                repository.addLog("LOCATION", "Missing location permissions. Cannot start GPS tracking.", "ERROR")
            }
            return
        }

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        if (!androidx.core.location.LocationManagerCompat.isLocationEnabled(locationManager)) {
            serviceScope.launch {
                repository.addLog("LOCATION", "Device location is disabled. Cannot start GPS tracking.", "ERROR")
            }
            return
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            isLocationCallbackRegistered = true
            
            val modeText = if (isCurrentlyStationary) "Power-Saving (Low Battery)" else "Active (High Frequency)"
            serviceScope.launch {
                repository.addLog("LOCATION", "GPS Tracking initiated: $modeText", "INFO")
                TrackerStateManager.triggerLogsUpdate()
            }
        } catch (e: Exception) {
            serviceScope.launch {
                repository.addLog("LOCATION", "GPS Tracking initialization failed: ${e.localizedMessage}", "ERROR")
            }
        }
    }

    private fun stopTracking() {
        if (isLocationCallbackRegistered) {
            locationCallback?.let {
                fusedLocationClient.removeLocationUpdates(it)
            }
            isLocationCallbackRegistered = false
        }
        locationCallback = null
    }

    private fun handleNewLocation(location: Location) {
        TrackerStateManager.setLastLocation(location)
        
        // Broadcast location data via WebSocket
        val config = lastSavedConfig ?: return
        if (!config.isPaired || TrackerStateManager.connectionState.value != ConnectionState.CONNECTED) return

        val ws = webSocket ?: return
        try {
            val payload = JSONObject().apply {
                put("type", "location")
                put("latitude", location.latitude)
                put("longitude", location.longitude)
                put("accuracy", location.accuracy)
                put("speed", location.speed)
                put("altitude", location.altitude)
                put("battery", getBatteryLevel())
                put("status", if (isCurrentlyStationary) "stationary" else "active")
            }

            val secureMsg = SecurityUtils.securePayload(
                clientId = config.clientId,
                secretToken = config.secretToken,
                payloadJson = payload.toString()
            )
            
            ws.send(secureMsg)
            
            serviceScope.launch {
                repository.addLog("LOCATION", "Synced position: [${location.latitude}, ${location.longitude}] with server", "INFO")
                TrackerStateManager.triggerLogsUpdate()
            }
        } catch (e: Exception) {
            serviceScope.launch {
                repository.addLog("LOCATION", "Failed sending telemetry: ${e.localizedMessage}", "ERROR")
            }
        }
    }

    private fun sendCurrentLocationImmediate() {
        if (!TrackerStateManager.isServiceRunning.value) {
            serviceScope.launch {
                repository.addLog("LOCATION", "Bypassing immediate location request: Service is not running in the foreground.", "WARNING")
            }
            return
        }
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED && 
            androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            serviceScope.launch {
                repository.addLog("LOCATION", "Missing location permissions. Cannot fetch immediate location.", "ERROR")
            }
            return
        }
        
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        if (!androidx.core.location.LocationManagerCompat.isLocationEnabled(locationManager)) {
            serviceScope.launch {
                repository.addLog("LOCATION", "Device location is disabled. Cannot fetch immediate location.", "ERROR")
            }
            return
        }

        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        handleNewLocation(location)
                    } else {
                        serviceScope.launch {
                            repository.addLog("LOCATION", "Immediate location request yielded null coordinates", "WARNING")
                        }
                    }
                }
        } catch (e: Exception) {
            serviceScope.launch {
                repository.addLog("LOCATION", "Immediate location fetch failed: ${e.localizedMessage}", "ERROR")
            }
        }
    }

    // --- Accelerometer Motion Detection & Power Saving Mode ---

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val magnitude = sqrt(x*x + y*y + z*z)
        val gravity = SensorManager.GRAVITY_EARTH
        val delta = kotlin.math.abs(magnitude - gravity)

        // If delta from gravity is substantial (> 1.2 m/s^2), register motion
        if (delta > 1.2f) {
            lastMotionTime = System.currentTimeMillis()
            
            if (isCurrentlyStationary) {
                isCurrentlyStationary = false
                TrackerStateManager.setPowerSaving(false)
                TrackerStateManager.setTrackerStatus("Active")
                
                serviceScope.launch {
                    repository.addLog("SENSOR", "Motion detected! Resuming normal active high-frequency tracking", "SUCCESS")
                    TrackerStateManager.triggerLogsUpdate()
                }
                
                // Re-start tracking at high rate
                startTracking()
                sendCurrentLocationImmediate()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Unused
    }

    private fun checkStationaryState() {
        val elapsed = System.currentTimeMillis() - lastMotionTime
        if (elapsed > STATIONARY_THRESHOLD_MS && !isCurrentlyStationary) {
            isCurrentlyStationary = true
            TrackerStateManager.setPowerSaving(true)
            TrackerStateManager.setTrackerStatus("Stationary (Power-Saving)")
            
            serviceScope.launch {
                repository.addLog("SENSOR", "Device has been stationary for over 5 mins. Low-Battery Power-Saving Mode engaged automatically.", "WARNING")
                TrackerStateManager.triggerLogsUpdate()
                
                // Sync stationary state immediately with server
                syncStationaryStatus()
            }
            
            // Re-start tracking with slow intervals to conserve battery
            startTracking()
        }
    }

    private fun syncStationaryStatus() {
        val config = lastSavedConfig ?: return
        if (!config.isPaired || TrackerStateManager.connectionState.value != ConnectionState.CONNECTED) return

        val ws = webSocket ?: return
        try {
            val payload = JSONObject().apply {
                put("type", "status_change")
                put("status", "stationary")
                put("batterySaving", true)
                put("battery", getBatteryLevel())
            }

            val secureMsg = SecurityUtils.securePayload(
                clientId = config.clientId,
                secretToken = config.secretToken,
                payloadJson = payload.toString()
            )
            ws.send(secureMsg)
        } catch (e: Exception) {
            // Ignored
        }
    }

    // --- Emergency Actions (Alarm & Flashlight) ---

    private fun playAlarm() {
        stopAlarm() // Stop previous if playing
        try {
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) 
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                
            ringtone = RingtoneManager.getRingtone(applicationContext, alertUri)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ringtone?.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }
            
            // Set volume to max on Alarm stream to bypass silent state
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            audioManager.setStreamVolume(
                android.media.AudioManager.STREAM_ALARM,
                audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_ALARM),
                0
            )
            
            ringtone?.play()
            serviceScope.launch {
                repository.addLog("COMMAND", "Siren Alarm started blaring on maximum volume", "WARNING")
                TrackerStateManager.triggerLogsUpdate()
            }

            // Automatically silence after 5 minutes to prevent total battery drain
            alarmTimeoutJob = serviceScope.launch {
                kotlinx.coroutines.delay(300_000L) // 5 minutes
                if (TrackerStateManager.isAlarmRunning.value) {
                    repository.addLog("COMMAND", "Siren Alarm automatically silenced after 5-minute safety timeout.", "INFO")
                    TrackerStateManager.emitCommandEvent(com.example.state.TrackerEvent.StopAlerts)
                }
            }
        } catch (e: Exception) {
            serviceScope.launch {
                repository.addLog("COMMAND", "Failed to play emergency alarm ringtone: ${e.localizedMessage}", "ERROR")
            }
        }
    }

    private fun stopAlarm() {
        alarmTimeoutJob?.cancel()
        alarmTimeoutJob = null
        ringtone?.let {
            if (it.isPlaying) {
                it.stop()
                serviceScope.launch {
                    repository.addLog("COMMAND", "Siren Alarm silenced", "INFO")
                    TrackerStateManager.triggerLogsUpdate()
                }
            }
        }
        ringtone = null
    }

    private fun startFlashingFlashlight() {
        isFlashingActive = true
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
        
        serviceScope.launch(Dispatchers.IO) {
            try {
                val cameraIdList = cameraManager.cameraIdList
                if (cameraIdList.isEmpty()) return@launch
                
                val cameraId = cameraIdList[0]
                
                var flashOn = false
                while (isFlashingActive) {
                    flashOn = !flashOn
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            cameraManager.setTorchMode(cameraId, flashOn)
                        } catch (e: Exception) {
                            // Ignore torch state error
                        }
                    }
                    delay(250) // Flash cycle of 250ms
                }
                
                // Ensure torch is left OFF
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        cameraManager.setTorchMode(cameraId, false)
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            } catch (e: Exception) {
                serviceScope.launch {
                    repository.addLog("COMMAND", "Flashlight hardware is unavailable for strobe flash: ${e.localizedMessage}", "ERROR")
                    TrackerStateManager.triggerLogsUpdate()
                }
            }
        }
    }

    private fun stopFlashingFlashlight() {
        isFlashingActive = false
    }
}
