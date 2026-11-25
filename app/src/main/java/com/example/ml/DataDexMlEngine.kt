package com.example.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import com.example.data.PokemonRepository
import com.example.model.DetectionOverlayBox
import com.example.model.PokemonEntry
import com.example.model.RecognitionMethod
import com.example.model.ScanMatch
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.sdkinternal.MlKitContext
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.interfaces.Detector
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class MlFrameAnalysis(
    val match: ScanMatch?,
    val secondaryMatches: List<PokemonEntry>,
    val rawTextPreview: String,
    val visionLabels: List<String>,
    val boxes: List<DetectionOverlayBox>
)

/**
 * Dual-pipeline on-device Machine Learning engine powered by ML Kit:
 * 1) ML Kit Text Recognition (OCR) for reading Pokémon names from screens, cards, and UIs.
 * 2) ML Kit Image Labeling + Object Detection for real-time visual recognition & bounding boxes.
 */
class DataDexMlEngine(
    private val repository: PokemonRepository = PokemonRepository.getInstance()
) {
    private var _textRecognizer: TextRecognizer? = null
    private var _imageLabeler: ImageLabeler? = null
    private var _objectDetector: ObjectDetector? = null

    val textRecognizer: TextRecognizer
        get() = _textRecognizer ?: TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            .also { _textRecognizer = it }

    private val maskPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }

    val imageLabeler: ImageLabeler
        get() = _imageLabeler ?: ImageLabeling.getClient(
            ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.52f)
                .build()
        ).also { _imageLabeler = it }

    val objectDetector: ObjectDetector
        get() = _objectDetector ?: ObjectDetection.getClient(
            ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .build()
        ).also { _objectDetector = it }

    @Volatile
    var lastErrorMessage: String? = null
        private set

    fun ensureInitialized(context: Context) {
        Companion.ensureInitialized(context)
    }

    fun getSafeTextRecognizer(context: Context): TextRecognizer? {
        ensureInitialized(context)
        return runCatching { textRecognizer }
            .onSuccess { lastErrorMessage = null }
            .onFailure { error ->
                lastErrorMessage = error.localizedMessage ?: error.javaClass.simpleName
            }
            .getOrNull()
    }

    fun getSafeImageLabeler(context: Context): ImageLabeler? {
        ensureInitialized(context)
        return runCatching { imageLabeler }
            .onFailure { error ->
                if (lastErrorMessage == null) {
                    lastErrorMessage = error.localizedMessage ?: error.javaClass.simpleName
                }
            }
            .getOrNull()
    }

    fun getSafeObjectDetector(context: Context): ObjectDetector? {
        ensureInitialized(context)
        return runCatching { objectDetector }
            .onFailure { error ->
                if (lastErrorMessage == null) {
                    lastErrorMessage = error.localizedMessage ?: error.javaClass.simpleName
                }
            }
            .getOrNull()
    }

    fun createCameraDetectors(
        context: Context,
        includeVisionDetectors: Boolean
    ): List<Detector<*>> {
        ensureInitialized(context)
        return buildList {
            getSafeTextRecognizer(context)?.let { add(it) }
            if (includeVisionDetectors) {
                getSafeImageLabeler(context)?.let { add(it) }
                getSafeObjectDetector(context)?.let { add(it) }
            }
        }
    }

    suspend fun analyzeBitmap(
        bitmap: Bitmap,
        enableVisionFallback: Boolean = true,
        allowNetworkLookup: Boolean = true,
        maskNormalizedRect: RectF? = null
    ): MlFrameAnalysis {
        if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) {
            return EMPTY_ANALYSIS
        }

        val normalizedBitmap = prepareScannableBitmap(bitmap, maskNormalizedRect) ?: return EMPTY_ANALYSIS

        try {
            val image = runCatching { InputImage.fromBitmap(normalizedBitmap, 0) }.getOrNull()
                ?: return EMPTY_ANALYSIS
            val width = normalizedBitmap.width.coerceAtLeast(1).toFloat()
            val height = normalizedBitmap.height.coerceAtLeast(1).toFloat()

            val visionText = runCatching { textRecognizer.process(image).awaitResult() }
                .onSuccess { lastErrorMessage = null }
                .onFailure {
                    if (it is CancellationException) throw it
                    lastErrorMessage = it.localizedMessage ?: it.javaClass.simpleName
                }
                .getOrNull()
            val textLines = visionText?.textBlocks
                ?.flatMap { block -> block.lines.map { it.text } }
                ?.takeIf { it.isNotEmpty() }
                ?: visionText?.text?.lines()?.filter { it.isNotBlank() }.orEmpty()
            val ocrCandidates = PokemonMatcher.matchAllFromTextLines(textLines)

            val imageLabels: List<ImageLabel>
            val detectedObjects: List<DetectedObject>
            if (ocrCandidates.isEmpty() && enableVisionFallback) {
                imageLabels = runCatching { imageLabeler.process(image).awaitResult() }
                    .onFailure { if (it is CancellationException) throw it }
                    .getOrElse { emptyList() }
                detectedObjects = runCatching { objectDetector.process(image).awaitResult() }
                    .onFailure { if (it is CancellationException) throw it }
                    .getOrElse { emptyList() }
            } else {
                imageLabels = emptyList()
                detectedObjects = emptyList()
            }

            return runCatching {
                processVisionOutputs(
                    visionText = visionText,
                    imageLabels = imageLabels,
                    detectedObjects = detectedObjects,
                    frameWidth = width,
                    frameHeight = height,
                    enableVisionFallback = enableVisionFallback,
                    allowNetworkLookup = allowNetworkLookup,
                    precomputedTextLines = textLines,
                    precomputedOcrCandidates = ocrCandidates
                )
            }.onFailure {
                if (it is CancellationException) throw it
            }.getOrDefault(
                MlFrameAnalysis(
                    match = null,
                    secondaryMatches = emptyList(),
                    rawTextPreview = textLines.take(3).joinToString(" • ").take(90),
                    visionLabels = emptyList(),
                    boxes = emptyList()
                )
            )
        } finally {
            if (normalizedBitmap !== bitmap && !normalizedBitmap.isRecycled) {
                normalizedBitmap.recycle()
            }
        }
    }

    private fun prepareScannableBitmap(
        source: Bitmap,
        maskNormalizedRect: RectF?
    ): Bitmap? {
        val needsConfigCopy = source.config != Bitmap.Config.ARGB_8888 ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && source.config == Bitmap.Config.HARDWARE)
        val needsPadding = source.width < MIN_MLKIT_DIMENSION || source.height < MIN_MLKIT_DIMENSION
        val needsMask = maskNormalizedRect != null && !maskNormalizedRect.isEmpty

        if (!needsConfigCopy && !needsPadding && !needsMask) {
            return source
        }

        val targetW = source.width.coerceAtLeast(MIN_MLKIT_DIMENSION)
        val targetH = source.height.coerceAtLeast(MIN_MLKIT_DIMENSION)

        return runCatching {
            val out = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            canvas.drawColor(Color.BLACK)

            val softwareSource = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                source.config == Bitmap.Config.HARDWARE
            ) {
                source.copy(Bitmap.Config.ARGB_8888, false)
            } else {
                source
            }

            try {
                if (softwareSource != null && !softwareSource.isRecycled) {
                    canvas.drawBitmap(softwareSource, 0f, 0f, null)
                }
            } finally {
                if (softwareSource !== source && softwareSource != null && !softwareSource.isRecycled) {
                    softwareSource.recycle()
                }
            }

            if (needsMask && maskNormalizedRect != null) {
                val w = source.width.toFloat()
                val h = source.height.toFloat()
                canvas.drawRect(
                    (maskNormalizedRect.left * w).coerceIn(0f, w),
                    (maskNormalizedRect.top * h).coerceIn(0f, h),
                    (maskNormalizedRect.right * w).coerceIn(0f, w),
                    (maskNormalizedRect.bottom * h).coerceIn(0f, h),
                    maskPaint
                )
            }
            out
        }.getOrNull()
    }

    suspend fun processVisionOutputs(
        visionText: Text?,
        imageLabels: List<ImageLabel>,
        detectedObjects: List<DetectedObject>,
        frameWidth: Float,
        frameHeight: Float,
        enableVisionFallback: Boolean,
        allowNetworkLookup: Boolean,
        precomputedTextLines: List<String>? = null,
        precomputedOcrCandidates: List<OcrCandidate>? = null
    ): MlFrameAnalysis {
        val textLines = precomputedTextLines ?: visionText?.textBlocks
            ?.flatMap { block -> block.lines.map { it.text } }
            .orEmpty()

        val rawPreview = textLines.take(3).joinToString(" • ").take(90)
        val labelPairs = imageLabels.map { it.text to it.confidence }
        val visionTagStrings = labelPairs.take(4).map { (label, conf) ->
            "$label ${(conf * 100).toInt()}%"
        }

        val boxes = mutableListOf<DetectionOverlayBox>()
        val ocrCandidates = precomputedOcrCandidates ?: PokemonMatcher.matchAllFromTextLines(textLines)
        val ocrCandidate = ocrCandidates.firstOrNull()
        val visionCandidate = if (enableVisionFallback && labelPairs.isNotEmpty()) {
            PokemonMatcher.matchFromVisionLabels(labelPairs)
        } else {
            null
        }

        // Build normalized bounding boxes for text blocks matching the Pokémon name
        visionText?.textBlocks?.forEach { block ->
            val rect = block.boundingBox ?: return@forEach
            val isMatch = ocrCandidate != null &&
                block.text.contains(ocrCandidate.matchedToken, ignoreCase = true)
            if (isMatch || boxes.size < 4) {
                boxes.add(
                    DetectionOverlayBox(
                        normalizedRect = RectF(
                            (rect.left / frameWidth).coerceIn(0f, 1f),
                            (rect.top / frameHeight).coerceIn(0f, 1f),
                            (rect.right / frameWidth).coerceIn(0f, 1f),
                            (rect.bottom / frameHeight).coerceIn(0f, 1f)
                        ),
                        label = if (isMatch) "Found: ${ocrCandidate?.matchedToken}" else block.text.take(18),
                        isPokemonMatch = isMatch
                    )
                )
            }
        }

        // Add detected object bounding boxes
        detectedObjects.take(3).forEach { obj ->
            val rect = obj.boundingBox
            val objLabel = obj.labels.firstOrNull()?.text ?: "Subject"
            boxes.add(
                DetectionOverlayBox(
                    normalizedRect = RectF(
                        (rect.left / frameWidth).coerceIn(0f, 1f),
                        (rect.top / frameHeight).coerceIn(0f, 1f),
                        (rect.right / frameWidth).coerceIn(0f, 1f),
                        (rect.bottom / frameHeight).coerceIn(0f, 1f)
                    ),
                    label = objLabel,
                    isPokemonMatch = false
                )
            )
        }

        val resolvedMatch = when {
            ocrCandidate != null -> {
                val entry = repository.getPokemonById(ocrCandidate.pokemonId, allowNetworkLookup)
                entry?.let {
                    val method = if (visionCandidate != null) {
                        RecognitionMethod.OCR_AND_VISION
                    } else {
                        RecognitionMethod.ML_OCR
                    }
                    ScanMatch(
                        pokemon = it,
                        confidence = ocrCandidate.confidence,
                        method = method,
                        detectedText = ocrCandidate.matchedToken,
                        visionLabels = visionTagStrings,
                        detectionBoxes = boxes
                    )
                }
            }
            visionCandidate != null -> {
                val entry = repository.getPokemonById(visionCandidate.pokemonId, allowNetworkLookup)
                entry?.let {
                    ScanMatch(
                        pokemon = it,
                        confidence = visionCandidate.confidence,
                        method = RecognitionMethod.ML_VISION,
                        detectedText = null,
                        visionLabels = visionTagStrings,
                        detectionBoxes = boxes
                    )
                }
            }
            else -> null
        }

        val secondaryEntries = ocrCandidates
            .drop(1)
            .take(3)
            .mapNotNull { cand -> repository.getPokemonById(cand.pokemonId, allowNetworkLookup) }

        return MlFrameAnalysis(
            match = resolvedMatch,
            secondaryMatches = secondaryEntries,
            rawTextPreview = rawPreview,
            visionLabels = visionTagStrings,
            boxes = boxes
        )
    }

    fun close() {
        runCatching { _textRecognizer?.close() }
        runCatching { _imageLabeler?.close() }
        runCatching { _objectDetector?.close() }
        _textRecognizer = null
        _imageLabeler = null
        _objectDetector = null
    }

    companion object {
        private const val MIN_MLKIT_DIMENSION = 32
        private val EMPTY_ANALYSIS = MlFrameAnalysis(
            match = null,
            secondaryMatches = emptyList(),
            rawTextPreview = "",
            visionLabels = emptyList(),
            boxes = emptyList()
        )

        fun ensureInitialized(context: Context) {
            val appContext = context.applicationContext ?: context
            prepareNativeStorage(appContext)
            runCatching {
                MlKitContext.initializeIfNeeded(appContext)
            }
        }

        fun prepareNativeStorage(context: Context) {
            runCatching {
                val accelDir = File(context.filesDir, "com.google.mlkit.acceleration")
                if (!accelDir.exists()) {
                    accelDir.mkdirs()
                }
                val analyticsFile = File(
                    accelDir,
                    "com.google.perception.acceleration_analytics_storage_v2."
                )
                if (!analyticsFile.exists()) {
                    analyticsFile.createNewFile()
                }
            }
        }
    }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        if (cont.isActive) cont.resume(result)
    }
    addOnFailureListener { exception ->
        if (cont.isActive) cont.resumeWithException(exception)
    }
}
