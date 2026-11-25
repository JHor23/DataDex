package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.R
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DexOutline
import com.example.ui.theme.DexSurface
import com.example.ui.theme.DexVoid
import com.example.ui.theme.EmeraldMatch
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoltGold

private const val KOFI_URL = "https://ko-fi.com/jhor23"
private const val SOURCE_URL = "https://github.com/JHor23/DataDex"
private const val PROJECTS_URL = "https://github.com/JHor23/"

@Composable
fun AboutMeScreen(
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DexVoid)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("about_me_screen"),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        // Hero Profile Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .border(2.dp, CyberCyan.copy(alpha = 0.8f), CircleShape)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(CyberCyan.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_creator_avatar),
                        contentDescription = stringResource(R.string.about_creator_avatar_desc),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "JHor",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Text(
                        text = "Pokémon Trainer & DataDex Builder",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CyberCyan
                    )
                }
            }
        }

        // Bio Section
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = DexSurface,
            border = BorderStroke(1.dp, DexOutline)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Bio",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                }
                Text(
                    text = "Lifelong Pokémon fan building fast, practical tools for trainers and collectors. DataDex was crafted to make type matchups, stats, and real-time screen scanning effortless during battles and card hunting.",
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 17.sp),
                    color = TextSecondary
                )
            }
        }

        // Quick Highlights Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AboutHighlightCard(
                icon = Icons.Default.CatchingPokemon,
                tint = EmeraldMatch,
                label = "Favorite Partner",
                value = "Shiny Charizard / Sharpedo X",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            AboutHighlightCard(
                icon = Icons.Default.Bolt,
                tint = VoltGold,
                label = "Go-To Type",
                value = "Fire, Ghost, Psychic",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }

        // Links & Support Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = DexSurface,
            border = BorderStroke(1.dp, DexOutline)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.about_links_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                AboutLinkButton(
                    label = stringResource(R.string.about_link_kofi),
                    icon = Icons.Default.LocalCafe,
                    accentColor = EmeraldMatch,
                    testTag = "about_kofi_button",
                    onClick = { runCatching { uriHandler.openUri(KOFI_URL) } }
                )
                AboutLinkButton(
                    label = stringResource(R.string.about_link_source),
                    icon = Icons.Default.Code,
                    accentColor = CyberCyan,
                    testTag = "about_source_button",
                    onClick = { runCatching { uriHandler.openUri(SOURCE_URL) } }
                )
                AboutLinkButton(
                    label = stringResource(R.string.about_link_projects),
                    icon = Icons.Default.Folder,
                    accentColor = Color(0xFFA78BFA),
                    testTag = "about_projects_button",
                    onClick = { runCatching { uriHandler.openUri(PROJECTS_URL) } }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Version Footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp)
                .testTag("about_version_footer"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = DexOutline.copy(alpha = 0.7f)
            )
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = DexSurface,
                border = BorderStroke(1.dp, DexOutline)
            ) {
                Text(
                    text = stringResource(
                        R.string.about_version_format,
                        BuildConfig.VERSION_NAME,
                        BuildConfig.VERSION_CODE
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = DexOutline.copy(alpha = 0.7f)
            )
        }
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
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(tint.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun AboutLinkButton(
    label: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    FilledTonalButton(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = accentColor.copy(alpha = 0.15f),
            contentColor = TextPrimary
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )
    }
}
