package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LeakAdd
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.PowerOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import android.widget.Toast
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.TrackerLog
import com.example.state.ConnectionState
import com.example.ui.PastePayloadDialog
import com.example.ui.theme.CyberBlack
import org.json.JSONObject
import com.example.ui.theme.CyberGrayBorder
import com.example.ui.theme.CyberGrayDeep
import com.example.ui.theme.CyberGrayLight
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TextGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextRed
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TrackerBlue
import com.example.ui.theme.TrackerGreen
import com.example.ui.theme.TrackerRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Show on lock screen and turn screen on for emergency alerts
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainContainer()
            }
        }
    }
}

@Composable
fun MainContainer() {
    val context = LocalContext.current
    val viewModel: TrackerViewModel = viewModel()
    
    // Permission states
    var permissionsGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }


    val launcher = rememberLauncherForActivityResult(

        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val fineLocation = results[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val camera = results[Manifest.permission.CAMERA] ?: false
        permissionsGranted = fineLocation && camera
        if (permissionsGranted) {
            viewModel.startService(context)
        }
    }

    LaunchedEffect(Unit) {
        if (!permissionsGranted) {
            val permissionsToRequest = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA
            )
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
            launcher.launch(permissionsToRequest.toTypedArray())
        } else {
            // Auto start background tracking service once permissions are valid
            viewModel.startService(context)
        }
    }

    if (!permissionsGranted) {
        PermissionRequiredScreen(
            onRequestPermissions = {
                val permissionsToRequest = mutableListOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.CAMERA
                )
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                launcher.launch(permissionsToRequest.toTypedArray())
            },
            onOpenSettings = {
                val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }
        )
    } else {
        TrackerDashboard(viewModel = viewModel)
    }
}

@Composable
fun PermissionRequiredScreen(
    onRequestPermissions: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = CyberBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Shield Security",
                tint = TrackerGreen,
                modifier = Modifier
                    .size(80.dp)
                    .padding(bottom = 16.dp)
            )

            Text(
                text = "Permissions Required",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "To track your phone securely, 'Find My Device' requires active GPS Location tracking and Camera access to pair with the client web portal using a secure QR code.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onRequestPermissions,
                colors = ButtonDefaults.buttonColors(containerColor = TrackerGreen, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("request_permissions_button")
            ) {
                Text(
                    text = "Grant Required Permissions",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(containerColor = CyberGrayDeep, contentColor = TextPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .border(1.dp, CyberGrayBorder, RoundedCornerShape(12.dp))
                    .testTag("open_settings_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SettingsInputComponent,
                        contentDescription = "Settings",
                        modifier = Modifier.padding(end = 8.dp).size(20.dp),
                        tint = TrackerBlue
                    )
                    Text(
                        text = "Open App System Settings",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerDashboard(viewModel: TrackerViewModel) {
    val context = LocalContext.current
    
    val pairing by viewModel.pairingConfig.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val isRunning by viewModel.isServiceRunning.collectAsState()
    val trackerStatus by viewModel.trackerStatus.collectAsState()
    val lastLoc by viewModel.lastLocation.collectAsState()
    val isPowerSaving by viewModel.isPowerSaving.collectAsState()
    val logs by viewModel.logs.collectAsState()
    
    // UI temporary flags
    var showPastePayloadDialog by remember { mutableStateOf(false) }
    var showManualPairDialog by remember { mutableStateOf(false) }

    // Background location state
    var backgroundLocationGranted by remember {
        mutableStateOf(
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val bgLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        backgroundLocationGranted = granted
    }

    // Active Command Alerts Observables
    val isAlarmRunning by viewModel.isAlarmRunning.collectAsState()
    val isFlashingRunning by viewModel.isFlashingRunning.collectAsState()
    val activeMessage by viewModel.activeMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Find My Device",
                            fontWeight = FontWeight.Normal,
                            fontSize = 22.sp,
                            color = TextPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isRunning) TrackerGreen else TextSecondary)
                            )
                            Text(
                                text = if (isRunning) "Connected • 12ms Latency" else "Disconnected",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isRunning) TrackerGreen else TextSecondary,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { shareLogFile(context, logs) },
                        modifier = Modifier.testTag("export_logs_action_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export Logs",
                            tint = TrackerGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberBlack, // Transparent blends with body background
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = CyberBlack
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val pagerState = rememberPagerState(pageCount = { 2 })

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp),
                pageSpacing = 16.dp
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 20.dp)
                ) {
                    if (page == 0) {
                        Column(
                            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                        ) {
                            // 1. Connection Radar & Status Overview
                            StatusOverviewCard(
                                isServiceRunning = isRunning,
                                connectionState = connectionState,
                                trackerStatus = trackerStatus,
                                isPowerSaving = isPowerSaving,
                                lastLocation = lastLoc,
                                pairingConfig = pairing,
                                onToggleService = {
                                    if (isRunning) viewModel.stopService(context) 
                                    else viewModel.startService(context)
                                }
                            )
            
                            if (!backgroundLocationGranted && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, TrackerRed, RoundedCornerShape(16.dp)),
                                    colors = CardDefaults.cardColors(containerColor = CyberGrayDeep),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = "Warning",
                                                tint = TrackerRed,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "BACKGROUND ACCESS REQUIRED",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TrackerRed,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "To allow the tracker to run automatically on boot, update, and when locked, please change location access to 'Allow all the time' in the system settings.",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = {
                                                bgLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = TrackerRed, contentColor = Color.White),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(40.dp)
                                                .testTag("grant_background_location_button")
                                        ) {
                                            Text("Grant 'Allow all the time' Access", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
            
                            if (pairing == null || !pairing!!.isPaired) {
                                Spacer(modifier = Modifier.height(16.dp))
                                PairingPortalCard(
                                    viewModel = viewModel,
                                    onPasteClick = { showPastePayloadDialog = true },
                                    onScanClick = {
                                        val options = GmsBarcodeScannerOptions.Builder()
                                            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                                            .enableAutoZoom()
                                            .build()
                                        val scanner = GmsBarcodeScanning.getClient(context, options)
                                        scanner.startScan()
                                            .addOnSuccessListener { barcode ->
                                                val qrText = barcode.rawValue
                                                if (qrText != null) {
                                                    viewModel.processPairingQR(qrText, context)
                                                }
                                            }
                                            .addOnCanceledListener {
                                                // Task canceled
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(context, "Scan failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                    },
                                    onManualClick = { showManualPairDialog = true }
                                )
                            }
                        }
                    } else {
                        // Page 1: Settings, Logs, Command Console
                        Column(
                            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                        ) {
                            if (pairing == null || !pairing!!.isPaired) {
                                TerminalLogsConsole(
                                    logs = logs,
                                    onClearLogs = { viewModel.clearLogs() },
                                    onDisconnect = null,
                                    onExportLogs = {
                                        shareLogFile(context, logs)
                                    }
                                )
                            } else {
                                // Location Reporting Frequency Customizer
                                TrackingFrequencySettingsCard(viewModel = viewModel)
        
                                Spacer(modifier = Modifier.height(16.dp))
        
                                // Terminal Logs console
                                TerminalLogsConsole(
                                    logs = logs,
                                    onClearLogs = { viewModel.clearLogs() },
                                    onDisconnect = { viewModel.disconnect(context) },
                                    onExportLogs = {
                                        shareLogFile(context, logs)
                                    }
                                )
        
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Collapsible Simulator Control Deck (Portal Command Console at the bottom)
                                ClientSimulatorDeck(viewModel = viewModel)
                            }
                        }
                    }
                }
            }

            // Manual Pairing Setup Dialogue (Highly useful in Emulators)
            if (showManualPairDialog) {
                ManualPairingDialog(
                    viewModel = viewModel,
                    onDismiss = { showManualPairDialog = false }
                )
            }

            if (showPastePayloadDialog) {
                PastePayloadDialog(
                    viewModel = viewModel,
                    onDismiss = { showPastePayloadDialog = false }
                )
            }

            // EMERGENCY COMMAND ACTION OVERLAYS
            EmergencyActionOverlays(
                isFlashing = isFlashingRunning,
                isAlarm = isAlarmRunning,
                alertMessage = activeMessage,
                onDismissAlert = { viewModel.stopSimulatedAlerts() }
            )
        }
    }
}

// --- Status Overview Composables ---

@Composable
fun StatusOverviewCard(
    isServiceRunning: Boolean,
    connectionState: ConnectionState,
    trackerStatus: String,
    isPowerSaving: Boolean,
    lastLocation: android.location.Location?,
    pairingConfig: com.example.data.PairingConfig?,
    onToggleService: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberGrayBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberGrayDeep),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CORE TELEMETRY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TrackerBlue,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = if (isServiceRunning) "Tracker Active" else "Tracker Inactive",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isServiceRunning) TextPrimary else TextSecondary
                    )
                }

                // Active Switch button
                Button(
                    onClick = onToggleService,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isServiceRunning) TrackerRed.copy(alpha = 0.2f) else TrackerGreen.copy(alpha = 0.2f),
                        contentColor = if (isServiceRunning) TrackerRed else TrackerGreen
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("toggle_service_button")
                ) {
                    Icon(
                        imageVector = if (isServiceRunning) Icons.Outlined.PowerOff else Icons.Default.Power,
                        contentDescription = "Power",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isServiceRunning) "Stop" else "Start",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Pulse radar style row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CyberGrayLight, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Connection Pulse Dot
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )

                Icon(
                    imageVector = Icons.Default.Circle,
                    contentDescription = "Pulse",
                    tint = when (connectionState) {
                        ConnectionState.CONNECTED -> TrackerGreen
                        ConnectionState.CONNECTING -> TrackerBlue
                        ConnectionState.DISCONNECTED -> TrackerRed
                    },
                    modifier = Modifier
                        .size(12.dp)
                        .alpha(if (connectionState == ConnectionState.CONNECTING) alpha else 1.0f)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "CONNECTION STATUS",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = when (connectionState) {
                            ConnectionState.CONNECTED -> "CONNECTED SECURELY"
                            ConnectionState.CONNECTING -> "ESTABLISHING HANDSHAKE..."
                            ConnectionState.DISCONNECTED -> "DISCONNECTED (OFFLINE)"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (connectionState) {
                            ConnectionState.CONNECTED -> TrackerGreen
                            ConnectionState.CONNECTING -> TrackerBlue
                            ConnectionState.DISCONNECTED -> TrackerRed
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Multi-Columns Details Grid
            Row(modifier = Modifier.fillMaxWidth()) {
                // Left Col
                Column(modifier = Modifier.weight(1f)) {
                    DetailRow(
                        label = "PAIRING STATE",
                        value = if (pairingConfig?.isPaired == true) "PAIRED (SECURED)" else "UNPAIRED",
                        color = if (pairingConfig?.isPaired == true) TextGreen else TextRed,
                        icon = Icons.Default.Security
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow(
                        label = "BEHAVIOR MODE",
                        value = trackerStatus,
                        color = if (isPowerSaving) TrackerBlue else TrackerGreen,
                        icon = Icons.Default.MotionPhotosOn
                    )
                }

                // Right Col
                Column(modifier = Modifier.weight(1f)) {
                    DetailRow(
                        label = "POWER CONSERVATION",
                        value = if (isPowerSaving) "LOW-BATTERY ENGAGED" else "NORMAL DENSITY",
                        color = if (isPowerSaving) TrackerBlue else TextSecondary,
                        icon = Icons.Default.SettingsInputComponent
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    DetailRow(
                        label = "CLIENT PORTAL",
                        value = if (pairingConfig != null && pairingConfig.serverHost.isNotEmpty()) 
                            "${pairingConfig.serverHost}:${pairingConfig.serverPort}" else "Not Set",
                        color = TextPrimary,
                        icon = Icons.Default.CellTower
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Last Position details
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberGrayBorder, RoundedCornerShape(12.dp))
                    .background(CyberBlack)
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GpsFixed,
                        contentDescription = "GPS",
                        tint = TrackerBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "LAST SYNCED GPS POSITION",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                        if (lastLocation != null) {
                            Text(
                                text = "LAT: ${String.format("%.6f", lastLocation.latitude)} | LON: ${String.format("%.6f", lastLocation.longitude)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Accuracy: ${String.format("%.1f", lastLocation.accuracy)}m | Speed: ${String.format("%.1f", lastLocation.speed)}m/s",
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        } else {
                            Text(
                                text = "Awaiting satellite coordinates signal...",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color.copy(alpha = 0.8f),
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(
                text = label,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

// --- Pairing Card Composables ---

@Composable
fun PairingPortalCard(
    modifier: Modifier = Modifier,
    viewModel: TrackerViewModel,
    onPasteClick: () -> Unit,
    onScanClick: () -> Unit,
    onManualClick: () -> Unit
) {
    val context = LocalContext.current
    val inputHost by viewModel.inputHost.collectAsState()
    val inputPort by viewModel.inputPort.collectAsState()
    val isConnecting by viewModel.isConnectingForPairing.collectAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberGrayBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberGrayDeep),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "PORTAL PAIRING HUB",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TrackerGreen,
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = "Secure Your Device",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Connect to your tracker web portal. Enter your host address and port to initiate handshake, then scan the displayed pairing QR code to establish secure encrypted sync.",
                fontSize = 11.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Host Entry
            OutlinedTextField(
                value = inputHost,
                onValueChange = { viewModel.inputHost.value = it },
                label = { Text("Web Portal Host / Website") },
                placeholder = { Text("e.g. 192.168.1.5 or mydeviceportal.com") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("portal_host_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TrackerGreen,
                    unfocusedBorderColor = CyberGrayBorder,
                    focusedLabelColor = TrackerGreen,
                    cursorColor = TrackerGreen
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Port Entry
            OutlinedTextField(
                value = inputPort,
                onValueChange = { viewModel.inputPort.value = it },
                label = { Text("Port") },
                placeholder = { Text("e.g. 8080") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("portal_port_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TrackerGreen,
                    unfocusedBorderColor = CyberGrayBorder,
                    focusedLabelColor = TrackerGreen,
                    cursorColor = TrackerGreen
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Row
            Row(modifier = Modifier.fillMaxWidth()) {
                // Handshake Connect
                Button(
                    onClick = { viewModel.initializePairingConnection(inputHost, inputPort, context) },
                    colors = ButtonDefaults.buttonColors(containerColor = TrackerGreen, contentColor = CyberBlack),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("portal_handshake_button"),
                    enabled = inputHost.isNotEmpty() && !isConnecting
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CyberBlack)
                    } else {
                        Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Connect Port", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scan QR / Manual pairing buttons (Enabled only after handshake configuration)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPasteClick,
                    colors = ButtonDefaults.buttonColors(containerColor = TrackerBlue, contentColor = CyberBlack),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("paste_pairing_payload_button")
                ) {
                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Paste JSON", fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = onScanClick,
                    colors = ButtonDefaults.buttonColors(containerColor = TrackerBlue, contentColor = CyberBlack),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("scan_pairing_qr_button")
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan QR", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onManualClick,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberGrayLight, contentColor = TextPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .border(1.dp, CyberGrayBorder, RoundedCornerShape(12.dp))
                        .testTag("manual_pairing_button")
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Manual", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// --- Client Simulator Dashboard (Developer Testing Portal) ---

@Composable
fun ClientSimulatorDeck(viewModel: TrackerViewModel) {
    var isExpanded by remember { mutableStateOf(false) }
    var mockMessageText by remember { mutableStateOf("EMERGENCY ALERT: This smartphone has been lost. If found, please return immediately!") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberGrayBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberGrayDeep.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Radio,
                        contentDescription = "Sim",
                        tint = TrackerBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "PORTAL COMMAND CONSOLE (SIMULATOR)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TrackerBlue,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Simulate remote operations in-app",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
                Text(
                    text = if (isExpanded) "CLOSE [-]" else "OPEN COMMANDS [+]",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TrackerBlue,
                    fontFamily = FontFamily.Monospace
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                ) {
                    Text(
                        text = "Because local network environments restrict connecting to hostports easily in browser-based emulators, use these actions to directly dispatch commands into the encrypted parsing pipelines:",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Commands Grid Rows
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.simulateIncomingCommand("get_current_location") },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGrayLight, contentColor = TextPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .border(1.dp, CyberGrayBorder, RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Sync Loc", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.simulateIncomingCommand("flash_flashlight_and_screen") },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGrayLight, contentColor = TextPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .border(1.dp, CyberGrayBorder, RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Flash Alert", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.simulateIncomingCommand("trigger_emergency_alarm") },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGrayLight, contentColor = TrackerRed),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .border(1.dp, TrackerRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Blare Siren", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.simulateIncomingCommand("display_message_on_screen", mockMessageText) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGrayLight, contentColor = TrackerGreen),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .border(1.dp, TrackerGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Lost Msg", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = mockMessageText,
                        onValueChange = { mockMessageText = it },
                        label = { Text("Display Message Text") },
                        textStyle = TextStyle(fontSize = 12.sp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TrackerBlue,
                            unfocusedBorderColor = CyberGrayBorder,
                            focusedLabelColor = TrackerBlue
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "SIMULATE MOTION SENSORS (ACCELEROMETER)",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.simulateSensorMotion() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGrayLight, contentColor = TrackerGreen),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .border(1.dp, CyberGrayBorder, RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Trigger Motion (Resume normal GPS)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.simulateSensorStationary() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGrayLight, contentColor = TrackerBlue),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .border(1.dp, CyberGrayBorder, RoundedCornerShape(8.dp)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Trigger Idle (Stationary Battery-Save)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// --- Terminal Logs Console Composable ---

@Composable
fun TerminalLogsConsole(
    logs: List<TrackerLog>,
    onClearLogs: () -> Unit,
    onDisconnect: (() -> Unit)?,
    onExportLogs: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF2E3036), RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B1B1F)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .height(300.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Logs",
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SECURE LOG CONSOLE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE3E2E6),
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Export Logs",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF69F0AE),
                        modifier = Modifier
                            .clickable { onExportLogs() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )

                    Text(
                        text = "Clear",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8E9099),
                        modifier = Modifier
                            .clickable { onClearLogs() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                    
                    if (onDisconnect != null) {
                        Text(
                            text = "Disconnect",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFA8A80),
                            modifier = Modifier
                                .clickable { onDisconnect() }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Logs Shell box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF0C0E12), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF2E3036), RoundedCornerShape(16.dp))
                    .padding(10.dp)
            ) {
                if (logs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$ _ Awaiting telemetry handshakes...",
                            color = Color(0xFF8E9099).copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        reverseLayout = false
                    ) {
                        items(logs) { log ->
                            LogTerminalRow(log = log)
                        }
                    }
                }
            }
        }
    }
}

fun shareLogFile(context: android.content.Context, logs: List<com.example.data.TrackerLog>) {
    try {
        val sb = StringBuilder()
        sb.append("==================================================\n")
        sb.append("  FIND MY DEVICE - BACKGROUND TRACKING LOGS\n")
        sb.append("  Generated on: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}\n")
        sb.append("  Device Model: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n")
        sb.append("  Android OS  : ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})\n")
        sb.append("==================================================\n\n")
        
        if (logs.isEmpty()) {
            sb.append("[NO LOGS RECORDED YET]\n")
        } else {
            logs.asReversed().forEach { log ->
                val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date(log.timestamp))
                sb.append("$dateStr [${log.type}] [${log.status}] ${log.message}\n")
            }
        }
        val fullLogsText = sb.toString()

        // 1. Write to cache file
        val cacheDir = context.cacheDir
        val file = java.io.File(cacheDir, "tracker_logs.txt")
        file.writeText(fullLogsText)

        // 2. Automatically copy to clipboard as backup
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Tracker Logs", fullLogsText)
        clipboard.setPrimaryClip(clip)
        android.widget.Toast.makeText(context, "Logs copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()

        // 3. Share with BOTH file stream and full text details
        val authority = "${context.packageName}.fileprovider"
        val fileUri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)

        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Find My Device - Tracker Logs")
            putExtra(android.content.Intent.EXTRA_TEXT, fullLogsText)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Tracker Logs"))
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Failed to share log file: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
    }
}

@Composable
fun LogTerminalRow(log: TrackerLog) {
    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val timeString = formatter.format(Date(log.timestamp))

    val prefix = when (log.type) {
        "CONNECTION" -> "[CONN]"
        "LOCATION" -> "[GPS ]"
        "COMMAND" -> "[CMD ]"
        "SECURITY" -> "[AUTH]"
        "SENSOR" -> "[SENS]"
        else -> "[INFO]"
    }

    val tagColor = when (log.type) {
        "CONNECTION" -> Color(0xFF40C4FF) // Bright cyan
        "LOCATION" -> Color(0xFF69F0AE)  // Bright mint green
        "COMMAND" -> Color(0xFFE040FB)   // Magenta
        "SECURITY" -> Color(0xFFFF5252)  // Bright light red
        "SENSOR" -> Color(0xFFFFAB40)    // Orange
        else -> Color(0xFF8E9099)
    }

    val messageColor = when (log.status) {
        "SUCCESS" -> Color(0xFF69F0AE) // Bright mint green
        "WARNING" -> Color(0xFFFFD600) // Bright yellow
        "ERROR" -> Color(0xFFFF5252)   // Bright light red
        else -> Color(0xFFE3E2E6)      // Soft white
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$timeString ",
            fontSize = 10.sp,
            color = Color(0xFF8E9099).copy(alpha = 0.8f),
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "$prefix ",
            fontSize = 10.sp,
            color = tagColor,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = log.message,
            fontSize = 10.sp,
            color = messageColor,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
    }
}

// --- Dialogs & Overlays ---

@Composable
fun ManualPairingDialog(
    viewModel: TrackerViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var clientIdInput by remember { mutableStateOf("DEVICE-${(1000..9999).random()}") }
    var secretTokenInput by remember { mutableStateOf("sec_token_${(100000..999999).random()}") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberGrayBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberGrayDeep),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Text(
                    text = "MANUAL SECURE PAIRING",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TrackerBlue,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Setup secure credentials",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = clientIdInput,
                    onValueChange = { clientIdInput = it },
                    label = { Text("Client ID / Device Identifier") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TrackerBlue,
                        unfocusedBorderColor = CyberGrayBorder,
                        focusedLabelColor = TrackerBlue
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = secretTokenInput,
                    onValueChange = { secretTokenInput = it },
                    label = { Text("Encrypted Secret Token") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TrackerBlue,
                        unfocusedBorderColor = CyberGrayBorder,
                        focusedLabelColor = TrackerBlue
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(12.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val qrSimPayload = JSONObject().apply {
                                put("clientId", clientIdInput)
                                put("token", secretTokenInput)
                            }
                            viewModel.processPairingQR(qrSimPayload.toString(), context)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TrackerBlue, contentColor = CyberBlack),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Confirm Pair", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EmergencyActionOverlays(
    isFlashing: Boolean,
    isAlarm: Boolean,
    alertMessage: String?,
    onDismissAlert: () -> Unit
) {
    val activity = LocalContext.current as? android.app.Activity
    LaunchedEffect(isFlashing, isAlarm, alertMessage) {
        val isEmergency = isFlashing || isAlarm || alertMessage != null
        if (isEmergency) {
            activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    if (isFlashing || alertMessage != null || isAlarm) {
        // High visibility background strobe/pulsing animation
        val infiniteTransition = rememberInfiniteTransition(label = "strobe")
        val strobeColor by infiniteTransition.animateColor(
            initialValue = TrackerRed,
            targetValue = Color(0xFF0D0E12), // Strobe between red and very dark slate to keep high urgency but avoid blinding
            animationSpec = infiniteRepeatable(
                animation = tween(400, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "strobeColor"
        )

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = if (isFlashing) strobeColor else TrackerRed
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Sleek, high-contrast container so elements are perfectly readable and never "white-on-white"
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .border(2.dp, TrackerRed, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF111218) // Sleek rich dark slate card container
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Alarm siren ringing animation
                        val scaleTransition = rememberInfiniteTransition(label = "ringScale")
                        val scale by scaleTransition.animateFloat(
                            initialValue = 0.9f,
                            targetValue = 1.2f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(350, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scale"
                        )

                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Alert siren ringing",
                            tint = TrackerRed,
                            modifier = Modifier
                                .size(80.dp)
                                .scale(scale)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "EMERGENCY TRACKING ACTIVE",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TrackerRed,
                            textAlign = TextAlign.Center
                        )

                        if (alertMessage != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = alertMessage,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                lineHeight = 30.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                        } else {
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        if (isAlarm) {
                            Text(
                                text = "SIREN BLARING • MAX STREAM VOLUME\n(Auto-silences in 5 minutes to protect battery)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        } else {
                            Text(
                                text = "STATIONARY SECURITY MODE ENABLED",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(36.dp))

                        Button(
                            onClick = onDismissAlert,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White, // Pure high-contrast white button background
                                contentColor = Color(0xFF111218) // Deep dark text for extreme readability
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("dismiss_emergency_overlay_button")
                        ) {
                            Text(
                                text = "Dismiss Alarm & Message",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackingFrequencySettingsCard(viewModel: TrackerViewModel) {
    val pairingConfig by viewModel.pairingConfig.collectAsState()
    
    val currentActive = pairingConfig?.locationIntervalSec ?: 10
    val currentStationary = pairingConfig?.stationaryIntervalSec ?: 300

    var activeValue by remember(currentActive) { mutableStateOf(currentActive.toFloat()) }
    var stationaryValue by remember(currentStationary) { mutableStateOf(currentStationary.toFloat()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberGrayBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberGrayDeep.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TrackerGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "LOCATION REPORTING FREQUENCY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TrackerGreen,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Customize update intervals",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Active / Moving Interval Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Active Tracking (Moving)",
                        fontSize = 12.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${activeValue.toInt()}s",
                        fontSize = 12.sp,
                        color = TrackerGreen,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = activeValue,
                    onValueChange = { activeValue = it },
                    onValueChangeFinished = {
                        viewModel.updateLocationIntervals(activeValue.toInt(), stationaryValue.toInt())
                    },
                    valueRange = 5f..120f,
                    steps = 22,
                    colors = SliderDefaults.colors(
                        activeTrackColor = TrackerGreen,
                        inactiveTrackColor = CyberGrayBorder,
                        thumbColor = TrackerGreen
                    ),
                    modifier = Modifier.testTag("active_interval_slider")
                )
                Text(
                    text = "Controls update frequency when device is actively moving.",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stationary Interval Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Stationary (Power-Saving)",
                        fontSize = 12.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (stationaryValue >= 60) "${(stationaryValue / 60).toInt()}m" else "${stationaryValue.toInt()}s",
                        fontSize = 12.sp,
                        color = TrackerGreen,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Slider(
                    value = stationaryValue,
                    onValueChange = { stationaryValue = it },
                    onValueChangeFinished = {
                        viewModel.updateLocationIntervals(activeValue.toInt(), stationaryValue.toInt())
                    },
                    valueRange = 10f..1800f,
                    steps = 59,
                    colors = SliderDefaults.colors(
                        activeTrackColor = TrackerGreen,
                        inactiveTrackColor = CyberGrayBorder,
                        thumbColor = TrackerGreen
                    ),
                    modifier = Modifier.testTag("stationary_interval_slider")
                )
                Text(
                    text = "Controls update frequency when device is stationary to conserve battery.",
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }
        }
    }
}
