package com.walli.darkaestheticwallpaper.ui.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import android.widget.Toast
import androidx.compose.runtime.*
import com.walli.darkaestheticwallpaper.ui.components.ExitRatingDialog
import androidx.activity.compose.BackHandler
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.walli.darkaestheticwallpaper.ui.components.SpidermanBottomBar
import com.walli.darkaestheticwallpaper.ui.screens.categories.CategoriesRoute
import com.walli.darkaestheticwallpaper.ui.screens.favorites.FavoritesRoute
import com.walli.darkaestheticwallpaper.ui.screens.home.HomeRoute
import com.walli.darkaestheticwallpaper.ui.screens.preview.PreviewRoute
import com.walli.darkaestheticwallpaper.ui.screens.settings.SettingsRoute
import com.walli.darkaestheticwallpaper.ui.screens.onboarding.OnboardingScreen
import com.walli.darkaestheticwallpaper.ui.screens.category_wallpapers.CategoryWallpapersRoute

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun SpidermanNavGraph(
    navController: NavHostController,
    startDestination: String = SpidermanRoute.Home.route,
    onOnboardingComplete: () -> Unit = {}
) {
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute != null && bottomBarRoutes.any { 
        currentRoute.startsWith(it.route) 
    }

    val context = LocalContext.current
    var showExitDialog by remember { mutableStateOf(false) }

    // Intercept back press when on Home screen to show Exit Dialog
    BackHandler(enabled = currentRoute == SpidermanRoute.Home.route + "?categoryId={categoryId}") {
        showExitDialog = true
    }

    if (showExitDialog) {
        ExitRatingDialog(
            onDismiss = { showExitDialog = false },
            onExit = {
                showExitDialog = false
                (context as? Activity)?.finish()
            },
            onRate = {
                showExitDialog = false
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.walli.darkaestheticwallpaper"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Play Store not found", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                SpidermanBottomBar(navController = navController, items = bottomBarRoutes)
            }
        },
    ) { _: PaddingValues ->
        SharedTransitionLayout {
            NavHost(
                navController = navController,
                startDestination = startDestination,
            ) {
                composable(SpidermanRoute.Onboarding.route) {
                    OnboardingScreen(
                        onFinish = {
                            onOnboardingComplete()
                            navController.navigate(SpidermanRoute.Home.route) {
                                popUpTo(SpidermanRoute.Onboarding.route) { inclusive = true }
                            }
                        }
                    )
                }
                composable(
                    route = SpidermanRoute.Home.route + "?categoryId={categoryId}",
                    arguments = listOf(
                        navArgument("categoryId") {
                            type = NavType.IntType
                            defaultValue = -1
                        }
                    )
                ) { backStackEntry ->
                    val categoryId = backStackEntry.arguments?.getInt("categoryId")?.takeIf { it != -1 }
                    HomeRoute(
                        initialCategoryId = categoryId,
                        onOpenPreview = { navController.navigate(SpidermanRoute.Preview.route) },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable
                    )
                }
                composable(SpidermanRoute.Categories.route) {
                    CategoriesRoute(
                        onCategoryClick = { category ->
                            navController.navigate(SpidermanRoute.CategoryWallpapers.route + "/${category.id}/${category.name}")
                        },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable
                    )
                }
                composable(
                    route = SpidermanRoute.CategoryWallpapers.route + "/{categoryId}/{categoryName}",
                    arguments = listOf(
                        navArgument("categoryId") { type = NavType.IntType },
                        navArgument("categoryName") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val categoryId = backStackEntry.arguments?.getInt("categoryId") ?: 0
                    val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
                    CategoryWallpapersRoute(
                        categoryId = categoryId,
                        categoryName = categoryName,
                        onBack = { navController.popBackStack() },
                        onOpenPreview = { navController.navigate(SpidermanRoute.Preview.route) },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable
                    )
                }
                composable(SpidermanRoute.Favorites.route) {
                    FavoritesRoute(
                        onOpenPreview = { navController.navigate(SpidermanRoute.Preview.route) },
                        onExplore = {
                            navController.navigate(SpidermanRoute.Home.route) {
                                popUpTo(SpidermanRoute.Home.route) { inclusive = true }
                            }
                        },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable
                    )
                }
                composable(SpidermanRoute.Settings.route) {
                    SettingsRoute(onBack = { navController.popBackStack() })
                }
                composable(SpidermanRoute.Preview.route) {
                    PreviewRoute(
                        onBack = { navController.popBackStack() },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this@composable
                    )
                }
            }
        }
    }
}
