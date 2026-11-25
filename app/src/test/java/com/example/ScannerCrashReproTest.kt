package com.example

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.DataDexStateHolder
import com.example.data.LocalPokedexCatalog
import com.example.ml.DataDexMlEngine
import com.example.model.OverlayHudState
import com.example.model.RecognitionMethod
import com.example.model.ScanMatch
import com.example.overlay.ScreenOverlayService
import com.example.viewmodel.DataDexViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
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
@Config(sdk = [36])
class ScannerCrashReproTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetState() {
        DataDexStateHolder.updateOverlayState { OverlayHudState() }
        ShadowSettings.setCanDrawOverlays(false)
    }

    @After
    fun cleanupState() {
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
    fun `ml engine analyzeBitmap handles tiny and non-argb bitmaps safely`() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<Application>()
        DataDexMlEngine.ensureInitialized(app)
        val engine = DataDexMlEngine()
        val rgb565 = Bitmap.createBitmap(12, 12, Bitmap.Config.RGB_565)
        val result = engine.analyzeBitmap(rgb565)
        assertNotNull(result)
        rgb565.recycle()
        engine.close()
    }
}
