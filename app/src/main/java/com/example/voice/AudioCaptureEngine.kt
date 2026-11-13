package com.example.voice

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.log10
import kotlin.math.sqrt

object AudioAmplitudeUtils {
    /**
     * Computes real normalized speech amplitude [0f..1f] from signed 16-bit PCM samples.
     * Pure silence produces 0f (never fabricates fake activity).
     */
    fun computePcm16Rms(samples: ShortArray, count: Int): Float {
        if (count <= 0) return 0f
        var sumSquares = 0.0
        for (i in 0 until count) {
            val v = samples[i].toDouble()
            sumSquares += v * v
        }
        val rms = sqrt(sumSquares / count) / 32768.0
        if (rms <= 1e-4) return 0f
        // Map -58 dBFS..0 dBFS linearly into [0f..1f]
        val db = 20.0 * log10(rms)
        return ((db + 58.0) / 58.0).toFloat().coerceIn(0f, 1f)
    }

    /**
     * Computes real normalized amplitude [0f..1f] from little-endian 16-bit PCM bytes.
     */
    fun computePcm16BytesRms(bytes: ByteArray, length: Int = bytes.size): Float {
        val sampleCount = length / 2
        if (sampleCount <= 0) return 0f
        var sumSquares = 0.0
        for (i in 0 until sampleCount) {
            val lo = bytes[i * 2].toInt() and 0xFF
            val hi = bytes[i * 2 + 1].toInt()
            val sample = ((hi shl 8) or lo).toShort().toDouble()
            sumSquares += sample * sample
        }
        val rms = sqrt(sumSquares / sampleCount) / 32768.0
        if (rms <= 1e-4) return 0f
        val db = 20.0 * log10(rms)
        return ((db + 58.0) / 58.0).toFloat().coerceIn(0f, 1f)
    }

    fun shortsToLittleEndianBytes(samples: ShortArray, count: Int): ByteArray {
        val out = ByteArray(count * 2)
        for (i in 0 until count) {
            val s = samples[i].toInt()
            out[i * 2] = (s and 0xFF).toByte()
            out[i * 2 + 1] = ((s ushr 8) and 0xFF).toByte()
        }
        return out
    }

    /**
     * Wraps raw little-endian PCM16 audio in a standard 44-byte RIFF/WAVE header.
     */
    fun encodePcm16ToWav(
        pcmBytes: ByteArray,
        sampleRate: Int = 16_000,
        channels: Int = 1,
        bitsPerSample: Int = 16
    ): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = channels * bitsPerSample / 8
        val dataSize = pcmBytes.size
        val chunkSize = 36 + dataSize

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(chunkSize)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16) // PCM subchunk size
            putShort(1.toShort()) // AudioFormat = 1 (PCM)
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort(blockAlign.toShort())
            putShort(bitsPerSample.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataSize)
        }.array()

        return header + pcmBytes
    }
}

class AndroidAudioCaptureEngine {
    private var audioRecord: AudioRecord? = null
    private var captureJob: Job? = null
    private val dictationBuffer = ByteArrayOutputStream()
    private val bufferLock = Any()
    private val intentionalStop = AtomicBoolean(false)

    @SuppressLint("MissingPermission")
    private fun createInitializedAudioRecord(sampleRate: Int = SAMPLE_RATE_HZ): AudioRecord? {
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(sampleRate / 5 * 2)

        val sources = listOf(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.MIC
        )

        for (source in sources) {
            val candidate = try {
                AudioRecord(
                    source,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    minBuf * 2
                )
            } catch (e: Exception) {
                Log.w(TAG, "AudioRecord creation failed for source $source", e)
                null
            }
            if (candidate != null && candidate.state == AudioRecord.STATE_INITIALIZED) {
                return candidate
            }
            candidate?.release()
        }
        return null
    }

    fun startPcmStream(
        scope: CoroutineScope,
        onChunk: suspend (pcmBytes: ByteArray, amplitude: Float) -> Unit,
        onError: (String) -> Unit
    ) {
        stop()
        intentionalStop.set(false)
        val record = createInitializedAudioRecord()
        if (record == null) {
            onError("Microphone hardware is unavailable or busy on this device.")
            return
        }
        audioRecord = record

        captureJob = scope.launch(Dispatchers.IO) {
            val frameSamples = 640 // 40ms at 16kHz (~25Hz smooth orb reactivity)
            val shortBuf = ShortArray(frameSamples)
            try {
                record.startRecording()
                if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                    if (!intentionalStop.get()) {
                        onError("Unable to start microphone audio capture.")
                    }
                    return@launch
                }
                while (isActive && !intentionalStop.get()) {
                    val read = record.read(shortBuf, 0, frameSamples)
                    if (intentionalStop.get() || !isActive) break
                    if (read > 0) {
                        val amp = AudioAmplitudeUtils.computePcm16Rms(shortBuf, read)
                        val bytes = AudioAmplitudeUtils.shortsToLittleEndianBytes(shortBuf, read)
                        onChunk(bytes, amp)
                    } else if (read < 0) {
                        onError("Microphone stream unavailable (code $read).")
                        break
                    }
                }
            } catch (e: Exception) {
                if (isActive && !intentionalStop.get()) {
                    onError(e.localizedMessage ?: "Microphone streaming failed.")
                }
            } finally {
                releaseRecorder(record)
            }
        }
    }

    fun startDictationCapture(
        scope: CoroutineScope,
        onAmplitude: (Float) -> Unit,
        onMaxDurationReached: () -> Unit,
        onError: (String) -> Unit
    ) {
        stop()
        intentionalStop.set(false)
        synchronized(bufferLock) {
            dictationBuffer.reset()
        }
        val record = createInitializedAudioRecord()
        if (record == null) {
            onError("Microphone hardware is unavailable on this device.")
            return
        }
        audioRecord = record

        captureJob = scope.launch(Dispatchers.IO) {
            val frameSamples = 640 // ~40ms at 16kHz for smooth live orb animation
            val shortBuf = ShortArray(frameSamples)
            val maxBytes = SAMPLE_RATE_HZ * 2 * MAX_DICTATION_SECONDS
            try {
                record.startRecording()
                if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                    if (!intentionalStop.get()) {
                        onError("Unable to start microphone recording.")
                    }
                    return@launch
                }
                while (isActive && !intentionalStop.get()) {
                    val read = record.read(shortBuf, 0, frameSamples)
                    if (intentionalStop.get() || !isActive) break
                    if (read > 0) {
                        val amp = AudioAmplitudeUtils.computePcm16Rms(shortBuf, read)
                        val bytes = AudioAmplitudeUtils.shortsToLittleEndianBytes(shortBuf, read)
                        val reachedCap = synchronized(bufferLock) {
                            dictationBuffer.write(bytes)
                            dictationBuffer.size() >= maxBytes
                        }
                        onAmplitude(amp)
                        if (reachedCap) {
                            onMaxDurationReached()
                            break
                        }
                    } else if (read < 0) {
                        onError("Microphone recording unavailable (code $read).")
                        break
                    }
                }
            } catch (e: Exception) {
                if (isActive && !intentionalStop.get()) {
                    onError(e.localizedMessage ?: "Audio recording failed.")
                }
            } finally {
                releaseRecorder(record)
            }
        }
    }

    fun stopDictationAndBuildWav(): ByteArray? {
        stop()
        val rawPcm = synchronized(bufferLock) {
            val bytes = dictationBuffer.toByteArray()
            dictationBuffer.reset()
            bytes
        }
        if (rawPcm.isEmpty()) return null
        return AudioAmplitudeUtils.encodePcm16ToWav(
            pcmBytes = rawPcm,
            sampleRate = SAMPLE_RATE_HZ,
            channels = 1,
            bitsPerSample = 16
        )
    }

    fun stop() {
        intentionalStop.set(true)
        captureJob?.cancel()
        captureJob = null
        val rec = audioRecord
        audioRecord = null
        if (rec != null) {
            releaseRecorder(rec)
        }
    }

    private fun releaseRecorder(record: AudioRecord) {
        try {
            if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                record.stop()
            }
        } catch (_: Exception) {
        }
        try {
            record.release()
        } catch (_: Exception) {
        }
    }

    companion object {
        const val SAMPLE_RATE_HZ = 16_000
        private const val MAX_DICTATION_SECONDS = 60
        private const val TAG = "AudioCaptureEngine"
    }
}
