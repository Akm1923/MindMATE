package com.example.data.ai

import android.util.Log
import com.example.data.model.AiModelTier
import com.example.data.model.CompanionMessage
import com.example.data.model.CompanionMode
import com.example.data.model.MoodLog
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.LiveGenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.AudioTranscriptionConfig
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.ContextWindowCompressionConfig
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.RealtimeInputConfig
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.SlidingWindow
import com.google.firebase.ai.type.SpeechConfig
import com.google.firebase.ai.type.Voice
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.liveGenerationConfig
import com.google.firebase.ai.type.realtimeInputConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

data class JournalReflectionResult(
    val summary: String,
    val reflectionQuestion: String
)

object CrisisSafetyGuard {
    private val crisisKeywords = listOf(
        "kill myself",
        "end my life",
        "suicide",
        "suicidal",
        "want to die",
        "better off dead",
        "hurt myself",
        "self harm",
        "self-harm",
        "no reason to live",
        "can't go on"
    )

    fun containsCrisisLanguage(text: String): Boolean {
        val lower = text.lowercase()
        return crisisKeywords.any { lower.contains(it) }
    }
}

class MindMateAiService(
    private val directClient: DirectGeminiApiClient = DirectGeminiApiClient()
) {

    private val chatModels = ConcurrentHashMap<AiModelTier, GenerativeModel>()

    fun hasCustomApiKey(): Boolean = directClient.isConfigured()

    private fun baseCompanionGuidelines(): String = """
        You are MindMate, a warm, emotionally considerate personal wellness companion and daily planning partner.
        CORE IDENTITY & SAFETY RULES:
        - You are NOT a therapist, psychiatrist, counselor, or medical diagnostic tool.
        - NEVER claim to diagnose, treat, or cure any mental health or medical condition.
        - Be natural, patient, non-judgmental, and empathetic. Keep responses concise (2-4 short paragraphs max) and conversational for mobile reading or listening.
        - Write clean, plain conversational sentences without emojis, markdown headings (###), asterisks (**), or decorative symbols.
        - Do not force wellness tips or checklists into every turn, especially in Listen or Casual mode.
        - When the user asks to schedule their day, add a calendar event, set an alarm/timer, save a task/note, draft an email, or launch one of their enabled connected apps, respond naturally AND append one or more structured action blocks on their own lines using this exact syntax:
          [ACTION]{"type":"calendar_event|set_alarm|set_timer|create_task|draft_email|open_app","title":"Short action title","detail":"Helpful note or email body","hour":14,"minute":30,"durationMinutes":30,"targetAppId":"google_calendar|google_tasks|gmail|google_keep|clock_alarms|todoist|notion|spotify|youtube_music|outlook|whatsapp|slack|google_drive|google_fit","recipient":""}[/ACTION]
          Only emit [ACTION] blocks when the user actually wants to schedule, remind, save, draft, or open an app.
        - If the user expresses self-harm, suicidal thoughts, or acute crisis, respond with warmth, encourage reaching out to a trusted person, local emergency services, or a crisis helpline (e.g., 988 in the US/Canada, 112/911 globally, or local crisis support), and do not act as a substitute for human care.
    """.trimIndent()

    private fun chatSystemInstruction(tier: AiModelTier): String = """
        ${baseCompanionGuidelines()}

        ${tier.roleInstruction}
    """.trimIndent()

    private fun getChatModel(tier: AiModelTier): GenerativeModel {
        return chatModels.getOrPut(tier) {
            Firebase.ai.generativeModel(
                modelName = tier.modelId,
                systemInstruction = content {
                    text(chatSystemInstruction(tier))
                }
            )
        }
    }

    private val reflectionModel by lazy {
        val reflectionSchema = Schema.obj(
            properties = mapOf(
                "summary" to Schema.string(
                    description = "A gentle, 1-2 sentence non-clinical summary of what the user shared in their journal."
                ),
                "reflectionQuestion" to Schema.string(
                    description = "One thoughtful, open-ended reflection question to invite deeper self-awareness."
                )
            )
        )
        Firebase.ai.generativeModel(
            modelName = AiModelTier.FLASH_LITE.modelId,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = reflectionSchema
            },
            systemInstruction = content {
                text(
                    "You summarize personal journal entries warmly without diagnosing or lecturing. Always output valid JSON matching the schema."
                )
            }
        )
    }

    private val transcribeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_TRANSCRIBE,
            systemInstruction = content {
                text(TRANSCRIBE_INSTRUCTION)
            }
        )
    }

    fun buildLiveSystemInstruction(
        mode: CompanionMode,
        profile: UserProfile?,
        recentMoods: List<MoodLog>
    ): String {
        val memoryContext = buildMemoryContext(profile, recentMoods)
        return """
            ${baseCompanionGuidelines()}
            VOICE CONVERSATION STYLE:
            - Keep spoken replies concise, warm, natural, and conversational (1-3 sentences per turn so dialogue feels fluid).
            - Active Mode (${mode.label}): ${mode.systemPrompt}
            - $memoryContext
        """.trimIndent()
    }

    fun openDirectLiveSession(
        mode: CompanionMode,
        profile: UserProfile?,
        recentMoods: List<MoodLog>,
        listener: DirectLiveListener
    ): DirectLiveConnection {
        return directClient.openLiveSession(
            modelId = MODEL_LIVE_VOICE,
            systemInstruction = buildLiveSystemInstruction(mode, profile, recentMoods),
            listener = listener
        )
    }

    @OptIn(PublicPreviewAPI::class)
    fun createLiveVoiceModel(
        mode: CompanionMode,
        profile: UserProfile?,
        recentMoods: List<MoodLog>
    ): LiveGenerativeModel {
        val systemText = buildLiveSystemInstruction(mode, profile, recentMoods)

        val config = liveGenerationConfig {
            responseModality = ResponseModality.AUDIO
            speechConfig = SpeechConfig(voice = Voice("Aoede"))
            inputAudioTranscription = AudioTranscriptionConfig()
            outputAudioTranscription = AudioTranscriptionConfig()
            contextWindowCompression = ContextWindowCompressionConfig(
                triggerTokens = 16_000,
                slidingWindow = SlidingWindow(targetTokens = 8_000)
            )
            realtimeInputConfig = realtimeInputConfig {
                activityHandling = RealtimeInputConfig.ActivityHandling.INTERRUPT
            }
        }

        return Firebase.ai.liveModel(
            modelName = MODEL_LIVE_VOICE,
            generationConfig = config,
            systemInstruction = content { text(systemText) }
        )
    }

    /**
     * Transcribes recorded PCM/WAV audio bytes using gemini-3.5-transcribe.
     */
    suspend fun transcribeAudio(
        audioBytes: ByteArray,
        mimeType: String = "audio/wav"
    ): String {
        if (audioBytes.size <= 44) {
            throw IllegalArgumentException("No audio was captured. Speak near the microphone and try again.")
        }

        return try {
            withTimeout(45_000L) {
                val directResult = if (directClient.isConfigured()) {
                    try {
                        directClient.transcribeAudio(
                            modelId = MODEL_TRANSCRIBE,
                            systemInstruction = TRANSCRIBE_INSTRUCTION,
                            audioBytes = audioBytes,
                            mimeType = mimeType
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "Direct transcription failed, falling back to Firebase AI Logic", e)
                        null
                    }
                } else null

                if (directResult != null) {
                    directResult
                } else {
                    val prompt = content {
                        inlineData(audioBytes, mimeType)
                        text("Transcribe the user's speech accurately into plain text.")
                    }
                    transcribeModel.generateContent(prompt).text.orEmpty().trim()
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Audio transcription timed out", e)
            throw IllegalStateException("Audio transcription timed out. Please try again.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Audio transcription failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Unable to transcribe audio right now.", e)
        }
    }

    internal fun buildSanitizedHistory(history: List<CompanionMessage>): List<Content> {
        val realMessages = history
            .filter { !it.id.startsWith("sample_") && it.content.isNotBlank() }
            .takeLast(16)

        val alternating = mutableListOf<Pair<String, String>>()
        for (msg in realMessages) {
            val role = if (msg.role == "user") "user" else "model"
            if (alternating.isEmpty()) {
                if (role == "user") {
                    alternating.add(role to msg.content.trim())
                }
            } else {
                val last = alternating.last()
                if (last.first == role) {
                    alternating[alternating.lastIndex] = role to "${last.second}\n${msg.content.trim()}"
                } else {
                    alternating.add(role to msg.content.trim())
                }
            }
        }

        if (alternating.lastOrNull()?.first == "user") {
            alternating.removeAt(alternating.lastIndex)
        }

        return alternating.map { (role, text) ->
            content(role) { text(text) }
        }
    }

    private fun buildMemoryContext(profile: UserProfile?, recentMoods: List<MoodLog>): String {
        return if (profile?.memoryConsentGranted == true) {
            val latestMood = recentMoods.firstOrNull()?.let { "${it.mood} (energy ${it.energyLevel}/5)" } ?: "not logged yet"
            "Consented User Context -> Name: ${profile.displayName}, Goal: ${profile.wellnessGoal}, Latest self-reported mood: $latestMood, Activity style: ${profile.activityPreference}, Diet: ${profile.dietaryPreference}."
        } else {
            "User has not enabled long-term memory sharing; respond using only the current conversation."
        }
    }

    fun streamCompanionReply(
        userMessage: String,
        mode: CompanionMode,
        modelTier: AiModelTier = mode.defaultModelTier,
        history: List<CompanionMessage>,
        profile: UserProfile?,
        recentMoods: List<MoodLog>,
        connectedAppsSummary: String = "",
        todayCalendarSummary: String = ""
    ): Flow<String> = flow {
        val memoryContext = buildMemoryContext(profile, recentMoods)

        val crisisHint = if (CrisisSafetyGuard.containsCrisisLanguage(userMessage)) {
            "IMPORTANT SAFETY OVERRIDE: The user may be in acute distress. Acknowledge their feelings gently, remind them they don't have to carry this alone, and clearly encourage contacting a trusted person, emergency services, or a trained crisis helpline (such as 988)."
        } else {
            ""
        }

        val geminiHistory = buildSanitizedHistory(history)

        val prompt = buildString {
            appendLine("[Active Companion Mode: ${mode.label} | Model Role: ${modelTier.roleLabel}]")
            appendLine(mode.systemPrompt)
            appendLine(memoryContext)
            if (connectedAppsSummary.isNotBlank()) {
                appendLine("Connected User Apps Enabled: $connectedAppsSummary")
            }
            if (todayCalendarSummary.isNotBlank()) {
                appendLine("User's Calendar Today: $todayCalendarSummary")
            }
            if (crisisHint.isNotBlank()) appendLine(crisisHint)
            appendLine()
            append("User: $userMessage")
        }

        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                var usedFirebaseFallback = false
                if (directClient.isConfigured()) {
                    try {
                        directClient.streamGenerateContent(
                            modelId = modelTier.modelId,
                            systemInstruction = chatSystemInstruction(modelTier),
                            history = geminiHistory,
                            userPrompt = prompt
                        ).collect { fullText ->
                            val clean = sanitizeCompanionText(fullText)
                            accumulated.clear()
                            accumulated.append(clean)
                            emit(clean)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        if (accumulated.isBlank()) {
                            Log.w(TAG, "Direct Gemini API failed (${modelTier.modelId}), falling back to Firebase AI Logic", e)
                            usedFirebaseFallback = true
                        } else {
                            throw e
                        }
                    }
                } else {
                    usedFirebaseFallback = true
                }

                if (usedFirebaseFallback) {
                    val activeModel = getChatModel(modelTier)
                    val chat = activeModel.startChat(history = geminiHistory)
                    chat.sendMessageStream(prompt).collect { chunk ->
                        val piece = chunk.text.orEmpty()
                        if (piece.isNotEmpty()) {
                            accumulated.append(piece)
                            emit(sanitizeCompanionText(accumulated.toString()))
                        }
                    }
                    if (accumulated.isBlank()) {
                        val fallbackResponse = activeModel.startChat(history = geminiHistory).sendMessage(prompt)
                        val fallbackText = sanitizeCompanionText(fallbackResponse.text.orEmpty().trim())
                        if (fallbackText.isNotEmpty()) {
                            accumulated.append(fallbackText)
                            emit(fallbackText)
                        }
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("MindMate received an empty response. Please try again.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Companion chat timed out (${modelTier.modelId})", e)
            throw IllegalStateException("Response timed out after 60 seconds. Please check your connection and try again.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Companion chat failed (${modelTier.modelId})", e)
            throw IllegalStateException(e.localizedMessage ?: "Unable to reach MindMate AI right now.", e)
        }
    }

    suspend fun generateJournalReflection(
        entryContent: String,
        moodTag: String
    ): JournalReflectionResult {
        val moodText = if (moodTag.isNotBlank()) "Self-reported mood tag: $moodTag." else ""
        val prompt = """
            $moodText
            Journal entry:
            $entryContent
        """.trimIndent()

        return try {
            val raw = withTimeout(60_000L) {
                val directRaw = if (directClient.isConfigured()) {
                    try {
                        val schema = JSONObject()
                            .put("type", "OBJECT")
                            .put(
                                "properties",
                                JSONObject()
                                    .put("summary", JSONObject().put("type", "STRING"))
                                    .put("reflectionQuestion", JSONObject().put("type", "STRING"))
                            )
                            .put("required", JSONArray().put("summary").put("reflectionQuestion"))
                        directClient.generateContent(
                            modelId = AiModelTier.FLASH_LITE.modelId,
                            systemInstruction = "You summarize personal journal entries warmly without diagnosing or lecturing. Always output valid JSON matching the schema.",
                            userPrompt = prompt,
                            responseMimeType = "application/json",
                            schemaJson = schema
                        )
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Log.w(TAG, "Direct reflection failed, falling back to Firebase AI Logic", e)
                        null
                    }
                } else null

                directRaw ?: (reflectionModel.generateContent(prompt).text
                    ?: throw IllegalStateException("Empty AI reflection response."))
            }
            val json = JSONObject(raw)
            JournalReflectionResult(
                summary = sanitizeCompanionText(
                    json.optString("summary", "Thank you for taking time to put your thoughts into words.")
                ),
                reflectionQuestion = sanitizeCompanionText(
                    json.optString("reflectionQuestion", "What feels most important to carry forward from this entry?")
                )
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Journal reflection timed out", e)
            throw IllegalStateException("Reflection timed out after 60 seconds. Please try again.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Journal reflection failed", e)
            throw IllegalStateException(e.localizedMessage ?: "Could not generate journal reflection.", e)
        }
    }

    companion object {
        const val MODEL_LIVE_VOICE = "gemini-3.8-live"
        const val MODEL_TRANSCRIBE = "gemini-3.5-transcribe"
        private const val TRANSCRIBE_INSTRUCTION =
            "You are an accurate speech-to-text transcriber. Output only the exact spoken words with natural punctuation and capitalization. If the audio is silent or unintelligible, return an empty string."
        private const val TAG = "MindMateAiService"

        fun sanitizeCompanionText(raw: String): String {
            return raw
                .replace(Regex("(?m)^#{1,6}\\s+"), "")
                .replace("**", "")
                .replace("__", "")
                .replace(Regex("`+"), "")
        }
    }
}
