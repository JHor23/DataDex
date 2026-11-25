package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.media.ExifInterface
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
import com.example.model.TypeEffectivenessChart
import com.example.audio.ScouterSoundEngine
import com.example.voice.DataDexVoiceSpeaker
import com.example.voice.VoiceOption
import kotlinx.coroutines.CancellationException
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
import kotlin.math.abs

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

data class CompareSuggestion(
    val pokemon: PokemonEntry,
    val reason: String
)

data class CompareUiState(
    val isVisible: Boolean = false,
    val basePokemon: PokemonEntry? = null,
    val challengerPokemon: PokemonEntry? = null,
    val searchQuery: String = "",
    val searchResults: List<PokemonEntry> = emptyList(),
    val recentSuggestions: List<PokemonEntry> = emptyList(),
    val similarSuggestions: List<CompareSuggestion> = emptyList(),
    val isLoadingPokemon: Boolean = false
)

class DataDexViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PokemonRepository.getInstance()
    private val prefsRepository = UserPreferencesRepository(application.applicationContext)
    val mlEngine = DataDexMlEngine(repository)
    private val voiceSpeaker = DataDexVoiceSpeaker.getInstance(application.applicationContext)
    private val soundEngine = ScouterSoundEngine.getInstance(application.applicationContext)

    init {
        DataDexMlEngine.ensureInitialized(application.applicationContext)
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

    private val _compareUi = MutableStateFlow(CompareUiState())
    val compareUi: StateFlow<CompareUiState> = _compareUi.asStateFlow()

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
            DataDexMlEngine.ensureInitialized(getApplication())
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
                    val mlErr = mlEngine.lastErrorMessage
                    _scannerUi.update {
                        it.copy(
                            isAnalyzingImage = false,
                            lastRawOcrText = analysis.rawTextPreview,
                            lastVisionTags = analysis.visionLabels,
                            statusBanner = if (mlErr != null && analysis.rawTextPreview.isBlank()) {
                                "ML scanner error: $mlErr"
                            } else {
                                "No Pokémon detected. Text seen: ${analysis.rawTextPreview.ifBlank { "none" }}"
                            }
                        )
                    }
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _scannerUi.update {
                    it.copy(
                        isAnalyzingImage = false,
                        statusBanner = "Scan error: ${t.localizedMessage ?: "Unable to analyze image"}"
                    )
                }
            } finally {
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
            }
        }
    }

    /**
     * Generates a synthetic screen-frame bitmap displaying a realistic battle nameplate
     * and passes it through the real ML Kit TextRecognizer + ImageLabeler pipeline.
     */
    fun testMlScreenReadOnSample(pokemonName: String, level: Int, hpText: String) {
        viewModelScope.launch {
            DataDexMlEngine.ensureInitialized(getApplication())
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
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _scannerUi.update {
                    it.copy(
                        isAnalyzingImage = false,
                        statusBanner = "Sample scan error: ${t.localizedMessage ?: "Unable to analyze"}"
                    )
                }
            } finally {
                if (!bitmap.isRecycled) {
                    bitmap.recycle()
                }
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

    fun openCompare(base: PokemonEntry) {
        val recents = buildRecentSuggestions(excludeId = base.id)
        val excludeIds = buildSet {
            add(base.id)
            recents.forEach { add(it.id) }
        }
        val similars = buildSimilarSuggestions(base = base, excludeIds = excludeIds)
        _compareUi.value = CompareUiState(
            isVisible = true,
            basePokemon = base,
            challengerPokemon = null,
            searchQuery = "",
            searchResults = emptyList(),
            recentSuggestions = recents,
            similarSuggestions = similars,
            isLoadingPokemon = false
        )
        if (base.source == DataSource.BUNDLED_DEX &&
            base.category.startsWith("Tap to load") &&
            preferences.value.enablePokeApiLiveFetch
        ) {
            viewModelScope.launch {
                val enriched = withContext(Dispatchers.IO) {
                    repository.getPokemonById(base.id, allowNetwork = true)
                }
                if (enriched != null) {
                    _compareUi.update { current ->
                        if (current.basePokemon?.id == enriched.id) {
                            current.copy(
                                basePokemon = enriched,
                                similarSuggestions = buildSimilarSuggestions(
                                    base = enriched,
                                    excludeIds = excludeIds
                                )
                            )
                        } else current
                    }
                }
            }
        }
    }

    fun updateCompareQuery(query: String) {
        val baseId = _compareUi.value.basePokemon?.id
        val results = if (query.isBlank()) {
            emptyList()
        } else {
            repository.searchLocalCatalog(query)
                .filter { it.id != baseId }
                .take(30)
        }
        _compareUi.update { it.copy(searchQuery = query, searchResults = results) }
    }

    fun selectCompareChallenger(entry: PokemonEntry) {
        _compareUi.update {
            it.copy(
                isVisible = true,
                challengerPokemon = entry,
                isLoadingPokemon = entry.source == DataSource.BUNDLED_DEX &&
                    entry.category.startsWith("Tap to load") &&
                    preferences.value.enablePokeApiLiveFetch
            )
        }
        if (entry.source == DataSource.BUNDLED_DEX &&
            entry.category.startsWith("Tap to load") &&
            preferences.value.enablePokeApiLiveFetch
        ) {
            viewModelScope.launch {
                val enriched = withContext(Dispatchers.IO) {
                    repository.getPokemonById(entry.id, allowNetwork = true)
                }
                _compareUi.update { current ->
                    if (current.challengerPokemon?.id == entry.id) {
                        current.copy(
                            challengerPokemon = enriched ?: entry,
                            isLoadingPokemon = false
                        )
                    } else current
                }
                if (enriched != null) {
                    _catalogRefreshTick.update { it + 1 }
                }
            }
        }
    }

    fun clearCompareChallenger() {
        _compareUi.update { it.copy(challengerPokemon = null, isLoadingPokemon = false) }
    }

    fun swapCompareSides() {
        _compareUi.update { state ->
            val a = state.basePokemon
            val b = state.challengerPokemon
            if (a != null && b != null) {
                val recents = buildRecentSuggestions(excludeId = b.id)
                val excludeIds = buildSet {
                    add(b.id)
                    recents.forEach { add(it.id) }
                }
                state.copy(
                    basePokemon = b,
                    challengerPokemon = a,
                    recentSuggestions = recents,
                    similarSuggestions = buildSimilarSuggestions(b, excludeIds)
                )
            } else state
        }
    }

    fun closeCompare() {
        _compareUi.update { it.copy(isVisible = false, isLoadingPokemon = false) }
    }

    private fun buildRecentSuggestions(excludeId: Int): List<PokemonEntry> {
        val seen = HashSet<Int>()
        seen.add(excludeId)
        val out = ArrayList<PokemonEntry>(6)
        for (item in scanHistory.value) {
            if (!seen.add(item.pokemonId)) continue
            val cached = repository.getAllCachedEntries().firstOrNull { it.id == item.pokemonId }
                ?: repository.searchLocalCatalog("#${item.pokemonId}").firstOrNull { it.id == item.pokemonId }
            if (cached != null) {
                out.add(cached)
                if (out.size >= 6) break
            }
        }
        return out
    }

    private fun buildSimilarSuggestions(
        base: PokemonEntry,
        excludeIds: Set<Int>
    ): List<CompareSuggestion> {
        val baseBst = base.baseStats.total
        val candidates = repository.getAllCachedEntries()
            .asSequence()
            .filter { it.id !in excludeIds && !it.category.startsWith("Tap to load") }
            .map { candidate ->
                val sharedTypes = candidate.types.count { it in base.types }
                val candMaxOffense = candidate.types.maxOfOrNull {
                    TypeEffectivenessChart.multiplierAgainst(it, base.types)
                } ?: 1f
                val baseMaxOffense = base.types.maxOfOrNull {
                    TypeEffectivenessChart.multiplierAgainst(it, candidate.types)
                } ?: 1f
                val bstDiff = abs(candidate.baseStats.total - baseBst)
                val score = (sharedTypes * 40) +
                    (if (candMaxOffense >= 2f || baseMaxOffense >= 2f) 24 else 0) +
                    (25 - (bstDiff / 10)).coerceAtLeast(0)

                val reason = when {
                    sharedTypes > 0 -> "Same Type"
                    candMaxOffense >= 2f -> "Type Counter"
                    baseMaxOffense >= 2f -> "Type Target"
                    else -> "Similar BST"
                }
                score to CompareSuggestion(candidate, reason)
            }
            .sortedByDescending { it.first }
            .take(8)
            .map { it.second }
            .toList()
        return candidates
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
            if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) {
                return null
            }
            val orientation = runCatching {
                resolver.openInputStream(uri)?.use { stream ->
                    ExifInterface(stream).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                }
            }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

            val maxDim = maxOf(boundsOpts.outWidth, boundsOpts.outHeight).coerceAtLeast(1)
            var sampleSize = 1
            while (maxDim / sampleSize > 1440) {
                sampleSize *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: return null

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.postRotate(90f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.postRotate(270f)
                    matrix.postScale(-1f, 1f)
                }
            }
            if (matrix.isIdentity) {
                decoded
            } else {
                runCatching {
                    val rotated = Bitmap.createBitmap(
                        decoded,
                        0,
                        0,
                        decoded.width,
                        decoded.height,
                        matrix,
                        true
                    )
                    if (rotated !== decoded && !decoded.isRecycled) {
                        decoded.recycle()
                    }
                    rotated
                }.getOrDefault(decoded)
            }
        } catch (_: Throwable) {
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
