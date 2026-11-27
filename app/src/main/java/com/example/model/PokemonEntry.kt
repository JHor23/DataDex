package com.example.model

import android.graphics.RectF
import kotlinx.serialization.Serializable

data class BaseStats(
    val hp: Int,
    val attack: Int,
    val defense: Int,
    val spAtk: Int,
    val spDef: Int,
    val speed: Int
) {
    val total: Int get() = hp + attack + defense + spAtk + spDef + speed
}

enum class DataSource {
    BUNDLED_DEX,
    POKEAPI_LIVE
}

data class NatureRecommendation(
    val name: String,
    val statDelta: String
)

data class PokemonAbilityInfo(
    val name: String,
    val isHidden: Boolean = false,
    val description: String = ""
)

data class PokemonEntry(
    val id: Int,
    val name: String,
    val types: List<PokemonType>,
    val baseStats: BaseStats,
    val abilityDetails: List<PokemonAbilityInfo> = emptyList(),
    val category: String = "Pokémon",
    val heightDm: Int = 10,
    val weightHg: Int = 100,
    val source: DataSource = DataSource.BUNDLED_DEX
) {
    val abilities: List<String>
        get() = abilityDetails.map { it.name }

    val formattedDexNumber: String
        get() = "#${id.toString().padStart(4, '0')}"

    val officialArtworkUrl: String
        get() = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/$id.png"

    val weaknessProfile: WeaknessProfile
        get() = TypeEffectivenessChart.computeWeaknessProfile(types)
}

enum class RecognitionMethod(val label: String) {
    ML_OCR("Text Scan"),
    ML_VISION("Visual Scan"),
    OCR_AND_VISION("Smart Scan"),
    MANUAL_PIN("Pinned")
}

data class DetectionOverlayBox(
    val normalizedRect: RectF,
    val label: String,
    val isPokemonMatch: Boolean
)

data class ScanMatch(
    val pokemon: PokemonEntry,
    val confidence: Float,
    val method: RecognitionMethod,
    val detectedText: String? = null,
    val visionLabels: List<String> = emptyList(),
    val detectionBoxes: List<DetectionOverlayBox> = emptyList(),
    val timestampMillis: Long = System.currentTimeMillis()
)

@Serializable
data class ScanHistoryItem(
    val pokemonId: Int,
    val pokemonName: String,
    val types: List<String>,
    val methodLabel: String,
    val confidence: Float,
    val timestampMillis: Long
)

data class OverlayHudState(
    val isOverlayRunning: Boolean = false,
    val isScanningPaused: Boolean = false,
    val isPausedForOwnApp: Boolean = false,
    val isCollapsed: Boolean = false,
    val showAbilitiesAndNatures: Boolean = false,
    val isTargetContentVisible: Boolean = true,
    val isActivelyReading: Boolean = false,
    val currentMatch: ScanMatch? = null,
    val secondaryMatches: List<PokemonEntry> = emptyList(),
    val lastScannedTextPreview: String = "",
    val lastVisionTags: List<String> = emptyList(),
    val framesProcessed: Int = 0,
    val lastScanTimestampMillis: Long = 0L,
    val lastMatchUpdatedMillis: Long = 0L,
    val statusMessage: String = "Ready to scan screen"
)
