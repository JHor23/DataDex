package com.example.ml

import com.example.data.LocalPokedexCatalog
import com.example.data.PokemonNameDictionary
import kotlin.math.min

data class OcrCandidate(
    val pokemonId: Int,
    val matchedToken: String,
    val confidence: Float
)

data class VisionCandidate(
    val pokemonId: Int,
    val matchedLabel: String,
    val confidence: Float
)

/**
 * Pure-Kotlin matching engine that extracts Pokémon names from noisy ML Kit OCR text
 * (exact + multi-word + fuzzy Levenshtein distance) and maps ML Kit ImageLabeler classes.
 */
object PokemonMatcher {

    private val stopWords = setOf(
        "pokemon", "pokémon", "basic", "stage", "vmax", "vstar", "mega", "ex", "gx",
        "hp", "cp", "lv", "lvl", "level", "atk", "def", "spa", "spd", "spe", "bst",
        "weakness", "resistance", "retreat", "ability", "type", "stats", "scan",
        "scanning", "datadex", "overlay", "battle", "wild", "caught", "shiny",
        "normal", "fire", "water", "electric", "grass", "ice", "fighting", "poison",
        "ground", "flying", "psychic", "bug", "rock", "ghost", "dragon", "dark",
        "steel", "fairy", "tap", "hold", "close", "stop", "pause", "resume"
    )

    private val sanitizeRegex = Regex("[^A-Za-zÀ-ÿ0-9'\\-\\s.]")
    private val whitespaceRegex = Regex("\\s+")

    private val normalizedIndex: List<Pair<String, Int>> by lazy {
        PokemonNameDictionary.normalizedNameToId.entries.map { it.key to it.value }
    }

    fun matchFromTextLines(lines: List<String>): OcrCandidate? =
        matchAllFromTextLines(lines).firstOrNull()

    fun matchAllFromTextLines(lines: List<String>): List<OcrCandidate> {
        if (lines.isEmpty()) return emptyList()

        val found = linkedMapOf<Int, OcrCandidate>()
        val cleanedTokens = mutableListOf<String>()
        for (line in lines) {
            val sanitized = line
                .replace(sanitizeRegex, " ")
                .trim()
            if (sanitized.isBlank()) continue

            val words = sanitized.split(whitespaceRegex).filter { it.isNotBlank() }
            // Check 2-word combinations first (e.g., "Flutter Mane", "Tapu Koko", "Mr. Mime")
            for (i in 0 until words.size - 1) {
                val twoWord = "${words[i]} ${words[i + 1]}"
                val normalizedTwo = PokemonNameDictionary.normalizeKey(twoWord)
                if (normalizedTwo.length >= 5) {
                    PokemonNameDictionary.normalizedNameToId[normalizedTwo]?.let { id ->
                        found.putIfAbsent(id, OcrCandidate(id, twoWord, 0.98f))
                    }
                }
            }

            // Collect single words for exact & fuzzy lookup
            for (word in words) {
                val stripped = word.trim('.', '-', '\'')
                if (stripped.length >= 3 && stripped.lowercase() !in stopWords) {
                    cleanedTokens.add(stripped)
                }
            }
        }

        // 1. Exact single-word matches
        for (token in cleanedTokens) {
            val key = PokemonNameDictionary.normalizeKey(token)
            if (key.length < 3) continue
            PokemonNameDictionary.normalizedNameToId[key]?.let { id ->
                found.putIfAbsent(id, OcrCandidate(id, token, 0.96f))
            }
        }

        if (found.isNotEmpty()) {
            return found.values.toList()
        }

        // 2. Fuzzy Levenshtein match (handles OCR typos like "P1kachu" -> "Pikachu" or "Charlzard" -> "Charizard")
        var bestCandidate: OcrCandidate? = null
        for (token in cleanedTokens) {
            val key = PokemonNameDictionary.normalizeKey(token)
                .replace('0', 'o')
                .replace('1', 'i')
                .replace('5', 's')
            if (key.length < 4) continue

            // Exact check after OCR digit normalization
            PokemonNameDictionary.normalizedNameToId[key]?.let { id ->
                found.putIfAbsent(id, OcrCandidate(id, token, 0.92f))
                return found.values.toList()
            }

            val maxDist = if (key.length >= 7) 2 else 1
            for ((dictKey, id) in normalizedIndex) {
                if (kotlin.math.abs(dictKey.length - key.length) > maxDist) continue
                val dist = levenshtein(key, dictKey, maxDist)
                if (dist <= maxDist) {
                    val score = (1f - dist.toFloat() / dictKey.length.coerceAtLeast(4)) * 0.90f
                    if (bestCandidate == null || score > bestCandidate.confidence) {
                        bestCandidate = OcrCandidate(id, token, score.coerceIn(0.68f, 0.91f))
                    }
                }
            }
        }
        return listOfNotNull(bestCandidate)
    }

    fun matchFromVisionLabels(labels: List<Pair<String, Float>>): VisionCandidate? {
        for ((rawLabel, confidence) in labels.sortedByDescending { it.second }) {
            if (confidence < 0.52f) continue
            val key = rawLabel.lowercase().trim()
            LocalPokedexCatalog.visionTagToPokemonId[key]?.let { id ->
                return VisionCandidate(
                    pokemonId = id,
                    matchedLabel = rawLabel,
                    confidence = confidence.coerceIn(0.55f, 0.95f)
                )
            }
        }
        return null
    }

    private fun levenshtein(a: String, b: String, maxDist: Int): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)

        for (i in a.indices) {
            curr[0] = i + 1
            var rowMin = curr[0]
            val ai = a[i]
            for (j in b.indices) {
                val cost = if (ai == b[j]) 0 else 1
                val v = min(
                    min(curr[j] + 1, prev[j + 1] + 1),
                    prev[j] + cost
                )
                curr[j + 1] = v
                if (v < rowMin) rowMin = v
            }
            if (rowMin > maxDist) return maxDist + 1
            curr.copyInto(prev)
        }
        return prev[b.length]
    }
}
