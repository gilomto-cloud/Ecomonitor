package com.river2.monitor

import android.content.Context
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

object Telemetry {
    private val PING_INTERVAL_MS = TimeUnit.DAYS.toMillis(1)

    suspend fun sendPingIfNeeded(context: Context, store: Store) = withContext(Dispatchers.IO) {
        if (!store.telemetryEnabled) {
            return@withContext
        }
        val urlString = store.telemetryUrl.ifBlank { return@withContext }
        val now = System.currentTimeMillis()
        if ((now - store.lastPingTime) < PING_INTERVAL_MS) {
            return@withContext
        }

        try {
            val appVersion = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
            } catch (_: Exception) {
                "unknown"
            }

            val payload = JSONObject().apply {
                put("installation_id", store.installationId)
                put("app_version", appVersion)
                put("android_version", Build.VERSION.RELEASE)
                put("sdk_version", Build.VERSION.SDK_INT)
                put("device_model", Build.MODEL)
                put("ecoflow_model", store.model)
                put("timestamp", now / 1000)
            }

            val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                doOutput = true
                connectTimeout = 5000
                readTimeout = 5000
            }

            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            if ((conn.responseCode >= 200) && (conn.responseCode <= 299)) {
                store.lastPingTime = now
            }
            conn.disconnect()
        } catch (_: Exception) {
            // Network errors or invalid URL silently ignored
        }
    }
}
