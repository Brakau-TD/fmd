package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.PowerOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.PairingConfig
import com.example.data.ServerTarget
import com.example.data.TrackerLog
import com.example.state.ConnectionState
import com.example.ui.PastePayloadDialog
import com.example.ui.ServerTargetDialog
import com.example.ui.ServerTargetSwitcherCard
import com.example.ui.theme.CyberBlack
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
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import org.json.JSONObject
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
                text = "Permissions Needed",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "To help locate your phone and pair with your tracking server via QR code, Find My Device needs Location and Camera access.",
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
                    text = "Allow Location & Camera",
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
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(20.dp),
                        tint = TrackerBlue
                    )
                    Text(
                        text = "Open System Settings",
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

    // Global toggle to reveal/hide all technical telemetry, raw logs, and test tools
    var showTechnicalDetails by rememberSaveable { mutableStateOf(false) }

    // Dialog states
    var showPastePayloadDialog by remember { mutableStateOf(false) }
    var showManualPairDialog by remember { mutableStateOf(false) }
    var showServerTargetDialog by remember { mutableStateOf(false) }
    var editingServerTarget by remember { mutableStateOf<ServerTarget?>(null) }

    // Background location check
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

    var isBatteryOptimized by remember {
        mutableStateOf(!BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context))
    }

    val batteryOptimizationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isBatteryOptimized = !BatteryOptimizationHelper.isIgnoringBatteryOptimizations(context)
    }

    // Active Command Alerts Observables
    val isAlarmRunning by viewModel.isAlarmRunning.collectAsState()
    val isFlashingRunning by viewModel.isFlashingRunning.collectAsState()
    val activeMessage by viewModel.activeMessage.collectAsState()

    val launchQrScanner: () -> Unit = {
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
            .addOnCanceledListener { }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Could not scan QR code: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    val friendlyTopSubtitle = when {
        !isRunning -> "Location sharing is paused"
        connectionState == ConnectionState.CONNECTED -> "Sharing active • Connected to target"
        connectionState == ConnectionState.CONNECTING -> "Connecting to target server..."
        else -> "Service running • Waiting for server"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Find My Device",
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp,
                            color = TextPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            !isRunning -> TextSecondary
                                            connectionState == ConnectionState.CONNECTED -> TrackerGreen
                                            connectionState == ConnectionState.CONNECTING -> TrackerBlue
                                            else -> TrackerRed
                                        }
                                    )
                            )
                            Text(
                                text = friendlyTopSubtitle,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isRunning && connectionState == ConnectionState.CONNECTED) TrackerGreen else TextSecondary
                            )
                        }
                    }
                },
                actions = {
                    // Quick 1-tap toggle in TopBar to show/hide technical data
                    FilterChip(
                        selected = showTechnicalDetails,
                        onClick = { showTechnicalDetails = !showTechnicalDetails },
                        label = {
                            Text(
                                text = if (showTechnicalDetails) "All Data: ON" else "All Data",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (showTechnicalDetails) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle All Data",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TrackerBlue.copy(alpha = 0.16f),
                            selectedLabelColor = TrackerBlue,
                            selectedLeadingIconColor = TrackerBlue
                        ),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("top_bar_technical_toggle")
                    )

                    IconButton(
                        onClick = { shareLogFile(context, logs) },
                        modifier = Modifier.testTag("export_logs_action_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Activity Logs",
                            tint = TrackerBlue
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberBlack,
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Friendly Status Overview Card (with expandable technical telemetry)
                StatusOverviewCard(
                    isServiceRunning = isRunning,
                    connectionState = connectionState,
                    trackerStatus = trackerStatus,
                    isPowerSaving = isPowerSaving,
                    lastLocation = lastLoc,
                    pairingConfig = pairing,
                    showTechnicalDetails = showTechnicalDetails,
                    onToggleService = {
                        if (isRunning) viewModel.stopService(context)
                        else viewModel.startService(context)
                    },
                    onClientPortalClick = {
                        editingServerTarget = null
                        showServerTargetDialog = true
                    }
                )

                // System Warnings (Friendly & Actionable)
                if (!backgroundLocationGranted && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    FriendlySystemNoticeCard(
                        title = "Allow Background Location",
                        description = "To keep tracking working when your phone is in your pocket or locked, set location access to 'Allow all the time'.",
                        buttonText = "Enable Background Location",
                        buttonTestTag = "grant_background_location_button",
                        onClick = {
                            bgLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                        }
                    )
                }

                if (isBatteryOptimized) {
                    FriendlySystemNoticeCard(
                        title = "Keep Tracking Reliable",
                        description = "Turn off battery restrictions for this app so Android doesn't pause location sharing while your phone is idle.",
                        buttonText = "Allow Unrestricted Battery",
                        buttonTestTag = "disable_battery_optimization_button",
                        onClick = {
                            try {
                                batteryOptimizationLauncher.launch(BatteryOptimizationHelper.getBatteryOptimizationIntent(context))
                            } catch (e: Exception) {
                                // Fallback if intent fails
                            }
                        }
                    )
                }

                // 2. Tracking Targets & Pairing Card (Always visible — switch, add, remove, or pair anytime!)
                ServerTargetSwitcherCard(
                    viewModel = viewModel,
                    showTechnicalDetails = showTechnicalDetails,
                    onOpenDialogForTarget = { target ->
                        editingServerTarget = target
                        showServerTargetDialog = true
                    },
                    onScanQrClick = launchQrScanner,
                    onPasteClick = { showPastePayloadDialog = true },
                    onManualPairClick = { showManualPairDialog = true }
                )

                // 3. Location Update Intervals Card (Always visible right on the main screen!)
                TrackingFrequencySettingsCard(viewModel = viewModel)

                // 4. Activity Summary & "Show All Data" Toggle Card
                ActivityAndDetailsToggleCard(
                    logs = logs,
                    showTechnicalDetails = showTechnicalDetails,
                    onToggleTechnicalDetails = { showTechnicalDetails = it }
                )

                // 5. Full Technical Data, Logs Console & Remote Simulator (Shown when toggle is ON)
                AnimatedVisibility(visible = showTechnicalDetails) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TerminalLogsConsole(
                            logs = logs,
                            onClearLogs = { viewModel.clearLogs() },
                            onDisconnect = if (pairing?.isPaired == true) {
                                { viewModel.disconnect(context) }
                            } else null,
                            onExportLogs = { shareLogFile(context, logs) }
                        )

                        ClientSimulatorDeck(viewModel = viewModel)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Dialogs
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

            if (showServerTargetDialog) {
                ServerTargetDialog(
                    viewModel = viewModel,
                    initialTarget = editingServerTarget,
                    onDismiss = { showServerTargetDialog = false }
                )
            }

            // Emergency Command Overlays
            EmergencyActionOverlays(
                isFlashing = isFlashingRunning,
                isAlarm = isAlarmRunning,
                alertMessage = activeMessage,
                onDismissAlert = { viewModel.stopSimulatedAlerts() }
            )
        }
    }
}

@Composable
private fun FriendlySystemNoticeCard(
    title: String,
    description: String,
    buttonText: String,
    buttonTestTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TrackerRed.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberGrayDeep),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Attention",
                    tint = TrackerRed,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = TrackerRed, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag(buttonTestTag)
            ) {
                Text(buttonText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- Status Overview Composable ---

@Composable
fun StatusOverviewCard(
    isServiceRunning: Boolean,
    connectionState: ConnectionState,
    trackerStatus: String,
    isPowerSaving: Boolean,
    lastLocation: android.location.Location?,
    pairingConfig: PairingConfig?,
    showTechnicalDetails: Boolean = false,
    onToggleService: () -> Unit,
    onClientPortalClick: () -> Unit = {}
) {
    val hasServer = pairingConfig != null && pairingConfig.serverHost.isNotEmpty()
    val serverLabel = when {
        !hasServer -> "No target chosen"
        pairingConfig!!.websocketUrl.isNotEmpty() -> pairingConfig.websocketUrl
        else -> "${pairingConfig.serverHost}:${pairingConfig.serverPort}"
    }

    val friendlyHeadline = when {
        !isServiceRunning -> "Tracking Paused"
        connectionState == ConnectionState.CONNECTED -> "Sharing Location"
        connectionState == ConnectionState.CONNECTING -> "Connecting..."
        hasServer -> "Waiting for Server"
        else -> "Ready to Set Up"
    }

    val friendlyExplanation = when {
        !isServiceRunning -> "Tap Start below to resume background location sharing."
        connectionState == ConnectionState.CONNECTED -> "Connected to $serverLabel"
        connectionState == ConnectionState.CONNECTING -> "Reaching $serverLabel..."
        hasServer -> "Cannot reach $serverLabel right now. Will retry automatically."
        else -> "Add or select a tracking target below to begin sharing."
    }

    val statusColor = when {
        !isServiceRunning -> TextSecondary
        connectionState == ConnectionState.CONNECTED -> TrackerGreen
        connectionState == ConnectionState.CONNECTING -> TrackerBlue
        else -> TrackerRed
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberGrayBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberGrayDeep),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Top Row: Friendly Status + Big Start/Pause Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.35f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(900, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulseAlpha"
                    )

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isServiceRunning && connectionState == ConnectionState.CONNECTED)
                                Icons.Default.CheckCircle
                            else
                                Icons.Default.Circle,
                            contentDescription = "Status",
                            tint = statusColor,
                            modifier = Modifier
                                .size(22.dp)
                                .alpha(if (connectionState == ConnectionState.CONNECTING) pulseAlpha else 1.0f)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = friendlyHeadline,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        Text(
                            text = friendlyExplanation,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Button(
                    onClick = onToggleService,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isServiceRunning) TrackerRed.copy(alpha = 0.15f) else TrackerGreen,
                        contentColor = if (isServiceRunning) TrackerRed else Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("toggle_service_button")
                ) {
                    Icon(
                        imageVector = if (isServiceRunning) Icons.Outlined.PowerOff else Icons.Default.Power,
                        contentDescription = "Toggle Tracking",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isServiceRunning) "Pause" else "Start",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Clean 2-Card Summary Grid: Current Target & Activity Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Active Target Summary Pill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CyberBlack)
                        .border(1.dp, CyberGrayBorder, RoundedCornerShape(14.dp))
                        .clickable { onClientPortalClick() }
                        .padding(12.dp)
                        .testTag("status_client_portal_row")
                ) {
                    DetailRow(
                        label = "Active Target",
                        value = serverLabel,
                        color = TextPrimary,
                        icon = Icons.Default.CellTower
                    )
                }

                // Activity & Battery Mode Pill
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CyberBlack)
                        .border(1.dp, CyberGrayBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    DetailRow(
                        label = "Tracking Mode",
                        value = if (isPowerSaving) "Battery Saver (Idle)" else if (isServiceRunning) "Active (Moving)" else "Paused",
                        color = if (isPowerSaving) TrackerBlue else if (isServiceRunning) TrackerGreen else TextSecondary,
                        icon = Icons.Default.MotionPhotosOn
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Friendly Last Location Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CyberBlack)
                    .border(1.dp, CyberGrayBorder, RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsFixed,
                            contentDescription = "GPS Status",
                            tint = if (lastLocation != null) TrackerGreen else TrackerBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Last Location Check",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                            if (lastLocation != null) {
                                val timeFormatted = remember(lastLocation.time) {
                                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastLocation.time))
                                }
                                Text(
                                    text = "Updated at $timeFormatted • Accurate to ~${lastLocation.accuracy.toInt()}m",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            } else {
                                Text(
                                    text = if (isServiceRunning) "Waiting for GPS signal..." else "Start tracking to get GPS position",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // Expandable Technical Telemetry Section (visible when "All Data" toggle is ON)
            AnimatedVisibility(visible = showTechnicalDetails) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CyberGrayLight.copy(alpha = 0.55f))
                        .border(1.dp, CyberGrayBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "TECHNICAL TELEMETRY & SECURITY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TrackerBlue,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Connection State: ${connectionState.name}  •  Raw Status: $trackerStatus",
                        fontSize = 11.sp,
                        color = TextPrimary,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Pairing: ${if (pairingConfig?.isPaired == true) "PAIRED (HMAC-SHA256)" else "UNPAIRED"}  •  Client ID: ${pairingConfig?.clientId?.ifEmpty { "None" } ?: "None"}",
                        fontSize = 11.sp,
                        color = if (pairingConfig?.isPaired == true) TextGreen else TextRed,
                        fontFamily = FontFamily.Monospace
                    )
                    if (lastLocation != null) {
                        Text(
                            text = String.format(
                                Locale.US,
                                "Coordinates: %.6f, %.6f (Acc: %.1fm, Speed: %.1fm/s)",
                                lastLocation.latitude,
                                lastLocation.longitude,
                                lastLocation.accuracy,
                                lastLocation.speed
                            ),
                            fontSize = 11.sp,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        Text(
                            text = "Coordinates: No GPS fix acquired yet",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, color: Color, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color.copy(alpha = 0.85f),
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// --- Location Reporting Frequency Customizer ---

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
        colors = CardDefaults.cardColors(containerColor = CyberGrayDeep),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(TrackerGreen.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Update Intervals",
                        tint = TrackerGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Update Intervals",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Choose how often your location is sent",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick 1-Tap Presets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val presets = listOf(
                    Triple("Fast", 5, 60),
                    Triple("Balanced", 15, 300),
                    Triple("Battery Saver", 60, 900)
                )
                presets.forEach { (label, activeSec, stationarySec) ->
                    val isSelected = activeValue.toInt() == activeSec && stationaryValue.toInt() == stationarySec
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            activeValue = activeSec.toFloat()
                            stationaryValue = stationarySec.toFloat()
                            viewModel.updateLocationIntervals(activeSec, stationarySec)
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TrackerGreen.copy(alpha = 0.18f),
                            selectedLabelColor = TrackerGreen
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Active / Moving Interval Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "When Moving",
                        fontSize = 14.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = formatFriendlyDuration(activeValue.toInt()),
                        fontSize = 13.sp,
                        color = TrackerGreen,
                        fontWeight = FontWeight.Bold
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
                    text = "Frequent updates while you are walking or driving.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stationary Interval Slider
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "When Stationary (Battery Saver)",
                        fontSize = 14.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = formatFriendlyDuration(stationaryValue.toInt()),
                        fontSize = 13.sp,
                        color = TrackerBlue,
                        fontWeight = FontWeight.Bold
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
                        activeTrackColor = TrackerBlue,
                        inactiveTrackColor = CyberGrayBorder,
                        thumbColor = TrackerBlue
                    ),
                    modifier = Modifier.testTag("stationary_interval_slider")
                )
                Text(
                    text = "Slower updates when your phone is resting still to save battery.",
                    fontSize = 11.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

private fun formatFriendlyDuration(seconds: Int): String {
    return when {
        seconds < 60 -> "Every ${seconds}s"
        seconds % 60 == 0 -> "Every ${seconds / 60} min"
        else -> "Every ${seconds / 60}m ${seconds % 60}s"
    }
}

// --- Activity Summary & Show All Technical Data Toggle Card ---

@Composable
fun ActivityAndDetailsToggleCard(
    logs: List<TrackerLog>,
    showTechnicalDetails: Boolean,
    onToggleTechnicalDetails: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberGrayBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberGrayDeep),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Recent Friendly Activity Summary (Up to 3 latest events in plain English)
            Text(
                text = "Recent Updates",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (logs.isEmpty()) {
                Text(
                    text = "No activity yet. Connect to a target above to start sharing your location.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            } else {
                val recentLogs = logs.take(3)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    recentLogs.forEach { log ->
                        FriendlyActivityRow(log = log)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = CyberGrayBorder)
            Spacer(modifier = Modifier.height(10.dp))

            // Prominent Toggle to view all technical data, logs & test tools
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onToggleTechnicalDetails(!showTechnicalDetails) }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Show All Technical Data",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "View raw GPS coordinates, security keys, full logs & test tools",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = showTechnicalDetails,
                    onCheckedChange = onToggleTechnicalDetails,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = TrackerBlue
                    ),
                    modifier = Modifier.testTag("toggle_technical_details_switch")
                )
            }
        }
    }
}

@Composable
private fun FriendlyActivityRow(log: TrackerLog) {
    val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeString = formatter.format(Date(log.timestamp))
    val friendlyMessage = remember(log.message, log.type) {
        humanizeLogMessage(log)
    }

    val dotColor = when (log.status) {
        "SUCCESS" -> TrackerGreen
        "WARNING" -> TrackerBlue
        "ERROR" -> TrackerRed
        else -> TextSecondary
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = friendlyMessage,
            fontSize = 12.sp,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = timeString,
            fontSize = 11.sp,
            color = TextSecondary
        )
    }
}

private fun humanizeLogMessage(log: TrackerLog): String {
    val msg = log.message
    return when {
        msg.contains("Successfully paired", ignoreCase = true) ->
            "Device paired and secured with server"
        msg.contains("Switched active target", ignoreCase = true) || msg.contains("Switched target server", ignoreCase = true) ->
            msg.substringBefore("[").trim()
        msg.contains("Tracking frequencies saved", ignoreCase = true) ->
            "Updated location reporting intervals"
        msg.contains("Synced position", ignoreCase = true) ->
            "Location shared with server"
        msg.contains("Entered low-battery", ignoreCase = true) ->
            "Phone is still — switched to Battery Saver mode"
        msg.contains("Motion detected", ignoreCase = true) ->
            "Movement detected — resumed active location updates"
        msg.contains("Initializing socket handshake", ignoreCase = true) ->
            "Connecting to tracking server..."
        msg.contains("Encrypted Tunnel Authenticated", ignoreCase = true) ->
            "Verified secure message from server"
        else -> msg
    }
}

// --- Legacy PairingPortalCard kept for compatibility ---

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
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CyberGrayBorder, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(containerColor = CyberGrayDeep),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Pair with Server",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Connect to your tracking web portal by entering the address or scanning a QR code.",
                fontSize = 12.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = inputHost,
                onValueChange = { viewModel.inputHost.value = it },
                label = { Text("Server Address / URL") },
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

            OutlinedTextField(
                value = inputPort,
                onValueChange = { viewModel.inputPort.value = it },
                label = { Text("Port") },
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

            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { viewModel.initializePairingConnection(inputHost, inputPort, context) },
                    colors = ButtonDefaults.buttonColors(containerColor = TrackerGreen, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("portal_handshake_button"),
                    enabled = inputHost.isNotEmpty() && !isConnecting
                ) {
                    if (isConnecting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                    } else {
                        Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Connect Server", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPasteClick,
                    colors = ButtonDefaults.buttonColors(containerColor = TrackerBlue, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                    ) {
                    Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Paste Code", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onScanClick,
                    colors = ButtonDefaults.buttonColors(containerColor = TrackerBlue, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
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
        colors = CardDefaults.cardColors(containerColor = CyberGrayDeep),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
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
                        contentDescription = "Test Remote Actions",
                        tint = TrackerBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Test Remote Actions (Simulator)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Preview how alerts, sirens, and motion work on this device",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
                Text(
                    text = if (isExpanded) "Hide" else "Open",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TrackerBlue
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.simulateIncomingCommand("get_current_location") },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGrayLight, contentColor = TextPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .border(1.dp, CyberGrayBorder, RoundedCornerShape(10.dp)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.GpsFixed, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Location", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.simulateIncomingCommand("flash_flashlight_and_screen") },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGrayLight, contentColor = TextPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .border(1.dp, CyberGrayBorder, RoundedCornerShape(10.dp)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Flash", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                                .height(42.dp)
                                .border(1.dp, TrackerRed.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Siren", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.simulateIncomingCommand("display_message_on_screen", mockMessageText) },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGrayLight, contentColor = TrackerGreen),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .border(1.dp, TrackerGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Test Message", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = mockMessageText,
                        onValueChange = { mockMessageText = it },
                        label = { Text("Emergency Screen Message") },
                        textStyle = TextStyle(fontSize = 12.sp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TrackerBlue,
                            unfocusedBorderColor = CyberGrayBorder,
                            focusedLabelColor = TrackerBlue
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.simulateSensorMotion() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGrayLight, contentColor = TrackerGreen),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .border(1.dp, CyberGrayBorder, RoundedCornerShape(10.dp)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Simulate Moving", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.simulateSensorStationary() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberGrayLight, contentColor = TrackerBlue),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .border(1.dp, CyberGrayBorder, RoundedCornerShape(10.dp)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Simulate Stationary", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                .height(280.dp)
        ) {
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
                        text = "TECHNICAL EVENT LOGS (${logs.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE3E2E6),
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Share",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF69F0AE),
                        modifier = Modifier
                            .clickable { onExportLogs() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )

                    Text(
                        text = "Clear",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8E9099),
                        modifier = Modifier
                            .clickable { onClearLogs() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )

                    if (onDisconnect != null) {
                        Text(
                            text = "Reset Pairing",
                            fontSize = 11.sp,
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
                            text = "No technical events recorded yet.",
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

fun shareLogFile(context: android.content.Context, logs: List<TrackerLog>) {
    try {
        val sb = StringBuilder()
        sb.append("==================================================\n")
        sb.append("  FIND MY DEVICE - BACKGROUND TRACKING LOGS\n")
        sb.append("  Generated on: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n")
        sb.append("  Device Model: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n")
        sb.append("  Android OS  : ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})\n")
        sb.append("==================================================\n\n")

        if (logs.isEmpty()) {
            sb.append("[NO LOGS RECORDED YET]\n")
        } else {
            logs.asReversed().forEach { log ->
                val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date(log.timestamp))
                sb.append("$dateStr [${log.type}] [${log.status}] ${log.message}\n")
            }
        }
        val fullLogsText = sb.toString()

        val cacheDir = context.cacheDir
        val file = java.io.File(cacheDir, "tracker_logs.txt")
        file.writeText(fullLogsText)

        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText("Tracker Logs", fullLogsText)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Logs copied to clipboard!", Toast.LENGTH_SHORT).show()

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
        Toast.makeText(context, "Failed to share log file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
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
        "CONNECTION" -> Color(0xFF40C4FF)
        "LOCATION" -> Color(0xFF69F0AE)
        "COMMAND" -> Color(0xFFE040FB)
        "SECURITY" -> Color(0xFFFF5252)
        "SENSOR" -> Color(0xFFFFAB40)
        else -> Color(0xFF8E9099)
    }

    val messageColor = when (log.status) {
        "SUCCESS" -> Color(0xFF69F0AE)
        "WARNING" -> Color(0xFFFFD600)
        "ERROR" -> Color(0xFFFF5252)
        else -> Color(0xFFE3E2E6)
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
    val currentHost by viewModel.inputHost.collectAsState()
    val currentPort by viewModel.inputPort.collectAsState()
    var hostInput by remember(currentHost) { mutableStateOf(currentHost) }
    var portInput by remember(currentPort) { mutableStateOf(currentPort) }
    var clientIdInput by remember { mutableStateOf("DEVICE-${(1000..9999).random()}") }
    var secretTokenInput by remember { mutableStateOf("sec_token_${(100000..999999).random()}") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberGrayBorder, RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = CyberGrayDeep),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Pair with Security Token",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Enter your server address and pairing credentials below.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = hostInput,
                        onValueChange = { hostInput = it },
                        label = { Text("Server Address") },
                        singleLine = true,
                        modifier = Modifier.weight(0.68f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TrackerGreen,
                            unfocusedBorderColor = CyberGrayBorder,
                            focusedLabelColor = TrackerGreen
                        )
                    )
                    OutlinedTextField(
                        value = portInput,
                        onValueChange = { portInput = it },
                        label = { Text("Port") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.32f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TrackerGreen,
                            unfocusedBorderColor = CyberGrayBorder,
                            focusedLabelColor = TrackerGreen
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = clientIdInput,
                    onValueChange = { clientIdInput = it },
                    label = { Text("Device ID") },
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
                    label = { Text("Pairing Token / Secret Key") },
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
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val parsed = TrackerViewModel.parseEndpoint(hostInput, portInput)
                            val qrSimPayload = JSONObject().apply {
                                put("clientId", clientIdInput)
                                put("token", secretTokenInput)
                                put("serverHost", parsed.host)
                                put("serverPort", parsed.port)
                                if (parsed.websocketUrl.isNotEmpty()) {
                                    put("websocketUrl", parsed.websocketUrl)
                                }
                            }
                            viewModel.processPairingQR(qrSimPayload.toString(), context)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TrackerGreen, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Pair & Connect", fontWeight = FontWeight.Bold)
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
        val infiniteTransition = rememberInfiniteTransition(label = "strobe")
        val strobeColor by infiniteTransition.animateColor(
            initialValue = TrackerRed,
            targetValue = Color(0xFF0D0E12),
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
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .border(2.dp, TrackerRed, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF111218)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
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
                            text = "EMERGENCY ALERT",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
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
                                text = "Siren is sounding at maximum volume\n(Automatically stops after 5 minutes)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(36.dp))

                        Button(
                            onClick = onDismissAlert,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF111218)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("dismiss_emergency_overlay_button")
                        ) {
                            Text(
                                text = "Dismiss Alert",
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
