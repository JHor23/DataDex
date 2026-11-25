package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.mlkit.vision.MlKitAnalyzer
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.ml.DataDexMlEngine
import com.example.model.DetectionOverlayBox
import com.example.model.PokemonEntry
import com.example.model.ScanMatch
import com.example.ui.components.ScanMatchSummaryBanner
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DexSurface
import com.example.ui.theme.DexVoid
import com.example.ui.theme.EmeraldMatch
import com.example.ui.theme.ScannerRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CameraScanScreen(
    mlEngine: DataDexMlEngine,
    enableVisionFallback: Boolean,
    allowNetworkLookup: Boolean,
    onMatchDetected: (ScanMatch, String, List<String>) -> Unit,
    onPinToOverlay: (PokemonEntry) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var latestMatch by remember { mutableStateOf<ScanMatch?>(null) }
    var liveBoxes by remember { mutableStateOf<List<DetectionOverlayBox>>(emptyList()) }
    var liveOcrPreview by remember { mutableStateOf("") }
    var cameraErrorMessage by remember { mutableStateOf<String?>(null) }
    var isProcessingFrame by remember { mutableStateOf(false) }

    val pulseTransition = rememberInfiniteTransition(label = "camera_scan_pulse")
    val radarWave by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "camera_radar_wave"
    )
    val sweepProgress by pulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "camera_scan_sweep"
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        mlEngine.ensureInitialized(context)
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DexVoid)
    ) {
        if (hasCameraPermission) {
            val previewView = remember { PreviewView(context) }
            val cameraController = remember {
                runCatching {
                    LifecycleCameraController(context).apply {
                        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
                    }
                }.getOrNull()
            }

            DisposableEffect(cameraController, lifecycleOwner, enableVisionFallback, allowNetworkLookup) {
                if (cameraController == null) {
                    cameraErrorMessage = "Camera hardware unavailable on this device."
                    onDispose { }
                } else {
                    cameraErrorMessage = null
                    val mainExecutor = ContextCompat.getMainExecutor(context)
                    mlEngine.ensureInitialized(context)

                    val textDetector = mlEngine.getSafeTextRecognizer(context)
                    val labelDetector = if (enableVisionFallback) mlEngine.getSafeImageLabeler(context) else null
                    val objectDetector = if (enableVisionFallback) mlEngine.getSafeObjectDetector(context) else null
                    val detectors = listOfNotNull(textDetector, labelDetector, objectDetector)

                    runCatching {
                        previewView.controller = cameraController
                        if (detectors.isNotEmpty()) {
                            val analyzer = MlKitAnalyzer(
                                detectors,
                                CameraController.COORDINATE_SYSTEM_VIEW_REFERENCED,
                                mainExecutor
                            ) { result ->
                                if (isProcessingFrame) return@MlKitAnalyzer
                                val visionText = textDetector?.let { det ->
                                    runCatching { result.getValue(det) }.getOrNull()
                                }
                                val labels = labelDetector?.let { det ->
                                    runCatching { result.getValue(det) }.getOrNull()
                                }.orEmpty()
                                val objects = objectDetector?.let { det ->
                                    runCatching { result.getValue(det) }.getOrNull()
                                }.orEmpty()

                                val viewW = previewView.width.coerceAtLeast(1).toFloat()
                                val viewH = previewView.height.coerceAtLeast(1).toFloat()

                                isProcessingFrame = true
                                coroutineScope.launch {
                                    try {
                                        val analysis = withContext(Dispatchers.Default) {
                                            mlEngine.processVisionOutputs(
                                                visionText = visionText,
                                                imageLabels = labels,
                                                detectedObjects = objects,
                                                frameWidth = viewW,
                                                frameHeight = viewH,
                                                enableVisionFallback = enableVisionFallback,
                                                allowNetworkLookup = allowNetworkLookup
                                            )
                                        }
                                        liveBoxes = analysis.boxes
                                        liveOcrPreview = analysis.rawTextPreview
                                        analysis.match?.let { match ->
                                            latestMatch = match
                                            onMatchDetected(match, analysis.rawTextPreview, analysis.visionLabels)
                                        }
                                    } catch (_: Throwable) {
                                        // Keep live camera running if a single frame fails
                                    } finally {
                                        isProcessingFrame = false
                                    }
                                }
                            }
                            cameraController.setImageAnalysisAnalyzer(mainExecutor, analyzer)
                        } else {
                            cameraErrorMessage = "ML scanner initializing; camera preview only."
                        }
                        cameraController.bindToLifecycle(lifecycleOwner)
                        cameraController.initializationFuture.addListener(
                            {
                                runCatching {
                                    val hasBack = cameraController.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                                    val hasFront = cameraController.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
                                    if (!hasBack && hasFront) {
                                        cameraController.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                                    } else if (!hasBack && !hasFront) {
                                        cameraErrorMessage = "No active camera detected on this device."
                                    }
                                }
                            },
                            mainExecutor
                        )
                    }.onFailure { error ->
                        cameraErrorMessage = error.localizedMessage ?: "Unable to start camera scanner."
                    }

                    onDispose {
                        runCatching { cameraController.clearImageAnalysisAnalyzer() }
                        runCatching { cameraController.unbind() }
                    }
                }
            }

            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // Real-time ML Kit HUD bounding box + visual scan pulse overlay
            val pulseColor = if (latestMatch != null) EmeraldMatch else CyberCyan
            val ringStroke = remember { Stroke(width = 3f) }
            val matchBoxStroke = remember { Stroke(width = 5f) }
            val otherBoxStroke = remember { Stroke(width = 2.5f) }
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("camera_visual_scan_pulse")
            ) {
                val center = Offset(size.width * 0.5f, size.height * 0.44f)
                val maxRadius = size.minDimension * 0.34f

                // Expanding concentric sonar pulse rings
                for (step in 0..1) {
                    val wave = if (step == 0) radarWave else (radarWave + 0.5f) % 1f
                    val radius = maxRadius * (0.18f + wave * 0.82f)
                    drawCircle(
                        color = pulseColor.copy(alpha = (1f - wave) * 0.52f),
                        radius = radius,
                        center = center,
                        style = ringStroke
                    )
                }

                // Vertical sweeping scanner beam across viewfinder
                val sweepY = size.height * (0.14f + sweepProgress * 0.64f)
                val beamBand = size.height * 0.08f
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            pulseColor.copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        startY = sweepY - beamBand,
                        endY = sweepY + beamBand
                    ),
                    topLeft = Offset(0f, sweepY - beamBand),
                    size = Size(size.width, beamBand * 2f)
                )
                drawLine(
                    color = pulseColor.copy(alpha = 0.65f),
                    start = Offset(size.width * 0.08f, sweepY),
                    end = Offset(size.width * 0.92f, sweepY),
                    strokeWidth = 2.5f
                )

                // Tactical corner viewfinder brackets
                val boxW = size.minDimension * 0.68f
                val boxH = size.minDimension * 0.54f
                val boxLeft = center.x - boxW * 0.5f
                val boxTop = center.y - boxH * 0.5f
                val boxRight = center.x + boxW * 0.5f
                val boxBottom = center.y + boxH * 0.5f
                val cornerLen = 28.dp.toPx()
                val bracketColor = pulseColor.copy(alpha = 0.72f)
                // Top-left
                drawLine(bracketColor, Offset(boxLeft, boxTop), Offset(boxLeft + cornerLen, boxTop), 4f)
                drawLine(bracketColor, Offset(boxLeft, boxTop), Offset(boxLeft, boxTop + cornerLen), 4f)
                // Top-right
                drawLine(bracketColor, Offset(boxRight - cornerLen, boxTop), Offset(boxRight, boxTop), 4f)
                drawLine(bracketColor, Offset(boxRight, boxTop), Offset(boxRight, boxTop + cornerLen), 4f)
                // Bottom-left
                drawLine(bracketColor, Offset(boxLeft, boxBottom - cornerLen), Offset(boxLeft, boxBottom), 4f)
                drawLine(bracketColor, Offset(boxLeft, boxBottom), Offset(boxLeft + cornerLen, boxBottom), 4f)
                // Bottom-right
                drawLine(bracketColor, Offset(boxRight - cornerLen, boxBottom), Offset(boxRight, boxBottom), 4f)
                drawLine(bracketColor, Offset(boxRight, boxBottom - cornerLen), Offset(boxRight, boxBottom), 4f)

                for (box in liveBoxes) {
                    val left = box.normalizedRect.left * size.width
                    val top = box.normalizedRect.top * size.height
                    val w = (box.normalizedRect.right - box.normalizedRect.left) * size.width
                    val h = (box.normalizedRect.bottom - box.normalizedRect.top) * size.height
                    val strokeColor = if (box.isPokemonMatch) EmeraldMatch else CyberCyan.copy(alpha = 0.65f)
                    drawRect(
                        color = strokeColor,
                        topLeft = Offset(left, top),
                        size = Size(w, h),
                        style = if (box.isPokemonMatch) matchBoxStroke else otherBoxStroke
                    )
                }
            }
        } else {
            // Permission rationale state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(CyberCyan.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Camera Access Needed",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.camera_permission_rationale),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier
                        .height(50.dp)
                        .testTag("grant_camera_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = ScannerRed)
                ) {
                    Text("Enable Camera Scanner")
                }
            }
        }

        // Top HUD Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = CircleShape,
                color = DexVoid.copy(alpha = 0.78f),
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.45f))
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("camera_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Scanner Hub",
                        tint = TextPrimary
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DexVoid.copy(alpha = 0.82f),
                border = BorderStroke(
                    1.dp,
                    if (cameraErrorMessage != null) ScannerRed.copy(alpha = 0.65f) else CyberCyan.copy(alpha = 0.45f)
                )
            ) {
                Text(
                    text = when {
                        cameraErrorMessage != null -> cameraErrorMessage!!
                        liveOcrPreview.isNotBlank() -> "Reading: ${liveOcrPreview.take(26)}"
                        else -> "AIM AT POKÉMON OR CARD"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = if (cameraErrorMessage != null) ScannerRed else CyberCyan,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        // Bottom Live Detection HUD Card
        AnimatedVisibility(
            visible = latestMatch != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            latestMatch?.let { match ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScanMatchSummaryBanner(match = match)
                    Button(
                        onClick = { onPinToOverlay(match.pokemon) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("camera_pin_overlay_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = DexVoid)
                    ) {
                        Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Pin ${match.pokemon.name} to Overlay")
                    }
                }
            }
        }
    }
}
