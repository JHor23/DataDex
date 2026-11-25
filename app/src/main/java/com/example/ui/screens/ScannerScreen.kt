package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.OverlayHudState
import com.example.model.PokemonEntry
import com.example.ui.components.ScanMatchSummaryBanner
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DexOutline
import com.example.ui.theme.DexSurface
import com.example.ui.theme.DexSurfaceElevated
import com.example.ui.theme.DexVoid
import com.example.ui.theme.EmeraldMatch
import com.example.ui.theme.PlusJakartaFontFamily
import com.example.ui.theme.ScannerRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoltGold
import com.example.viewmodel.ScannerUiState

private data class SampleBattleCard(
    val name: String,
    val level: Int,
    val hpText: String,
    val subtitle: String
)

private val sampleCards = listOf(
    SampleBattleCard("Charizard", 78, "HP 248/248", "4× Rock Threat"),
    SampleBattleCard("Garchomp", 50, "HP 183/183", "4× Ice Threat"),
    SampleBattleCard("Flutter Mane", 50, "HP 131/131", "Paradox Special"),
    SampleBattleCard("Tinkaton", 62, "HP 194/194", "9 Resistances"),
    SampleBattleCard("Pikachu", 42, "HP 98/98", "1× Ground Weak")
)

private enum class ScanInputMode(
    val title: String,
    val subtitle: String,
    val actionLabel: String
) {
    SCREEN_OVERLAY(
        title = "Floating Screen Overlay",
        subtitle = "Reads Pokémon live over any app",
        actionLabel = "Launch Screen Overlay"
    ),
    LIVE_CAMERA(
        title = "Live Camera",
        subtitle = "Point camera at cards or screens",
        actionLabel = "Open Live Camera"
    ),
    SCREENSHOT(
        title = "Scan Screenshot",
        subtitle = "Pick a card photo or screenshot",
        actionLabel = "Choose Screenshot"
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScannerScreen(
    scannerUi: ScannerUiState,
    overlayState: OverlayHudState,
    hasOverlayPermission: Boolean,
    onRequestOverlayPermission: () -> Unit,
    onStartScreenOverlay: () -> Unit,
    onStopScreenOverlay: () -> Unit,
    onToggleOverlayPause: () -> Unit,
    onToggleOverlayAbilities: () -> Unit,
    onScanScreenNow: () -> Unit,
    onClearOverlayMatch: () -> Unit,
    onLaunchCameraScan: () -> Unit,
    onLaunchPhotoPicker: () -> Unit,
    onTestSampleCard: (name: String, level: Int, hpText: String) -> Unit,
    onComparePokemon: (PokemonEntry) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(overlayState.isOverlayRunning) {
        while (overlayState.isOverlayRunning) {
            nowMillis = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000L)
        }
    }

    val isActivelyScanning = (overlayState.isOverlayRunning && !overlayState.isScanningPaused) ||
        scannerUi.isAnalyzingImage

    var pulseScale = 1f
    var pulseAlpha = 1f
    var radarWave = 0f
    var scanSweep = 0f

    if (isActivelyScanning) {
        val pulseTransition = rememberInfiniteTransition(label = "scanner_live_pulse")
        pulseScale = pulseTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(750),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scanner_dot_scale"
        ).value
        pulseAlpha = pulseTransition.animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(750),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scanner_dot_alpha"
        ).value
        radarWave = pulseTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "scanner_radar_wave"
        ).value
        scanSweep = pulseTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "scanner_beam_sweep"
        ).value
    }

    val scanAgeText = if (overlayState.lastScanTimestampMillis > 0L) {
        val secs = ((nowMillis - overlayState.lastScanTimestampMillis).coerceAtLeast(0L) / 1000L)
        if (secs <= 1L) "just now" else "${secs}s ago"
    } else {
        "waiting..."
    }
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DexVoid),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. DBZ Scouter-Inspired Black & Blue Hero Banner
        item {
            val scouterBlue = Color(0xFF38BDF8)
            val scouterCobalt = Color(0xFF1D4ED8)
            val scouterObsidian = Color(0xFF040812)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .border(
                        width = 1.25.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                scouterCobalt.copy(alpha = 0.7f),
                                scouterBlue.copy(alpha = 0.85f),
                                Color(0xFF0E2954)
                            )
                        ),
                        shape = RoundedCornerShape(22.dp)
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_scouter_hero),
                    contentDescription = "DataDex blue scanner visor banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    scouterObsidian.copy(alpha = 0.88f),
                                    scouterObsidian.copy(alpha = 0.45f),
                                    Color(0xFF071B3D).copy(alpha = 0.12f)
                                )
                            )
                        )
                )
                // Visual Scan Pulse Overlay (Active only during scanning, never when idle)
                val pulseTint = when {
                    overlayState.isOverlayRunning && !overlayState.isScanningPaused -> EmeraldMatch
                    overlayState.isOverlayRunning && overlayState.isScanningPaused -> VoltGold
                    scannerUi.isAnalyzingImage -> CyberCyan
                    else -> scouterBlue
                }
                if (isActivelyScanning) {
                    val waveStroke = remember { Stroke(width = 2.2f) }
                    val coreStroke = remember { Stroke(width = 1.5f) }
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("scanner_visual_pulse")
                    ) {
                        val reticleCenter = Offset(size.width * 0.78f, size.height * 0.48f)
                        val maxRadius = size.minDimension * 0.56f

                        for (step in 0..1) {
                            val wave = if (step == 0) radarWave else (radarWave + 0.5f) % 1f
                            val radius = maxRadius * (0.16f + wave * 0.84f)
                            val waveAlpha = (1f - wave) * 0.55f
                            drawCircle(
                                color = pulseTint.copy(alpha = waveAlpha),
                                radius = radius,
                                center = reticleCenter,
                                style = waveStroke
                            )
                        }

                        drawCircle(
                            color = pulseTint.copy(alpha = 0.35f),
                            radius = maxRadius * 0.16f,
                            center = reticleCenter,
                            style = coreStroke
                        )

                        val beamX = size.width * scanSweep
                        val beamWidth = size.width * 0.16f
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    pulseTint.copy(alpha = 0.14f),
                                    Color.Transparent
                                ),
                                startX = beamX - beamWidth,
                                endX = beamX + beamWidth
                            ),
                            topLeft = Offset(0f, 0f),
                            size = size
                        )
                        drawLine(
                            color = pulseTint.copy(alpha = 0.32f),
                            start = Offset(beamX, 0f),
                            end = Offset(beamX, size.height),
                            strokeWidth = 1.5f
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = scouterObsidian.copy(alpha = 0.82f),
                            border = BorderStroke(
                                1.dp,
                                if (overlayState.isOverlayRunning) EmeraldMatch.copy(alpha = 0.7f) else scouterBlue.copy(alpha = 0.6f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier.size(14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isActivelyScanning) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            drawCircle(
                                                color = pulseTint.copy(alpha = (1f - radarWave) * 0.65f),
                                                radius = (size.minDimension * 0.5f) * (0.45f + radarWave * 0.55f),
                                                style = Stroke(width = 1.5f)
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(pulseTint)
                                    )
                                }
                                Text(
                                    text = when {
                                        overlayState.isOverlayRunning && overlayState.isScanningPaused -> "OVERLAY PAUSED"
                                        overlayState.isOverlayRunning -> "OVERLAY ACTIVE"
                                        scannerUi.isAnalyzingImage -> "SCANNING IMAGE"
                                        else -> "SCANNER READY"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = pulseTint
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontFamily = PlusJakartaFontFamily,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 31.sp,
                                letterSpacing = (-0.5).sp
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = "Pokédex Scanner & Overlay",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFBFD7FF)
                        )
                    }
                }
            }
        }

        // 2. Unified Scan Mode Control Deck (Overlay / Live Camera / Screenshot)
        item {
            var selectedMode by remember { mutableStateOf(ScanInputMode.SCREEN_OVERLAY) }
            var modeMenuExpanded by remember { mutableStateOf(false) }

            val scouterBlue = Color(0xFF38BDF8)
            val scouterCobalt = Color(0xFF2563EB)

            val modeIcon = when (selectedMode) {
                ScanInputMode.SCREEN_OVERLAY -> Icons.Default.Layers
                ScanInputMode.LIVE_CAMERA -> Icons.Default.CameraAlt
                ScanInputMode.SCREENSHOT -> Icons.Default.PhotoLibrary
            }
            val modeTint = when (selectedMode) {
                ScanInputMode.SCREEN_OVERLAY -> scouterBlue
                ScanInputMode.LIVE_CAMERA -> CyberCyan
                ScanInputMode.SCREENSHOT -> Color(0xFF60A5FA)
            }

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
                    if (overlayState.isOverlayRunning) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(EmeraldMatch.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = null,
                                        tint = EmeraldMatch
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Floating Screen Overlay",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "Frames scanned: ${overlayState.framesProcessed}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Overlay active",
                                tint = EmeraldMatch,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Live Heartbeat & Screen Stream Panel
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = DexSurfaceElevated,
                            border = BorderStroke(
                                1.dp,
                                if (overlayState.isScanningPaused) VoltGold.copy(alpha = 0.5f) else CyberCyan.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(9.dp)
                                                .scale(if (!overlayState.isScanningPaused) pulseScale else 1f)
                                                .alpha(if (!overlayState.isScanningPaused) pulseAlpha else 1f)
                                                .clip(CircleShape)
                                                .background(if (overlayState.isScanningPaused) VoltGold else EmeraldMatch)
                                        )
                                        Text(
                                            text = if (overlayState.isScanningPaused) {
                                                "SCANNER PAUSED"
                                            } else {
                                                "READING SCREEN • SCAN #${overlayState.framesProcessed}"
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (overlayState.isScanningPaused) VoltGold else EmeraldMatch
                                        )
                                    }
                                    Text(
                                        text = scanAgeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }

                                if (!overlayState.isScanningPaused) {
                                    val barColor = if (overlayState.isActivelyReading) EmeraldMatch else CyberCyan
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(DexSurface)
                                    ) {
                                        val pulseWidth = size.width * 0.28f
                                        val centerX = (size.width + pulseWidth) * scanSweep - pulseWidth * 0.5f
                                        drawRect(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    barColor,
                                                    Color.Transparent
                                                ),
                                                startX = centerX - pulseWidth * 0.5f,
                                                endX = centerX + pulseWidth * 0.5f
                                            ),
                                            topLeft = Offset(0f, 0f),
                                            size = size
                                        )
                                    }
                                }

                                Text(
                                    text = overlayState.statusMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextPrimary
                                )

                                Text(
                                    text = if (overlayState.lastScannedTextPreview.isNotBlank()) {
                                        "Reading screen: \"${overlayState.lastScannedTextPreview}\""
                                    } else {
                                        "Reading screen: Watching for Pokémon names..."
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CyberCyan,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onScanScreenNow,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("scan_now_button"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.6f))
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = CyberCyan, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Scan",
                                    color = CyberCyan,
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            FilledTonalButton(
                                onClick = onToggleOverlayPause,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("toggle_overlay_pause_button"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = if (overlayState.isScanningPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (overlayState.isScanningPaused) "Resume" else "Pause",
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Button(
                                onClick = onStopScreenOverlay,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("stop_overlay_button"),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ScannerRed)
                            ) {
                                Icon(Icons.Default.StopCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Stop",
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        // Sleek Inline Expandable Scan Mode Selector
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF091122))
                                .border(
                                    width = 1.dp,
                                    color = if (modeMenuExpanded) modeTint.copy(alpha = 0.7f) else DexOutline,
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { modeMenuExpanded = !modeMenuExpanded }
                                    .padding(horizontal = 12.dp, vertical = 11.dp)
                                    .testTag("scan_mode_dropdown"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(modeTint.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = modeIcon,
                                        contentDescription = selectedMode.title,
                                        tint = modeTint
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "SCAN MODE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = modeTint
                                    )
                                    Text(
                                        text = selectedMode.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = if (selectedMode == ScanInputMode.SCREEN_OVERLAY && !hasOverlayPermission) {
                                            "Needs Display Over Other Apps permission"
                                        } else {
                                            selectedMode.subtitle
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = DexSurfaceElevated,
                                    border = BorderStroke(1.dp, DexOutline)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Choose scan mode",
                                        tint = modeTint,
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .size(22.dp)
                                            .rotate(if (modeMenuExpanded) 180f else 0f)
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = modeMenuExpanded,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    HorizontalDivider(
                                        color = DexOutline.copy(alpha = 0.65f),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                    ScanInputMode.entries.forEach { mode ->
                                        val isSelected = mode == selectedMode
                                        val itemIcon = when (mode) {
                                            ScanInputMode.SCREEN_OVERLAY -> Icons.Default.Layers
                                            ScanInputMode.LIVE_CAMERA -> Icons.Default.CameraAlt
                                            ScanInputMode.SCREENSHOT -> Icons.Default.PhotoLibrary
                                        }
                                        val itemTint = when (mode) {
                                            ScanInputMode.SCREEN_OVERLAY -> scouterBlue
                                            ScanInputMode.LIVE_CAMERA -> CyberCyan
                                            ScanInputMode.SCREENSHOT -> Color(0xFF60A5FA)
                                        }

                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable {
                                                    selectedMode = mode
                                                    modeMenuExpanded = false
                                                }
                                                .testTag("scan_mode_option_${mode.name.lowercase()}"),
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) Color(0xFF112344) else Color(0xFF0D172B),
                                            border = BorderStroke(
                                                width = 1.dp,
                                                color = if (isSelected) itemTint.copy(alpha = 0.7f) else DexOutline.copy(alpha = 0.5f)
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 9.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(RoundedCornerShape(9.dp))
                                                        .background(itemTint.copy(alpha = 0.16f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = itemIcon,
                                                        contentDescription = null,
                                                        tint = itemTint,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = mode.title,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        color = TextPrimary
                                                    )
                                                    Text(
                                                        text = mode.subtitle,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = TextSecondary
                                                    )
                                                }
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = itemTint,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Single Primary Action Button for the selected mode
                        if (selectedMode == ScanInputMode.SCREEN_OVERLAY && !hasOverlayPermission) {
                            OutlinedButton(
                                onClick = onRequestOverlayPermission,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("grant_overlay_permission_button"),
                                border = BorderStroke(1.dp, scouterBlue.copy(alpha = 0.75f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = scouterBlue),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.action_grant_overlay))
                            }
                        } else {
                            Button(
                                onClick = {
                                    when (selectedMode) {
                                        ScanInputMode.SCREEN_OVERLAY -> onStartScreenOverlay()
                                        ScanInputMode.LIVE_CAMERA -> onLaunchCameraScan()
                                        ScanInputMode.SCREENSHOT -> onLaunchPhotoPicker()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("launch_screen_overlay_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selectedMode == ScanInputMode.SCREEN_OVERLAY) scouterCobalt else modeTint,
                                    contentColor = if (selectedMode == ScanInputMode.SCREEN_OVERLAY) Color.White else DexVoid
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                val ctaIcon = when (selectedMode) {
                                    ScanInputMode.SCREEN_OVERLAY -> Icons.Default.Radar
                                    ScanInputMode.LIVE_CAMERA -> Icons.Default.CameraAlt
                                    ScanInputMode.SCREENSHOT -> Icons.Default.PhotoLibrary
                                }
                                Icon(ctaIcon, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = selectedMode.actionLabel,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Interactive ML Screen-Read Test Deck
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "TRY SAMPLE POKÉMON",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyberCyan
                    )
                }
                Text(
                    text = "Tap a battle nameplate below to test screen reading & stats lookup:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sampleCards) { card ->
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onTestSampleCard(card.name, card.level, card.hpText) }
                                .testTag("sample_card_${card.name.lowercase().replace(" ", "_")}"),
                            shape = RoundedCornerShape(14.dp),
                            color = DexSurfaceElevated,
                            border = BorderStroke(1.dp, DexOutline)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = "${card.name} • Lv.${card.level}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                                Text(
                                    text = card.subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberCyan
                                )
                            }
                        }
                    }
                }
            }
        }

        // Progress & Status Feedback
        item {
            AnimatedVisibility(visible = scannerUi.isAnalyzingImage || scannerUi.statusBanner != null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (scannerUi.isAnalyzingImage) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp)),
                            color = CyberCyan,
                            trackColor = DexSurfaceElevated
                        )
                    }
                    scannerUi.statusBanner?.let { banner ->
                        Text(
                            text = banner,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                    }
                }
            }
        }

        // 5. Latest Recognized Pokémon Tactical HUD Card (prefer live overlay match if active)
        val activeMatch = overlayState.currentMatch ?: scannerUi.lastScanMatch
        activeMatch?.let { match ->
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "LAST DETECTED POKÉMON",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldMatch
                        )
                        if (overlayState.currentMatch != null) {
                            Text(
                                text = "Clear",
                                style = MaterialTheme.typography.labelMedium,
                                color = ScannerRed,
                                modifier = Modifier.clickable(onClick = onClearOverlayMatch)
                            )
                        }
                    }
                    ScanMatchSummaryBanner(
                        match = match,
                        showAbilitiesAndNatures = overlayState.showAbilitiesAndNatures,
                        onToggleAbilities = onToggleOverlayAbilities,
                        onCompare = { onComparePokemon(match.pokemon) }
                    )
                }
            }
        }

        item { Spacer(Modifier.height(12.dp)) }
    }
}
