package com.miguelcode.warrantyvault.navigation

sealed class Screen(val route: String) {
    data object Onboarding : Screen("onboarding")
    data object Home : Screen("home")
    data object Detail : Screen("detail/{warrantyId}") {
        fun createRoute(id: Long) = "detail/$id"
        const val ARG = "warrantyId"
    }
    data object Add : Screen("add")
    data object Edit : Screen("edit/{warrantyId}") {
        fun createRoute(id: Long) = "edit/$id"
        const val ARG = "warrantyId"
    }
    data object Settings : Screen("settings")
    data object Premium : Screen("premium")
}
