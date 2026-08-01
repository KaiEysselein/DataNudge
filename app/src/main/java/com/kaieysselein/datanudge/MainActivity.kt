package com.kaieysselein.datanudge

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.kaieysselein.datanudge.ui.theme.DataNudgeTheme

private val DataBlue = Color(0xFF0878F9)
private val DataGreen = Color(0xFF20C94B)
private val DataOrange = Color(0xFFFFA000)
private val DataRed = Color(0xFFFF5D5D)
private val ScreenDark = Color(0xFF07101D)
private val CardDark = Color(0xE6111E2F)
private val TextMuted = Color(0xFFB8C4D6)

private const val VERSION_DISPLAY = "0.0.4.1 (alpha)"
private const val GITHUB_URL = "https://github.com/KaiEysselein/DataNudge"
private const val UI_PREFERENCES = "datanudge_ui_preferences"
private const val KEY_SETUP_COMPLETED = "setup_completed"

data class LaunchableApp(
    val label: String,
    val packageName: String
)

private val CURATED_HIGH_DATA_APPS =
    listOf(
        LaunchableApp("YouTube", "com.google.android.youtube"),
        LaunchableApp("Netflix", "com.netflix.mediaclient"),
        LaunchableApp("TikTok", "com.zhiliaoapp.musically"),
        LaunchableApp("Spotify", "com.spotify.music"),
        LaunchableApp("YouTube Music", "com.google.android.apps.youtube.music"),
        LaunchableApp("Instagram", "com.instagram.android"),
        LaunchableApp("Facebook", "com.facebook.katana"),
        LaunchableApp("WhatsApp", "com.whatsapp"),
        LaunchableApp("Messenger", "com.facebook.orca"),
        LaunchableApp("Snapchat", "com.snapchat.android"),
        LaunchableApp("Prime Video", "com.amazon.avod.thirdpartyclient"),
        LaunchableApp("Disney+", "com.disney.disneyplus"),
        LaunchableApp("Twitch", "tv.twitch.android.app"),
        LaunchableApp("X", "com.twitter.android"),
        LaunchableApp("Google Meet", "com.google.android.apps.tachyon"),
        LaunchableApp("Zoom", "us.zoom.videomeetings"),
        LaunchableApp("Microsoft Teams", "com.microsoft.teams"),
        LaunchableApp("Google Chrome", "com.android.chrome"),
        LaunchableApp("Google Maps", "com.google.android.apps.maps"),
        LaunchableApp("Google Drive", "com.google.android.apps.docs")
    )

private enum class Screen {
    HOME,
    SETUP,
    APPS,
    PERMISSIONS,
    SETTINGS,
    ABOUT
}

class MainActivity : ComponentActivity() {

    private var permissionRefreshKey by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DataNudgeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ScreenDark
                ) {
                    DataNudgeApp(
                        permissionRefreshKey = permissionRefreshKey,
                        hideApp = { moveTaskToBack(true) }
                    )
                }
            }
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
    hideApp: () -> Unit
) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            DataNudgeTopBar(
                screen = currentScreen,
                menuExpanded = menuExpanded,
                onMenuExpandedChange = { menuExpanded = it },
                onHome = { currentScreen = Screen.HOME },
                onSettings = { currentScreen = Screen.SETTINGS },
                onSetup = { currentScreen = Screen.SETUP },
                onAbout = { currentScreen = Screen.ABOUT }
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
                    onOpenSetup = { currentScreen = Screen.SETUP },
                    onOpenApps = { currentScreen = Screen.APPS },
                    onOpenPermissions = { currentScreen = Screen.PERMISSIONS },
                    hideApp = hideApp
                )

                Screen.SETUP -> SetupScreen(
                    permissionRefreshKey = permissionRefreshKey,
                    onOpenApps = { currentScreen = Screen.APPS },
                    onFinished = { currentScreen = Screen.HOME }
                )

                Screen.APPS -> AppsScreen(
                    onBack = { currentScreen = Screen.SETTINGS }
                )

                Screen.PERMISSIONS -> PermissionsScreen(
                    permissionRefreshKey = permissionRefreshKey
                )

                Screen.SETTINGS -> SettingsScreen(
                    onApps = { currentScreen = Screen.APPS },
                    onPermissions = { currentScreen = Screen.PERMISSIONS },
                    onSetup = { currentScreen = Screen.SETUP },
                    onAbout = { currentScreen = Screen.ABOUT }
                )

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
    onHome: () -> Unit,
    onSettings: () -> Unit,
    onSetup: () -> Unit,
    onAbout: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(ScreenDark)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
                    text = { Text("Run setup again") },
                    onClick = {
                        onMenuExpandedChange(false)
                        onSetup()
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
    onOpenApps: () -> Unit,
    onOpenPermissions: () -> Unit,
    hideApp: () -> Unit
) {
    val context = LocalContext.current
    val connectivityManager = remember {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    var connectionStatus by remember {
        mutableStateOf(getConnectionStatus(connectivityManager))
    }

    var monitoringEnabled by remember {
        mutableStateOf(NetworkMonitorService.isMonitoringEnabled(context))
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
                monitoringEnabled = monitoringEnabled,
                selectedAppCount = selectedPackages.size,
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
            QuickActionCard(
                title = "Apps to monitor",
                subtitle = "${selectedPackages.size} selected",
                onClick = onOpenApps
            )

            Spacer(Modifier.height(10.dp))

            QuickActionCard(
                title = "Permissions",
                subtitle =
                    if (notificationGranted && usageGranted && overlayGranted) {
                        "All required permissions granted"
                    } else {
                        "Action required"
                    },
                onClick = onOpenPermissions
            )

            Spacer(Modifier.height(18.dp))

            OutlinedButton(
                onClick = hideApp,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Close app - keep monitoring", color = Color.White)
            }

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
    monitoringEnabled: Boolean,
    selectedAppCount: Int,
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

            Text(
                text = "$selectedAppCount monitored app${if (selectedAppCount == 1) "" else "s"}",
                modifier = Modifier.padding(top = 8.dp),
                color = TextMuted,
                fontSize = 13.sp
            )

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
                            items(
                                items = CURATED_HIGH_DATA_APPS,
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
private fun AppsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var selectedPackages by remember {
        mutableStateOf(NetworkMonitorService.getSelectedPackages(context))
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
                text = "DataNudge warns when a selected app opens while mobile data is active.",
                modifier = Modifier.padding(top = 5.dp, bottom = 12.dp),
                color = TextMuted,
                fontSize = 14.sp
            )

            Text(
                text = "${selectedPackages.size} selected",
                color = DataGreen,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(10.dp))
        }

        items(
            items = CURATED_HIGH_DATA_APPS,
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
                    .padding(vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

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

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
        }

        item {
            Spacer(Modifier.height(18.dp))

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Done")
            }

            Spacer(Modifier.height(28.dp))
        }
    }
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
                buttonText = "Allow notifications",
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    }
                }
            )

            Spacer(Modifier.height(10.dp))

            PermissionCard(
                title = "Usage access",
                description = "Detects when a selected app enters the foreground.",
                granted = usageGranted,
                buttonText = "Open usage access",
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
                buttonText = "Open overlay permission",
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
    onSetup: () -> Unit,
    onAbout: () -> Unit
) {
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

            SettingsRow(
                title = "About DataNudge",
                subtitle = VERSION_DISPLAY,
                onClick = onAbout
            )

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

@Composable
private fun AboutScreen() {
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

            if (!granted) {
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




