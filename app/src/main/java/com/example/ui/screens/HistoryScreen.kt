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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.PokemonType
import com.example.model.ScanHistoryItem
import com.example.ui.components.TypeBadgeChip
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DexOutline
import com.example.ui.theme.DexSurface
import com.example.ui.theme.DexSurfaceElevated
import com.example.ui.theme.DexVoid
import com.example.ui.theme.ScannerRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    history: List<ScanHistoryItem>,
    onSelectHistoryPokemon: (Int) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DexVoid),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Scan History Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null, tint = ScannerRed)
                    Column {
                        Text(
                            text = "Recent Scans",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "Tap any Pokémon to inspect full stats & abilities",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }

                if (history.isNotEmpty()) {
                    TextButton(
                        onClick = onClearHistory,
                        modifier = Modifier.testTag("clear_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = ScannerRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Clear",
                            color = ScannerRed,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        if (history.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = DexSurface,
                    border = BorderStroke(1.dp, DexOutline)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(38.dp)
                        )
                        Text(
                            text = stringResource(R.string.empty_history_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = stringResource(R.string.empty_history_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(history, key = { "${it.pokemonId}_${it.timestampMillis}" }) { item ->
                HistoryScanCard(
                    item = item,
                    onClick = { onSelectHistoryPokemon(item.pokemonId) }
                )
            }
        }

        item { Spacer(Modifier.height(14.dp)) }
    }
}

@Composable
private fun HistoryScanCard(
    item: ScanHistoryItem,
    onClick: () -> Unit
) {
    val locale = Locale.getDefault()
    val formatter = androidx.compose.runtime.remember(locale) {
        SimpleDateFormat("MMM d • HH:mm", locale)
    }
    val timeText = formatter.format(Date(item.timestampMillis))
    val artworkUrl = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/${item.pokemonId}.png"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("history_item_${item.pokemonId}"),
        shape = RoundedCornerShape(16.dp),
        color = DexSurface,
        border = BorderStroke(1.dp, DexOutline)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DexSurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = "${item.pokemonName} artwork",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(44.dp)
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
                    Text(
                        text = item.pokemonName,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item.types.mapNotNull { PokemonType.fromApiName(it) }.forEach { type ->
                        TypeBadgeChip(type = type, compact = true)
                    }
                    val cleanMethodLabel = when (item.methodLabel) {
                        "ML OCR" -> "Text Scan"
                        "ML Vision" -> "Visual Scan"
                        "OCR + Vision" -> "Smart Scan"
                        else -> item.methodLabel
                    }
                    Text(
                        text = cleanMethodLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = CyberCyan
                    )
                }
            }
        }
    }
}
