package com.shorashim.core.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Progress of an in-app update download. */
sealed interface UpdateDownloadState {
    data object Idle : UpdateDownloadState
    /** [progress] is 0..1, or null while the size is still unknown. */
    data class Downloading(val progress: Float?) : UpdateDownloadState
    data class ReadyToInstall(val file: File) : UpdateDownloadState
    data class Failed(val message: String) : UpdateDownloadState
}

/**
 * Checks GitHub releases for a newer version and installs it from inside the app.
 *
 * Previously "Update" only opened the APK URL in a browser, the check ran once per launch (a release published while
 * the app was open was never noticed), and a dismissed prompt came back every 10 minutes. Now the APK is downloaded
 * with DownloadManager (progress shown in the dialog) and handed straight to the system installer.
 */
class AppUpdateManager {
    private val _updateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val updateInfo: StateFlow<AppUpdateInfo?> = _updateInfo.asStateFlow()

    private val _downloadState = MutableStateFlow<UpdateDownloadState>(UpdateDownloadState.Idle)
    val downloadState: StateFlow<UpdateDownloadState> = _downloadState.asStateFlow()

    var isDismissedForSession: Boolean = false
        private set

    /** Version the user tapped "later" on; not offered again during this app session. */
    private var snoozedVersion: String? = null
    private var lastCheckAt = 0L
    private var checkJob: Job? = null
    private var downloadJob: Job? = null
    private var scope: CoroutineScope? = null
    /** The user was sent to "install unknown apps" settings; install as soon as they come back. */
    private var installPendingPermission = false

    fun start(scope: CoroutineScope, checkIntervalMs: Long = 30 * 60 * 1000L) {
        this.scope = scope
        checkJob?.cancel()
        checkJob = scope.launch {
            checkNow()
            while (isActive) {
                delay(checkIntervalMs)
                checkNow()
            }
        }
    }

    /**
     * Called when the app returns to the foreground: re-checks (at most every 15 minutes) so a release published
     * while the app was in the background is offered, and resumes an install that waited for permission.
     */
    fun onAppForeground(context: Context) {
        if (installPendingPermission) {
            val state = _downloadState.value
            if (state is UpdateDownloadState.ReadyToInstall && canInstallPackages(context)) {
                installPendingPermission = false
                launchInstaller(context, state.file)
            }
        }
        val now = System.currentTimeMillis()
        if (now - lastCheckAt > 15 * 60 * 1000L) {
            scope?.launch { checkNow() }
        }
    }

    suspend fun checkNow() {
        lastCheckAt = System.currentTimeMillis()
        if (isDismissedForSession) return
        try {
            val info = AppUpdateChecker.checkForUpdate()
            if (info != null && info.latestVersion != snoozedVersion && _downloadState.value is UpdateDownloadState.Idle) {
                _updateInfo.value = info
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check for update", e)
        }
    }

    fun dismiss(dontAskAgainForSession: Boolean) {
        if (dontAskAgainForSession) {
            isDismissedForSession = true
        }
        snoozedVersion = _updateInfo.value?.latestVersion
        _updateInfo.value = null
        downloadJob?.cancel()
        _downloadState.value = UpdateDownloadState.Idle
    }

    fun resetSessionDismissal() {
        isDismissedForSession = false
        snoozedVersion = null
    }

    /** Downloads the release APK and opens the system installer when done. */
    fun downloadAndInstall(context: Context, info: AppUpdateInfo) {
        val appContext = context.applicationContext
        val current = _downloadState.value
        if (current is UpdateDownloadState.Downloading) return
        if (current is UpdateDownloadState.ReadyToInstall) {
            install(context, current.file)
            return
        }
        val launchScope = scope ?: return
        downloadJob?.cancel()
        downloadJob = launchScope.launch {
            _downloadState.value = UpdateDownloadState.Downloading(null)
            try {
                val file = download(appContext, info)
                _downloadState.value = UpdateDownloadState.ReadyToInstall(file)
                install(context, file)
            } catch (t: Throwable) {
                if (t is kotlinx.coroutines.CancellationException) throw t
                Log.e(TAG, "Update download failed", t)
                _downloadState.value = UpdateDownloadState.Failed(t.message ?: "ההורדה נכשלה")
            }
        }
    }

    private suspend fun download(context: Context, info: AppUpdateInfo): File = withContext(Dispatchers.IO) {
        val dir = File(context.getExternalFilesDir(null), UPDATES_DIR).apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() } // drop older downloads
        val fileName = "shorashim-${info.latestVersion}.apk"
        val target = File(dir, fileName)

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(info.downloadUrl))
            .setTitle("שורשים ${info.latestVersion}")
            .setDescription("מוריד עדכון")
            .setMimeType(APK_MIME)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalFilesDir(context, null, "$UPDATES_DIR/$fileName")
        val id = dm.enqueue(request)

        try {
            while (true) {
                val query = DownloadManager.Query().setFilterById(id)
                dm.query(query).use { c ->
                    if (!c.moveToFirst()) error("ההורדה בוטלה")
                    val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val done = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> return@withContext target
                        DownloadManager.STATUS_FAILED -> {
                            val reason = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                            error("ההורדה נכשלה (קוד $reason)")
                        }
                        else -> _downloadState.value = UpdateDownloadState.Downloading(
                            if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else null,
                        )
                    }
                }
                delay(400)
            }
            @Suppress("UNREACHABLE_CODE")
            target
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) dm.remove(id)
            throw t
        }
    }

    private fun install(context: Context, file: File) {
        if (!canInstallPackages(context)) {
            // Android 8+: the user must allow this app to install updates once.
            installPendingPermission = true
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(intent) }
                .onFailure { launchInstaller(context, file) }
            return
        }
        launchInstaller(context, file)
    }

    private fun launchInstaller(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", file)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, APK_MIME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure {
                Log.e(TAG, "Could not open the installer", it)
                _downloadState.value = UpdateDownloadState.Failed("לא ניתן לפתוח את מתקין העדכון")
            }
    }

    private fun canInstallPackages(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    private companion object {
        const val TAG = "AppUpdateManager"
        const val UPDATES_DIR = "updates"
        const val APK_MIME = "application/vnd.android.package-archive"
    }
}
