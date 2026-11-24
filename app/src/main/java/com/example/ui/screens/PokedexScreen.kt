package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.model.PokemonEntry
import com.example.model.PokemonType
import com.example.ui.components.TypeBadgeChip
import com.example.ui.components.TypeVisualizerModal
import com.example.ui.components.WeaknessMultiplierChip
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DexOutline
import com.example.ui.theme.DexSurface
import com.example.ui.theme.DexSurfaceElevated
import com.example.ui.theme.DexVoid
import com.example.ui.theme.ScannerRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoltGold
import com.example.viewmodel.PokedexUiState

@Composable
fun PokedexScreen(
    state: PokedexUiState,
    onQueryChange: (String) -> Unit,
    onToggleTypeFilter: (PokemonType) -> Unit,
    onSelectPokemon: (PokemonEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    var showTypeVisualizer by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DexVoid)
    ) {
        // Search Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 10.dp)
                ) {
                    Text(
                        text = "DataDex Encyclopedia",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = "Search all 1,025 Pokémon",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }

                Surface(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showTypeVisualizer = true }
                        .testTag("type_visualizer_button"),
                    shape = RoundedCornerShape(12.dp),
                    color = CyberCyan.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Open Type Visualizer",
                            tint = CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Type Visualizer",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = CyberCyan,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            OutlinedTextField(
                value = state.query,
                onValueChange = { onQueryChange(it.replace("\n", "")) },
                placeholder = { Text("Search name or Dex #\n(e.g. Garchomp, 25)") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = CyberCyan)
                },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextSecondary)
                        }
                    }
                },
                maxLines = 2,
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
                    .testTag("pokedex_search_input")
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(PokemonType.entries) { type ->
                    val selected = state.selectedType == type
                    FilterChip(
                        selected = selected,
                        onClick = { onToggleTypeFilter(type) },
                        label = {
                            Text(
                                text = type.displayName,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = type.badgeColor.copy(alpha = 0.28f),
                            selectedLabelColor = TextPrimary,
                            containerColor = DexSurface,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = DexOutline,
                            selectedBorderColor = type.badgeColor
                        ),
                        modifier = Modifier.testTag("type_filter_${type.name.lowercase()}")
                    )
                }
            }
        }

        // Pokédex Entry List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.entries, key = { it.id }) { entry ->
                PokedexEntryCard(
                    entry = entry,
                    onClick = { onSelectPokemon(entry) }
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showTypeVisualizer) {
        TypeVisualizerModal(
            onDismiss = { showTypeVisualizer = false }
        )
    }
}

@Composable
private fun PokedexEntryCard(
    entry: PokemonEntry,
    onClick: () -> Unit
) {
    val primaryTypeColor = entry.types.firstOrNull()?.badgeColor ?: CyberCyan
    val topWeaknesses = entry.weaknessProfile.allWeaknesses.take(3)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("pokedex_item_${entry.id}"),
        shape = RoundedCornerShape(18.dp),
        color = DexSurface,
        border = BorderStroke(1.dp, DexOutline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primaryTypeColor.copy(alpha = 0.30f),
                                DexSurfaceElevated
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = entry.officialArtworkUrl,
                    contentDescription = "${entry.name} artwork",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(54.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = entry.formattedDexNumber,
                            style = MaterialTheme.typography.labelMedium,
                            color = CyberCyan
                        )
                        Text(
                            text = entry.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "BST ${entry.baseStats.total}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = VoltGold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    entry.types.forEach { type ->
                        TypeBadgeChip(type = type, compact = true)
                    }
                }

                if (topWeaknesses.isNotEmpty() && !entry.category.startsWith("Tap to load")) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "WEAK:",
                            style = MaterialTheme.typography.labelSmall,
                            color = ScannerRed
                        )
                        topWeaknesses.forEach { w ->
                            WeaknessMultiplierChip(item = w)
                        }
                    }
                } else {
                    Text(
                        text = entry.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}
