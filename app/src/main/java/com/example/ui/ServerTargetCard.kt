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
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.data.PairingConfig
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
    onOpenDialogForTarget: (ServerTarget?) -> Unit
) {
    val context = LocalContext.current
    val pairingConfig by viewModel.pairingConfig.collectAsState()
    val serverTargets by viewModel.serverTargets.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    var isQuickEntryExpanded by remember { mutableStateOf(false) }

    var quickName by remember { mutableStateOf("") }
    var quickHost by remember(pairingConfig?.serverHost) {
        mutableStateOf(pairingConfig?.serverHost?.takeIf { it.isNotEmpty() } ?: viewModel.inputHost.value)
    }
    var quickPort by remember(pairingConfig?.serverPort) {
        mutableStateOf((pairingConfig?.serverPort ?: viewModel.inputPort.value.toIntOrNull() ?: 8080).toString())
    }

    LaunchedEffect(serverTargets.isEmpty()) {
        if (serverTargets.isEmpty()) {
            isQuickEntryExpanded = true
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberGrayBorder, RoundedCornerShape(24.dp))
            .testTag("server_target_switcher_card"),
        colors = CardDefaults.cardColors(containerColor = CyberGrayDeep),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = "Server Targets",
                        tint = TrackerBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.target_server_header),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TrackerBlue,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = stringResource(R.string.target_server_title),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                    }
                }

                Button(
                    onClick = { onOpenDialogForTarget(null) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TrackerBlue.copy(alpha = 0.2f),
                        contentColor = TrackerBlue
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("add_server_target_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Target Server",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.target_server_add_button),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.target_server_subtitle),
                fontSize = 11.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Active Server Summary Banner
            ActiveServerBanner(
                pairingConfig = pairingConfig,
                connectionState = connectionState,
                onEditCurrent = {
                    val current = pairingConfig
                    if (current != null && current.serverHost.isNotEmpty()) {
                        val matchingTarget = serverTargets.find {
                            it.serverHost == current.serverHost &&
                            it.serverPort == current.serverPort &&
                            it.websocketUrl == current.websocketUrl
                        }
                        if (matchingTarget != null) {
                            onOpenDialogForTarget(matchingTarget)
                        } else {
                            onOpenDialogForTarget(
                                ServerTarget(
                                    id = 0,
                                    name = current.serverHost,
                                    serverHost = current.serverHost,
                                    serverPort = current.serverPort,
                                    websocketUrl = current.websocketUrl,
                                    clientId = current.clientId,
                                    secretToken = current.secretToken
                                )
                            )
                        }
                    } else {
                        isQuickEntryExpanded = true
                    }
                },
                onReconnect = {
                    viewModel.triggerReconnect(context)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Expandable Direct Target Server Entry Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberGrayLight)
                    .clickable { isQuickEntryExpanded = !isQuickEntryExpanded }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("toggle_quick_server_input_button"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = TrackerGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.target_server_quick_entry_toggle),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Icon(
                    imageVector = if (isQuickEntryExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isQuickEntryExpanded) "Collapse server input" else "Expand server input",
                    tint = TrackerGreen
                )
            }

            AnimatedVisibility(visible = isQuickEntryExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    OutlinedTextField(
                        value = quickName,
                        onValueChange = { quickName = it },
                        label = { Text(stringResource(R.string.target_server_name_label)) },
                        placeholder = { Text("e.g. Cloud Server, Local Dev, Office") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("target_server_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TrackerBlue,
                            unfocusedBorderColor = CyberGrayBorder,
                            focusedLabelColor = TrackerBlue,
                            cursorColor = TrackerBlue
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = quickHost,
                            onValueChange = { quickHost = it },
                            label = { Text(stringResource(R.string.target_server_host_label)) },
                            placeholder = { Text("192.168.1.50 or portal.com") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(0.68f)
                                .testTag("target_server_host_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TrackerGreen,
                                unfocusedBorderColor = CyberGrayBorder,
                                focusedLabelColor = TrackerGreen,
                                cursorColor = TrackerGreen
                            )
                        )

                        OutlinedTextField(
                            value = quickPort,
                            onValueChange = { quickPort = it },
                            label = { Text(stringResource(R.string.target_server_port_label)) },
                            placeholder = { Text("8080") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(0.32f)
                                .testTag("target_server_port_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = TrackerGreen,
                                unfocusedBorderColor = CyberGrayBorder,
                                focusedLabelColor = TrackerGreen,
                                cursorColor = TrackerGreen
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (quickHost.isNotBlank()) {
                                    viewModel.saveAndActivateServerTarget(
                                        name = quickName,
                                        rawHost = quickHost,
                                        rawPort = quickPort,
                                        context = context
                                    )
                                    quickName = ""
                                }
                            },
                            enabled = quickHost.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TrackerGreen,
                                contentColor = CyberBlack
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("switch_and_connect_server_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.target_server_switch_connect_button),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = {
                                if (quickHost.isNotBlank()) {
                                    viewModel.saveServerTargetOnly(
                                        name = quickName,
                                        rawHost = quickHost,
                                        rawPort = quickPort
                                    )
                                    quickName = ""
                                }
                            },
                            enabled = quickHost.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberGrayLight,
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .border(1.dp, CyberGrayBorder, RoundedCornerShape(10.dp))
                                .testTag("save_server_target_only_button")
                        ) {
                            Text(
                                text = stringResource(R.string.target_server_save_only_button),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Saved Server Targets List for 1-Tap Switching
            Text(
                text = stringResource(R.string.target_server_saved_list_label),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (serverTargets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CyberGrayBorder, RoundedCornerShape(12.dp))
                        .background(CyberBlack, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.target_server_empty_hint),
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            onSelect = {
                                viewModel.switchToServerTarget(target, context)
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
private fun ActiveServerBanner(
    pairingConfig: PairingConfig?,
    connectionState: ConnectionState,
    onEditCurrent: () -> Unit,
    onReconnect: () -> Unit
) {
    val hasServer = pairingConfig != null && pairingConfig.serverHost.isNotEmpty()
    val endpointText = when {
        !hasServer -> "No target server configured"
        pairingConfig!!.websocketUrl.isNotEmpty() -> pairingConfig.websocketUrl
        else -> "${pairingConfig.serverHost}:${pairingConfig.serverPort}"
    }

    val statusColor = when (connectionState) {
        ConnectionState.CONNECTED -> TrackerGreen
        ConnectionState.CONNECTING -> TrackerBlue
        ConnectionState.DISCONNECTED -> TrackerRed
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberGrayBorder, RoundedCornerShape(14.dp))
            .background(CyberBlack, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "ACTIVE TARGET ENDPOINT",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = endpointText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (hasServer) TextPrimary else TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onEditCurrent,
                modifier = Modifier.testTag("edit_active_server_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit active target server",
                    tint = TrackerBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
            if (hasServer) {
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
        }
    }
}

@Composable
private fun ServerTargetItemRow(
    target: ServerTarget,
    isActive: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val borderColor = if (isActive) TrackerGreen else CyberGrayBorder
    val containerColor = if (isActive) TrackerGreen.copy(alpha = 0.08f) else CyberBlack

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .background(containerColor)
            .clickable { onSelect() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag("server_target_item_${target.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (isActive) Icons.Default.CheckCircle else Icons.Default.CellTower,
                contentDescription = if (isActive) "Active Target Server" else "Saved Target Server",
                tint = if (isActive) TrackerGreen else TrackerBlue,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = target.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .background(TrackerGreen.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.target_server_active_badge),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TrackerGreen,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
                val endpointSubtitle = if (target.websocketUrl.isNotEmpty()) {
                    target.websocketUrl
                } else {
                    "${target.serverHost}:${target.serverPort}"
                }
                Text(
                    text = endpointSubtitle,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (!isActive) {
                TextButton(
                    onClick = onSelect,
                    modifier = Modifier.testTag("switch_server_target_${target.id}")
                ) {
                    Text(
                        text = stringResource(R.string.target_server_switch_button),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TrackerGreen
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
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_server_target_${target.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Server Target",
                    tint = TrackerRed.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
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

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, CyberGrayBorder, RoundedCornerShape(20.dp))
                .testTag("server_target_dialog"),
            colors = CardDefaults.cardColors(containerColor = CyberGrayDeep),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (initialTarget == null || initialTarget.id == 0L) "ADD TARGET SERVER" else "EDIT TARGET SERVER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TrackerBlue,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (initialTarget == null || initialTarget.id == 0L) "Configure Server Endpoint" else "Update Server Endpoint",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text(stringResource(R.string.target_server_name_label)) },
                    placeholder = { Text("e.g. Production Portal, Local Server") },
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

                OutlinedTextField(
                    value = hostInput,
                    onValueChange = { hostInput = it },
                    label = { Text(stringResource(R.string.target_server_host_label)) },
                    placeholder = { Text("e.g. 192.168.1.50 or tracker.example.com") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_server_host_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TrackerGreen,
                        unfocusedBorderColor = CyberGrayBorder,
                        focusedLabelColor = TrackerGreen
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = portInput,
                    onValueChange = { portInput = it },
                    label = { Text(stringResource(R.string.target_server_port_label)) },
                    placeholder = { Text("8080 or 443") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_server_port_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TrackerGreen,
                        unfocusedBorderColor = CyberGrayBorder,
                        focusedLabelColor = TrackerGreen
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

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
                    label = { Text("Client ID (Optional per-server override)") },
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
                    label = { Text("Secret Token (Optional per-server override)") },
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
                            contentColor = CyberBlack
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("dialog_server_activate_button")
                    ) {
                        Text("Switch & Connect", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
