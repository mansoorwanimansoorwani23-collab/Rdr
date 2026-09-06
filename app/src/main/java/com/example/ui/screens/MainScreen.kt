package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.Surface
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
import com.example.ui.theme.AlertGreen
import com.example.ui.theme.AlertRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    uiState: ReplyMateUiState,
    viewModel: ReplyMateViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToDiagnostics: () -> Unit,
    onOpenAiWizard: () -> Unit
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
    var selectedLogFilter by remember { mutableStateOf("ALL") }

    val filteredLogs = remember(uiState.recentLogs, selectedLogFilter) {
        when (selectedLogFilter) {
            "RECEIVED" -> uiState.recentLogs.filter { it.status == ReplyLogEntity.STATUS_RECEIVED || it.status == ReplyLogEntity.STATUS_PARSED }
            "SENT" -> uiState.recentLogs.filter { it.status == ReplyLogEntity.STATUS_AUTO_SENT || it.status == ReplyLogEntity.STATUS_MANUAL_SENT }
            "APPROVAL" -> uiState.recentLogs.filter { it.status == ReplyLogEntity.STATUS_APPROVAL_REQUIRED }
            "IGNORED" -> uiState.recentLogs.filter { it.status == ReplyLogEntity.STATUS_IGNORED || it.status == ReplyLogEntity.STATUS_SENSITIVE }
            else -> uiState.recentLogs
        }
    }

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
                                text = "Personal WhatsApp AI Assistant",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToDiagnostics,
                        modifier = Modifier.testTag("top_diagnostics_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Diagnostics",
                            tint = if (uiState.isServiceConnected) MaterialTheme.colorScheme.primary else AlertAmber
                        )
                    }
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
            // 1. Notification Access Banner (if missing)
            item {
                NotificationAccessBanner(
                    isConnected = uiState.isNotificationAccessGranted,
                    onEnableClick = { viewModel.openNotificationSettings() }
                )
            }

            // 2. Emergency Kill Switch Banner (if active)
            if (uiState.settings.emergencyKillSwitch) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = AlertRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("emergency_stop_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "EMERGENCY STOP ACTIVE",
                                    color = MaterialTheme.colorScheme.onError,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Auto-replies stopped & queue cleared. Safe state confirmed.",
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

            // 3. Hero Status Card
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
                                        "Assistant is paused. Toggle switch to start auto-replies."
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

                        // Status indicators: Service State & Target WhatsApp
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Listener Service State
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (uiState.isServiceConnected) AlertGreen.copy(alpha = 0.15f) else AlertAmber.copy(alpha = 0.15f),
                                modifier = Modifier.clickable {
                                    if (!uiState.isServiceConnected) viewModel.forceRebindService()
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isServiceConnected) Icons.Default.CheckCircle else Icons.Default.Sync,
                                        contentDescription = null,
                                        tint = if (uiState.isServiceConnected) AlertGreen else AlertAmber,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (uiState.isServiceConnected) "Service: Active" else "Service: Tap to Reconnect",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (uiState.isServiceConnected) AlertGreen else AlertAmber
                                    )
                                }
                            }

                            // WhatsApp Package Target
                            val pkgLabel = when (uiState.settings.selectedWhatsAppPackage) {
                                SettingsData.PACKAGE_WHATSAPP -> "Messenger"
                                SettingsData.PACKAGE_WHATSAPP_BUSINESS -> "Business"
                                else -> "Both Messenger & Business"
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = "Target: $pkgLabel",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // EMERGENCY STOP Button (Section 21)
                        if (!uiState.settings.emergencyKillSwitch) {
                            Button(
                                onClick = { viewModel.emergencyStop() },
                                colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("emergency_stop_button")
                            ) {
                                Icon(Icons.Default.StopCircle, contentDescription = "Stop", tint = MaterialTheme.colorScheme.onError)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("STOP AUTO REPLY (EMERGENCY)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onError)
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

                        // Status Info Chips
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Provider",
                                    tint = if (isAutoReplyOn) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AI: ${if (uiState.settings.aiProvider == SettingsData.PROVIDER_GEMINI) "Gemini" else "OpenAI"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (uiState.settings.approvalMode) Icons.Default.HourglassEmpty else Icons.Default.CheckCircle,
                                    contentDescription = "Mode",
                                    tint = if (isAutoReplyOn) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (uiState.settings.approvalMode) "Approval Mode" else "Auto Mode",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Text(
                                text = "${uiState.todayReplyCount} today",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isAutoReplyOn) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 4. Quick Actions Row: Diagnostics & AI Wizard
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilledTonalButton(
                        onClick = onNavigateToDiagnostics,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("action_diagnostics_button")
                    ) {
                        Icon(imageVector = Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Diagnostics Audit", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }

                    FilledTonalButton(
                        onClick = onOpenAiWizard,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("action_ai_wizard_button")
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI Setup Wizard", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 5. Pending Approvals Section (If any)
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

            // 6. Interactive Test Simulator
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
                                        text = "Simulate incoming WhatsApp messages to test AI intent, tone, & language",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

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
                                label = { Text("Hinglish Work") }
                            )
                            SuggestionChip(
                                onClick = {
                                    simSenderInput = "Boss"
                                    simMessageInput = "Please send the weekly report by 5 PM today."
                                },
                                label = { Text("Formal Boss") }
                            )
                            SuggestionChip(
                                onClick = {
                                    simSenderInput = "Bank Alert"
                                    simMessageInput = "Your OTP is 948210. Never share this code."
                                },
                                label = { Text("Sensitive / OTP") }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = simSenderInput,
                            onValueChange = { simSenderInput = it },
                            label = { Text("Contact Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = simMessageInput,
                            onValueChange = { simMessageInput = it },
                            label = { Text("Incoming Message") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.simulateTestIncomingMessage(
                                        sender = simSenderInput,
                                        message = simMessageInput
                                    )
                                },
                                enabled = !uiState.isTestingSim && !uiState.isRunningReplyTest && simSenderInput.isNotBlank() && simMessageInput.isNotBlank(),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("run_simulation_button")
                            ) {
                                if (uiState.isTestingSim) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Testing...")
                                } else {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Simulate AI")
                                }
                            }

                            FilledTonalButton(
                                onClick = {
                                    viewModel.runReplyTest(
                                        sender = simSenderInput,
                                        message = simMessageInput
                                    )
                                },
                                enabled = !uiState.isRunningReplyTest && !uiState.isTestingSim && simSenderInput.isNotBlank() && simMessageInput.isNotBlank(),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(46.dp)
                                    .testTag("run_reply_test_main_button")
                            ) {
                                if (uiState.isRunningReplyTest) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Auditing...")
                                } else {
                                    Icon(imageVector = Icons.Default.BugReport, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Run Reply Test")
                                }
                            }
                        }

                        AnimatedVisibility(visible = uiState.replyTestResult != null) {
                            uiState.replyTestResult?.let { testResult ->
                                Card(
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Reply Test Result (${testResult.finalStatus})",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            IconButton(onClick = { viewModel.clearReplyTestResult() }, modifier = Modifier.size(20.dp)) {
                                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        testResult.steps.forEach { step ->
                                            Text(
                                                text = "${if (step.isSuccess) "✓" else "⚠"} ${step.stepName}: ${step.details}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (step.isSuccess) MaterialTheme.colorScheme.onSurfaceVariant else AlertAmber,
                                                modifier = Modifier.padding(vertical = 2.dp)
                                            )
                                        }
                                        if (!testResult.generatedReply.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "AI Reply: \"${testResult.generatedReply}\"",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = AlertGreen
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(visible = uiState.testSimResult != null) {
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Simulation Result",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        IconButton(onClick = { viewModel.clearSimResult() }, modifier = Modifier.size(20.dp)) {
                                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = uiState.testSimResult ?: "",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 7. Recent Activity Logs Section with Filters
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
                            text = "Recent Activity",
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

            // Activity Log Filter Chips
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val filters = listOf("ALL", "RECEIVED", "SENT", "APPROVAL", "IGNORED")
                    filters.forEach { filter ->
                        FilterChip(
                            selected = selectedLogFilter == filter,
                            onClick = { selectedLogFilter = filter },
                            label = { Text(filter) }
                        )
                    }
                }
            }

            if (filteredLogs.isEmpty()) {
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
                                text = "No recent activity matching filter",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Incoming WhatsApp notifications will appear here in real-time",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                items(filteredLogs.take(25), key = { it.id }) { log ->
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
        SimpleDateFormat("MMM dd, hh:mm:ss a", Locale.getDefault()).format(Date(log.timestamp))
    }

    val (statusLabel, statusColor) = when (log.status) {
        ReplyLogEntity.STATUS_RECEIVED -> "Received" to AlertGreen
        ReplyLogEntity.STATUS_PARSED -> "Parsed" to MaterialTheme.colorScheme.primary
        ReplyLogEntity.STATUS_PROCESSING -> "Processing" to MaterialTheme.colorScheme.primary
        ReplyLogEntity.STATUS_APPROVAL_REQUIRED -> "Approval Required" to AlertAmber
        ReplyLogEntity.STATUS_AUTO_SENT -> "Auto-Sent" to AlertGreen
        ReplyLogEntity.STATUS_MANUAL_SENT -> "Manual Sent" to AlertGreen
        ReplyLogEntity.STATUS_SENSITIVE -> "Protected (Sensitive)" to AlertAmber
        ReplyLogEntity.STATUS_RATE_LIMITED -> "Rate Limited" to AlertAmber
        ReplyLogEntity.STATUS_IGNORED -> "Skipped / Rule" to AlertAmber
        ReplyLogEntity.STATUS_NO_ACTION -> "No Reply Action" to AlertAmber
        else -> "Failed" to AlertRed
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
                text = log.replyText,
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
