package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DexOutline
import com.example.ui.theme.DexSurface
import com.example.ui.theme.DexSurfaceElevated
import com.example.ui.theme.DexVoid
import com.example.ui.theme.EmeraldMatch
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoltGold

@Composable
fun AboutMeScreen(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DexVoid)
            .testTag("about_me_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "About Me",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
                Text(
                    text = "Creator profile & DataDex story",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        // Hero Profile Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = DexSurface,
                border = BorderStroke(
                    1.2.dp,
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1D4ED8).copy(alpha = 0.8f),
                            CyberCyan.copy(alpha = 0.7f),
                            DexOutline
                        )
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF0A1933),
                                    DexSurface
                                )
                            )
                        )
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(CyberCyan.copy(alpha = 0.14f))
                            .border(2.dp, CyberCyan.copy(alpha = 0.75f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Creator avatar",
                            tint = CyberCyan,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "Creator Name",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Text(
                            text = "Pokémon Trainer & DataDex Builder",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CyberCyan
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DexSurfaceElevated,
                        border = BorderStroke(1.dp, DexOutline)
                    ) {
                        Text(
                            text = "SAMPLE PROFILE • READY FOR YOUR CUSTOM DETAILS",
                            style = MaterialTheme.typography.labelSmall,
                            color = VoltGold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // Bio Section
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = DexSurface,
                border = BorderStroke(1.dp, DexOutline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Bio",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "Lifelong Pokémon fan building fast, practical tools for trainers and collectors. DataDex was crafted to make type matchups, stats, and real-time screen scanning effortless during battles and card hunting.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        // Quick Highlights Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AboutHighlightCard(
                        icon = Icons.Default.CatchingPokemon,
                        tint = EmeraldMatch,
                        label = "Favorite Partner",
                        value = "Charizard",
                        modifier = Modifier.weight(1f)
                    )
                    AboutHighlightCard(
                        icon = Icons.Default.Bolt,
                        tint = VoltGold,
                        label = "Go-To Type",
                        value = "Electric / Dragon",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AboutHighlightCard(
                        icon = Icons.Default.Radar,
                        tint = CyberCyan,
                        label = "Project",
                        value = "DataDex Scanner",
                        modifier = Modifier.weight(1f)
                    )
                    AboutHighlightCard(
                        icon = Icons.Default.Code,
                        tint = Color(0xFF60A5FA),
                        label = "Focus",
                        value = "Android & Tools",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Contact / Links Placeholder Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = DexSurface,
                border = BorderStroke(1.dp, DexOutline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Links & Contact",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    AboutLinkRow(platform = "GitHub / Portfolio", handle = "Add your link")
                    AboutLinkRow(platform = "Social / Trainer ID", handle = "Add your handle")
                    AboutLinkRow(platform = "Email", handle = "Add your contact email")
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun AboutHighlightCard(
    icon: ImageVector,
    tint: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = DexSurface,
        border = BorderStroke(1.dp, DexOutline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun AboutLinkRow(
    platform: String,
    handle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DexSurfaceElevated)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = platform,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
        Text(
            text = handle,
            style = MaterialTheme.typography.bodyMedium,
            color = CyberCyan
        )
    }
}
