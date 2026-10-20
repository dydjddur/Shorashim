package com.shorashim

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.shorashim.core.common.LocaleHelper
import com.shorashim.core.common.shorashimViewModelFactory
import com.shorashim.core.model.AuthState
import com.shorashim.core.navigation.AuthNavHost
import com.shorashim.core.navigation.ShorashimBottomBar
import com.shorashim.core.navigation.ShorashimDestination
import com.shorashim.core.navigation.ShorashimNavHost
import com.shorashim.core.ui.component.ShorashimLoadingState
import com.shorashim.core.ui.theme.ShorashimTheme
import com.shorashim.core.update.AppUpdateDialog
import com.shorashim.core.update.AppUpdateManager
import com.shorashim.feature.auth.AuthViewModel
import androidx.lifecycle.lifecycleScope

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    private val pendingInviteToken = androidx.compose.runtime.mutableStateOf<String?>(null)
    private val appUpdateManager = AppUpdateManager()

    override fun onStart() {
        super.onStart()
        com.shorashim.core.common.AppVisibility.setForeground(true)
        appUpdateManager.onAppForeground(this)
    }

    override fun onStop() {
        super.onStop()
        com.shorashim.core.common.AppVisibility.setForeground(false)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractInviteToken(intent)
    }

    private fun extractInviteToken(intent: android.content.Intent?) {
        val uri = intent?.data ?: return
        android.util.Log.d("MainActivity", "Received deep link URI: $uri")
        // https://shorashim-invite.vercel.app/invite/{token} OR shorashim://invite/{token}
        val token = when {
            uri.scheme == "https" && uri.host == "shorashim-invite.vercel.app" -> {
                val segments = uri.pathSegments
                if (segments.size >= 2 && segments[0] == "invite") segments[1]
                else if (segments.size == 1 && segments[0] != "invite") segments[0]
                else null
            }
            uri.scheme == "shorashim" -> {
                uri.pathSegments.firstOrNull() ?: uri.lastPathSegment
            }
            else -> null
        }
        android.util.Log.d("MainActivity", "Extracted token: $token")
        if (!token.isNullOrBlank()) {
            pendingInviteToken.value = token
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        extractInviteToken(intent)
        enableEdgeToEdge()
        val appContainer = (application as ShorashimApplication).container
        appUpdateManager.start(lifecycleScope)
        setContent {
            ShorashimTheme {
                val updateInfo by appUpdateManager.updateInfo.collectAsStateWithLifecycle()
                val updateDownloadState by appUpdateManager.downloadState.collectAsStateWithLifecycle()
                updateInfo?.let { info ->
                    AppUpdateDialog(
                        updateInfo = info,
                        downloadState = updateDownloadState,
                        onUpdate = { appUpdateManager.downloadAndInstall(this@MainActivity, info) },
                        onDismiss = { dontAskAgain ->
                            appUpdateManager.dismiss(dontAskAgain)
                        }
                    )
                }

                val authViewModel: AuthViewModel = viewModel(
                    factory = shorashimViewModelFactory {
                        AuthViewModel(appContainer.authRepository)
                    },
                )
                val authState by authViewModel.authState.collectAsStateWithLifecycle()

                when (val state = authState) {
                    AuthState.Loading -> {
                        ShorashimLoadingState()
                    }
                    is AuthState.Authenticated -> {
                        // Publish this device's E2EE public key as soon as the user is signed in, so friends can
                        // message them even before they ever open the chat tab.
                        androidx.compose.runtime.LaunchedEffect(state.user.uid) {
                            appContainer.chatRepository.ensurePublicKeyPublished()
                        }
                        val navController = rememberNavController()
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route

                        val inviteToken by pendingInviteToken
                        androidx.compose.runtime.LaunchedEffect(inviteToken) {
                            val token = inviteToken
                            android.util.Log.d("MainActivity", "LaunchedEffect triggered with inviteToken: $token")
                            if (token != null) {
                                pendingInviteToken.value = null
                                try {
                                    navController.navigate(com.shorashim.core.navigation.ShorashimRoutes.invitationDeepLink(token))
                                    android.util.Log.d("MainActivity", "Navigated to deep link route: ${com.shorashim.core.navigation.ShorashimRoutes.invitationDeepLink(token)}")
                                } catch (e: Exception) {
                                    android.util.Log.e("MainActivity", "Navigation to deep link failed", e)
                                }
                            }
                        }

                        // Compare without the query part so e.g. "tehillim?deceasedName=..." keeps the app bars
                        // (it used to hide them, leaving the memorial Tehillim screen without any navigation).
                        val baseRoute = currentRoute?.substringBefore('?')
                        val shouldShowBars = baseRoute == null ||
                            baseRoute in com.shorashim.core.navigation.ShorashimRoutes.bottomBarRoutes ||
                            baseRoute in com.shorashim.core.navigation.ShorashimRoutes.topBarRoutes

                        // Android 13+: notifications (birthday / memorial reminders) are silently dropped unless the
                        // runtime permission is granted, and it was never requested.
                        val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                            androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
                        ) { }
                        androidx.compose.runtime.LaunchedEffect(Unit) {
                            if (android.os.Build.VERSION.SDK_INT >= 33 &&
                                androidx.core.content.ContextCompat.checkSelfPermission(
                                    this@MainActivity,
                                    android.Manifest.permission.POST_NOTIFICATIONS,
                                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                            ) {
                                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }

                        val userProfile by androidx.compose.runtime.produceState<com.shorashim.core.model.UserProfile?>(initialValue = null, currentRoute) {
                            value = appContainer.profileRepository.getProfile().getOrNull()
                        }

                        val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
                        val needsUsername = state.user.username.isNullOrBlank() && (userProfile?.username.isNullOrBlank())
                        var showClaimUsernameDialog by androidx.compose.runtime.remember(needsUsername) {
                            androidx.compose.runtime.mutableStateOf(needsUsername)
                        }

                        if (showClaimUsernameDialog) {
                            com.shorashim.feature.auth.ClaimUsernameDialog(
                                isOpen = true,
                                onConfirm = { chosenUsername ->
                                    coroutineScope.launch {
                                        val res = appContainer.profileRepository.setUsername(chosenUsername)
                                        if (res.isSuccess) {
                                            showClaimUsernameDialog = false
                                            appContainer.authRepository.updateCurrentUsername(chosenUsername)
                                        }
                                    }
                                }
                            )
                        }

                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
                            topBar = {
                                if (shouldShowBars) {
                                    com.shorashim.core.navigation.ShorashimTopBar(
                                        currentRoute = currentRoute,
                                        userAvatarUrl = userProfile?.avatarUrl,
                                        userDisplayName = userProfile?.displayName?.ifBlank { null }
                                            ?: userProfile?.fullName.orEmpty(),
                                        onNavigate = { route ->
                                            navController.navigate(route) {
                                                popUpTo(ShorashimDestination.FamilyTree.route) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                    )
                                }
                            },
                            bottomBar = {
                                if (shouldShowBars) {
                                    ShorashimBottomBar(
                                        currentRoute = currentRoute,
                                        onNavigate = { destination ->
                                            navController.navigate(destination.route) {
                                                popUpTo(ShorashimDestination.FamilyTree.route) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        },
                                    )
                                }
                            },
                        ) { innerPadding ->
                            ShorashimNavHost(
                                navController = navController,
                                appContainer = appContainer,
                                innerPadding = innerPadding,
                                authState = state,
                                onLogout = { authViewModel.logout() },
                            )
                        }
                    }
                    is AuthState.Unauthenticated, is AuthState.Unconfigured -> {
                        AuthNavHost(
                            authViewModel = authViewModel,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}
