package com.example.data.ai

import com.example.data.model.AiModelTier
import com.example.data.model.CompanionMessage
import com.example.data.model.CompanionMode
import com.google.firebase.ai.type.TextPart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MindMateAiServiceTest {

    private val service = MindMateAiService()

    @Test
    fun `buildSanitizedHistory strips sample messages and leading model turns`() {
        val raw = listOf(
            CompanionMessage(id = "sample_1", role = "companion", content = "Sample greeting"),
            CompanionMessage(id = "m1", role = "companion", content = "Orphaned model greeting"),
            CompanionMessage(id = "m2", role = "user", content = "Hi MindMate"),
            CompanionMessage(id = "m3", role = "companion", content = "Hello! How are you feeling today?")
        )

        val sanitized = service.buildSanitizedHistory(raw)
        assertEquals(2, sanitized.size)
        assertEquals("user", sanitized[0].role)
        assertEquals("model", sanitized[1].role)
        assertEquals("Hi MindMate", (sanitized[0].parts.first() as TextPart).text)
    }

    @Test
    fun `buildSanitizedHistory merges consecutive same-role turns and drops trailing user turn`() {
        val raw = listOf(
            CompanionMessage(id = "u1", role = "user", content = "First user line"),
            CompanionMessage(id = "u2", role = "user", content = "Second user line"),
            CompanionMessage(id = "a1", role = "companion", content = "Companion reply"),
            CompanionMessage(id = "u3", role = "user", content = "Pending user prompt")
        )

        val sanitized = service.buildSanitizedHistory(raw)
        assertEquals(2, sanitized.size)
        assertEquals("user", sanitized[0].role)
        assertEquals("First user line\nSecond user line", (sanitized[0].parts.first() as TextPart).text)
        assertEquals("model", sanitized[1].role)
        assertEquals("Companion reply", (sanitized[1].parts.first() as TextPart).text)
    }

    @Test
    fun `crisis safety guard detects distress phrases case-insensitively`() {
        assertTrue(CrisisSafetyGuard.containsCrisisLanguage("I feel like I want to die today"))
        assertFalse(CrisisSafetyGuard.containsCrisisLanguage("I feel a bit stressed about deadlines"))
    }

    @Test
    fun `gemini models match live, transcribe, and four chatbot tiers`() {
        assertEquals("gemini-3.8-live", MindMateAiService.MODEL_LIVE_VOICE)
        assertEquals("gemini-3.5-transcribe", MindMateAiService.MODEL_TRANSCRIBE)
        assertEquals("gemini-4-carbon", AiModelTier.CARBON.modelId)
        assertEquals("gemini-4-argon", AiModelTier.ARGON.modelId)
        assertEquals("gemini-4-hydrogen", AiModelTier.HYDROGEN.modelId)
        assertEquals("gemini-3.1-flash-lite", AiModelTier.FLASH_LITE.modelId)

        assertEquals(AiModelTier.HYDROGEN, CompanionMode.LISTEN.defaultModelTier)
        assertEquals(AiModelTier.CARBON, CompanionMode.EXPLORE.defaultModelTier)
        assertEquals(AiModelTier.ARGON, CompanionMode.SUPPORT.defaultModelTier)
        assertEquals(AiModelTier.HYDROGEN, CompanionMode.RELAX.defaultModelTier)
        assertEquals(AiModelTier.FLASH_LITE, CompanionMode.CASUAL.defaultModelTier)
    }
}
