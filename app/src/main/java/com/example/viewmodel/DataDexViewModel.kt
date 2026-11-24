package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DataDexPreferences
import com.example.data.DataDexStateHolder
import com.example.data.PokemonRepository
import com.example.data.UserPreferencesRepository
import com.example.ml.DataDexMlEngine
import com.example.model.DataSource
import com.example.model.OverlayHudState
import com.example.model.PokemonEntry
import com.example.model.PokemonType
import com.example.model.RecognitionMethod
import com.example.model.ScanHistoryItem
import com.example.model.ScanMatch
import com.example.audio.ScouterSoundEngine
import com.example.voice.DataDexVoiceSpeaker
import com.example.voice.VoiceOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ScannerUiState(
    val isAnalyzingImage: Boolean = false,
    val lastScanMatch: ScanMatch? = null,
    val lastRawOcrText: String = "",
    val lastVisionTags: List<String> = emptyList(),
    val statusBanner: String? = null
)

data class PokedexUiState(
    val query: String = "",
    val selectedType: PokemonType? = null,
    val entries: List<PokemonEntry> = emptyList(),
    val selectedDetail: PokemonEntry? = null,
    val isLoadingDetail: Boolean = false
)

class DataDexViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PokemonRepository.getInstance()
    private val prefsRepository = UserPreferencesRepository(application.applicationContext)
    val mlEngine = DataDexMlEngine(repository)
    private val voiceSpeaker = DataDexVoiceSpeaker.getInstance(application.applicationContext)
    private val soundEngine = ScouterSoundEngine.getInstance(application.applicationContext)

    init {
        DataDexMlEngine.prepareNativeStorage(application.applicationContext)
    }

    val overlayState: StateFlow<OverlayHudState> = DataDexStateHolder.overlayState

    val availableVoices: StateFlow<List<VoiceOption>> = voiceSpeaker.availableVoices
    val isVoiceReady: StateFlow<Boolean> = voiceSpeaker.isReady

    val preferences: StateFlow<DataDexPreferences> = prefsRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DataDexPreferences())

    val scanHistory: StateFlow<List<ScanHistoryItem>> = prefsRepository.scanHistoryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _scannerUi = MutableStateFlow(ScannerUiState())
    val scannerUi: StateFlow<ScannerUiState> = _scannerUi.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _typeFilter = MutableStateFlow<PokemonType?>(null)
    private val _selectedDetail = MutableStateFlow<PokemonEntry?>(null)
    private val _isLoadingDetail = MutableStateFlow(false)
    private val _catalogRefreshTick = MutableStateFlow(0)

    val pokedexUi: StateFlow<PokedexUiState> = combine(
        _searchQuery,
        _typeFilter,
        _selectedDetail,
        _isLoadingDetail,
        _catalogRefreshTick
    ) { query, typeFilter, selected, loading, _ ->
        PokedexUiState(
            query = query,
            selectedType = typeFilter,
            entries = repository.searchLocalCatalog(query, typeFilter),
            selectedDetail = selected,
            isLoadingDetail = loading
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        PokedexUiState(entries = repository.getBundledEntries())
    )

    fun analyzePhotoUri(uri: Uri) {
        viewModelScope.launch {
            _scannerUi.update { it.copy(isAnalyzingImage = true, statusBanner = "Scanning image for Pokémon...") }
            val bitmap = withContext(Dispatchers.IO) { decodeScaledBitmap(uri) }
            if (bitmap == null) {
                _scannerUi.update { it.copy(isAnalyzingImage = false, statusBanner = "Could not decode selected image.") }
                return@launch
            }
            try {
                val prefs = preferences.value
                val analysis = withContext(Dispatchers.Default) {
                    mlEngine.analyzeBitmap(
                        bitmap = bitmap,
                        enableVisionFallback = prefs.enableVisionFallback,
                        allowNetworkLookup = prefs.enablePokeApiLiveFetch
                    )
                }
                val match = analysis.match
                if (match != null) {
                    DataDexStateHolder.setOverlayMatch(match, analysis.rawTextPreview, analysis.visionLabels)
                    prefsRepository.recordScan(match)
                    soundEngine.playPokemonDetected(match.pokemon.id, prefs, forcePlay = true)
                    voiceSpeaker.announcePokemon(match.pokemon, prefs)
                    _scannerUi.update {
                        it.copy(
                            isAnalyzingImage = false,
                            lastScanMatch = match,
                            lastRawOcrText = analysis.rawTextPreview,
                            lastVisionTags = analysis.visionLabels,
                            statusBanner = "Matched ${match.pokemon.name} via ${match.method.label}"
                        )
                    }
                } else {
                    _scannerUi.update {
                        it.copy(
                            isAnalyzingImage = false,
                            lastRawOcrText = analysis.rawTextPreview,
                            lastVisionTags = analysis.visionLabels,
                            statusBanner = "No Pokémon detected. Text seen: ${analysis.rawTextPreview.ifBlank { "none" }}"
                        )
                    }
                }
            } finally {
                bitmap.recycle()
            }
        }
    }

    /**
     * Generates a synthetic screen-frame bitmap displaying a realistic battle nameplate
     * and passes it through the real ML Kit TextRecognizer + ImageLabeler pipeline.
     */
    fun testMlScreenReadOnSample(pokemonName: String, level: Int, hpText: String) {
        viewModelScope.launch {
            _scannerUi.update { it.copy(isAnalyzingImage = true, statusBanner = "Scanning sample card...") }
            val bitmap = withContext(Dispatchers.Default) {
                renderSampleBattleFrameBitmap(pokemonName, level, hpText)
            }
            try {
                val prefs = preferences.value
                val analysis = withContext(Dispatchers.Default) {
                    mlEngine.analyzeBitmap(
                        bitmap = bitmap,
                        enableVisionFallback = prefs.enableVisionFallback,
                        allowNetworkLookup = prefs.enablePokeApiLiveFetch
                    )
                }
                val match = analysis.match
                if (match != null) {
                    DataDexStateHolder.setOverlayMatch(match, analysis.rawTextPreview, analysis.visionLabels)
                    prefsRepository.recordScan(match)
                    soundEngine.playPokemonDetected(match.pokemon.id, prefs, forcePlay = true)
                    voiceSpeaker.announcePokemon(match.pokemon, prefs)
                    _scannerUi.update {
                        it.copy(
                            isAnalyzingImage = false,
                            lastScanMatch = match,
                            lastRawOcrText = analysis.rawTextPreview,
                            lastVisionTags = analysis.visionLabels,
                            statusBanner = "Detected \"${match.detectedText ?: match.pokemon.name}\""
                        )
                    }
                } else {
                    _scannerUi.update {
                        it.copy(
                            isAnalyzingImage = false,
                            lastRawOcrText = analysis.rawTextPreview,
                            statusBanner = "Could not match any Pokémon name."
                        )
                    }
                }
            } finally {
                bitmap.recycle()
            }
        }
    }

    fun onCameraDetection(match: ScanMatch, rawText: String, visionTags: List<String>) {
        viewModelScope.launch {
            val current = _scannerUi.value.lastScanMatch
            val shouldRecord = current == null ||
                current.pokemon.id != match.pokemon.id ||
                (match.timestampMillis - current.timestampMillis) > 10_000L

            _scannerUi.update {
                it.copy(
                    lastScanMatch = match,
                    lastRawOcrText = rawText,
                    lastVisionTags = visionTags,
                    statusBanner = "Camera spotted: ${match.pokemon.name}"
                )
            }
            DataDexStateHolder.setOverlayMatch(match, rawText, visionTags)
            val prefs = preferences.value
            soundEngine.playPokemonDetected(match.pokemon.id, prefs, forcePlay = false)
            voiceSpeaker.announcePokemon(match.pokemon, prefs)
            if (shouldRecord) {
                prefsRepository.recordScan(match)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleTypeFilter(type: PokemonType) {
        _typeFilter.value = if (_typeFilter.value == type) null else type
    }

    fun selectPokemonForDetail(entry: PokemonEntry) {
        _selectedDetail.value = entry
        if (entry.source == DataSource.BUNDLED_DEX &&
            entry.category.startsWith("Tap to load") &&
            preferences.value.enablePokeApiLiveFetch
        ) {
            viewModelScope.launch {
                _isLoadingDetail.value = true
                val enriched = withContext(Dispatchers.IO) {
                    repository.getPokemonById(entry.id, allowNetwork = true)
                }
                if (enriched != null) {
                    _selectedDetail.value = enriched
                    _catalogRefreshTick.update { it + 1 }
                }
                _isLoadingDetail.value = false
            }
        }
    }

    fun selectPokemonById(id: Int) {
        viewModelScope.launch {
            _isLoadingDetail.value = true
            val entry = withContext(Dispatchers.IO) {
                repository.getPokemonById(id, allowNetwork = preferences.value.enablePokeApiLiveFetch)
            }
            _selectedDetail.value = entry
            _isLoadingDetail.value = false
        }
    }

    fun dismissPokemonDetail() {
        _selectedDetail.value = null
    }

    fun pinPokemonToOverlay(entry: PokemonEntry) {
        val match = ScanMatch(
            pokemon = entry,
            confidence = 1.0f,
            method = RecognitionMethod.MANUAL_PIN,
            detectedText = entry.name
        )
        DataDexStateHolder.setOverlayMatch(match, entry.name)
        soundEngine.playOverlayUpdated(preferences.value, forcePlay = true)
        _scannerUi.update {
            it.copy(
                lastScanMatch = match,
                statusBanner = "Pinned ${entry.name} to Overlay"
            )
        }
    }

    fun toggleOverlayPause() {
        DataDexStateHolder.updateOverlayState {
            it.copy(isScanningPaused = !it.isScanningPaused)
        }
        soundEngine.playOverlayUpdated(preferences.value)
    }

    fun toggleOverlayAbilities() {
        DataDexStateHolder.toggleOverlayAbilities()
        soundEngine.playOverlayUpdated(preferences.value)
    }

    fun updateScanInterval(intervalMs: Int) {
        viewModelScope.launch { prefsRepository.updateScanInterval(intervalMs) }
    }

    fun updateOverlayOpacity(opacity: Float) {
        viewModelScope.launch { prefsRepository.updateOverlayOpacity(opacity) }
    }

    fun setVisionFallbackEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setVisionFallbackEnabled(enabled) }
    }

    fun setPokeApiLiveEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setPokeApiLiveEnabled(enabled) }
    }

    fun setVoiceOutputEnabled(enabled: Boolean) {
        if (!enabled) voiceSpeaker.stop()
        viewModelScope.launch { prefsRepository.setVoiceOutputEnabled(enabled) }
    }

    fun setVoiceSpeakDetails(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setVoiceSpeakDetails(enabled) }
    }

    fun setVoiceSpeakAbilities(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setVoiceSpeakAbilities(enabled) }
    }

    fun updateVoiceSpeechRate(rate: Float) {
        viewModelScope.launch { prefsRepository.updateVoiceSpeechRate(rate) }
    }

    fun updateVoicePitch(pitch: Float) {
        viewModelScope.launch { prefsRepository.updateVoicePitch(pitch) }
    }

    fun updateVoiceCooldownSeconds(seconds: Int) {
        viewModelScope.launch { prefsRepository.updateVoiceCooldownSeconds(seconds) }
    }

    fun updateVoiceName(voiceName: String) {
        viewModelScope.launch { prefsRepository.updateVoiceName(voiceName) }
    }

    fun setVoiceClassic90sMode(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setVoiceClassic90sMode(enabled) }
    }

    fun testVoiceOutput() {
        voiceSpeaker.speakSample(preferences.value)
    }

    fun setScouterSfxEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setScouterSfxEnabled(enabled) }
    }

    fun updateScouterSfxVolume(volume: Float) {
        viewModelScope.launch { prefsRepository.updateScouterSfxVolume(volume) }
    }

    fun setScouterSfxOnDetect(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setScouterSfxOnDetect(enabled) }
    }

    fun setScouterSfxOnOverlayUpdate(enabled: Boolean) {
        viewModelScope.launch { prefsRepository.setScouterSfxOnOverlayUpdate(enabled) }
    }

    fun testScouterSfx() {
        soundEngine.playPreview(preferences.value)
    }

    fun clearHistory() {
        viewModelScope.launch { prefsRepository.clearHistory() }
    }

    private fun decodeScaledBitmap(uri: Uri): Bitmap? {
        return try {
            val resolver = getApplication<Application>().contentResolver
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOpts) }
            val maxDim = maxOf(boundsOpts.outWidth, boundsOpts.outHeight).coerceAtLeast(1)
            var sampleSize = 1
            while (maxDim / sampleSize > 1080) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOpts) }
        } catch (_: Exception) {
            null
        }
    }

    private fun renderSampleBattleFrameBitmap(name: String, level: Int, hpText: String): Bitmap {
        val width = 720
        val height = 320
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                Color.rgb(16, 26, 48),
                Color.rgb(30, 44, 78),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val cardPaint = Paint().apply {
            color = Color.rgb(245, 248, 255)
            isAntiAlias = true
        }
        canvas.drawRoundRect(40f, 44f, width - 40f, height - 44f, 24f, 24f, cardPaint)

        val namePaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 54f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText(name, 78f, 145f, namePaint)

        val metaPaint = Paint().apply {
            color = Color.rgb(71, 85, 105)
            textSize = 34f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("Lv.$level   $hpText", 78f, 215f, metaPaint)

        val hpBarBg = Paint().apply { color = Color.rgb(203, 213, 225) }
        val hpBarFg = Paint().apply { color = Color.rgb(34, 197, 94) }
        canvas.drawRoundRect(78f, 240f, width - 78f, 260f, 10f, 10f, hpBarBg)
        canvas.drawRoundRect(78f, 240f, width - 160f, 260f, 10f, 10f, hpBarFg)

        return bmp
    }

    override fun onCleared() {
        mlEngine.close()
        super.onCleared()
    }
}
