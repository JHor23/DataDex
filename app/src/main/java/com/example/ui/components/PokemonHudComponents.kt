package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.AbilityDescriptions
import com.example.data.NatureAdvisor
import com.example.model.PokemonAbilityInfo
import com.example.model.PokemonEntry
import com.example.model.PokemonType
import com.example.model.ScanMatch
import com.example.model.TypeMultiplier
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DexOutline
import com.example.ui.theme.DexSurface
import com.example.ui.theme.DexSurfaceElevated
import com.example.ui.theme.DexVoid
import com.example.ui.theme.EmeraldMatch
import com.example.ui.theme.ScannerRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoltGold
import com.example.ui.theme.VoltGoldContainer

@Composable
fun TypeBadgeChip(
    type: PokemonType,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = type.badgeColor.copy(alpha = 0.22f),
        border = BorderStroke(1.dp, type.badgeColor.copy(alpha = 0.75f))
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (compact) 7.dp else 10.dp,
                vertical = if (compact) 3.dp else 5.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (compact) 6.dp else 8.dp)
                    .clip(CircleShape)
                    .background(type.badgeColor)
            )
            Text(
                text = type.displayName.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = if (compact) 10.sp else 11.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = TextPrimary
            )
        }
    }
}

@Composable
fun WeaknessMultiplierChip(
    item: TypeMultiplier,
    modifier: Modifier = Modifier
) {
    val multText = when {
        item.multiplier >= 3.9f -> "4×"
        item.multiplier >= 1.9f -> "2×"
        item.multiplier == 0f -> "0×"
        item.multiplier <= 0.26f -> "¼×"
        else -> "½×"
    }
    val accent = when {
        item.multiplier >= 3.9f -> ScannerRed
        item.multiplier >= 1.9f -> VoltGold
        item.multiplier == 0f -> CyberCyan
        else -> EmeraldMatch
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(7.dp),
        color = item.type.badgeColor.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = multText,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = accent
            )
            Text(
                text = item.type.displayName.take(3).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TextPrimary
            )
        }
    }
}

@Composable
fun AbilitiesTogglePill(
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .testTag("overlay_abilities_toggle"),
        shape = RoundedCornerShape(10.dp),
        color = if (active) VoltGold.copy(alpha = 0.22f) else DexSurfaceElevated,
        border = BorderStroke(
            width = 1.dp,
            color = if (active) VoltGold else CyberCyan.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Toggle abilities and natures",
                tint = if (active) VoltGold else CyberCyan,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = if (active) "BUILDS ON" else "ABILITIES",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = if (active) VoltGold else TextPrimary
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AbilitiesAndNaturesSection(
    pokemon: PokemonEntry,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DexSurface.copy(alpha = 0.9f))
            .border(1.dp, VoltGold.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(
            text = "ABILITIES & POPULAR NATURES",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = VoltGold
        )

        if (pokemon.abilityDetails.isEmpty()) {
            Text(
                text = "Tap Pokémon in DataDex tab to sync abilities.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        } else {
            pokemon.abilityDetails.forEach { ability ->
                AbilityNatureRow(pokemon = pokemon, ability = ability)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AbilityNatureRow(
    pokemon: PokemonEntry,
    ability: PokemonAbilityInfo
) {
    val natures = NatureAdvisor.bestNaturesForAbility(pokemon, ability)
    val abilityDescription = AbilityDescriptions.resolveDescription(ability)
    val rowBg = if (ability.isHidden) VoltGoldContainer.copy(alpha = 0.42f) else DexSurfaceElevated.copy(alpha = 0.72f)
    val rowBorder = if (ability.isHidden) VoltGold.copy(alpha = 0.68f) else DexOutline

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(rowBg)
            .border(1.dp, rowBorder, RoundedCornerShape(9.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(5.dp),
                color = if (ability.isHidden) VoltGold else CyberCyan.copy(alpha = 0.18f)
            ) {
                Text(
                    text = if (ability.isHidden) "HIDDEN" else "ABILITY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (ability.isHidden) DexVoid else CyberCyan,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
            }
            Text(
                text = ability.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = if (ability.isHidden) VoltGold else TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = abilityDescription,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic
            ),
            color = TextSecondary,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .basicMarquee(
                    iterations = Int.MAX_VALUE,
                    initialDelayMillis = 1200,
                    velocity = 34.dp
                )
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            natures.forEach { nature ->
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = DexVoid.copy(alpha = 0.85f),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (ability.isHidden) VoltGold.copy(alpha = 0.45f) else CyberCyan.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = nature.name,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = nature.statDelta,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = EmeraldMatch
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatBarRow(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
    maxValue: Int = 180
) {
    val fraction by animateFloatAsState(
        targetValue = (value.toFloat() / maxValue.toFloat()).coerceIn(0.05f, 1f),
        animationSpec = tween(durationMillis = 380),
        label = "stat_bar_$label"
    )
    val barColor = when {
        value >= 120 -> EmeraldMatch
        value >= 90 -> CyberCyan
        value >= 65 -> VoltGold
        else -> ScannerRed
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.width(36.dp)
        )
        Text(
            text = value.toString().padStart(3, ' '),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
            modifier = Modifier.width(34.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(DexOutline.copy(alpha = 0.4f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(barColor.copy(alpha = 0.75f), barColor)
                        )
                    )
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PokemonTacticalBreakdown(
    pokemon: PokemonEntry,
    badgeLabel: String,
    confidence: Float?,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    showAbilitiesAndNatures: Boolean = false,
    headerTrailingContent: (@Composable () -> Unit)? = null
) {
    val weaknesses = pokemon.weaknessProfile
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp)
    ) {
        // Header row with artwork, Dex#, Name, and optional right-side toggle control
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(if (compact) 52.dp else 68.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                pokemon.types.first().badgeColor.copy(alpha = 0.35f),
                                DexSurfaceElevated
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        color = pokemon.types.first().badgeColor.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = pokemon.officialArtworkUrl,
                    contentDescription = "${pokemon.name} artwork",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(if (compact) 46.dp else 60.dp)
                        .padding(2.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = pokemon.formattedDexNumber,
                        style = MaterialTheme.typography.labelMedium,
                        color = CyberCyan
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CyberCyan.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = badgeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = pokemon.name,
                    style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 3.dp)
                ) {
                    pokemon.types.forEach { type ->
                        TypeBadgeChip(type = type, compact = true)
                    }
                }
            }

            if (headerTrailingContent != null) {
                headerTrailingContent()
            }
        }

        // Expandable Abilities & Best Natures panel
        AnimatedVisibility(visible = showAbilitiesAndNatures) {
            AbilitiesAndNaturesSection(pokemon = pokemon)
        }

        // Weaknesses section
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "WEAKNESSES",
                style = MaterialTheme.typography.labelSmall,
                color = ScannerRed
            )
            if (weaknesses.allWeaknesses.isEmpty()) {
                Text(
                    text = "No elemental weaknesses",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    weaknesses.allWeaknesses.forEach { item ->
                        WeaknessMultiplierChip(item = item)
                    }
                }
            }
        }

        // Resistances & Immunities
        if (weaknesses.allResistancesAndImmunities.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "RESISTANCES & IMMUNITIES",
                    style = MaterialTheme.typography.labelSmall,
                    color = EmeraldMatch
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    weaknesses.allResistancesAndImmunities.take(if (compact) 6 else 12).forEach { item ->
                        WeaknessMultiplierChip(item = item)
                    }
                }
            }
        }

        // Base Stats
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DexSurface.copy(alpha = 0.72f))
                .border(1.dp, DexOutline.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BASE STATS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Text(
                    text = "BST ${pokemon.baseStats.total}",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = VoltGold
                )
            }
            StatBarRow("HP", pokemon.baseStats.hp)
            StatBarRow("ATK", pokemon.baseStats.attack)
            StatBarRow("DEF", pokemon.baseStats.defense)
            StatBarRow("SPA", pokemon.baseStats.spAtk)
            StatBarRow("SPD", pokemon.baseStats.spDef)
            StatBarRow("SPE", pokemon.baseStats.speed)
        }
    }
}

@Composable
fun ScanMatchSummaryBanner(
    match: ScanMatch,
    modifier: Modifier = Modifier,
    showAbilitiesAndNatures: Boolean = false,
    onToggleAbilities: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = DexSurfaceElevated.copy(alpha = 0.96f),
        border = BorderStroke(
            width = 1.dp,
            color = CyberCyan.copy(alpha = 0.45f)
        )
    ) {
        PokemonTacticalBreakdown(
            pokemon = match.pokemon,
            badgeLabel = match.method.label,
            confidence = match.confidence,
            compact = true,
            showAbilitiesAndNatures = showAbilitiesAndNatures,
            headerTrailingContent = if (onToggleAbilities != null) {
                {
                    AbilitiesTogglePill(
                        active = showAbilitiesAndNatures,
                        onClick = onToggleAbilities
                    )
                }
            } else null,
            modifier = Modifier.padding(12.dp)
        )
    }
}
