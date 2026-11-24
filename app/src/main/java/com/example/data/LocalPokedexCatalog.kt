package com.example.data

import com.example.model.BaseStats
import com.example.model.DataSource
import com.example.model.PokemonAbilityInfo
import com.example.model.PokemonEntry
import com.example.model.PokemonType

/**
 * Rich offline Pokédex entries for instant 0ms overlay rendering plus ML Vision label hints
 * when identifying creatures visually without visible OCR text.
 */
object LocalPokedexCatalog {

    private fun ab(name: String, hidden: Boolean = false) = PokemonAbilityInfo(name, hidden)

    private fun entry(
        id: Int,
        name: String,
        t1: PokemonType,
        t2: PokemonType? = null,
        hp: Int,
        atk: Int,
        def: Int,
        spa: Int,
        spd: Int,
        spe: Int,
        abilities: List<PokemonAbilityInfo>,
        category: String
    ) = PokemonEntry(
        id = id,
        name = name,
        types = listOfNotNull(t1, t2),
        baseStats = BaseStats(hp, atk, def, spa, spd, spe),
        abilityDetails = abilities,
        category = category,
        source = DataSource.BUNDLED_DEX
    )

    val bundledEntries: List<PokemonEntry> = listOf(
        entry(1, "Bulbasaur", PokemonType.GRASS, PokemonType.POISON, 45, 49, 49, 65, 65, 45, listOf(ab("Overgrow"), ab("Chlorophyll", true)), "Seed Pokémon"),
        entry(3, "Venusaur", PokemonType.GRASS, PokemonType.POISON, 80, 82, 83, 100, 100, 80, listOf(ab("Overgrow"), ab("Chlorophyll", true)), "Seed Pokémon"),
        entry(4, "Charmander", PokemonType.FIRE, null, 39, 52, 43, 60, 50, 65, listOf(ab("Blaze"), ab("Solar Power", true)), "Lizard Pokémon"),
        entry(6, "Charizard", PokemonType.FIRE, PokemonType.FLYING, 78, 84, 78, 109, 85, 100, listOf(ab("Blaze"), ab("Solar Power", true)), "Flame Pokémon"),
        entry(7, "Squirtle", PokemonType.WATER, null, 44, 48, 65, 50, 64, 43, listOf(ab("Torrent"), ab("Rain Dish", true)), "Tiny Turtle Pokémon"),
        entry(9, "Blastoise", PokemonType.WATER, null, 79, 83, 100, 85, 105, 78, listOf(ab("Torrent"), ab("Rain Dish", true)), "Shellfish Pokémon"),
        entry(12, "Butterfree", PokemonType.BUG, PokemonType.FLYING, 60, 45, 50, 90, 80, 70, listOf(ab("Compound Eyes"), ab("Tinted Lens", true)), "Butterfly Pokémon"),
        entry(18, "Pidgeot", PokemonType.NORMAL, PokemonType.FLYING, 83, 80, 75, 70, 70, 101, listOf(ab("Keen Eye"), ab("Tangled Feet"), ab("Big Pecks", true)), "Bird Pokémon"),
        entry(25, "Pikachu", PokemonType.ELECTRIC, null, 35, 55, 40, 50, 50, 90, listOf(ab("Static"), ab("Lightning Rod", true)), "Mouse Pokémon"),
        entry(26, "Raichu", PokemonType.ELECTRIC, null, 60, 90, 55, 90, 80, 110, listOf(ab("Static"), ab("Lightning Rod", true)), "Mouse Pokémon"),
        entry(38, "Ninetales", PokemonType.FIRE, null, 73, 76, 75, 81, 100, 100, listOf(ab("Flash Fire"), ab("Drought", true)), "Fox Pokémon"),
        entry(39, "Jigglypuff", PokemonType.NORMAL, PokemonType.FAIRY, 115, 45, 20, 45, 25, 20, listOf(ab("Cute Charm"), ab("Competitive"), ab("Friend Guard", true)), "Balloon Pokémon"),
        entry(52, "Meowth", PokemonType.NORMAL, null, 40, 45, 35, 40, 40, 90, listOf(ab("Pickup"), ab("Technician"), ab("Unnerve", true)), "Scratch Cat Pokémon"),
        entry(54, "Psyduck", PokemonType.WATER, null, 50, 52, 48, 65, 50, 55, listOf(ab("Damp"), ab("Cloud Nine"), ab("Swift Swim", true)), "Duck Pokémon"),
        entry(59, "Arcanine", PokemonType.FIRE, null, 90, 110, 80, 100, 80, 95, listOf(ab("Intimidate"), ab("Flash Fire"), ab("Justified", true)), "Legendary Pokémon"),
        entry(65, "Alakazam", PokemonType.PSYCHIC, null, 55, 50, 45, 135, 95, 120, listOf(ab("Synchronize"), ab("Inner Focus"), ab("Magic Guard", true)), "Psi Pokémon"),
        entry(68, "Machamp", PokemonType.FIGHTING, null, 90, 130, 80, 65, 85, 55, listOf(ab("Guts"), ab("No Guard"), ab("Steadfast", true)), "Superpower Pokémon"),
        entry(76, "Golem", PokemonType.ROCK, PokemonType.GROUND, 80, 120, 130, 55, 65, 45, listOf(ab("Rock Head"), ab("Sturdy"), ab("Sand Veil", true)), "Megaton Pokémon"),
        entry(80, "Slowbro", PokemonType.WATER, PokemonType.PSYCHIC, 95, 75, 110, 100, 80, 30, listOf(ab("Oblivious"), ab("Own Tempo"), ab("Regenerator", true)), "Hermit Crab Pokémon"),
        entry(94, "Gengar", PokemonType.GHOST, PokemonType.POISON, 60, 65, 60, 130, 75, 110, listOf(ab("Cursed Body")), "Shadow Pokémon"),
        entry(99, "Kingler", PokemonType.WATER, null, 55, 130, 115, 50, 50, 75, listOf(ab("Hyper Cutter"), ab("Shell Armor"), ab("Sheer Force", true)), "Pincer Pokémon"),
        entry(123, "Scyther", PokemonType.BUG, PokemonType.FLYING, 70, 110, 80, 55, 80, 105, listOf(ab("Swarm"), ab("Technician"), ab("Steadfast", true)), "Mantis Pokémon"),
        entry(130, "Gyarados", PokemonType.WATER, PokemonType.FLYING, 95, 125, 79, 60, 100, 81, listOf(ab("Intimidate"), ab("Moxie", true)), "Atrocious Pokémon"),
        entry(131, "Lapras", PokemonType.WATER, PokemonType.ICE, 130, 85, 80, 85, 95, 60, listOf(ab("Water Absorb"), ab("Shell Armor"), ab("Hydration", true)), "Transport Pokémon"),
        entry(133, "Eevee", PokemonType.NORMAL, null, 55, 55, 50, 45, 65, 55, listOf(ab("Run Away"), ab("Adaptability"), ab("Anticipation", true)), "Evolution Pokémon"),
        entry(134, "Vaporeon", PokemonType.WATER, null, 130, 65, 60, 110, 95, 65, listOf(ab("Water Absorb"), ab("Hydration", true)), "Bubble Jet Pokémon"),
        entry(135, "Jolteon", PokemonType.ELECTRIC, null, 65, 65, 60, 110, 95, 130, listOf(ab("Volt Absorb"), ab("Quick Feet", true)), "Lightning Pokémon"),
        entry(136, "Flareon", PokemonType.FIRE, null, 65, 130, 60, 95, 110, 65, listOf(ab("Flash Fire"), ab("Guts", true)), "Flame Pokémon"),
        entry(143, "Snorlax", PokemonType.NORMAL, null, 160, 110, 65, 65, 110, 30, listOf(ab("Immunity"), ab("Thick Fat"), ab("Gluttony", true)), "Sleeping Pokémon"),
        entry(144, "Articuno", PokemonType.ICE, PokemonType.FLYING, 90, 85, 100, 95, 125, 85, listOf(ab("Pressure"), ab("Snow Cloak", true)), "Freeze Pokémon"),
        entry(145, "Zapdos", PokemonType.ELECTRIC, PokemonType.FLYING, 90, 90, 85, 125, 90, 100, listOf(ab("Pressure"), ab("Static", true)), "Electric Pokémon"),
        entry(146, "Moltres", PokemonType.FIRE, PokemonType.FLYING, 90, 100, 90, 125, 85, 90, listOf(ab("Pressure"), ab("Flame Body", true)), "Flame Pokémon"),
        entry(149, "Dragonite", PokemonType.DRAGON, PokemonType.FLYING, 91, 134, 95, 100, 100, 80, listOf(ab("Inner Focus"), ab("Multiscale", true)), "Dragon Pokémon"),
        entry(150, "Mewtwo", PokemonType.PSYCHIC, null, 106, 110, 90, 154, 90, 130, listOf(ab("Pressure"), ab("Unnerve", true)), "Genetic Pokémon"),
        entry(151, "Mew", PokemonType.PSYCHIC, null, 100, 100, 100, 100, 100, 100, listOf(ab("Synchronize")), "New Species Pokémon"),
        entry(169, "Crobat", PokemonType.POISON, PokemonType.FLYING, 85, 90, 80, 70, 80, 130, listOf(ab("Inner Focus"), ab("Infiltrator", true)), "Bat Pokémon"),
        entry(181, "Ampharos", PokemonType.ELECTRIC, null, 90, 75, 85, 115, 90, 55, listOf(ab("Static"), ab("Plus", true)), "Light Pokémon"),
        entry(184, "Azumarill", PokemonType.WATER, PokemonType.FAIRY, 100, 50, 80, 60, 80, 50, listOf(ab("Thick Fat"), ab("Huge Power"), ab("Sap Sipper", true)), "Aqua Rabbit Pokémon"),
        entry(196, "Espeon", PokemonType.PSYCHIC, null, 65, 65, 60, 130, 95, 110, listOf(ab("Synchronize"), ab("Magic Bounce", true)), "Sun Pokémon"),
        entry(197, "Umbreon", PokemonType.DARK, null, 95, 65, 110, 60, 130, 65, listOf(ab("Synchronize"), ab("Inner Focus", true)), "Moonlight Pokémon"),
        entry(212, "Scizor", PokemonType.BUG, PokemonType.STEEL, 70, 130, 100, 55, 80, 65, listOf(ab("Swarm"), ab("Technician"), ab("Light Metal", true)), "Pincer Pokémon"),
        entry(214, "Heracross", PokemonType.BUG, PokemonType.FIGHTING, 80, 125, 75, 40, 95, 85, listOf(ab("Swarm"), ab("Guts"), ab("Moxie", true)), "Single Horn Pokémon"),
        entry(227, "Skarmory", PokemonType.STEEL, PokemonType.FLYING, 65, 80, 140, 40, 70, 70, listOf(ab("Keen Eye"), ab("Sturdy"), ab("Weak Armor", true)), "Armor Bird Pokémon"),
        entry(248, "Tyranitar", PokemonType.ROCK, PokemonType.DARK, 100, 134, 110, 95, 100, 61, listOf(ab("Sand Stream"), ab("Unnerve", true)), "Armor Pokémon"),
        entry(249, "Lugia", PokemonType.PSYCHIC, PokemonType.FLYING, 106, 90, 130, 90, 154, 110, listOf(ab("Pressure"), ab("Multiscale", true)), "Diving Pokémon"),
        entry(250, "Ho-Oh", PokemonType.FIRE, PokemonType.FLYING, 106, 130, 90, 110, 154, 90, listOf(ab("Pressure"), ab("Regenerator", true)), "Rainbow Pokémon"),
        entry(254, "Sceptile", PokemonType.GRASS, null, 70, 85, 65, 105, 85, 120, listOf(ab("Overgrow"), ab("Unburden", true)), "Forest Pokémon"),
        entry(257, "Blaziken", PokemonType.FIRE, PokemonType.FIGHTING, 80, 120, 70, 110, 70, 80, listOf(ab("Blaze"), ab("Speed Boost", true)), "Blaze Pokémon"),
        entry(260, "Swampert", PokemonType.WATER, PokemonType.GROUND, 100, 110, 90, 85, 90, 60, listOf(ab("Torrent"), ab("Damp", true)), "Mud Fish Pokémon"),
        entry(282, "Gardevoir", PokemonType.PSYCHIC, PokemonType.FAIRY, 68, 65, 65, 125, 115, 80, listOf(ab("Synchronize"), ab("Trace"), ab("Telepathy", true)), "Embrace Pokémon"),
        entry(373, "Salamence", PokemonType.DRAGON, PokemonType.FLYING, 95, 135, 80, 110, 80, 100, listOf(ab("Intimidate"), ab("Moxie", true)), "Dragon Pokémon"),
        entry(376, "Metagross", PokemonType.STEEL, PokemonType.PSYCHIC, 80, 135, 130, 95, 90, 70, listOf(ab("Clear Body"), ab("Light Metal", true)), "Iron Leg Pokémon"),
        entry(382, "Kyogre", PokemonType.WATER, null, 100, 100, 90, 150, 140, 90, listOf(ab("Drizzle")), "Sea Basin Pokémon"),
        entry(383, "Groudon", PokemonType.GROUND, null, 100, 150, 140, 100, 90, 90, listOf(ab("Drought")), "Continent Pokémon"),
        entry(384, "Rayquaza", PokemonType.DRAGON, PokemonType.FLYING, 105, 150, 90, 150, 90, 95, listOf(ab("Air Lock")), "Sky High Pokémon"),
        entry(392, "Infernape", PokemonType.FIRE, PokemonType.FIGHTING, 76, 104, 71, 104, 71, 108, listOf(ab("Blaze"), ab("Iron Fist", true)), "Flame Pokémon"),
        entry(395, "Empoleon", PokemonType.WATER, PokemonType.STEEL, 84, 86, 88, 111, 101, 60, listOf(ab("Torrent"), ab("Competitive", true)), "Emperor Pokémon"),
        entry(445, "Garchomp", PokemonType.DRAGON, PokemonType.GROUND, 108, 130, 95, 80, 85, 102, listOf(ab("Sand Veil"), ab("Rough Skin", true)), "Mach Pokémon"),
        entry(448, "Lucario", PokemonType.FIGHTING, PokemonType.STEEL, 70, 110, 70, 115, 70, 90, listOf(ab("Steadfast"), ab("Inner Focus"), ab("Justified", true)), "Aura Pokémon"),
        entry(461, "Weavile", PokemonType.DARK, PokemonType.ICE, 70, 120, 65, 45, 85, 125, listOf(ab("Pressure"), ab("Pickpocket", true)), "Sharp Claw Pokémon"),
        entry(468, "Togekiss", PokemonType.FAIRY, PokemonType.FLYING, 85, 50, 95, 120, 115, 80, listOf(ab("Hustle"), ab("Serene Grace"), ab("Super Luck", true)), "Jubilee Pokémon"),
        entry(479, "Rotom", PokemonType.ELECTRIC, PokemonType.GHOST, 50, 50, 77, 95, 77, 91, listOf(ab("Levitate")), "Plasma Pokémon"),
        entry(483, "Dialga", PokemonType.STEEL, PokemonType.DRAGON, 100, 120, 120, 150, 100, 90, listOf(ab("Pressure"), ab("Telepathy", true)), "Temporal Pokémon"),
        entry(484, "Palkia", PokemonType.WATER, PokemonType.DRAGON, 90, 120, 100, 150, 120, 100, listOf(ab("Pressure"), ab("Telepathy", true)), "Spatial Pokémon"),
        entry(487, "Giratina", PokemonType.GHOST, PokemonType.DRAGON, 150, 100, 120, 100, 120, 90, listOf(ab("Pressure"), ab("Telepathy", true)), "Renegade Pokémon"),
        entry(491, "Darkrai", PokemonType.DARK, null, 70, 90, 90, 135, 90, 125, listOf(ab("Bad Dreams")), "Pitch-Black Pokémon"),
        entry(493, "Arceus", PokemonType.NORMAL, null, 120, 120, 120, 120, 120, 120, listOf(ab("Multitype")), "Alpha Pokémon"),
        entry(530, "Excadrill", PokemonType.GROUND, PokemonType.STEEL, 110, 135, 60, 50, 65, 88, listOf(ab("Sand Rush"), ab("Sand Force"), ab("Mold Breaker", true)), "Subterrene Pokémon"),
        entry(571, "Zoroark", PokemonType.DARK, null, 60, 105, 60, 120, 60, 105, listOf(ab("Illusion")), "Illusion Fox Pokémon"),
        entry(609, "Chandelure", PokemonType.GHOST, PokemonType.FIRE, 60, 55, 90, 145, 90, 80, listOf(ab("Flash Fire"), ab("Flame Body"), ab("Infiltrator", true)), "Luring Pokémon"),
        entry(635, "Hydreigon", PokemonType.DARK, PokemonType.DRAGON, 92, 105, 90, 125, 90, 98, listOf(ab("Levitate")), "Brutal Pokémon"),
        entry(637, "Volcarona", PokemonType.BUG, PokemonType.FIRE, 85, 60, 65, 135, 105, 100, listOf(ab("Flame Body"), ab("Swarm", true)), "Sun Pokémon"),
        entry(658, "Greninja", PokemonType.WATER, PokemonType.DARK, 72, 95, 67, 103, 71, 122, listOf(ab("Torrent"), ab("Protean", true)), "Ninja Pokémon"),
        entry(681, "Aegislash", PokemonType.STEEL, PokemonType.GHOST, 60, 50, 140, 50, 140, 60, listOf(ab("Stance Change")), "Royal Sword Pokémon"),
        entry(700, "Sylveon", PokemonType.FAIRY, null, 95, 65, 65, 110, 130, 60, listOf(ab("Cute Charm"), ab("Pixilate", true)), "Intertwining Pokémon"),
        entry(724, "Decidueye", PokemonType.GRASS, PokemonType.GHOST, 78, 107, 75, 100, 100, 70, listOf(ab("Overgrow"), ab("Long Reach", true)), "Arrow Quill Pokémon"),
        entry(727, "Incineroar", PokemonType.FIRE, PokemonType.DARK, 95, 115, 90, 80, 90, 60, listOf(ab("Blaze"), ab("Intimidate", true)), "Heel Pokémon"),
        entry(748, "Toxapex", PokemonType.POISON, PokemonType.WATER, 50, 63, 152, 53, 142, 35, listOf(ab("Merciless"), ab("Limber"), ab("Regenerator", true)), "Brutal Star Pokémon"),
        entry(778, "Mimikyu", PokemonType.GHOST, PokemonType.FAIRY, 55, 90, 80, 50, 105, 96, listOf(ab("Disguise")), "Disguise Pokémon"),
        entry(823, "Corviknight", PokemonType.FLYING, PokemonType.STEEL, 98, 87, 105, 53, 85, 67, listOf(ab("Pressure"), ab("Unnerve"), ab("Mirror Armor", true)), "Raven Pokémon"),
        entry(887, "Dragapult", PokemonType.DRAGON, PokemonType.GHOST, 88, 120, 75, 100, 75, 142, listOf(ab("Clear Body"), ab("Infiltrator"), ab("Cursed Body", true)), "Stealth Pokémon"),
        entry(888, "Zacian", PokemonType.FAIRY, PokemonType.STEEL, 92, 150, 115, 80, 115, 148, listOf(ab("Intrepid Sword")), "Warrior Pokémon"),
        entry(937, "Ceruledge", PokemonType.FIRE, PokemonType.GHOST, 75, 125, 80, 60, 100, 85, listOf(ab("Flash Fire"), ab("Weak Armor", true)), "Fire Blades Pokémon"),
        entry(959, "Tinkaton", PokemonType.FAIRY, PokemonType.STEEL, 85, 75, 77, 70, 105, 94, listOf(ab("Mold Breaker"), ab("Own Tempo"), ab("Pickpocket", true)), "Hammer Pokémon"),
        entry(983, "Kingambit", PokemonType.DARK, PokemonType.STEEL, 100, 135, 120, 60, 85, 50, listOf(ab("Defiant"), ab("Supreme Overlord"), ab("Pressure", true)), "Big Blade Pokémon"),
        entry(987, "Flutter Mane", PokemonType.GHOST, PokemonType.FAIRY, 55, 55, 55, 135, 135, 135, listOf(ab("Protosynthesis")), "Paradox Pokémon"),
        entry(998, "Baxcalibur", PokemonType.DRAGON, PokemonType.ICE, 115, 145, 92, 75, 86, 87, listOf(ab("Thermal Exchange"), ab("Ice Body", true)), "Ice Dragon Pokémon"),
        entry(1000, "Gholdengo", PokemonType.STEEL, PokemonType.GHOST, 87, 60, 95, 133, 91, 84, listOf(ab("Good as Gold")), "Coin Entity Pokémon"),
        entry(1007, "Koraidon", PokemonType.FIGHTING, PokemonType.DRAGON, 100, 135, 115, 85, 100, 135, listOf(ab("Orichalcum Pulse")), "Paradox Pokémon"),
        entry(1008, "Miraidon", PokemonType.ELECTRIC, PokemonType.DRAGON, 100, 85, 100, 135, 115, 135, listOf(ab("Hadron Engine")), "Paradox Pokémon")
    )

    val byId: Map<Int, PokemonEntry> by lazy { bundledEntries.associateBy { it.id } }

    /**
     * Maps ML Kit ImageLabeler classes to likely Pokémon IDs when no OCR text is present.
     */
    val visionTagToPokemonId: Map<String, Int> = mapOf(
        "mouse" to 25,       // Pikachu
        "rodent" to 25,      // Pikachu
        "dragon" to 6,       // Charizard
        "reptile" to 4,      // Charmander
        "lizard" to 4,       // Charmander
        "dinosaur" to 248,   // Tyranitar
        "turtle" to 7,       // Squirtle
        "tortoise" to 9,     // Blastoise
        "bird" to 18,        // Pidgeot
        "owl" to 724,        // Decidueye
        "crow" to 823,       // Corviknight
        "penguin" to 395,    // Empoleon
        "cat" to 52,         // Meowth
        "dog" to 59,         // Arcanine
        "fox" to 133,        // Eevee
        "duck" to 54,        // Psyduck
        "butterfly" to 12,   // Butterfree
        "insect" to 123,     // Scyther
        "frog" to 658,       // Greninja
        "amphibian" to 260,  // Swampert
        "fish" to 130,       // Gyarados
        "shark" to 445,      // Garchomp
        "crab" to 99,        // Kingler
        "bat" to 169,        // Crobat
        "rabbit" to 184,     // Azumarill
        "monkey" to 392,     // Infernape
        "bear" to 143,       // Snorlax
        "plant" to 1,        // Bulbasaur
        "flower" to 3,       // Venusaur
        "fire" to 6,         // Charizard
        "flame" to 6,        // Charizard
        "lightning" to 25,   // Pikachu
        "ghost" to 94,       // Gengar
        "plush" to 25,       // Pikachu
        "stuffed toy" to 25, // Pikachu
        "toy" to 25          // Pikachu
    )
}
