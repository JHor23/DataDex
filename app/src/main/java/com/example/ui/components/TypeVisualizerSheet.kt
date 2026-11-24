package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PokemonType
import com.example.model.TypeEffectivenessChart
import com.example.model.TypeMultiplier
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DexOutline
import com.example.ui.theme.DexSurface
import com.example.ui.theme.DexSurfaceElevated
import com.example.ui.theme.DexVoid
import com.example.ui.theme.EmeraldMatch
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.ScannerRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoltGold
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private enum class RadarMode(val label: String) {
    BOTH("Dual"),
    DEFENSE("Defend"),
    OFFENSE("Attack")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypeVisualizerModal(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var mode by remember { mutableStateOf(RadarMode.BOTH) }
    var primaryType by remember { mutableStateOf(PokemonType.FIRE) }
    var secondaryType by remember { mutableStateOf<PokemonType?>(null) }
    var inspectedSpokeType by remember { mutableStateOf(PokemonType.GRASS) }

    val defendingTypes = remember(primaryType, secondaryType, mode) {
        if (mode == RadarMode.DEFENSE) listOfNotNull(primaryType, secondaryType) else listOf(primaryType)
    }

    val defenseMultipliers = remember(defendingTypes) {
        PokemonType.entries.map { atk ->
            atk to TypeEffectivenessChart.multiplierAgainst(atk, defendingTypes)
        }
    }

    val offenseMultipliers = remember(primaryType) {
        PokemonType.entries.map { def ->
            def to TypeEffectivenessChart.attackingMultiplier(primaryType, def)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DexSurfaceElevated,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Compact Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(CyberCyan.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Type Attribute Graph",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Tap any spoke on the graph or select a type badge below",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee(iterations = Int.MAX_VALUE)
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("close_type_visualizer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Type Visualizer",
                        tint = TextSecondary
                    )
                }
            }

            // Mode Switcher + Selected Type Pill(s)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(DexVoid)
                        .border(1.dp, DexOutline, RoundedCornerShape(10.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    RadarMode.entries.forEach { tab ->
                        val selected = mode == tab
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(7.dp))
                                .clickable {
                                    mode = tab
                                    if (tab != RadarMode.DEFENSE) {
                                        secondaryType = null
                                    }
                                }
                                .testTag("visualizer_mode_${tab.name.lowercase()}"),
                            shape = RoundedCornerShape(7.dp),
                            color = if (selected) CyberCyan.copy(alpha = 0.22f) else Color.Transparent,
                            border = if (selected) BorderStroke(1.dp, CyberCyan.copy(alpha = 0.65f)) else null
                        ) {
                            Text(
                                text = tab.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (selected) CyberCyan else TextSecondary,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TypeBadgeChip(type = primaryType, compact = true)
                    if (mode == RadarMode.DEFENSE && secondaryType != null) {
                        TypeBadgeChip(type = secondaryType!!, compact = true)
                    }
                }
            }

            // NBA 2K-Style 18-Spoke Radial Attribute Spider Graph
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF060B16),
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Legend Row (Cleanly separated from graph below)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (mode != RadarMode.DEFENSE) {
                                LegendDot(color = EmeraldMatch, label = "Atk 2×")
                            }
                            if (mode != RadarMode.OFFENSE) {
                                LegendDot(color = ScannerRed, label = "Weak 2×/4×")
                            }
                            LegendDot(color = CyberCyan, label = "1× Ring")
                        }

                        val spokeAtk = TypeEffectivenessChart.attackingMultiplier(primaryType, inspectedSpokeType)
                        val spokeDef = TypeEffectivenessChart.multiplierAgainst(inspectedSpokeType, defendingTypes)
                        val inspectText = when (mode) {
                            RadarMode.OFFENSE -> "${primaryType.shortCode()}→${inspectedSpokeType.shortCode()}: ${formatMult(spokeAtk)}"
                            RadarMode.DEFENSE -> "${inspectedSpokeType.shortCode()}→${defendingTypes.joinToString("/") { it.shortCode() }}: ${formatMult(spokeDef)}"
                            RadarMode.BOTH -> "${inspectedSpokeType.shortCode()}: Atk ${formatMult(spokeAtk)} • Def ${formatMult(spokeDef)}"
                        }
                        Text(
                            text = inspectText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = CyberCyan,
                            maxLines = 1,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .basicMarquee(iterations = Int.MAX_VALUE)
                        )
                    }

                    Spacer(Modifier.height(2.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AttributeSpiderCanvas(
                            mode = mode,
                            defenseMultipliers = defenseMultipliers,
                            offenseMultipliers = offenseMultipliers,
                            inspectedType = inspectedSpokeType,
                            onSelectSpoke = { inspectedSpokeType = it },
                            modifier = Modifier
                                .fillMaxWidth(0.84f)
                                .heightIn(max = 248.dp)
                                .aspectRatio(1f, matchHeightConstraintsFirst = true)
                                .clipToBounds()
                                .testTag("type_radar_canvas")
                        )
                    }
                }
            }

            // Compact Advantage / Disadvantage Readout (Zero Scroll)
            val atkProfile = remember(primaryType) {
                TypeEffectivenessChart.computeOffensiveProfile(primaryType)
            }
            val defProfile = remember(defendingTypes) {
                TypeEffectivenessChart.computeWeaknessProfile(defendingTypes)
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = DexSurface,
                border = BorderStroke(1.dp, DexOutline)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    if (mode == RadarMode.OFFENSE) {
                        CompactMatchupLine(
                            label = "2× STRONG:",
                            labelColor = EmeraldMatch,
                            items = atkProfile.superEffective
                        )
                        CompactMatchupLine(
                            label = "½× / 0×:",
                            labelColor = ScannerRed,
                            items = atkProfile.notVeryEffective + atkProfile.noEffect
                        )
                    } else if (mode == RadarMode.DEFENSE) {
                        CompactMatchupLine(
                            label = "WEAK TO:",
                            labelColor = ScannerRed,
                            items = defProfile.quadWeaknesses + defProfile.doubleWeaknesses
                        )
                        CompactMatchupLine(
                            label = "RESISTS:",
                            labelColor = EmeraldMatch,
                            items = defProfile.immunities + defProfile.doubleResistances + defProfile.resistances
                        )
                    } else {
                        CompactMatchupLine(
                            label = "2× ATK:",
                            labelColor = EmeraldMatch,
                            items = atkProfile.superEffective
                        )
                        CompactMatchupLine(
                            label = "WEAK TO:",
                            labelColor = ScannerRed,
                            items = defProfile.quadWeaknesses + defProfile.doubleWeaknesses
                        )
                        CompactMatchupLine(
                            label = "RESISTS:",
                            labelColor = CyberCyan,
                            items = defProfile.immunities + defProfile.resistances
                        )
                    }
                }
            }

            // 6x3 Compact Type Selector Grid (All 18 types visible at once, zero scrolling)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (mode == RadarMode.DEFENSE) {
                        "SELECT 1 OR 2 DEFENDING TYPES"
                    } else {
                        "SELECT POKÉMON TYPE"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                val rows = PokemonType.entries.chunked(6)
                rows.forEach { rowTypes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        rowTypes.forEach { type ->
                            val isPrimary = type == primaryType
                            val isSecondary = mode == RadarMode.DEFENSE && type == secondaryType
                            val selected = isPrimary || isSecondary
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (mode != RadarMode.DEFENSE) {
                                            primaryType = type
                                        } else {
                                            when {
                                                type == secondaryType -> secondaryType = null
                                                type == primaryType -> {
                                                    secondaryType?.let {
                                                        primaryType = it
                                                        secondaryType = null
                                                    }
                                                }
                                                secondaryType == null -> secondaryType = type
                                                else -> {
                                                    primaryType = secondaryType!!
                                                    secondaryType = type
                                                }
                                            }
                                        }
                                    }
                                    .testTag("visualizer_type_${type.name.lowercase()}"),
                                shape = RoundedCornerShape(8.dp),
                                color = if (selected) type.badgeColor.copy(alpha = 0.36f) else DexVoid,
                                border = BorderStroke(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) type.badgeColor else DexOutline.copy(alpha = 0.8f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(type.badgeColor)
                                    )
                                    Spacer(Modifier.size(3.dp))
                                    Text(
                                        text = type.shortCode(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (selected) TextPrimary else TextSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttributeSpiderCanvas(
    mode: RadarMode,
    defenseMultipliers: List<Pair<PokemonType, Float>>,
    offenseMultipliers: List<Pair<PokemonType, Float>>,
    inspectedType: PokemonType,
    onSelectSpoke: (PokemonType) -> Unit,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val types = PokemonType.entries
    val count = types.size
    val ringPath = remember { Path() }
    val defPath = remember { Path() }
    val atkPath = remember { Path() }
    val ringLevels = remember { floatArrayOf(0.24f, 0.50f, 0.76f, 1.0f) }
    val neutralRingStroke = remember {
        Stroke(width = 1.6f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f)))
    }
    val outerRingStroke = remember { Stroke(width = 1f) }
    val polygonStroke = remember { Stroke(width = 2.4f) }
    val labelStyleMedium = remember {
        TextStyle(
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 9.5.sp
        )
    }
    val labelStyleBold = remember {
        TextStyle(
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 9.5.sp
        )
    }

    Canvas(
        modifier = modifier.pointerInput(count) {
            detectTapGestures { tap ->
                val cx = size.width / 2f
                val cy = size.height / 2f
                val dx = tap.x - cx
                val dy = tap.y - cy
                // Angle starting from -90 deg (top = index 0) clockwise
                var angle = atan2(dy.toDouble(), dx.toDouble()) + PI / 2.0
                if (angle < 0) angle += 2.0 * PI
                val step = (2.0 * PI) / count
                val idx = (((angle + step / 2.0) / step).toInt()) % count
                onSelectSpoke(types[idx])
            }
        }
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxRadius = (size.minDimension / 2f) * 0.64f
        val labelRadius = (size.minDimension / 2f) * 0.83f
        val stepRad = (2.0 * PI) / count

        fun spokeAngle(index: Int): Double = -PI / 2.0 + index * stepRad
        fun pointAt(index: Int, norm: Float): Offset {
            val a = spokeAngle(index)
            val r = maxRadius * norm
            return Offset(cx + (r * cos(a)).toFloat(), cy + (r * sin(a)).toFloat())
        }

        // NBA 2K-style concentric octagonal/18-gon web rings
        for (levelIdx in ringLevels.indices) {
            val norm = ringLevels[levelIdx]
            ringPath.reset()
            for (i in 0 until count) {
                val pt = pointAt(i, norm)
                if (i == 0) ringPath.moveTo(pt.x, pt.y) else ringPath.lineTo(pt.x, pt.y)
            }
            ringPath.close()
            val isNeutralRing = levelIdx == 1
            drawPath(
                path = ringPath,
                color = if (isNeutralRing) CyberCyan.copy(alpha = 0.36f) else DexOutline.copy(alpha = 0.55f),
                style = if (isNeutralRing) neutralRingStroke else outerRingStroke
            )
        }

        // Radial spokes
        for (i in 0 until count) {
            val outer = pointAt(i, 1f)
            val isInspected = types[i] == inspectedType
            drawLine(
                color = if (isInspected) CyberCyan.copy(alpha = 0.75f) else DexOutline.copy(alpha = 0.45f),
                start = Offset(cx, cy),
                end = outer,
                strokeWidth = if (isInspected) 2.2f else 1f
            )
        }

        fun mapDefenseNorm(mult: Float): Float = when {
            mult == 0f -> 0.10f
            mult <= 0.26f -> 0.22f
            mult <= 0.51f -> 0.34f
            mult <= 1.1f -> 0.50f
            mult <= 2.1f -> 0.78f
            else -> 1.0f
        }

        fun mapOffenseNorm(mult: Float): Float = when {
            mult == 0f -> 0.10f
            mult <= 0.51f -> 0.30f
            mult <= 1.1f -> 0.50f
            else -> 0.92f
        }

        // Draw Defense Polygon (Weaknesses spike outward, resistances hug center)
        if (mode == RadarMode.DEFENSE || mode == RadarMode.BOTH) {
            defPath.reset()
            for (idx in defenseMultipliers.indices) {
                val pt = pointAt(idx, mapDefenseNorm(defenseMultipliers[idx].second))
                if (idx == 0) defPath.moveTo(pt.x, pt.y) else defPath.lineTo(pt.x, pt.y)
            }
            defPath.close()
            drawPath(defPath, color = ScannerRed.copy(alpha = if (mode == RadarMode.BOTH) 0.18f else 0.26f))
            drawPath(defPath, color = ScannerRed.copy(alpha = 0.92f), style = polygonStroke)

            for (idx in defenseMultipliers.indices) {
                val mult = defenseMultipliers[idx].second
                val pt = pointAt(idx, mapDefenseNorm(mult))
                val dotColor = when {
                    mult >= 3.5f -> VoltGold
                    mult >= 1.9f -> ScannerRed
                    mult == 0f -> CyberCyan
                    mult <= 0.51f -> EmeraldMatch
                    else -> Color(0xFF475569)
                }
                val dotRadius = if (mult >= 1.9f || mult <= 0.51f) 4.8f else 2.5f
                drawCircle(color = dotColor, radius = dotRadius, center = pt)
            }
        }

        // Draw Offense Polygon (2x super effective spikes outward)
        if (mode == RadarMode.OFFENSE || mode == RadarMode.BOTH) {
            atkPath.reset()
            for (idx in offenseMultipliers.indices) {
                val pt = pointAt(idx, mapOffenseNorm(offenseMultipliers[idx].second))
                if (idx == 0) atkPath.moveTo(pt.x, pt.y) else atkPath.lineTo(pt.x, pt.y)
            }
            atkPath.close()
            drawPath(atkPath, color = EmeraldMatch.copy(alpha = if (mode == RadarMode.BOTH) 0.20f else 0.28f))
            drawPath(atkPath, color = EmeraldMatch.copy(alpha = 0.95f), style = polygonStroke)

            for (idx in offenseMultipliers.indices) {
                val mult = offenseMultipliers[idx].second
                val pt = pointAt(idx, mapOffenseNorm(mult))
                val dotColor = when {
                    mult >= 1.9f -> EmeraldMatch
                    mult == 0f -> Color(0xFF64748B)
                    mult <= 0.51f -> ScannerRed
                    else -> Color(0xFF475569)
                }
                val dotRadius = if (mult >= 1.9f || mult <= 0.51f) 4.8f else 2.5f
                drawCircle(color = dotColor, radius = dotRadius, center = pt)
            }
        }

        // Perimeter Type Labels (All 18 visible cleanly around the wheel)
        for (idx in types.indices) {
            val type = types[idx]
            val angle = spokeAngle(idx)
            val lx = cx + (labelRadius * cos(angle)).toFloat()
            val ly = cy + (labelRadius * sin(angle)).toFloat()

            val atkMult = offenseMultipliers[idx].second
            val defMult = defenseMultipliers[idx].second
            val isHighlighted = when (mode) {
                RadarMode.OFFENSE -> atkMult != 1f
                RadarMode.DEFENSE -> defMult != 1f
                RadarMode.BOTH -> atkMult >= 1.9f || defMult >= 1.9f || defMult == 0f
            }
            val labelColor = when {
                type == inspectedType -> CyberCyan
                mode != RadarMode.DEFENSE && atkMult >= 1.9f -> EmeraldMatch
                mode != RadarMode.OFFENSE && defMult >= 1.9f -> ScannerRed
                mode != RadarMode.OFFENSE && defMult == 0f -> CyberCyan
                isHighlighted -> TextPrimary
                else -> TextMuted
            }

            drawCenteredLabel(
                textMeasurer = textMeasurer,
                text = type.shortCode(),
                center = Offset(lx, ly),
                style = (if (type == inspectedType || isHighlighted) labelStyleBold else labelStyleMedium)
                    .copy(color = labelColor)
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCenteredLabel(
    textMeasurer: TextMeasurer,
    text: String,
    center: Offset,
    style: TextStyle
) {
    val result = textMeasurer.measure(
        text = text,
        style = style
    )
    drawText(
        textLayoutResult = result,
        topLeft = Offset(
            x = (center.x - result.size.width / 2f).coerceIn(2f, (size.width - result.size.width - 2f).coerceAtLeast(2f)),
            y = (center.y - result.size.height / 2f).coerceIn(2f, (size.height - result.size.height - 2f).coerceAtLeast(2f))
        )
    )
}

@Composable
private fun CompactMatchupLine(
    label: String,
    labelColor: Color,
    items: List<TypeMultiplier>
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = labelColor
        )
        Text(
            text = if (items.isEmpty()) {
                "None"
            } else {
                items.joinToString(" • ") { "${formatMult(it.multiplier)} ${it.type.displayName}" }
            },
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .basicMarquee(iterations = Int.MAX_VALUE)
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

private fun formatMult(mult: Float): String = when {
    mult == 0f -> "0×"
    mult == 0.25f -> "¼×"
    mult == 0.5f -> "½×"
    mult == 2f -> "2×"
    mult == 4f -> "4×"
    else -> "1×"
}

private fun PokemonType.shortCode(): String = when (this) {
    PokemonType.NORMAL -> "NOR"
    PokemonType.FIRE -> "FIR"
    PokemonType.WATER -> "WAT"
    PokemonType.ELECTRIC -> "ELE"
    PokemonType.GRASS -> "GRA"
    PokemonType.ICE -> "ICE"
    PokemonType.FIGHTING -> "FIG"
    PokemonType.POISON -> "POI"
    PokemonType.GROUND -> "GRO"
    PokemonType.FLYING -> "FLY"
    PokemonType.PSYCHIC -> "PSY"
    PokemonType.BUG -> "BUG"
    PokemonType.ROCK -> "ROC"
    PokemonType.GHOST -> "GHO"
    PokemonType.DRAGON -> "DRA"
    PokemonType.DARK -> "DAR"
    PokemonType.STEEL -> "STE"
    PokemonType.FAIRY -> "FAI"
}
