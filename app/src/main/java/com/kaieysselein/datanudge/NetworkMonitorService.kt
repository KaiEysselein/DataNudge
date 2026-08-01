package com.kaieysselein.datanudge

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat

class NetworkMonitorService : Service() {

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var windowManager: WindowManager

    private val mainHandler = Handler(Looper.getMainLooper())

    private var callbackRegistered = false
    private var lastConnectionStatus: String? = null
    private var lastUsageCheckTime = 0L
    private var lastForegroundPackage: String? = null
    private var overlayView: View? = null

    private val delayedNetworkCheck = Runnable {
        updateCurrentNetworkStatus()
    }

    private val appMonitorCheck = object : Runnable {
        override fun run() {
            checkForegroundApp()
            mainHandler.postDelayed(this, APP_CHECK_INTERVAL_MS)
        }
    }

    private val networkCallback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                scheduleNetworkUpdate()
            }

            override fun onLost(network: Network) {
                scheduleNetworkUpdate()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                scheduleNetworkUpdate()
            }
        }

    override fun onCreate() {
        super.onCreate()

        connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        usageStatsManager =
            getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        windowManager =
            getSystemService(Context.WINDOW_SERVICE) as WindowManager

        createNotificationChannel()
        removeLegacyNotifications()

        lastConnectionStatus = getConnectionStatus(connectivityManager)
        lastUsageCheckTime = System.currentTimeMillis() - 3_000L

        startAsForegroundService(
            lastConnectionStatus ?: "Unknown connection"
        )

        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        callbackRegistered = true

        mainHandler.post(appMonitorCheck)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        setMonitoringEnabled(this, true)
        return START_STICKY
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(delayedNetworkCheck)
        mainHandler.removeCallbacks(appMonitorCheck)

        if (callbackRegistered) {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            callbackRegistered = false
        }

        hideMobileDataOverlay()
        setMonitoringEnabled(this, false)

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun scheduleNetworkUpdate() {
        mainHandler.removeCallbacks(delayedNetworkCheck)
        mainHandler.postDelayed(delayedNetworkCheck, NETWORK_SETTLE_DELAY_MS)
    }

    private fun updateCurrentNetworkStatus() {
        val newStatus = getConnectionStatus(connectivityManager)

        if (newStatus == lastConnectionStatus) {
            return
        }

        lastConnectionStatus = newStatus

        getSystemService(NotificationManager::class.java).notify(
            MONITORING_NOTIFICATION_ID,
            createMonitoringNotification(
                status = newStatus,
                silent = false
            )
        )
    }

    private fun startAsForegroundService(
        status: String
    ) {
        val notification =
            createMonitoringNotification(
                status = status,
                silent = true
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

    private fun createMonitoringNotification(
        status: String,
        silent: Boolean
    ): Notification {
        val openAppPendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

        val builder =
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(getStatusBarIconResId(status))
                .setContentTitle("DataNudge")
                .setContentText("Current connection: $status")
                .setContentIntent(openAppPendingIntent)
                .setOngoing(true)
                .setAutoCancel(false)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOnlyAlertOnce(silent)

        if (silent) {
            builder.setSilent(true)
        }

        return builder.build()
    }

    private fun checkForegroundApp() {
        if (!Settings.canDrawOverlays(this)) {
            return
        }

        val selectedPackages = getSelectedPackages(this)

        if (selectedPackages.isEmpty()) {
            return
        }

        val now = System.currentTimeMillis()
        val begin = maxOf(lastUsageCheckTime, now - 5_000L)
        val events = usageStatsManager.queryEvents(begin, now)
        lastUsageCheckTime = now

        val event = UsageEvents.Event()
        var newestPackage: String? = null
        var newestTimestamp = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)

            val isForegroundEvent =
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                    (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                            event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
                    )

            if (isForegroundEvent && event.timeStamp >= newestTimestamp) {
                newestTimestamp = event.timeStamp
                newestPackage = event.packageName
            }
        }

        val packageName = newestPackage ?: return

        if (packageName == lastForegroundPackage) {
            return
        }

        lastForegroundPackage = packageName

        if (
            packageName in selectedPackages &&
            getConnectionStatus(connectivityManager) == "Mobile data"
        ) {
            showMobileDataOverlay(
                appLabel = getAppLabel(packageName)
            )
        }
    }

    private fun showMobileDataOverlay(
        appLabel: String
    ) {
        hideMobileDataOverlay()

        val density = resources.displayMetrics.density
        fun dp(value: Int): Int = (value * density).toInt()

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(22), dp(24), dp(20))

            background = GradientDrawable().apply {
                cornerRadius = dp(22).toFloat()
                setColor(Color.rgb(12, 28, 48))
                setStroke(dp(2), Color.rgb(32, 201, 75))
            }
        }

        val title = TextView(this).apply {
            text = "Mobile data reminder"
            setTextColor(Color.WHITE)
            textSize = 21f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
        }

        val message = TextView(this).apply {
            text = "$appLabel is open while your phone is using mobile data."
            setTextColor(Color.rgb(220, 231, 242))
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(18))
        }

        val okButton = Button(this).apply {
            text = "OK"
            isAllCaps = false
            setTextColor(Color.WHITE)
            textSize = 16f
            backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    Color.rgb(8, 120, 249)
                )
            setOnClickListener {
                hideMobileDataOverlay()
            }
        }

        container.addView(
            title,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        container.addView(
            message,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        container.addView(
            okButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
            )
        )

        val params =
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
                horizontalMargin = 0.07f
            }

        overlayView = container
        windowManager.addView(container, params)
    }

    private fun hideMobileDataOverlay() {
        val view = overlayView ?: return

        try {
            windowManager.removeView(view)
        } catch (_: IllegalArgumentException) {
        }

        overlayView = null
    }

    private fun getAppLabel(
        packageName: String
    ): String {
        return try {
            val appInfo =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getApplicationInfo(
                        packageName,
                        PackageManager.ApplicationInfoFlags.of(0)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getApplicationInfo(packageName, 0)
                }

            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    @DrawableRes
    private fun getStatusBarIconResId(
        status: String
    ): Int {
        return when (status) {
            "Wi-Fi" -> R.drawable.ic_status_wifi
            "Mobile data" -> R.drawable.ic_status_mobile
            "No network connection" -> R.drawable.ic_status_offline
            "VPN connection" -> R.drawable.ic_status_vpn
            else -> R.drawable.ic_status_other
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Connection status",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description =
                        "Shows the current connection and announces connection changes."
                    setShowBadge(false)
                    enableVibration(true)
                }
            )
    }

    private fun removeLegacyNotifications() {
        val manager =
            getSystemService(NotificationManager::class.java)

        manager.cancel(1002)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.deleteNotificationChannel("datanudge_network_monitor")
            manager.deleteNotificationChannel("datanudge_network_change_alerts_v1")
            manager.deleteNotificationChannel("datanudge_network_change_alerts_v2")
        }
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID =
            "datanudge_connection_status_v3"

        private const val MONITORING_NOTIFICATION_ID = 1001
        private const val NETWORK_SETTLE_DELAY_MS = 1_200L
        private const val APP_CHECK_INTERVAL_MS = 1_000L

        private const val PREFERENCES_NAME =
            "datanudge_preferences"

        private const val KEY_MONITORING_ENABLED =
            "monitoring_enabled"

        private const val KEY_SELECTED_PACKAGES =
            "selected_packages"

        fun isMonitoringEnabled(
            context: Context
        ): Boolean {
            return context
                .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_MONITORING_ENABLED, false)
        }

        fun setMonitoringEnabled(
            context: Context,
            enabled: Boolean
        ) {
            context
                .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_MONITORING_ENABLED, enabled)
                .apply()
        }

        fun getSelectedPackages(
            context: Context
        ): Set<String> {
            return context
                .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .getStringSet(KEY_SELECTED_PACKAGES, emptySet())
                ?.toSet()
                ?: emptySet()
        }

        fun setSelectedPackages(
            context: Context,
            packages: Set<String>
        ) {
            context
                .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .edit()
                .putStringSet(KEY_SELECTED_PACKAGES, packages.toSet())
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
        connectivityManager.getNetworkCapabilities(activeNetwork)
            ?: return "Unknown connection"

    return describeNetworkCapabilities(capabilities)
}

fun describeNetworkCapabilities(
    capabilities: NetworkCapabilities
): String {
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
