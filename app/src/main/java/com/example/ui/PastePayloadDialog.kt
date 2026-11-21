package com.example.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.TrackerViewModel
import com.example.ui.theme.CyberGrayBorder
import com.example.ui.theme.CyberGrayDeep
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TrackerBlue

@Composable
fun PastePayloadDialog(
    viewModel: TrackerViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var payloadInput by remember { mutableStateOf("") }

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
                    text = "Paste Pairing Code",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Paste the pairing link, token, or setup code from your tracking web portal to connect this device.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = payloadInput,
                    onValueChange = { payloadInput = it },
                    label = { Text("Pairing Code or Link") },
                    placeholder = { Text("tracker://pair?... or {\"clientId\":...}") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
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
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (payloadInput.isNotBlank()) {
                                viewModel.processPairingQR(payloadInput, context)
                                onDismiss()
                            }
                        },
                        enabled = payloadInput.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TrackerBlue)
                    ) {
                        Text("Pair Device", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
