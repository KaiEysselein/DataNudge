package com.kaieysselein.datanudge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat

class NetworkMonitorService : Service() {

    private lateinit var connectivityManager: ConnectivityManager

    private val mainHandler =
        Handler(Looper.getMainLooper())

    private var callbackRegistered = false
    private var lastConnectionStatus: String? = null

    private val delayedNetworkCheck =
        Runnable {
            checkForNetworkChange()
        }

    private val networkCallback =
        object : ConnectivityManager.NetworkCallback() {

            override fun onAvailable(network: Network) {
                scheduleNetworkCheck()
            }

            override fun onLost(network: Network) {
                scheduleNetworkCheck()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                scheduleNetworkCheck()
            }
        }

    override fun onCreate() {
        super.onCreate()

        connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE)
                    as ConnectivityManager

        createNotificationChannels()

        lastConnectionStatus =
            getConnectionStatus(connectivityManager)

        startAsForegroundService()

        connectivityManager.registerDefaultNetworkCallback(
            networkCallback
        )

        callbackRegistered = true
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (intent?.action == ACTION_DISMISS_ALERT) {
            dismissNetworkChangeAlert()
            return START_STICKY
        }

        setMonitoringEnabled(this, true)
        updateMonitoringNotification()

        return START_STICKY
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(delayedNetworkCheck)

        if (callbackRegistered) {
            connectivityManager.unregisterNetworkCallback(
                networkCallback
            )

            callbackRegistered = false
        }

        dismissNetworkChangeAlert()
        setMonitoringEnabled(this, false)

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun scheduleNetworkCheck() {
        mainHandler.removeCallbacks(delayedNetworkCheck)

        mainHandler.postDelayed(
            delayedNetworkCheck,
            NETWORK_SETTLE_DELAY_MS
        )
    }

    private fun checkForNetworkChange() {
        val newStatus =
            getConnectionStatus(connectivityManager)

        val previousStatus =
            lastConnectionStatus

        updateMonitoringNotification(newStatus)

        if (newStatus == previousStatus) {
            return
        }

        lastConnectionStatus =
            newStatus

        if (previousStatus != null) {
            showNetworkChangeAlert(
                previousStatus = previousStatus,
                newStatus = newStatus
            )
        }
    }

    private fun startAsForegroundService() {
        val notification =
            createMonitoringNotification(
                getConnectionStatus(connectivityManager)
            )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                MONITORING_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(
                MONITORING_NOTIFICATION_ID,
                notification
            )
        }
    }

    private fun updateMonitoringNotification(
        status: String = getConnectionStatus(connectivityManager)
    ) {
        val notificationManager =
            getSystemService(NotificationManager::class.java)

        notificationManager.notify(
            MONITORING_NOTIFICATION_ID,
            createMonitoringNotification(status)
        )
    }

    private fun showNetworkChangeAlert(
        previousStatus: String,
        newStatus: String
    ) {
        val notificationManager =
            getSystemService(NotificationManager::class.java)

        notificationManager.cancel(
            NETWORK_CHANGE_NOTIFICATION_ID
        )

        notificationManager.notify(
            NETWORK_CHANGE_NOTIFICATION_ID,
            createNetworkChangeNotification(
                previousStatus = previousStatus,
                newStatus = newStatus
            )
        )
    }

    private fun dismissNetworkChangeAlert() {
        val notificationManager =
            getSystemService(NotificationManager::class.java)

        notificationManager.cancel(
            NETWORK_CHANGE_NOTIFICATION_ID
        )
    }

    private fun createMonitoringNotification(
        status: String
    ): Notification {
        val openAppIntent =
            Intent(
                this,
                MainActivity::class.java
            )

        val openAppPendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        return NotificationCompat.Builder(
            this,
            MONITORING_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("DataNudge is monitoring")
            .setContentText("Current connection: $status")
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNetworkChangeNotification(
        previousStatus: String,
        newStatus: String
    ): Notification {

        val dismissIntent =
            Intent(
                this,
                NetworkMonitorService::class.java
            ).apply {
                action = ACTION_DISMISS_ALERT
            }

        val dismissPendingIntent =
            PendingIntent.getService(
                this,
                1,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                        PendingIntent.FLAG_IMMUTABLE
            )

        return NotificationCompat.Builder(
            this,
            ALERT_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Network changed")
            .setContentText("You are now using $newStatus")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "Your connection changed from " +
                                "$previousStatus to $newStatus."
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setTimeoutAfter(ALERT_TIMEOUT_MS)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .addAction(
                0,
                "OK",
                dismissPendingIntent
            )
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val notificationManager =
            getSystemService(NotificationManager::class.java)

        val monitoringChannel =
            NotificationChannel(
                MONITORING_CHANNEL_ID,
                "Background monitoring",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description =
                    "Shows the current connection while DataNudge monitors."
            }

        val alertChannel =
            NotificationChannel(
                ALERT_CHANNEL_ID,
                "Network change alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description =
                    "Pops up when the active connection changes."

                enableVibration(true)
                setShowBadge(true)
            }

        notificationManager.createNotificationChannel(
            monitoringChannel
        )

        notificationManager.createNotificationChannel(
            alertChannel
        )
    }

    companion object {
        private const val MONITORING_CHANNEL_ID =
            "datanudge_network_monitor"

        private const val ALERT_CHANNEL_ID =
            "datanudge_network_change_alerts_v2"

        private const val MONITORING_NOTIFICATION_ID =
            1001

        private const val NETWORK_CHANGE_NOTIFICATION_ID =
            1002

        private const val NETWORK_SETTLE_DELAY_MS =
            1_200L

        private const val ALERT_TIMEOUT_MS =
            30_000L

        private const val ACTION_DISMISS_ALERT =
            "com.kaieysselein.datanudge.DISMISS_NETWORK_ALERT"

        private const val PREFERENCES_NAME =
            "datanudge_preferences"

        private const val KEY_MONITORING_ENABLED =
            "monitoring_enabled"

        fun isMonitoringEnabled(
            context: Context
        ): Boolean {
            return context
                .getSharedPreferences(
                    PREFERENCES_NAME,
                    Context.MODE_PRIVATE
                )
                .getBoolean(
                    KEY_MONITORING_ENABLED,
                    false
                )
        }

        fun setMonitoringEnabled(
            context: Context,
            enabled: Boolean
        ) {
            context
                .getSharedPreferences(
                    PREFERENCES_NAME,
                    Context.MODE_PRIVATE
                )
                .edit()
                .putBoolean(
                    KEY_MONITORING_ENABLED,
                    enabled
                )
                .apply()
        }
    }
}

fun getConnectionStatus(
    connectivityManager: ConnectivityManager
): String {
    val activeNetwork =
        connectivityManager.activeNetwork
            ?: return "No network connection"

    val capabilities =
        connectivityManager.getNetworkCapabilities(
            activeNetwork
        ) ?: return "Unknown connection"

    return describeNetworkCapabilities(capabilities)
}

fun describeNetworkCapabilities(
    capabilities: NetworkCapabilities
): String {
    return when {
        capabilities.hasTransport(
            NetworkCapabilities.TRANSPORT_VPN
        ) -> "VPN connection"

        capabilities.hasTransport(
            NetworkCapabilities.TRANSPORT_WIFI
        ) -> "Wi-Fi"

        capabilities.hasTransport(
            NetworkCapabilities.TRANSPORT_CELLULAR
        ) -> "Mobile data"

        capabilities.hasTransport(
            NetworkCapabilities.TRANSPORT_ETHERNET
        ) -> "Ethernet"

        capabilities.hasTransport(
            NetworkCapabilities.TRANSPORT_BLUETOOTH
        ) -> "Bluetooth network"

        else -> "Other connection"
    }
}
