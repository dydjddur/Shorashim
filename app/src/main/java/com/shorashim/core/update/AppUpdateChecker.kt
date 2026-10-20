package com.shorashim.core.update

import android.util.Log
import com.shorashim.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AppUpdateChecker {
    private const val TAG = "AppUpdateChecker"
    private const val VERCEL_VERSION_URL = "https://shorashim-invite.vercel.app/api/version"
    private const val GITHUB_LATEST_RELEASE_URL = "https://api.github.com/repos/dydjddur/Shorashim/releases/latest"

    /**
     * Checks if a newer version is available.
     * Returns AppUpdateInfo if an update is available, or null if up to date or on network failure.
     */
    suspend fun checkForUpdate(currentVersion: String = BuildConfig.VERSION_NAME): AppUpdateInfo? = withContext(Dispatchers.IO) {
        // 1. GitHub releases directly: this is the source of truth for releases. (The Vercel proxy used to be asked
        //    first, but when GitHub rate-limited it, it answered with a hard-coded "1.0.0", which the app took as
        //    "up to date" and never looked further - so new releases were silently missed.)
        val fromGitHub = runCatching { fetchFromGitHub() }
            .onFailure { Log.w(TAG, "GitHub update check failed, trying Vercel", it) }
            .getOrNull()
        // 2. Vercel proxy only as a fallback (e.g. GitHub API rate limit on this network).
        val info = fromGitHub ?: runCatching { fetchFromVercel() }
            .onFailure { Log.w(TAG, "Vercel update check failed", it) }
            .getOrNull()

        if (info != null && isNewerVersion(currentVersion, info.latestVersion)) {
            Log.d(TAG, "Update available: current=$currentVersion, latest=${info.latestVersion}")
            return@withContext info
        }
        Log.d(TAG, "No update: current=$currentVersion, latest=${info?.latestVersion}")
        null
    }

    private fun fetchFromVercel(): AppUpdateInfo? {
        val url = URL(VERCEL_VERSION_URL)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(jsonStr)
                val version = obj.optString("version").ifBlank { obj.optString("tagName").removePrefix("v").removePrefix("V") }
                val downloadUrl = obj.optString("downloadUrl")
                val tagName = obj.optString("tagName", "v$version")
                val title = obj.optString("name", "")
                val body = obj.optString("body", "")
                if (version.isNotBlank() && downloadUrl.isNotBlank()) {
                    return AppUpdateInfo(
                        latestVersion = version,
                        tagName = tagName,
                        downloadUrl = downloadUrl,
                        releaseTitle = title,
                        releaseNotes = body
                    )
                }
            }
        } finally {
            conn.disconnect()
        }
        return null
    }

    private fun fetchFromGitHub(): AppUpdateInfo? {
        val url = URL(GITHUB_LATEST_RELEASE_URL)
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("User-Agent", "Shorashim-App")
            setRequestProperty("Accept", "application/vnd.github.v3+json")
        }
        try {
            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(jsonStr)
                val tagName = obj.optString("tag_name", "")
                val version = tagName.removePrefix("v").removePrefix("V")
                val title = obj.optString("name", "")
                val body = obj.optString("body", "")

                var downloadUrl = ""
                val assets = obj.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val assetName = asset.optString("name", "")
                        if (assetName.endsWith(".apk", ignoreCase = true)) {
                            downloadUrl = asset.optString("browser_download_url", "")
                            break
                        }
                    }
                }
                if (downloadUrl.isBlank() && tagName.isNotBlank()) {
                    downloadUrl = "https://github.com/dydjddur/Shorashim/releases/download/$tagName/shorashim-$tagName.apk"
                }

                if (version.isNotBlank() && downloadUrl.isNotBlank()) {
                    return AppUpdateInfo(
                        latestVersion = version,
                        tagName = tagName,
                        downloadUrl = downloadUrl,
                        releaseTitle = title,
                        releaseNotes = body
                    )
                }
            }
        } finally {
            conn.disconnect()
        }
        return null
    }

    /**
     * Compares two semantic version strings (e.g. "1.0.0" and "1.0.1").
     * Returns true if latest > current.
     */
    fun isNewerVersion(current: String, latest: String): Boolean {
        val curParts = current.trim().removePrefix("v").removePrefix("V").split(".")
            .map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }
        val latParts = latest.trim().removePrefix("v").removePrefix("V").split(".")
            .map { it.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }

        val maxLen = maxOf(curParts.size, latParts.size)
        for (i in 0 until maxLen) {
            val c = curParts.getOrElse(i) { 0 }
            val l = latParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }
}
