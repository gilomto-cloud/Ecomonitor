package com.river2.monitor

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ReleaseInfo(
    val tagName: String,
    val versionName: String,
    val releaseNotes: String,
    val htmlUrl: String,
)

class UpdateManager(
    private val context: Context,
    private val repo: String = "gilomto-cloud/EcoMonitor",
) {

    private fun getCurrentVersionName(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0"
        } catch (_: Exception) {
            "1.0"
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun postUpdateNotification(release: ReleaseInfo) {
        try {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    "updates",
                    "App Updates",
                    NotificationManager.IMPORTANCE_DEFAULT,
                )
            )

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(release.htmlUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val resolved = intent.resolveActivity(context.packageManager) ?: return
            intent.component = resolved
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

            val notification = NotificationCompat.Builder(context, "updates")
                .setContentTitle("New Version Available: ${release.tagName}")
                .setContentText("Tap to view and download release from GitHub.")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            nm.notify(2, notification)
        } catch (_: Exception) {}
    }

    suspend fun checkForUpdates(): ReleaseInfo? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/$repo/releases/latest"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "EcoMonitor-AndroidApp")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            val (code, body) = http.newCall(req).execute().use { resp ->
                resp.code to resp.body.string()
            }
            if (code != 200 || body.isEmpty()) {
                return@withContext null
            }

            val json = JSONObject(body)
            val tagName = json.optString("tag_name", "")
            val versionName = tagName.removePrefix("v").removePrefix("V").trim()
            val releaseNotes = json.optString("body", "No release notes provided.")
            val htmlUrl = json.optString("html_url", "https://github.com/$repo/releases")

            val release = ReleaseInfo(
                tagName = tagName,
                versionName = versionName,
                releaseNotes = releaseNotes,
                htmlUrl = htmlUrl,
            )

            val currentVer = getCurrentVersionName()
            val isNewer = isVersionNewer(versionName, currentVer)

            if (isNewer) {
                postUpdateNotification(release)
            }
            release
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private val http = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()

        fun isVersionNewer(newVersion: String, currentVersion: String): Boolean {
            val newParts = parseVersionParts(newVersion)
            val currParts = parseVersionParts(currentVersion)

            val maxLen = maxOf(newParts.size, currParts.size)
            for (i in 0 until maxLen) {
                val n = newParts.getOrElse(i) { 0 }
                val c = currParts.getOrElse(i) { 0 }
                if (n > c) return true
                if (n < c) return false
            }
            return false
        }

        private fun parseVersionParts(versionStr: String): List<Int> {
            val clean = versionStr.trim().takeWhile { (it.isDigit() || (it == '.')) }
            return clean.split('.').mapNotNull { it.toIntOrNull() }
        }
    }
}
