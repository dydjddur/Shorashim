package com.shorashim

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
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
                updateInfo?.let { info ->
                    AppUpdateDialog(
                        updateInfo = info,
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

                        val shouldShowBars = currentRoute == null ||
                            currentRoute in com.shorashim.core.navigation.ShorashimRoutes.bottomBarRoutes ||
                            currentRoute in com.shorashim.core.navigation.ShorashimRoutes.topBarRoutes

                        val userProfile by androidx.compose.runtime.produceState<com.shorashim.core.model.UserProfile?>(initialValue = null, currentRoute) {
                            value = appContainer.profileRepository.getProfile().getOrNull()
                        }

                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            topBar = {
                                if (shouldShowBars) {
                                    com.shorashim.core.navigation.ShorashimTopBar(
                                        currentRoute = currentRoute,
                                        userAvatarUrl = userProfile?.avatarUrl,
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
