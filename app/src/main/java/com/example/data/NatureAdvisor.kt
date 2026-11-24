package com.example.data

import com.example.model.NatureRecommendation
import com.example.model.PokemonAbilityInfo
import com.example.model.PokemonEntry
import kotlin.math.abs

/**
 * Recommends the best / most popular competitive and playthrough natures for each ability
 * on a Pokémon based on ability mechanics and base stat distribution.
 */
object NatureAdvisor {

    private val ADAMANT = NatureRecommendation("Adamant", "+Atk/-SpA")
    private val JOLLY = NatureRecommendation("Jolly", "+Spe/-SpA")
    private val MODEST = NatureRecommendation("Modest", "+SpA/-Atk")
    private val TIMID = NatureRecommendation("Timid", "+Spe/-Atk")
    private val BOLD = NatureRecommendation("Bold", "+Def/-Atk")
    private val IMPISH = NatureRecommendation("Impish", "+Def/-SpA")
    private val CALM = NatureRecommendation("Calm", "+SpD/-Atk")
    private val CAREFUL = NatureRecommendation("Careful", "+SpD/-SpA")
    private val BRAVE = NatureRecommendation("Brave", "+Atk/-Spe")
    private val QUIET = NatureRecommendation("Quiet", "+SpA/-Spe")
    private val RELAXED = NatureRecommendation("Relaxed", "+Def/-Spe")
    private val SASSY = NatureRecommendation("Sassy", "+SpD/-Spe")
    private val HASTY = NatureRecommendation("Hasty", "+Spe/-Def")
    private val NAIVE = NatureRecommendation("Naive", "+Spe/-SpD")

    private val physicalBoostAbilities = setOf(
        "huge power", "pure power", "guts", "moxie", "sheer force", "tough claws",
        "strong jaw", "iron fist", "sharpness", "supreme overlord", "intrepid sword",
        "hustle", "gorilla tactics", "sword of ruin", "reckless", "skill link",
        "rock head", "hyper cutter", "defiant", "disguise", "orichalcum pulse",
        "thermal exchange", "speed boost", "technician", "swarm", "sand rush"
    )

    private val specialBoostAbilities = setOf(
        "solar power", "drought", "drizzle", "hadron engine", "electric surge",
        "psychic surge", "beads of ruin", "soul-heart", "competitive", "tinted lens",
        "good as gold", "magic guard", "mega launcher", "punk rock", "transistor",
        "levitate", "compound eyes", "serene grace", "bad dreams", "protosynthesis"
    )

    private val defensiveSupportAbilities = setOf(
        "regenerator", "unaware", "multiscale", "thick fat", "sturdy", "prankster",
        "magic bounce", "mirror armor", "water absorb", "volt absorb", "flash fire",
        "immunity", "marvel scale", "natural cure", "shed skin", "drizzle",
        "pressure", "oblivious", "cursed body", "cute charm", "inner focus",
        "own tempo", "shell armor", "rock head", "sand stream"
    )

    private val weatherOrTerrainSpeedAbilities = setOf(
        "swift swim", "chlorophyll", "sand rush", "slush rush", "surge surfer",
        "unburden", "speed boost"
    )

    fun bestNaturesForAbility(
        pokemon: PokemonEntry,
        ability: PokemonAbilityInfo
    ): List<NatureRecommendation> {
        val stats = pokemon.baseStats
        val key = ability.name.lowercase().trim()
        val isPhysLean = stats.attack > stats.spAtk + 10
        val isSpecLean = stats.spAtk > stats.attack + 10
        val isMixed = abs(stats.attack - stats.spAtk) <= 15 && minOf(stats.attack, stats.spAtk) >= 85
        val isSlow = stats.speed <= 55
        val isBulky = (stats.hp + stats.defense + stats.spDef) >= 265 && maxOf(stats.defense, stats.spDef) >= 85
        val prefersDef = stats.defense >= stats.spDef

        val picks = linkedSetOf<NatureRecommendation>()

        // 1. Specific signature / high-impact ability logic
        when (key) {
            "intimidate" -> {
                if (stats.speed >= 80 && isPhysLean) {
                    picks.addAll(listOf(JOLLY, ADAMANT, IMPISH))
                } else {
                    picks.addAll(listOf(CAREFUL, IMPISH, ADAMANT))
                }
            }
            "protosynthesis", "quark drive" -> {
                if (isSpecLean) {
                    picks.addAll(listOf(TIMID, MODEST))
                } else if (isPhysLean) {
                    picks.addAll(listOf(JOLLY, ADAMANT))
                } else {
                    picks.addAll(listOf(TIMID, JOLLY, MODEST))
                }
            }
            "stance change" -> picks.addAll(listOf(QUIET, BRAVE, ADAMANT))
            "analytic" -> picks.addAll(if (isSpecLean) listOf(QUIET, MODEST) else listOf(BRAVE, ADAMANT))
            "protean", "libero" -> {
                if (isMixed) picks.addAll(listOf(NAIVE, TIMID, JOLLY))
                else if (isSpecLean) picks.addAll(listOf(TIMID, MODEST))
                else picks.addAll(listOf(JOLLY, ADAMANT))
            }
            "sand stream" -> picks.addAll(listOf(ADAMANT, CAREFUL, BRAVE))
            "drought" -> picks.addAll(if (isPhysLean) listOf(ADAMANT, JOLLY) else listOf(TIMID, MODEST, CALM))
            "drizzle" -> picks.addAll(if (stats.spAtk >= 110) listOf(MODEST, TIMID) else listOf(BOLD, CALM, MODEST))
            "multiscale" -> {
                if (stats.attack >= 115) picks.addAll(listOf(ADAMANT, JOLLY))
                else picks.addAll(listOf(CALM, BOLD, TIMID))
            }
            "regenerator" -> {
                if (prefersDef) picks.addAll(listOf(BOLD, IMPISH, RELAXED))
                else picks.addAll(listOf(CALM, CAREFUL, SASSY))
            }
            "good as gold" -> picks.addAll(listOf(TIMID, MODEST, BOLD))
            "supreme overlord" -> picks.addAll(listOf(ADAMANT, BRAVE, JOLLY))
            "disguise" -> picks.addAll(listOf(JOLLY, ADAMANT))
            "technician" -> {
                if (isSpecLean) picks.addAll(listOf(TIMID, MODEST))
                else picks.addAll(listOf(ADAMANT, JOLLY))
            }
            "mold breaker" -> {
                if (isPhysLean) picks.addAll(listOf(JOLLY, ADAMANT))
                else if (isSpecLean) picks.addAll(listOf(TIMID, MODEST))
                else picks.addAll(listOf(CAREFUL, JOLLY, ADAMANT))
            }
            "mirror armor", "sturdy" -> {
                if (prefersDef) picks.addAll(listOf(IMPISH, BOLD, CAREFUL))
                else picks.addAll(listOf(CAREFUL, IMPISH))
            }
        }

        if (picks.isNotEmpty()) {
            return picks.take(3)
        }

        // 2. Archetype ability matching
        if (key in weatherOrTerrainSpeedAbilities) {
            if (isSpecLean) {
                picks.addAll(listOf(MODEST, TIMID))
            } else {
                picks.addAll(listOf(ADAMANT, JOLLY))
            }
        } else if (key in physicalBoostAbilities) {
            if (isSlow) {
                picks.addAll(listOf(ADAMANT, BRAVE, JOLLY))
            } else {
                picks.addAll(listOf(JOLLY, ADAMANT))
            }
        } else if (key in specialBoostAbilities) {
            if (isSlow) {
                picks.addAll(listOf(MODEST, QUIET, TIMID))
            } else {
                picks.addAll(listOf(TIMID, MODEST))
            }
        } else if (key in defensiveSupportAbilities && isBulky && maxOf(stats.attack, stats.spAtk) < 115) {
            if (isSpecLean) {
                picks.addAll(if (prefersDef) listOf(BOLD, CALM, MODEST) else listOf(CALM, BOLD, MODEST))
            } else if (isPhysLean) {
                picks.addAll(if (prefersDef) listOf(IMPISH, CAREFUL, ADAMANT) else listOf(CAREFUL, IMPISH, ADAMANT))
            } else {
                picks.addAll(if (prefersDef) listOf(BOLD, CALM) else listOf(CALM, BOLD))
            }
        }

        // 3. Base stat fallback so every ability (including PokéAPI abilities) gets crisp recommendations
        if (picks.size < 2) {
            when {
                isMixed && stats.speed >= 85 -> {
                    picks.addAll(listOf(NAIVE, HASTY, TIMID))
                }
                isPhysLean -> {
                    if (isSlow && isBulky) {
                        picks.addAll(listOf(ADAMANT, BRAVE, IMPISH))
                    } else if (stats.speed >= 75) {
                        picks.addAll(listOf(JOLLY, ADAMANT))
                    } else {
                        picks.addAll(listOf(ADAMANT, JOLLY))
                    }
                }
                isSpecLean -> {
                    if (isSlow && isBulky) {
                        picks.addAll(listOf(MODEST, QUIET, CALM))
                    } else if (stats.speed >= 75) {
                        picks.addAll(listOf(TIMID, MODEST))
                    } else {
                        picks.addAll(listOf(MODEST, TIMID))
                    }
                }
                isBulky -> {
                    picks.addAll(if (prefersDef) listOf(BOLD, IMPISH, CALM) else listOf(CALM, CAREFUL, BOLD))
                }
                else -> {
                    picks.addAll(listOf(TIMID, JOLLY, MODEST))
                }
            }
        }

        return picks.take(3)
    }
}
