package com.walli.spidermanwallpaperss.ui.screens.favorites

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalConfiguration
import com.walli.spidermanwallpaperss.BuildConfig
import com.walli.spidermanwallpaperss.ui.common.LoadState
import com.walli.spidermanwallpaperss.ui.components.BannerAd
import com.walli.spidermanwallpaperss.ui.components.EmptyState
import com.walli.spidermanwallpaperss.ui.components.WallpaperCard
import com.walli.spidermanwallpaperss.ui.components.WallpaperCardShimmer

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun FavoritesRoute(
    onOpenPreview: () -> Unit,
    onExplore: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: FavoritesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val columns = when {
        configuration.screenWidthDp >= 900 -> 4
        configuration.screenWidthDp >= 600 -> 3
        else -> 2
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Spider Hero Wallpapers 4K",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (state.loadState) {
                LoadState.Loading -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        userScrollEnabled = false
                    ) {
                        items(10) { WallpaperCardShimmer() }
                    }
                }
                LoadState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            title = "No favorites yet",
                            subtitle = "Explore our collection and save the ones you love!",
                            actionText = "Explore Wallpapers",
                            onAction = onExplore
                        )
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp, top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        state.items.forEachIndexed { index, wallpaper ->
                            item(key = wallpaper.id, contentType = "wallpaper") {
                                WallpaperCard(
                                    wallpaper = wallpaper,
                                    isFavorite = state.favoriteIds.contains(wallpaper.id),
                                    isUnlocked = state.unlockedIds.contains(wallpaper.id),
                                    onClick = {
                                        viewModel.onWallpaperClick(wallpaper)
                                        onOpenPreview()
                                    },
                                    sharedTransitionScope = sharedTransitionScope,
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                            }

                            // Insert Native Ad every 8 items
                            if (index > 0 && (index + 1) % 8 == 0) {
                                item(
                                    key = "native-ad-${index}",
                                    contentType = "native_ad"
                                ) {
                                    com.walli.spidermanwallpaperss.ui.components.NativeAdItem(
                                        adUnitId = BuildConfig.ADMOB_NATIVE_WALLPAPERS,
                                        key = "fav-grid-ad-$index",
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        item(span = { GridItemSpan(maxLineSpan) }) {
                            if (state.items.isNotEmpty()) {
                                BannerAd(
                                    adUnitId = BuildConfig.ADMOB_BANNER_FAVORITES,
                                    modifier = Modifier.padding(top = 16.dp),
                                    collapsible = "bottom"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
