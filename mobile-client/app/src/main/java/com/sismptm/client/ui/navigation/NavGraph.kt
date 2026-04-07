package com.sismptm.client.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sismptm.client.ui.screens.HomeScreen
import com.sismptm.client.ui.screens.LoginScreen
import com.sismptm.client.ui.screens.PartnerSearchScreen
import com.sismptm.client.ui.screens.RegisterScreen
import com.sismptm.client.ui.screens.RequestScreen
import com.sismptm.client.ui.screens.ServiceDetailScreen
import com.sismptm.client.ui.screens.ServiceWaitingScreen
import com.sismptm.client.ui.screens.WelcomeScreen

/**
 * Sealed class representing all navigation routes in the mobile-client application.
 * Each object corresponds to a screen in the navigation graph.
 */
sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object PartnerSearch : Screen("partner_search")
    object Solicitud : Screen("solicitud")
    object ServiceDetail : Screen("service_detail")
    object ServiceWaiting : Screen("service_waiting/{serviceId}") {
        fun createRoute(serviceId: Long): String = "service_waiting/$serviceId"
    }
}

/**
 * Main navigation graph composable for the mobile-client application.
 * Defines the navigation structure and relationships between all screens.
 * Manages the NavController and handles navigation between Welcome, Login, Register,
 * Home, PartnerSearch, Solicitud, and ServiceDetail screens.
 */
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
                onNavigateToPartnerSearch = {
                    navController.navigate(Screen.PartnerSearch.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
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
            RequestScreen(
                onViewDetails = { serviceId ->
                    navController.navigate(Screen.ServiceWaiting.createRoute(serviceId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ServiceWaiting.route,
            arguments = listOf(navArgument("serviceId") { type = NavType.LongType })
        ) { backStackEntry ->
            val serviceId = backStackEntry.arguments?.getLong("serviceId") ?: 0L
            ServiceWaitingScreen(
                serviceId = serviceId,
                onBackHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
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
