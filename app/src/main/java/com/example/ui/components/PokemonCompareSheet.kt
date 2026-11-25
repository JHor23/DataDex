package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.NatureAdvisor
import com.example.model.DataSource
import com.example.model.PokemonEntry
import com.example.model.PokemonType
import com.example.model.TypeEffectivenessChart
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
import com.example.viewmodel.CompareSuggestion
import com.example.viewmodel.CompareUiState
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PokemonCompareModal(
    state: CompareUiState,
    onQueryChange: (String) -> Unit,
    onSelectChallenger: (PokemonEntry) -> Unit,
    onClearChallenger: () -> Unit,
    onSwapSides: () -> Unit,
    onDismiss: () -> Unit
) {
    val base = state.basePokemon ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DexSurfaceElevated,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp)
                .testTag("pokemon_compare_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Sheet Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CyberCyan.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CompareArrows,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Tactical Compare",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = if (state.challengerPokemon != null) {
                                "${base.name} vs ${state.challengerPokemon.name}"
                            } else {
                                "Pick any Pokémon to compare with ${base.name}"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (state.challengerPokemon != null) {
                        IconButton(
                            onClick = onSwapSides,
                            modifier = Modifier.testTag("compare_swap_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Swap Pokémon sides",
                                tint = CyberCyan
                            )
                        }
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onClearChallenger)
                                .testTag("compare_change_button"),
                            shape = RoundedCornerShape(8.dp),
                            color = DexSurface,
                            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.55f))
                        ) {
                            Text(
                                text = "Change",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = CyberCyan,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("compare_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close compare",
                            tint = TextSecondary
                        )
                    }
                }
            }

            if (state.challengerPokemon == null) {
                ComparePickerContent(
                    base = base,
                    searchQuery = state.searchQuery,
                    searchResults = state.searchResults,
                    recentSuggestions = state.recentSuggestions,
                    similarSuggestions = state.similarSuggestions,
                    onQueryChange = onQueryChange,
                    onSelectChallenger = onSelectChallenger
                )
            } else {
                if (state.isLoadingPokemon) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(4.dp)),
                        color = CyberCyan,
                        trackColor = DexSurface
                    )
                }
                CompareHeadToHeadContent(
                    left = base,
                    right = state.challengerPokemon,
                    onPickAnother = onClearChallenger
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ComparePickerContent(
    base: PokemonEntry,
    searchQuery: String,
    searchResults: List<PokemonEntry>,
    recentSuggestions: List<PokemonEntry>,
    similarSuggestions: List<CompareSuggestion>,
    onQueryChange: (String) -> Unit,
    onSelectChallenger: (PokemonEntry) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Active Base Pokémon Summary Strip
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = DexSurface,
            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AsyncImage(
                    model = base.officialArtworkUrl,
                    contentDescription = "${base.name} artwork",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(44.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "CURRENT SCAN",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = CyberCyan
                        )
                        Text(
                            text = base.formattedDexNumber,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                    Text(
                        text = base.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        base.types.forEach { TypeBadgeChip(type = it, compact = true) }
                    }
                    Text(
                        text = "BST ${base.baseStats.total}",
                        style = MaterialTheme.typography.labelSmall,
                        color = VoltGold
                    )
                }
            }
        }

        // Pinned Search input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    text = "Search any Pokémon by name or Dex #...",
                    color = TextMuted
                )
            },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = CyberCyan)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.testTag("compare_search_clear")
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextSecondary)
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = DexOutline,
                focusedContainerColor = DexSurface,
                unfocusedContainerColor = DexSurface,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("compare_search_input")
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 430.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (searchQuery.isNotBlank()) {
                item(key = "search_count") {
                    Text(
                        text = if (searchResults.isEmpty()) "NO MATCHES FOUND" else "${searchResults.size} MATCHES",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
                if (searchResults.isEmpty()) {
                    item(key = "search_empty") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = DexSurface,
                            border = BorderStroke(1.dp, DexOutline)
                        ) {
                            Text(
                                text = "Try searching a Pokémon name like Garchomp, Pikachu, or a Dex number like 150.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(searchResults, key = { it.id }) { entry ->
                        CompareSearchResultRow(
                            entry = entry,
                            onClick = { onSelectChallenger(entry) }
                        )
                    }
                }
            } else {
                if (recentSuggestions.isNotEmpty()) {
                    item(key = "recent_section") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "RECENT SCANS",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = CyberCyan
                                )
                            }
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(end = 4.dp)
                            ) {
                                items(recentSuggestions, key = { it.id }) { entry ->
                                    CompareSuggestionMiniCard(
                                        entry = entry,
                                        tagLabel = "RECENT",
                                        tagColor = CyberCyan,
                                        onClick = { onSelectChallenger(entry) },
                                        modifier = Modifier
                                            .width(142.dp)
                                            .testTag("compare_recent_${entry.id}")
                                    )
                                }
                            }
                        }
                    }
                }

                if (similarSuggestions.isNotEmpty()) {
                    item(key = "similar_header") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = VoltGold,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "SIMILAR & RIVAL MATCHUPS",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = VoltGold
                            )
                        }
                    }
                    val pairs = similarSuggestions.chunked(2)
                    items(pairs.size, key = { idx -> "similar_row_$idx" }) { idx ->
                        val pair = pairs[idx]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            pair.forEach { item ->
                                val tagColor = when (item.reason) {
                                    "Same Type" -> EmeraldMatch
                                    "Type Counter" -> ScannerRed
                                    "Type Target" -> CyberCyan
                                    else -> VoltGold
                                }
                                CompareSuggestionMiniCard(
                                    entry = item.pokemon,
                                    tagLabel = item.reason.uppercase(),
                                    tagColor = tagColor,
                                    onClick = { onSelectChallenger(item.pokemon) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .testTag("compare_similar_${item.pokemon.id}")
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompareSearchResultRow(
    entry: PokemonEntry,
    onClick: () -> Unit
) {
    val isMinimalStub = entry.source == DataSource.BUNDLED_DEX && entry.category.startsWith("Tap to load")
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("compare_search_item_${entry.id}"),
        shape = RoundedCornerShape(14.dp),
        color = DexSurface,
        border = BorderStroke(1.dp, DexOutline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AsyncImage(
                model = entry.officialArtworkUrl,
                contentDescription = "${entry.name} artwork",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(44.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "${entry.formattedDexNumber} • ${entry.name}",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isMinimalStub) {
                    Text(
                        text = "Tap to load live PokéAPI stats",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        entry.types.forEach { TypeBadgeChip(type = it, compact = true) }
                    }
                }
            }
            if (!isMinimalStub) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DexSurfaceElevated,
                    border = BorderStroke(1.dp, DexOutline)
                ) {
                    Text(
                        text = "BST ${entry.baseStats.total}",
                        style = MaterialTheme.typography.labelSmall,
                        color = VoltGold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompareSuggestionMiniCard(
    entry: PokemonEntry,
    tagLabel: String,
    tagColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMinimalStub = entry.source == DataSource.BUNDLED_DEX && entry.category.startsWith("Tap to load")
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = DexSurface,
        border = BorderStroke(1.dp, DexOutline)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(5.dp),
                    color = tagColor.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = tagLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = tagColor,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
                Text(
                    text = entry.formattedDexNumber,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = TextMuted
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AsyncImage(
                    model = entry.officialArtworkUrl,
                    contentDescription = "${entry.name} artwork",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(38.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isMinimalStub) "Live lookup" else "BST ${entry.baseStats.total}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMinimalStub) CyberCyan else TextSecondary
                    )
                }
            }

            if (!isMinimalStub) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    entry.types.forEach { TypeBadgeChip(type = it, compact = true) }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompareHeadToHeadContent(
    left: PokemonEntry,
    right: PokemonEntry,
    onPickAnother: () -> Unit
) {
    val leftBestAttack = bestOffensiveTypeAgainst(left.types, right.types)
    val rightBestAttack = bestOffensiveTypeAgainst(right.types, left.types)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Face-Off Header Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = DexSurface,
            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CompareFighterColumn(
                    pokemon = left,
                    accent = CyberCyan,
                    isBstWinner = left.baseStats.total > right.baseStats.total,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = CircleShape,
                    color = DexSurfaceElevated,
                    border = BorderStroke(1.dp, VoltGold.copy(alpha = 0.7f))
                ) {
                    Text(
                        text = "VS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = VoltGold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)
                    )
                }

                CompareFighterColumn(
                    pokemon = right,
                    accent = VoltGold,
                    isBstWinner = right.baseStats.total > left.baseStats.total,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 2. Type Matchup Verdict Card
        val matchupVerdict = when {
            leftBestAttack.second > rightBestAttack.second -> "${left.name} Type Edge" to EmeraldMatch
            rightBestAttack.second > leftBestAttack.second -> "${right.name} Type Edge" to VoltGold
            else -> "Even Typing" to CyberCyan
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = DexSurface,
            border = BorderStroke(1.dp, DexOutline)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HEAD-TO-HEAD TYPING",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = matchupVerdict.second.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = matchupVerdict.first.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = matchupVerdict.second,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OffenseEdgeBox(
                        attackerName = left.name,
                        bestType = leftBestAttack.first,
                        multiplier = leftBestAttack.second,
                        modifier = Modifier.weight(1f)
                    )
                    OffenseEdgeBox(
                        attackerName = right.name,
                        bestType = rightBestAttack.first,
                        multiplier = rightBestAttack.second,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Base Stat Duel Card
        val speedDiff = left.baseStats.speed - right.baseStats.speed
        val speedLabel = when {
            speedDiff > 0 -> "${left.name} +$speedDiff SPE"
            speedDiff < 0 -> "${right.name} +${-speedDiff} SPE"
            else -> "Speed Tie"
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = DexSurface,
            border = BorderStroke(1.dp, DexOutline)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BASE STAT DUEL",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CyberCyan.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = speedLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberCyan,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }

                DuelStatRow("HP", left.baseStats.hp, right.baseStats.hp)
                DuelStatRow("ATK", left.baseStats.attack, right.baseStats.attack)
                DuelStatRow("DEF", left.baseStats.defense, right.baseStats.defense)
                DuelStatRow("SPA", left.baseStats.spAtk, right.baseStats.spAtk)
                DuelStatRow("SPD", left.baseStats.spDef, right.baseStats.spDef)
                DuelStatRow("SPE", left.baseStats.speed, right.baseStats.speed)

                HorizontalDivider(color = DexOutline.copy(alpha = 0.6f))
                val bstDiff = left.baseStats.total - right.baseStats.total
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${left.baseStats.total}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (bstDiff > 0) EmeraldMatch else TextPrimary
                    )
                    Text(
                        text = if (bstDiff == 0) "BST TIED" else "TOTAL BST (${if (bstDiff > 0) "${left.name} +$bstDiff" else "${right.name} +${-bstDiff}"})",
                        style = MaterialTheme.typography.labelSmall,
                        color = VoltGold
                    )
                    Text(
                        text = "${right.baseStats.total}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = if (bstDiff < 0) EmeraldMatch else TextPrimary
                    )
                }
            }
        }

        // 4. Weaknesses Comparison
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = DexSurface,
            border = BorderStroke(1.dp, DexOutline)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "WEAKNESSES",
                    style = MaterialTheme.typography.labelSmall,
                    color = ScannerRed
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WeaknessColumn(
                        pokemon = left,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(DexOutline.copy(alpha = 0.6f))
                    )
                    WeaknessColumn(
                        pokemon = right,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 5. Abilities & Natures Comparison
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = DexSurface,
            border = BorderStroke(1.dp, DexOutline)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "ABILITIES & TOP NATURES",
                    style = MaterialTheme.typography.labelSmall,
                    color = VoltGold
                )
                CompareAbilitiesSummary(pokemon = left, accent = CyberCyan)
                HorizontalDivider(color = DexOutline.copy(alpha = 0.5f))
                CompareAbilitiesSummary(pokemon = right, accent = VoltGold)
            }
        }

        OutlinedButton(
            onClick = onPickAnother,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("compare_pick_another_button")
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = CyberCyan,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Compare Another Pokémon",
                color = CyberCyan,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompareFighterColumn(
    pokemon: PokemonEntry,
    accent: Color,
    isBstWinner: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            pokemon.types.first().badgeColor.copy(alpha = 0.34f),
                            DexSurfaceElevated
                        )
                    )
                )
                .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = pokemon.officialArtworkUrl,
                contentDescription = "${pokemon.name} artwork",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(56.dp)
            )
        }
        Text(
            text = pokemon.formattedDexNumber,
            style = MaterialTheme.typography.labelSmall,
            color = accent
        )
        Text(
            text = pokemon.name,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            pokemon.types.forEach { TypeBadgeChip(type = it, compact = true) }
        }
        Text(
            text = "BST ${pokemon.baseStats.total}",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isBstWinner) EmeraldMatch else TextSecondary
        )
    }
}

@Composable
private fun OffenseEdgeBox(
    attackerName: String,
    bestType: PokemonType,
    multiplier: Float,
    modifier: Modifier = Modifier
) {
    val multText = formatMultiplier(multiplier)
    val accent = when {
        multiplier >= 3.9f -> EmeraldMatch
        multiplier >= 1.9f -> EmeraldMatch
        multiplier == 0f -> ScannerRed
        multiplier <= 0.51f -> VoltGold
        else -> CyberCyan
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DexSurfaceElevated)
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = "$attackerName Best STAB",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = multText,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = accent
            )
            Text(
                text = bestType.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DuelStatRow(
    label: String,
    leftValue: Int,
    rightValue: Int,
    maxValue: Int = 180
) {
    val leftFrac by animateFloatAsState(
        targetValue = (leftValue.toFloat() / maxValue.toFloat()).coerceIn(0.05f, 1f),
        animationSpec = tween(360),
        label = "cmp_left_$label"
    )
    val rightFrac by animateFloatAsState(
        targetValue = (rightValue.toFloat() / maxValue.toFloat()).coerceIn(0.05f, 1f),
        animationSpec = tween(360),
        label = "cmp_right_$label"
    )
    val leftWins = leftValue > rightValue
    val rightWins = rightValue > leftValue
    val leftColor = if (leftWins) EmeraldMatch else if (leftValue == rightValue) CyberCyan else TextMuted.copy(alpha = 0.65f)
    val rightColor = if (rightWins) EmeraldMatch else if (leftValue == rightValue) CyberCyan else TextMuted.copy(alpha = 0.65f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = leftValue.toString().padStart(3, ' '),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (leftWins) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (leftWins) EmeraldMatch else TextPrimary,
            modifier = Modifier.width(30.dp),
            textAlign = TextAlign.End
        )

        // Left bar growing from right (center) to left
        Box(
            modifier = Modifier
                .weight(1f)
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(DexOutline.copy(alpha = 0.35f)),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(leftFrac)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(leftColor)
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = TextSecondary,
            modifier = Modifier.width(30.dp),
            textAlign = TextAlign.Center
        )

        // Right bar growing from left (center) to right
        Box(
            modifier = Modifier
                .weight(1f)
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(DexOutline.copy(alpha = 0.35f)),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(rightFrac)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(rightColor)
            )
        }

        Text(
            text = rightValue.toString().padEnd(3, ' '),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (rightWins) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (rightWins) EmeraldMatch else TextPrimary,
            modifier = Modifier.width(30.dp),
            textAlign = TextAlign.Start
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeaknessColumn(
    pokemon: PokemonEntry,
    modifier: Modifier = Modifier
) {
    val weaknesses = pokemon.weaknessProfile.allWeaknesses
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = pokemon.name,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (weaknesses.isEmpty()) {
            Text(
                text = "No weaknesses",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                weaknesses.forEach { WeaknessMultiplierChip(item = it) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompareAbilitiesSummary(
    pokemon: PokemonEntry,
    accent: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            text = pokemon.name,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = accent
        )
        if (pokemon.abilityDetails.isEmpty()) {
            Text(
                text = "No ability metadata synced",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                pokemon.abilityDetails.forEach { ability ->
                    val topNature = NatureAdvisor.bestNaturesForAbility(pokemon, ability).firstOrNull()
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (ability.isHidden) VoltGold.copy(alpha = 0.14f) else DexSurfaceElevated,
                        border = BorderStroke(
                            1.dp,
                            if (ability.isHidden) VoltGold.copy(alpha = 0.5f) else DexOutline
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = ability.name,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (ability.isHidden) VoltGold else TextPrimary
                            )
                            if (topNature != null) {
                                Text(
                                    text = "• ${topNature.name}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldMatch
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun bestOffensiveTypeAgainst(
    attackerTypes: List<PokemonType>,
    defenderTypes: List<PokemonType>
): Pair<PokemonType, Float> {
    var bestType = attackerTypes.firstOrNull() ?: PokemonType.NORMAL
    var bestMult = -1f
    for (atk in attackerTypes) {
        val m = TypeEffectivenessChart.multiplierAgainst(atk, defenderTypes)
        if (m > bestMult) {
            bestMult = m
            bestType = atk
        }
    }
    return bestType to bestMult.coerceAtLeast(1f)
}

private fun formatMultiplier(mult: Float): String = when {
    mult >= 3.9f -> "4×"
    mult >= 1.9f -> "2×"
    mult == 0f -> "0×"
    mult <= 0.26f -> "¼×"
    mult <= 0.51f -> "½×"
    else -> "1×"
}
