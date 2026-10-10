package com.shorashim.core.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.shorashim.core.common.shorashimViewModelFactory
import com.shorashim.core.model.AuthState
import com.shorashim.data.repositories.AppContainer
import com.shorashim.feature.family.AddEditPersonRoute
import com.shorashim.feature.family.AddEditPersonViewModel
import com.shorashim.feature.family.CreateFamilyRoute
import com.shorashim.feature.family.CreateFamilyViewModel
import com.shorashim.feature.family.FamilyManagementRoute
import com.shorashim.feature.family.FamilyManagementViewModel
import com.shorashim.feature.family.MyFamiliesRoute
import com.shorashim.feature.family.MyFamiliesViewModel
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
        // Main Family Tab: Shows "המשפחות שלי"
        composable(ShorashimDestination.FamilyTree.route) {
            val viewModel: MyFamiliesViewModel = viewModel(
                factory = shorashimViewModelFactory {
                    MyFamiliesViewModel(appContainer.familyRepository)
                },
            )
            MyFamiliesRoute(
                viewModel = viewModel,
                onNavigateToCreateFamily = {
                    navController.navigate(ShorashimRoutes.CREATE_FAMILY)
                },
                onNavigateToFamilyManagement = { familyId ->
                    navController.navigate(ShorashimRoutes.familyManagement(familyId))
                },
            )
        }

        // Create Family Screen
        composable(ShorashimRoutes.CREATE_FAMILY) {
            val viewModel: CreateFamilyViewModel = viewModel(
                factory = shorashimViewModelFactory {
                    CreateFamilyViewModel(appContainer.familyRepository)
                },
            )
            CreateFamilyRoute(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onFamilyCreated = { newFamilyId ->
                    navController.navigate(ShorashimRoutes.familyManagement(newFamilyId)) {
                        popUpTo(ShorashimDestination.FamilyTree.route)
                    }
                },
            )
        }

        // Family Management Screen
        composable(
            route = ShorashimRoutes.FAMILY_MANAGEMENT,
            arguments = listOf(
                navArgument("familyId") { type = NavType.StringType },
            ),
        ) { backStackEntry ->
            val familyId = backStackEntry.arguments?.getString("familyId").orEmpty()
            val viewModel: FamilyManagementViewModel = viewModel(
                key = "FamilyMgmt_$familyId",
                factory = shorashimViewModelFactory {
                    FamilyManagementViewModel(
                        familyId = familyId,
                        familyRepository = appContainer.familyRepository,
                        personRepository = appContainer.personRepository,
                        relationshipRepository = appContainer.relationshipRepository,
                    )
                },
            )
            FamilyManagementRoute(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddPerson = { fId ->
                    navController.navigate(ShorashimRoutes.addPerson(fId))
                },
                onNavigateToEditPerson = { fId, pId ->
                    navController.navigate(ShorashimRoutes.editPerson(fId, pId))
                },
                onNavigateToCreateFamily = {
                    navController.navigate(ShorashimRoutes.CREATE_FAMILY)
                },
            )
        }

        // Add or Edit Person Screen
        composable(
            route = ShorashimRoutes.ADD_EDIT_PERSON,
            arguments = listOf(
                navArgument("familyId") { type = NavType.StringType },
                navArgument("personId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            val familyId = backStackEntry.arguments?.getString("familyId").orEmpty()
            val personId = backStackEntry.arguments?.getString("personId")
            val viewModel: AddEditPersonViewModel = viewModel(
                key = "AddEditPerson_${familyId}_${personId ?: "new"}",
                factory = shorashimViewModelFactory {
                    AddEditPersonViewModel(
                        familyId = familyId,
                        personId = personId,
                        personRepository = appContainer.personRepository,
                    )
                },
            )
            AddEditPersonRoute(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
            )
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
