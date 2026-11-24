package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.ScanHistoryItem
import com.example.model.ScanMatch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataDexDataStore: DataStore<Preferences> by preferencesDataStore(name = "datadex_prefs")

data class DataDexPreferences(
    val scanIntervalMs: Int = 1400,
    val overlayOpacity: Float = 0.94f,
    val enableVisionFallback: Boolean = true,
    val enablePokeApiLiveFetch: Boolean = true,
    val voiceOutputEnabled: Boolean = false,
    val voiceSpeakDetails: Boolean = true,
    val voiceSpeakAbilities: Boolean = false,
    val voiceSpeechRate: Float = 1.0f,
    val voicePitch: Float = 1.0f,
    val voiceCooldownSeconds: Int = 5,
    val voiceName: String = "",
    val voiceClassic90sMode: Boolean = false,
    val scouterSfxEnabled: Boolean = true,
    val scouterSfxVolume: Float = 0.50f,
    val scouterSfxOnDetect: Boolean = true,
    val scouterSfxOnOverlayUpdate: Boolean = true
)

class UserPreferencesRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val SCAN_INTERVAL_MS = intPreferencesKey("scan_interval_ms")
        val OVERLAY_OPACITY = floatPreferencesKey("overlay_opacity")
        val ENABLE_VISION_FALLBACK = booleanPreferencesKey("enable_vision_fallback")
        val ENABLE_POKEAPI_LIVE = booleanPreferencesKey("enable_pokeapi_live")
        val SCAN_HISTORY_JSON = stringPreferencesKey("scan_history_json")
        val VOICE_OUTPUT_ENABLED = booleanPreferencesKey("voice_output_enabled")
        val VOICE_SPEAK_DETAILS = booleanPreferencesKey("voice_speak_details")
        val VOICE_SPEAK_ABILITIES = booleanPreferencesKey("voice_speak_abilities")
        val VOICE_SPEECH_RATE = floatPreferencesKey("voice_speech_rate")
        val VOICE_PITCH = floatPreferencesKey("voice_pitch")
        val VOICE_COOLDOWN_SECONDS = intPreferencesKey("voice_cooldown_seconds")
        val VOICE_NAME = stringPreferencesKey("voice_name")
        val VOICE_CLASSIC_90S_MODE = booleanPreferencesKey("voice_classic_90s_mode")
        val SCOUTER_SFX_ENABLED = booleanPreferencesKey("scouter_sfx_enabled")
        val SCOUTER_SFX_VOLUME = floatPreferencesKey("scouter_sfx_volume")
        val SCOUTER_SFX_ON_DETECT = booleanPreferencesKey("scouter_sfx_on_detect")
        val SCOUTER_SFX_ON_OVERLAY_UPDATE = booleanPreferencesKey("scouter_sfx_on_overlay_update")
    }

    val preferencesFlow: Flow<DataDexPreferences> = context.dataDexDataStore.data.map { prefs ->
        DataDexPreferences(
            scanIntervalMs = prefs[Keys.SCAN_INTERVAL_MS] ?: 1400,
            overlayOpacity = prefs[Keys.OVERLAY_OPACITY] ?: 0.94f,
            enableVisionFallback = prefs[Keys.ENABLE_VISION_FALLBACK] ?: true,
            enablePokeApiLiveFetch = prefs[Keys.ENABLE_POKEAPI_LIVE] ?: true,
            voiceOutputEnabled = prefs[Keys.VOICE_OUTPUT_ENABLED] ?: false,
            voiceSpeakDetails = prefs[Keys.VOICE_SPEAK_DETAILS] ?: true,
            voiceSpeakAbilities = prefs[Keys.VOICE_SPEAK_ABILITIES] ?: false,
            voiceSpeechRate = prefs[Keys.VOICE_SPEECH_RATE] ?: 1.0f,
            voicePitch = prefs[Keys.VOICE_PITCH] ?: 1.0f,
            voiceCooldownSeconds = prefs[Keys.VOICE_COOLDOWN_SECONDS] ?: 5,
            voiceName = prefs[Keys.VOICE_NAME] ?: "",
            voiceClassic90sMode = prefs[Keys.VOICE_CLASSIC_90S_MODE] ?: false,
            scouterSfxEnabled = prefs[Keys.SCOUTER_SFX_ENABLED] ?: true,
            scouterSfxVolume = prefs[Keys.SCOUTER_SFX_VOLUME] ?: 0.50f,
            scouterSfxOnDetect = prefs[Keys.SCOUTER_SFX_ON_DETECT] ?: true,
            scouterSfxOnOverlayUpdate = prefs[Keys.SCOUTER_SFX_ON_OVERLAY_UPDATE] ?: true
        )
    }

    val scanHistoryFlow: Flow<List<ScanHistoryItem>> = context.dataDexDataStore.data.map { prefs ->
        val raw = prefs[Keys.SCAN_HISTORY_JSON] ?: return@map emptyList()
        runCatching { json.decodeFromString<List<ScanHistoryItem>>(raw) }.getOrDefault(emptyList())
    }

    suspend fun updateScanInterval(intervalMs: Int) {
        context.dataDexDataStore.edit { it[Keys.SCAN_INTERVAL_MS] = intervalMs.coerceIn(750, 4000) }
    }

    suspend fun updateOverlayOpacity(opacity: Float) {
        context.dataDexDataStore.edit { it[Keys.OVERLAY_OPACITY] = opacity.coerceIn(0.65f, 1.0f) }
    }

    suspend fun setVisionFallbackEnabled(enabled: Boolean) {
        context.dataDexDataStore.edit { it[Keys.ENABLE_VISION_FALLBACK] = enabled }
    }

    suspend fun setPokeApiLiveEnabled(enabled: Boolean) {
        context.dataDexDataStore.edit { it[Keys.ENABLE_POKEAPI_LIVE] = enabled }
    }

    suspend fun setVoiceOutputEnabled(enabled: Boolean) {
        context.dataDexDataStore.edit { it[Keys.VOICE_OUTPUT_ENABLED] = enabled }
    }

    suspend fun setVoiceSpeakDetails(enabled: Boolean) {
        context.dataDexDataStore.edit { it[Keys.VOICE_SPEAK_DETAILS] = enabled }
    }

    suspend fun setVoiceSpeakAbilities(enabled: Boolean) {
        context.dataDexDataStore.edit { it[Keys.VOICE_SPEAK_ABILITIES] = enabled }
    }

    suspend fun updateVoiceSpeechRate(rate: Float) {
        context.dataDexDataStore.edit { it[Keys.VOICE_SPEECH_RATE] = rate.coerceIn(0.65f, 1.6f) }
    }

    suspend fun updateVoicePitch(pitch: Float) {
        context.dataDexDataStore.edit { it[Keys.VOICE_PITCH] = pitch.coerceIn(0.7f, 1.4f) }
    }

    suspend fun updateVoiceCooldownSeconds(seconds: Int) {
        context.dataDexDataStore.edit { it[Keys.VOICE_COOLDOWN_SECONDS] = seconds.coerceIn(2, 15) }
    }

    suspend fun updateVoiceName(voiceName: String) {
        context.dataDexDataStore.edit { it[Keys.VOICE_NAME] = voiceName }
    }

    suspend fun setVoiceClassic90sMode(enabled: Boolean) {
        context.dataDexDataStore.edit {
            it[Keys.VOICE_CLASSIC_90S_MODE] = enabled
            if (enabled) {
                it[Keys.VOICE_OUTPUT_ENABLED] = true
            }
        }
    }

    suspend fun setScouterSfxEnabled(enabled: Boolean) {
        context.dataDexDataStore.edit { it[Keys.SCOUTER_SFX_ENABLED] = enabled }
    }

    suspend fun updateScouterSfxVolume(volume: Float) {
        context.dataDexDataStore.edit { it[Keys.SCOUTER_SFX_VOLUME] = volume.coerceIn(0.1f, 1.0f) }
    }

    suspend fun setScouterSfxOnDetect(enabled: Boolean) {
        context.dataDexDataStore.edit { it[Keys.SCOUTER_SFX_ON_DETECT] = enabled }
    }

    suspend fun setScouterSfxOnOverlayUpdate(enabled: Boolean) {
        context.dataDexDataStore.edit { it[Keys.SCOUTER_SFX_ON_OVERLAY_UPDATE] = enabled }
    }

    suspend fun recordScan(match: ScanMatch) {
        val newItem = ScanHistoryItem(
            pokemonId = match.pokemon.id,
            pokemonName = match.pokemon.name,
            types = match.pokemon.types.map { it.name },
            methodLabel = match.method.label,
            confidence = match.confidence,
            timestampMillis = match.timestampMillis
        )
        context.dataDexDataStore.edit { prefs ->
            val current = prefs[Keys.SCAN_HISTORY_JSON]?.let { raw ->
                runCatching { json.decodeFromString<List<ScanHistoryItem>>(raw) }.getOrDefault(emptyList())
            } ?: emptyList()

            val deduplicated = current.filterNot {
                it.pokemonId == newItem.pokemonId &&
                    (newItem.timestampMillis - it.timestampMillis) < 15_000L
            }
            val updated = (listOf(newItem) + deduplicated).take(40)
            prefs[Keys.SCAN_HISTORY_JSON] = json.encodeToString(updated)
        }
    }

    suspend fun clearHistory() {
        context.dataDexDataStore.edit { it.remove(Keys.SCAN_HISTORY_JSON) }
    }
}
