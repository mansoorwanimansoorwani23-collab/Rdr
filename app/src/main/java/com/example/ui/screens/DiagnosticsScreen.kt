package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.DiagnosticItemResult
import com.example.service.DiagnosticLogEntry
import com.example.service.DiagnosticStatus
import com.example.ui.ReplyMateUiState
import com.example.ui.ReplyMateViewModel
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.AlertGreen
import com.example.ui.theme.AlertRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DiagnosticsScreen(
    uiState: ReplyMateUiState,
    viewModel: ReplyMateViewModel,
    onBackClick: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onOpenAiWizard: () -> Unit
) {
    // Auto-run diagnostics on screen launch if empty
    LaunchedEffect(Unit) {
        if (uiState.diagnosticResults.isEmpty()) {
            viewModel.runFullDiagnostics()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Notification Diagnostics",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "End-to-end audit & live WhatsApp testing",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("diag_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.runFullDiagnostics() },
                        enabled = !uiState.isDiagnosticsRunning,
                        modifier = Modifier.testTag("diag_refresh_button")
                    ) {
                        if (uiState.isDiagnosticsRunning) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Rerun Diagnostics")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Overall Pipeline Health Status Card
            item {
                PipelineStatusCard(
                    uiState = uiState,
                    onReconnectService = { viewModel.forceRebindService() },
                    onOpenNotificationSettings = { viewModel.openNotificationSettings() }
                )
            }

            // 2. Action Button: RUN FULL DIAGNOSTICS
            item {
                Button(
                    onClick = { viewModel.runFullDiagnostics() },
                    enabled = !uiState.isDiagnosticsRunning,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("run_diagnostics_button")
                ) {
                    if (uiState.isDiagnosticsRunning) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Auditing Pipeline...")
                    } else {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("RUN FULL DIAGNOSTICS", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 3. 15 Diagnostic Tests Results
            if (uiState.diagnosticResults.isNotEmpty()) {
                item {
                    Text(
                        text = "15-Point Diagnostic Audit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                items(uiState.diagnosticResults, key = { it.id }) { itemResult ->
                    DiagnosticItemCard(
                        result = itemResult,
                        onAction = { actionType ->
                            when (actionType) {
                                "NOTIFICATION_SETTINGS" -> viewModel.openNotificationSettings()
                                "REBIND_SERVICE" -> viewModel.forceRebindService()
                                "SETTINGS_AI" -> onOpenAiWizard()
                                "SETTINGS_WHATSAPP" -> onNavigateToSettings()
                                else -> onNavigateToSettings()
                            }
                        }
                    )
                }
            }

            // 4. Live Notification Test Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                LiveTestSection(
                    events = uiState.diagnosticEvents,
                    onClear = { viewModel.clearDiagnostics() }
                )
            }

            // 5. Troubleshooting Guide Card
            item {
                TroubleshootingGuideCard(
                    onOpenNotificationSettings = { viewModel.openNotificationSettings() }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun PipelineStatusCard(
    uiState: ReplyMateUiState,
    onReconnectService: () -> Unit,
    onOpenNotificationSettings: () -> Unit
) {
    val accessGranted = uiState.isNotificationAccessGranted
    val serviceConnected = uiState.isServiceConnected
    val waInstalled = uiState.whatsAppInstallStatus.anyInstalled

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (accessGranted && serviceConnected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
            } else {
                AlertAmber.copy(alpha = 0.15f)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "SYSTEM CONNECTION STATUS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Status items
            StatusRow(
                label = "Notification Access",
                status = if (accessGranted) "Granted ✓" else "Missing ✕",
                isOk = accessGranted
            )
            Spacer(modifier = Modifier.height(6.dp))
            StatusRow(
                label = "Listener Service",
                status = if (serviceConnected) "Bound & Connected ✓" else "Unbound / Disconnected ⚠",
                isOk = serviceConnected
            )
            Spacer(modifier = Modifier.height(6.dp))
            StatusRow(
                label = "WhatsApp Target",
                status = if (waInstalled) {
                    val status = uiState.whatsAppInstallStatus
                    buildList {
                        if (status.isWhatsAppInstalled) add("Messenger")
                        if (status.isBusinessInstalled) add("Business")
                    }.joinToString(", ") + " ✓"
                } else {
                    "Not Detected ✕"
                },
                isOk = waInstalled
            )

            if (!serviceConnected) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Service not connected yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = onReconnectService,
                        modifier = Modifier.testTag("rebind_service_button")
                    ) {
                        Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reconnect")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, status: String, isOk: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = status,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isOk) AlertGreen else AlertRed
        )
    }
}

@Composable
private fun DiagnosticItemCard(
    result: DiagnosticItemResult,
    onAction: (String) -> Unit
) {
    val (statusColor, statusIcon, statusLabel) = when (result.status) {
        DiagnosticStatus.PASS -> Triple(AlertGreen, Icons.Default.CheckCircle, "PASS")
        DiagnosticStatus.WARNING -> Triple(AlertAmber, Icons.Default.Warning, "WARNING")
        DiagnosticStatus.FAILED -> Triple(AlertRed, Icons.Default.Close, "FAILED")
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("diag_test_card_${result.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = statusColor.copy(alpha = 0.15f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = statusLabel,
                                tint = statusColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${result.id}. ${result.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = result.explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!result.actionLabel.isNullOrBlank() && !result.actionType.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                FilledTonalButton(
                    onClick = { onAction(result.actionType) },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(text = result.actionLabel, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun LiveTestSection(
    events: List<DiagnosticLogEntry>,
    onClear: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Live Notification Test",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Send a WhatsApp message to test live detection",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (events.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear logs")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (events.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Waiting for incoming notifications...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Send a message from another phone to test",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            } else {
                val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                events.take(10).forEach { entry ->
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (entry.isWhatsApp) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = entry.eventType,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (entry.isWhatsApp) AlertGreen else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = timeFormat.format(Date(entry.timestamp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = entry.details,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (entry.isWhatsApp && (entry.senderAvailable || entry.messageAvailable || entry.hasReplyAction)) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BadgePill(
                                        label = if (entry.senderAvailable) "Sender: Available" else "Sender: Unavailable",
                                        isOk = entry.senderAvailable
                                    )
                                    BadgePill(
                                        label = if (entry.messageAvailable) "Message: Available" else "Message: Hidden",
                                        isOk = entry.messageAvailable
                                    )
                                    BadgePill(
                                        label = if (entry.hasReplyAction) "Reply: Available" else "Reply: No Action",
                                        isOk = entry.hasReplyAction
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgePill(label: String, isOk: Boolean) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isOk) AlertGreen.copy(alpha = 0.12f) else AlertAmber.copy(alpha = 0.12f)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = if (isOk) AlertGreen else AlertAmber,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun TroubleshootingGuideCard(onOpenNotificationSettings: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Why WhatsApp Notifications May Be Missed",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            val tips = listOf(
                "1. WhatsApp is open on screen: Android suppresses notifications when you are actively inside the chat.",
                "2. WhatsApp Web is active: Incoming messages read on your computer may not post notifications to the phone.",
                "3. Chat or Group is muted: Muted WhatsApp conversations do not generate full notifications.",
                "4. Battery optimization: If Android kills background services, tap 'Reconnect Service' to rebind the listener."
            )
            tips.forEach { tip ->
                Text(
                    text = tip,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 3.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onOpenNotificationSettings,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Notification Access Settings")
            }
        }
    }
}
