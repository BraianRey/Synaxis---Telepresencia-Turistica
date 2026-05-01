package com.sismptm.client.ui.navigation

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
import com.sismptm.client.ui.features.auth.LoginScreen
import com.sismptm.client.ui.features.map.PartnerSearchScreen
import com.sismptm.client.ui.features.auth.RegisterScreen
import com.sismptm.client.ui.features.tour.RequestScreen
import com.sismptm.client.ui.features.tour.ServiceDetailScreen
import com.sismptm.client.ui.features.tour.ServiceWaitingScreen
import com.sismptm.client.ui.features.streaming.StreamingScreen
import com.sismptm.client.ui.features.auth.WelcomeScreen
import com.sismptm.client.ui.features.map.MapServiceScreen
import com.sismptm.client.ui.features.home.HomeScreen

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
    object Request : Screen("service_request")
    object ServiceDetail : Screen("service_detail")
    object MapService : Screen("map_service")
    object ServiceWaiting : Screen("service_waiting/{serviceId}") {
        fun createRoute(serviceId: Long): String = "service_waiting/$serviceId"
    }
    object Streaming : Screen("streaming/{serviceId}") {
        fun createRoute(serviceId: Long): String = "streaming/$serviceId"
    }
}

/**
 * Main navigation graph composable for the mobile-client application.
 * Defines the navigation structure and relationships between all screens.
 * Manages the NavController and handles navigation between all application screens.
 */
@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Welcome.route,
        enterTransition = { slideInHorizontally(tween(300)) { it } + fadeIn(tween(300)) },
        exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(200)) },
        popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(300)) },
        popExitTransition = { slideOutHorizontally(tween(300)) { it } + fadeOut(tween(200)) }
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
                },
                onNavigateToStreaming = {
                    // Default for testing if needed
                    navController.navigate(Screen.Streaming.createRoute(0L))
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
                onOpenServiceWaiting = { serviceId ->
                    navController.navigate(Screen.ServiceWaiting.createRoute(serviceId))
                },
                onNavigateToMapService = {  // ← AGREGAR ESTE PARÁMETRO
                    navController.navigate(Screen.MapService.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.MapService.route) {
            MapServiceScreen(
                onBack = {
                    navController.popBackStack()
                },
                onServiceCreated = { serviceId ->
                    navController.navigate(Screen.ServiceWaiting.createRoute(serviceId)) {
                        popUpTo(Screen.MapService.route) { inclusive = true }
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
                    navController.navigate(Screen.Request.route)
                }
            )
        }

        composable(Screen.Request.route) {
            RequestScreen(
                onViewDetails = { serviceId ->
                    navController.navigate(Screen.ServiceWaiting.createRoute(serviceId)) {
                        popUpTo(Screen.Request.route) { inclusive = true }
                    }
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
                },
                onNavigateToStreaming = { sid ->
                    navController.navigate(Screen.Streaming.createRoute(sid)) {
                        popUpTo(Screen.ServiceWaiting.route) { inclusive = true }
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

        composable(
            route = Screen.Streaming.route,
            arguments = listOf(navArgument("serviceId") { type = NavType.LongType })
        ) { backStackEntry ->
            val serviceId = backStackEntry.arguments?.getLong("serviceId") ?: 0L
            StreamingScreen(
                serviceId = serviceId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

