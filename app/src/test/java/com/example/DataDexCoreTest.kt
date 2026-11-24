package com.example

import com.example.audio.ScouterSoundEngine
import com.example.data.AbilityDescriptions
import com.example.data.LocalPokedexCatalog
import com.example.data.NatureAdvisor
import com.example.data.PokeApiAbilityDetailDto
import com.example.data.PokeApiPokemonDto
import com.example.data.PokemonNameDictionary
import com.example.ml.PokemonMatcher
import com.example.model.PokemonType
import com.example.model.TypeEffectivenessChart
import com.example.voice.DataDexVoiceSpeaker
import com.example.voice.RetroPokedexDsp
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DataDexCoreTest {

    @Test
    fun `type chart computes 4x weakness and immunities accurately`() {
        val garchompProfile = TypeEffectivenessChart.computeWeaknessProfile(
            listOf(PokemonType.DRAGON, PokemonType.GROUND)
        )
        assertTrue(garchompProfile.quadWeaknesses.any { it.type == PokemonType.ICE })
        assertTrue(garchompProfile.immunities.any { it.type == PokemonType.ELECTRIC })
        assertTrue(garchompProfile.doubleWeaknesses.any { it.type == PokemonType.DRAGON })
        assertTrue(garchompProfile.doubleWeaknesses.any { it.type == PokemonType.FAIRY })

        val fireOffense = TypeEffectivenessChart.computeOffensiveProfile(PokemonType.FIRE)
        assertTrue(fireOffense.superEffective.any { it.type == PokemonType.GRASS })
        assertTrue(fireOffense.superEffective.any { it.type == PokemonType.STEEL })
        assertTrue(fireOffense.notVeryEffective.any { it.type == PokemonType.WATER })
        assertEquals(0f, TypeEffectivenessChart.attackingMultiplier(PokemonType.GROUND, PokemonType.FLYING), 0.001f)
        assertEquals(0f, TypeEffectivenessChart.attackingMultiplier(PokemonType.DRAGON, PokemonType.FAIRY), 0.001f)
    }

    @Test
    fun `ocr matcher identifies exact, noisy, and multi-word pokemon names`() {
        val exact = PokemonMatcher.matchFromTextLines(listOf("Lv. 78", "Charizard", "HP 248/248"))
        assertNotNull(exact)
        assertEquals(6, exact?.pokemonId)

        val multiWord = PokemonMatcher.matchFromTextLines(listOf("Wild Flutter Mane appeared!"))
        assertNotNull(multiWord)
        assertEquals(987, multiWord?.pokemonId)

        val noisyOcr = PokemonMatcher.matchFromTextLines(listOf("P1kachu Lv.50"))
        assertNotNull(noisyOcr)
        assertEquals(25, noisyOcr?.pokemonId)
    }

    @Test
    fun `vision matcher maps ML Kit image labels to pokemon candidates`() {
        val candidate = PokemonMatcher.matchFromVisionLabels(
            listOf("Text" to 0.40f, "Dragon" to 0.87f, "Poster" to 0.72f)
        )
        assertNotNull(candidate)
        assertEquals(6, candidate?.pokemonId)
    }

    @Test
    fun `dictionary contains all 1025 pokemon names`() {
        assertEquals(1025, PokemonNameDictionary.allNamesInDexOrder.size)
        assertEquals(1025, PokemonNameDictionary.idForName("Pecharunt"))
        assertEquals(1, PokemonNameDictionary.idForName("Bulbasaur"))
    }

    @Test
    fun `ocr matcher extracts multiple distinct pokemon names on screen`() {
        val matches = PokemonMatcher.matchAllFromTextLines(
            listOf("Double Battle!", "Garchomp Lv.50", "vs Dragapult Lv.50")
        )
        assertEquals(listOf(445, 887), matches.map { it.pokemonId })
    }

    @Test
    fun `nature advisor recommends distinct popular natures per ability and marks hidden ability`() {
        val incineroar = LocalPokedexCatalog.byId.getValue(727)
        val hidden = incineroar.abilityDetails.first { it.isHidden }
        assertEquals("Intimidate", hidden.name)
        val intimidateNatures = NatureAdvisor.bestNaturesForAbility(incineroar, hidden).map { it.name }
        assertTrue("Careful" in intimidateNatures)

        val flutterMane = LocalPokedexCatalog.byId.getValue(987)
        val protosynthesis = flutterMane.abilityDetails.first()
        val flutterNatures = NatureAdvisor.bestNaturesForAbility(flutterMane, protosynthesis).map { it.name }
        assertTrue("Timid" in flutterNatures && "Modest" in flutterNatures)
    }

    @Test
    fun `every bundled ability resolves a non-blank description`() {
        LocalPokedexCatalog.bundledEntries.forEach { entry ->
            entry.abilityDetails.forEach { ability ->
                val desc = AbilityDescriptions.resolveDescription(ability)
                assertTrue("Missing description for ${ability.name}", desc.isNotBlank())
                assertTrue(
                    "Catalog should explicitly cover ${ability.name}",
                    AbilityDescriptions.forName(ability.name) != null
                )
            }
        }
    }

    @Test
    fun `voice speaker builds clean spoken summary respecting voice toggles`() {
        val charizard = LocalPokedexCatalog.byId.getValue(6)

        val nameOnly = DataDexVoiceSpeaker.buildSpokenSummary(
            pokemon = charizard,
            includeDetails = false,
            includeAbilities = false
        )
        assertEquals("Charizard.", nameOnly)

        val fullReadout = DataDexVoiceSpeaker.buildSpokenSummary(
            pokemon = charizard,
            includeDetails = true,
            includeAbilities = true
        )
        assertTrue(fullReadout.contains("Fire and Flying type"))
        assertTrue(fullReadout.contains("Four times weak to Rock"))
        assertTrue(fullReadout.contains("hidden ability Solar Power"))
    }

    @Test
    fun `moshi reflection adapter deserializes pokeapi pokemon and ability dtos`() {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val pokemonJson = """
            {
              "id": 6,
              "name": "charizard",
              "height": 17,
              "weight": 905,
              "types": [{"slot": 1, "type": {"name": "fire"}}],
              "stats": [{"base_stat": 78, "stat": {"name": "hp"}}],
              "abilities": [{"is_hidden": true, "ability": {"name": "solar-power"}}]
            }
        """.trimIndent()
        val pokemonDto = moshi.adapter(PokeApiPokemonDto::class.java).fromJson(pokemonJson)
        assertNotNull(pokemonDto)
        assertEquals(6, pokemonDto?.id)
        assertEquals(78, pokemonDto?.stats?.first()?.baseStat)
        assertEquals(true, pokemonDto?.abilities?.first()?.isHidden)

        val abilityJson = """
            {
              "name": "solar-power",
              "effect_entries": [{"short_effect": "Boosts Sp. Atk in sun.", "language": {"name": "en"}}]
            }
        """.trimIndent()
        val abilityDto = moshi.adapter(PokeApiAbilityDetailDto::class.java).fromJson(abilityJson)
        assertNotNull(abilityDto)
        assertEquals("Boosts Sp. Atk in sun.", abilityDto?.effectEntries?.first()?.shortEffect)
    }

    @Test
    fun `scouter sound engine synthesizes bounded sci-fi waveforms and perceptual gain`() {
        val detectPcm = ScouterSoundEngine.generateScouterDetectedPcm()
        val updatePcm = ScouterSoundEngine.generateScouterUpdatePcm()
        val pulsePcm = ScouterSoundEngine.generateScanPulsePcm()

        assertTrue(detectPcm.size > ScouterSoundEngine.SAMPLE_RATE / 5)
        assertTrue(updatePcm.size > ScouterSoundEngine.SAMPLE_RATE / 15)
        assertTrue(pulsePcm.isNotEmpty())
        assertTrue(detectPcm.any { it != 0.toShort() })
        assertTrue(updatePcm.any { it != 0.toShort() })
        assertTrue(detectPcm.maxOf { kotlin.math.abs(it.toInt()) } <= (Short.MAX_VALUE * 0.6).toInt())

        val lowGain = ScouterSoundEngine.perceptualGain(0.2f)
        val highGain = ScouterSoundEngine.perceptualGain(1.0f)
        assertTrue(lowGain in 0.01f..0.1f)
        assertTrue(highGain > lowGain && highGain <= 0.66f)
    }

    @Test
    fun `classic 90s pokedex voice formats species class and shapes wav audio cleanly`() {
        val charizard = LocalPokedexCatalog.byId.getValue(6)
        val classicScript = DataDexVoiceSpeaker.buildSpokenSummary(
            pokemon = charizard,
            includeDetails = true,
            includeAbilities = true,
            classic90sMode = true
        )
        assertTrue(classicScript.startsWith("Charizard. The Flame Pokémon."))
        assertTrue(classicScript.contains("Registered abilities:"))

        // Build a synthetic 22050 Hz mono 16-bit WAV buffer and verify decode + DSP shaping
        val sampleRate = 22050
        val pcmSamples = ShortArray(2048) { idx ->
            (kotlin.math.sin(2.0 * kotlin.math.PI * 220.0 * idx / sampleRate) * 14000.0).toInt().toShort()
        }
        val wavBytes = java.nio.ByteBuffer.allocate(44 + pcmSamples.size * 2)
            .order(java.nio.ByteOrder.LITTLE_ENDIAN)
            .apply {
                put("RIFF".toByteArray(Charsets.US_ASCII))
                putInt(36 + pcmSamples.size * 2)
                put("WAVE".toByteArray(Charsets.US_ASCII))
                put("fmt ".toByteArray(Charsets.US_ASCII))
                putInt(16)
                putShort(1) // PCM
                putShort(1) // Mono
                putInt(sampleRate)
                putInt(sampleRate * 2)
                putShort(2)
                putShort(16)
                put("data".toByteArray(Charsets.US_ASCII))
                putInt(pcmSamples.size * 2)
                pcmSamples.forEach { putShort(it) }
            }
            .array()

        val decoded = RetroPokedexDsp.decodeWavToMonoPcm(wavBytes)
        assertNotNull(decoded)
        assertEquals(sampleRate, decoded!!.sampleRate)
        assertEquals(pcmSamples.size, decoded.samples.size)

        val shaped = RetroPokedexDsp.processClassic90sVoice(decoded.samples, decoded.sampleRate)
        assertEquals(decoded.samples.size, shaped.size)
        assertTrue(shaped.any { it != 0.toShort() })

        val chime = RetroPokedexDsp.generateClassicPokedexChime(sampleRate)
        assertTrue(chime.size > sampleRate / 12)
        assertTrue(chime.any { it != 0.toShort() })
    }
}
