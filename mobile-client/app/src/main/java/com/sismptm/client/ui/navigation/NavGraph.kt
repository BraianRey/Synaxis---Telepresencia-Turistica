package com.sismptm.client.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
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
import com.sismptm.client.ui.features.tour.ServiceSummaryScreen
import com.sismptm.client.ui.features.streaming.StreamingScreen
import com.sismptm.client.ui.features.auth.WelcomeScreen
import com.sismptm.client.ui.features.map.MapServiceScreen
import com.sismptm.client.ui.features.home.HomeScreen
import com.sismptm.client.ui.features.reservation.ReserveServiceScreen

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
    object ServiceSummary : Screen("service_summary/{serviceId}") {
        fun createRoute(serviceId: Long): String = "service_summary/$serviceId"
    }
    object Reserve : Screen("reserve/{lat}/{lon}/{description}") {
        fun createRoute(lat: Double, lon: Double, description: String): String =
            "reserve/$lat/$lon/${java.net.URLEncoder.encode(description, "UTF-8")}"
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
            WelcomeComposable(navController)
        }
        composable(Screen.Login.route) {
            LoginComposable(navController)
        }
        composable(Screen.Register.route) {
            RegisterComposable(navController)
        }
        composable(Screen.Home.route) {
            HomeComposable(navController)
        }

        composable(
            route = Screen.MapService.route + "?mode={mode}",
            arguments = listOf(navArgument("mode") {
                type = NavType.StringType
                defaultValue = "request"
            })
        ) { backStackEntry ->
            MapServiceComposable(navController, backStackEntry)
        }

        composable(
            route = Screen.Reserve.route,
            arguments = listOf(
                navArgument("lat") { type = NavType.FloatType },
                navArgument("lon") { type = NavType.FloatType },
                navArgument("description") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            ReserveComposable(navController, backStackEntry)
        }

        composable(Screen.PartnerSearch.route) {
            PartnerSearchComposable(navController)
        }

        composable(Screen.Request.route) {
            RequestComposable(navController)
        }

        composable(
            route = Screen.ServiceWaiting.route,
            arguments = listOf(navArgument("serviceId") { type = NavType.LongType })
        ) { backStackEntry ->
            ServiceWaitingComposable(navController, backStackEntry)
        }

        composable(Screen.ServiceDetail.route) {
            ServiceDetailComposable(navController)
        }

        composable(
            route = Screen.Streaming.route,
            arguments = listOf(navArgument("serviceId") { type = NavType.LongType })
        ) { backStackEntry ->
            StreamingComposable(navController, backStackEntry)
        }

        composable(
            route = Screen.ServiceSummary.route,
            arguments = listOf(navArgument("serviceId") { type = NavType.LongType })
        ) { backStackEntry ->
            ServiceSummaryComposable(navController, backStackEntry)
        }
    }
}

@Composable
private fun WelcomeComposable(navController: NavHostController) {
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
            navController.navigate(Screen.Streaming.createRoute(0L))
        }
    )
}

@Composable
private fun LoginComposable(navController: NavHostController) {
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

@Composable
private fun RegisterComposable(navController: NavHostController) {
    RegisterScreen(
        onRegisterSuccess = {
            navController.navigate(Screen.Login.route) {
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

@Composable
private fun HomeComposable(navController: NavHostController) {
    HomeScreen(
        onNavigateToPartnerSearch = {
            navController.navigate(Screen.PartnerSearch.route)
        },
        onOpenServiceWaiting = { serviceId ->
            navController.navigate(Screen.ServiceWaiting.createRoute(serviceId))
        },
        onNavigateToMapService = {
            navController.navigate(Screen.MapService.route)
        },
        onNavigateToReserveMap = {
            navController.navigate(Screen.MapService.route + "?mode=reserve")
        },
        onLogout = {
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    )
}

@Composable
private fun MapServiceComposable(navController: NavHostController, backStackEntry: NavBackStackEntry) {
    val mode = backStackEntry.arguments?.getString("mode") ?: "request"
    val isReserveMode = mode == "reserve"
    MapServiceScreen(
        reserveMode = isReserveMode,
        onBack = {
            if (!navController.popBackStack()) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Home.route) { inclusive = false }
                    launchSingleTop = true
                }
            }
        },
        onServiceCreated = { serviceId ->
            navController.navigate(Screen.ServiceWaiting.createRoute(serviceId)) {
                popUpTo(Screen.MapService.route) { inclusive = true }
            }
        },
        onNavigateToReserve = { lat, lon, description ->
            navController.navigate(Screen.Reserve.createRoute(lat, lon, description)) {
                popUpTo(Screen.MapService.route) { inclusive = true }
            }
        }
    )
}

@Composable
private fun ReserveComposable(navController: NavHostController, backStackEntry: NavBackStackEntry) {
    val lat = backStackEntry.arguments?.getFloat("lat")?.toDouble() ?: 0.0
    val lon = backStackEntry.arguments?.getFloat("lon")?.toDouble() ?: 0.0
    val description = backStackEntry.arguments?.getString("description") ?: ""
    ReserveServiceScreen(
        location = com.sismptm.client.ui.features.map.MapLocation(lat = lat, lon = lon),
        description = java.net.URLDecoder.decode(description, "UTF-8"),
        onBack = { navController.popBackStack() },
        onReservationCreated = { serviceId ->
            navController.navigate(Screen.ServiceWaiting.createRoute(serviceId)) {
                popUpTo(Screen.Reserve.route) { inclusive = true }
            }
        }
    )
}

@Composable
private fun PartnerSearchComposable(navController: NavHostController) {
    PartnerSearchScreen(
        onCancelSearch = { navController.popBackStack() },
        onRequestTour = { navController.navigate(Screen.Request.route) }
    )
}

@Composable
private fun RequestComposable(navController: NavHostController) {
    RequestScreen(
        onViewDetails = { serviceId ->
            navController.navigate(Screen.ServiceWaiting.createRoute(serviceId)) {
                popUpTo(Screen.Request.route) { inclusive = true }
            }
        },
        onBack = { navController.popBackStack() }
    )
}

@Composable
private fun ServiceWaitingComposable(navController: NavHostController, backStackEntry: NavBackStackEntry) {
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
        },
        onNavigateToSummary = { sid ->
            navController.navigate(Screen.ServiceSummary.createRoute(sid)) {
                popUpTo(Screen.ServiceWaiting.route) { inclusive = true }
            }
        }
    )
}

@Composable
private fun ServiceDetailComposable(navController: NavHostController) {
    ServiceDetailScreen(onConfirm = { navController.popBackStack() }, onBack = { navController.popBackStack() })
}

@Composable
private fun StreamingComposable(navController: NavHostController, backStackEntry: NavBackStackEntry) {
    val serviceId = backStackEntry.arguments?.getLong("serviceId") ?: 0L
    StreamingScreen(
        serviceId = serviceId,
        onBack = { navController.popBackStack() },
        onNavigateToSummary = { sid ->
            navController.navigate(Screen.ServiceSummary.createRoute(sid)) {
                popUpTo(Screen.Streaming.route) { inclusive = true }
            }
        }
    )
}

@Composable
private fun ServiceSummaryComposable(navController: NavHostController, backStackEntry: NavBackStackEntry) {
    val serviceId = backStackEntry.arguments?.getLong("serviceId") ?: 0L
    ServiceSummaryScreen(
        serviceId = serviceId,
        onBackToHome = {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Home.route) { inclusive = false }
                launchSingleTop = true
            }
        }
    )
}

