package com.sismptm.client.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sismptm.client.ui.screens.HomeScreen
import com.sismptm.client.ui.screens.LoginScreen
import com.sismptm.client.ui.screens.PartnerSearchScreen
import com.sismptm.client.ui.screens.RegisterScreen
import com.sismptm.client.ui.screens.SolicitudScreen
import com.sismptm.client.ui.screens.ServiceDetailScreen
import com.sismptm.client.ui.screens.WelcomeScreen

sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object PartnerSearch : Screen("partner_search")
    object Solicitud : Screen("solicitud")
    object ServiceDetail : Screen("service_detail")
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Welcome.route
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onGetStarted = {
                    navController.navigate(Screen.Register.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onSignIn = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onGoToPartnerSearch = {
                    navController.navigate(Screen.PartnerSearch.route)
                }
            )
        }

        composable(Screen.PartnerSearch.route) {
            PartnerSearchScreen(
                onCancelSearch = {
                    navController.popBackStack()
                },
                onRequestTour = {
                    navController.navigate(Screen.Solicitud.route)
                }
            )
        }

        composable(Screen.Solicitud.route) {
            SolicitudScreen(
                onViewDetails = { navController.navigate(Screen.ServiceDetail.route) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ServiceDetail.route) {
            ServiceDetailScreen(
                onConfirm = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
