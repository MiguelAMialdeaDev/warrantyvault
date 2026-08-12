package com.miguelcode.warrantyvault.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.miguelcode.warrantyvault.feature.addedit.AddEditScreen
import com.miguelcode.warrantyvault.feature.detail.DetailScreen
import com.miguelcode.warrantyvault.feature.home.HomeScreen
import com.miguelcode.warrantyvault.feature.onboarding.OnboardingScreen
import com.miguelcode.warrantyvault.feature.premium.PremiumScreen
import com.miguelcode.warrantyvault.feature.settings.SettingsScreen

@Composable
fun AppNavGraph(
    navController: NavHostController,
    onboardingDone: Boolean
) {
    NavHost(
        navController = navController,
        startDestination = if (onboardingDone) Screen.Home.route else Screen.Onboarding.route
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(onFinish = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                    // Segunda red bajo la guarda del ViewModel: si aun así llegaran dos
                    // navegaciones, la segunda no apila un Home duplicado.
                    launchSingleTop = true
                }
            })
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToDetail = { navController.navigate(Screen.Detail.createRoute(it)) },
                onNavigateToAdd = { navController.navigate(Screen.Add.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToPremium = { navController.navigate(Screen.Premium.route) }
            )
        }
        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument(Screen.Detail.ARG) { type = NavType.LongType })
        ) { entry ->
            val warrantyId = entry.arguments?.getLong(Screen.Detail.ARG) ?: return@composable
            DetailScreen(
                warrantyId = warrantyId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEdit = { navController.navigate(Screen.Edit.createRoute(it)) }
            )
        }
        composable(Screen.Add.route) {
            AddEditScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.Edit.route,
            arguments = listOf(navArgument(Screen.Edit.ARG) { type = NavType.LongType })
        ) { entry ->
            val warrantyId = entry.arguments?.getLong(Screen.Edit.ARG) ?: return@composable
            AddEditScreen(
                warrantyId = warrantyId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPremium = { navController.navigate(Screen.Premium.route) }
            )
        }
        composable(Screen.Premium.route) {
            PremiumScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
