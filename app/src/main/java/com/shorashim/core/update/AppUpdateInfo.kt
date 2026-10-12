package com.shorashim.core.update

data class AppUpdateInfo(
    val latestVersion: String,
    val tagName: String,
    val downloadUrl: String,
    val releaseTitle: String = "",
    val releaseNotes: String = "",
)
