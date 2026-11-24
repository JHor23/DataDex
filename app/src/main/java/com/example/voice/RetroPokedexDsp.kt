package com.example.voice

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tanh

data class DecodedWavPcm(
    val sampleRate: Int,
    val samples: ShortArray
)

/**
 * Pure-Kotlin digital signal processor that shapes standard TTS speech audio into a
 * classic late-90s handheld Pokédex synthesized computer voice.
 */
object RetroPokedexDsp {

    /**
     * Parses a standard RIFF/WAVE PCM file (8/16-bit mono or stereo) into mono 16-bit samples.
     */
    fun decodeWavToMonoPcm(wavBytes: ByteArray): DecodedWavPcm? {
        if (wavBytes.size < 44) return null
        val buf = ByteBuffer.wrap(wavBytes).order(ByteOrder.LITTLE_ENDIAN)

        if (wavBytes[0] != 'R'.code.toByte() ||
            wavBytes[1] != 'I'.code.toByte() ||
            wavBytes[2] != 'F'.code.toByte() ||
            wavBytes[3] != 'F'.code.toByte()
        ) return null

        if (wavBytes[8] != 'W'.code.toByte() ||
            wavBytes[9] != 'A'.code.toByte() ||
            wavBytes[10] != 'V'.code.toByte() ||
            wavBytes[11] != 'E'.code.toByte()
        ) return null

        var pos = 12
        var sampleRate = 22050
        var channels = 1
        var bitsPerSample = 16
        var audioFormat = 1
        var dataOffset = -1
        var dataLength = 0

        while (pos + 8 <= wavBytes.size) {
            val chunkId = String(wavBytes, pos, 4, Charsets.US_ASCII)
            val chunkSize = buf.getInt(pos + 4).coerceAtLeast(0)
            val payloadStart = pos + 8
            if (payloadStart + chunkSize > wavBytes.size) break

            when (chunkId) {
                "fmt " -> if (chunkSize >= 16) {
                    audioFormat = buf.getShort(payloadStart).toInt() and 0xFFFF
                    channels = (buf.getShort(payloadStart + 2).toInt() and 0xFFFF).coerceAtLeast(1)
                    sampleRate = buf.getInt(payloadStart + 4).coerceIn(8000, 96000)
                    bitsPerSample = buf.getShort(payloadStart + 14).toInt() and 0xFFFF
                }
                "data" -> {
                    dataOffset = payloadStart
                    dataLength = chunkSize
                    break
                }
            }
            // RIFF chunks are word-aligned (padded to even byte boundary)
            pos = payloadStart + chunkSize + (chunkSize and 1)
        }

        if (audioFormat != 1 || dataOffset < 0 || dataLength <= 0) return null

        return when (bitsPerSample) {
            16 -> {
                val frameBytes = 2 * channels
                val frameCount = dataLength / frameBytes
                val mono = ShortArray(frameCount)
                for (i in 0 until frameCount) {
                    var sum = 0
                    val base = dataOffset + i * frameBytes
                    for (c in 0 until channels) {
                        sum += buf.getShort(base + c * 2).toInt()
                    }
                    mono[i] = (sum / channels).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                DecodedWavPcm(sampleRate = sampleRate, samples = mono)
            }
            8 -> {
                val frameCount = dataLength / channels
                val mono = ShortArray(frameCount)
                for (i in 0 until frameCount) {
                    var sum = 0
                    val base = dataOffset + i * channels
                    for (c in 0 until channels) {
                        val unsigned = wavBytes[base + c].toInt() and 0xFF
                        sum += (unsigned - 128) shl 8
                    }
                    mono[i] = (sum / channels).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
                DecodedWavPcm(sampleRate = sampleRate, samples = mono)
            }
            else -> null
        }
    }

    /**
     * Shapes mono 16-bit PCM voice audio with:
     * 1) Handheld 90s Pokédex bandpass EQ (240 Hz HPF + 4.6 kHz LPF)
     * 2) Subtle short comb-resonator (5.2 ms metallic computer cavity resonance)
     * 3) Gentle synthetic carrier sheen & mild retro sample-and-hold / bit quantization
     * 4) Soft limiter and click-free fade edges
     */
    fun processClassic90sVoice(input: ShortArray, sampleRate: Int): ShortArray {
        if (input.isEmpty()) return input
        val sr = sampleRate.coerceIn(8000, 96000)

        // Filter coefficients
        val hpRc = 1.0 / (2.0 * PI * 240.0)
        val dt = 1.0 / sr
        val hpAlpha = hpRc / (hpRc + dt)

        val lpRc = 1.0 / (2.0 * PI * 4600.0)
        val lpAlpha = dt / (lpRc + dt)

        // Short metallic computer comb delay (~5.2 ms)
        val combDelaySamples = ((sr * 0.0052).roundToInt()).coerceIn(16, 512)
        val combBuffer = DoubleArray(combDelaySamples)
        var combIdx = 0

        // Gentle retro sample-and-hold step for high sample rates
        val holdStep = if (sr >= 36000) 2 else 1
        var heldSample = 0.0

        var prevInput = 0.0
        var hpOut = 0.0
        var lpOut = 0.0

        val fadeSamples = (sr * 0.006).roundToInt().coerceAtLeast(1)
        val output = ShortArray(input.size)
        val quantLevels = 2048.0 // ~12-bit retro DAC texture without harsh hiss

        for (i in input.indices) {
            val raw = input[i] / 32768.0

            // 1. High-pass filter (removes muddy lows)
            hpOut = hpAlpha * (hpOut + raw - prevInput)
            prevInput = raw

            // 2. Low-pass filter (tames modern sibilance into 90s speaker bandwidth)
            lpOut += lpAlpha * (hpOut - lpOut)

            // 3. Subtle 90s hardware comb resonance (crisp computer enclosure timbre)
            val delayed = combBuffer[combIdx]
            val combResonance = lpOut + 0.24 * delayed
            combBuffer[combIdx] = lpOut + 0.18 * delayed
            combIdx = (combIdx + 1) % combDelaySamples

            // 4. Very light 115 Hz synthetic carrier sheen for robotic computer edge
            val carrier = 1.0 - 0.07 * (1.0 - sin(2.0 * PI * 115.0 * i / sr))
            val modulated = combResonance * carrier

            // 5. Mild retro DAC sample-and-hold + 12-bit quantization
            if (i % holdStep == 0) {
                heldSample = (modulated * quantLevels).roundToInt() / quantLevels
            }

            // 6. Soft-clip saturation & slight edge fade to keep audio crisp & click-free
            val gainBoosted = tanh(heldSample * 1.35) * 0.92
            val fadeIn = if (i < fadeSamples) i.toDouble() / fadeSamples else 1.0
            val fadeOut = if (i >= input.size - fadeSamples) {
                (input.size - i).toDouble() / fadeSamples
            } else {
                1.0
            }

            val finalSample = (gainBoosted * fadeIn * fadeOut * Short.MAX_VALUE)
                .roundToInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                .toShort()
            output[i] = finalSample
        }

        return output
    }

    /**
     * Generates a crisp two-tone 90s handheld Pokédex activation chirp (~110 ms)
     * that precedes classic computer voice readouts.
     */
    fun generateClassicPokedexChime(sampleRate: Int): ShortArray {
        val sr = sampleRate.coerceIn(8000, 96000)
        val pip1Samples = (sr * 42) / 1000
        val gapSamples = (sr * 16) / 1000
        val pip2Samples = (sr * 52) / 1000
        val out = ShortArray(pip1Samples + gapSamples + pip2Samples)

        fun writePip(startIdx: Int, count: Int, freqHz: Double, amp: Double) {
            val attack = (sr * 3) / 1000
            val release = (sr * 8) / 1000
            var phase = 0.0
            for (i in 0 until count) {
                phase += 2.0 * PI * freqHz / sr
                val a = if (i < attack) i.toDouble() / attack.coerceAtLeast(1) else 1.0
                val r = if (i >= count - release) (count - i).toDouble() / release.coerceAtLeast(1) else 1.0
                val decay = exp(-14.0 * i / sr)
                val wave = 0.78 * sin(phase) + 0.22 * sin(phase * 2.0)
                out[startIdx + i] = (wave * amp * a * r * decay * Short.MAX_VALUE)
                    .roundToInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    .toShort()
            }
        }

        writePip(0, pip1Samples, 1568.0, 0.28)
        writePip(pip1Samples + gapSamples, pip2Samples, 2093.0, 0.32)
        return out
    }
}
