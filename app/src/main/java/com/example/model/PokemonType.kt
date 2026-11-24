package com.example.model

import androidx.compose.ui.graphics.Color
import java.util.concurrent.ConcurrentHashMap

enum class PokemonType(
    val displayName: String,
    val badgeColor: Color,
    val onBadgeColor: Color = Color.White
) {
    NORMAL("Normal", Color(0xFFA8A77A), Color(0xFF1A1A14)),
    FIRE("Fire", Color(0xFFEE8130)),
    WATER("Water", Color(0xFF6390F0)),
    ELECTRIC("Electric", Color(0xFFF7D02C), Color(0xFF1F1A00)),
    GRASS("Grass", Color(0xFF7AC74C), Color(0xFF0E2105)),
    ICE("Ice", Color(0xFF96D9D6), Color(0xFF092224)),
    FIGHTING("Fighting", Color(0xFFC22E28)),
    POISON("Poison", Color(0xFFA33EA1)),
    GROUND("Ground", Color(0xFFE2BF65), Color(0xFF231B04)),
    FLYING("Flying", Color(0xFFA98FF3), Color(0xFF180E38)),
    PSYCHIC("Psychic", Color(0xFFF95587)),
    BUG("Bug", Color(0xFFA6B91A), Color(0xFF1C2000)),
    ROCK("Rock", Color(0xFFB6A136), Color(0xFF1E1902)),
    GHOST("Ghost", Color(0xFF735797)),
    DRAGON("Dragon", Color(0xFF6F35FC)),
    DARK("Dark", Color(0xFF705746)),
    STEEL("Steel", Color(0xFFB7B7CE), Color(0xFF151624)),
    FAIRY("Fairy", Color(0xFFD685AD), Color(0xFF2C091C));

    companion object {
        fun fromApiName(raw: String): PokemonType? =
            entries.firstOrNull { it.name.equals(raw.trim(), ignoreCase = true) }
    }
}

data class TypeMultiplier(
    val type: PokemonType,
    val multiplier: Float
)

data class WeaknessProfile(
    val quadWeaknesses: List<TypeMultiplier>,
    val doubleWeaknesses: List<TypeMultiplier>,
    val resistances: List<TypeMultiplier>,
    val doubleResistances: List<TypeMultiplier>,
    val immunities: List<TypeMultiplier>
) {
    val allWeaknesses: List<TypeMultiplier>
        get() = quadWeaknesses + doubleWeaknesses

    val allResistancesAndImmunities: List<TypeMultiplier>
        get() = immunities + doubleResistances + resistances
}

data class OffensiveProfile(
    val superEffective: List<TypeMultiplier>,
    val notVeryEffective: List<TypeMultiplier>,
    val noEffect: List<TypeMultiplier>
)

object TypeEffectivenessChart {
    private val offensiveProfileCache = ConcurrentHashMap<PokemonType, OffensiveProfile>()
    private val weaknessProfileCache = ConcurrentHashMap<List<PokemonType>, WeaknessProfile>()

    // Map<AttackingType, Map<DefendingType, Multiplier>>
    private val chart: Map<PokemonType, Map<PokemonType, Float>> = mapOf(
        PokemonType.NORMAL to mapOf(
            PokemonType.ROCK to 0.5f,
            PokemonType.GHOST to 0f,
            PokemonType.STEEL to 0.5f
        ),
        PokemonType.FIRE to mapOf(
            PokemonType.FIRE to 0.5f,
            PokemonType.WATER to 0.5f,
            PokemonType.GRASS to 2f,
            PokemonType.ICE to 2f,
            PokemonType.BUG to 2f,
            PokemonType.ROCK to 0.5f,
            PokemonType.DRAGON to 0.5f,
            PokemonType.STEEL to 2f
        ),
        PokemonType.WATER to mapOf(
            PokemonType.FIRE to 2f,
            PokemonType.WATER to 0.5f,
            PokemonType.GRASS to 0.5f,
            PokemonType.GROUND to 2f,
            PokemonType.ROCK to 2f,
            PokemonType.DRAGON to 0.5f
        ),
        PokemonType.ELECTRIC to mapOf(
            PokemonType.WATER to 2f,
            PokemonType.ELECTRIC to 0.5f,
            PokemonType.GRASS to 0.5f,
            PokemonType.GROUND to 0f,
            PokemonType.FLYING to 2f,
            PokemonType.DRAGON to 0.5f
        ),
        PokemonType.GRASS to mapOf(
            PokemonType.FIRE to 0.5f,
            PokemonType.WATER to 2f,
            PokemonType.GRASS to 0.5f,
            PokemonType.POISON to 0.5f,
            PokemonType.GROUND to 2f,
            PokemonType.FLYING to 0.5f,
            PokemonType.BUG to 0.5f,
            PokemonType.ROCK to 2f,
            PokemonType.DRAGON to 0.5f,
            PokemonType.STEEL to 0.5f
        ),
        PokemonType.ICE to mapOf(
            PokemonType.FIRE to 0.5f,
            PokemonType.WATER to 0.5f,
            PokemonType.GRASS to 2f,
            PokemonType.ICE to 0.5f,
            PokemonType.GROUND to 2f,
            PokemonType.FLYING to 2f,
            PokemonType.DRAGON to 2f,
            PokemonType.STEEL to 0.5f
        ),
        PokemonType.FIGHTING to mapOf(
            PokemonType.NORMAL to 2f,
            PokemonType.ICE to 2f,
            PokemonType.POISON to 0.5f,
            PokemonType.FLYING to 0.5f,
            PokemonType.PSYCHIC to 0.5f,
            PokemonType.BUG to 0.5f,
            PokemonType.ROCK to 2f,
            PokemonType.GHOST to 0f,
            PokemonType.DARK to 2f,
            PokemonType.STEEL to 2f,
            PokemonType.FAIRY to 0.5f
        ),
        PokemonType.POISON to mapOf(
            PokemonType.GRASS to 2f,
            PokemonType.POISON to 0.5f,
            PokemonType.GROUND to 0.5f,
            PokemonType.ROCK to 0.5f,
            PokemonType.GHOST to 0.5f,
            PokemonType.STEEL to 0f,
            PokemonType.FAIRY to 2f
        ),
        PokemonType.GROUND to mapOf(
            PokemonType.FIRE to 2f,
            PokemonType.ELECTRIC to 2f,
            PokemonType.GRASS to 0.5f,
            PokemonType.POISON to 2f,
            PokemonType.FLYING to 0f,
            PokemonType.BUG to 0.5f,
            PokemonType.ROCK to 2f,
            PokemonType.STEEL to 2f
        ),
        PokemonType.FLYING to mapOf(
            PokemonType.ELECTRIC to 0.5f,
            PokemonType.GRASS to 2f,
            PokemonType.FIGHTING to 2f,
            PokemonType.BUG to 2f,
            PokemonType.ROCK to 0.5f,
            PokemonType.STEEL to 0.5f
        ),
        PokemonType.PSYCHIC to mapOf(
            PokemonType.FIGHTING to 2f,
            PokemonType.POISON to 2f,
            PokemonType.PSYCHIC to 0.5f,
            PokemonType.DARK to 0f,
            PokemonType.STEEL to 0.5f
        ),
        PokemonType.BUG to mapOf(
            PokemonType.FIRE to 0.5f,
            PokemonType.GRASS to 2f,
            PokemonType.FIGHTING to 0.5f,
            PokemonType.POISON to 0.5f,
            PokemonType.FLYING to 0.5f,
            PokemonType.PSYCHIC to 2f,
            PokemonType.GHOST to 0.5f,
            PokemonType.DARK to 2f,
            PokemonType.STEEL to 0.5f,
            PokemonType.FAIRY to 0.5f
        ),
        PokemonType.ROCK to mapOf(
            PokemonType.FIRE to 2f,
            PokemonType.ICE to 2f,
            PokemonType.FIGHTING to 0.5f,
            PokemonType.GROUND to 0.5f,
            PokemonType.FLYING to 2f,
            PokemonType.BUG to 2f,
            PokemonType.STEEL to 0.5f
        ),
        PokemonType.GHOST to mapOf(
            PokemonType.NORMAL to 0f,
            PokemonType.PSYCHIC to 2f,
            PokemonType.GHOST to 2f,
            PokemonType.DARK to 0.5f
        ),
        PokemonType.DRAGON to mapOf(
            PokemonType.DRAGON to 2f,
            PokemonType.STEEL to 0.5f,
            PokemonType.FAIRY to 0f
        ),
        PokemonType.DARK to mapOf(
            PokemonType.FIGHTING to 0.5f,
            PokemonType.PSYCHIC to 2f,
            PokemonType.GHOST to 2f,
            PokemonType.DARK to 0.5f,
            PokemonType.FAIRY to 0.5f
        ),
        PokemonType.STEEL to mapOf(
            PokemonType.FIRE to 0.5f,
            PokemonType.WATER to 0.5f,
            PokemonType.ELECTRIC to 0.5f,
            PokemonType.ICE to 2f,
            PokemonType.ROCK to 2f,
            PokemonType.STEEL to 0.5f,
            PokemonType.FAIRY to 2f
        ),
        PokemonType.FAIRY to mapOf(
            PokemonType.FIRE to 0.5f,
            PokemonType.FIGHTING to 2f,
            PokemonType.POISON to 0.5f,
            PokemonType.DRAGON to 2f,
            PokemonType.DARK to 2f,
            PokemonType.STEEL to 0.5f
        )
    )

    fun attackingMultiplier(attacking: PokemonType, defending: PokemonType): Float =
        chart[attacking]?.get(defending) ?: 1f

    fun multiplierAgainst(attacking: PokemonType, defending: List<PokemonType>): Float {
        if (defending.isEmpty()) return 1f
        var mult = 1f
        val attackMap = chart[attacking].orEmpty()
        for (def in defending) {
            mult *= (attackMap[def] ?: 1f)
        }
        return mult
    }

    fun computeOffensiveProfile(attacking: PokemonType): OffensiveProfile =
        offensiveProfileCache.getOrPut(attacking) {
            val superEff = mutableListOf<TypeMultiplier>()
            val notVery = mutableListOf<TypeMultiplier>()
            val immune = mutableListOf<TypeMultiplier>()
            for (def in PokemonType.entries) {
                val mult = attackingMultiplier(attacking, def)
                when {
                    mult == 0f -> immune.add(TypeMultiplier(def, 0f))
                    mult >= 1.9f -> superEff.add(TypeMultiplier(def, 2f))
                    mult <= 0.51f -> notVery.add(TypeMultiplier(def, 0.5f))
                }
            }
            OffensiveProfile(
                superEffective = superEff,
                notVeryEffective = notVery,
                noEffect = immune
            )
        }

    fun computeWeaknessProfile(defending: List<PokemonType>): WeaknessProfile =
        weaknessProfileCache.getOrPut(defending) {
            val quad = mutableListOf<TypeMultiplier>()
            val dbl = mutableListOf<TypeMultiplier>()
            val res = mutableListOf<TypeMultiplier>()
            val dblRes = mutableListOf<TypeMultiplier>()
            val imm = mutableListOf<TypeMultiplier>()

            for (atk in PokemonType.entries) {
                val mult = multiplierAgainst(atk, defending)
                when {
                    mult == 0f -> imm.add(TypeMultiplier(atk, 0f))
                    mult >= 3.9f -> quad.add(TypeMultiplier(atk, 4f))
                    mult >= 1.9f -> dbl.add(TypeMultiplier(atk, 2f))
                    mult <= 0.26f -> dblRes.add(TypeMultiplier(atk, 0.25f))
                    mult <= 0.51f -> res.add(TypeMultiplier(atk, 0.5f))
                }
            }
            WeaknessProfile(
                quadWeaknesses = quad,
                doubleWeaknesses = dbl,
                resistances = res,
                doubleResistances = dblRes,
                immunities = imm
            )
        }
}
