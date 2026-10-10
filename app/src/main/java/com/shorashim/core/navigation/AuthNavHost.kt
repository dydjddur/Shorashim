package com.shorashim.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shorashim.feature.auth.AuthViewModel
import com.shorashim.feature.auth.ForgotPasswordRoute
import com.shorashim.feature.auth.LoginRoute
import com.shorashim.feature.auth.RegisterRoute

/**
 * Navigation graph for unauthenticated flows (Login, Register, Forgot Password).
 */
@Composable
fun AuthNavHost(
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = ShorashimRoutes.LOGIN,
        modifier = modifier,
    ) {
        composable(ShorashimRoutes.LOGIN) {
            LoginRoute(
                viewModel = authViewModel,
                onNavigateToRegister = {
                    navController.navigate(ShorashimRoutes.REGISTER)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(ShorashimRoutes.FORGOT_PASSWORD)
                },
                onLoginSuccess = {
                    // Auth state flow observer handles navigation automatically
                },
            )
        }

        composable(ShorashimRoutes.REGISTER) {
            RegisterRoute(
                viewModel = authViewModel,
                onNavigateBackToLogin = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    // Auth state flow observer handles navigation automatically
                },
            )
        }

        composable(ShorashimRoutes.FORGOT_PASSWORD) {
            ForgotPasswordRoute(
                viewModel = authViewModel,
                onNavigateBackToLogin = {
                    navController.popBackStack()
                },
            )
        }
    }
}
