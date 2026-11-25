package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CatchingPokemon
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CatchingPokemon
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.overlay.ScreenOverlayService
import com.example.ui.components.CompareActionPill
import com.example.ui.components.PokemonCompareModal
import com.example.ui.components.PokemonTacticalBreakdown
import com.example.ui.screens.AboutMeScreen
import com.example.ui.screens.CameraScanScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.PokedexScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DataDexTheme
import com.example.ui.theme.DexOutline
import com.example.ui.theme.DexSurface
import com.example.ui.theme.DexSurfaceElevated
import com.example.ui.theme.DexVoid
import com.example.ui.theme.ScannerRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.DataDexViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable object ScannerRoute
@Serializable object PokedexRoute
@Serializable object HistoryRoute
@Serializable object SettingsRoute
@Serializable object AboutMeRoute
@Serializable object CameraScanRoute

private enum class ScannerHubTab {
    SCANNER,
    HISTORY
}

private enum class SettingsHubTab {
    SETTINGS,
    ABOUT_ME
}

private data class TopLevelDestination(
    val route: Any,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val centerTopLevelDestinations = listOf(
    TopLevelDestination(
        route = PokedexRoute,
        labelRes = R.string.nav_pokedex,
        selectedIcon = Icons.Filled.CatchingPokemon,
        unselectedIcon = Icons.Outlined.CatchingPokemon,
        testTag = "nav_pokedex"
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DataDexTheme(darkTheme = true) {
                DataDexRootApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataDexRootApp(
    viewModel: DataDexViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val scannerUi by viewModel.scannerUi.collectAsStateWithLifecycle()
    val overlayState by viewModel.overlayState.collectAsStateWithLifecycle()
    val pokedexUi by viewModel.pokedexUi.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val scanHistory by viewModel.scanHistory.collectAsStateWithLifecycle()
    val availableVoices by viewModel.availableVoices.collectAsStateWithLifecycle()
    val isVoiceReady by viewModel.isVoiceReady.collectAsStateWithLifecycle()
    val compareUi by viewModel.compareUi.collectAsStateWithLifecycle()

    var hasOverlayPermission by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }

    // Refresh overlay permission whenever user returns from system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val overlaySettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        hasOverlayPermission = Settings.canDrawOverlays(context)
    }

    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val serviceIntent = ScreenOverlayService.startIntent(
                context = context,
                resultCode = result.resultCode,
                data = result.data!!
            )
            ContextCompat.startForegroundService(context, serviceIntent)
            scope.launch {
                snackbarHostState.showSnackbar("DataDex Screen Overlay launched!")
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Screen capture consent is required for real-time screen reading.")
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.analyzePhotoUri(it) }
    }

    fun launchOverlayFlow() {
        if (!Settings.canDrawOverlays(context)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            overlaySettingsLauncher.launch(intent)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isCameraRoute = currentDestination?.hasRoute<CameraScanRoute>() == true
    val isOnScanner = currentDestination?.hasRoute<ScannerRoute>() == true
    val isOnHistory = currentDestination?.hasRoute<HistoryRoute>() == true
    val isHubSelected = isOnScanner || isOnHistory

    val isOnSettings = currentDestination?.hasRoute<SettingsRoute>() == true
    val isOnAboutMe = currentDestination?.hasRoute<AboutMeRoute>() == true
    val isSettingsSelected = isOnSettings || isOnAboutMe

    var lastHubTab by remember { mutableStateOf(ScannerHubTab.SCANNER) }
    if (isOnHistory) {
        lastHubTab = ScannerHubTab.HISTORY
    } else if (isOnScanner) {
        lastHubTab = ScannerHubTab.SCANNER
    }

    var lastSettingsTab by remember { mutableStateOf(SettingsHubTab.SETTINGS) }
    if (isOnAboutMe) {
        lastSettingsTab = SettingsHubTab.ABOUT_ME
    } else if (isOnSettings) {
        lastSettingsTab = SettingsHubTab.SETTINGS
    }

    var navHubMenuExpanded by remember { mutableStateOf(false) }
    var navSettingsMenuExpanded by remember { mutableStateOf(false) }

    fun navigateTopLevel(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val hubLabelRes = if (lastHubTab == ScannerHubTab.HISTORY) R.string.nav_history else R.string.nav_scanner
    val hubSelectedIcon = if (lastHubTab == ScannerHubTab.HISTORY) Icons.Filled.History else Icons.Filled.Radar
    val hubUnselectedIcon = if (lastHubTab == ScannerHubTab.HISTORY) Icons.Outlined.History else Icons.Outlined.Radar

    val settingsLabelRes = if (lastSettingsTab == SettingsHubTab.ABOUT_ME) R.string.nav_about_me else R.string.nav_settings
    val settingsSelectedIcon = if (lastSettingsTab == SettingsHubTab.ABOUT_ME) Icons.Filled.Person else Icons.Filled.Settings
    val settingsUnselectedIcon = if (lastSettingsTab == SettingsHubTab.ABOUT_ME) Icons.Outlined.Person else Icons.Outlined.Settings

    @Composable
    fun ScannerHistoryDropdownMenu() {
        DropdownMenu(
            expanded = navHubMenuExpanded,
            onDismissRequest = { navHubMenuExpanded = false },
            containerColor = DexSurfaceElevated,
            border = BorderStroke(1.dp, DexOutline),
            shape = RoundedCornerShape(14.dp)
        ) {
            DropdownMenuItem(
                text = {
                    Column {
                        Text(
                            text = stringResource(R.string.nav_scanner),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isOnScanner) CyberCyan else TextPrimary
                        )
                        Text(
                            text = "Overlay, Camera, & Photo",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            maxLines = 1
                        )
                    }
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Radar,
                        contentDescription = null,
                        tint = if (isOnScanner) CyberCyan else TextSecondary
                    )
                },
                trailingIcon = {
                    if (lastHubTab == ScannerHubTab.SCANNER) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Active",
                            tint = CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                onClick = {
                    lastHubTab = ScannerHubTab.SCANNER
                    navHubMenuExpanded = false
                    navigateTopLevel(ScannerRoute)
                },
                modifier = Modifier.testTag("nav_dropdown_scanner")
            )
            HorizontalDivider(color = DexOutline.copy(alpha = 0.55f))
            DropdownMenuItem(
                text = {
                    Column {
                        Text(
                            text = stringResource(R.string.nav_history),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isOnHistory) CyberCyan else TextPrimary
                        )
                        Text(
                            text = "${scanHistory.size} recent scans",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null,
                        tint = if (isOnHistory) CyberCyan else TextSecondary
                    )
                },
                trailingIcon = {
                    if (lastHubTab == ScannerHubTab.HISTORY) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Active",
                            tint = CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                onClick = {
                    lastHubTab = ScannerHubTab.HISTORY
                    navHubMenuExpanded = false
                    navigateTopLevel(HistoryRoute)
                },
                modifier = Modifier.testTag("nav_dropdown_history")
            )
        }
    }

    @Composable
    fun SettingsAboutDropdownMenu() {
        DropdownMenu(
            expanded = navSettingsMenuExpanded,
            onDismissRequest = { navSettingsMenuExpanded = false },
            containerColor = DexSurfaceElevated,
            border = BorderStroke(1.dp, DexOutline),
            shape = RoundedCornerShape(14.dp)
        ) {
            DropdownMenuItem(
                text = {
                    Column {
                        Text(
                            text = stringResource(R.string.nav_settings),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isOnSettings) CyberCyan else TextPrimary
                        )
                        Text(
                            text = "Audio, voice, & scanner",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            maxLines = 1
                        )
                    }
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = null,
                        tint = if (isOnSettings) CyberCyan else TextSecondary
                    )
                },
                trailingIcon = {
                    if (lastSettingsTab == SettingsHubTab.SETTINGS) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Active",
                            tint = CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                onClick = {
                    lastSettingsTab = SettingsHubTab.SETTINGS
                    navSettingsMenuExpanded = false
                    navigateTopLevel(SettingsRoute)
                },
                modifier = Modifier.testTag("nav_dropdown_settings")
            )
            HorizontalDivider(color = DexOutline.copy(alpha = 0.55f))
            DropdownMenuItem(
                text = {
                    Column {
                        Text(
                            text = stringResource(R.string.nav_about_me),
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isOnAboutMe) CyberCyan else TextPrimary
                        )
                        Text(
                            text = "Creator profile & bio",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = if (isOnAboutMe) CyberCyan else TextSecondary
                    )
                },
                trailingIcon = {
                    if (lastSettingsTab == SettingsHubTab.ABOUT_ME) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Active",
                            tint = CyberCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                onClick = {
                    lastSettingsTab = SettingsHubTab.ABOUT_ME
                    navSettingsMenuExpanded = false
                    navigateTopLevel(AboutMeRoute)
                },
                modifier = Modifier.testTag("nav_dropdown_about_me")
            )
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp && !isCameraRoute

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = DexVoid,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!isCameraRoute && !useNavRail) {
                    NavigationBar(
                        containerColor = DexSurface,
                        tonalElevation = 4.dp
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                NavigationBarItem(
                                    selected = isHubSelected,
                                    onClick = { navHubMenuExpanded = true },
                                    icon = {
                                        Icon(
                                            imageVector = if (isHubSelected) hubSelectedIcon else hubUnselectedIcon,
                                            contentDescription = "Scanner or History menu"
                                        )
                                    },
                                    label = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(stringResource(hubLabelRes), maxLines = 1)
                                            Icon(
                                                imageVector = Icons.Filled.ArrowDropDown,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = CyberCyan,
                                        selectedTextColor = CyberCyan,
                                        indicatorColor = CyberCyan.copy(alpha = 0.16f),
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    ),
                                    modifier = Modifier.testTag("nav_scanner_history_dropdown")
                                )
                            }
                            ScannerHistoryDropdownMenu()
                        }

                        centerTopLevelDestinations.forEach { dest ->
                            val selected = currentDestination?.hasRoute(dest.route::class) == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyberCyan,
                                    selectedTextColor = CyberCyan,
                                    indicatorColor = CyberCyan.copy(alpha = 0.16f),
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                NavigationBarItem(
                                    selected = isSettingsSelected,
                                    onClick = { navSettingsMenuExpanded = true },
                                    icon = {
                                        Icon(
                                            imageVector = if (isSettingsSelected) settingsSelectedIcon else settingsUnselectedIcon,
                                            contentDescription = "Settings or About Me menu"
                                        )
                                    },
                                    label = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(stringResource(settingsLabelRes), maxLines = 1)
                                            Icon(
                                                imageVector = Icons.Filled.ArrowDropDown,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = CyberCyan,
                                        selectedTextColor = CyberCyan,
                                        indicatorColor = CyberCyan.copy(alpha = 0.16f),
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary
                                    ),
                                    modifier = Modifier.testTag("nav_settings_about_dropdown")
                                )
                            }
                            SettingsAboutDropdownMenu()
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = DexSurface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(Modifier.height(16.dp))
                        Box {
                            NavigationRailItem(
                                selected = isHubSelected,
                                onClick = { navHubMenuExpanded = true },
                                icon = {
                                    Icon(
                                        imageVector = if (isHubSelected) hubSelectedIcon else hubUnselectedIcon,
                                        contentDescription = "Scanner or History menu"
                                    )
                                },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(stringResource(hubLabelRes), maxLines = 1)
                                        Icon(
                                            imageVector = Icons.Filled.ArrowDropDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = CyberCyan,
                                    selectedTextColor = CyberCyan,
                                    indicatorColor = CyberCyan.copy(alpha = 0.16f),
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_scanner_history_dropdown")
                            )
                            ScannerHistoryDropdownMenu()
                        }

                        centerTopLevelDestinations.forEach { dest ->
                            val selected = currentDestination?.hasRoute(dest.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(dest.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = CyberCyan,
                                    selectedTextColor = CyberCyan,
                                    indicatorColor = CyberCyan.copy(alpha = 0.16f),
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }

                        Box {
                            NavigationRailItem(
                                selected = isSettingsSelected,
                                onClick = { navSettingsMenuExpanded = true },
                                icon = {
                                    Icon(
                                        imageVector = if (isSettingsSelected) settingsSelectedIcon else settingsUnselectedIcon,
                                        contentDescription = "Settings or About Me menu"
                                    )
                                },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(stringResource(settingsLabelRes), maxLines = 1)
                                        Icon(
                                            imageVector = Icons.Filled.ArrowDropDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = CyberCyan,
                                    selectedTextColor = CyberCyan,
                                    indicatorColor = CyberCyan.copy(alpha = 0.16f),
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_settings_about_dropdown")
                            )
                            SettingsAboutDropdownMenu()
                        }
                    }
                }

                BoxWithConstraints(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val contentModifier = if (!isCameraRoute && maxWidth > 680.dp) {
                        Modifier.widthIn(max = 680.dp).fillMaxSize()
                    } else {
                        Modifier.fillMaxSize()
                    }

                    NavHost(
                        navController = navController,
                        startDestination = ScannerRoute,
                        modifier = contentModifier
                    ) {
                        composable<ScannerRoute> {
                            ScannerScreen(
                                scannerUi = scannerUi,
                                overlayState = overlayState,
                                hasOverlayPermission = hasOverlayPermission,
                                onRequestOverlayPermission = {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    overlaySettingsLauncher.launch(intent)
                                },
                                onStartScreenOverlay = { launchOverlayFlow() },
                                onStopScreenOverlay = {
                                    context.startService(ScreenOverlayService.stopIntent(context))
                                },
                                onToggleOverlayPause = { viewModel.toggleOverlayPause() },
                                onToggleOverlayAbilities = { viewModel.toggleOverlayAbilities() },
                                onScanScreenNow = {
                                    context.startService(ScreenOverlayService.scanNowIntent(context))
                                },
                                onClearOverlayMatch = {
                                    context.startService(ScreenOverlayService.clearMatchIntent(context))
                                },
                                onLaunchCameraScan = { navController.navigate(CameraScanRoute) },
                                onLaunchPhotoPicker = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onTestSampleCard = { name, level, hpText ->
                                    viewModel.testMlScreenReadOnSample(name, level, hpText)
                                },
                                onComparePokemon = viewModel::openCompare
                            )
                        }

                        composable<PokedexRoute> {
                            PokedexScreen(
                                state = pokedexUi,
                                onQueryChange = viewModel::updateSearchQuery,
                                onToggleTypeFilter = viewModel::toggleTypeFilter,
                                onSelectPokemon = viewModel::selectPokemonForDetail
                            )
                        }

                        composable<HistoryRoute> {
                            HistoryScreen(
                                history = scanHistory,
                                onSelectHistoryPokemon = viewModel::selectPokemonById,
                                onClearHistory = viewModel::clearHistory
                            )
                        }

                        composable<SettingsRoute> {
                            SettingsScreen(
                                preferences = preferences,
                                availableVoices = availableVoices,
                                isVoiceReady = isVoiceReady,
                                onToggleVoiceOutput = viewModel::setVoiceOutputEnabled,
                                onToggleVoiceDetails = viewModel::setVoiceSpeakDetails,
                                onToggleVoiceAbilities = viewModel::setVoiceSpeakAbilities,
                                onUpdateVoiceRate = viewModel::updateVoiceSpeechRate,
                                onUpdateVoicePitch = viewModel::updateVoicePitch,
                                onUpdateVoiceCooldown = viewModel::updateVoiceCooldownSeconds,
                                onSelectVoiceName = viewModel::updateVoiceName,
                                onToggleVoiceClassic90s = viewModel::setVoiceClassic90sMode,
                                onTestVoice = viewModel::testVoiceOutput,
                                onToggleScouterSfx = viewModel::setScouterSfxEnabled,
                                onUpdateScouterSfxVolume = viewModel::updateScouterSfxVolume,
                                onToggleScouterSfxOnDetect = viewModel::setScouterSfxOnDetect,
                                onToggleScouterSfxOnOverlayUpdate = viewModel::setScouterSfxOnOverlayUpdate,
                                onTestScouterSfx = viewModel::testScouterSfx,
                                onUpdateScanInterval = viewModel::updateScanInterval,
                                onUpdateOverlayOpacity = viewModel::updateOverlayOpacity,
                                onToggleVisionFallback = viewModel::setVisionFallbackEnabled,
                                onTogglePokeApiLive = viewModel::setPokeApiLiveEnabled
                            )
                        }

                        composable<AboutMeRoute> {
                            AboutMeScreen()
                        }

                        composable<CameraScanRoute> {
                            CameraScanScreen(
                                mlEngine = viewModel.mlEngine,
                                enableVisionFallback = preferences.enableVisionFallback,
                                allowNetworkLookup = preferences.enablePokeApiLiveFetch,
                                onMatchDetected = viewModel::onCameraDetection,
                                onPinToOverlay = { entry ->
                                    viewModel.pinPokemonToOverlay(entry)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Pinned ${entry.name} to Overlay!")
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }

        // Pokémon Tactical Detail Modal Bottom Sheet
        pokedexUi.selectedDetail?.let { detail ->
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = viewModel::dismissPokemonDetail,
                sheetState = sheetState,
                containerColor = DexSurfaceElevated,
                contentColor = TextPrimary
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (pokedexUi.isLoadingDetail) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = CyberCyan
                        )
                    }
                    PokemonTacticalBreakdown(
                        pokemon = detail,
                        badgeLabel = detail.category,
                        confidence = null,
                        compact = false,
                        showAbilitiesAndNatures = true,
                        headerTrailingContent = {
                            CompareActionPill(
                                onClick = {
                                    viewModel.dismissPokemonDetail()
                                    viewModel.openCompare(detail)
                                }
                            )
                        }
                    )

                    if (detail.abilities.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ABILITIES:",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyan
                            )
                            Text(
                                text = detail.abilities.joinToString(" • "),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary
                            )
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.pinPokemonToOverlay(detail)
                            viewModel.dismissPokemonDetail()
                            scope.launch {
                                snackbarHostState.showSnackbar("Pinned ${detail.name} to Overlay!")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("pin_to_overlay_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = ScannerRed)
                    ) {
                        Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Pin ${detail.name} to Overlay")
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        if (compareUi.isVisible && compareUi.basePokemon != null) {
            PokemonCompareModal(
                state = compareUi,
                onQueryChange = viewModel::updateCompareQuery,
                onSelectChallenger = viewModel::selectCompareChallenger,
                onClearChallenger = viewModel::clearCompareChallenger,
                onSwapSides = viewModel::swapCompareSides,
                onDismiss = viewModel::closeCompare
            )
        }
    }
}
