package com.example.data

import com.squareup.moshi.Json
import retrofit2.http.GET
import retrofit2.http.Path

data class PokeApiPokemonDto(
    val id: Int,
    val name: String,
    val height: Int,
    val weight: Int,
    val types: List<PokeApiTypeSlotDto>,
    val stats: List<PokeApiStatSlotDto>,
    val abilities: List<PokeApiAbilitySlotDto>
)

data class PokeApiTypeSlotDto(
    val slot: Int,
    val type: PokeApiNamedResourceDto
)

data class PokeApiStatSlotDto(
    @Json(name = "base_stat") val baseStat: Int,
    val stat: PokeApiNamedResourceDto
)

data class PokeApiAbilitySlotDto(
    @Json(name = "is_hidden") val isHidden: Boolean,
    val ability: PokeApiNamedResourceDto
)

data class PokeApiNamedResourceDto(
    val name: String
)

data class PokeApiAbilityDetailDto(
    val name: String,
    @Json(name = "effect_entries") val effectEntries: List<PokeApiEffectEntryDto> = emptyList(),
    @Json(name = "flavor_text_entries") val flavorTextEntries: List<PokeApiFlavorTextDto> = emptyList()
)

data class PokeApiEffectEntryDto(
    @Json(name = "short_effect") val shortEffect: String,
    val language: PokeApiNamedResourceDto
)

data class PokeApiFlavorTextDto(
    @Json(name = "flavor_text") val flavorText: String,
    val language: PokeApiNamedResourceDto
)

interface PokeApiService {
    @GET("pokemon/{idOrName}")
    suspend fun getPokemon(@Path("idOrName") idOrName: String): PokeApiPokemonDto

    @GET("ability/{name}")
    suspend fun getAbility(@Path("name") name: String): PokeApiAbilityDetailDto
}
