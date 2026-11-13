package com.example.data.ai

import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.TextPart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

interface DirectLiveListener {
    fun onSetupComplete()
    fun onAudioOutput(pcmBytes: ByteArray)
    fun onInputTranscription(text: String)
    fun onOutputTranscription(text: String)
    fun onInterrupted()
    fun onTurnComplete()
    fun onError(message: String)
    fun onClosed()
}

class DirectGeminiApiClient(
    private val apiKeyProvider: () -> String = { BuildConfig.GEMINI_API_KEY }
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    fun isConfigured(): Boolean = isValidApiKey(apiKeyProvider())

    internal fun buildGeneratePayload(
        systemInstruction: String,
        history: List<Content>,
        userPrompt: String,
        responseMimeType: String? = null,
        schemaJson: JSONObject? = null
    ): JSONObject {
        val contentsArray = JSONArray()
        for (turn in history) {
            val role = turn.role ?: "user"
            val partsArray = JSONArray()
            for (part in turn.parts) {
                if (part is TextPart && part.text.isNotBlank()) {
                    partsArray.put(JSONObject().put("text", part.text))
                }
            }
            if (partsArray.length() > 0) {
                contentsArray.put(
                    JSONObject()
                        .put("role", role)
                        .put("parts", partsArray)
                )
            }
        }
        contentsArray.put(
            JSONObject()
                .put("role", "user")
                .put("parts", JSONArray().put(JSONObject().put("text", userPrompt)))
        )

        val root = JSONObject()
            .put("contents", contentsArray)
            .put(
                "systemInstruction",
                JSONObject().put(
                    "parts",
                    JSONArray().put(JSONObject().put("text", systemInstruction))
                )
            )

        if (!responseMimeType.isNullOrBlank() || schemaJson != null) {
            val genConfig = JSONObject()
            if (!responseMimeType.isNullOrBlank()) {
                genConfig.put("responseMimeType", responseMimeType)
            }
            if (schemaJson != null) {
                genConfig.put("responseSchema", schemaJson)
            }
            root.put("generationConfig", genConfig)
        }
        return root
    }

    internal fun buildAudioTranscriptionPayload(
        systemInstruction: String,
        audioBase64: String,
        mimeType: String
    ): JSONObject {
        val parts = JSONArray()
            .put(
                JSONObject().put(
                    "inlineData",
                    JSONObject()
                        .put("mimeType", mimeType)
                        .put("data", audioBase64)
                )
            )
            .put(JSONObject().put("text", "Transcribe the user's speech accurately into plain text."))

        return JSONObject()
            .put(
                "contents",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("parts", parts)
                )
            )
            .put(
                "systemInstruction",
                JSONObject().put(
                    "parts",
                    JSONArray().put(JSONObject().put("text", systemInstruction))
                )
            )
    }

    internal fun buildLiveSetupPayload(
        modelId: String,
        systemInstruction: String
    ): JSONObject {
        val generationConfig = JSONObject()
            .put("responseModalities", JSONArray().put("AUDIO"))
            .put(
                "speechConfig",
                JSONObject().put(
                    "voiceConfig",
                    JSONObject().put(
                        "prebuiltVoiceConfig",
                        JSONObject().put("voiceName", "Aoede")
                    )
                )
            )

        return JSONObject().put(
            "setup",
            JSONObject()
                .put("model", "models/$modelId")
                .put("generationConfig", generationConfig)
                .put(
                    "systemInstruction",
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", systemInstruction))
                    )
                )
                .put("inputAudioTranscription", JSONObject())
                .put("outputAudioTranscription", JSONObject())
        )
    }

    internal fun buildLiveAudioInputPayload(pcmBase64: String): JSONObject {
        return JSONObject().put(
            "realtimeInput",
            JSONObject().put(
                "mediaChunks",
                JSONArray().put(
                    JSONObject()
                        .put("mimeType", "audio/pcm;rate=16000")
                        .put("data", pcmBase64)
                )
            )
        )
    }

    fun streamGenerateContent(
        modelId: String,
        systemInstruction: String,
        history: List<Content>,
        userPrompt: String
    ): Flow<String> = flow {
        val apiKey = apiKeyProvider().trim()
        require(isValidApiKey(apiKey)) { "GEMINI_API_KEY is not configured." }

        val payload = buildGeneratePayload(systemInstruction, history, userPrompt)
        val bodyString = payload.toString()

        val bases = listOf(GEMINI_DEV_BASE_URL, VERTEX_EXPRESS_BASE_URL)
        var lastError: Exception? = null
        val accumulated = StringBuilder()

        for (baseUrl in bases) {
            val sseUrl = "$baseUrl/$modelId:streamGenerateContent?alt=sse"
            val request = Request.Builder()
                .url(sseUrl)
                .addHeader("x-goog-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(bodyString.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errBody = response.body?.string().orEmpty()
                        val msg = extractApiErrorMessage(response.code, errBody)
                        if (response.code in listOf(401, 403, 404)) {
                            lastError = IllegalStateException(msg)
                            return@use
                        }
                        throw IllegalStateException(msg)
                    }
                    val stream = response.body?.byteStream()
                        ?: throw IllegalStateException("Empty streaming response body.")
                    BufferedReader(InputStreamReader(stream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            val trimmed = line.orEmpty().trim()
                            if (trimmed.startsWith("data:")) {
                                val jsonText = trimmed.removePrefix("data:").trim()
                                if (jsonText.isNotEmpty() && jsonText != "[DONE]") {
                                    val chunkText = extractCandidateText(jsonText)
                                    if (chunkText.isNotEmpty()) {
                                        accumulated.append(chunkText)
                                        emit(accumulated.toString())
                                    }
                                }
                            }
                        }
                    }
                }
                if (accumulated.isNotBlank()) return@flow
            } catch (e: Exception) {
                lastError = e
                if (baseUrl == bases.last()) throw e
            }
        }

        if (accumulated.isBlank()) {
            throw lastError ?: IllegalStateException("No response received from Gemini API.")
        }
    }.flowOn(Dispatchers.IO)

    suspend fun generateContent(
        modelId: String,
        systemInstruction: String,
        userPrompt: String,
        responseMimeType: String? = null,
        schemaJson: JSONObject? = null
    ): String = withContext(Dispatchers.IO) {
        val payload = buildGeneratePayload(
            systemInstruction = systemInstruction,
            history = emptyList(),
            userPrompt = userPrompt,
            responseMimeType = responseMimeType,
            schemaJson = schemaJson
        )
        executeGenerateRequest(modelId, payload)
    }

    suspend fun transcribeAudio(
        modelId: String,
        systemInstruction: String,
        audioBytes: ByteArray,
        mimeType: String
    ): String = withContext(Dispatchers.IO) {
        val base64 = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
        val payload = buildAudioTranscriptionPayload(systemInstruction, base64, mimeType)
        executeGenerateRequest(modelId, payload)
    }

    private fun executeGenerateRequest(modelId: String, payload: JSONObject): String {
        val apiKey = apiKeyProvider().trim()
        require(isValidApiKey(apiKey)) { "GEMINI_API_KEY is not configured." }
        val bodyString = payload.toString()

        val bases = listOf(GEMINI_DEV_BASE_URL, VERTEX_EXPRESS_BASE_URL)
        var lastError: Exception? = null

        for (baseUrl in bases) {
            val url = "$baseUrl/$modelId:generateContent"
            val request = Request.Builder()
                .url(url)
                .addHeader("x-goog-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(bodyString.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val raw = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        val msg = extractApiErrorMessage(response.code, raw)
                        if (response.code in listOf(401, 403, 404)) {
                            lastError = IllegalStateException(msg)
                            return@use
                        }
                        throw IllegalStateException(msg)
                    }
                    return extractCandidateText(raw).trim()
                }
            } catch (e: Exception) {
                lastError = e
                if (baseUrl == bases.last()) throw e
            }
        }
        throw lastError ?: IllegalStateException("Gemini API request failed.")
    }

    fun openLiveSession(
        modelId: String,
        systemInstruction: String,
        listener: DirectLiveListener
    ): DirectLiveConnection {
        val apiKey = apiKeyProvider().trim()
        require(isValidApiKey(apiKey)) { "GEMINI_API_KEY is not configured." }

        val wsUrl = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=$apiKey"
        val request = Request.Builder()
            .url(wsUrl)
            .addHeader("x-goog-api-key", apiKey)
            .build()

        val ws = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val setupJson = buildLiveSetupPayload(modelId, systemInstruction).toString()
                webSocket.send(setupJson)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleLiveServerJson(text, listener)
            }

            override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) {
                handleLiveServerJson(bytes.utf8(), listener)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                listener.onClosed()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                listener.onClosed()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val detail = response?.let { "HTTP ${it.code}: ${it.message}" } ?: t.localizedMessage
                Log.e(TAG, "Direct Live WebSocket failure: $detail", t)
                listener.onError(detail ?: "Live WebSocket connection failed.")
            }
        })

        return DirectLiveConnection(ws, this)
    }

    private fun handleLiveServerJson(raw: String, listener: DirectLiveListener) {
        try {
            val json = JSONObject(raw)
            if (json.has("setupComplete")) {
                listener.onSetupComplete()
                return
            }
            val serverContent = json.optJSONObject("serverContent") ?: return
            if (serverContent.optBoolean("interrupted", false)) {
                listener.onInterrupted()
            }
            serverContent.optJSONObject("inputTranscription")
                ?.optString("text", "")
                ?.takeIf { it.isNotEmpty() }
                ?.let { listener.onInputTranscription(it) }

            serverContent.optJSONObject("outputTranscription")
                ?.optString("text", "")
                ?.takeIf { it.isNotEmpty() }
                ?.let { listener.onOutputTranscription(it) }

            val modelTurn = serverContent.optJSONObject("modelTurn")
            val parts = modelTurn?.optJSONArray("parts")
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val part = parts.optJSONObject(i) ?: continue
                    val inlineData = part.optJSONObject("inlineData")
                    if (inlineData != null) {
                        val mime = inlineData.optString("mimeType", "")
                        val dataB64 = inlineData.optString("data", "")
                        if (mime.startsWith("audio/") && dataB64.isNotEmpty()) {
                            val pcm = Base64.decode(dataB64, Base64.DEFAULT)
                            listener.onAudioOutput(pcm)
                        }
                    } else if (!serverContent.has("outputTranscription")) {
                        val txt = part.optString("text", "")
                        if (txt.isNotEmpty()) {
                            listener.onOutputTranscription(txt)
                        }
                    }
                }
            }

            if (serverContent.optBoolean("turnComplete", false)) {
                listener.onTurnComplete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed parsing Live server JSON", e)
        }
    }

    private fun extractCandidateText(rawJson: String): String {
        val json = JSONObject(rawJson)
        val candidates = json.optJSONArray("candidates") ?: return ""
        if (candidates.length() == 0) return ""
        val parts = candidates.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts") ?: return ""
        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            val text = parts.optJSONObject(i)?.optString("text", "").orEmpty()
            sb.append(text)
        }
        return sb.toString()
    }

    private fun extractApiErrorMessage(code: Int, rawBody: String): String {
        return try {
            val errObj = JSONObject(rawBody).optJSONObject("error")
            val msg = errObj?.optString("message").orEmpty()
            if (msg.isNotBlank()) "Gemini API ($code): $msg" else "Gemini API returned HTTP $code."
        } catch (_: Exception) {
            "Gemini API returned HTTP $code."
        }
    }

    companion object {
        private const val GEMINI_DEV_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        private const val VERTEX_EXPRESS_BASE_URL = "https://aiplatform.googleapis.com/v1/publishers/google/models"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private const val TAG = "DirectGeminiApiClient"

        fun isValidApiKey(key: String): Boolean {
            val trimmed = key.trim()
            return trimmed.isNotEmpty() && trimmed != "MY_GEMINI_API_KEY"
        }
    }
}

class DirectLiveConnection(
    private val webSocket: WebSocket,
    private val client: DirectGeminiApiClient
) {
    @Volatile
    private var closed = false

    fun sendPcmAudio(pcmBytes: ByteArray) {
        if (closed) return
        val b64 = Base64.encodeToString(pcmBytes, Base64.NO_WRAP)
        webSocket.send(client.buildLiveAudioInputPayload(b64).toString())
    }

    fun sendActivityStartSignal() {
        if (closed) return
        val msg = JSONObject().put(
            "realtimeInput",
            JSONObject().put("activityStart", JSONObject())
        )
        webSocket.send(msg.toString())
    }

    fun close() {
        if (!closed) {
            closed = true
            try {
                webSocket.close(1000, "User ended session")
            } catch (_: Exception) {
            }
        }
    }
}
