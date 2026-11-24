package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.DataDexPreferences
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
import com.example.voice.VoiceOption

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    preferences: DataDexPreferences,
    availableVoices: List<VoiceOption>,
    isVoiceReady: Boolean,
    onToggleVoiceOutput: (Boolean) -> Unit,
    onToggleVoiceDetails: (Boolean) -> Unit,
    onToggleVoiceAbilities: (Boolean) -> Unit,
    onUpdateVoiceRate: (Float) -> Unit,
    onUpdateVoicePitch: (Float) -> Unit,
    onUpdateVoiceCooldown: (Int) -> Unit,
    onSelectVoiceName: (String) -> Unit,
    onToggleVoiceClassic90s: (Boolean) -> Unit,
    onTestVoice: () -> Unit,
    onToggleScouterSfx: (Boolean) -> Unit,
    onUpdateScouterSfxVolume: (Float) -> Unit,
    onToggleScouterSfxOnDetect: (Boolean) -> Unit,
    onToggleScouterSfxOnOverlayUpdate: (Boolean) -> Unit,
    onTestScouterSfx: () -> Unit,
    onUpdateScanInterval: (Int) -> Unit,
    onUpdateOverlayOpacity: (Float) -> Unit,
    onToggleVisionFallback: (Boolean) -> Unit,
    onTogglePokeApiLive: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var scouterExpanded by rememberSaveable { mutableStateOf(false) }
    var voiceExpanded by rememberSaveable { mutableStateOf(false) }
    var scannerExpanded by rememberSaveable { mutableStateOf(false) }

    val scouterChevronRotation by animateFloatAsState(
        targetValue = if (scouterExpanded) 180f else 0f,
        label = "scouter_chevron"
    )
    val voiceChevronRotation by animateFloatAsState(
        targetValue = if (voiceExpanded) 180f else 0f,
        label = "voice_chevron"
    )
    val scannerChevronRotation by animateFloatAsState(
        targetValue = if (scannerExpanded) 180f else 0f,
        label = "scanner_chevron"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DexVoid),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Screen Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
                Text(
                    text = "Scanner audio, voice readout, scan speed, and overlay preferences",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }

        // 0. Scouter Sound Effects Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = DexSurface,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (preferences.scouterSfxEnabled) CyberCyan.copy(alpha = 0.65f) else DexOutline
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { scouterExpanded = !scouterExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CyberCyan.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Radar,
                                    contentDescription = null,
                                    tint = CyberCyan
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Scanner Sound Effects",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Subtle audio cues on Pokémon lock-on & overlay updates",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Switch(
                                checked = preferences.scouterSfxEnabled,
                                onCheckedChange = onToggleScouterSfx,
                                colors = SwitchDefaults.colors(checkedTrackColor = CyberCyan),
                                modifier = Modifier.testTag("scouter_sfx_switch")
                            )
                            IconButton(
                                onClick = { scouterExpanded = !scouterExpanded },
                                modifier = Modifier.testTag("scouter_sfx_expand_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (scouterExpanded) "Collapse Scanner Sound Effects" else "Expand Scanner Sound Effects",
                                    tint = TextSecondary,
                                    modifier = Modifier.rotate(scouterChevronRotation)
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = scouterExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            HorizontalDivider(color = DexOutline.copy(alpha = 0.7f))

                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Scanner Audio Level",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "${(preferences.scouterSfxVolume * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CyberCyan
                                    )
                                }
                                Slider(
                                    value = preferences.scouterSfxVolume,
                                    onValueChange = onUpdateScouterSfxVolume,
                                    valueRange = 0.1f..1.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = CyberCyan,
                                        activeTrackColor = CyberCyan
                                    ),
                                    modifier = Modifier.testTag("scouter_sfx_volume_slider")
                                )
                            }

                            SettingsToggleRow(
                                title = "Detection Lock-On Tone",
                                subtitle = "Ascending scanner chirp when a Pokémon is identified",
                                checked = preferences.scouterSfxOnDetect,
                                onCheckedChange = onToggleScouterSfxOnDetect,
                                checkedTrackColor = CyberCyan,
                                testTag = "scouter_sfx_detect_switch"
                            )

                            SettingsToggleRow(
                                title = "Overlay Update Cue",
                                subtitle = "Soft digital tick when the overlay opens, toggles, or refreshes",
                                checked = preferences.scouterSfxOnOverlayUpdate,
                                onCheckedChange = onToggleScouterSfxOnOverlayUpdate,
                                checkedTrackColor = EmeraldMatch,
                                testTag = "scouter_sfx_overlay_switch"
                            )

                            FilledTonalButton(
                                onClick = onTestScouterSfx,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("test_scouter_sfx_button"),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = CyberCyan.copy(alpha = 0.18f),
                                    contentColor = CyberCyan
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Test Scanner Sound",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1. Voice Output & Voice Settings Panel
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = DexSurface,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (preferences.voiceOutputEnabled) CyberCyan.copy(alpha = 0.65f) else DexOutline
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { voiceExpanded = !voiceExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CyberCyan.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = CyberCyan
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Voice Output",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Speak detected Pokémon names & weaknesses aloud",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Switch(
                                checked = preferences.voiceOutputEnabled,
                                onCheckedChange = onToggleVoiceOutput,
                                colors = SwitchDefaults.colors(checkedTrackColor = CyberCyan),
                                modifier = Modifier.testTag("voice_output_switch")
                            )
                            IconButton(
                                onClick = { voiceExpanded = !voiceExpanded },
                                modifier = Modifier.testTag("voice_output_expand_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (voiceExpanded) "Collapse Voice Output" else "Expand Voice Output",
                                    tint = TextSecondary,
                                    modifier = Modifier.rotate(voiceChevronRotation)
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = voiceExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            HorizontalDivider(color = DexOutline.copy(alpha = 0.7f))

                            // Classic 90s Computer Voice Toggle
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = if (preferences.voiceClassic90sMode) {
                                    VoltGold.copy(alpha = 0.12f)
                                } else {
                                    DexSurfaceElevated.copy(alpha = 0.72f)
                                },
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (preferences.voiceClassic90sMode) {
                                        VoltGold.copy(alpha = 0.65f)
                                    } else {
                                        DexOutline.copy(alpha = 0.7f)
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(
                                            text = "Classic Computer Voice",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (preferences.voiceClassic90sMode) VoltGold else TextPrimary
                                        )
                                        Text(
                                            text = "Synthesized classic Pokédex computer tone & readout style",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextSecondary
                                        )
                                    }
                                    Switch(
                                        checked = preferences.voiceClassic90sMode,
                                        onCheckedChange = onToggleVoiceClassic90s,
                                        colors = SwitchDefaults.colors(checkedTrackColor = VoltGold),
                                        modifier = Modifier.testTag("voice_classic_90s_switch")
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = VoltGold,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Voice Settings",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = VoltGold
                                )
                            }

                            // Speak Typing & Weaknesses Toggle
                            SettingsToggleRow(
                                title = "Speak Typing & Weaknesses",
                                subtitle = "Include type matchups (4× & 2×) after the name",
                                checked = preferences.voiceSpeakDetails,
                                onCheckedChange = onToggleVoiceDetails,
                                checkedTrackColor = EmeraldMatch,
                                testTag = "voice_speak_details_switch"
                            )

                            // Speak Abilities Toggle
                            SettingsToggleRow(
                                title = "Speak Abilities",
                                subtitle = "Announce regular and hidden abilities",
                                checked = preferences.voiceSpeakAbilities,
                                onCheckedChange = onToggleVoiceAbilities,
                                checkedTrackColor = VoltGold,
                                testTag = "voice_speak_abilities_switch"
                            )

                            // Speech Rate Slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Voice Speed",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = String.format("%.2f×", preferences.voiceSpeechRate),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CyberCyan
                                    )
                                }
                                Slider(
                                    value = preferences.voiceSpeechRate,
                                    onValueChange = onUpdateVoiceRate,
                                    valueRange = 0.65f..1.6f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = CyberCyan,
                                        activeTrackColor = CyberCyan
                                    ),
                                    modifier = Modifier.testTag("voice_speed_slider")
                                )
                            }

                            // Voice Pitch Slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Voice Pitch",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = String.format("%.2f×", preferences.voicePitch),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CyberCyan
                                    )
                                }
                                Slider(
                                    value = preferences.voicePitch,
                                    onValueChange = onUpdateVoicePitch,
                                    valueRange = 0.7f..1.4f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = VoltGold,
                                        activeTrackColor = VoltGold
                                    ),
                                    modifier = Modifier.testTag("voice_pitch_slider")
                                )
                            }

                            // Repeat Cooldown Slider
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Repeat Cooldown",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                    Text(
                                        text = "${preferences.voiceCooldownSeconds}s",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CyberCyan
                                    )
                                }
                                Slider(
                                    value = preferences.voiceCooldownSeconds.toFloat(),
                                    onValueChange = { onUpdateVoiceCooldown(it.toInt()) },
                                    valueRange = 2f..15f,
                                    steps = 12,
                                    colors = SliderDefaults.colors(
                                        thumbColor = EmeraldMatch,
                                        activeTrackColor = EmeraldMatch
                                    ),
                                    modifier = Modifier.testTag("voice_cooldown_slider")
                                )
                            }

                            // Voice Selector Chips
                            if (availableVoices.size > 1) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Voice Style",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy((-4).dp)
                                    ) {
                                        availableVoices.forEach { option ->
                                            val selected = preferences.voiceName == option.id
                                            FilterChip(
                                                selected = selected,
                                                onClick = { onSelectVoiceName(option.id) },
                                                label = {
                                                    Text(
                                                        text = option.label,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        maxLines = 1
                                                    )
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = CyberCyan.copy(alpha = 0.22f),
                                                    selectedLabelColor = CyberCyan,
                                                    containerColor = DexSurfaceElevated,
                                                    labelColor = TextSecondary
                                                )
                                            )
                                        }
                                    }
                                }
                            }

                            FilledTonalButton(
                                onClick = onTestVoice,
                                enabled = isVoiceReady,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("test_voice_button"),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = CyberCyan.copy(alpha = 0.18f),
                                    contentColor = CyberCyan
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = when {
                                        !isVoiceReady -> "Preparing Device Voice..."
                                        preferences.voiceClassic90sMode -> "Test Classic Voice"
                                        else -> "Test Voice Output"
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (!isVoiceReady) {
                                Text(
                                    text = "Device speech engine is initializing.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Scanner & Overlay Settings Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = DexSurface,
                border = BorderStroke(1.dp, DexOutline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { scannerExpanded = !scannerExpanded },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CyberCyan.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = CyberCyan
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "Scanner & Overlay Settings",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Scan interval, overlay opacity & smart recognition",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }

                        IconButton(
                            onClick = { scannerExpanded = !scannerExpanded },
                            modifier = Modifier.testTag("scanner_settings_expand_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = if (scannerExpanded) "Collapse Scanner & Overlay Settings" else "Expand Scanner & Overlay Settings",
                                tint = TextSecondary,
                                modifier = Modifier.rotate(scannerChevronRotation)
                            )
                        }
                    }

                    AnimatedVisibility(visible = scannerExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            HorizontalDivider(color = DexOutline.copy(alpha = 0.7f))

                            // Scan interval slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Screen Scan Interval",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Text(
                                text = "${preferences.scanIntervalMs} ms",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberCyan
                            )
                        }
                        Slider(
                            value = preferences.scanIntervalMs.toFloat(),
                            onValueChange = { onUpdateScanInterval(it.toInt()) },
                            valueRange = 800f..3500f,
                            steps = 9,
                            colors = SliderDefaults.colors(
                                thumbColor = CyberCyan,
                                activeTrackColor = CyberCyan
                            ),
                            modifier = Modifier.testTag("scan_interval_slider")
                        )
                    }

                    // Overlay opacity slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Overlay Opacity",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                            Text(
                                text = "${(preferences.overlayOpacity * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberCyan
                            )
                        }
                        Slider(
                            value = preferences.overlayOpacity,
                            onValueChange = onUpdateOverlayOpacity,
                            valueRange = 0.65f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = ScannerRed,
                                activeTrackColor = ScannerRed
                            ),
                            modifier = Modifier.testTag("overlay_opacity_slider")
                        )
                    }

                    SettingsToggleRow(
                        title = "Visual Creature Recognition",
                        subtitle = "Identify creatures visually when no text nameplate is visible",
                        checked = preferences.enableVisionFallback,
                        onCheckedChange = onToggleVisionFallback,
                        checkedTrackColor = CyberCyan,
                        testTag = "vision_fallback_switch"
                    )

                    SettingsToggleRow(
                        title = "Live PokéAPI Enrichment",
                        subtitle = "Fetch full stats for all 1,025 Pokémon on demand",
                        checked = preferences.enablePokeApiLiveFetch,
                        onCheckedChange = onTogglePokeApiLive,
                        checkedTrackColor = EmeraldMatch,
                        testTag = "pokeapi_live_switch"
                    )
                        }
                    }
                }
            }
        }

        // 3. Recognition Accuracy Disclaimer
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = DexSurfaceElevated.copy(alpha = 0.72f),
                border = BorderStroke(1.dp, DexOutline)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Recognition Disclaimer",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "Scan results may vary depending on screen font, lighting, camera focus, or artwork angle. Double-check typing and stats during key battles.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(14.dp)) }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    checkedTrackColor: androidx.compose.ui.graphics.Color,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = checkedTrackColor),
            modifier = Modifier.testTag(testTag)
        )
    }
}
