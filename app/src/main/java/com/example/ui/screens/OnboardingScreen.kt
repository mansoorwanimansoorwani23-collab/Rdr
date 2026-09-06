package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class OnboardingStep(
    val stepNumber: Int,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val highlight: String
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onOpenNotificationSettings: () -> Unit
) {
    val steps = remember {
        listOf(
            OnboardingStep(
                1,
                "Welcome to ReplyMate Pro",
                "Your private, on-device AI assistant for smart WhatsApp replies. Keep your conversations flowing without getting distracted.",
                Icons.Default.AutoAwesome,
                "Instant, natural texting assistance"
            ),
            OnboardingStep(
                2,
                "Enable Notification Access",
                "ReplyMate securely listens to WhatsApp notifications to detect incoming messages and deliver quick replies without modifying WhatsApp.",
                Icons.Default.NotificationsActive,
                "Zero background modifications, 100% Android standard"
            ),
            OnboardingStep(
                3,
                "Configure AI Provider",
                "Use Google Gemini or OpenAI with your personal API key. Stored encrypted on-device via Android KeyStore (AES-GCM).",
                Icons.Default.Key,
                "Your keys never leave your device"
            ),
            OnboardingStep(
                4,
                "Choose Your Reply Personality",
                "Pick from Friendly, Casual, Professional, Funny, or direct styles. You can even train it with your own texting samples.",
                Icons.Default.Psychology,
                "Sounds like you, not an AI robot"
            ),
            OnboardingStep(
                5,
                "Safety & Private Control",
                "Financial OTPs and emergencies are protected automatically. Approval mode lets you review before sending, and an Emergency Kill Switch is one tap away.",
                Icons.Default.Security,
                "Total human control & safety"
            )
        )
    }

    var currentStepIndex by remember { mutableIntStateOf(0) }
    val step = steps[currentStepIndex]

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Step ${step.stepNumber} of ${steps.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = onFinish) {
                    Text("Skip")
                }
            }

            // Center Content
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = step.icon,
                            contentDescription = step.title,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = step.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "✨ ${step.highlight}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (step.stepNumber == 2) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = onOpenNotificationSettings,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Grant Notification Access")
                        }
                    }
                }
            }

            // Bottom Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStepIndex > 0) {
                    OutlinedButton(
                        onClick = { currentStepIndex-- },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Back")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        if (currentStepIndex < steps.size - 1) {
                            currentStepIndex++
                        } else {
                            onFinish()
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (currentStepIndex == steps.size - 1) {
                        Text("Get Started")
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.Check, contentDescription = "Done")
                    } else {
                        Text("Next")
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                    }
                }
            }
        }
    }
}
