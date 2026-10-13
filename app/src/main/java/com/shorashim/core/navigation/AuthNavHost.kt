package com.shorashim.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.shorashim.feature.auth.AuthViewModel
import com.shorashim.feature.auth.EmailVerificationPendingScreen
import com.shorashim.feature.auth.ForgotPasswordRoute
import com.shorashim.feature.auth.LoginRoute
import com.shorashim.feature.auth.RegisterRoute

/**
 * Navigation graph for unauthenticated flows (Login, Register, Forgot Password, Email Verification).
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
                onRegisterSuccess = { email ->
                    if (email.isNotBlank()) {
                        navController.navigate(ShorashimRoutes.verificationPending(email)) {
                            popUpTo(ShorashimRoutes.REGISTER) { inclusive = true }
                        }
                    } else {
                        // Registration without email: return to login screen
                        navController.popBackStack()
                    }
                },
            )
        }

        composable(
            route = ShorashimRoutes.VERIFICATION_PENDING,
            arguments = listOf(
                navArgument("email") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email").orEmpty()
            EmailVerificationPendingScreen(
                email = email,
                onNavigateToLogin = {
                    navController.navigate(ShorashimRoutes.LOGIN) {
                        popUpTo(ShorashimRoutes.LOGIN) { inclusive = true }
                    }
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
