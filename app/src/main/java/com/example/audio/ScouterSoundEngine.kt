package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.data.DataDexPreferences
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Pure-Kotlin procedural sci-fi sound synthesizer inspired by a tactical DBZ Scouter visor.
 * Generates subtle, click-free PCM cues and plays them off the main thread via AudioTrack.
 */
class ScouterSoundEngine private constructor(context: Context) {

    @Suppress("unused")
    private val appContext = context.applicationContext
    private val audioExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "DataDex-ScouterAudio").apply { isDaemon = true }
    }

    private var detectedTrack: AudioTrack? = null
    private var updateTrack: AudioTrack? = null
    private var scanPulseTrack: AudioTrack? = null

    private var lastDetectedPokemonId: Int = -1
    private var lastDetectedAtMillis: Long = 0L
    private var lastUpdateAtMillis: Long = 0L

    fun playPokemonDetected(
        pokemonId: Int,
        prefs: DataDexPreferences,
        forcePlay: Boolean = false
    ) {
        if (!prefs.scouterSfxEnabled || !prefs.scouterSfxOnDetect) return
        val now = System.currentTimeMillis()
        synchronized(this) {
            if (!forcePlay &&
                pokemonId == lastDetectedPokemonId &&
                (now - lastDetectedAtMillis) < DETECT_REPEAT_COOLDOWN_MS
            ) {
                return
            }
            lastDetectedPokemonId = pokemonId
            lastDetectedAtMillis = now
        }
        val gain = perceptualGain(prefs.scouterSfxVolume)
        audioExecutor.execute {
            val track = getOrCreateDetectedTrack() ?: return@execute
            playStaticTrack(track, gain)
        }
    }

    fun playOverlayUpdated(
        prefs: DataDexPreferences,
        forcePlay: Boolean = false
    ) {
        if (!prefs.scouterSfxEnabled || !prefs.scouterSfxOnOverlayUpdate) return
        val now = System.currentTimeMillis()
        synchronized(this) {
            if (!forcePlay && (now - lastUpdateAtMillis) < UPDATE_DEBOUNCE_MS) {
                return
            }
            lastUpdateAtMillis = now
        }
        val gain = perceptualGain(prefs.scouterSfxVolume) * 0.78f
        audioExecutor.execute {
            val track = getOrCreateUpdateTrack() ?: return@execute
            playStaticTrack(track, gain)
        }
    }

    fun playScanPulse(prefs: DataDexPreferences) {
        if (!prefs.scouterSfxEnabled || !prefs.scouterSfxOnOverlayUpdate) return
        val gain = perceptualGain(prefs.scouterSfxVolume) * 0.65f
        audioExecutor.execute {
            val track = getOrCreateScanPulseTrack() ?: return@execute
            playStaticTrack(track, gain)
        }
    }

    fun playPreview(prefs: DataDexPreferences) {
        val previewVolume = prefs.scouterSfxVolume.coerceAtLeast(0.25f)
        val gain = perceptualGain(previewVolume)
        audioExecutor.execute {
            val track = getOrCreateDetectedTrack() ?: return@execute
            playStaticTrack(track, gain)
        }
    }

    fun resetDetectionLock() {
        synchronized(this) {
            lastDetectedPokemonId = -1
            lastDetectedAtMillis = 0L
        }
    }

    private fun getOrCreateDetectedTrack(): AudioTrack? {
        detectedTrack?.let { return it }
        val pcm = generateScouterDetectedPcm()
        return createStaticTrack(pcm)?.also { detectedTrack = it }
    }

    private fun getOrCreateUpdateTrack(): AudioTrack? {
        updateTrack?.let { return it }
        val pcm = generateScouterUpdatePcm()
        return createStaticTrack(pcm)?.also { updateTrack = it }
    }

    private fun getOrCreateScanPulseTrack(): AudioTrack? {
        scanPulseTrack?.let { return it }
        val pcm = generateScanPulsePcm()
        return createStaticTrack(pcm)?.also { scanPulseTrack = it }
    }

    private fun createStaticTrack(pcm: ShortArray): AudioTrack? {
        return runCatching {
            val byteCount = pcm.size * 2
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(byteCount)
                .build()
                .apply {
                    write(pcm, 0, pcm.size)
                }
        }.getOrNull()
    }

    private fun playStaticTrack(track: AudioTrack, gain: Float) {
        runCatching {
            if (track.state != AudioTrack.STATE_INITIALIZED) return
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                track.stop()
            }
            track.reloadStaticData()
            track.setVolume(gain.coerceIn(0f, 1f))
            track.play()
        }
    }

    companion object {
        const val SAMPLE_RATE = 44_100
        private const val DETECT_REPEAT_COOLDOWN_MS = 12_000L
        private const val UPDATE_DEBOUNCE_MS = 130L

        @Volatile
        private var instance: ScouterSoundEngine? = null

        fun getInstance(context: Context): ScouterSoundEngine =
            instance ?: synchronized(this) {
                instance ?: ScouterSoundEngine(context).also { instance = it }
            }

        internal fun perceptualGain(volume: Float): Float {
            val clamped = volume.coerceIn(0f, 1f)
            return clamped * clamped * 0.65f
        }

        /**
         * Iconic 3-step ascending DBZ-Scouter lock-on chirp (~248 ms):
         * Two crisp telemetry pips (1318 Hz -> 1760 Hz) followed by a shimmering C7/E7 lock tone.
         */
        fun generateScouterDetectedPcm(): ShortArray {
            val seg1 = synthTone(
                durationMs = 42,
                startFreqHz = 1318.5,
                endFreqHz = 1340.0,
                amplitude = 0.42,
                harmonic2Mix = 0.16,
                decayRate = 14.0
            )
            val gapSamples = msToSamples(20)
            val seg2 = synthTone(
                durationMs = 42,
                startFreqHz = 1760.0,
                endFreqHz = 1790.0,
                amplitude = 0.46,
                harmonic2Mix = 0.16,
                decayRate = 14.0
            )
            val lockTone = synthLockChime(durationMs = 124)
            val out = ShortArray(seg1.size + gapSamples + seg2.size + gapSamples + lockTone.size)
            var offset = 0
            seg1.copyInto(out, offset)
            offset += seg1.size + gapSamples
            seg2.copyInto(out, offset)
            offset += seg2.size + gapSamples
            lockTone.copyInto(out, offset)
            return out
        }

        /**
         * Subtle dual micro-tick telemetry update sound (~84 ms) for overlay refreshes and toggles.
         */
        fun generateScouterUpdatePcm(): ShortArray {
            val tick1 = synthTone(
                durationMs = 26,
                startFreqHz = 1046.5,
                endFreqHz = 1244.5,
                amplitude = 0.34,
                harmonic2Mix = 0.12,
                decayRate = 20.0
            )
            val gapSamples = msToSamples(14)
            val tick2 = synthTone(
                durationMs = 44,
                startFreqHz = 1480.0,
                endFreqHz = 1520.0,
                amplitude = 0.38,
                harmonic2Mix = 0.14,
                decayRate = 24.0
            )
            val out = ShortArray(tick1.size + gapSamples + tick2.size)
            tick1.copyInto(out, 0)
            tick2.copyInto(out, tick1.size + gapSamples)
            return out
        }

        /**
         * Subtle short radar sweep pip (~56 ms) for manual scan triggers.
         */
        fun generateScanPulsePcm(): ShortArray =
            synthTone(
                durationMs = 56,
                startFreqHz = 880.0,
                endFreqHz = 1320.0,
                amplitude = 0.30,
                harmonic2Mix = 0.10,
                decayRate = 22.0
            )

        private fun msToSamples(ms: Int): Int = (SAMPLE_RATE * ms) / 1000

        private fun synthTone(
            durationMs: Int,
            startFreqHz: Double,
            endFreqHz: Double,
            amplitude: Double,
            harmonic2Mix: Double,
            decayRate: Double
        ): ShortArray {
            val sampleCount = msToSamples(durationMs)
            val out = ShortArray(sampleCount)
            val attackSamples = msToSamples(4).coerceAtLeast(1)
            val releaseSamples = msToSamples(8).coerceAtLeast(1)
            var phase = 0.0
            var phase2 = 0.0

            for (i in 0 until sampleCount) {
                val progress = i.toDouble() / sampleCount.coerceAtLeast(1)
                val freq = startFreqHz + (endFreqHz - startFreqHz) * progress
                val phaseStep = 2.0 * PI * freq / SAMPLE_RATE
                phase += phaseStep
                phase2 += phaseStep * 2.0

                val attackEnv = if (i < attackSamples) i.toDouble() / attackSamples else 1.0
                val releaseEnv = if (i >= sampleCount - releaseSamples) {
                    (sampleCount - i).toDouble() / releaseSamples
                } else {
                    1.0
                }
                val decayEnv = exp(-decayRate * (i.toDouble() / SAMPLE_RATE))
                val env = attackEnv * releaseEnv * decayEnv

                val wave = sin(phase) + harmonic2Mix * sin(phase2)
                val normalized = (wave / (1.0 + harmonic2Mix)) * amplitude * env
                out[i] = (normalized * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    .toShort()
            }
            return out
        }

        private fun synthLockChime(durationMs: Int): ShortArray {
            val sampleCount = msToSamples(durationMs)
            val out = ShortArray(sampleCount)
            val attackSamples = msToSamples(5).coerceAtLeast(1)
            val releaseSamples = msToSamples(18).coerceAtLeast(1)
            var pMain = 0.0
            var pHarmonic = 0.0
            var pSub = 0.0

            for (i in 0 until sampleCount) {
                val t = i.toDouble() / SAMPLE_RATE
                val freqMain = 2093.0
                val freqHarmonic = 2637.0
                val freqSub = 1046.5
                pMain += 2.0 * PI * freqMain / SAMPLE_RATE
                pHarmonic += 2.0 * PI * freqHarmonic / SAMPLE_RATE
                pSub += 2.0 * PI * freqSub / SAMPLE_RATE

                val attackEnv = if (i < attackSamples) i.toDouble() / attackSamples else 1.0
                val releaseEnv = if (i >= sampleCount - releaseSamples) {
                    (sampleCount - i).toDouble() / releaseSamples
                } else {
                    1.0
                }
                val decayEnv = exp(-18.0 * t)
                val env = attackEnv * releaseEnv * decayEnv

                val wave = 0.65 * sin(pMain) + 0.22 * sin(pHarmonic) + 0.13 * sin(pSub)
                val normalized = wave * 0.50 * env
                out[i] = (normalized * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    .toShort()
            }
            return out
        }
    }
}
