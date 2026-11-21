package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.TrackerViewModel
import com.example.data.ServerTarget
import com.example.state.ConnectionState
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberGrayBorder
import com.example.ui.theme.CyberGrayDeep
import com.example.ui.theme.CyberGrayLight
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TrackerBlue
import com.example.ui.theme.TrackerGreen
import com.example.ui.theme.TrackerRed

@Composable
fun ServerTargetSwitcherCard(
    viewModel: TrackerViewModel,
    showTechnicalDetails: Boolean = false,
    onOpenDialogForTarget: (ServerTarget?) -> Unit,
    onScanQrClick: () -> Unit = {},
    onPasteClick: () -> Unit = {},
    onManualPairClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val pairingConfig by viewModel.pairingConfig.collectAsState()
    val serverTargets by viewModel.serverTargets.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    var showPairingOptions by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberGrayBorder, RoundedCornerShape(24.dp))
            .testTag("server_target_switcher_card"),
        colors = CardDefaults.cardColors(containerColor = CyberGrayDeep),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Friendly Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TrackerBlue.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = "Tracking Targets",
                            tint = TrackerBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.target_server_title),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Switch active target or pair a new server",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Action Bar: Always visible Pair QR & Add Target buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onOpenDialogForTarget(null) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TrackerBlue,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("add_server_target_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Target Server",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.target_server_add_button),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onScanQrClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TrackerGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("scan_pairing_qr_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Pair via QR Code",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pair via QR",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Secondary Pairing Options (Always reachable via toggle or when technical details are enabled)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showPairingOptions = !showPairingOptions }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showPairingOptions || showTechnicalDetails)
                        "Other pairing methods (Paste code or enter token)"
                    else
                        "Have a setup code or token? Tap for more pairing options",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TrackerBlue
                )
                Icon(
                    imageVector = if (showPairingOptions || showTechnicalDetails)
                        Icons.Default.KeyboardArrowUp
                    else
                        Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle pairing methods",
                    tint = TrackerBlue,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = showPairingOptions || showTechnicalDetails) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onPasteClick,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("paste_pairing_payload_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = TrackerBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Paste Code",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    OutlinedButton(
                        onClick = onManualPairClick,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("manual_pairing_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = TrackerBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Manual Token",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Saved Targets List Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Targets (${serverTargets.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                if (serverTargets.size > 1) {
                    Text(
                        text = "Tap any target to switch",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (serverTargets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberGrayBorder, RoundedCornerShape(16.dp))
                        .background(CyberBlack, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No tracking targets yet",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.target_server_empty_hint),
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    serverTargets.forEach { target ->
                        val isActiveTarget = pairingConfig != null &&
                            pairingConfig!!.serverHost.isNotEmpty() &&
                            pairingConfig!!.serverHost == target.serverHost &&
                            pairingConfig!!.serverPort == target.serverPort &&
                            pairingConfig!!.websocketUrl == target.websocketUrl

                        ServerTargetItemRow(
                            target = target,
                            isActive = isActiveTarget,
                            connectionState = connectionState,
                            showTechnicalDetails = showTechnicalDetails,
                            onSelect = {
                                viewModel.switchToServerTarget(target, context)
                            },
                            onReconnect = {
                                viewModel.triggerReconnect(context)
                            },
                            onEdit = {
                                onOpenDialogForTarget(target)
                            },
                            onDelete = {
                                viewModel.deleteServerTarget(target)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServerTargetItemRow(
    target: ServerTarget,
    isActive: Boolean,
    connectionState: ConnectionState,
    showTechnicalDetails: Boolean,
    onSelect: () -> Unit,
    onReconnect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val borderColor = if (isActive) TrackerGreen else CyberGrayBorder
    val containerColor = if (isActive) TrackerGreen.copy(alpha = 0.08f) else CyberBlack

    val friendlyStatus = when {
        !isActive -> "Tap to use this target"
        connectionState == ConnectionState.CONNECTED -> "Connected & active"
        connectionState == ConnectionState.CONNECTING -> "Connecting..."
        else -> "Selected (Waiting for connection)"
    }

    val endpointSubtitle = if (target.websocketUrl.isNotEmpty()) {
        target.websocketUrl
    } else {
        "${target.serverHost}:${target.serverPort}"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(if (isActive) 1.5.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable { onSelect() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("server_target_item_${target.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (isActive) "Active Target Server" else "Inactive Target Server",
                    tint = if (isActive) TrackerGreen else TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = target.name.ifBlank { target.serverHost },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isActive) {
                            Box(
                                modifier = Modifier
                                    .background(TrackerGreen.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Active",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TrackerGreen
                                )
                            }
                        }
                    }

                    Text(
                        text = endpointSubtitle,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = friendlyStatus,
                        fontSize = 11.sp,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isActive) TrackerGreen else TextSecondary.copy(alpha = 0.8f)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (!isActive) {
                    Button(
                        onClick = onSelect,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TrackerBlue.copy(alpha = 0.14f),
                            contentColor = TrackerBlue
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("switch_server_target_${target.id}")
                    ) {
                        Text(
                            text = "Switch",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    IconButton(
                        onClick = onReconnect,
                        modifier = Modifier.testTag("reconnect_active_server_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reconnect to target server",
                            tint = TrackerGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("edit_server_target_${target.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Server Target",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_server_target_${target.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Server Target",
                        tint = TrackerRed.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Extra Technical Metadata when "Show All Data" toggle is ON
        AnimatedVisibility(visible = showTechnicalDetails) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .background(CyberGrayDeep, RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = "Host: ${target.serverHost}  •  Port: ${target.serverPort}",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
                if (target.websocketUrl.isNotEmpty()) {
                    Text(
                        text = "WebSocket: ${target.websocketUrl}",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = "Client ID: ${target.clientId.ifEmpty { "Default" }}  •  Token: ${if (target.secretToken.isNotEmpty()) "Configured (${target.secretToken.take(4)}…)" else "None"}",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun ServerTargetDialog(
    viewModel: TrackerViewModel,
    initialTarget: ServerTarget?,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentConfig by viewModel.pairingConfig.collectAsState()

    var nameInput by remember(initialTarget) {
        mutableStateOf(initialTarget?.name ?: "")
    }
    var hostInput by remember(initialTarget) {
        mutableStateOf(initialTarget?.serverHost ?: currentConfig?.serverHost ?: "")
    }
    var portInput by remember(initialTarget) {
        mutableStateOf((initialTarget?.serverPort ?: currentConfig?.serverPort ?: 8080).toString())
    }
    var wsUrlInput by remember(initialTarget) {
        mutableStateOf(initialTarget?.websocketUrl ?: "")
    }
    var clientIdInput by remember(initialTarget) {
        mutableStateOf(initialTarget?.clientId?.ifEmpty { currentConfig?.clientId ?: "" } ?: currentConfig?.clientId ?: "")
    }
    var secretTokenInput by remember(initialTarget) {
        mutableStateOf(initialTarget?.secretToken?.ifEmpty { currentConfig?.secretToken ?: "" } ?: currentConfig?.secretToken ?: "")
    }

    var showAdvancedFields by remember(initialTarget) {
        mutableStateOf(!initialTarget?.websocketUrl.isNullOrEmpty())
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberGrayBorder, RoundedCornerShape(24.dp))
                .testTag("server_target_dialog"),
            colors = CardDefaults.cardColors(containerColor = CyberGrayDeep),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (initialTarget == null || initialTarget.id == 0L) "Add Tracking Target" else "Edit Tracking Target",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Enter the address of the server or web portal where your location should be sent.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text(stringResource(R.string.target_server_name_label)) },
                    placeholder = { Text("e.g. Home Server, Family Portal") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_server_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TrackerBlue,
                        unfocusedBorderColor = CyberGrayBorder,
                        focusedLabelColor = TrackerBlue
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = hostInput,
                        onValueChange = { hostInput = it },
                        label = { Text(stringResource(R.string.target_server_host_label)) },
                        placeholder = { Text("192.168.1.50 or portal.com") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.68f)
                            .testTag("dialog_server_host_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TrackerGreen,
                            unfocusedBorderColor = CyberGrayBorder,
                            focusedLabelColor = TrackerGreen
                        )
                    )

                    OutlinedTextField(
                        value = portInput,
                        onValueChange = { portInput = it },
                        label = { Text(stringResource(R.string.target_server_port_label)) },
                        placeholder = { Text("8080") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(0.32f)
                            .testTag("dialog_server_port_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TrackerGreen,
                            unfocusedBorderColor = CyberGrayBorder,
                            focusedLabelColor = TrackerGreen
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Toggle for advanced connection/credentials fields
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showAdvancedFields = !showAdvancedFields }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showAdvancedFields) "Hide Advanced Options" else "Show Advanced Options (Optional)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TrackerBlue
                    )
                    Icon(
                        imageVector = if (showAdvancedFields) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle advanced options",
                        tint = TrackerBlue
                    )
                }

                AnimatedVisibility(visible = showAdvancedFields) {
                    Column {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = wsUrlInput,
                            onValueChange = { wsUrlInput = it },
                            label = { Text(stringResource(R.string.target_server_ws_url_label)) },
                            placeholder = { Text("e.g. wss://my-portal.com/ws/tracker") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dialog_server_ws_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TrackerBlue,
                                unfocusedBorderColor = CyberGrayBorder,
                                focusedLabelColor = TrackerBlue
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = clientIdInput,
                            onValueChange = { clientIdInput = it },
                            label = { Text("Device ID (Optional)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dialog_server_client_id_input"),
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
                            label = { Text("Pairing Key / Token (Optional)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dialog_server_token_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TrackerBlue,
                                unfocusedBorderColor = CyberGrayBorder,
                                focusedLabelColor = TrackerBlue
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("dialog_server_cancel_button")
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    TextButton(
                        onClick = {
                            if (hostInput.isNotBlank()) {
                                viewModel.saveServerTargetOnly(
                                    name = nameInput,
                                    rawHost = hostInput,
                                    rawPort = portInput,
                                    explicitWsUrl = wsUrlInput,
                                    clientIdOverride = clientIdInput,
                                    secretTokenOverride = secretTokenInput,
                                    targetId = initialTarget?.id ?: 0L
                                )
                                onDismiss()
                            }
                        },
                        enabled = hostInput.isNotBlank(),
                        modifier = Modifier.testTag("dialog_server_save_only_button")
                    ) {
                        Text("Save Only", color = TrackerBlue, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Button(
                        onClick = {
                            if (hostInput.isNotBlank()) {
                                viewModel.saveAndActivateServerTarget(
                                    name = nameInput,
                                    rawHost = hostInput,
                                    rawPort = portInput,
                                    explicitWsUrl = wsUrlInput,
                                    clientIdOverride = clientIdInput,
                                    secretTokenOverride = secretTokenInput,
                                    targetId = initialTarget?.id ?: 0L,
                                    context = context
                                )
                                onDismiss()
                            }
                        },
                        enabled = hostInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TrackerGreen,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("dialog_server_activate_button")
                    ) {
                        Text("Save & Connect", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
