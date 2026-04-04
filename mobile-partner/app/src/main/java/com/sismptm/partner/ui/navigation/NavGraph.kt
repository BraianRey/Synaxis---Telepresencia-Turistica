package com.sismptm.partner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sismptm.partner.ui.screens.HomeScreen
import com.sismptm.partner.ui.screens.LoginScreen
import com.sismptm.partner.ui.screens.RegisterScreen
import com.sismptm.partner.ui.screens.SolicitudDetailScreen
import com.sismptm.partner.ui.screens.ServicioDetailScreen

/**
 * Sealed class representing all navigation routes in the mobile-partner application.
 * Each object corresponds to a screen in the partner navigation graph.
 */
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object SolicitudDetail : Screen("solicitud_detail")
    object ServicioDetail : Screen("servicio_detail")
}

/**
 * Main navigation graph composable for the mobile-partner application.
 * Defines the navigation structure and relationships between all screens.
 * Manages the NavController and handles navigation between Login, Register, Home,
 * SolicitudDetail, and ServicioDetail screens.
 */
@Composable
fun PartnerNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
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
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
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
                onRequestTour = {
                    navController.navigate(Screen.SolicitudDetail.route)
                }
            )
        }

        composable(Screen.SolicitudDetail.route) {
            SolicitudDetailScreen(
                onAccept = { navController.navigate(Screen.ServicioDetail.route) },
                onReject = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ServicioDetail.route) {
            ServicioDetailScreen(
                onComplete = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}