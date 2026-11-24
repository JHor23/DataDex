package com.example.data

import android.util.Log
import com.example.model.BaseStats
import com.example.model.DataSource
import com.example.model.OverlayHudState
import com.example.model.PokemonAbilityInfo
import com.example.model.PokemonEntry
import com.example.model.PokemonType
import com.example.model.RecognitionMethod
import com.example.model.ScanMatch
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object DataDexStateHolder {
    private val _overlayState = MutableStateFlow(OverlayHudState())
    val overlayState: StateFlow<OverlayHudState> = _overlayState.asStateFlow()

    fun updateOverlayState(transform: (OverlayHudState) -> OverlayHudState) {
        _overlayState.update(transform)
    }

    fun setActivelyReading(reading: Boolean) {
        _overlayState.update { it.copy(isActivelyReading = reading) }
    }

    fun setOverlayMatch(
        match: ScanMatch,
        textPreview: String = "",
        visionTags: List<String> = emptyList(),
        secondaryMatches: List<PokemonEntry> = emptyList()
    ) {
        val now = System.currentTimeMillis()
        _overlayState.update { state ->
            val changedPokemon = state.currentMatch?.pokemon?.id != match.pokemon.id
            state.copy(
                currentMatch = match,
                secondaryMatches = secondaryMatches,
                lastScannedTextPreview = textPreview.ifBlank { state.lastScannedTextPreview },
                lastVisionTags = visionTags.ifEmpty { state.lastVisionTags },
                framesProcessed = state.framesProcessed + 1,
                lastScanTimestampMillis = now,
                lastMatchUpdatedMillis = if (changedPokemon || state.lastMatchUpdatedMillis == 0L) now else state.lastMatchUpdatedMillis,
                isActivelyReading = false,
                statusMessage = "Identified ${match.pokemon.name}"
            )
        }
    }

    fun toggleOverlayAbilities() {
        _overlayState.update { it.copy(showAbilitiesAndNatures = !it.showAbilitiesAndNatures) }
    }

    fun clearCurrentMatch() {
        _overlayState.update {
            it.copy(
                currentMatch = null,
                secondaryMatches = emptyList(),
                statusMessage = "Cleared • Watching screen"
            )
        }
    }

    fun recordFrameTelemetry(
        textPreview: String,
        visionTags: List<String>,
        statusOverride: String? = null
    ) {
        val now = System.currentTimeMillis()
        _overlayState.update { state ->
            state.copy(
                lastScannedTextPreview = textPreview,
                lastVisionTags = visionTags,
                framesProcessed = state.framesProcessed + 1,
                lastScanTimestampMillis = now,
                isActivelyReading = false,
                statusMessage = statusOverride ?: if (textPreview.isNotBlank()) {
                    "Reading text • Watching for Pokémon"
                } else {
                    "Watching screen for Pokémon names"
                }
            )
        }
    }
}

class PokemonRepository {
    private val cache = ConcurrentHashMap<Int, PokemonEntry>().apply {
        putAll(LocalPokedexCatalog.byId)
    }
    private val abilityDescCache = ConcurrentHashMap<String, String>()

    private val bundledSorted: List<PokemonEntry> by lazy {
        LocalPokedexCatalog.bundledEntries.sortedBy { it.id }
    }

    @Volatile
    private var sortedCacheSnapshot: List<PokemonEntry> = emptyList()

    private val apiService: PokeApiService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl("https://pokeapi.co/api/v2/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(PokeApiService::class.java)
    }

    fun getBundledEntries(): List<PokemonEntry> = bundledSorted

    fun getAllCachedEntries(): List<PokemonEntry> {
        val snapshot = sortedCacheSnapshot
        if (snapshot.size == cache.size) return snapshot
        return cache.values.sortedBy { it.id }.also { sortedCacheSnapshot = it }
    }

    suspend fun getPokemonById(id: Int, allowNetwork: Boolean = true): PokemonEntry? {
        cache[id]?.let { return it }
        if (!allowNetwork) {
            return createMinimalFallback(id)
        }
        return fetchFromPokeApi(id.toString()) ?: createMinimalFallback(id)
    }

    suspend fun getPokemonByName(name: String, allowNetwork: Boolean = true): PokemonEntry? {
        val dexId = PokemonNameDictionary.idForName(name)
        if (dexId != null) {
            return getPokemonById(dexId, allowNetwork)
        }
        if (!allowNetwork) return null
        return fetchFromPokeApi(name.lowercase().trim().replace(" ", "-"))
    }

    fun searchLocalCatalog(query: String, typeFilter: PokemonType? = null): List<PokemonEntry> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            val all = getAllCachedEntries()
            return if (typeFilter == null) all else all.filter { typeFilter in it.types }
        }

        val strippedIdQuery = trimmed.removePrefix("#")
        val allCached = getAllCachedEntries()
        val cachedMatches = allCached.filter { entry ->
            (typeFilter == null || typeFilter in entry.types) &&
                (entry.name.contains(trimmed, ignoreCase = true) ||
                    entry.id.toString() == strippedIdQuery)
        }

        // Minimal fallback entries default to NORMAL type; skip allocating them if filtered out.
        if (typeFilter != null && typeFilter != PokemonType.NORMAL) {
            return cachedMatches
        }

        val dictMatches = ArrayList<PokemonEntry>()
        val names = PokemonNameDictionary.allNamesInDexOrder
        for (idx in names.indices) {
            val id = idx + 1
            if (cache.containsKey(id)) continue
            val name = names[idx]
            if (name.contains(trimmed, ignoreCase = true) || id.toString() == strippedIdQuery) {
                createMinimalFallback(id)?.let { dictMatches.add(it) }
            }
        }
        if (dictMatches.isEmpty()) return cachedMatches
        return (cachedMatches + dictMatches).sortedBy { it.id }
    }

    private suspend fun resolveRemoteAbilityDescription(slug: String, prettyName: String): String {
        AbilityDescriptions.forName(prettyName)?.let { return it }
        abilityDescCache[slug]?.let { return it }
        return runCatching {
            val dto = apiService.getAbility(slug)
            val enEffect = dto.effectEntries.firstOrNull { it.language.name == "en" }?.shortEffect
                ?: dto.flavorTextEntries.lastOrNull { it.language.name == "en" }?.flavorText
            val cleaned = enEffect?.replace(WHITESPACE_REGEX, " ")?.trim().orEmpty()
            if (cleaned.isNotBlank()) {
                abilityDescCache[slug] = cleaned
            }
            cleaned
        }.getOrDefault("")
    }

    private suspend fun fetchFromPokeApi(idOrName: String): PokemonEntry? {
        return try {
            val dto = apiService.getPokemon(idOrName)
            val statMap = dto.stats.associate { it.stat.name to it.baseStat }
            val types = dto.types
                .sortedBy { it.slot }
                .mapNotNull { PokemonType.fromApiName(it.type.name) }
                .ifEmpty { listOf(PokemonType.NORMAL) }
            val abilities = dto.abilities.map { slot ->
                val prettyName = slot.ability.name.split("-").joinToString(" ") { part ->
                    part.replaceFirstChar { it.uppercase() }
                }
                val desc = resolveRemoteAbilityDescription(slot.ability.name, prettyName)
                PokemonAbilityInfo(
                    name = prettyName,
                    isHidden = slot.isHidden,
                    description = desc
                )
            }
            val displayName = PokemonNameDictionary.displayNameForId(dto.id)
                ?: dto.name.split("-").joinToString(" ") { part ->
                    part.replaceFirstChar { it.uppercase() }
                }

            val entry = PokemonEntry(
                id = dto.id,
                name = displayName,
                types = types,
                baseStats = BaseStats(
                    hp = statMap["hp"] ?: 50,
                    attack = statMap["attack"] ?: 50,
                    defense = statMap["defense"] ?: 50,
                    spAtk = statMap["special-attack"] ?: 50,
                    spDef = statMap["special-defense"] ?: 50,
                    speed = statMap["speed"] ?: 50
                ),
                abilityDetails = abilities,
                category = "National Dex #${dto.id}",
                heightDm = dto.height,
                weightHg = dto.weight,
                source = DataSource.POKEAPI_LIVE
            )
            cache[entry.id] = entry
            entry
        } catch (e: Exception) {
            Log.w("PokemonRepository", "PokéAPI lookup failed for '$idOrName': ${e.message}")
            null
        }
    }

    private fun createMinimalFallback(id: Int): PokemonEntry? {
        val name = PokemonNameDictionary.displayNameForId(id) ?: return null
        return PokemonEntry(
            id = id,
            name = name,
            types = listOf(PokemonType.NORMAL),
            baseStats = BaseStats(60, 60, 60, 60, 60, 60),
            abilityDetails = listOf(PokemonAbilityInfo("Tap to sync PokéAPI", isHidden = false)),
            category = "Tap to load live PokéAPI stats",
            source = DataSource.BUNDLED_DEX
        )
    }

    companion object {
        private val WHITESPACE_REGEX = Regex("\\s+")

        @Volatile
        private var instance: PokemonRepository? = null

        fun getInstance(): PokemonRepository =
            instance ?: synchronized(this) {
                instance ?: PokemonRepository().also { instance = it }
            }
    }
}
