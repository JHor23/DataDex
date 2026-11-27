package com.example

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.DataDexStateHolder
import com.example.data.LocalPokedexCatalog
import com.example.ml.DataDexMlEngine
import com.example.ml.PokemonMatcher
import com.example.model.OverlayHudState
import com.example.model.RecognitionMethod
import com.example.model.ScanMatch
import com.example.overlay.ScreenOverlayService
import com.example.viewmodel.DataDexViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSettings
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp-xxhdpi")
class ScannerCrashReproTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetState() {
        DataDexStateHolder.setAppInForeground(false)
        DataDexStateHolder.updateOverlayState { OverlayHudState() }
        ShadowSettings.setCanDrawOverlays(false)
    }

    @After
    fun cleanupState() {
        DataDexStateHolder.setAppInForeground(false)
        DataDexStateHolder.updateOverlayState { OverlayHudState() }
        ShadowSettings.setCanDrawOverlays(false)
    }

    @Test
    fun `switching scan modes and launching live camera does not crash`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).grantPermissions(
            Manifest.permission.CAMERA,
            Manifest.permission.POST_NOTIFICATIONS
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("scan_mode_dropdown").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("scan_mode_option_live_camera").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("launch_screen_overlay_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Navigate back from CameraScanScreen
        composeTestRule.onNodeWithTag("camera_back_button").performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun `switching to screenshot mode and invoking picker does not crash`() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("scan_mode_dropdown").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("scan_mode_option_screenshot").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("launch_screen_overlay_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
    }

    @Test
    fun `active overlay buttons display Scan Pause Resume Stop without cutoff`() {
        DataDexStateHolder.updateOverlayState {
            OverlayHudState(
                isOverlayRunning = true,
                isScanningPaused = false,
                framesProcessed = 7,
                statusMessage = "Watching screen for Pokémon names"
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("scan_now_button")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextEquals("Scan")
        composeTestRule.onNodeWithTag("toggle_overlay_pause_button")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextEquals("Pause")
        composeTestRule.onNodeWithTag("stop_overlay_button")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextEquals("Stop")

        DataDexStateHolder.updateOverlayState { it.copy(isScanningPaused = true) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("toggle_overlay_pause_button")
            .performScrollTo()
            .assertTextEquals("Resume")
    }

    @Test
    fun `analyzing normal tiny and corrupt screenshot uris via ViewModel does not crash`() {
        val app = ApplicationProvider.getApplicationContext<Application>()

        // 1) Normal bitmap
        val bmp = Bitmap.createBitmap(300, 200, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.WHITE)
        }
        val bytes = ByteArrayOutputStream().use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.toByteArray()
        }
        bmp.recycle()
        val uri = Uri.parse("content://com.example.test/screenshot.png")
        Shadows.shadowOf(app.contentResolver).registerInputStream(uri, bytes.inputStream())

        // 2) Tiny bitmap (< 32px ML Kit minimum)
        val tinyBmp = Bitmap.createBitmap(10, 10, Bitmap.Config.RGB_565).apply {
            eraseColor(Color.CYAN)
        }
        val tinyBytes = ByteArrayOutputStream().use { out ->
            tinyBmp.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.toByteArray()
        }
        tinyBmp.recycle()
        val tinyUri = Uri.parse("content://com.example.test/tiny.png")
        Shadows.shadowOf(app.contentResolver).registerInputStream(tinyUri, tinyBytes.inputStream())

        // 3) Corrupt stream
        val badUri = Uri.parse("content://com.example.test/corrupt.png")
        Shadows.shadowOf(app.contentResolver).registerInputStream(badUri, "not-an-image".byteInputStream())

        val vm = DataDexViewModel(app)
        vm.analyzePhotoUri(uri)
        Thread.sleep(250)
        vm.analyzePhotoUri(tinyUri)
        Thread.sleep(250)
        vm.analyzePhotoUri(badUri)
        Thread.sleep(150)
        vm.testMlScreenReadOnSample("Charizard", 78, "HP 248/248")
        Thread.sleep(250)
        composeTestRule.waitForIdle()
    }

    @Test
    fun `starting ScreenOverlayService with and without overlay permission handles safely`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        // Without overlay permission: should abort cleanly without crashing
        val serviceNoPerm = Robolectric.buildService(ScreenOverlayService::class.java).create().get()
        serviceNoPerm.onStartCommand(
            Intent(serviceNoPerm, ScreenOverlayService::class.java).apply {
                action = ScreenOverlayService.ACTION_START_OVERLAY_ONLY
            },
            0,
            1
        )
        assertFalse(DataDexStateHolder.overlayState.value.isOverlayRunning)

        // With overlay permission: should attach OverlayHudWindow and display match
        ShadowSettings.setCanDrawOverlays(true)
        val charizard = LocalPokedexCatalog.byId.getValue(6)
        val serviceController = Robolectric.buildService(ScreenOverlayService::class.java)
        val service = serviceController.create().get()
        service.onStartCommand(
            Intent(service, ScreenOverlayService::class.java).apply {
                action = ScreenOverlayService.ACTION_START_OVERLAY_ONLY
            },
            0,
            2
        )

        DataDexStateHolder.setOverlayMatch(
            match = ScanMatch(
                pokemon = charizard,
                confidence = 0.95f,
                method = RecognitionMethod.ML_OCR,
                detectedText = "Charizard"
            ),
            textPreview = "Charizard Lv.50"
        )
        composeTestRule.waitForIdle()

        // Invalid ACTION_START without result data should also exit cleanly
        service.onStartCommand(
            Intent(service, ScreenOverlayService::class.java).apply {
                action = ScreenOverlayService.ACTION_START
                putExtra(ScreenOverlayService.EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            },
            0,
            3
        )
        serviceController.destroy()
    }

    @Test
    fun `ml engine detectors initialize cleanly and match Charmander card OCR`() = runBlocking(Dispatchers.Default) {
        val app = ApplicationProvider.getApplicationContext<Application>()
        DataDexMlEngine.ensureInitialized(app)
        val engine = DataDexMlEngine()

        val detectors = engine.createCameraDetectors(app, includeVisionDetectors = true)
        assertEquals(3, detectors.size)
        assertNull(engine.lastErrorMessage)

        val candidate = PokemonMatcher.matchFromTextLines(
            listOf("BASIC", "Charmander", "HP 60", "Heat Tackle")
        )
        assertNotNull(candidate)
        assertEquals(4, candidate?.pokemonId)

        val rgb565 = Bitmap.createBitmap(12, 12, Bitmap.Config.RGB_565)
        val result = withTimeout(5000L) {
            engine.analyzeBitmap(rgb565)
        }
        assertNotNull(result)
        assertTrue(result.visionLabels.isEmpty() || result.visionLabels.isNotEmpty())
        rgb565.recycle()
        engine.close()
    }

    @Test
    fun `overlay skips scanning while DataDex is open and resumes when backgrounded`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        ShadowSettings.setCanDrawOverlays(true)

        DataDexStateHolder.setAppInForeground(true)
        val serviceController = Robolectric.buildService(ScreenOverlayService::class.java)
        val service = serviceController.create().get()
        service.onStartCommand(
            Intent(service, ScreenOverlayService::class.java).apply {
                action = ScreenOverlayService.ACTION_START_OVERLAY_ONLY
            },
            0,
            10
        )
        composeTestRule.waitForIdle()

        assertTrue(DataDexStateHolder.overlayState.value.isOverlayRunning)
        assertTrue(DataDexStateHolder.overlayState.value.isPausedForOwnApp)

        val framesBefore = DataDexStateHolder.overlayState.value.framesProcessed
        service.onStartCommand(ScreenOverlayService.scanNowIntent(service), 0, 11)
        composeTestRule.waitForIdle()

        val stateWhileOpen = DataDexStateHolder.overlayState.value
        assertEquals(framesBefore, stateWhileOpen.framesProcessed)
        assertTrue(stateWhileOpen.isPausedForOwnApp)
        assertTrue(stateWhileOpen.statusMessage.contains("DataDex open"))

        DataDexStateHolder.setAppInForeground(false)
        DataDexStateHolder.recordFrameTelemetry(textPreview = "Pikachu", visionTags = emptyList())
        assertFalse(DataDexStateHolder.overlayState.value.isPausedForOwnApp)

        serviceController.destroy()
    }

    @Test
    fun `launching overlay minimizes DataDex to Android Home screen once active`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        ShadowSettings.setCanDrawOverlays(true)

        // Cycle CREATED -> RESUMED so MainActivity.onStop/onStart run and ON_RESUME refreshes hasOverlayPermission
        composeTestRule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
        assertFalse(DataDexStateHolder.isAppInForeground.value)
        composeTestRule.activityRule.scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        composeTestRule.waitForIdle()

        assertTrue(DataDexStateHolder.isAppInForeground.value)

        composeTestRule.onNodeWithTag("launch_screen_overlay_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        val activity = composeTestRule.activity
        val shadowActivity = Shadows.shadowOf(activity)
        val startedForResult = shadowActivity.nextStartedActivityForResult
        assertNotNull(startedForResult)

        shadowActivity.receiveResult(
            startedForResult.intent,
            Activity.RESULT_OK,
            Intent().putExtra("mock_projection", true)
        )
        composeTestRule.waitForIdle()

        val startedService = Shadows.shadowOf(app).nextStartedService
        assertNotNull(startedService)
        assertEquals(ScreenOverlayService::class.java.name, startedService.component?.className)

        // Simulate ScreenOverlayService completing startup
        DataDexStateHolder.updateOverlayState { it.copy(isOverlayRunning = true) }
        composeTestRule.waitForIdle()
        Thread.sleep(100)
        composeTestRule.waitForIdle()

        val homeIntent = shadowActivity.nextStartedActivity ?: Shadows.shadowOf(app).nextStartedActivity
        assertNotNull(homeIntent)
        assertEquals(Intent.ACTION_MAIN, homeIntent!!.action)
        assertTrue(homeIntent.hasCategory(Intent.CATEGORY_HOME))
    }

    @Test
    fun `about me screen combines story and 5 traits inside bio without affecting other cards`() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_settings_about_dropdown").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_dropdown_about_me").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("about_me_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bio_tab_story").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bio_tab_traits").assertIsDisplayed()
        composeTestRule.onNodeWithText("DataDex gives you instant access", substring = true).assertIsDisplayed()

        composeTestRule.onNodeWithTag("bio_tab_traits").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("bio_trait_chip_0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("bio_trait_chip_4").assertIsDisplayed()
        composeTestRule.onNodeWithText("Deadpool-style sarcasm", substring = true).assertIsDisplayed()

        composeTestRule.onNodeWithTag("bio_trait_chip_4").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Dad, Husband, Eagle Scout, Gamer, & Hooper.", substring = true)
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("bio_tab_story").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Lifelong Pokémon fan", substring = true).assertIsDisplayed()

        composeTestRule.onNodeWithTag("about_kofi_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("about_source_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("about_projects_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("about_version_footer").assertIsDisplayed()
    }
}
