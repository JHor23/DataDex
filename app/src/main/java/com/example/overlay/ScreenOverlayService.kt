package com.example.overlay

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.core.content.IntentCompat
import com.example.MainActivity
import com.example.R
import com.example.audio.ScouterSoundEngine
import com.example.data.DataDexPreferences
import com.example.data.DataDexStateHolder
import com.example.data.UserPreferencesRepository
import com.example.ml.DataDexMlEngine
import com.example.model.RecognitionMethod
import com.example.model.ScanMatch
import com.example.voice.DataDexVoiceSpeaker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

class ScreenOverlayService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mlEngine = DataDexMlEngine()
    private lateinit var prefsRepository: UserPreferencesRepository
    private val voiceSpeaker by lazy { DataDexVoiceSpeaker.getInstance(applicationContext) }
    private val soundEngine by lazy { ScouterSoundEngine.getInstance(applicationContext) }

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var overlayHudWindow: OverlayHudWindow? = null
    private var scanJob: Job? = null
    private val scanMutex = Mutex()
    private var reusableRawBitmap: Bitmap? = null
    private var reusableCroppedBitmap: Bitmap? = null
    private var lastCapturedBitmap: Bitmap? = null

    @Volatile
    private var latestPrefs: DataDexPreferences = DataDexPreferences()

    private var screenWidth = 1080
    private var screenHeight = 1920
    private var captureWidth = 720
    private var captureHeight = 1280
    private var captureDensity = 320

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            stopOverlayAndSelf()
        }

        override fun onCapturedContentVisibilityChanged(isVisible: Boolean) {
            DataDexStateHolder.updateOverlayState {
                it.copy(
                    isTargetContentVisible = isVisible,
                    statusMessage = if (isVisible) {
                        "Actively reading screen"
                    } else {
                        "Shared window hidden • Switch to target app"
                    }
                )
            }
        }

        override fun onCapturedContentResize(width: Int, height: Int) {
            if (width <= 0 || height <= 0) return
            screenWidth = width
            screenHeight = height
            val scale = (640f / width.coerceAtLeast(1)).coerceAtMost(1f)
            val newW = (width * scale).toInt().coerceAtLeast(320)
            val newH = (height * scale).toInt().coerceAtLeast(320)
            if (newW != captureWidth || newH != captureHeight) {
                captureWidth = newW
                captureHeight = newH
                reconfigureVirtualDisplay()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        DataDexMlEngine.prepareNativeStorage(applicationContext)
        prefsRepository = UserPreferencesRepository(applicationContext)
        serviceScope.launch {
            prefsRepository.preferencesFlow.collect { prefs ->
                latestPrefs = prefs
                overlayHudWindow?.updateOpacity(prefs.overlayOpacity)
            }
        }
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopOverlayAndSelf()
                return START_NOT_STICKY
            }

            ACTION_SCAN_NOW -> {
                serviceScope.launch {
                    soundEngine.playScanPulse(latestPrefs)
                    scanSingleCycle(forceUsingLastFrame = true)
                }
                return START_NOT_STICKY
            }

            ACTION_CLEAR_MATCH -> {
                soundEngine.resetDetectionLock()
                DataDexStateHolder.clearCurrentMatch()
                soundEngine.playOverlayUpdated(latestPrefs)
                return START_NOT_STICKY
            }

            ACTION_START_OVERLAY_ONLY -> {
                val notification = buildNotification()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIF_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                    )
                } else {
                    startForeground(NOTIF_ID, notification)
                }
                showOverlayWindow()
                return START_NOT_STICKY
            }

            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                val resultData = IntentCompat.getParcelableExtra(
                    intent,
                    EXTRA_RESULT_DATA,
                    Intent::class.java
                )
                if (resultCode != Activity.RESULT_OK || resultData == null) {
                    stopSelf()
                    return START_NOT_STICKY
                }

                val notification = buildNotification()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIF_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                    )
                } else {
                    startForeground(NOTIF_ID, notification)
                }

                startProjectionAndOverlay(resultCode, resultData)
            }
        }
        return START_NOT_STICKY
    }

    private fun startProjectionAndOverlay(resultCode: Int, resultData: Intent) {
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = manager.getMediaProjection(resultCode, resultData) ?: run {
            stopSelf()
            return
        }
        mediaProjection = projection
        projection.registerCallback(projectionCallback, Handler(Looper.getMainLooper()))

        initCaptureDimensions()
        createVirtualDisplay()
        showOverlayWindow()
        startScanningLoop()
    }

    private fun initCaptureDimensions() {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = wm.maximumWindowMetrics.bounds
            screenWidth = bounds.width().coerceAtLeast(360)
            screenHeight = bounds.height().coerceAtLeast(640)
            val scale = (640f / screenWidth).coerceAtMost(1f)
            captureWidth = (screenWidth * scale).toInt().coerceAtLeast(360)
            captureHeight = (screenHeight * scale).toInt().coerceAtLeast(640)
            captureDensity = resources.displayMetrics.densityDpi
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getMetrics(metrics)
            screenWidth = metrics.widthPixels.coerceAtLeast(360)
            screenHeight = metrics.heightPixels.coerceAtLeast(640)
            val scale = (640f / screenWidth).coerceAtMost(1f)
            captureWidth = (screenWidth * scale).toInt().coerceAtLeast(360)
            captureHeight = (screenHeight * scale).toInt().coerceAtLeast(640)
            captureDensity = metrics.densityDpi
        }
    }

    @SuppressLint("WrongConstant")
    private fun createVirtualDisplay() {
        imageReader?.close()
        imageReader = ImageReader.newInstance(
            captureWidth,
            captureHeight,
            PixelFormat.RGBA_8888,
            2
        )
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "DataDexCapture",
            captureWidth,
            captureHeight,
            captureDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )
    }

    @SuppressLint("WrongConstant")
    private fun reconfigureVirtualDisplay() {
        imageReader?.close()
        imageReader = ImageReader.newInstance(
            captureWidth,
            captureHeight,
            PixelFormat.RGBA_8888,
            2
        )
        virtualDisplay?.resize(captureWidth, captureHeight, captureDensity)
        virtualDisplay?.surface = imageReader?.surface
    }

    private fun showOverlayWindow() {
        val prefs = latestPrefs
        DataDexStateHolder.updateOverlayState {
            it.copy(
                isOverlayRunning = true,
                isScanningPaused = false,
                isTargetContentVisible = true,
                statusMessage = "Overlay active • Watching screen"
            )
        }
        if (overlayHudWindow == null) {
            overlayHudWindow = OverlayHudWindow(
                context = applicationContext,
                onTogglePause = {
                    val willPause = !DataDexStateHolder.overlayState.value.isScanningPaused
                    if (willPause) soundEngine.resetDetectionLock()
                    DataDexStateHolder.updateOverlayState {
                        it.copy(
                            isScanningPaused = !it.isScanningPaused,
                            statusMessage = if (!it.isScanningPaused) "Scanner paused" else "Watching screen"
                        )
                    }
                    soundEngine.playOverlayUpdated(latestPrefs)
                },
                onToggleCollapse = {
                    DataDexStateHolder.updateOverlayState {
                        it.copy(isCollapsed = !it.isCollapsed)
                    }
                    soundEngine.playOverlayUpdated(latestPrefs)
                },
                onToggleAbilities = {
                    DataDexStateHolder.toggleOverlayAbilities()
                    soundEngine.playOverlayUpdated(latestPrefs)
                },
                onScanNow = {
                    serviceScope.launch {
                        soundEngine.playScanPulse(latestPrefs)
                        scanSingleCycle(forceUsingLastFrame = true)
                    }
                },
                onClearMatch = {
                    soundEngine.resetDetectionLock()
                    DataDexStateHolder.clearCurrentMatch()
                    soundEngine.playOverlayUpdated(latestPrefs)
                },
                onSelectSecondaryPokemon = { entry ->
                    val pinned = ScanMatch(
                        pokemon = entry,
                        confidence = 0.96f,
                        method = RecognitionMethod.ML_OCR,
                        detectedText = entry.name
                    )
                    val existing = DataDexStateHolder.overlayState.value
                    val updatedSecondary = buildList {
                        existing.currentMatch?.pokemon?.let { if (it.id != entry.id) add(it) }
                        addAll(existing.secondaryMatches.filter { it.id != entry.id })
                    }.distinctBy { it.id }.take(3)
                    DataDexStateHolder.setOverlayMatch(
                        match = pinned,
                        textPreview = existing.lastScannedTextPreview,
                        visionTags = existing.lastVisionTags,
                        secondaryMatches = updatedSecondary
                    )
                    val currentPrefs = latestPrefs
                    soundEngine.playOverlayUpdated(currentPrefs, forcePlay = true)
                    voiceSpeaker.announcePokemon(
                        pokemon = pinned.pokemon,
                        prefs = currentPrefs,
                        forceSpeak = currentPrefs.voiceOutputEnabled
                    )
                },
                onCloseOverlay = {
                    stopOverlayAndSelf()
                }
            )
        }
        overlayHudWindow?.show(prefs.overlayOpacity)
        soundEngine.playOverlayUpdated(prefs)
    }

    private fun startScanningLoop() {
        scanJob?.cancel()
        scanJob = serviceScope.launch {
            // Brief initial delay so VirtualDisplay renders first frame
            delay(250L)
            while (isActive) {
                val prefs = latestPrefs
                val state = DataDexStateHolder.overlayState.value
                if (!state.isScanningPaused) {
                    scanSingleCycle(forceUsingLastFrame = false)
                }
                delay(prefs.scanIntervalMs.toLong())
            }
        }
    }

    private suspend fun scanSingleCycle(forceUsingLastFrame: Boolean) = scanMutex.withLock {
        val prefs = latestPrefs
        DataDexStateHolder.setActivelyReading(true)

        val freshBitmap = withContext(Dispatchers.Default) { acquireScreenBitmap() }
        val bitmapToAnalyze = if (freshBitmap != null) {
            lastCapturedBitmap = freshBitmap
            freshBitmap
        } else if (forceUsingLastFrame) {
            lastCapturedBitmap?.takeIf { !it.isRecycled }
        } else {
            null
        }

        if (bitmapToAnalyze == null) {
            // Screen hasn't emitted a new frame since previous scan; keep live heartbeat ticking
            val current = DataDexStateHolder.overlayState.value
            DataDexStateHolder.recordFrameTelemetry(
                textPreview = current.lastScannedTextPreview,
                visionTags = current.lastVisionTags,
                statusOverride = if (!current.isTargetContentVisible) {
                    "Target window hidden • Switch to app"
                } else {
                    "Screen steady • Watching for updates"
                }
            )
            return@withLock
        }

        val maskRect = overlayHudWindow?.getNormalizedOverlayRect(screenWidth, screenHeight)
        val analysis = withContext(Dispatchers.Default) {
            mlEngine.analyzeBitmap(
                bitmap = bitmapToAnalyze,
                enableVisionFallback = prefs.enableVisionFallback,
                allowNetworkLookup = prefs.enablePokeApiLiveFetch,
                maskNormalizedRect = maskRect
            )
        }
        val previousState = DataDexStateHolder.overlayState.value
        val previousPokemonId = previousState.currentMatch?.pokemon?.id
        val previousSecondaryIds = previousState.secondaryMatches.map { it.id }

        val match = analysis.match
        if (match != null) {
            DataDexStateHolder.setOverlayMatch(
                match = match,
                textPreview = analysis.rawTextPreview,
                visionTags = analysis.visionLabels,
                secondaryMatches = analysis.secondaryMatches
            )
            val newSecondaryIds = analysis.secondaryMatches.map { it.id }
            if (previousPokemonId != match.pokemon.id || forceUsingLastFrame) {
                soundEngine.playPokemonDetected(
                    pokemonId = match.pokemon.id,
                    prefs = prefs,
                    forcePlay = forceUsingLastFrame
                )
            } else if (previousSecondaryIds != newSecondaryIds) {
                soundEngine.playOverlayUpdated(prefs)
            }
            voiceSpeaker.announcePokemon(match.pokemon, prefs)
            prefsRepository.recordScan(match)
        } else {
            DataDexStateHolder.recordFrameTelemetry(
                textPreview = analysis.rawTextPreview,
                visionTags = analysis.visionLabels
            )
        }
    }

    private fun acquireScreenBitmap(): Bitmap? {
        val reader = imageReader ?: return null
        val image = runCatching { reader.acquireLatestImage() }.getOrNull() ?: return null
        return try {
            val plane = image.planes.firstOrNull() ?: return null
            val buffer: ByteBuffer = plane.buffer
            buffer.rewind()
            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride
            val rowPadding = rowStride - pixelStride * image.width
            val bitmapWidth = image.width + rowPadding / pixelStride
            val rawBitmap = obtainReusableBitmap(
                existing = reusableRawBitmap,
                width = bitmapWidth,
                height = image.height
            ).also { reusableRawBitmap = it }
            rawBitmap.copyPixelsFromBuffer(buffer)
            if (bitmapWidth != image.width) {
                val cropped = obtainReusableBitmap(
                    existing = reusableCroppedBitmap,
                    width = image.width,
                    height = image.height
                ).also { reusableCroppedBitmap = it }
                val rect = Rect(0, 0, image.width, image.height)
                Canvas(cropped).drawBitmap(rawBitmap, rect, rect, null)
                cropped
            } else {
                rawBitmap
            }
        } catch (_: Exception) {
            null
        } finally {
            image.close()
        }
    }

    private fun obtainReusableBitmap(existing: Bitmap?, width: Int, height: Int): Bitmap {
        if (existing != null && !existing.isRecycled && existing.width == width && existing.height == height) {
            return existing
        }
        existing?.recycle()
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }

    private fun recycleCaptureBitmaps() {
        lastCapturedBitmap = null
        reusableCroppedBitmap?.recycle()
        reusableCroppedBitmap = null
        reusableRawBitmap?.recycle()
        reusableRawBitmap = null
    }

    private fun stopOverlayAndSelf() {
        scanJob?.cancel()
        scanJob = null
        soundEngine.resetDetectionLock()
        recycleCaptureBitmaps()
        overlayHudWindow?.dismiss()
        overlayHudWindow = null
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        mediaProjection?.unregisterCallback(projectionCallback)
        mediaProjection?.stop()
        mediaProjection = null
        DataDexStateHolder.updateOverlayState {
            it.copy(
                isOverlayRunning = false,
                isScanningPaused = false,
                isActivelyReading = false,
                statusMessage = "Overlay stopped"
            )
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        scanJob?.cancel()
        recycleCaptureBitmaps()
        overlayHudWindow?.dismiss()
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.unregisterCallback(projectionCallback)
        mediaProjection?.stop()
        mlEngine.close()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.overlay_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.overlay_channel_desc)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, ScreenOverlayService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.overlay_notif_title))
            .setContentText(getString(R.string.overlay_notif_body))
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .addAction(0, getString(R.string.action_stop_overlay), stopIntent)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "datadex_overlay_channel"
        private const val NOTIF_ID = 2026
        const val ACTION_START = "com.example.datadex.ACTION_START_PROJECTION_OVERLAY"
        const val ACTION_START_OVERLAY_ONLY = "com.example.datadex.ACTION_START_OVERLAY_ONLY"
        const val ACTION_SCAN_NOW = "com.example.datadex.ACTION_SCAN_NOW"
        const val ACTION_CLEAR_MATCH = "com.example.datadex.ACTION_CLEAR_MATCH"
        const val ACTION_STOP = "com.example.datadex.ACTION_STOP_OVERLAY"
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        fun startIntent(context: Context, resultCode: Int, data: Intent): Intent =
            Intent(context, ScreenOverlayService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, data)
            }

        fun scanNowIntent(context: Context): Intent =
            Intent(context, ScreenOverlayService::class.java).apply {
                action = ACTION_SCAN_NOW
            }

        fun clearMatchIntent(context: Context): Intent =
            Intent(context, ScreenOverlayService::class.java).apply {
                action = ACTION_CLEAR_MATCH
            }

        fun stopIntent(context: Context): Intent =
            Intent(context, ScreenOverlayService::class.java).apply {
                action = ACTION_STOP
            }
    }
}
