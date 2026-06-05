package com.sismptm.partner.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sismptm.partner.ui.features.auth.LoginScreen
import com.sismptm.partner.ui.features.auth.RegisterScreen
import com.sismptm.partner.ui.features.home.HomeScreen
import com.sismptm.partner.ui.features.streaming.StreamingScreen
import com.sismptm.partner.ui.features.tour.RequestDetailScreen
import com.sismptm.partner.ui.features.tour.ServiceDetailScreen
import com.sismptm.partner.ui.features.tour.ServiceReadyScreen
import com.sismptm.partner.ui.features.tour.PartnerServiceSummaryScreen
import com.sismptm.partner.ui.features.profile.ProfileScreen

/**
 * Defines the navigation structure and routes for the Partner application.
 * Routes are pointing to the new features architecture.
 */
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object ServiceReady : Screen("service_ready/{serviceId}") {
        fun createRoute(serviceId: Long) = "service_ready/$serviceId"
    }
    object Streaming : Screen("streaming/{serviceId}") {
        fun createRoute(serviceId: Long) = "streaming/$serviceId"
    }
    object ServiceSummary : Screen("service_summary/{serviceId}") {
        fun createRoute(serviceId: Long) = "service_summary/$serviceId"
    }
    object RequestDetail : Screen("request_detail")
    object ServiceDetail : Screen("service_detail")
    object Profile : Screen("profile")
}

/**
 * Main navigation graph implementation using Jetpack Compose Navigation.
 */
@Composable
fun PartnerNavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
        enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
        exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(200)) },
        popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) },
        popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(200)) }
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
                },
                onNavigateToStreaming = {
                    navController.navigate(Screen.Streaming.createRoute(0L))
                }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Login.route) { inclusive = false }
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
                onNavigateToServiceReady = { serviceId ->
                    navController.navigate(Screen.ServiceReady.createRoute(serviceId))
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                }
            )
        }

        composable(
            route = Screen.ServiceReady.route,
            arguments = listOf(navArgument("serviceId") { type = NavType.LongType })
        ) { backStackEntry ->
            val serviceId = backStackEntry.arguments?.getLong("serviceId") ?: 0L
            ServiceReadyScreen(
                serviceId = serviceId,
                onReadyConfirmed = { id ->
                    navController.navigate(Screen.Streaming.createRoute(id)) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onBack = { navController.popBackStack() },
                onServiceCancelled = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = Screen.Streaming.route,
            arguments = listOf(navArgument("serviceId") { type = NavType.LongType })
        ) { backStackEntry ->
            val serviceId = backStackEntry.arguments?.getLong("serviceId") ?: 0L
            StreamingScreen(
                serviceId = serviceId,
                onBack = { 
                    // Navigate to summary after streaming ends
                    navController.navigate(Screen.ServiceSummary.createRoute(serviceId)) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = Screen.ServiceSummary.route,
            arguments = listOf(navArgument("serviceId") { type = NavType.LongType })
        ) { backStackEntry ->
            val serviceId = backStackEntry.arguments?.getLong("serviceId") ?: 0L
            PartnerServiceSummaryScreen(
                serviceId = serviceId,
                onBackToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Screen.RequestDetail.route) {
            RequestDetailScreen(
                onAccept = { navController.navigate(Screen.ServiceDetail.route) },
                onReject = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ServiceDetail.route) {
            ServiceDetailScreen(
                onComplete = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
