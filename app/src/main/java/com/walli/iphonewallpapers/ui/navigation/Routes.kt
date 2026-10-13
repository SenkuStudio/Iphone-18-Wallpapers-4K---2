package com.walli.iphonewallpapers.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class SpidermanRoute(
    val route: String,
    val label: String? = null,
    val icon: ImageVector? = null,
) {
    data object Home : SpidermanRoute("home", "Home", Icons.Rounded.Home)
    data object Categories : SpidermanRoute("categories", "Categories", Icons.Rounded.Collections)
    data object Favorites : SpidermanRoute("favorites", "Favorites", Icons.Rounded.Favorite)
    data object Settings : SpidermanRoute("settings", "Settings", Icons.Rounded.Settings)
    data object Preview : SpidermanRoute("preview")
    data object Search : SpidermanRoute("search")
    data object Onboarding : SpidermanRoute("onboarding")
    data object CategoryWallpapers : SpidermanRoute("category_wallpapers")
}

val bottomBarRoutes = listOf(
    SpidermanRoute.Home,
    SpidermanRoute.Categories,
    SpidermanRoute.Favorites,
    SpidermanRoute.Settings,
)
