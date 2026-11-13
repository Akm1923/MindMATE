package com.example.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioAmplitudeUtilsTest {

    @Test
    fun `pure silence yields exactly zero amplitude`() {
        val silence = ShortArray(320) { 0 }
        assertEquals(0f, AudioAmplitudeUtils.computePcm16Rms(silence, silence.size), 0.0001f)

        val silentBytes = ByteArray(640) { 0 }
        assertEquals(0f, AudioAmplitudeUtils.computePcm16BytesRms(silentBytes), 0.0001f)
    }

    @Test
    fun `louder pcm signal produces strictly higher amplitude than quiet signal`() {
        val quiet = ShortArray(320) { idx -> if (idx % 2 == 0) 400 else -400 }
        val loud = ShortArray(320) { idx -> if (idx % 2 == 0) 16000 else -16000 }

        val quietAmp = AudioAmplitudeUtils.computePcm16Rms(quiet, quiet.size)
        val loudAmp = AudioAmplitudeUtils.computePcm16Rms(loud, loud.size)

        assertTrue("Quiet speech amplitude should be > 0f, was $quietAmp", quietAmp > 0f)
        assertTrue("Loud speech ($loudAmp) should exceed quiet ($quietAmp)", loudAmp > quietAmp)
        assertTrue("Amplitude must stay <= 1f", loudAmp <= 1f)

        val loudBytes = AudioAmplitudeUtils.shortsToLittleEndianBytes(loud, loud.size)
        assertEquals(loudAmp, AudioAmplitudeUtils.computePcm16BytesRms(loudBytes), 0.001f)
    }

    @Test
    fun `encodePcm16ToWav writes valid RIFF WAVE header and payload size`() {
        val pcm = ByteArray(100) { 0x1A }
        val wav = AudioAmplitudeUtils.encodePcm16ToWav(pcm, sampleRate = 16_000, channels = 1, bitsPerSample = 16)

        assertEquals(144, wav.size)
        assertEquals("RIFF", String(wav, 0, 4, Charsets.US_ASCII))
        assertEquals("WAVE", String(wav, 8, 4, Charsets.US_ASCII))
        assertEquals("fmt ", String(wav, 12, 4, Charsets.US_ASCII))
        assertEquals("data", String(wav, 36, 4, Charsets.US_ASCII))
    }
}
