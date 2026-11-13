package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UserProfile
import com.example.ui.components.GlassCard
import com.example.ui.components.MindMateAmbientBackground
import com.example.ui.theme.CalmMint
import com.example.ui.theme.DeepIndigoSurface
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.ElevatedCardSurface
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.LuminousViolet
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidMidnight
import com.example.ui.theme.eyebrow
import com.example.ui.theme.tabular
import com.example.ui.viewmodel.MindMateDashboardState

@Composable
fun ProfileScreen(
    state: MindMateDashboardState,
    onSaveProfile: (UserProfile) -> Unit,
    onClearHistory: () -> Unit,
    onExportJson: () -> String,
    onDeleteAccountData: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profile = state.profile ?: UserProfile()

    var displayName by remember(profile.displayName) { mutableStateOf(profile.displayName) }
    var wellnessGoal by remember(profile.wellnessGoal) { mutableStateOf(profile.wellnessGoal) }
    var dietaryPref by remember(profile.dietaryPreference) { mutableStateOf(profile.dietaryPreference) }
    var activityPref by remember(profile.activityPreference) { mutableStateOf(profile.activityPreference) }
    var indoorOutdoorPref by remember(profile.indoorOutdoorPreference) { mutableStateOf(profile.indoorOutdoorPreference) }
    var hydrationGoal by remember(profile.hydrationGoalMl) { mutableLongStateOf(profile.hydrationGoalMl) }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var exportedPreview by remember { mutableStateOf<String?>(null) }

    MindMateAmbientBackground(
        accentTop = LuminousViolet,
        accentSide = ElectricIndigo,
        modifier = modifier
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Hero Identity Card
            item {
                GlassCard(accentGlow = LuminousViolet) {
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
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(LuminousViolet.copy(alpha = 0.35f), ElectricIndigo.copy(alpha = 0.25f))
                                        )
                                    )
                                    .border(
                                        1.5.dp,
                                        Brush.linearGradient(listOf(LuminousViolet, CalmMint.copy(alpha = 0.7f))),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = profile.displayName.ifBlank { "F" }.take(1).uppercase(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "PERSONAL SPACE",
                                    style = MaterialTheme.typography.labelMedium.eyebrow(),
                                    color = CalmMint
                                )
                                Text(
                                    text = profile.displayName.ifBlank { "Friend" },
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = profile.email.ifBlank { "Signed in with Google" },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SoftLavender
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = onSignOut,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier.testTag("sign_out_button")
                        ) {
                            Text("Sign Out", color = TextPrimary)
                        }
                    }
                }
            }

            // Personal Details & Goals
            item {
                GlassCard {
                    Text(
                        text = "IDENTITY & WELLNESS GOAL",
                        style = MaterialTheme.typography.labelMedium.eyebrow(),
                        color = SoftLavender
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Personal Details", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = profileFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = wellnessGoal,
                        onValueChange = { wellnessGoal = it },
                        label = { Text("Primary Wellness Goal") },
                        maxLines = 2,
                        shape = RoundedCornerShape(16.dp),
                        colors = profileFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Daily Hydration Target", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1500L, 2000L, 2500L, 3000L).forEach { ml ->
                            val selected = hydrationGoal == ml
                            FilterChip(
                                selected = selected,
                                onClick = { hydrationGoal = ml },
                                label = { Text("${ml} ml", style = MaterialTheme.typography.labelLarge.tabular()) },
                                shape = RoundedCornerShape(14.dp),
                                colors = profileChipColors(),
                                border = profileChipBorder(selected)
                            )
                        }
                    }
                }
            }

            // Dietary & Activity Preferences
            item {
                GlassCard {
                    Text(
                        text = "PERSONALIZATION",
                        style = MaterialTheme.typography.labelMedium.eyebrow(),
                        color = SoftLavender
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Dietary & Activity Style", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Dietary style", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "balanced" to "Balanced",
                            "indian_home" to "Indian Home",
                            "vegetarian" to "Vegetarian",
                            "vegan" to "Vegan",
                            "high_protein" to "High Protein"
                        ).forEach { (id, label) ->
                            val selected = dietaryPref == id
                            FilterChip(
                                selected = selected,
                                onClick = { dietaryPref = id },
                                label = { Text(label) },
                                shape = RoundedCornerShape(14.dp),
                                colors = profileChipColors(),
                                border = profileChipBorder(selected)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Activity style", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "gentle" to "Gentle",
                            "moderate" to "Moderate",
                            "energetic" to "Energetic",
                            "outdoor_first" to "Outdoor First"
                        ).forEach { (id, label) ->
                            val selected = activityPref == id
                            FilterChip(
                                selected = selected,
                                onClick = { activityPref = id },
                                label = { Text(label) },
                                shape = RoundedCornerShape(14.dp),
                                colors = profileChipColors(),
                                border = profileChipBorder(selected)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Environment", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("both" to "Both", "indoor" to "Indoor", "outdoor" to "Outdoor").forEach { (id, label) ->
                            val selected = indoorOutdoorPref == id
                            FilterChip(
                                selected = selected,
                                onClick = { indoorOutdoorPref = id },
                                label = { Text(label) },
                                shape = RoundedCornerShape(14.dp),
                                colors = profileChipColors(),
                                border = profileChipBorder(selected)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            onSaveProfile(
                                profile.copy(
                                    displayName = displayName,
                                    wellnessGoal = wellnessGoal,
                                    dietaryPreference = dietaryPref,
                                    activityPreference = activityPref,
                                    indoorOutdoorPreference = indoorOutdoorPref,
                                    hydrationGoalMl = hydrationGoal
                                )
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LuminousViolet,
                            contentColor = VoidMidnight
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_profile_preferences_button")
                    ) {
                        Text("Save Preferences", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // Voice, Memory, Accessibility & Privacy Controls
            item {
                GlassCard(accentGlow = CalmMint) {
                    Text(
                        text = "PRIVACY & CONSENT",
                        style = MaterialTheme.typography.labelMedium.eyebrow(),
                        color = CalmMint
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Voice, Memory & Comfort", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Spacer(modifier = Modifier.height(10.dp))

                    SettingToggleRow(
                        title = "Voice Interaction Consent",
                        subtitle = "Allow microphone speech input during companion sessions.",
                        checked = profile.voiceConsentGranted,
                        onCheckedChange = { onSaveProfile(profile.copy(voiceConsentGranted = it)) }
                    )
                    SettingToggleRow(
                        title = "Consent-Based Memory",
                        subtitle = "Let MindMate reference your goal and latest mood in replies.",
                        checked = profile.memoryConsentGranted,
                        onCheckedChange = { onSaveProfile(profile.copy(memoryConsentGranted = it)) }
                    )
                    SettingToggleRow(
                        title = "Save Conversation History",
                        subtitle = "Store companion messages in your private account.",
                        checked = profile.conversationHistoryEnabled,
                        onCheckedChange = { onSaveProfile(profile.copy(conversationHistoryEnabled = it)) }
                    )
                    SettingToggleRow(
                        title = "Spoken AI Replies (TTS)",
                        subtitle = "Read MindMate companion replies aloud when unmuted.",
                        checked = profile.voiceSpeechEnabled,
                        onCheckedChange = { onSaveProfile(profile.copy(voiceSpeechEnabled = it)) }
                    )
                    SettingToggleRow(
                        title = "Gentle Wellness Reminders",
                        subtitle = "Show supportive daily hydration and break nudges.",
                        checked = profile.notificationsEnabled,
                        onCheckedChange = { onSaveProfile(profile.copy(notificationsEnabled = it)) }
                    )
                    SettingToggleRow(
                        title = "Reduced Motion",
                        subtitle = "Minimize 3D orb motion and wave animations.",
                        checked = profile.reducedMotionEnabled,
                        onCheckedChange = { onSaveProfile(profile.copy(reducedMotionEnabled = it)) }
                    )
                }
            }

            // Data Management & Deletion
            item {
                GlassCard {
                    Text("Your Data Control", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { exportedPreview = onExportJson() },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_data_button")
                        ) {
                            Text("Export Data", color = SoftLavender)
                        }
                        OutlinedButton(
                            onClick = onClearHistory,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, GlassBorder),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("clear_history_button")
                        ) {
                            Text("Clear Chat", color = TextSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ErrorCoral.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_account_data_button")
                    ) {
                        Text("Delete All Personal Data", color = ErrorCoral)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(14.dp)) }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = DeepIndigoSurface,
            title = { Text("Delete all MindMate data?", color = TextPrimary) },
            text = {
                Text(
                    "This permanently deletes your profile, mood logs, journal entries, activity history, and conversations from Firestore, then signs you out.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccountData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorCoral, contentColor = VoidMidnight)
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (exportedPreview != null) {
        AlertDialog(
            onDismissRequest = { exportedPreview = null },
            containerColor = DeepIndigoSurface,
            title = { Text("MindMate JSON Export", color = TextPrimary) },
            text = {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = VoidMidnight,
                    modifier = Modifier.height(220.dp)
                ) {
                    LazyColumn(modifier = Modifier.padding(12.dp)) {
                        item {
                            Text(
                                text = exportedPreview.orEmpty(),
                                style = MaterialTheme.typography.labelMedium,
                                color = CalmMint
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            putExtra(Intent.EXTRA_TEXT, exportedPreview.orEmpty())
                            type = "application/json"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Export MindMate Data"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LuminousViolet, contentColor = VoidMidnight)
                ) {
                    Text("Share")
                }
            },
            dismissButton = {
                TextButton(onClick = { exportedPreview = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = TextMuted)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = VoidMidnight,
                checkedTrackColor = CalmMint,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = ElevatedCardSurface,
                uncheckedBorderColor = GlassBorder
            )
        )
    }
}

@Composable
private fun profileFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = LuminousViolet,
    unfocusedBorderColor = GlassBorder,
    focusedContainerColor = DeepIndigoSurface,
    unfocusedContainerColor = DeepIndigoSurface,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary
)

@Composable
private fun profileChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = LuminousViolet.copy(alpha = 0.25f),
    selectedLabelColor = TextPrimary,
    containerColor = DeepIndigoSurface,
    labelColor = TextSecondary
)

@Composable
private fun profileChipBorder(selected: Boolean) = FilterChipDefaults.filterChipBorder(
    enabled = true,
    selected = selected,
    borderColor = GlassBorder,
    selectedBorderColor = LuminousViolet
)
