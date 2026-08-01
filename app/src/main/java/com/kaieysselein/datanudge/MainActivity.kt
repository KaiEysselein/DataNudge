package com.kaieysselein.datanudge

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.kaieysselein.datanudge.ui.theme.DataNudgeTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DataNudgeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ConnectionScreen(
                        hideApp = {
                            moveTaskToBack(true)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectionScreen(
    hideApp: () -> Unit
) {
    val context = LocalContext.current

    val connectivityManager = remember {
        context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager
    }

    var connectionStatus by remember {
        mutableStateOf(
            getConnectionStatus(connectivityManager)
        )
    }

    var monitoringEnabled by remember {
        mutableStateOf(
            NetworkMonitorService.isMonitoringEnabled(context)
        )
    }

    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { permissionGranted ->

            if (permissionGranted ||
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
            ) {
                startNetworkMonitorService(context)
                monitoringEnabled = true
            }
        }

    DisposableEffect(connectivityManager) {
        val networkCallback =
            object : ConnectivityManager.NetworkCallback() {

                override fun onAvailable(network: Network) {
                    connectionStatus =
                        getConnectionStatus(connectivityManager)
                }

                override fun onLost(network: Network) {
                    connectionStatus =
                        getConnectionStatus(connectivityManager)
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    connectionStatus =
                        describeNetworkCapabilities(
                            networkCapabilities
                        )
                }
            }

        connectivityManager.registerDefaultNetworkCallback(
            networkCallback
        )

        connectionStatus =
            getConnectionStatus(connectivityManager)

        onDispose {
            connectivityManager.unregisterNetworkCallback(
                networkCallback
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "DataNudge",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        Text(
            text = "Your current connection:",
            fontSize = 18.sp
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text(
            text = connectionStatus,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            modifier = Modifier.height(32.dp)
        )

        Text(
            text =
                if (monitoringEnabled) {
                    "Background monitoring is on"
                } else {
                    "Background monitoring is off"
                },
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(
            modifier = Modifier.height(16.dp)
        )

        Button(
            onClick = {
                if (monitoringEnabled) {
                    stopNetworkMonitorService(context)
                    monitoringEnabled = false
                } else {
                    val permissionRequired =
                        Build.VERSION.SDK_INT >=
                                Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED

                    if (permissionRequired) {
                        notificationPermissionLauncher.launch(
                            Manifest.permission.POST_NOTIFICATIONS
                        )
                    } else {
                        startNetworkMonitorService(context)
                        monitoringEnabled = true
                    }
                }
            }
        ) {
            Text(
                text =
                    if (monitoringEnabled) {
                        "Stop background monitoring"
                    } else {
                        "Start background monitoring"
                    }
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Button(
            onClick = hideApp
        ) {
            Text("OK")
        }
    }
}

private fun startNetworkMonitorService(
    context: Context
) {
    val intent =
        Intent(
            context,
            NetworkMonitorService::class.java
        )

    ContextCompat.startForegroundService(
        context,
        intent
    )

    NetworkMonitorService.setMonitoringEnabled(
        context,
        true
    )
}

private fun stopNetworkMonitorService(
    context: Context
) {
    val intent =
        Intent(
            context,
            NetworkMonitorService::class.java
        )

    context.stopService(intent)

    NetworkMonitorService.setMonitoringEnabled(
        context,
        false
    )
}
