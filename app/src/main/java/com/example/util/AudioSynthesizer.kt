package com.example.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

object AudioSynthesizer {

    private const val SAMPLE_RATE = 44100

    /**
     * Synthesize a full rich harmonic cinematic soundtrack WAV file.
     */
    suspend fun synthesizeSoundtrack(
        context: Context,
        style: String = "سينمائي",
        durationSeconds: Int = 18
    ): File = withContext(Dispatchers.IO) {
        val outputFile = File(context.cacheDir, "soundtrack_${UUID.randomUUID()}.wav")

        val totalSamples = SAMPLE_RATE * durationSeconds
        val buffer = ShortArray(totalSamples)

        // Base chord frequencies depending on style
        val chordProgressions = when {
            style.contains("أنمي") || style.contains("خيالي") -> listOf(
                doubleArrayOf(261.63, 329.63, 392.00, 523.25), // C maj
                doubleArrayOf(220.00, 261.63, 329.63, 440.00), // A min
                doubleArrayOf(174.61, 220.00, 261.63, 349.23), // F maj
                doubleArrayOf(196.00, 246.94, 293.66, 392.00)  // G maj
            )
            style.contains("خيال علمي") || style.contains("سايبربانك") -> listOf(
                doubleArrayOf(110.00, 164.81, 220.00, 329.63), // A min drone
                doubleArrayOf(130.81, 196.00, 261.63, 392.00), // C drone
                doubleArrayOf(98.00, 146.83, 196.00, 293.66),  // G drone
                doubleArrayOf(116.54, 174.61, 233.08, 349.23)  // Bb drone
            )
            style.contains("غموض") || style.contains("إثارة") -> listOf(
                doubleArrayOf(146.83, 174.61, 220.00, 311.13), // D dim/min
                doubleArrayOf(138.59, 174.61, 207.65, 277.18), // C# aug
                doubleArrayOf(146.83, 220.00, 293.66, 440.00), // D min
                doubleArrayOf(123.47, 164.81, 246.94, 329.63)  // B min
            )
            else -> listOf(
                // Cinematic D minor epic progression
                doubleArrayOf(146.83, 220.00, 261.63, 349.23, 73.42), // Dm9
                doubleArrayOf(116.54, 174.61, 233.08, 349.23, 58.27), // Bb maj7
                doubleArrayOf(174.61, 220.00, 261.63, 349.23, 87.31), // F maj
                doubleArrayOf(130.81, 196.00, 246.94, 329.63, 65.41)  // C add9
            )
        }

        val chordDurationSamples = totalSamples / chordProgressions.size

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val chordIndex = (i / chordDurationSamples).coerceIn(0, chordProgressions.size - 1)
            val activeChord = chordProgressions[chordIndex]
            val chordLocalTime = (i % chordDurationSamples).toDouble() / SAMPLE_RATE
            val chordPeriod = chordDurationSamples.toDouble() / SAMPLE_RATE

            // Smooth crossfade envelope per chord
            val chordEnvelope = sin(PI * (chordLocalTime / chordPeriod)).coerceAtLeast(0.0)

            // Global fade in (first 2s) and fade out (last 2s)
            val globalFade = when {
                t < 2.0 -> (t / 2.0).coerceIn(0.0, 1.0)
                t > durationSeconds - 2.5 -> ((durationSeconds - t) / 2.5).coerceIn(0.0, 1.0)
                else -> 1.0
            }

            var sampleSum = 0.0

            // Harmonic voices in chord
            for ((voiceIdx, freq) in activeChord.withIndex()) {
                val vibrato = 1.0 + 0.004 * sin(2.0 * PI * 4.5 * t)
                val voiceFreq = freq * vibrato
                // Fundamental sine wave
                val fundamental = sin(2.0 * PI * voiceFreq * t)
                // Warm second harmonic
                val secondHarmonic = 0.4 * sin(2.0 * PI * (voiceFreq * 2.0) * t)
                // Soft octave sub-harmonic for deep warmth
                val subHarmonic = 0.3 * sin(2.0 * PI * (voiceFreq * 0.5) * t)

                val voiceWeight = 1.0 / (voiceIdx + 1.2)
                sampleSum += (fundamental + secondHarmonic + subHarmonic) * voiceWeight
            }

            // Low frequency cinematic swell pulse (0.25 Hz slow breath)
            val cinematicSwell = 0.8 + 0.2 * sin(2.0 * PI * 0.25 * t)

            val finalAmplitude = (sampleSum * chordEnvelope * globalFade * cinematicSwell * 0.28)
                .coerceIn(-1.0, 1.0)

            buffer[i] = (finalAmplitude * Short.MAX_VALUE).toInt().toShort()
        }

        writeWavFile(outputFile, buffer, SAMPLE_RATE)
        outputFile
    }

    private fun writeWavFile(file: File, pcmData: ShortArray, sampleRate: Int) {
        val totalAudioLen = (pcmData.size * 2).toLong()
        val totalDataLen = totalAudioLen + 36
        val channels = 1
        val byteRate = (sampleRate * channels * 2).toLong()

        val header = ByteArray(44)
        val headerBuf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)

        // RIFF chunk
        headerBuf.put("RIFF".toByteArray())
        headerBuf.putInt(totalDataLen.toInt())
        headerBuf.put("WAVE".toByteArray())

        // fmt chunk
        headerBuf.put("fmt ".toByteArray())
        headerBuf.putInt(16) // Subchunk1Size
        headerBuf.putShort(1) // AudioFormat (PCM = 1)
        headerBuf.putShort(channels.toShort())
        headerBuf.putInt(sampleRate)
        headerBuf.putInt(byteRate.toInt())
        headerBuf.putShort((channels * 2).toShort()) // BlockAlign
        headerBuf.putShort(16) // BitsPerSample

        // data chunk
        headerBuf.put("data".toByteArray())
        headerBuf.putInt(totalAudioLen.toInt())

        FileOutputStream(file).use { fos ->
            fos.write(header)
            val audioBytes = ByteArray(pcmData.size * 2)
            ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(pcmData)
            fos.write(audioBytes)
        }
    }
}
