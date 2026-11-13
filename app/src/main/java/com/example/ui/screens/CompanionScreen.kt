package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.model.AiModelTier
import com.example.data.model.CompanionMode
import com.example.data.model.OrbState
import com.example.integrations.AgentScheduledAction
import com.example.ui.components.AgentScheduledActionCard
import com.example.ui.components.CompanionOrb
import com.example.ui.components.ConnectedAppsSheet
import com.example.ui.components.CrisisSupportBanner
import com.example.ui.components.MindMateAmbientBackground
import com.example.ui.theme.CalmMint
import com.example.ui.theme.DeepIndigoSurface
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.ElevatedCardSurface
import com.example.ui.theme.ErrorCoral
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassSpecularRim
import com.example.ui.theme.GlassSurface
import com.example.ui.theme.LuminousViolet
import com.example.ui.theme.SoftLavender
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidMidnight
import com.example.ui.theme.WarmRose
import com.example.ui.theme.eyebrow
import com.example.ui.theme.tabular
import com.example.ui.viewmodel.MindMateDashboardState
import com.example.voice.LiveConnectionState
import com.example.voice.VoiceUiState
import java.util.Locale

private enum class PendingMicAction {
    NONE,
    LIVE_VOICE,
    DICTATE_TRANSCRIBE
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompanionScreen(
    state: MindMateDashboardState,
    voiceState: VoiceUiState,
    onSelectMode: (CompanionMode) -> Unit,
    onSelectModelTier: (AiModelTier?) -> Unit,
    onSendMessage: (String) -> Unit,
    onRetryLastMessage: () -> Unit,
    onToggleLiveVoice: (Boolean) -> Unit,
    onToggleDictate: (Boolean) -> Unit,
    onInterruptAssistant: () -> Unit,
    onToggleMute: () -> Unit,
    onEndSession: () -> Unit,
    onGrantVoiceConsent: (Boolean) -> Unit,
    onConsumePendingTranscript: () -> Unit,
    onDismissCrisis: () -> Unit,
    onClearVoiceError: () -> Unit = {},
    onToggleConnectedApp: (String, Boolean) -> Unit = { _, _ -> },
    onRefreshCalendar: () -> Unit = {},
    onExecuteScheduledAction: (AgentScheduledAction) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var pendingMicAction by remember { mutableStateOf(PendingMicAction.NONE) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        when (pendingMicAction) {
            PendingMicAction.LIVE_VOICE -> onToggleLiveVoice(granted)
            PendingMicAction.DICTATE_TRANSCRIBE -> onToggleDictate(granted)
            PendingMicAction.NONE -> Unit
        }
        pendingMicAction = PendingMicAction.NONE
    }

    var inputText by remember { mutableStateOf("") }
    var showHistory by remember { mutableStateOf(true) }
    var showAppsSheet by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(state.pendingTranscript) {
        val transcript = state.pendingTranscript
        if (!transcript.isNullOrBlank()) {
            inputText = if (inputText.isBlank()) transcript else "${inputText.trim()} $transcript"
            onConsumePendingTranscript()
        }
    }

    val effectiveOrbState = when {
        voiceState.connectionState == LiveConnectionState.ERROR ||
            (state.orbState == OrbState.ERROR && !voiceState.connectionState.isLiveConnected) -> OrbState.ERROR
        voiceState.isListening || voiceState.isDictating -> OrbState.LISTENING
        voiceState.isSpeaking || state.orbState == OrbState.SPEAKING -> OrbState.SPEAKING
        voiceState.connectionState == LiveConnectionState.CONNECTING ||
            voiceState.connectionState == LiveConnectionState.RECONNECTING ||
            voiceState.isTranscribing ||
            state.orbState == OrbState.THINKING -> OrbState.THINKING
        else -> OrbState.IDLE
    }

    val ambientColor by animateColorAsState(
        targetValue = when (effectiveOrbState) {
            OrbState.LISTENING -> CalmMint
            OrbState.SPEAKING -> ElectricIndigo
            OrbState.THINKING -> SoftLavender
            OrbState.ERROR -> ErrorCoral
            OrbState.IDLE -> LuminousViolet
        },
        animationSpec = tween(420),
        label = "companion_ambient"
    )

    val visibleMessages = remember(state.messages) {
        state.messages.filterNot { it.id.startsWith("sample_") }
    }

    val isThinkingWaiting = state.orbState == OrbState.THINKING && state.streamingReply.isBlank()

    LaunchedEffect(visibleMessages.size, state.streamingReply, isThinkingWaiting) {
        val totalItems = visibleMessages.size +
            (if (state.streamingReply.isNotBlank() || isThinkingWaiting) 1 else 0)
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    val elapsedFormatted = remember(state.conversationElapsedSeconds) {
        val m = state.conversationElapsedSeconds / 60
        val s = state.conversationElapsedSeconds % 60
        String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }

    fun submitInput() {
        val toSend = inputText.trim()
        if (toSend.isNotEmpty() && state.orbState != OrbState.THINKING && !voiceState.isTranscribing) {
            inputText = ""
            onClearVoiceError()
            onSendMessage(toSend)
        }
    }

    fun triggerMicAction(action: PendingMicAction) {
        onClearVoiceError()
        if (state.profile?.voiceConsentGranted != true) {
            onGrantVoiceConsent(hasMicPermission)
        }
        if (!hasMicPermission) {
            pendingMicAction = action
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else {
            when (action) {
                PendingMicAction.LIVE_VOICE -> onToggleLiveVoice(true)
                PendingMicAction.DICTATE_TRANSCRIBE -> onToggleDictate(true)
                PendingMicAction.NONE -> Unit
            }
        }
    }

    MindMateAmbientBackground(
        accentTop = ambientColor,
        accentSide = if (effectiveOrbState == OrbState.SPEAKING) WarmRose else ElectricIndigo,
        modifier = modifier
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .align(Alignment.TopCenter)
        ) {
            val isCompactHeight = maxHeight < 640.dp
            val isVoiceEngaged = voiceState.connectionState.isLiveConnected ||
                voiceState.isListening ||
                voiceState.isDictating ||
                voiceState.isSpeaking ||
                !showHistory
            val targetOrbSize = when {
                isCompactHeight -> 76.dp
                isVoiceEngaged -> 156.dp
                visibleMessages.isNotEmpty() || isThinkingWaiting -> 92.dp
                else -> 148.dp
            }
            val animatedOrbSize by animateDpAsState(
                targetValue = targetOrbSize,
                animationSpec = tween(300),
                label = "orb_stage_size"
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = if (isCompactHeight) 8.dp else 10.dp)
            ) {
                // Clean Header Bar (no truncated text or clutter icons)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "MindMate",
                                style = MaterialTheme.typography.headlineMedium,
                                color = TextPrimary
                            )
                            if (voiceState.connectionState.isLiveConnected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(100.dp),
                                    color = CalmMint.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, CalmMint.copy(alpha = 0.35f))
                                ) {
                                    Text(
                                        text = elapsedFormatted,
                                        style = MaterialTheme.typography.labelMedium.tabular(),
                                        color = CalmMint,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        val (connDotColor, connTextColor, connLabel) = when (voiceState.connectionState) {
                            LiveConnectionState.CONNECTED -> Triple(
                                CalmMint,
                                CalmMint,
                                "Live Connected • gemini-3.8-live"
                            )
                            LiveConnectionState.CONNECTING -> Triple(
                                ElectricIndigo,
                                SoftLavender,
                                "Connecting Live Voice..."
                            )
                            LiveConnectionState.RECONNECTING -> Triple(
                                WarmRose,
                                WarmRose,
                                "Reconnecting Live..."
                            )
                            LiveConnectionState.ERROR -> Triple(
                                ErrorCoral,
                                ErrorCoral,
                                "Voice Error • Text Ready"
                            )
                            LiveConnectionState.DISCONNECTED -> Triple(
                                if (state.orbState == OrbState.THINKING) SoftLavender else TextMuted,
                                if (state.orbState == OrbState.THINKING) SoftLavender else TextSecondary,
                                if (state.orbState == OrbState.THINKING) {
                                    "Processing with ${state.effectiveModelTier.label}..."
                                } else {
                                    "Voice Offline • ${state.effectiveModelTier.label}"
                                }
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(connDotColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = connLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = connTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("companion_connection_status")
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { showHistory = !showHistory },
                            modifier = Modifier.testTag("companion_toggle_history")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.History,
                                contentDescription = "Toggle conversation history",
                                tint = if (showHistory) LuminousViolet else TextMuted
                            )
                        }
                        IconButton(
                            onClick = onToggleMute,
                            modifier = Modifier.testTag("companion_mute_button")
                        ) {
                            Icon(
                                imageVector = if (voiceState.isMuted) {
                                    Icons.AutoMirrored.Rounded.VolumeOff
                                } else {
                                    Icons.AutoMirrored.Rounded.VolumeUp
                                },
                                contentDescription = if (voiceState.isMuted) {
                                    "Unmute speech"
                                } else {
                                    "Mute speech"
                                },
                                tint = if (voiceState.isMuted) ErrorCoral else SoftLavender
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Mode Selector Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CompanionMode.entries.forEach { mode ->
                        val selected = state.companionMode == mode
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectMode(mode) },
                            label = { Text(mode.label, style = MaterialTheme.typography.labelMedium) },
                            shape = RoundedCornerShape(14.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LuminousViolet.copy(alpha = 0.25f),
                                selectedLabelColor = TextPrimary,
                                containerColor = DeepIndigoSurface,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = GlassBorder,
                                selectedBorderColor = LuminousViolet
                            ),
                            modifier = Modifier.testTag("companion_mode_${mode.id}")
                        )
                    }
                }

                // Compact Role-Based Model Selector Strip (No decorative sparkle icons)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isAuto = state.selectedModelTier == null
                    FilterChip(
                        selected = isAuto,
                        onClick = { onSelectModelTier(null) },
                        label = {
                            Text(
                                text = "Auto",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        shape = RoundedCornerShape(100.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricIndigo.copy(alpha = 0.25f),
                            selectedLabelColor = SoftLavender,
                            containerColor = GlassSurface,
                            labelColor = TextMuted
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isAuto,
                            borderColor = GlassBorder,
                            selectedBorderColor = ElectricIndigo
                        ),
                        modifier = Modifier.testTag("model_tier_auto")
                    )

                    AiModelTier.entries.forEach { tier ->
                        val selected = state.selectedModelTier == tier
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectModelTier(tier) },
                            label = {
                                Text(
                                    text = tier.label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            shape = RoundedCornerShape(100.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = LuminousViolet.copy(alpha = 0.22f),
                                selectedLabelColor = TextPrimary,
                                containerColor = GlassSurface,
                                labelColor = TextMuted
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = GlassBorder,
                                selectedBorderColor = LuminousViolet
                            ),
                            modifier = Modifier.testTag("model_tier_${tier.name.lowercase()}")
                        )
                    }
                }

                // Voice & Privacy Consent Banner
                AnimatedVisibility(visible = state.profile?.voiceConsentGranted == false) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DeepIndigoSurface,
                        border = BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Voice & Privacy Consent",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SoftLavender
                                )
                                Text(
                                    text = "Audio streams live without storing raw recordings.",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onGrantVoiceConsent(hasMicPermission) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CalmMint,
                                    contentColor = VoidMidnight
                                ),
                                modifier = Modifier.testTag("grant_voice_consent_button")
                            ) {
                                Text("Allow", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = state.crisisBannerVisible) {
                    CrisisSupportBanner(
                        onDismiss = onDismissCrisis,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                // Central Siri-Style 3D Orb Stage & Live Voice Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CompanionOrb(
                        orbState = effectiveOrbState,
                        audioAmplitude = voiceState.audioAmplitude,
                        reducedMotion = state.profile?.reducedMotionEnabled == true,
                        interactive = true,
                        size = animatedOrbSize
                    )

                    // Show compact live status only during active voice transcription/dictation
                    AnimatedVisibility(
                        visible = voiceState.isTranscribing || voiceState.isDictating
                    ) {
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = GlassSurface,
                            border = BorderStroke(1.dp, ambientColor.copy(alpha = 0.35f)),
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Text(
                                text = if (voiceState.isTranscribing) {
                                    "Transcribing speech..."
                                } else {
                                    "Listening — tap Stop when finished"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = SoftLavender,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Real Audio Amplitude Waveform Bars (when mic/Live audio data is present)
                    AnimatedVisibility(
                        visible = voiceState.isListening || voiceState.isDictating || voiceState.isSpeaking
                    ) {
                        RealAudioAmplitudeVisualizer(
                            samples = voiceState.waveformHistory,
                            hasRealSignal = voiceState.hasRealAudioSignal,
                            activeColor = if (voiceState.isSpeaking) ElectricIndigo else CalmMint,
                            modifier = Modifier
                                .padding(top = 4.dp, bottom = 2.dp)
                                .testTag("audio_amplitude_visualizer")
                        )
                    }

                    // Clean Live Call & Interrupt Strip (No clutter icons)
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isLiveBusy = voiceState.connectionState == LiveConnectionState.CONNECTING ||
                            voiceState.connectionState == LiveConnectionState.RECONNECTING
                        val isLiveConnected = voiceState.connectionState.isLiveConnected

                        OutlinedButton(
                            onClick = {
                                if (isLiveConnected || isLiveBusy) {
                                    onEndSession()
                                } else {
                                    triggerMicAction(PendingMicAction.LIVE_VOICE)
                                }
                            },
                            shape = RoundedCornerShape(100.dp),
                            border = BorderStroke(
                                1.dp,
                                when {
                                    isLiveConnected -> ErrorCoral.copy(alpha = 0.65f)
                                    isLiveBusy -> ElectricIndigo.copy(alpha = 0.65f)
                                    else -> CalmMint.copy(alpha = 0.55f)
                                }
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                            modifier = Modifier
                                .heightIn(min = 34.dp)
                                .testTag("companion_live_voice_button")
                        ) {
                            if (isLiveBusy) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = SoftLavender,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = when {
                                    isLiveConnected -> "End Live"
                                    isLiveBusy -> "Cancel"
                                    else -> "Live Voice"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isLiveConnected) ErrorCoral else CalmMint
                            )
                        }

                        if (isLiveConnected) {
                            OutlinedButton(
                                onClick = onToggleMute,
                                shape = RoundedCornerShape(100.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (voiceState.isMuted) ErrorCoral.copy(alpha = 0.6f) else GlassBorder
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .heightIn(min = 34.dp)
                                    .testTag("live_mic_mute_pill")
                            ) {
                                Text(
                                    text = if (voiceState.isMuted) "Muted" else "Mic On",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (voiceState.isMuted) ErrorCoral else TextPrimary
                                )
                            }
                        }

                        val enabledAppsCount = state.connectedApps.count { it.isEnabled }
                        OutlinedButton(
                            onClick = { showAppsSheet = true },
                            shape = RoundedCornerShape(100.dp),
                            border = BorderStroke(1.dp, ElectricIndigo.copy(alpha = 0.55f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .heightIn(min = 34.dp)
                                .testTag("companion_apps_button")
                        ) {
                            Text(
                                text = if (enabledAppsCount > 0) "Apps ($enabledAppsCount)" else "Apps",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = SoftLavender
                            )
                        }

                        if (voiceState.canInterrupt || state.orbState == OrbState.SPEAKING) {
                            Button(
                                onClick = onInterruptAssistant,
                                shape = RoundedCornerShape(100.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WarmRose.copy(alpha = 0.22f),
                                    contentColor = WarmRose
                                ),
                                border = BorderStroke(1.dp, WarmRose.copy(alpha = 0.55f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .heightIn(min = 34.dp)
                                    .testTag("companion_interrupt_button")
                            ) {
                                Text(
                                    text = "Interrupt",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Live Real-Time Input & Output Transcriptions
                    if (voiceState.liveInputTranscript.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "You: \"${voiceState.liveInputTranscript}\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = CalmMint,
                            textAlign = TextAlign.Center
                        )
                    }
                    if (voiceState.liveOutputTranscript.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "MindMate: \"${voiceState.liveOutputTranscript}\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = SoftLavender,
                            textAlign = TextAlign.Center
                        )
                    }
                    if (voiceState.errorMessage != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = voiceState.errorMessage,
                            style = MaterialTheme.typography.labelMedium,
                            color = ErrorCoral,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("companion_voice_error")
                        )
                    }
                }

                // Inline AI Error Card with Retry
                AnimatedVisibility(visible = state.companionError != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DeepIndigoSurface,
                        border = BorderStroke(1.dp, ErrorCoral.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.companionError.orEmpty(),
                                style = MaterialTheme.typography.labelMedium,
                                color = ErrorCoral,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = onRetryLastMessage,
                                modifier = Modifier.testTag("companion_retry_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = null,
                                    tint = CalmMint,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Retry", color = CalmMint)
                            }
                        }
                    }
                }

                // Spacious Conversation Stream
                if (showHistory) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (visibleMessages.isEmpty() && state.streamingReply.isBlank() && !isThinkingWaiting) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = GlassSurface,
                                    border = BorderStroke(1.dp, GlassBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = "A calm space whenever you're ready",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Send a message below, tap the mic to dictate, or start Live Voice for real-time spoken conversation.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        items(visibleMessages, key = { it.id }) { msg ->
                            val isUser = msg.role == "user"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(
                                        topStart = 20.dp,
                                        topEnd = 20.dp,
                                        bottomStart = if (isUser) 20.dp else 6.dp,
                                        bottomEnd = if (isUser) 6.dp else 20.dp
                                    ),
                                    color = if (isUser) {
                                        LuminousViolet.copy(alpha = 0.22f)
                                    } else {
                                        ElevatedCardSurface
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (isUser) LuminousViolet.copy(alpha = 0.48f) else GlassBorder
                                    ),
                                    modifier = Modifier.widthIn(max = 328.dp)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                                        Text(
                                            text = if (isUser) "YOU" else "MINDMATE",
                                            style = MaterialTheme.typography.labelSmall.eyebrow(),
                                            color = if (isUser) SoftLavender else CalmMint
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = msg.content,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary
                                        )
                                        if (msg.scheduledActions.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                msg.scheduledActions.forEach { action ->
                                                    AgentScheduledActionCard(
                                                        action = action,
                                                        onExecute = onExecuteScheduledAction
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (isThinkingWaiting) {
                            item(key = "thinking_bubble") {
                                ThinkingIndicatorBubble(
                                    modelLabel = state.effectiveModelTier.label,
                                    modifier = Modifier.testTag("companion_processing_indicator")
                                )
                            }
                        }

                        if (state.streamingReply.isNotBlank()) {
                            item(key = "streaming_bubble") {
                                Surface(
                                    shape = RoundedCornerShape(
                                        topStart = 20.dp,
                                        topEnd = 20.dp,
                                        bottomStart = 6.dp,
                                        bottomEnd = 20.dp
                                    ),
                                    color = ElevatedCardSurface,
                                    border = BorderStroke(1.dp, CalmMint.copy(alpha = 0.5f)),
                                    modifier = Modifier.widthIn(max = 328.dp)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp)) {
                                        Text(
                                            text = "MINDMATE",
                                            style = MaterialTheme.typography.labelSmall.eyebrow(),
                                            color = CalmMint
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = state.streamingReply,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Quick Conversation Starter Strip (when conversation is empty and idle)
                if (visibleMessages.isEmpty() && !isThinkingWaiting && !voiceState.connectionState.isLiveConnected && !isCompactHeight) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "Schedule my day using my connected apps.",
                            "I had a long day and want to unwind.",
                            "Guide me through a 2-minute breath."
                        ).forEach { prompt ->
                            OutlinedButton(
                                onClick = { onSendMessage(prompt) },
                                enabled = state.orbState != OrbState.THINKING,
                                shape = RoundedCornerShape(100.dp),
                                border = BorderStroke(1.dp, LuminousViolet.copy(alpha = 0.4f)),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = prompt,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SoftLavender
                                )
                            }
                        }
                    }
                }

                // Bottom Composer Dock (Clean Mic + Input + Processing/Send Button)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = when {
                            voiceState.isDictating -> CalmMint
                            voiceState.isTranscribing -> ElectricIndigo
                            else -> LuminousViolet
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .shadow(
                                elevation = if (voiceState.isDictating) 12.dp else 4.dp,
                                shape = CircleShape,
                                ambientColor = (if (voiceState.isDictating) CalmMint else LuminousViolet).copy(alpha = 0.45f)
                            )
                            .border(1.dp, GlassSpecularRim.copy(alpha = 0.4f), CircleShape)
                    ) {
                        IconButton(
                            onClick = { triggerMicAction(PendingMicAction.DICTATE_TRANSCRIBE) },
                            enabled = !voiceState.isTranscribing,
                            modifier = Modifier.testTag("companion_mic_button")
                        ) {
                            when {
                                voiceState.isTranscribing -> {
                                    CircularProgressIndicator(
                                        strokeWidth = 2.dp,
                                        color = VoidMidnight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                voiceState.isDictating -> {
                                    Icon(
                                        imageVector = Icons.Rounded.Stop,
                                        contentDescription = "Stop recording and transcribe audio",
                                        tint = VoidMidnight
                                    )
                                }
                                else -> {
                                    Icon(
                                        imageVector = Icons.Rounded.Mic,
                                        contentDescription = "Record speech to transcribe",
                                        tint = VoidMidnight
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = {
                            inputText = it
                            if (voiceState.errorMessage != null) {
                                onClearVoiceError()
                            }
                        },
                        placeholder = {
                            Text(
                                text = when {
                                    voiceState.isDictating -> "Listening... tap Stop to transcribe"
                                    voiceState.isTranscribing -> "Transcribing audio..."
                                    state.orbState == OrbState.THINKING -> "MindMate is thinking..."
                                    else -> "Message MindMate..."
                                },
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { submitInput() }),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LuminousViolet,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = DeepIndigoSurface,
                            unfocusedContainerColor = DeepIndigoSurface,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("companion_input")
                    )

                    val isSendingOrThinking = state.orbState == OrbState.THINKING
                    Surface(
                        shape = CircleShape,
                        color = when {
                            isSendingOrThinking -> ElectricIndigo.copy(alpha = 0.35f)
                            inputText.isNotBlank() -> ElectricIndigo
                            else -> ElevatedCardSurface
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .border(
                                1.dp,
                                if (inputText.isNotBlank() || isSendingOrThinking) {
                                    SoftLavender.copy(alpha = 0.45f)
                                } else {
                                    GlassBorder
                                },
                                CircleShape
                            )
                    ) {
                        IconButton(
                            onClick = { submitInput() },
                            enabled = inputText.isNotBlank() &&
                                !isSendingOrThinking &&
                                !voiceState.isTranscribing,
                            modifier = Modifier.testTag("companion_send_button")
                        ) {
                            if (isSendingOrThinking) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    color = SoftLavender,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Send,
                                    contentDescription = "Send message",
                                    tint = if (inputText.isNotBlank()) VoidMidnight else TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThinkingIndicatorBubble(
    modelLabel: String,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "thinking_dots")
    val dotScales = List(3) { index ->
        transition.animateFloat(
            initialValue = 0.55f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 520, delayMillis = index * 140),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot_scale_$index"
        ).value
    }
    val dotAlphas = List(3) { index ->
        transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 520, delayMillis = index * 140),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot_alpha_$index"
        ).value
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 6.dp, bottomEnd = 20.dp),
            color = ElevatedCardSurface,
            border = BorderStroke(1.dp, LuminousViolet.copy(alpha = 0.42f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    dotScales.forEachIndexed { idx, scale ->
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .scale(scale)
                                .alpha(dotAlphas[idx])
                                .clip(CircleShape)
                                .background(CalmMint)
                        )
                    }
                }
                Text(
                    text = "$modelLabel is thinking...",
                    style = MaterialTheme.typography.bodySmall,
                    color = SoftLavender
                )
            }
        }
    }
}

@Composable
private fun RealAudioAmplitudeVisualizer(
    samples: List<Float>,
    hasRealSignal: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(100.dp))
            .background(GlassSurface)
            .border(1.dp, GlassBorder, RoundedCornerShape(100.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        samples.forEach { rawSample ->
            val clamped = if (hasRealSignal) rawSample.coerceIn(0f, 1f) else 0f
            val barHeight by animateDpAsState(
                targetValue = (4f + clamped * 22f).dp,
                animationSpec = tween(durationMillis = 60),
                label = "amp_bar"
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(100.dp))
                    .background(
                        if (clamped > 0.08f) activeColor else TextMuted.copy(alpha = 0.45f)
                    )
            )
        }
    }
}
