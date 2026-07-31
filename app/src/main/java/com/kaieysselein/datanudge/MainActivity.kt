package com.kaieysselein.datanudge

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                    ConnectionScreen()
                }
            }
        }
    }
}

@Composable
fun ConnectionScreen() {
    val context = LocalContext.current
    val connectionStatus = getConnectionStatus(context)

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

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Your current connection:",
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = connectionStatus,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun getConnectionStatus(context: Context): String {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val activeNetwork =
        connectivityManager.activeNetwork
            ?: return "No network connection"

    val capabilities =
        connectivityManager.getNetworkCapabilities(activeNetwork)
            ?: return "Unknown connection"

    return when {
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ->
            "VPN connection"

        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->
            "Wi-Fi"

        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->
            "Mobile data"

        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->
            "Ethernet"

        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) ->
            "Bluetooth network"

        else ->
            "Other connection"
    }
}