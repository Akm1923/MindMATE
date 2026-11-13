package com.example.data.ai

import com.google.firebase.ai.type.content
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DirectGeminiApiClientTest {

    @Test
    fun `isValidApiKey rejects blank and placeholder values`() {
        assertFalse(DirectGeminiApiClient.isValidApiKey(""))
        assertFalse(DirectGeminiApiClient.isValidApiKey("   "))
        assertFalse(DirectGeminiApiClient.isValidApiKey("MY_GEMINI_API_KEY"))

        assertTrue(DirectGeminiApiClient.isValidApiKey("sample-valid-key-value"))
    }

    @Test
    fun `isConfigured respects injected key provider`() {
        val unconfigured = DirectGeminiApiClient { "MY_GEMINI_API_KEY" }
        assertFalse(unconfigured.isConfigured())

        val configured = DirectGeminiApiClient { "test-key-from-secrets" }
        assertTrue(configured.isConfigured())
    }

    @Test
    fun `buildGeneratePayload includes history and system instruction`() {
        val client = DirectGeminiApiClient { "test-key" }
        val history = listOf(
            content("user") { text("Hi") },
            content("model") { text("Hello! How can I help?") }
        )

        val payload = client.buildGeneratePayload(
            systemInstruction = "Be warm.",
            history = history,
            userPrompt = "Feeling tired today."
        )

        val contents = payload.getJSONArray("contents")
        assertEquals(3, contents.length())
        assertEquals("user", contents.getJSONObject(0).getString("role"))
        assertEquals("model", contents.getJSONObject(1).getString("role"))
        assertEquals("user", contents.getJSONObject(2).getString("role"))
        assertEquals(
            "Be warm.",
            payload.getJSONObject("systemInstruction").getJSONArray("parts").getJSONObject(0).getString("text")
        )
    }

    @Test
    fun `buildLiveSetupPayload sets audio modality and model path`() {
        val client = DirectGeminiApiClient { "test-key" }
        val setup = client.buildLiveSetupPayload("gemini-3.8-live", "Warm companion").getJSONObject("setup")

        assertEquals("models/gemini-3.8-live", setup.getString("model"))
        assertEquals(
            "AUDIO",
            setup.getJSONObject("generationConfig").getJSONArray("responseModalities").getString(0)
        )
        assertTrue(setup.has("inputAudioTranscription"))
        assertTrue(setup.has("outputAudioTranscription"))
    }
}
