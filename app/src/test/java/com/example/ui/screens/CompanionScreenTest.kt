package com.example.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.data.model.AiModelTier
import com.example.data.model.UserProfile
import com.example.ui.theme.MindMateTheme
import com.example.ui.viewmodel.MindMateDashboardState
import com.example.voice.LiveConnectionState
import com.example.voice.VoiceUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CompanionScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun companionScreen_reportsHonestConnectionStatesAndInterruptControl() {
        var voiceState by mutableStateOf(VoiceUiState(connectionState = LiveConnectionState.DISCONNECTED))
        var dashState by mutableStateOf(
            MindMateDashboardState(
                isLoading = false,
                profile = UserProfile(voiceConsentGranted = true, reducedMotionEnabled = true)
            )
        )
        var interrupted = false
        var selectedTier: AiModelTier? = null

        composeRule.setContent {
            MindMateTheme {
                CompanionScreen(
                    state = dashState,
                    voiceState = voiceState,
                    onSelectMode = {},
                    onSelectModelTier = { selectedTier = it },
                    onSendMessage = {},
                    onRetryLastMessage = {},
                    onToggleLiveVoice = {},
                    onToggleDictate = {},
                    onInterruptAssistant = { interrupted = true },
                    onToggleMute = {},
                    onEndSession = {},
                    onGrantVoiceConsent = {},
                    onConsumePendingTranscript = {},
                    onDismissCrisis = {}
                )
            }
        }

        // Disconnected state must be honest: never pretends Live is connected
        composeRule.onNodeWithTag("companion_connection_status")
            .assertIsDisplayed()
            .assertTextContains("Voice Offline", substring = true)

        // Amplitude visualizer should not show when idle/not listening/not speaking
        composeRule.onNodeWithTag("audio_amplitude_visualizer").assertDoesNotExist()

        // Selecting Carbon model tier chip forwards AiModelTier.CARBON
        composeRule.onNodeWithTag("model_tier_carbon").performClick()
        assertEquals(AiModelTier.CARBON, selectedTier)

        // Transition to genuine CONNECTED + SPEAKING state
        composeRule.runOnIdle {
            voiceState = VoiceUiState(
                connectionState = LiveConnectionState.CONNECTED,
                isSpeaking = true,
                canInterrupt = true,
                hasRealAudioSignal = true,
                audioAmplitude = 0.6f,
                waveformHistory = List(VoiceUiState.WAVEFORM_BAR_COUNT) { 0.5f }
            )
        }

        composeRule.onNodeWithTag("companion_connection_status")
            .assertTextContains("Live Connected • gemini-3.8-live", substring = true)
        composeRule.onNodeWithTag("audio_amplitude_visualizer").assertIsDisplayed()
        composeRule.onNodeWithTag("companion_interrupt_button")
            .assertIsDisplayed()
            .performClick()
        assertTrue(interrupted)

        // Processing / loading bubble appears immediately when a message is sent and awaiting response
        composeRule.runOnIdle {
            dashState = dashState.copy(orbState = com.example.data.model.OrbState.THINKING, streamingReply = "")
        }
        composeRule.onNodeWithTag("companion_processing_indicator").assertIsDisplayed()

        // Error state reports connection error honestly
        composeRule.runOnIdle {
            dashState = dashState.copy(orbState = com.example.data.model.OrbState.IDLE)
            voiceState = VoiceUiState(
                connectionState = LiveConnectionState.ERROR,
                errorMessage = "WebSocket handshake failed"
            )
        }
        composeRule.onNodeWithTag("companion_connection_status")
            .assertTextContains("Error", substring = true)
        composeRule.onNodeWithTag("companion_voice_error")
            .assertIsDisplayed()
            .assertTextContains("WebSocket handshake failed", substring = true)
    }
}
