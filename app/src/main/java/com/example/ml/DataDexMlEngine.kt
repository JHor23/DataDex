package com.example.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.data.PokemonRepository
import com.example.model.DetectionOverlayBox
import com.example.model.PokemonEntry
import com.example.model.RecognitionMethod
import com.example.model.ScanMatch
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    val textRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    private val maskPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }

    val imageLabeler: ImageLabeler by lazy {
        ImageLabeling.getClient(
            ImageLabelerOptions.Builder()
                .setConfidenceThreshold(0.52f)
                .build()
        )
    }

    val objectDetector: ObjectDetector by lazy {
        ObjectDetection.getClient(
            ObjectDetectorOptions.Builder()
                .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                .enableMultipleObjects()
                .enableClassification()
                .build()
        )
    }

    suspend fun analyzeBitmap(
        bitmap: Bitmap,
        enableVisionFallback: Boolean = true,
        allowNetworkLookup: Boolean = true,
        maskNormalizedRect: RectF? = null
    ): MlFrameAnalysis = coroutineScope {
        val scanBitmap = if (maskNormalizedRect != null && !maskNormalizedRect.isEmpty) {
            val mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(mutable)
            val w = mutable.width.toFloat()
            val h = mutable.height.toFloat()
            canvas.drawRect(
                (maskNormalizedRect.left * w).coerceIn(0f, w),
                (maskNormalizedRect.top * h).coerceIn(0f, h),
                (maskNormalizedRect.right * w).coerceIn(0f, w),
                (maskNormalizedRect.bottom * h).coerceIn(0f, h),
                maskPaint
            )
            mutable
        } else {
            bitmap
        }

        try {
            val image = InputImage.fromBitmap(scanBitmap, 0)
            val width = scanBitmap.width.coerceAtLeast(1).toFloat()
            val height = scanBitmap.height.coerceAtLeast(1).toFloat()

            val visionText = runCatching { textRecognizer.process(image).awaitResult() }.getOrNull()
            val textLines = visionText?.textBlocks
                ?.flatMap { block -> block.lines.map { it.text } }
                .orEmpty()
            val ocrCandidates = PokemonMatcher.matchAllFromTextLines(textLines)

            val imageLabels: List<ImageLabel>
            val detectedObjects: List<DetectedObject>
            if (ocrCandidates.isEmpty() && enableVisionFallback) {
                val labelsDeferred = async {
                    runCatching { imageLabeler.process(image).awaitResult() }.getOrElse { emptyList() }
                }
                val objectsDeferred = async {
                    runCatching { objectDetector.process(image).awaitResult() }.getOrElse { emptyList() }
                }
                imageLabels = labelsDeferred.await()
                detectedObjects = objectsDeferred.await()
            } else {
                imageLabels = emptyList()
                detectedObjects = emptyList()
            }

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
        } finally {
            if (scanBitmap !== bitmap) {
                scanBitmap.recycle()
            }
        }
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
        runCatching { textRecognizer.close() }
        runCatching { imageLabeler.close() }
        runCatching { objectDetector.close() }
    }

    companion object {
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
