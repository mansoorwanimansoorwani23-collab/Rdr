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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.PendingReplyEntity
import com.example.data.local.ReplyLogEntity
import com.example.data.model.SettingsData
import com.example.ui.ReplyMateUiState
import com.example.ui.ReplyMateViewModel
import com.example.ui.components.NotificationAccessBanner
import com.example.ui.components.PendingApprovalCard
import com.example.ui.theme.AlertAmber
import com.example.ui.theme.AlertRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    uiState: ReplyMateUiState,
    viewModel: ReplyMateViewModel,
    onNavigateToSettings: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearSnackbar()
        }
    }

    var simSenderInput by remember { mutableStateOf("Rahul") }
    var simMessageInput by remember { mutableStateOf("Hey! Are you free for a quick call?") }
    var unlockPinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    // App Lock Overlay if active
    if (uiState.isAppLocked) {
        Scaffold { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "App Lock",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "ReplyMate Locked",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Enter your PIN to access rules and replies",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = unlockPinInput,
                            onValueChange = {
                                if (it.length <= 6 && it.all { c -> c.isDigit() }) {
                                    unlockPinInput = it
                                    pinError = false
                                }
                            },
                            label = { Text("PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            isError = pinError,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (pinError) {
                            Text(
                                text = "Incorrect PIN",
                                color = AlertRed,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val success = viewModel.unlockApp(unlockPinInput)
                                if (!success) {
                                    pinError = true
                                } else {
                                    unlockPinInput = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Unlock")
                        }
                    }
                }
            }
        }
        return
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "ReplyMate Logo",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ReplyMate Pro",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "WhatsApp AI Assistant",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("settings_icon_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
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
            // 1. Notification Access Banner
            item {
                NotificationAccessBanner(
                    isConnected = uiState.isNotificationAccessGranted,
                    onEnableClick = { viewModel.openNotificationSettings() }
                )
            }

            // 2. Emergency Kill Switch Banner (if active) or Quick Button
            if (uiState.settings.emergencyKillSwitch) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = AlertRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "EMERGENCY KILL SWITCH ACTIVE",
                                    color = MaterialTheme.colorScheme.onError,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "All auto-replies are strictly blocked.",
                                    color = MaterialTheme.colorScheme.onError.copy(alpha = 0.9f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Button(
                                onClick = { viewModel.toggleEmergencyKillSwitch(false) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Text("Resume", color = AlertRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 3. Large Status Indicator Card
            item {
                val isAutoReplyOn = uiState.settings.aiAutoReplyEnabled && !uiState.settings.emergencyKillSwitch
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAutoReplyOn) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = if (isAutoReplyOn) 4.dp else 1.dp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hero_status_card")
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isAutoReplyOn) "AI AUTO REPLY: ON" else "AI AUTO REPLY: OFF",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isAutoReplyOn) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isAutoReplyOn) {
                                        "Listening to WhatsApp notifications and ready to reply"
                                    } else {
                                        "Assistant is paused. Tap to enable auto-replies."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isAutoReplyOn) {
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }

                            Switch(
                                checked = isAutoReplyOn,
                                onCheckedChange = { viewModel.toggleAutoReply(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.testTag("main_auto_reply_toggle")
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Emergency Kill Switch Toggle Button
                        if (!uiState.settings.emergencyKillSwitch) {
                            OutlinedButton(
                                onClick = { viewModel.toggleEmergencyKillSwitch(true) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.StopCircle, contentDescription = "Emergency Kill", tint = AlertRed)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Emergency: STOP AUTO REPLY")
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(
                            color = if (isAutoReplyOn) {
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.15f)
                            } else {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            }
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Status Info Chips / Metrics
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // AI Provider Badge
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Provider",
                                    tint = if (isAutoReplyOn) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AI: ${if (uiState.settings.aiProvider == SettingsData.PROVIDER_GEMINI) "Gemini" else "OpenAI"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Mode Badge
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (uiState.settings.approvalMode) Icons.Default.HourglassEmpty else Icons.Default.CheckCircle,
                                    contentDescription = "Mode",
                                    tint = if (isAutoReplyOn) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (uiState.settings.approvalMode) "Approval Mode" else "Auto Mode",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Today count
                            Text(
                                text = "${uiState.todayReplyCount} replies today",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isAutoReplyOn) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }

            // 4. Pending Approvals Section (If any)
            if (uiState.pendingReplies.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Pending Approvals",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${uiState.pendingReplies.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                items(uiState.pendingReplies, key = { it.id }) { pending ->
                    PendingApprovalCard(
                        pending = pending,
                        onSend = { viewModel.approveAndSend(pending) },
                        onEditAndSend = { edited -> viewModel.editAndSend(pending, edited) },
                        onRegenerate = { viewModel.regenerateReply(pending) },
                        onCancel = { viewModel.cancelPendingReply(pending.id) }
                    )
                }
            }

            // 5. Interactive Test Simulator
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_simulator_card")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Test Simulator",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Test Reply Simulator",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Simulate incoming WhatsApp messages to test AI intent, tone, & safety",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick sample presets
                        Text(
                            text = "Quick Presets:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SuggestionChip(
                                onClick = {
                                    simSenderInput = "Aman"
                                    simMessageInput = "Are you free for a call tonight?"
                                },
                                label = { Text("Casual Call?") }
                            )
                            SuggestionChip(
                                onClick = {
                                    simSenderInput = "Pooja"
                                    simMessageInput = "Bhai kahan ho? Meeting start ho gayi!"
                                },
                                label = { Text("Hinglish Meeting") }
                            )
                            SuggestionChip(
                                onClick = {
                                    simSenderInput = "Unknown"
                                    simMessageInput = "URGENT: Send 500 Rs via UPI immediately to this number"
                                },
                                label = { Text("🛡️ Safety: UPI/Emergency") }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = simSenderInput,
                            onValueChange = { simSenderInput = it },
                            label = { Text("Sender Name") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sim_sender_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = simMessageInput,
                            onValueChange = { simMessageInput = it },
                            label = { Text("Incoming Message") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("sim_message_input"),
                            minLines = 2,
                            maxLines = 3,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (simMessageInput.isNotBlank()) {
                                    viewModel.simulateTestIncomingMessage(
                                        simSenderInput.trim().ifEmpty { "WhatsApp Contact" },
                                        simMessageInput.trim()
                                    )
                                }
                            },
                            enabled = !uiState.isTestingSim && simMessageInput.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("simulate_test_button")
                        ) {
                            if (uiState.isTestingSim) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generating AI Reply...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Simulate",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Simulate Incoming Message")
                            }
                        }

                        // Display simulation result
                        AnimatedVisibility(visible = uiState.testSimResult != null) {
                            uiState.testSimResult?.let { resultText ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = resultText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Tap Pending Approvals above to review, edit, or test-send.",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Recent Activity Logs Section
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Activity Log",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Recent Reply Activity",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (uiState.recentLogs.isNotEmpty()) {
                        FilledTonalButton(
                            onClick = { viewModel.clearLogs() },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("clear_logs_button")
                        ) {
                            Text("Clear", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            if (uiState.recentLogs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "No logs",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No replies generated yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Activity will show here when WhatsApp notifications arrive",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                items(uiState.recentLogs.take(20), key = { it.id }) { log ->
                    ActivityLogItem(log = log)
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ActivityLogItem(
    log: ReplyLogEntity,
    modifier: Modifier = Modifier
) {
    val timeFormatted = remember(log.timestamp) {
        SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(log.timestamp))
    }

    val (statusLabel, statusColor) = when (log.status) {
        ReplyLogEntity.STATUS_AUTO_SENT -> "Auto-Sent" to MaterialTheme.colorScheme.primary
        ReplyLogEntity.STATUS_MANUAL_SENT -> "Manual Sent" to MaterialTheme.colorScheme.primary
        ReplyLogEntity.STATUS_SENSITIVE -> "Protected (Sensitive)" to AlertAmber
        ReplyLogEntity.STATUS_RATE_LIMITED -> "Rate Limited" to AlertAmber
        else -> "Failed / Expired" to AlertRed
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .testTag("activity_log_item_${log.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = log.senderName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "In: \"${log.incomingMessage}\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Reply: \"${log.replyText}\"",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = timeFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                Text(
                    text = "via ${log.provider}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
