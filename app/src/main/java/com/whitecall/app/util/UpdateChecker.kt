package com.whitecall.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.whitecall.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(
    val hasUpdate: Boolean,
    val latestVersion: String,
    val currentVersion: String,
    val releaseTitle: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val releaseUrl: String
)

object UpdateChecker {

    private const val GITHUB_API_LATEST_RELEASE =
        "https://api.github.com/repos/EvgeniyKrasnyanskiy/WhiteCall/releases/latest"

    suspend fun checkLatestRelease(): Result<UpdateInfo> = withContext(Dispatchers.IO) {
        try {
            val url = URL(GITHUB_API_LATEST_RELEASE)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "WhiteCall-Android-App")
                connectTimeout = 8000
                readTimeout = 8000
            }

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)

                val tagName = json.optString("tag_name", "").trimStart('v', 'V')
                val title = json.optString("name", tagName)
                val body = json.optString("body", "")
                val htmlUrl = json.optString("html_url", "https://github.com/EvgeniyKrasnyanskiy/WhiteCall/releases")

                var apkDownloadUrl = htmlUrl
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkDownloadUrl = asset.optString("browser_download_url", htmlUrl)
                            break
                        }
                    }
                }

                val currentVersion = BuildConfig.VERSION_NAME
                val hasUpdate = isNewerVersion(remote = tagName, current = currentVersion)

                Result.success(
                    UpdateInfo(
                        hasUpdate = hasUpdate,
                        latestVersion = tagName,
                        currentVersion = currentVersion,
                        releaseTitle = title,
                        releaseNotes = body,
                        downloadUrl = apkDownloadUrl,
                        releaseUrl = htmlUrl
                    )
                )
            } else if (responseCode == 404) {
                // No releases published yet on GitHub repo
                Result.success(
                    UpdateInfo(
                        hasUpdate = false,
                        latestVersion = BuildConfig.VERSION_NAME,
                        currentVersion = BuildConfig.VERSION_NAME,
                        releaseTitle = "",
                        releaseNotes = "",
                        downloadUrl = "",
                        releaseUrl = ""
                    )
                )
            } else {
                Result.failure(Exception("HTTP $responseCode: ${connection.responseMessage}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun openDownloadUrl(context: Context, urlString: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlString)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    private fun isNewerVersion(remote: String, current: String): Boolean {
        val remoteClean = remote.trim().trimStart('v', 'V')
        val currentClean = current.trim().trimStart('v', 'V')
        if (remoteClean.isBlank() || currentClean.isBlank() || remoteClean == currentClean) return false

        val remoteParts = remoteClean.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = currentClean.split(".").mapNotNull { it.toIntOrNull() }

        val maxParts = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxParts) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
