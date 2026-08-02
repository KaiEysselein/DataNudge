package com.kaieysselein.datanudge

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

data class StoredUpdateInfo(
    val latestVersion: String?,
    val releaseUrl: String?,
    val apkDownloadUrl: String?,
    val apkSha256: String?,
    val updateAvailable: Boolean
)

object UpdateCheckScheduler {

    private const val WORK_NAME =
        "datanudge_daily_update_check"

    private const val IMMEDIATE_WORK_NAME =
        "datanudge_immediate_update_check"

    private const val PREFERENCES_NAME =
        "datanudge_update_preferences"

    private const val KEY_AUTO_CHECK_ENABLED =
        "automatic_update_checks_enabled"

    private const val KEY_LATEST_VERSION =
        "latest_version"

    private const val KEY_RELEASE_URL =
        "release_url"

    private const val KEY_APK_DOWNLOAD_URL =
        "apk_download_url"

    private const val KEY_APK_SHA256 =
        "apk_sha256"

    private const val KEY_UPDATE_AVAILABLE =
        "update_available"

    private const val GITHUB_LATEST_RELEASE_API =
        "https://api.github.com/repos/" +
            "KaiEysselein/DataNudge/releases/latest"

    fun isEnabled(context: Context): Boolean {
        return context
            .getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE
            )
            .getBoolean(KEY_AUTO_CHECK_ENABLED, true)
    }

    fun setEnabled(
        context: Context,
        enabled: Boolean
    ) {
        context
            .getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putBoolean(KEY_AUTO_CHECK_ENABLED, enabled)
            .apply()

        if (enabled) {
            ensureScheduled(context)
            checkNow(context)
        } else {
            WorkManager
                .getInstance(context)
                .cancelUniqueWork(WORK_NAME)
        }
    }

    fun ensureScheduled(context: Context) {
        if (!isEnabled(context)) {
            return
        }

        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        val request =
            PeriodicWorkRequest.Builder(
                UpdateCheckWorker::class.java,
                1,
                TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .build()

        WorkManager
            .getInstance(context)
            .enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
    }

    fun checkNow(context: Context) {
        val constraints =
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

        val request =
            OneTimeWorkRequest.Builder(
                UpdateCheckWorker::class.java
            )
                .setConstraints(constraints)
                .build()

        WorkManager
            .getInstance(context)
            .enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
    }

    fun getStoredUpdate(context: Context): StoredUpdateInfo {
        val preferences =
            context.getSharedPreferences(
                PREFERENCES_NAME,
                Context.MODE_PRIVATE
            )

        return StoredUpdateInfo(
            latestVersion =
                preferences.getString(KEY_LATEST_VERSION, null),
            releaseUrl =
                preferences.getString(KEY_RELEASE_URL, null),
            apkDownloadUrl =
                preferences.getString(KEY_APK_DOWNLOAD_URL, null),
            apkSha256 =
                preferences.getString(KEY_APK_SHA256, null),
            updateAvailable =
                preferences.getBoolean(
                    KEY_UPDATE_AVAILABLE,
                    false
                )
        )
    }

    internal fun fetchAndStore(context: Context): Boolean {
        var connection: HttpURLConnection? = null

        try {
            connection =
                (
                    URL(GITHUB_LATEST_RELEASE_API)
                        .openConnection() as HttpURLConnection
                ).apply {
                    requestMethod = "GET"
                    connectTimeout = 15_000
                    readTimeout = 15_000
                    setRequestProperty(
                        "Accept",
                        "application/vnd.github+json"
                    )
                    setRequestProperty(
                        "User-Agent",
                        "DataNudge-Android"
                    )
                }

            if (connection.responseCode !in 200..299) {
                return false
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

            val releaseUrl =
                response
                    .optString("html_url")
                    .ifBlank { null }

            val assets = response.optJSONArray("assets")
            var apkDownloadUrl: String? = null
            var apkSha256: String? = null

            if (assets != null) {
                for (index in 0 until assets.length()) {
                    val asset =
                        assets.optJSONObject(index)
                            ?: continue

                    val name = asset.optString("name")

                    if (name.endsWith(".apk", ignoreCase = true)) {
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

            val updateAvailable =
                latestVersion.isNotBlank() &&
                    isNewerVersion(
                        candidate = latestVersion,
                        current = installedVersion(context)
                    )

            context
                .getSharedPreferences(
                    PREFERENCES_NAME,
                    Context.MODE_PRIVATE
                )
                .edit()
                .putString(KEY_LATEST_VERSION, latestVersion)
                .putString(KEY_RELEASE_URL, releaseUrl)
                .putString(
                    KEY_APK_DOWNLOAD_URL,
                    apkDownloadUrl
                )
                .putString(KEY_APK_SHA256, apkSha256)
                .putBoolean(
                    KEY_UPDATE_AVAILABLE,
                    updateAvailable
                )
                .apply()

            refreshMonitoringNotification(context)
            return true
        } catch (_: Exception) {
            return false
        } finally {
            connection?.disconnect()
        }
    }

    private fun refreshMonitoringNotification(
        context: Context
    ) {
        if (!NetworkMonitorService.isMonitoringEnabled(context)) {
            return
        }

        val intent =
            Intent(
                context,
                NetworkMonitorService::class.java
            ).apply {
                action =
                    NetworkMonitorService.ACTION_REFRESH_NOTIFICATION
            }

        ContextCompat.startForegroundService(
            context,
            intent
        )
    }

    private fun installedVersion(
        context: Context
    ): String {
        return try {
            val packageInfo =
                if (
                    android.os.Build.VERSION.SDK_INT >=
                        android.os.Build.VERSION_CODES.TIRAMISU
                ) {
                    context.packageManager.getPackageInfo(
                        context.packageName,
                        android.content.pm.PackageManager
                            .PackageInfoFlags.of(0L)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(
                        context.packageName,
                        0
                    )
                }

            packageInfo.versionName ?: "0.0.0.0"
        } catch (_: Exception) {
            "0.0.0.0"
        }
    }

    fun isNewerVersion(
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
}

class UpdateCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        return if (
            UpdateCheckScheduler.fetchAndStore(
                applicationContext
            )
        ) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}

