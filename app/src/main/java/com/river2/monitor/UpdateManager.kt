package com.river2.monitor

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class ReleaseInfo(
    val tagName: String,
    val versionName: String,
    val releaseNotes: String,
    val apkUrl: String,
    val apkName: String,
    val apkSize: Long,
    val htmlUrl: String,
)

sealed class UpdateStatus {
    object Idle : UpdateStatus()
    object Checking : UpdateStatus()
    data class Available(val release: ReleaseInfo, val isNewer: Boolean) : UpdateStatus()
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : UpdateStatus()
    data class ReadyToInstall(val file: File, val release: ReleaseInfo) : UpdateStatus()
    data class Error(val message: String) : UpdateStatus()
}

class UpdateManager(
    private val context: Context,
    private val repo: String = "gilomto-cloud/Ecomonitor"
) {

    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val _status = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val status: StateFlow<UpdateStatus> = _status.asStateFlow()

    fun getCurrentVersionName(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0"
        } catch (_: Exception) {
            "1.0"
        }
    }

    suspend fun checkForUpdates(): ReleaseInfo? = withContext(Dispatchers.IO) {
        _status.value = UpdateStatus.Checking
        try {
            val url = "https://api.github.com/repos/$repo/releases/latest"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "EcoMonitor-AndroidApp")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            val response = http.newCall(req).execute()
            val code = response.code
            val body = response.body.string()

            if (code == 404) {
                _status.value = UpdateStatus.Error("No releases found on GitHub repo $repo")
                return@withContext null
            }
            if (code != 200) {
                _status.value = UpdateStatus.Error("GitHub API HTTP $code")
                return@withContext null
            }

            val json = JSONObject(body)
            val tagName = json.optString("tag_name", "")
            val versionName = tagName.removePrefix("v").removePrefix("V").trim()
            val releaseNotes = json.optString("body", "No release notes provided.")
            val htmlUrl = json.optString("html_url", "https://github.com/$repo/releases")

            val assets = json.optJSONArray("assets")
            var apkUrl = ""
            var apkName = ""
            var apkSize = 0L

            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url", "")
                        apkName = name
                        apkSize = asset.optLong("size", 0L)
                        break
                    }
                }
            }

            if (apkUrl.isEmpty()) {
                _status.value = UpdateStatus.Error("Latest release ($tagName) has no attached APK file.")
                return@withContext null
            }

            val release = ReleaseInfo(
                tagName = tagName,
                versionName = versionName,
                releaseNotes = releaseNotes,
                apkUrl = apkUrl,
                apkName = apkName,
                apkSize = apkSize,
                htmlUrl = htmlUrl
            )

            val currentVer = getCurrentVersionName()
            val isNewer = isVersionNewer(versionName, currentVer)

            _status.value = UpdateStatus.Available(release, isNewer)
            release
        } catch (e: Exception) {
            _status.value = UpdateStatus.Error("Check failed: ${e.localizedMessage ?: e.message}")
            null
        }
    }

    suspend fun downloadApk(release: ReleaseInfo) = withContext(Dispatchers.IO) {
        try {
            _status.value = UpdateStatus.Downloading(0f, 0, release.apkSize)

            val req = Request.Builder()
                .url(release.apkUrl)
                .header("User-Agent", "EcoMonitor-AndroidApp")
                .build()

            val response = http.newCall(req).execute()
            if (!response.isSuccessful) {
                _status.value = UpdateStatus.Error("Download failed: HTTP ${response.code}")
                return@withContext
            }

            val responseBody = response.body
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.cacheDir
            val destFile = File(downloadDir, release.apkName)
            if (destFile.exists()) {
                destFile.delete()
            }

            val totalBytes = if (responseBody.contentLength() > 0) responseBody.contentLength() else release.apkSize

            responseBody.byteStream().use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var downloadedBytes = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead

                        val progress = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes.toFloat() else 0f
                        _status.value = UpdateStatus.Downloading(
                            progress = progress.coerceIn(0f, 1f),
                            downloadedBytes = downloadedBytes,
                            totalBytes = totalBytes
                        )
                    }
                    output.flush()
                }
            }

            _status.value = UpdateStatus.ReadyToInstall(destFile, release)
        } catch (e: Exception) {
            _status.value = UpdateStatus.Error("Download error: ${e.localizedMessage ?: e.message}")
        }
    }

    fun canInstallApks(): Boolean {
        return context.packageManager.canRequestPackageInstalls()
    }

    fun openInstallPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun installApk(file: File) {
        try {
            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            _status.value = UpdateStatus.Error("Failed to trigger installation: ${e.localizedMessage ?: e.message}")
        }
    }

    fun resetStatus() {
        _status.value = UpdateStatus.Idle
    }

    companion object {
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
            val clean = versionStr.trim().takeWhile { it.isDigit() || it == '.' }
            return clean.split('.').mapNotNull { it.toIntOrNull() }
        }
    }
}
