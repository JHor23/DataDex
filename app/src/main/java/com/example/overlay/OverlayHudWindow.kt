package com.example.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.RectF
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DataDexStateHolder
import com.example.model.OverlayHudState
import com.example.model.PokemonEntry
import com.example.ui.components.AbilitiesTogglePill
import com.example.ui.components.PokemonTacticalBreakdown
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DataDexTheme
import com.example.ui.theme.DexSurface
import com.example.ui.theme.DexSurfaceElevated
import com.example.ui.theme.DexVoid
import com.example.ui.theme.EmeraldMatch
import com.example.ui.theme.ScannerRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoltGold
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Manages the floating WindowManager Compose HUD overlay over other apps.
 */
class OverlayHudWindow(
    private val context: Context,
    private val onTogglePause: () -> Unit,
    private val onToggleCollapse: () -> Unit,
    private val onToggleAbilities: () -> Unit,
    private val onScanNow: () -> Unit,
    private val onClearMatch: () -> Unit,
    private val onSelectSecondaryPokemon: (PokemonEntry) -> Unit,
    private val onCloseOverlay: () -> Unit
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var overlayAlpha: Float = 0.96f

    fun show(opacity: Float = 0.96f): Boolean {
        overlayAlpha = opacity
        if (composeView != null) return true
        if (!Settings.canDrawOverlays(context)) return false

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 28
            y = 140
        }
        layoutParams = params

        val owner = OverlayLifecycleOwner()
        val view = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                val hudState by DataDexStateHolder.overlayState.collectAsStateWithLifecycle()
                DataDexTheme(darkTheme = true) {
                    FloatingDataDexHud(
                        state = hudState,
                        opacity = overlayAlpha,
                        onDrag = { dx, dy ->
                            params.x += dx.roundToInt()
                            params.y += dy.roundToInt()
                            runCatching { windowManager.updateViewLayout(this, params) }
                        },
                        onTogglePause = onTogglePause,
                        onToggleCollapse = onToggleCollapse,
                        onToggleAbilities = onToggleAbilities,
                        onScanNow = onScanNow,
                        onClearMatch = onClearMatch,
                        onSelectSecondaryPokemon = onSelectSecondaryPokemon,
                        onClose = onCloseOverlay
                    )
                }
            }
        }
        return runCatching {
            owner.attachToView(view)
            windowManager.addView(view, params)
            composeView = view
            lifecycleOwner = owner
            true
        }.getOrElse {
            runCatching { windowManager.removeViewImmediate(view) }
            runCatching { owner.destroy() }
            composeView = null
            lifecycleOwner = null
            layoutParams = null
            false
        }
    }

    /**
     * Returns normalized [0..1] screen bounds of this overlay window so ML Kit can mask it out
     * and never read its own HUD text.
     */
    fun getNormalizedOverlayRect(screenWidth: Int, screenHeight: Int): RectF? {
        val view = composeView ?: return null
        if (!view.isAttachedToWindow || screenWidth <= 0 || screenHeight <= 0 || view.width <= 0 || view.height <= 0) {
            return null
        }
        return runCatching {
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            val left = (location[0].toFloat() / screenWidth).coerceIn(0f, 1f)
            val top = (location[1].toFloat() / screenHeight).coerceIn(0f, 1f)
            val right = ((location[0] + view.width).toFloat() / screenWidth).coerceIn(0f, 1f)
            val bottom = ((location[1] + view.height).toFloat() / screenHeight).coerceIn(0f, 1f)
            RectF(left, top, right, bottom)
        }.getOrNull()
    }

    fun updateOpacity(opacity: Float) {
        overlayAlpha = opacity
    }

    fun dismiss() {
        composeView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        lifecycleOwner?.destroy()
        composeView = null
        lifecycleOwner = null
        layoutParams = null
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FloatingDataDexHud(
    state: OverlayHudState,
    opacity: Float,
    onDrag: (Float, Float) -> Unit,
    onTogglePause: () -> Unit,
    onToggleCollapse: () -> Unit,
    onToggleAbilities: () -> Unit,
    onScanNow: () -> Unit,
    onClearMatch: () -> Unit,
    onSelectSecondaryPokemon: (PokemonEntry) -> Unit,
    onClose: () -> Unit
) {
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.isOverlayRunning) {
        while (state.isOverlayRunning) {
            nowMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    var flashMatchHighlight by remember { mutableStateOf(false) }
    LaunchedEffect(state.lastMatchUpdatedMillis) {
        if (state.lastMatchUpdatedMillis > 0L) {
            flashMatchHighlight = true
            delay(900L)
            flashMatchHighlight = false
        }
    }

    val isActivelyScanning = state.isOverlayRunning && !state.isScanningPaused && !state.isPausedForOwnApp
    var pulseScale = 1f
    var pulseAlpha = 1f
    var radarWave = 0f

    if (isActivelyScanning) {
        val infiniteTransition = rememberInfiniteTransition(label = "hud_pulse")
        pulseScale = infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.28f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 750),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot_scale"
        ).value
        pulseAlpha = infiniteTransition.animateFloat(
            initialValue = 0.45f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 750),
                repeatMode = RepeatMode.Reverse
            ),
            label = "dot_alpha"
        ).value
        radarWave = infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "hud_radar_wave"
        ).value
    }

    val borderColor by animateColorAsState(
        targetValue = when {
            flashMatchHighlight -> EmeraldMatch
            state.showAbilitiesAndNatures -> VoltGold.copy(alpha = 0.82f)
            state.isScanningPaused || state.isPausedForOwnApp -> VoltGold.copy(alpha = 0.7f)
            !state.isTargetContentVisible -> ScannerRed.copy(alpha = 0.7f)
            else -> CyberCyan.copy(alpha = 0.78f)
        },
        animationSpec = tween(durationMillis = 280),
        label = "hud_border_color"
    )

    val collapsedRingStroke = remember { Stroke(width = 1.6f) }
    val expandedRingStroke = remember { Stroke(width = 1.5f) }

    val statusDotColor = when {
        state.isScanningPaused || state.isPausedForOwnApp -> VoltGold
        !state.isTargetContentVisible -> ScannerRed
        state.isActivelyReading -> EmeraldMatch
        else -> CyberCyan
    }

    val scanAgeText = if (state.lastScanTimestampMillis > 0L) {
        val secs = ((nowMillis - state.lastScanTimestampMillis).coerceAtLeast(0L) / 1000L)
        if (secs <= 1L) "just now" else "${secs}s ago"
    } else {
        "starting..."
    }

    Surface(
        modifier = Modifier
            .widthIn(min = 216.dp, max = 354.dp)
            .alpha(opacity),
        shape = RoundedCornerShape(20.dp),
        color = DexVoid.copy(alpha = 0.98f),
        border = BorderStroke(width = if (flashMatchHighlight) 2.dp else 1.5.dp, color = borderColor),
        shadowElevation = 14.dp
    ) {
        AnimatedContent(
            targetState = state.isCollapsed,
            label = "overlay_collapse_transition"
        ) { collapsed ->
            if (collapsed) {
                Row(
                    modifier = Modifier
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                onDrag(dragAmount.x, dragAmount.y)
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier.size(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isActivelyScanning) {
                            Canvas(modifier = Modifier.size(16.dp)) {
                                drawCircle(
                                    color = statusDotColor.copy(alpha = (1f - radarWave) * 0.7f),
                                    radius = (size.minDimension * 0.5f) * (0.4f + radarWave * 0.6f),
                                    style = collapsedRingStroke
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .scale(if (isActivelyScanning) pulseScale else 1f)
                                .alpha(if (isActivelyScanning) pulseAlpha else 1f)
                                .clip(CircleShape)
                                .background(statusDotColor)
                        )
                    }
                    Column {
                        Text(
                            text = state.currentMatch?.pokemon?.name ?: "DataDex",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = when {
                                state.isScanningPaused -> "PAUSED"
                                state.isPausedForOwnApp -> "STANDBY"
                                else -> "SCAN #${state.framesProcessed}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.isScanningPaused || state.isPausedForOwnApp) VoltGold else CyberCyan
                        )
                    }
                    IconButton(
                        onClick = onToggleCollapse,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand overlay",
                            tint = CyberCyan
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close overlay",
                            tint = ScannerRed
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Draggable Top Bar with live frame counter & heartbeat
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DexSurface)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    onDrag(dragAmount.x, dragAmount.y)
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(15.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isActivelyScanning) {
                                    Canvas(modifier = Modifier.size(15.dp)) {
                                        drawCircle(
                                            color = statusDotColor.copy(alpha = (1f - radarWave) * 0.7f),
                                            radius = (size.minDimension * 0.5f) * (0.4f + radarWave * 0.6f),
                                            style = expandedRingStroke
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .scale(if (isActivelyScanning) pulseScale else 1f)
                                        .alpha(if (isActivelyScanning) pulseAlpha else 1f)
                                        .clip(CircleShape)
                                        .background(statusDotColor)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Radar,
                                contentDescription = "DataDex live scanner",
                                tint = statusDotColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = when {
                                    state.isScanningPaused -> "PAUSED"
                                    state.isPausedForOwnApp -> "DATADEX OPEN"
                                    !state.isTargetContentVisible -> "HIDDEN"
                                    else -> "READING #${state.framesProcessed}"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = statusDotColor
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onScanNow,
                                modifier = Modifier
                                    .size(30.dp)
                                    .semantics { contentDescription = "Scan screen now" }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Scan screen now",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            IconButton(
                                onClick = onTogglePause,
                                modifier = Modifier
                                    .size(30.dp)
                                    .semantics { contentDescription = "Pause or resume scan" }
                            ) {
                                Icon(
                                    imageVector = if (state.isScanningPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = if (state.isScanningPaused) "Resume scanning" else "Pause scanning",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            IconButton(
                                onClick = onToggleCollapse,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Collapse overlay",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Stop overlay",
                                    tint = ScannerRed,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    // Continuous visual scan pulse bar while overlay scanning is active
                    if (isActivelyScanning) {
                        val sweepBarColor = if (state.isActivelyReading) EmeraldMatch else CyberCyan
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.5.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(DexSurface)
                        ) {
                            val pulseWidth = size.width * 0.30f
                            val centerX = (size.width + pulseWidth) * radarWave - pulseWidth * 0.5f
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        sweepBarColor,
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

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 470.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val match = state.currentMatch
                        if (match != null) {
                            PokemonTacticalBreakdown(
                                pokemon = match.pokemon,
                                badgeLabel = if (flashMatchHighlight) "NEW • ${match.method.label}" else match.method.label,
                                confidence = match.confidence,
                                compact = true,
                                showAbilitiesAndNatures = state.showAbilitiesAndNatures,
                                headerTrailingContent = {
                                    AbilitiesTogglePill(
                                        active = state.showAbilitiesAndNatures,
                                        onClick = onToggleAbilities
                                    )
                                }
                            )

                            if (state.secondaryMatches.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "ALSO ON SCREEN",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyberCyan
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        state.secondaryMatches.forEach { other ->
                                            Surface(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { onSelectSecondaryPokemon(other) },
                                                shape = RoundedCornerShape(8.dp),
                                                color = DexSurfaceElevated,
                                                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.45f))
                                            ) {
                                                Text(
                                                    text = other.name,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextPrimary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DexSurface)
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = state.statusMessage,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Open any Pokémon game, card, or webpage. DataDex reads names automatically.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    // Always-visible Live OCR Telemetry Footer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(9.dp))
                            .background(DexSurface.copy(alpha = 0.9f))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (state.lastScannedTextPreview.isNotBlank()) {
                                "Screen: ${state.lastScannedTextPreview}"
                            } else {
                                "Watching screen ($scanAgeText)"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.lastScannedTextPreview.isNotBlank()) CyberCyan else TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (state.currentMatch != null) {
                            Text(
                                text = "Clear",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = ScannerRed,
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .clickable(onClick = onClearMatch)
                            )
                        } else {
                            Text(
                                text = scanAgeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
