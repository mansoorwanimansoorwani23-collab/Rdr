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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContactRule
import com.example.data.model.SettingsData
import com.example.ui.ReplyMateUiState
import com.example.ui.ReplyMateViewModel
import com.example.ui.theme.AlertRed
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    uiState: ReplyMateUiState,
    viewModel: ReplyMateViewModel,
    onBackClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    var geminiKeyInput by remember { mutableStateOf("") }
    var openAiKeyInput by remember { mutableStateOf("") }
    var showGeminiKeyDialog by remember { mutableStateOf(false) }
    var showOpenAiKeyDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }

    // Dialog state for Add Contact Rule
    var showAddContactDialog by remember { mutableStateOf(false) }
    var newContactName by remember { mutableStateOf("") }
    var newContactPersonality by remember { mutableStateOf(SettingsData.PROFILE_FRIENDLY) }
    var newContactNotes by remember { mutableStateOf("") }

    // Dialog state for Learn My Style sample input
    var showAddSampleDialog by remember { mutableStateOf(false) }
    var sampleMessageInput by remember { mutableStateOf("") }

    // Dialog state for Pin setup
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }

    val settings = uiState.settings

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings & Intelligence",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. AI Provider & Keys Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Engine",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI Engine & Multi-Provider",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Active AI Provider",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = settings.aiProvider == SettingsData.PROVIDER_GEMINI,
                            onClick = {
                                viewModel.updateSettings(settings.copy(aiProvider = SettingsData.PROVIDER_GEMINI))
                            },
                            label = { Text("Google Gemini") },
                            modifier = Modifier.testTag("provider_chip_gemini")
                        )

                        FilterChip(
                            selected = settings.aiProvider == SettingsData.PROVIDER_OPENAI,
                            onClick = {
                                viewModel.updateSettings(settings.copy(aiProvider = SettingsData.PROVIDER_OPENAI))
                            },
                            label = { Text("OpenAI") },
                            modifier = Modifier.testTag("provider_chip_openai")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Smart Fallback toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Smart Fallback Provider", fontWeight = FontWeight.Medium)
                            Text(
                                "Automatically falls back to alternate provider if primary fails or is rate-limited",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.smartFallbackEnabled,
                            onCheckedChange = {
                                viewModel.updateSettings(settings.copy(smartFallbackEnabled = it))
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Gemini API Key entry
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Gemini API Key",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = uiState.geminiKeyMasked,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { showGeminiKeyDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("configure_gemini_key_button")
                        ) {
                            Text("Configure")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // OpenAI API Key entry
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "OpenAI API Key",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = uiState.openAiKeyMasked,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { showOpenAiKeyDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("configure_openai_key_button")
                        ) {
                            Text("Configure")
                        }
                    }
                }
            }

            // 2. Personality Profiles & Reply Length
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Reply Personality",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Personality Profiles & Length",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Global default reply personality:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SettingsData.ALL_PROFILES.forEach { style ->
                            FilterChip(
                                selected = settings.replyStyle == style,
                                onClick = {
                                    viewModel.updateSettings(settings.copy(replyStyle = style))
                                },
                                label = { Text(style) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Reply Length:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SettingsData.ALL_LENGTHS.forEach { len ->
                            FilterChip(
                                selected = settings.replyLength == len,
                                onClick = {
                                    viewModel.updateSettings(settings.copy(replyLength = len))
                                },
                                label = { Text(len) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Smart Language Matching:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SettingsData.ALL_LANGUAGES.forEach { lang ->
                            FilterChip(
                                selected = settings.preferredLanguage == lang,
                                onClick = {
                                    viewModel.updateSettings(settings.copy(preferredLanguage = lang))
                                },
                                label = { Text(lang) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Natural Language Custom Rules",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Add conversational rules like 'Never commit to weekend plans' or 'Always ask when they are free'",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    var naturalRulesInput by remember(settings.customNaturalLanguageRules) {
                        mutableStateOf(settings.customNaturalLanguageRules)
                    }

                    OutlinedTextField(
                        value = naturalRulesInput,
                        onValueChange = {
                            naturalRulesInput = it
                            viewModel.updateSettings(settings.copy(customNaturalLanguageRules = it))
                        },
                        placeholder = { Text("e.g. If anyone asks for money or loan, politely decline.") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // 3. "Learn My Style" Profile Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Learn My Style",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Learn My Style Profile",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(onClick = { showAddSampleDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add sample message")
                        }
                    }

                    Text(
                        text = "ReplyMate analyzes your sample texting messages locally to mimic your real tone, emoji habits, and expressions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Analyzed summary box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Detected Tone: ${uiState.styleAnalysis.tone}",
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Language: ${uiState.styleAnalysis.detectedLanguage} | Emojis: ${uiState.styleAnalysis.emojiFrequency}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Common words: ${uiState.styleAnalysis.commonExpressions.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Your Sample Messages (${uiState.userSampleMessages.size}):",
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodySmall
                    )

                    uiState.userSampleMessages.forEachIndexed { idx, sample ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "\"$sample\"",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { viewModel.removeUserSampleMessage(idx) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.resetUserStyle() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset Style Profile")
                    }
                }
            }

            // 4. Contact Intelligence & Per-Contact Rules
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Contact Rules",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Contact Intelligence Rules",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(onClick = { showAddContactDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Contact Rule")
                        }
                    }

                    Text(
                        text = "Assign distinct personalities (e.g. Professional for Boss, Funny for Best Friend) and manage local private memories.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (uiState.contactRules.isEmpty()) {
                        Text(
                            text = "No custom contact rules yet. Tap + to configure special contacts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        uiState.contactRules.values.forEach { rule ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(rule.contactName, fontWeight = FontWeight.Bold)
                                        Text(
                                            "Personality: ${rule.personality} | Auto-reply: ${if (rule.autoReplyEnabled) "ON" else "OFF"}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        if (rule.customNotes.isNotBlank()) {
                                            Text("Note: ${rule.customNotes}", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    IconButton(onClick = { viewModel.deleteContactRule(rule.contactName) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Rule")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Smart Group Mode & Batching
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = "Groups",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Group Mode & Smart Batching",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("WhatsApp Group Handling:", fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf(
                            SettingsData.GROUP_MODE_DISABLED to "OFF (Recommended)",
                            SettingsData.GROUP_MODE_MENTION_ONLY to "Mention Only (@)",
                            SettingsData.GROUP_MODE_SELECTED to "Selected Groups",
                            SettingsData.GROUP_MODE_ALL to "All Groups"
                        ).forEach { (mode, label) ->
                            FilterChip(
                                selected = settings.groupMode == mode,
                                onClick = {
                                    viewModel.updateSettings(settings.copy(groupMode = mode))
                                },
                                label = { Text(label) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Smart Batching
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Smart Message Batching", fontWeight = FontWeight.Medium)
                            Text(
                                "Combines rapid-fire messages into one single context before replying",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.smartBatchingEnabled,
                            onCheckedChange = {
                                viewModel.updateSettings(settings.copy(smartBatchingEnabled = it))
                            }
                        )
                    }

                    AnimatedVisibility(visible = settings.smartBatchingEnabled) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            Text("Batching Window: ${settings.batchWindowSeconds} seconds")
                            Slider(
                                value = settings.batchWindowSeconds.toFloat(),
                                onValueChange = {
                                    viewModel.updateSettings(settings.copy(batchWindowSeconds = it.roundToInt()))
                                },
                                valueRange = 2f..15f,
                                steps = 12
                            )
                        }
                    }
                }
            }

            // 6. Quiet Schedule & Security App Lock
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = "Quiet Hours",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Quiet Schedule & Security Lock",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quiet hours switch
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Quiet Hours Schedule", fontWeight = FontWeight.Medium)
                            Text(
                                "Sleep & focus hours (10:00 PM to 07:00 AM) auto-pauses replies",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.quietHoursEnabled,
                            onCheckedChange = {
                                viewModel.updateSettings(settings.copy(quietHoursEnabled = it))
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // App Lock (PIN)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("App Lock Protection", fontWeight = FontWeight.Medium)
                            Text(
                                if (settings.appLockEnabled) "PIN protection is active" else "Protect app settings & private memories with PIN",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.appLockEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    showPinDialog = true
                                } else {
                                    viewModel.updateSettings(settings.copy(appLockEnabled = false, appLockPin = ""))
                                }
                            }
                        )
                    }
                }
            }

            // 7. Privacy Center & Data Deletion
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Privacy",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Privacy Center & Local Data",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "All memory facts, rules, and conversation logs are strictly kept on-device. You can clear them at any time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.clearAllMemories() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clear Memories")
                        }

                        OutlinedButton(
                            onClick = { viewModel.clearConversationContext() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clear Context")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.clearLogs() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clear Logs")
                        }

                        OutlinedButton(
                            onClick = { viewModel.clearApiKeys() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Remove Keys")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { showResetConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AlertRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reset All Settings & Data")
                    }
                }
            }
        }
    }

    // Dialog: Add Contact Rule
    if (showAddContactDialog) {
        AlertDialog(
            onDismissRequest = { showAddContactDialog = false },
            title = { Text("Add Contact Rule") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newContactName,
                        onValueChange = { newContactName = it },
                        label = { Text("Contact Name (Exact WhatsApp name)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Personality Profile:")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        SettingsData.ALL_PROFILES.take(4).forEach { p ->
                            FilterChip(
                                selected = newContactPersonality == p,
                                onClick = { newContactPersonality = p },
                                label = { Text(p) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = newContactNotes,
                        onValueChange = { newContactNotes = it },
                        label = { Text("Notes (e.g. My project manager)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newContactName.isNotBlank()) {
                        viewModel.saveContactRule(
                            ContactRule(
                                contactName = newContactName.trim(),
                                personality = newContactPersonality,
                                customNotes = newContactNotes.trim()
                            )
                        )
                        showAddContactDialog = false
                        newContactName = ""
                        newContactNotes = ""
                    }
                }) {
                    Text("Save Rule")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddContactDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Add Sample Message
    if (showAddSampleDialog) {
        AlertDialog(
            onDismissRequest = { showAddSampleDialog = false },
            title = { Text("Add Message Sample") },
            text = {
                Column {
                    Text("Paste an example message you often send on WhatsApp:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = sampleMessageInput,
                        onValueChange = { sampleMessageInput = it },
                        placeholder = { Text("e.g. Haan bro, call karta hu 10 min mein") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (sampleMessageInput.isNotBlank()) {
                        viewModel.addUserSampleMessage(sampleMessageInput.trim())
                        showAddSampleDialog = false
                        sampleMessageInput = ""
                    }
                }) {
                    Text("Analyze & Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSampleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: PIN Setup
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Set 4-Digit Security PIN") },
            text = {
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) pinInput = it },
                    label = { Text("Enter PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (pinInput.length >= 4) {
                        viewModel.updateSettings(settings.copy(appLockEnabled = true, appLockPin = pinInput))
                        showPinDialog = false
                        pinInput = ""
                    }
                }) {
                    Text("Enable PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Gemini API Key
    if (showGeminiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showGeminiKeyDialog = false },
            title = { Text("Set Google Gemini API Key") },
            text = {
                Column {
                    Text(
                        "Stored encrypted in hardware KeyStore (AES-GCM).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = geminiKeyInput,
                        onValueChange = { geminiKeyInput = it },
                        label = { Text("Gemini API Key") },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gemini_key_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (geminiKeyInput.isNotBlank()) {
                            viewModel.saveGeminiApiKey(geminiKeyInput.trim())
                        }
                        showGeminiKeyDialog = false
                        geminiKeyInput = ""
                    }
                ) {
                    Text("Save Key")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGeminiKeyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: OpenAI API Key
    if (showOpenAiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showOpenAiKeyDialog = false },
            title = { Text("Set OpenAI API Key") },
            text = {
                Column {
                    Text(
                        "Stored encrypted in Android KeyStore.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = openAiKeyInput,
                        onValueChange = { openAiKeyInput = it },
                        label = { Text("OpenAI API Key") },
                        placeholder = { Text("sk-...") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("openai_key_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (openAiKeyInput.isNotBlank()) {
                            viewModel.saveOpenAiApiKey(openAiKeyInput.trim())
                        }
                        showOpenAiKeyDialog = false
                        openAiKeyInput = ""
                    }
                ) {
                    Text("Save Key")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOpenAiKeyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Reset Confirmation
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = { Text("Reset All Settings & Data?") },
            text = {
                Text("This will remove all stored API keys from Keystore, clear conversation context, delete reply history, and reset all filters to default.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllSettings()
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("Reset Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
