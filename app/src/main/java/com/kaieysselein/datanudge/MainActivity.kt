package com.kaieysselein.datanudge

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.kaieysselein.datanudge.ui.theme.DataNudgeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.File
import java.security.MessageDigest

private val DataBlue = Color(0xFF0878F9)
private val DataGreen = Color(0xFF20C94B)
private val DataOrange = Color(0xFFFFA000)
private val DataRed = Color(0xFFFF5D5D)
private val ScreenDark = Color(0xFF07101D)
private val CardDark = Color(0xE6111E2F)
private val TextMuted = Color(0xFFB8C4D6)

private const val VERSION_DISPLAY = "0.2.2.0"
private const val GITHUB_URL = "https://github.com/KaiEysselein/DataNudge"
private const val GITHUB_LATEST_RELEASE_API =
    "https://api.github.com/repos/KaiEysselein/DataNudge/releases/latest"
private const val UI_PREFERENCES = "datanudge_ui_preferences"
private const val KEY_SETUP_COMPLETED = "setup_completed"

data class LaunchableApp(
    val label: String,
    val packageName: String
)

data class InstalledAppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable
)

data class ConnectionSessionSnapshot(
    val connectionStatus: String,
    val startedAtMillis: Long,
    val approximateUsedBytes: Long?
)

private val DEFAULT_MONITORED_PACKAGE_NAMES =
    setOf(
        "com.google.android.youtube",
        "com.netflix.mediaclient",
        "com.zhiliaoapp.musically",
        "com.spotify.music",
        "com.google.android.apps.youtube.music",
        "com.instagram.android",
        "com.facebook.katana",
        "com.whatsapp",
        "com.facebook.orca",
        "com.snapchat.android",
        "com.amazon.avod.thirdpartyclient",
        "com.disney.disneyplus",
        "tv.twitch.android.app",
        "com.twitter.android",
        "com.google.android.apps.tachyon",
        "us.zoom.videomeetings",
        "com.microsoft.teams",
        "com.android.chrome",
        "com.google.android.apps.maps",
        "com.google.android.apps.docs"
    )

private enum class Screen {
    HOME,
    SETUP,
    APPS,
    PERMISSIONS,
    SETTINGS,
    UPDATES,
    ABOUT
}

class MainActivity : ComponentActivity() {

    private var permissionRefreshKey by mutableIntStateOf(0)
    private var openUpdatesKey by mutableIntStateOf(0)

    companion object {
        const val EXTRA_OPEN_UPDATES =
            "open_updates"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (
            intent.getBooleanExtra(
                EXTRA_OPEN_UPDATES,
                false
            )
        ) {
            openUpdatesKey++
        }

        UpdateCheckScheduler.ensureScheduled(this)

        val disclaimerPreferences = getSharedPreferences(
            DISCLAIMER_PREFERENCES,
            android.content.Context.MODE_PRIVATE
        )
        if (
            disclaimerPreferences.getInt(DISCLAIMER_ACCEPTED_VERSION, 0) <
            DISCLAIMER_VERSION
        ) {
            window.decorView.post {
                showDataNudgeDisclaimer(this, true)
            }
        }

        setContent {
            DataNudgeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ScreenDark
                ) {
                    DataNudgeApp(
                        permissionRefreshKey = permissionRefreshKey,
                        openUpdatesKey = openUpdatesKey,
                        hideApp = { moveTaskToBack(true) }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        if (
            intent.getBooleanExtra(
                EXTRA_OPEN_UPDATES,
                false
            )
        ) {
            openUpdatesKey++
        }
    }

    override fun onResume() {
        super.onResume()
        permissionRefreshKey++
    }
}

@Composable
private fun DataNudgeApp(
    permissionRefreshKey: Int,
    openUpdatesKey: Int,
    hideApp: () -> Unit
) {
    val context = LocalContext.current
    var navigationStack by remember {
        mutableStateOf(listOf(Screen.HOME))
    }
    var menuExpanded by remember { mutableStateOf(false) }

    val currentScreen = navigationStack.last()

    fun navigateTo(screen: Screen) {
        menuExpanded = false

        if (screen == Screen.HOME) {
            navigationStack = listOf(Screen.HOME)
        } else if (screen != currentScreen) {
            navigationStack = navigationStack + screen
        }
    }

    fun navigateBack() {
        menuExpanded = false

        if (navigationStack.size > 1) {
            navigationStack = navigationStack.dropLast(1)
        }
    }

    BackHandler(enabled = navigationStack.size > 1) {
        navigateBack()
    }

    LaunchedEffect(openUpdatesKey) {
        if (openUpdatesKey > 0) {
            navigateTo(Screen.UPDATES)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            DataNudgeTopBar(
                screen = currentScreen,
                menuExpanded = menuExpanded,
                onMenuExpandedChange = { menuExpanded = it },
                onBack = ::navigateBack,
                onHome = { navigateTo(Screen.HOME) },
                onSettings = { navigateTo(Screen.SETTINGS) },
                onUpdates = { navigateTo(Screen.UPDATES) },
                onAbout = { navigateTo(Screen.ABOUT) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            ScreenDark,
                            Color(0xFF091B2C),
                            Color(0xFF07171A)
                        )
                    )
                )
        ) {
            when (currentScreen) {
                Screen.HOME -> HomeScreen(
                    permissionRefreshKey = permissionRefreshKey,
                    onOpenSetup = { navigateTo(Screen.SETUP) },
                    onOpenPermissions = {
                        navigateTo(Screen.PERMISSIONS)
                    },
                    hideApp = hideApp
                )

                Screen.SETUP -> SetupScreen(
                    permissionRefreshKey = permissionRefreshKey,
                    onOpenApps = { navigateTo(Screen.APPS) },
                    onFinished = { navigateTo(Screen.HOME) }
                )

                Screen.APPS -> AppsScreen()

                Screen.PERMISSIONS -> PermissionsScreen(
                    permissionRefreshKey = permissionRefreshKey
                )

                Screen.SETTINGS -> SettingsScreen(
                    onApps = { navigateTo(Screen.APPS) },
                    onPermissions = {
                        navigateTo(Screen.PERMISSIONS)
                    },
                    onSetup = { navigateTo(Screen.SETUP) }
                )

                Screen.UPDATES -> UpdatesScreen()

                Screen.ABOUT -> AboutScreen()
            }
        }
    }
}

@Composable
private fun DataNudgeTopBar(
    screen: Screen,
    menuExpanded: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onSettings: () -> Unit,
    onUpdates: () -> Unit,
    onAbout: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ScreenDark)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (screen != Screen.HOME) {
            Text(
                text = "\u2190",
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(
                        start = 2.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 4.dp
                    ),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = if (screen == Screen.HOME) "DataNudge" else screenTitle(screen),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp
            )

            if (screen != Screen.HOME) {
                Text(
                    text = "DataNudge",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }

        Box {
            Text(
                text = "...",
                modifier = Modifier
                    .clickable { onMenuExpandedChange(true) }
                    .padding(horizontal = 12.dp, vertical = 2.dp),
                color = Color.White,
                fontSize = 29.sp,
                fontWeight = FontWeight.Bold
            )

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { onMenuExpandedChange(false) }
            ) {
                DropdownMenuItem(
                    text = { Text("Home") },
                    onClick = {
                        onMenuExpandedChange(false)
                        onHome()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Settings") },
                    onClick = {
                        onMenuExpandedChange(false)
                        onSettings()
                    }
                )

                DropdownMenuItem(
                    text = { Text("Updates") },
                    onClick = {
                        onMenuExpandedChange(false)
                        onUpdates()
                    }
                )
                DropdownMenuItem(
                    text = { Text("About") },
                    onClick = {
                        onMenuExpandedChange(false)
                        onAbout()
                    }
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    permissionRefreshKey: Int,
    onOpenSetup: () -> Unit,
    onOpenPermissions: () -> Unit,
    hideApp: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val connectivityManager = remember {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    var connectionStatus by remember {
        mutableStateOf(getConnectionStatus(connectivityManager))
    }

    var currentTimeMillis by remember {
        mutableStateOf(System.currentTimeMillis())
    }

    var connectionSession by remember(connectionStatus) {
        mutableStateOf(
            NetworkMonitorService.getConnectionSession(
                context = context,
                currentStatus = connectionStatus
            )
        )
    }

    var monitoringEnabled by remember {
        mutableStateOf(NetworkMonitorService.isMonitoringEnabled(context))
    }

    var swipeDistance by remember {
        mutableStateOf(0f)
    }

    fun refreshHomeContent() {
        val refreshedStatus =
            getConnectionStatus(connectivityManager)

        connectionStatus = refreshedStatus
        currentTimeMillis = System.currentTimeMillis()
        connectionSession =
            NetworkMonitorService.getConnectionSession(
                context = context,
                currentStatus = refreshedStatus
            )
        monitoringEnabled =
            NetworkMonitorService.isMonitoringEnabled(context)
    }

    var appResumed by remember {
        mutableStateOf(
            lifecycleOwner.lifecycle.currentState.isAtLeast(
                Lifecycle.State.RESUMED
            )
        )
    }

    val selectedPackages =
        NetworkMonitorService.getSelectedPackages(context)

    val notificationGranted =
        notificationPermissionGranted(context)

    val usageGranted =
        remember(permissionRefreshKey) { hasUsageAccess(context) }

    val overlayGranted =
        remember(permissionRefreshKey) { Settings.canDrawOverlays(context) }

    val setupComplete =
        notificationGranted &&
            usageGranted &&
            overlayGranted &&
            selectedPackages.isNotEmpty() &&
            monitoringEnabled

    LaunchedEffect(setupComplete) {
        context
            .getSharedPreferences(UI_PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SETUP_COMPLETED, setupComplete)
            .apply()
    }

    LaunchedEffect(Unit) {
        connectionStatus = getConnectionStatus(connectivityManager)

        if (
            NetworkMonitorService.isMonitoringEnabled(context) &&
            notificationPermissionGranted(context)
        ) {
            startNetworkMonitorService(context)
            monitoringEnabled = true
        }
    }

    DisposableEffect(lifecycleOwner, connectivityManager) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        appResumed = true

                        val refreshedStatus =
                            getConnectionStatus(connectivityManager)

                        connectionStatus = refreshedStatus
                        currentTimeMillis = System.currentTimeMillis()
                        connectionSession =
                            NetworkMonitorService.getConnectionSession(
                                context = context,
                                currentStatus = refreshedStatus
                            )

                        monitoringEnabled =
                            NetworkMonitorService.isMonitoringEnabled(
                                context
                            )
                    }

                    Lifecycle.Event.ON_PAUSE,
                    Lifecycle.Event.ON_STOP -> {
                        appResumed = false
                    }

                    else -> Unit
                }
            }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(appResumed) {
        if (!appResumed) {
            return@LaunchedEffect
        }

        while (true) {
            val refreshedStatus =
                getConnectionStatus(connectivityManager)

            connectionStatus = refreshedStatus
            currentTimeMillis = System.currentTimeMillis()
            connectionSession =
                NetworkMonitorService.getConnectionSession(
                    context = context,
                    currentStatus = refreshedStatus
                )

            kotlinx.coroutines.delay(1_000L)
        }
    }

    LaunchedEffect(connectionStatus) {
        connectionSession =
            NetworkMonitorService.getConnectionSession(
                context = context,
                currentStatus = connectionStatus
            )
    }

    DisposableEffect(connectivityManager) {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                connectionStatus = getConnectionStatus(connectivityManager)
            }

            override fun onLost(network: Network) {
                connectionStatus = getConnectionStatus(connectivityManager)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                connectionStatus = describeNetworkCapabilities(networkCapabilities)
            }
        }

        connectivityManager.registerDefaultNetworkCallback(callback)

        onDispose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }

    LazyColumn(
        modifier = Modifier
                        .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = {
                        swipeDistance = 0f
                    },
                    onVerticalDrag = { _, dragAmount ->
                        swipeDistance += dragAmount
                    },
                    onDragEnd = {
                        if (swipeDistance <= -120f) {
                            refreshHomeContent()
                        }

                        swipeDistance = 0f
                    },
                    onDragCancel = {
                        swipeDistance = 0f
                    }
                )
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = {
                        swipeDistance = 0f
                    },
                    onVerticalDrag = { _, dragAmount ->
                        swipeDistance += dragAmount
                    },
                    onDragEnd = {
                        if (swipeDistance <= -120f) {
                            refreshHomeContent()
                        }

                        swipeDistance = 0f
                    },
                    onDragCancel = {
                        swipeDistance = 0f
                    }
                )
            }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = {
                        swipeDistance = 0f
                    },
                    onVerticalDrag = { _, dragAmount ->
                        swipeDistance += dragAmount
                    },
                    onDragEnd = {
                        if (swipeDistance <= -120f) {
                            refreshHomeContent()
                        }

                        swipeDistance = 0f
                    },
                    onDragCancel = {
                        swipeDistance = 0f
                    }
                )
            }
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(Modifier.height(20.dp))

            Image(
                painter = painterResource(R.mipmap.datanudge_icon),
                contentDescription = "DataNudge logo",
                modifier = Modifier.size(92.dp)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Swipe up to refresh",
                color = TextMuted,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(12.dp))
        }

        if (!setupComplete) {
            item {
                MustDoNowCard(
                    onSetup = onOpenSetup
                )

                Spacer(Modifier.height(16.dp))
            }
        }

        item {
            ConnectionCard(
                connectionStatus = connectionStatus,
                connectedForText =
                    formatConnectionDuration(
                        elapsedMillis =
                            (currentTimeMillis - connectionSession.startedAtMillis)
                                .coerceAtLeast(0L)
                    ),
                approximateUsageText =
                    formatApproximateDataUsage(
                        bytes = connectionSession.approximateUsedBytes
                    ),
                monitoringEnabled = monitoringEnabled,
                onToggleMonitoring = {
                    if (monitoringEnabled) {
                        stopNetworkMonitorService(context)
                        monitoringEnabled = false
                    } else if (notificationPermissionGranted(context)) {
                        startNetworkMonitorService(context)
                        monitoringEnabled = true
                    } else {
                        onOpenPermissions()
                    }
                }
            )

            Spacer(Modifier.height(16.dp))
        }

        item {
            Spacer(Modifier.height(2.dp))

            OutlinedButton(
                onClick = hideApp,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Hide DataNudge", color = Color.White)
            }

            Text(
                text = "Monitoring will continue in the background.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                color = TextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun MustDoNowCard(
    onSetup: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF342A12))
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "SETUP REQUIRED",
                color = DataOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            Text(
                text = "Complete setup now",
                modifier = Modifier.padding(top = 6.dp),
                color = Color.White,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "DataNudge needs a few Android permissions and at least one selected app before reminders can work.",
                modifier = Modifier.padding(top = 8.dp),
                color = TextMuted,
                fontSize = 14.sp
            )

            Button(
                onClick = onSetup,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DataOrange),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "Set up DataNudge now",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ConnectionCard(
    connectionStatus: String,
    connectedForText: String,
    approximateUsageText: String,
    monitoringEnabled: Boolean,
    onToggleMonitoring: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "CURRENT CONNECTION",
                color = TextMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )

            Text(
                text = connectionStatus,
                modifier = Modifier.padding(top = 8.dp),
                color = connectionColour(connectionStatus),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Connected for $connectedForText",
                modifier = Modifier.padding(top = 12.dp),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Text(
                text = approximateUsageText,
                modifier = Modifier.padding(top = 5.dp),
                color = TextMuted,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier.padding(top = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (monitoringEnabled) DataGreen else TextMuted)
                )

                Text(
                    text =
                        if (monitoringEnabled) {
                            "Monitoring active"
                        } else {
                            "Monitoring paused"
                        },
                    modifier = Modifier.padding(start = 9.dp),
                    color = Color.White
                )
            }


            Button(
                onClick = onToggleMonitoring,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (monitoringEnabled) DataBlue else DataGreen
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    if (monitoringEnabled) "Pause monitoring" else "Resume monitoring"
                )
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )

                Text(
                    text = subtitle,
                    modifier = Modifier.padding(top = 3.dp),
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }

            Text(
                text = ">",
                color = DataBlue,
                fontSize = 29.sp
            )
        }
    }
}

@Composable
private fun SetupScreen(
    permissionRefreshKey: Int,
    onOpenApps: () -> Unit,
    onFinished: () -> Unit
) {
    val context = LocalContext.current

    var currentStep by remember {
        mutableIntStateOf(1)
    }

    var monitoringEnabled by remember {
        mutableStateOf(NetworkMonitorService.isMonitoringEnabled(context))
    }

    var selectedPackages by remember {
        mutableStateOf(NetworkMonitorService.getSelectedPackages(context))
    }

    var installedApps by remember {
        mutableStateOf<List<InstalledAppInfo>>(emptyList())
    }

    var installedAppsLoading by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(Unit) {
        installedAppsLoading = true

        val loadedApps =
            withContext(Dispatchers.IO) {
                loadInstalledLaunchableApps(context)
            }

        installedApps = loadedApps

        if (
            !NetworkMonitorService
                .hasSelectedPackagesPreference(context)
        ) {
            val installedPackageNames =
                loadedApps
                    .map { it.packageName }
                    .toSet()

            selectedPackages =
                DEFAULT_MONITORED_PACKAGE_NAMES
                    .intersect(installedPackageNames)

            NetworkMonitorService.setSelectedPackages(
                context,
                selectedPackages
            )
        } else {
            selectedPackages =
                NetworkMonitorService.getSelectedPackages(context)
        }

        installedAppsLoading = false
    }

    val notificationGranted =
        notificationPermissionGranted(context)

    val usageGranted =
        remember(permissionRefreshKey) {
            hasUsageAccess(context)
        }

    val overlayGranted =
        remember(permissionRefreshKey) {
            Settings.canDrawOverlays(context)
        }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { }

    val currentStepComplete =
        when (currentStep) {
            1 -> notificationGranted
            2 -> usageGranted
            3 -> overlayGranted
            4 -> selectedPackages.isNotEmpty()
            5 -> monitoringEnabled
            else -> false
        }

    LaunchedEffect(Unit) {
        currentStep =
            when {
                !notificationGranted -> 1
                !usageGranted -> 2
                !overlayGranted -> 3
                selectedPackages.isEmpty() -> 4
                !monitoringEnabled -> 5
                else -> 5
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(18.dp))

        Text(
            text = "SETUP REQUIRED",
            color = DataOrange,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            letterSpacing = 1.1.sp
        )

        Text(
            text = "Step $currentStep of 5",
            modifier = Modifier.padding(top = 4.dp),
            color = TextMuted,
            fontSize = 14.sp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(5) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (index + 1 <= currentStep) DataBlue
                            else Color.White.copy(alpha = 0.12f)
                        )
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = CardDark)
        ) {
            when (currentStep) {
                1 -> {
                    WizardPage(
                        title = "Allow notifications",
                        description = "DataNudge needs notification permission to show the permanent connection-status notification.",
                        complete = notificationGranted,
                        actionText = "Allow notifications",
                        onAction = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(
                                    Manifest.permission.POST_NOTIFICATIONS
                                )
                            }
                        }
                    )
                }

                2 -> {
                    WizardPage(
                        title = "Allow usage access",
                        description = "This lets DataNudge detect when YouTube, Spotify, Netflix, or another selected app enters the foreground.",
                        complete = usageGranted,
                        actionText = "Open usage access settings",
                        onAction = {
                            context.startActivity(
                                Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    )
                }

                3 -> {
                    WizardPage(
                        title = "Allow display over other apps",
                        description = "This lets DataNudge place the mobile-data reminder above the app you just opened.",
                        complete = overlayGranted,
                        actionText = "Open overlay settings",
                        onAction = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    )
                }

                4 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Choose apps to monitor",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Select at least one app. You can change this later in Settings.",
                            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
                            color = TextMuted,
                            fontSize = 14.sp
                        )

                        Text(
                            text = "${selectedPackages.size} selected",
                            color = if (selectedPackages.isNotEmpty()) DataGreen else DataOrange,
                            fontWeight = FontWeight.Bold
                        )

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(top = 8.dp)
                        ) {
                            if (installedAppsLoading) {
                                item {
                                    Text(
                                        text = "Loading installed apps...",
                                        color = TextMuted,
                                        modifier = Modifier.padding(
                                            vertical = 12.dp
                                        )
                                    )
                                }
                            }

                            items(
                                items = installedApps,
                                key = { it.packageName }
                            ) { app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedPackages =
                                                togglePackage(
                                                    selectedPackages,
                                                    app.packageName
                                                )

                                            NetworkMonitorService.setSelectedPackages(
                                                context,
                                                selectedPackages
                                            )
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = app.label,
                                        modifier = Modifier.weight(1f),
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )

                                    Checkbox(
                                        checked = app.packageName in selectedPackages,
                                        onCheckedChange = { checked ->
                                            selectedPackages =
                                                if (checked) {
                                                    selectedPackages + app.packageName
                                                } else {
                                                    selectedPackages - app.packageName
                                                }

                                            NetworkMonitorService.setSelectedPackages(
                                                context,
                                                selectedPackages
                                            )
                                        }
                                    )
                                }

                                HorizontalDivider(
                                    color = Color.White.copy(alpha = 0.08f)
                                )
                            }
                        }
                    }
                }

                5 -> {
                    WizardPage(
                        title = "Start background monitoring",
                        description = "DataNudge will continue checking your connection and selected apps after you close the app.",
                        complete = monitoringEnabled,
                        actionText = "Start monitoring",
                        onAction = {
                            if (notificationPermissionGranted(context)) {
                                startNetworkMonitorService(context)
                                monitoringEnabled = true
                            }
                        }
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    if (currentStep > 1) {
                        currentStep--
                    }
                },
                enabled = currentStep > 1,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Back")
            }

            Button(
                onClick = {
                    if (currentStep < 5) {
                        currentStep++
                    } else {
                        context
                            .getSharedPreferences(
                                UI_PREFERENCES,
                                Context.MODE_PRIVATE
                            )
                            .edit()
                            .putBoolean(KEY_SETUP_COMPLETED, true)
                            .apply()

                        onFinished()
                    }
                },
                enabled = currentStepComplete,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (currentStep == 5) DataGreen else DataBlue
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    if (currentStep == 5) "Finish" else "Next",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun WizardPage(
    title: String,
    description: String,
    complete: Boolean,
    actionText: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(if (complete) DataGreen else DataBlue),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (complete) "OK" else "!",
                color = Color.White,
                fontSize = 31.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = title,
            modifier = Modifier.padding(top = 20.dp),
            color = Color.White,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = description,
            modifier = Modifier.padding(top = 10.dp),
            color = TextMuted,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )

        if (complete) {
            Text(
                text = "Completed",
                modifier = Modifier.padding(top = 20.dp),
                color = DataGreen,
                fontWeight = FontWeight.Bold
            )
        } else {
            Button(
                onClick = onAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(actionText)
            }
        }
    }
}

@Composable
private fun SetupStepCard(
    number: Int,
    title: String,
    description: String,
    complete: Boolean,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (complete) DataGreen else DataBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (complete) "OK" else number.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Text(
                        text = description,
                        modifier = Modifier.padding(top = 3.dp),
                        color = TextMuted,
                        fontSize = 13.sp
                    )
                }
            }

            if (!complete) {
                OutlinedButton(
                    onClick = onClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(buttonText)
                }
            }
        }
    }
}

@Composable
private fun AppsScreen() {
    val context = LocalContext.current

    var loading by remember {
        mutableStateOf(true)
    }

    var apps by remember {
        mutableStateOf<List<InstalledAppInfo>>(emptyList())
    }

    var selectedPackages by remember {
        mutableStateOf(
            NetworkMonitorService.getSelectedPackages(context)
        )
    }

    var loadError by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(Unit) {
        loading = true
        loadError = null

        try {
            val installedApps =
                withContext(Dispatchers.IO) {
                    loadInstalledLaunchableApps(context)
                }

            apps = installedApps

            if (
                !NetworkMonitorService
                    .hasSelectedPackagesPreference(context)
            ) {
                val installedPackageNames =
                    installedApps
                        .map { it.packageName }
                        .toSet()

                selectedPackages =
                    DEFAULT_MONITORED_PACKAGE_NAMES
                        .intersect(installedPackageNames)

                NetworkMonitorService.setSelectedPackages(
                    context,
                    selectedPackages
                )
            } else {
                selectedPackages =
                    NetworkMonitorService.getSelectedPackages(
                        context
                    )
            }
        } catch (_: Exception) {
            loadError =
                "DataNudge could not read the installed app list."
        } finally {
            loading = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        item {
            Spacer(Modifier.height(16.dp))

            Text(
                text = "Choose apps to monitor",
                color = Color.White,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text =
                    "Select any installed app that should trigger " +
                        "a reminder when it opens on mobile data.",
                modifier = Modifier.padding(
                    top = 5.dp,
                    bottom = 12.dp
                ),
                color = TextMuted,
                fontSize = 14.sp
            )

            when {
                loading -> {
                    Text(
                        text = "Loading installed apps...",
                        color = TextMuted
                    )
                }

                loadError != null -> {
                    Text(
                        text =
                            loadError
                                ?: "Could not load installed apps.",
                        color = DataOrange
                    )
                }

                else -> {
                    Text(
                        text =
                            "${selectedPackages.size} selected " +
                                "from ${apps.size} installed apps",
                        color = DataGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
        }

        items(
            items = apps,
            key = { it.packageName }
        ) { app ->
            val checked =
                app.packageName in selectedPackages

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedPackages =
                            togglePackage(
                                selectedPackages,
                                app.packageName
                            )

                        NetworkMonitorService.setSelectedPackages(
                            context,
                            selectedPackages
                        )
                    }
                    .padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AndroidView(
                    factory = { viewContext ->
                        android.widget.ImageView(viewContext).apply {
                            scaleType =
                                android.widget.ImageView
                                    .ScaleType.FIT_CENTER
                        }
                    },
                    update = { imageView ->
                        imageView.setImageDrawable(app.icon)
                    },
                    modifier = Modifier.size(44.dp)
                )

                Spacer(Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = app.label,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = app.packageName,
                        modifier = Modifier.padding(top = 2.dp),
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                Checkbox(
                    checked = checked,
                    onCheckedChange = { isChecked ->
                        selectedPackages =
                            if (isChecked) {
                                selectedPackages +
                                    app.packageName
                            } else {
                                selectedPackages -
                                    app.packageName
                            }

                        NetworkMonitorService.setSelectedPackages(
                            context,
                            selectedPackages
                        )
                    }
                )
            }

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.08f)
            )
        }

        if (
            !loading &&
            loadError == null &&
            apps.isEmpty()
        ) {
            item {
                Text(
                    text = "No launchable apps were found.",
                    color = TextMuted,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        item {
            Spacer(Modifier.height(28.dp))
        }
    }
}


private fun loadInstalledLaunchableApps(
    context: Context
): List<InstalledAppInfo> {
    val launcherApps =
        context.getSystemService(
            Context.LAUNCHER_APPS_SERVICE
        ) as LauncherApps

    return launcherApps
        .getActivityList(
            null,
            Process.myUserHandle()
        )
        .groupBy { it.applicationInfo.packageName }
        .mapNotNull { (_, activities) ->
            val activity = activities.firstOrNull()
                ?: return@mapNotNull null

            InstalledAppInfo(
                label =
                    activity.label
                        ?.toString()
                        ?.trim()
                        .orEmpty()
                        .ifBlank {
                            activity.applicationInfo.packageName
                        },
                packageName =
                    activity.applicationInfo.packageName,
                icon = activity.getBadgedIcon(0)
            )
        }
        .sortedWith(
            compareBy(
                String.CASE_INSENSITIVE_ORDER,
                InstalledAppInfo::label
            )
        )
}
@Composable
private fun PermissionsScreen(
    permissionRefreshKey: Int
) {
    val context = LocalContext.current

    val notificationGranted =
        notificationPermissionGranted(context)

    val usageGranted =
        remember(permissionRefreshKey) { hasUsageAccess(context) }

    val overlayGranted =
        remember(permissionRefreshKey) { Settings.canDrawOverlays(context) }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        item {
            Spacer(Modifier.height(16.dp))

            Text(
                text = "Permissions",
                color = Color.White,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Review the Android permissions DataNudge needs.",
                modifier = Modifier.padding(top = 5.dp, bottom = 16.dp),
                color = TextMuted
            )

            PermissionCard(
                title = "Notifications",
                description = "Shows current connection status and foreground monitoring.",
                granted = notificationGranted,
                buttonText =
                    if (notificationGranted) {
                        "Review notification settings"
                    } else {
                        "Allow notifications"
                    },
                onClick = {
                    if (
                        Build.VERSION.SDK_INT >=
                            Build.VERSION_CODES.TIRAMISU &&
                        !notificationGranted
                    ) {
                        notificationPermissionLauncher.launch(
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    } else {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APP_NOTIFICATION_SETTINGS
                            )
                                .putExtra(
                                    Settings.EXTRA_APP_PACKAGE,
                                    context.packageName
                                )
                                .addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK
                                )
                        )
                    }
                }
            )

            Spacer(Modifier.height(10.dp))

            PermissionCard(
                title = "Usage access",
                description = "Detects when a selected app enters the foreground.",
                granted = usageGranted,
                buttonText =
                    if (usageGranted) {
                        "Review usage access"
                    } else {
                        "Open usage access"
                    },
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            )

            Spacer(Modifier.height(10.dp))

            PermissionCard(
                title = "Display over other apps",
                description = "Displays the mobile-data reminder over the selected app.",
                granted = overlayGranted,
                buttonText =
                    if (overlayGranted) {
                        "Review overlay permission"
                    } else {
                        "Open overlay permission"
                    },
                onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            )

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SettingsScreen(
    onApps: () -> Unit,
    onPermissions: () -> Unit,
    onSetup: () -> Unit
) {
    val context = LocalContext.current

    var automaticUpdateChecks by remember {
        mutableStateOf(
            UpdateCheckScheduler.isEnabled(context)
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        item {
            Spacer(Modifier.height(16.dp))

            Text(
                text = "Settings",
                color = Color.White,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Manage DataNudge without cluttering the home screen.",
                modifier = Modifier.padding(top = 5.dp, bottom = 16.dp),
                color = TextMuted
            )

            SettingsRow(
                title = "Apps to monitor",
                subtitle = "Choose high-data apps",
                onClick = onApps
            )

            Spacer(Modifier.height(10.dp))


            SettingsRow(
                title = "Permissions",
                subtitle = "Notifications, usage access and overlays",
                onClick = onPermissions
            )

            Spacer(Modifier.height(10.dp))

            SettingsRow(
                title = "Run setup again",
                subtitle = "Review every required setup step",
                onClick = onSetup
            )

            Spacer(Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CardDark
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Automatic update checks",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text =
                                "Check GitHub approximately once " +
                                    "per day",
                            modifier =
                                Modifier.padding(top = 4.dp),
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }

                    Switch(
                        checked = automaticUpdateChecks,
                        onCheckedChange = { enabled ->
                            automaticUpdateChecks = enabled
                            UpdateCheckScheduler.setEnabled(
                                context = context,
                                enabled = enabled
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    QuickActionCard(
        title = title,
        subtitle = subtitle,
        onClick = onClick
    )
}


private data class UpdateCheckResult(
    val latestVersion: String?,
    val releaseUrl: String?,
    val apkDownloadUrl: String?,
    val apkSha256: String?,
    val errorMessage: String?
)

@Composable
private fun UpdatesScreen() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()

    var checking by remember { mutableStateOf(true) }
    var downloadStatus by remember {
        mutableStateOf<String?>(null)
    }
    var downloadProgress by remember {
        mutableIntStateOf(0)
    }
    var refreshKey by remember { mutableIntStateOf(0) }
    var result by remember {
        mutableStateOf(
            UpdateCheckResult(
                latestVersion = null,
                releaseUrl = null,
                apkDownloadUrl = null,
                apkSha256 = null,
                errorMessage = null
            )
        )
    }

    LaunchedEffect(refreshKey) {
        checking = true
        UpdateCheckScheduler.checkNow(context)
        result = checkForDataNudgeUpdate()
        checking = false
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))

            Image(
                painter = painterResource(R.mipmap.datanudge_icon),
                contentDescription = "DataNudge logo",
                modifier = Modifier.size(92.dp)
            )

            Text(
                text = "Updates",
                modifier = Modifier.padding(top = 14.dp),
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Installed version: $VERSION_DISPLAY",
                modifier = Modifier.padding(top = 7.dp),
                color = TextMuted,
                fontSize = 15.sp
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CardDark
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when {
                        checking -> {
                            Text(
                                text =
                                    "Checking GitHub for the latest " +
                                        "DataNudge release...",
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }

                        result.errorMessage != null -> {
                            Text(
                                text = result.errorMessage ?: "Update check failed.",
                                color = DataOrange,
                                textAlign = TextAlign.Center
                            )
                        }

                        result.latestVersion == null -> {
                            Text(
                                text = "No published release was found.",
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                        }

                        isNewerVersion(
                            candidate = result.latestVersion ?: VERSION_DISPLAY,
                            current = VERSION_DISPLAY
                        ) -> {
                            Text(
                                text = "A newer version is available",
                                color = DataGreen,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text =
                                    "Latest version: " +
                                        result.latestVersion,
                                modifier = Modifier.padding(top = 8.dp),
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Button(
                                onClick = {
                                    val apkUrl =
                                        result.apkDownloadUrl

                                    if (apkUrl != null) {
                                        if (
                                            Build.VERSION.SDK_INT >=
                                                Build.VERSION_CODES.O &&
                                            !context.packageManager
                                                .canRequestPackageInstalls()
                                        ) {
                                            downloadStatus =
                                                "Allow DataNudge to " +
                                                    "install unknown " +
                                                    "apps, then return " +
                                                    "and tap again."

                                            context.startActivity(
                                                Intent(
                                                    Settings
                                                        .ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                                    Uri.parse(
                                                        "package:" +
                                                            context
                                                                .packageName
                                                    )
                                                )
                                            )
                                        } else {
                                            coroutineScope.launch {
                                                downloadStatus =
                                                    "Downloading..."
                                                downloadProgress = 0

                                                val downloadedApk =
                                                    downloadUpdateApk(
                                                        context = context,
                                                        url = apkUrl,
                                                        expectedSha256 =
                                                            result.apkSha256,
                                                        onProgress = {
                                                            downloadProgress =
                                                                it
                                                        }
                                                    )

                                                if (downloadedApk == null) {
                                                    downloadStatus =
                                                        "Download or " +
                                                            "verification " +
                                                            "failed."
                                                } else {
                                                    downloadStatus =
                                                        "Opening Android " +
                                                            "installer..."
                                                    openApkInstaller(
                                                        context,
                                                        downloadedApk
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                enabled =
                                    result.apkDownloadUrl != null &&
                                        downloadStatus !=
                                            "Downloading...",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 18.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DataBlue
                                )
                            ) {
                                Text("Download and install APK")
                            }

                            if (downloadStatus != null) {
                                Text(
                                    text =
                                        if (
                                            downloadStatus ==
                                                "Downloading..."
                                        ) {
                                            "Downloading... " +
                                                "$downloadProgress%"
                                        } else {
                                            downloadStatus ?: ""
                                        },
                                    modifier =
                                        Modifier.padding(top = 10.dp),
                                    color = TextMuted,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Text(
                                text = "View release information",
                                modifier = Modifier
                                    .padding(top = 14.dp)
                                    .clickable {
                                        result.releaseUrl?.let(
                                            uriHandler::openUri
                                        )
                                    },
                                color = DataBlue,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }

                        else -> {
                            Text(
                                text = "DataNudge is up to date",
                                color = DataGreen,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text =
                                    "Latest version: " +
                                        result.latestVersion,
                                modifier = Modifier.padding(top = 8.dp),
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = { refreshKey++ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 18.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Check again")
            }

            Text(
                text =
                    "The update check reads the latest published " +
                        "DataNudge release from GitHub. It does not " +
                        "download or install anything automatically.",
                modifier = Modifier.padding(
                    top = 14.dp,
                    bottom = 28.dp
                ),
                color = TextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

private suspend fun checkForDataNudgeUpdate(): UpdateCheckResult {
    return withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null

        try {
            connection =
                (
                    URL(GITHUB_LATEST_RELEASE_API)
                        .openConnection() as HttpURLConnection
                ).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    setRequestProperty(
                        "Accept",
                        "application/vnd.github+json"
                    )
                    setRequestProperty(
                        "User-Agent",
                        "DataNudge-Android"
                    )
                }

            val responseCode = connection.responseCode

            if (responseCode !in 200..299) {
                return@withContext UpdateCheckResult(
                    latestVersion = null,
                    releaseUrl = null,
                    apkDownloadUrl = null,
                    apkSha256 = null,
                errorMessage =
                        "Update check failed. GitHub returned " +
                            "$responseCode."
                )
            }

            val json =
                connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }

            val response = JSONObject(json)
            val latestVersion =
                response
                    .optString("tag_name")
                    .removePrefix("v")
                    .trim()

            val assets = response.optJSONArray("assets")
            var apkDownloadUrl: String? = null
            var apkSha256: String? = null

            if (assets != null) {
                for (index in 0 until assets.length()) {
                    val asset = assets.optJSONObject(index) ?: continue
                    val assetName = asset.optString("name")

                    if (assetName.endsWith(".apk", ignoreCase = true)) {
                        apkDownloadUrl =
                            asset
                                .optString("browser_download_url")
                                .ifBlank { null }

                        apkSha256 =
                            asset
                                .optString("digest")
                                .removePrefix("sha256:")
                                .ifBlank { null }

                        break
                    }
                }
            }

            UpdateCheckResult(
                latestVersion =
                    latestVersion.ifBlank { null },
                releaseUrl =
                    response
                        .optString("html_url")
                        .ifBlank { null },
                apkDownloadUrl = apkDownloadUrl,
                apkSha256 = apkSha256,
                errorMessage = null
            )
        } catch (_: Exception) {
            UpdateCheckResult(
                latestVersion = null,
                releaseUrl = null,
                apkDownloadUrl = null,
                apkSha256 = null,
                errorMessage =
                    "Could not check for updates. Check your " +
                        "internet connection and try again."
            )
        } finally {
            connection?.disconnect()
        }
    }
}


private suspend fun downloadUpdateApk(
    context: Context,
    url: String,
    expectedSha256: String?,
    onProgress: (Int) -> Unit
): File? {
    return withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null

        try {
            val updateDirectory =
                File(context.cacheDir, "updates").apply {
                    mkdirs()
                }

            val target =
                File(
                    updateDirectory,
                    "DataNudge-update.apk"
                )

            connection =
                (URL(url).openConnection() as HttpURLConnection)
                    .apply {
                        instanceFollowRedirects = true
                        connectTimeout = 20_000
                        readTimeout = 30_000
                        setRequestProperty(
                            "User-Agent",
                            "DataNudge-Android"
                        )
                    }

            if (connection.responseCode !in 200..299) {
                return@withContext null
            }

            val length = connection.contentLengthLong
            var copied = 0L

            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(32 * 1024)

                    while (true) {
                        val count = input.read(buffer)

                        if (count < 0) {
                            break
                        }

                        output.write(buffer, 0, count)
                        copied += count

                        if (length > 0L) {
                            val progress =
                                (
                                    copied * 100L / length
                                ).toInt()
                                    .coerceIn(0, 100)

                            withContext(Dispatchers.Main) {
                                onProgress(progress)
                            }
                        }
                    }
                }
            }

            if (!expectedSha256.isNullOrBlank()) {
                val actual =
                    sha256(target)

                if (
                    !actual.equals(
                        expectedSha256,
                        ignoreCase = true
                    )
                ) {
                    target.delete()
                    return@withContext null
                }
            }

            withContext(Dispatchers.Main) {
                onProgress(100)
            }

            target
        } catch (_: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }
}

private fun sha256(file: File): String {
    val digest =
        MessageDigest.getInstance("SHA-256")

    file.inputStream().use { input ->
        val buffer = ByteArray(32 * 1024)

        while (true) {
            val count = input.read(buffer)

            if (count < 0) {
                break
            }

            digest.update(buffer, 0, count)
        }
    }

    return digest
        .digest()
        .joinToString("") { byte ->
            "%02x".format(byte)
        }
}

private fun openApkInstaller(
    context: Context,
    apkFile: File
) {
    val apkUri =
        androidx.core.content.FileProvider.getUriForFile(
            context,
            context.packageName + ".fileprovider",
            apkFile
        )

    val intent =
        Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            data = apkUri
            flags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            putExtra(Intent.EXTRA_RETURN_RESULT, false)
        }

    context.startActivity(intent)
}
private fun isNewerVersion(
    candidate: String,
    current: String
): Boolean {
    val candidateParts =
        candidate
            .substringBefore(" ")
            .split(".")
            .map { it.toIntOrNull() ?: 0 }

    val currentParts =
        current
            .substringBefore(" ")
            .split(".")
            .map { it.toIntOrNull() ?: 0 }

    val length =
        maxOf(
            candidateParts.size,
            currentParts.size
        )

    for (index in 0 until length) {
        val candidateValue =
            candidateParts.getOrElse(index) { 0 }

        val currentValue =
            currentParts.getOrElse(index) { 0 }

        if (candidateValue != currentValue) {
            return candidateValue > currentValue
        }
    }

    return false
}
@Composable
private fun AboutScreen() {
    val aboutContext = androidx.compose.ui.platform.LocalContext.current
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.mipmap.datanudge_icon),
            contentDescription = "DataNudge logo",
            modifier = Modifier.size(124.dp)
        )

        Text(
            text = "DataNudge",
            modifier = Modifier.padding(top = 16.dp),
            color = Color.White,
            fontSize = 31.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "f\u00fcr Lena & Pascal",
            color = TextMuted
        )
        Text(
            text = VERSION_DISPLAY,
            modifier = Modifier.padding(top = 5.dp),
            color = TextMuted
        )

        Text(
            text = "By Kai Eysselein",
            modifier = Modifier.padding(top = 8.dp),
            color = TextMuted
        )

        Text(
            text = "A simple reminder when selected apps are opened while mobile data is active.",
            modifier = Modifier.padding(top = 18.dp),
            color = Color.White,
            textAlign = TextAlign.Center
        )

        OutlinedButton(
            onClick = { uriHandler.openUri(GITHUB_URL) },
            modifier = Modifier.padding(top = 18.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Open GitHub")
        }

        OutlinedButton(
            onClick = {
                (aboutContext as? android.app.Activity)?.let { activity ->
                    showDataNudgePrivacyNotice(activity)
                }
            },
            modifier = Modifier.padding(top = 10.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Privacy statement")
        }

        OutlinedButton(
            onClick = {
                (aboutContext as? android.app.Activity)?.let { activity ->
                    showDataNudgeDisclaimer(
                        activity = activity,
                        mandatory = false
                    )
                }
            },
            modifier = Modifier.padding(top = 10.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Disclaimer notice")
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    granted: Boolean,
    buttonText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (granted) DataGreen else DataOrange)
                )

                Text(
                    text =
                        if (granted) {
                            "$title granted"
                        } else {
                            "$title required"
                        },
                    modifier = Modifier.padding(start = 9.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = description,
                modifier = Modifier.padding(top = 6.dp),
                color = TextMuted,
                fontSize = 13.sp
            )

            OutlinedButton(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                Text(buttonText)
            }
        }
    }
}

private fun screenTitle(
    screen: Screen
): String {
    return when (screen) {
        Screen.HOME -> "DataNudge"
        Screen.SETUP -> "Setup"
        Screen.APPS -> "Apps to monitor"
        Screen.PERMISSIONS -> "Permissions"
        Screen.SETTINGS -> "Settings"
        Screen.UPDATES -> "Updates"
        Screen.ABOUT -> "About"
    }
}

private fun togglePackage(
    current: Set<String>,
    packageName: String
): Set<String> {
    return if (packageName in current) {
        current - packageName
    } else {
        current + packageName
    }
}

private fun notificationPermissionGranted(
    context: Context
): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
}

private fun hasUsageAccess(
    context: Context
): Boolean {
    val appOps =
        context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

    val mode =
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )

    return mode == AppOpsManager.MODE_ALLOWED
}

private fun connectionColour(
    status: String
): Color {
    return when (status) {
        "Wi-Fi" -> DataGreen
        "Mobile data" -> DataBlue
        "No network connection" -> DataRed
        "VPN connection" -> DataOrange
        else -> Color.White
    }
}

private fun startNetworkMonitorService(
    context: Context
) {
    ContextCompat.startForegroundService(
        context,
        Intent(context, NetworkMonitorService::class.java)
    )

    NetworkMonitorService.setMonitoringEnabled(context, true)
}

private fun stopNetworkMonitorService(
    context: Context
) {
    context.stopService(
        Intent(context, NetworkMonitorService::class.java)
    )

    NetworkMonitorService.setMonitoringEnabled(context, false)
}

private val DATANUDGE_PRIVACY_TEXT = """
DataNudge is designed to keep its operational information on your device.

DataNudge stores app settings, disclaimer acceptance, monitoring preferences and the list of selected apps locally on the device.

To provide app-specific reminders, DataNudge may read the list of launchable apps installed on the device and may use Android Usage Access to identify which app is currently in the foreground. DataNudge does not read the content of apps, messages, passwords, documents, photographs, calls or keystrokes.

DataNudge checks the device's active network type and uses Android traffic counters to estimate device-wide data use during the current connection session. These estimates are processed on the device and may differ from mobile-network-provider billing.

DataNudge uses notifications, a foreground service and, where enabled, display-over-other-apps access to show reminders. These functions depend on permissions granted by the user and Android device settings.

DataNudge does not require an account and does not intentionally upload the selected-app list, usage history or connection-session data to the developer.

When the user manually checks for updates, DataNudge contacts GitHub to read the latest published release information. Opening GitHub, downloading an APK, or opening an online legal-information page is handled by the user's browser or Android download system and is subject to those services' own privacy practices.

Removing DataNudge normally removes its locally stored app data, subject to Android backup, restore and device-management behaviour.

This notice describes the current DataNudge implementation. Future versions may require an updated privacy notice if their data handling changes.
""".trimIndent()

private fun showDataNudgePrivacyNotice(
    activity: android.app.Activity
) {
    val container = android.widget.LinearLayout(activity).apply {
        orientation = android.widget.LinearLayout.VERTICAL
        val padding = (20 * resources.displayMetrics.density).toInt()
        setPadding(padding, padding, padding, padding)
    }

    val body = android.widget.TextView(activity).apply {
        text = DATANUDGE_PRIVACY_TEXT
        textSize = 15f
        setTextIsSelectable(true)
    }

    container.addView(body)

    val scroll = android.widget.ScrollView(activity).apply {
        addView(container)
    }

    android.app.AlertDialog.Builder(activity)
        .setTitle("Privacy statement")
        .setView(scroll)
        .setCancelable(true)
        .setPositiveButton("Close", null)
        .setNeutralButton("Read online") { _, _ ->
            val intent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse(
                    "https://kaieysselein.github.io/DataNudge/privacy.html"
                )
            )
            activity.startActivity(intent)
        }
        .show()
}
private const val DISCLAIMER_VERSION = 1
private const val DISCLAIMER_PREFERENCES = "datanudge_legal"
private const val DISCLAIMER_ACCEPTED_VERSION = "accepted_disclaimer_version"

private val DATANUDGE_DISCLAIMER_TEXT = """
DataNudge is an informational reminder tool intended to help users remain aware of their current network connection.

DataNudge does not block mobile-data use, measure actual data consumption, prevent roaming, guarantee detection of every app or connection change, or guarantee that reminders will be displayed correctly or on time.

Network status, foreground-app detection, notifications and reminders depend on Android permissions, device settings, manufacturer restrictions and operating-system behaviour. DataNudge may provide delayed, incomplete or incorrect information.

DataNudge runs a foreground monitoring service and may periodically check network and app-usage status. Depending on the device, Android version, permissions, manufacturer settings and monitoring configuration, this may use processor, memory, network or other system resources and may cause the battery to deplete faster than it otherwise would.

You use DataNudge entirely at your own discretion and risk. You remain solely responsible for checking your network connection, mobile-data usage, data allowance, roaming status, device settings, battery level and all charges imposed by your mobile-network provider or another service provider.

By accepting this notice, you accept responsibility for mobile-data charges, battery consumption, device-resource usage, missed or delayed reminders, service interruptions, lost settings and other consequences arising from your use of or reliance on DataNudge.

To the fullest extent permitted by applicable law, the developer will not be liable for indirect, incidental, consequential or other losses arising from the use of, inability to use, or reliance on DataNudge. Nothing in this notice excludes or limits any liability or consumer right that cannot legally be excluded or limited.

DataNudge stores its settings and selected-app list locally on the device. Refer to the Privacy Information page for further details.
""".trimIndent()

private fun showDataNudgeDisclaimer(
    activity: android.app.Activity,
    mandatory: Boolean
) {
    val container = android.widget.LinearLayout(activity).apply {
        orientation = android.widget.LinearLayout.VERTICAL
        val padding = (20 * resources.displayMetrics.density).toInt()
        setPadding(padding, padding, padding, padding)
    }

    val body = android.widget.TextView(activity).apply {
        text = DATANUDGE_DISCLAIMER_TEXT
        textSize = 15f
        setTextIsSelectable(true)
    }
    container.addView(body)

    val acknowledgement = android.widget.CheckBox(activity).apply {
        text = "I have read and accept the Disclaimer and Terms of Use. I understand that I use DataNudge at my own risk and remain responsible for checking my connection, data usage, battery level and any resulting charges."
        visibility = if (mandatory) android.view.View.VISIBLE else android.view.View.GONE
    }
    container.addView(acknowledgement)

    val scroll = android.widget.ScrollView(activity).apply {
        addView(container)
    }

    val builder = android.app.AlertDialog.Builder(activity)
        .setTitle("Disclaimer and Terms of Use")
        .setView(scroll)
        .setCancelable(!mandatory)
        .setPositiveButton(if (mandatory) "Accept and continue" else "Close", null)
        .setNeutralButton("Read online") { _, _ ->
            val intent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://kaieysselein.github.io/DataNudge/disclaimer.html")
            )
            activity.startActivity(intent)
        }

    if (mandatory) {
        builder.setNegativeButton("Decline and exit", null)
    }

    val dialog = builder.create()

    dialog.setOnShowListener {
        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (!mandatory || acknowledgement.isChecked) {
                if (mandatory) {
                    activity.getSharedPreferences(
                        DISCLAIMER_PREFERENCES,
                        android.content.Context.MODE_PRIVATE
                    ).edit()
                        .putInt(DISCLAIMER_ACCEPTED_VERSION, DISCLAIMER_VERSION)
                        .apply()
                }
                dialog.dismiss()
            } else {
                android.widget.Toast.makeText(
                    activity,
                    "Please tick the acceptance box before continuing.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }

        if (mandatory) {
            dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                dialog.dismiss()
                activity.finishAffinity()
            }
        }
    }

    dialog.show()
}
private fun formatConnectionDuration(
    elapsedMillis: Long
): String {
    val totalMinutes = elapsedMillis / 60_000L

    if (totalMinutes < 1L) {
        return "less than a minute"
    }

    val days = totalMinutes / (24L * 60L)
    val hours = (totalMinutes % (24L * 60L)) / 60L
    val minutes = totalMinutes % 60L

    return when {
        days > 0L && hours > 0L ->
            "$days day${if (days == 1L) "" else "s"} " +
                "$hours hour${if (hours == 1L) "" else "s"}"

        days > 0L ->
            "$days day${if (days == 1L) "" else "s"}"

        hours > 0L && minutes > 0L ->
            "$hours hour${if (hours == 1L) "" else "s"} " +
                "$minutes minute${if (minutes == 1L) "" else "s"}"

        hours > 0L ->
            "$hours hour${if (hours == 1L) "" else "s"}"

        else ->
            "$minutes minute${if (minutes == 1L) "" else "s"}"
    }
}

private fun formatApproximateDataUsage(
    bytes: Long?
): String {
    if (bytes == null) {
        return "Approximate data use is unavailable on this device"
    }

    val safeBytes = bytes.coerceAtLeast(0L)
    val kibibytes = safeBytes / 1_024.0
    val mebibytes = kibibytes / 1_024.0
    val gibibytes = mebibytes / 1_024.0

    val value =
        when {
            gibibytes >= 1.0 ->
                String.format(java.util.Locale.US, "%.2f GB", gibibytes)

            mebibytes >= 1.0 ->
                String.format(java.util.Locale.US, "%.1f MB", mebibytes)

            kibibytes >= 1.0 ->
                String.format(java.util.Locale.US, "%.0f KB", kibibytes)

            else ->
                "$safeBytes bytes"
        }

    return "Approximately $value used since the connection changed"
}



















