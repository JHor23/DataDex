package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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

private val NeonPurple = Color(0xFFA855F7)

private enum class BioTab {
    STORY,
    TRAITS
}

private data class AboutBioTrait(
    val title: String,
    val detail: String,
    val dotColor: Color
)

private val jhorBioTraits = listOf(
    AboutBioTrait(
        title = "Not Your Average Dev",
        detail = "Powered by caffeine, dad jokes, & a healthy dose of Deadpool-style sarcasm.",
        dotColor = CyberCyan
    ),
    AboutBioTrait(
        title = "Indie App Builder",
        detail = "Crafting lightweight, bloat-free Android, PC, & web based applications.",
        dotColor = NeonPurple
    ),
    AboutBioTrait(
        title = "For The People",
        detail = "Dedicated to open-source, free, & user-controlled software.",
        dotColor = CyberCyan
    ),
    AboutBioTrait(
        title = "Gaming Tinkerer",
        detail = "Modern titles & retro emulation across PC, handhelds, & mobile.",
        dotColor = NeonPurple
    ),
    AboutBioTrait(
        title = "Off the Clock",
        detail = "Dad, Husband, Eagle Scout, Gamer, & Hooper.",
        dotColor = CyberCyan
    )
)

private const val KOFI_URL = "https://ko-fi.com/jhor23"
private const val SOURCE_URL = "https://github.com/JHor23/DataDex"
private const val PROJECTS_URL = "https://github.com/JHor23/"

@OptIn(ExperimentalLayoutApi::class)
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

        // Bio Section (combines DataDex story + JHor dev traits inside the Bio card only)
        var selectedBioTab by remember { mutableStateOf(BioTab.STORY) }
        var selectedTraitIndex by remember { mutableStateOf(0) }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = DexSurface,
            border = BorderStroke(1.dp, DexOutline)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
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

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(DexVoid)
                            .border(1.dp, DexOutline, RoundedCornerShape(999.dp))
                            .padding(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        BioPillTab(
                            label = "Story",
                            selected = selectedBioTab == BioTab.STORY,
                            accentColor = CyberCyan,
                            testTag = "bio_tab_story",
                            onClick = { selectedBioTab = BioTab.STORY }
                        )
                        BioPillTab(
                            label = "5 Traits",
                            selected = selectedBioTab == BioTab.TRAITS,
                            accentColor = NeonPurple,
                            testTag = "bio_tab_traits",
                            onClick = { selectedBioTab = BioTab.TRAITS }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    CyberCyan,
                                    NeonPurple,
                                    DexOutline.copy(alpha = 0.25f)
                                )
                            )
                        )
                )

                AnimatedContent(
                    targetState = selectedBioTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "bio_tab_transition"
                ) { tab ->
                    when (tab) {
                        BioTab.STORY -> {
                            Text(
                                text = "Lifelong Pokémon fan building fast, practical tools for trainers, collectors, and everyday players. DataDex gives you instant access to type matchups, stats, abilities, and preferred natures. With built-in real-time screen scanning, it’s made to keep up during battles, card hunts, and gaming sessions.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.sp,
                                    lineHeight = 16.5.sp
                                ),
                                color = TextSecondary
                            )
                        }
                        BioTab.TRAITS -> {
                            val activeTrait = jhorBioTraits[selectedTraitIndex.coerceIn(jhorBioTraits.indices)]
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    jhorBioTraits.forEachIndexed { index, trait ->
                                        val isSelected = index == selectedTraitIndex
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(999.dp))
                                                .background(
                                                    if (isSelected) {
                                                        trait.dotColor.copy(alpha = 0.18f)
                                                    } else {
                                                        DexVoid.copy(alpha = 0.75f)
                                                    }
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) {
                                                        trait.dotColor.copy(alpha = 0.75f)
                                                    } else {
                                                        DexOutline
                                                    },
                                                    shape = RoundedCornerShape(999.dp)
                                                )
                                                .clickable { selectedTraitIndex = index }
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                                .testTag("bio_trait_chip_$index"),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(trait.dotColor)
                                            )
                                            Text(
                                                text = trait.title,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.5.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                ),
                                                color = if (isSelected) TextPrimary else TextSecondary
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(
                                            SpanStyle(
                                                color = activeTrait.dotColor,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        ) {
                                            append("${activeTrait.title}: ")
                                        }
                                        withStyle(SpanStyle(color = TextSecondary)) {
                                            append(activeTrait.detail)
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 12.sp,
                                        lineHeight = 16.sp
                                    ),
                                    modifier = Modifier.testTag("bio_trait_detail_text")
                                )
                            }
                        }
                    }
                }
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

@Composable
private fun BioPillTab(
    label: String,
    selected: Boolean,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) accentColor.copy(alpha = 0.2f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 3.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            ),
            color = if (selected) accentColor else TextMuted
        )
    }
}
