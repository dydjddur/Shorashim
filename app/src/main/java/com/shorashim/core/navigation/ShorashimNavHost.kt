package com.shorashim.core.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.shorashim.core.common.shorashimViewModelFactory
import com.shorashim.data.repositories.AppContainer
import com.shorashim.core.model.AuthState
import com.shorashim.feature.familytree.FamilyTreeRoute
import com.shorashim.feature.familytree.FamilyTreeViewModel
import com.shorashim.feature.memorials.MemorialsRoute
import com.shorashim.feature.memorials.MemorialsViewModel
import com.shorashim.feature.settings.SettingsRoute
import com.shorashim.feature.tehillim.TehillimRoute
import com.shorashim.feature.tehillim.TehillimViewModel

@Composable
fun ShorashimNavHost(
    navController: NavHostController,
    appContainer: AppContainer,
    innerPadding: PaddingValues,
    authState: AuthState = AuthState.Unauthenticated,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = ShorashimDestination.FamilyTree.route,
        modifier = modifier.padding(innerPadding),
    ) {
        composable(ShorashimDestination.FamilyTree.route) {
            val viewModel: FamilyTreeViewModel = viewModel(
                factory = shorashimViewModelFactory {
                    FamilyTreeViewModel(appContainer.familyRepository)
                },
            )
            FamilyTreeRoute(viewModel = viewModel)
        }
        composable(ShorashimDestination.Memorials.route) {
            val viewModel: MemorialsViewModel = viewModel(
                factory = shorashimViewModelFactory {
                    MemorialsViewModel(appContainer.memorialRepository)
                },
            )
            MemorialsRoute(viewModel = viewModel)
        }
        composable(ShorashimDestination.Tehillim.route) {
            val viewModel: TehillimViewModel = viewModel(
                factory = shorashimViewModelFactory {
                    TehillimViewModel(appContainer.psalmsRepository)
                },
            )
            TehillimRoute(viewModel = viewModel)
        }
        composable(ShorashimDestination.Settings.route) {
            SettingsRoute(
                authState = authState,
                onLogout = onLogout,
            )
        }
    }
}
