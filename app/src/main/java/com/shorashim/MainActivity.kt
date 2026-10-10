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
import com.shorashim.feature.auth.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val appContainer = (application as ShorashimApplication).container
        setContent {
            ShorashimTheme {
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
                        val navController = rememberNavController()
                        val navBackStackEntry by navController.currentBackStackEntryAsState()
                        val currentRoute = navBackStackEntry?.destination?.route
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            bottomBar = {
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
