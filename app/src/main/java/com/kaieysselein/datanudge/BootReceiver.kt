package com.kaieysselein.datanudge

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        val supportedAction =
            intent.action == Intent.ACTION_BOOT_COMPLETED ||
                intent.action == Intent.ACTION_MY_PACKAGE_REPLACED

        if (!supportedAction) {
            return
        }

        val monitoringEnabled =
            NetworkMonitorService.isMonitoringEnabled(context)

        if (!monitoringEnabled) {
            return
        }

        val notificationPermissionGranted =
            Build.VERSION.SDK_INT <
                Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

        if (!notificationPermissionGranted) {
            return
        }

        val serviceIntent =
            Intent(
                context,
                NetworkMonitorService::class.java
            )

        ContextCompat.startForegroundService(
            context,
            serviceIntent
        )
    }
}
