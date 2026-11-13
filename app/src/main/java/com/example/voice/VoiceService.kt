package com.example.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.data.ai.DirectLiveConnection
import com.example.data.ai.DirectLiveListener
import com.example.data.ai.MindMateAiService
import com.example.data.model.CompanionMode
import com.example.data.model.MoodLog
import com.example.data.model.UserProfile
import com.google.firebase.ai.type.InlineData
import com.google.firebase.ai.type.LiveServerContent
import com.google.firebase.ai.type.LiveServerGoAway
import com.google.firebase.ai.type.LiveServerSetupComplete
import com.google.firebase.ai.type.LiveSession
import com.google.firebase.ai.type.LiveSessionResumptionUpdate
import com.google.firebase.ai.type.PublicPreviewAPI
import com.google.firebase.ai.type.SessionResumptionConfig
import com.google.firebase.ai.type.asInlineDataPartOrNull
import com.google.firebase.ai.type.asTextOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

enum class LiveConnectionState(val label: String, val isLiveConnected: Boolean) {
    DISCONNECTED("Voice Offline • Text Chat Ready", false),
    CONNECTING("Connecting to Gemini Live...", false),
    CONNECTED("Live Connected • gemini-3.8-live", true),
    RECONNECTING("Reconnecting Live Session...", false),
    ERROR("Live Connection Failed", false)
}

data class VoiceUiState(
    val connectionState: LiveConnectionState = LiveConnectionState.DISCONNECTED,
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val isDictating: Boolean = false,
    val isTranscribing: Boolean = false,
    val isMuted: Boolean = false,
    val liveInputTranscript: String = "",
    val liveOutputTranscript: String = "",
    val audioAmplitude: Float = 0f,
    val waveformHistory: List<Float> = List(WAVEFORM_BAR_COUNT) { 0f },
    val hasRealAudioSignal: Boolean = false,
    val canInterrupt: Boolean = false,
    val sessionResumable: Boolean = false,
    val errorMessage: String? = null
) {
    companion object {
        const val WAVEFORM_BAR_COUNT = 24
    }
}

interface VoiceService {
    val state: StateFlow<VoiceUiState>
    fun connectLive(
        hasMicPermission: Boolean,
        mode: CompanionMode,
        profile: UserProfile?,
        recentMoods: List<MoodLog>,
        onTurnComplete: (userText: String, assistantText: String) -> Unit
    )
    fun disconnectLive()
    fun toggleMute()
    fun interruptAssistant()
    fun startDictation(hasMicPermission: Boolean, onAutoStopTranscript: (String) -> Unit)
    fun stopAndTranscribeDictation(onTranscript: (String) -> Unit)
    fun speakTextReply(text: String)
    fun stopSpeaking()
    fun clearError()
    fun release()
}

@OptIn(PublicPreviewAPI::class)
class MindMateVoiceService(
    context: Context,
    private val aiService: MindMateAiService
) : VoiceService {

    private val appContext = context.applicationContext
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _state = MutableStateFlow(VoiceUiState())
    override val state: StateFlow<VoiceUiState> = _state.asStateFlow()

    private val captureEngine = AndroidAudioCaptureEngine()

    private var liveSession: LiveSession? = null
    private var directLiveConnection: DirectLiveConnection? = null
    private var liveSessionJob: Job? = null
    private var playbackJob: Job? = null
    private var playbackChannel: Channel<ByteArray>? = null
    private var audioTrack: AudioTrack? = null
    private var resumptionHandle: String? = null
    private val userRequestedClose = AtomicBoolean(false)
    private val activePlaybackGen = java.util.concurrent.atomic.AtomicInteger(0)

    private var tts: TextToSpeech? = null
    private var ttsReady = false

    private val inputTranscriptBuf = StringBuilder()
    private val outputTranscriptBuf = StringBuilder()

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                ttsReady = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _state.value = _state.value.copy(
                            isSpeaking = true,
                            canInterrupt = true,
                            // TTS does not expose raw PCM samples; keep real-signal false so UI never fakes PCM bars
                            audioAmplitude = 0f,
                            hasRealAudioSignal = false
                        )
                    }

                    override fun onDone(utteranceId: String?) {
                        _state.value = _state.value.copy(
                            isSpeaking = false,
                            canInterrupt = false,
                            audioAmplitude = 0f,
                            hasRealAudioSignal = false
                        )
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _state.value = _state.value.copy(
                            isSpeaking = false,
                            canInterrupt = false,
                            audioAmplitude = 0f,
                            hasRealAudioSignal = false
                        )
                    }
                })
            }
        }
    }

    private fun pushAmplitudeSample(amplitude: Float, hasSignal: Boolean) {
        val clamped = amplitude.coerceIn(0f, 1f)
        val prev = _state.value.waveformHistory
        val next = if (prev.size >= VoiceUiState.WAVEFORM_BAR_COUNT) {
            prev.drop(1) + clamped
        } else {
            List(VoiceUiState.WAVEFORM_BAR_COUNT - prev.size - 1) { 0f } + prev + clamped
        }
        _state.value = _state.value.copy(
            audioAmplitude = clamped,
            waveformHistory = next,
            hasRealAudioSignal = hasSignal
        )
    }

    private fun resetWaveform() {
        _state.value = _state.value.copy(
            audioAmplitude = 0f,
            waveformHistory = List(VoiceUiState.WAVEFORM_BAR_COUNT) { 0f },
            hasRealAudioSignal = false
        )
    }

    override fun connectLive(
        hasMicPermission: Boolean,
        mode: CompanionMode,
        profile: UserProfile?,
        recentMoods: List<MoodLog>,
        onTurnComplete: (userText: String, assistantText: String) -> Unit
    ) {
        if (!hasMicPermission) {
            _state.value = _state.value.copy(
                connectionState = LiveConnectionState.DISCONNECTED,
                errorMessage = "Microphone permission is required for real-time Gemini Live voice."
            )
            return
        }

        // Stop any active dictation or TTS before establishing live full-duplex call
        captureEngine.stop()
        stopSpeaking()
        teardownLiveResources(markDisconnected = false)

        userRequestedClose.set(false)
        inputTranscriptBuf.clear()
        outputTranscriptBuf.clear()

        _state.value = _state.value.copy(
            connectionState = LiveConnectionState.CONNECTING,
            isListening = false,
            isSpeaking = false,
            isDictating = false,
            isTranscribing = false,
            liveInputTranscript = "",
            liveOutputTranscript = "",
            canInterrupt = false,
            errorMessage = null
        )

        liveSessionJob = serviceScope.launch {
            if (aiService.hasCustomApiKey()) {
                connectDirectLive(mode, profile, recentMoods, onTurnComplete)
                return@launch
            }
            runFirebaseLiveLoop(mode, profile, recentMoods, onTurnComplete)
        }
    }

    private suspend fun runFirebaseLiveLoop(
        mode: CompanionMode,
        profile: UserProfile?,
        recentMoods: List<MoodLog>,
        onTurnComplete: (userText: String, assistantText: String) -> Unit
    ) {
            var reconnectAttempt = 0
            while (serviceScope.isActive && !userRequestedClose.get()) {
                try {
                    val liveModel = aiService.createLiveVoiceModel(mode, profile, recentMoods)
                    val handle = resumptionHandle
                    val session = withTimeout(20_000L) {
                        if (handle != null && reconnectAttempt > 0) {
                            liveModel.connect(SessionResumptionConfig(handle))
                        } else {
                            liveModel.connect()
                        }
                    }
                    liveSession = session

                    // Only mark CONNECTED after LiveModel handshake actually succeeds
                    _state.value = _state.value.copy(
                        connectionState = LiveConnectionState.CONNECTED,
                        isListening = !_state.value.isMuted,
                        errorMessage = null
                    )

                    startPlaybackLoop()
                    startLiveMicStream(session)

                    var goAwayRequested = false

                    session.receive().collect { message ->
                        when (message) {
                            is LiveServerSetupComplete -> {
                                _state.value = _state.value.copy(
                                    connectionState = LiveConnectionState.CONNECTED,
                                    errorMessage = null
                                )
                            }

                            is LiveSessionResumptionUpdate -> {
                                val nextHandle = message.newHandle
                                if (message.resumable == true && !nextHandle.isNullOrBlank()) {
                                    resumptionHandle = nextHandle
                                    _state.value = _state.value.copy(sessionResumable = true)
                                }
                            }

                            is LiveServerGoAway -> {
                                Log.i(TAG, "LiveServerGoAway received, timeLeft=${message.timeLeft}")
                                goAwayRequested = true
                                if (resumptionHandle != null && !userRequestedClose.get()) {
                                    _state.value = _state.value.copy(
                                        connectionState = LiveConnectionState.RECONNECTING
                                    )
                                    try {
                                        session.close()
                                    } catch (_: Exception) {
                                    }
                                }
                            }

                            is LiveServerContent -> {
                                if (message.interrupted) {
                                    clearPlaybackBuffer()
                                    val spokenSoFar = outputTranscriptBuf.toString().trim()
                                    if (spokenSoFar.isNotEmpty()) {
                                        val userSaid = inputTranscriptBuf.toString().trim()
                                        onTurnComplete(userSaid, "$spokenSoFar [interrupted]")
                                    }
                                    inputTranscriptBuf.clear()
                                    outputTranscriptBuf.clear()
                                    _state.value = _state.value.copy(
                                        isSpeaking = false,
                                        canInterrupt = false,
                                        liveInputTranscript = "",
                                        liveOutputTranscript = ""
                                    )
                                }

                                message.inputTranscription?.text?.let { chunk ->
                                    if (chunk.isNotEmpty()) {
                                        inputTranscriptBuf.append(chunk)
                                        _state.value = _state.value.copy(
                                            liveInputTranscript = inputTranscriptBuf.toString().trim()
                                        )
                                    }
                                }

                                message.outputTranscription?.text?.let { chunk ->
                                    if (chunk.isNotEmpty()) {
                                        outputTranscriptBuf.append(chunk)
                                        _state.value = _state.value.copy(
                                            liveOutputTranscript = outputTranscriptBuf.toString().trim()
                                        )
                                    }
                                }

                                message.content?.parts?.forEach { part ->
                                    val inlineAudio = part.asInlineDataPartOrNull()
                                    if (inlineAudio != null && inlineAudio.mimeType.startsWith("audio/")) {
                                        playbackChannel?.trySend(inlineAudio.inlineData)
                                    } else if (message.outputTranscription == null) {
                                        part.asTextOrNull()?.let { textFallback ->
                                            if (textFallback.isNotEmpty()) {
                                                outputTranscriptBuf.append(textFallback)
                                                _state.value = _state.value.copy(
                                                    liveOutputTranscript = outputTranscriptBuf.toString().trim()
                                                )
                                            }
                                        }
                                    }
                                }

                                if (message.turnComplete) {
                                    val finalUser = inputTranscriptBuf.toString().trim()
                                    val finalAssistant = outputTranscriptBuf.toString().trim()
                                    if (finalUser.isNotEmpty() || finalAssistant.isNotEmpty()) {
                                        onTurnComplete(finalUser, finalAssistant)
                                    }
                                    inputTranscriptBuf.clear()
                                    outputTranscriptBuf.clear()
                                    _state.value = _state.value.copy(
                                        liveInputTranscript = "",
                                        liveOutputTranscript = ""
                                    )
                                }
                            }
                        }
                    }

                    // Stream ended cleanly
                    if (userRequestedClose.get()) break

                    if (goAwayRequested && resumptionHandle != null && reconnectAttempt < 2) {
                        reconnectAttempt++
                        _state.value = _state.value.copy(
                            connectionState = LiveConnectionState.RECONNECTING
                        )
                        captureEngine.stop()
                        continue
                    } else {
                        // Honest disconnect when server closes stream
                        _state.value = _state.value.copy(
                            connectionState = LiveConnectionState.DISCONNECTED,
                            isListening = false,
                            isSpeaking = false,
                            canInterrupt = false
                        )
                        break
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Live session failed", e)
                    if (!userRequestedClose.get() && resumptionHandle != null && reconnectAttempt < 1) {
                        reconnectAttempt++
                        _state.value = _state.value.copy(
                            connectionState = LiveConnectionState.RECONNECTING,
                            isListening = false,
                            isSpeaking = false
                        )
                        captureEngine.stop()
                        delay(600L)
                        continue
                    }
                    _state.value = _state.value.copy(
                        connectionState = LiveConnectionState.ERROR,
                        isListening = false,
                        isSpeaking = false,
                        canInterrupt = false,
                        errorMessage = e.localizedMessage ?: "Failed to maintain Gemini Live connection."
                    )
                    resetWaveform()
                    break
                }
            }
            teardownLiveResources(markDisconnected = userRequestedClose.get())
    }

    private fun startLiveMicStream(session: LiveSession) {
        captureEngine.startPcmStream(
            scope = serviceScope,
            onChunk = { pcmBytes, amplitude ->
                if (_state.value.isMuted) {
                    if (!_state.value.isSpeaking) {
                        pushAmplitudeSample(0f, hasSignal = false)
                    }
                    return@startPcmStream
                }
        if (!_state.value.isSpeaking) {
            pushAmplitudeSample(amplitude, hasSignal = amplitude > 0f)
        }
        if (!_state.value.isListening) {
            _state.value = _state.value.copy(isListening = true)
        }
        if (!session.isClosed()) {
            try {
                session.sendAudioRealtime(InlineData(pcmBytes, "audio/pcm"))
            } catch (e: Exception) {
                Log.w(TAG, "Failed sending realtime audio frame", e)
            }
        }
            },
            onError = { micError ->
                _state.value = _state.value.copy(
                    isListening = false,
                    errorMessage = micError
                )
                resetWaveform()
            }
        )
    }

    private fun connectDirectLive(
        mode: CompanionMode,
        profile: UserProfile?,
        recentMoods: List<MoodLog>,
        onTurnComplete: (userText: String, assistantText: String) -> Unit
    ) {
        val setupCompleted = AtomicBoolean(false)
        val conn = aiService.openDirectLiveSession(
            mode = mode,
            profile = profile,
            recentMoods = recentMoods,
            listener = object : DirectLiveListener {
                override fun onSetupComplete() {
                    setupCompleted.set(true)
                    _state.value = _state.value.copy(
                        connectionState = LiveConnectionState.CONNECTED,
                        isListening = !_state.value.isMuted,
                        errorMessage = null
                    )
                    startPlaybackLoop()
                    captureEngine.startPcmStream(
                        scope = serviceScope,
                        onChunk = { pcmBytes, amplitude ->
                            if (_state.value.isMuted) {
                                if (!_state.value.isSpeaking) {
                                    pushAmplitudeSample(0f, hasSignal = false)
                                }
                                return@startPcmStream
                            }
                            if (!_state.value.isSpeaking) {
                                pushAmplitudeSample(amplitude, hasSignal = amplitude > 0f)
                            }
                            if (!_state.value.isListening) {
                                _state.value = _state.value.copy(isListening = true)
                            }
                            directLiveConnection?.sendPcmAudio(pcmBytes)
                        },
                        onError = { micError ->
                            _state.value = _state.value.copy(
                                isListening = false,
                                errorMessage = micError
                            )
                            resetWaveform()
                        }
                    )
                }

                override fun onAudioOutput(pcmBytes: ByteArray) {
                    playbackChannel?.trySend(pcmBytes)
                }

                override fun onInputTranscription(text: String) {
                    inputTranscriptBuf.append(text)
                    _state.value = _state.value.copy(
                        liveInputTranscript = inputTranscriptBuf.toString().trim()
                    )
                }

                override fun onOutputTranscription(text: String) {
                    outputTranscriptBuf.append(text)
                    _state.value = _state.value.copy(
                        liveOutputTranscript = outputTranscriptBuf.toString().trim()
                    )
                }

                override fun onInterrupted() {
                    clearPlaybackBuffer()
                    val spokenSoFar = outputTranscriptBuf.toString().trim()
                    if (spokenSoFar.isNotEmpty()) {
                        onTurnComplete(inputTranscriptBuf.toString().trim(), "$spokenSoFar [interrupted]")
                    }
                    inputTranscriptBuf.clear()
                    outputTranscriptBuf.clear()
                    _state.value = _state.value.copy(
                        isSpeaking = false,
                        canInterrupt = false,
                        liveInputTranscript = "",
                        liveOutputTranscript = ""
                    )
                }

                override fun onTurnComplete() {
                    val u = inputTranscriptBuf.toString().trim()
                    val a = outputTranscriptBuf.toString().trim()
                    if (u.isNotEmpty() || a.isNotEmpty()) {
                        onTurnComplete(u, a)
                    }
                    inputTranscriptBuf.clear()
                    outputTranscriptBuf.clear()
                    _state.value = _state.value.copy(
                        liveInputTranscript = "",
                        liveOutputTranscript = ""
                    )
                }

                override fun onError(message: String) {
                    if (userRequestedClose.get()) return
                    if (!setupCompleted.get()) {
                        Log.w(TAG, "Direct Live failed before setup ($message); falling back to Firebase Live")
                        directLiveConnection?.close()
                        directLiveConnection = null
                        liveSessionJob = serviceScope.launch {
                            runFirebaseLiveLoop(mode, profile, recentMoods, onTurnComplete)
                        }
                        return
                    }
                    _state.value = _state.value.copy(
                        connectionState = LiveConnectionState.ERROR,
                        isListening = false,
                        isSpeaking = false,
                        canInterrupt = false,
                        errorMessage = message
                    )
                    teardownLiveResources(markDisconnected = false)
                }

                override fun onClosed() {
                    if (!userRequestedClose.get() &&
                        setupCompleted.get() &&
                        _state.value.connectionState != LiveConnectionState.ERROR
                    ) {
                        teardownLiveResources(markDisconnected = true)
                    }
                }
            }
        )
        directLiveConnection = conn
    }

    private fun startPlaybackLoop() {
        playbackJob?.cancel()
        playbackChannel?.close()

        val channel = Channel<ByteArray>(Channel.UNLIMITED)
        playbackChannel = channel

        val minBuf = AudioTrack.getMinBufferSize(
            LIVE_OUTPUT_SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4800)

        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(LIVE_OUTPUT_SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf * 3)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AudioTrack", e)
            _state.value = _state.value.copy(
                errorMessage = "Audio playback hardware unavailable: ${e.localizedMessage}"
            )
            return
        }

        audioTrack = track
        val gen = activePlaybackGen.incrementAndGet()

        playbackJob = serviceScope.launch(Dispatchers.IO) {
            try {
                track.play()
                while (isActive) {
                    val chunk = withTimeoutOrNull(240L) {
                        channel.receive()
                    }
                    if (chunk == null) {
                        if (_state.value.isSpeaking && activePlaybackGen.get() == gen) {
                            _state.value = _state.value.copy(
                                isSpeaking = false,
                                canInterrupt = false
                            )
                            if (_state.value.isMuted || !_state.value.isListening) {
                                pushAmplitudeSample(0f, hasSignal = false)
                            }
                        }
                        continue
                    }

                    if (activePlaybackGen.get() != gen) continue

                    // Slice playback into ~40ms frames (1920 bytes @ 24kHz PCM16) for smooth 25Hz orb animation
                    val stepBytes = 1920
                    var offset = 0
                    while (offset < chunk.size && isActive && activePlaybackGen.get() == gen) {
                        val len = minOf(stepBytes, chunk.size - offset)
                        val slice = if (offset == 0 && len == chunk.size) chunk else chunk.copyOfRange(offset, offset + len)
                        val outAmp = AudioAmplitudeUtils.computePcm16BytesRms(slice)
                        _state.value = _state.value.copy(
                            isSpeaking = true,
                            canInterrupt = true
                        )
                        pushAmplitudeSample(outAmp, hasSignal = true)
                        track.write(slice, 0, slice.size)
                        offset += len
                    }
                }
            } catch (_: CancellationException) {
            } catch (e: Exception) {
                Log.w(TAG, "AudioTrack playback loop error", e)
            } finally {
                releaseAudioTrack(track)
            }
        }
    }

    private fun clearPlaybackBuffer() {
        activePlaybackGen.incrementAndGet()
        val ch = playbackChannel
        while (ch?.tryReceive()?.isSuccess == true) {
            // drain queued PCM chunks immediately on interruption
        }
        try {
            audioTrack?.let { track ->
                if (track.state == AudioTrack.STATE_INITIALIZED) {
                    track.pause()
                    track.flush()
                    track.play()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed flushing AudioTrack on interruption", e)
        }
    }

    private fun releaseAudioTrack(track: AudioTrack?) {
        if (track == null) return
        try {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING ||
                track.playState == AudioTrack.PLAYSTATE_PAUSED
            ) {
                track.stop()
            }
        } catch (_: Exception) {
        }
        try {
            track.release()
        } catch (_: Exception) {
        }
    }

    private fun teardownLiveResources(markDisconnected: Boolean) {
        captureEngine.stop()
        playbackJob?.cancel()
        playbackJob = null
        playbackChannel?.close()
        playbackChannel = null
        releaseAudioTrack(audioTrack)
        audioTrack = null

        directLiveConnection?.close()
        directLiveConnection = null

        val session = liveSession
        liveSession = null
        if (session != null && !session.isClosed()) {
            serviceScope.launch {
                try {
                    session.close()
                } catch (_: Exception) {
                }
            }
        }

        if (markDisconnected) {
            resumptionHandle = null
            _state.value = _state.value.copy(
                connectionState = LiveConnectionState.DISCONNECTED,
                isListening = false,
                isSpeaking = false,
                canInterrupt = false,
                sessionResumable = false,
                liveInputTranscript = "",
                liveOutputTranscript = ""
            )
            resetWaveform()
        }
    }

    override fun disconnectLive() {
        userRequestedClose.set(true)
        liveSessionJob?.cancel()
        liveSessionJob = null
        teardownLiveResources(markDisconnected = true)
    }

    override fun toggleMute() {
        val nextMuted = !_state.value.isMuted
        _state.value = _state.value.copy(
            isMuted = nextMuted,
            isListening = if (nextMuted) false else _state.value.connectionState.isLiveConnected,
            errorMessage = null
        )
        if (nextMuted && !_state.value.isSpeaking) {
            resetWaveform()
        }
    }

    override fun interruptAssistant() {
        clearPlaybackBuffer()
        stopSpeaking()
        directLiveConnection?.sendActivityStartSignal()
        val session = liveSession
        if (session != null && !session.isClosed()) {
            serviceScope.launch {
                try {
                    session.sendStartActivityRealtime()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed sending barge-in activity signal", e)
                }
            }
        }
        _state.value = _state.value.copy(
            isSpeaking = false,
            canInterrupt = false,
            liveOutputTranscript = ""
        )
    }

    override fun startDictation(
        hasMicPermission: Boolean,
        onAutoStopTranscript: (String) -> Unit
    ) {
        if (!hasMicPermission) {
            _state.value = _state.value.copy(
                errorMessage = "Grant microphone permission to record and transcribe audio."
            )
            return
        }
        if (_state.value.isMuted) {
            _state.value = _state.value.copy(isMuted = false)
        }
        if (_state.value.connectionState.isLiveConnected) {
            disconnectLive()
        }
        stopSpeaking()

        _state.value = _state.value.copy(
            isDictating = true,
            isListening = true,
            isTranscribing = false,
            errorMessage = null
        )

        captureEngine.startDictationCapture(
            scope = serviceScope,
            onAmplitude = { amp ->
                pushAmplitudeSample(amp, hasSignal = amp > 0f)
            },
            onMaxDurationReached = {
                stopAndTranscribeDictation(onAutoStopTranscript)
            },
            onError = { err ->
                _state.value = _state.value.copy(
                    isDictating = false,
                    isListening = false,
                    isTranscribing = false,
                    errorMessage = err
                )
                resetWaveform()
            }
        )
    }

    override fun stopAndTranscribeDictation(onTranscript: (String) -> Unit) {
        val wavBytes = captureEngine.stopDictationAndBuildWav()
        _state.value = _state.value.copy(
            isDictating = false,
            isListening = false
        )
        resetWaveform()

        if (wavBytes == null || wavBytes.size <= 44) {
            _state.value = _state.value.copy(
                isTranscribing = false,
                errorMessage = null
            )
            return
        }

        _state.value = _state.value.copy(isTranscribing = true, errorMessage = null)
        serviceScope.launch {
            try {
                val transcript = aiService.transcribeAudio(wavBytes, "audio/wav")
                _state.value = _state.value.copy(isTranscribing = false)
                if (transcript.isNotBlank()) {
                    onTranscript(transcript)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isTranscribing = false,
                    errorMessage = e.localizedMessage ?: "Audio transcription failed."
                )
            }
        }
    }

    override fun speakTextReply(text: String) {
        if (!ttsReady || _state.value.isMuted || text.isBlank() || _state.value.connectionState.isLiveConnected) {
            return
        }
        val clean = text.take(900)
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    override fun stopSpeaking() {
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
        _state.value = _state.value.copy(
            isSpeaking = false,
            canInterrupt = false
        )
        if (!_state.value.isListening) {
            resetWaveform()
        }
    }

    override fun clearError() {
        if (_state.value.errorMessage != null) {
            _state.value = _state.value.copy(errorMessage = null)
        }
    }

    override fun release() {
        disconnectLive()
        captureEngine.stop()
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (_: Exception) {
        }
        serviceScope.cancel()
    }

    private companion object {
        const val LIVE_OUTPUT_SAMPLE_RATE = 24_000
        const val TAG = "MindMateVoiceService"
    }
}
