package com.example.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.example.data.DataDexPreferences
import com.example.data.LocalPokedexCatalog
import com.example.model.PokemonEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

data class VoiceOption(
    val id: String,
    val label: String
)

/**
 * On-device Android TextToSpeech manager for speaking detected Pokémon names,
 * typing, key weaknesses, and abilities aloud, including a classic 90s synthesized
 * Pokédex computer voice mode inspired by the animated series.
 */
class DataDexVoiceSpeaker private constructor(context: Context) : TextToSpeech.OnInitListener {

    private data class PendingClassicUtterance(
        val file: File,
        val fallbackText: String,
        val token: Long
    )

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private val audioExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "DataDex-VoiceDsp").apply { isDaemon = true }
    }

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _availableVoices = MutableStateFlow<List<VoiceOption>>(
        listOf(VoiceOption("", "Default"))
    )
    val availableVoices: StateFlow<List<VoiceOption>> = _availableVoices.asStateFlow()

    private var cachedVoices: List<Voice> = emptyList()
    private var lastSpokenPokemonId: Int = -1
    private var lastSpokenTimeMillis: Long = 0L

    private val playbackGeneration = AtomicLong(0L)
    private val pendingClassicUtterances = ConcurrentHashMap<String, PendingClassicUtterance>()
    @Volatile
    private var activeRetroTrack: AudioTrack? = null

    private val utteranceListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {}

        override fun onDone(utteranceId: String?) {
            val id = utteranceId ?: return
            val pending = pendingClassicUtterances.remove(id) ?: return
            audioExecutor.execute {
                playProcessedRetroWav(pending)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            val id = utteranceId ?: return
            val pending = pendingClassicUtterances.remove(id) ?: return
            pending.file.delete()
            fallbackDirectSpeak(pending)
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            val id = utteranceId ?: return
            val pending = pendingClassicUtterances.remove(id) ?: return
            pending.file.delete()
            fallbackDirectSpeak(pending)
        }
    }

    init {
        runCatching {
            tts = TextToSpeech(appContext, this).apply {
                setOnUtteranceProgressListener(utteranceListener)
            }
        }
    }

    override fun onInit(status: Int) {
        val engine = tts ?: return
        if (status == TextToSpeech.SUCCESS) {
            val localeResult = engine.setLanguage(Locale.getDefault())
            if (localeResult == TextToSpeech.LANG_MISSING_DATA ||
                localeResult == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                engine.setLanguage(Locale.US)
            }
            engine.setOnUtteranceProgressListener(utteranceListener)
            refreshAvailableVoices(engine)
            _isReady.value = true
        } else {
            _isReady.value = false
        }
    }

    private fun refreshAvailableVoices(engine: TextToSpeech) {
        val deviceLang = Locale.getDefault().language
        val voices = runCatching {
            engine.voices
                ?.filter { !it.isNetworkConnectionRequired && (it.locale.language == deviceLang || it.locale.language == "en") }
                ?.sortedWith(compareBy({ it.locale.country }, { it.name }))
                .orEmpty()
        }.getOrDefault(emptyList())

        cachedVoices = voices
        val options = buildList {
            add(VoiceOption("", "Default"))
            voices.take(8).forEachIndexed { idx, voice ->
                val countryCode = voice.locale.country.uppercase(Locale.US)
                val shortRegion = when {
                    countryCode == "GB" -> "UK"
                    countryCode.isNotBlank() -> countryCode
                    else -> voice.locale.language.uppercase(Locale.US)
                }
                add(VoiceOption(voice.name, "Voice ${idx + 1} ($shortRegion)"))
            }
        }
        _availableVoices.value = options
    }

    fun announcePokemon(
        pokemon: PokemonEntry,
        prefs: DataDexPreferences,
        forceSpeak: Boolean = false
    ) {
        if (!forceSpeak && !prefs.voiceOutputEnabled) return
        if (!_isReady.value) return
        val engine = tts ?: return

        val now = System.currentTimeMillis()
        val cooldownMs = prefs.voiceCooldownSeconds * 1000L
        if (!forceSpeak &&
            pokemon.id == lastSpokenPokemonId &&
            (now - lastSpokenTimeMillis) < cooldownMs
        ) {
            return
        }

        lastSpokenPokemonId = pokemon.id
        lastSpokenTimeMillis = now

        val classicMode = prefs.voiceClassic90sMode
        val text = buildSpokenSummary(
            pokemon = pokemon,
            includeDetails = prefs.voiceSpeakDetails,
            includeAbilities = prefs.voiceSpeakAbilities,
            classic90sMode = classicMode
        )

        val generationToken = playbackGeneration.incrementAndGet()
        stopActiveRetroTrack()

        if (classicMode) {
            runCatching {
                engine.setSpeechRate((prefs.voiceSpeechRate * 0.94f).coerceIn(0.65f, 1.45f))
                engine.setPitch((prefs.voicePitch * 0.87f).coerceIn(0.68f, 1.25f))
            }
            applyVoiceSelection(engine, prefs.voiceName, preferClassicMaleUs = true)

            val utteranceId = "datadex_classic_${pokemon.id}_$now"
            val tempFile = File(appContext.cacheDir, "$utteranceId.wav")
            cleanupOldTempWavs()

            pendingClassicUtterances[utteranceId] = PendingClassicUtterance(
                file = tempFile,
                fallbackText = text,
                token = generationToken
            )

            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }
            val result = runCatching {
                engine.synthesizeToFile(text, params, tempFile, utteranceId)
            }.getOrDefault(TextToSpeech.ERROR)

            if (result != TextToSpeech.SUCCESS) {
                pendingClassicUtterances.remove(utteranceId)
                tempFile.delete()
                runCatching {
                    engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "datadex_fallback_${pokemon.id}_$now")
                }
            }
        } else {
            pendingClassicUtterances.clear()
            runCatching {
                engine.setSpeechRate(prefs.voiceSpeechRate.coerceIn(0.65f, 1.6f))
                engine.setPitch(prefs.voicePitch.coerceIn(0.7f, 1.4f))
                applyVoiceSelection(engine, prefs.voiceName, preferClassicMaleUs = false)
                engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "datadex_${pokemon.id}_$now")
            }
        }
    }

    private fun applyVoiceSelection(
        engine: TextToSpeech,
        requestedVoiceName: String,
        preferClassicMaleUs: Boolean
    ) {
        if (requestedVoiceName.isNotBlank()) {
            val chosen = cachedVoices.firstOrNull { it.name == requestedVoiceName }
            if (chosen != null) {
                runCatching { engine.voice = chosen }
                return
            }
        }
        if (preferClassicMaleUs) {
            val classicCandidate = selectClassic90sVoiceCandidate(cachedVoices)
            if (classicCandidate != null) {
                runCatching { engine.voice = classicCandidate }
                return
            }
        }
        runCatching {
            engine.defaultVoice?.let { engine.voice = it }
        }
    }

    private fun selectClassic90sVoiceCandidate(voices: List<Voice>): Voice? {
        if (voices.isEmpty()) return null
        val usVoices = voices.filter { it.locale.language == "en" && it.locale.country == "US" }
            .ifEmpty { voices.filter { it.locale.language == "en" } }
            .ifEmpty { voices }

        val maleHints = listOf("male", "-iom-", "-iol-", "-tpd-", "-sfg-", "-rjs-", "#male")
        return usVoices.firstOrNull { voice ->
            val lower = voice.name.lowercase(Locale.US)
            maleHints.any { hint -> lower.contains(hint) }
        } ?: usVoices.firstOrNull()
    }

    private fun playProcessedRetroWav(pending: PendingClassicUtterance) {
        val rawBytes = runCatching { pending.file.readBytes() }.getOrNull()
        pending.file.delete()

        if (pending.token != playbackGeneration.get()) return
        if (rawBytes == null) {
            fallbackDirectSpeak(pending)
            return
        }

        val decoded = RetroPokedexDsp.decodeWavToMonoPcm(rawBytes)
        if (decoded == null || decoded.samples.isEmpty()) {
            fallbackDirectSpeak(pending)
            return
        }

        val processedSpeech = RetroPokedexDsp.processClassic90sVoice(
            input = decoded.samples,
            sampleRate = decoded.sampleRate
        )
        val chime = RetroPokedexDsp.generateClassicPokedexChime(decoded.sampleRate)
        val combined = ShortArray(chime.size + processedSpeech.size)
        chime.copyInto(combined, destinationOffset = 0)
        processedSpeech.copyInto(combined, destinationOffset = chime.size)

        if (pending.token != playbackGeneration.get()) return

        playMonoPcmTrack(
            pcm = combined,
            sampleRate = decoded.sampleRate,
            token = pending.token
        )
    }

    private fun playMonoPcmTrack(pcm: ShortArray, sampleRate: Int, token: Long) {
        runCatching {
            val byteCount = pcm.size * 2
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)

            val track = AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .setBufferSizeInBytes(minBuf.coerceAtLeast(8192))
                .build()

            if (track.state != AudioTrack.STATE_INITIALIZED || token != playbackGeneration.get()) {
                track.release()
                return
            }

            activeRetroTrack = track
            track.play()

            var offset = 0
            while (offset < pcm.size && token == playbackGeneration.get()) {
                val chunk = (pcm.size - offset).coerceAtMost(minBuf / 2)
                val written = track.write(pcm, offset, chunk)
                if (written <= 0) break
                offset += written
            }

            if (token == playbackGeneration.get()) {
                val approxRemainingMs = ((minBuf.toLong() * 1000L) / (sampleRate * 2L)).coerceIn(40L, 180L)
                Thread.sleep(approxRemainingMs)
            }

            runCatching {
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.stop()
                }
                track.release()
            }
            if (activeRetroTrack === track) {
                activeRetroTrack = null
            }
            Unit
        }
    }

    private fun fallbackDirectSpeak(pending: PendingClassicUtterance) {
        if (pending.token != playbackGeneration.get()) return
        val engine = tts ?: return
        engine.speak(pending.fallbackText, TextToSpeech.QUEUE_FLUSH, null, "datadex_fb_${pending.token}")
    }

    private fun stopActiveRetroTrack() {
        val track = activeRetroTrack ?: return
        activeRetroTrack = null
        runCatching {
            if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                track.stop()
            }
            track.release()
        }
    }

    private fun cleanupOldTempWavs() {
        runCatching {
            appContext.cacheDir
                .listFiles { _, name -> name.startsWith("datadex_classic_") && name.endsWith(".wav") }
                ?.forEach { file ->
                    if (System.currentTimeMillis() - file.lastModified() > 60_000L) {
                        file.delete()
                    }
                }
        }
    }

    fun speakSample(prefs: DataDexPreferences) {
        val sample = LocalPokedexCatalog.byId[6] ?: return
        announcePokemon(sample, prefs, forceSpeak = true)
    }

    fun stop() {
        playbackGeneration.incrementAndGet()
        pendingClassicUtterances.values.forEach { it.file.delete() }
        pendingClassicUtterances.clear()
        stopActiveRetroTrack()
        runCatching { tts?.stop() }
        lastSpokenPokemonId = -1
        lastSpokenTimeMillis = 0L
    }

    companion object {
        @Volatile
        private var instance: DataDexVoiceSpeaker? = null

        fun getInstance(context: Context): DataDexVoiceSpeaker =
            instance ?: synchronized(this) {
                instance ?: DataDexVoiceSpeaker(context).also { instance = it }
            }

        fun buildSpokenSummary(
            pokemon: PokemonEntry,
            includeDetails: Boolean,
            includeAbilities: Boolean,
            classic90sMode: Boolean = false
        ): String {
            val parts = mutableListOf<String>()
            parts.add("${pokemon.name}.")

            if (classic90sMode) {
                val cat = pokemon.category.trim()
                val isSpecificSpeciesClass = cat.isNotBlank() &&
                    !cat.equals("Pokémon", ignoreCase = true) &&
                    !cat.startsWith("National Dex", ignoreCase = true) &&
                    !cat.startsWith("Tap to load", ignoreCase = true)
                if (isSpecificSpeciesClass) {
                    val prefixed = if (cat.startsWith("The ", ignoreCase = true)) cat else "The $cat"
                    parts.add("$prefixed.")
                }
            }

            if (includeDetails) {
                val typeText = pokemon.types.joinToString(" and ") { it.displayName }
                parts.add("$typeText type.")

                val profile = pokemon.weaknessProfile
                if (profile.quadWeaknesses.isNotEmpty()) {
                    val quadText = profile.quadWeaknesses.joinToString(" and ") { it.type.displayName }
                    parts.add("Four times weak to $quadText.")
                }
                if (profile.doubleWeaknesses.isNotEmpty()) {
                    val dblText = profile.doubleWeaknesses.take(3).joinToString(", ") { it.type.displayName }
                    parts.add("Weak to $dblText.")
                }
            }

            if (includeAbilities && pokemon.abilityDetails.isNotEmpty()) {
                val abilityPhrases = pokemon.abilityDetails.map { ab ->
                    if (ab.isHidden) "hidden ability ${ab.name}" else ab.name
                }
                val prefix = if (classic90sMode) "Registered abilities" else "Abilities"
                parts.add("$prefix: ${abilityPhrases.joinToString(", ")}.")
            }

            return parts.joinToString(" ")
        }
    }
}
