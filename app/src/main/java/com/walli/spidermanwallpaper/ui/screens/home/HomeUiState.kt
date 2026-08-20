package com.walli.spidermanwallpaper.ui.screens.home

import com.walli.spidermanwallpaper.domain.model.Wallpaper
import com.walli.spidermanwallpaper.domain.model.WallpaperCategory
import com.walli.spidermanwallpaper.domain.model.WallpaperSort
import com.walli.spidermanwallpaper.ui.common.LoadState

data class HomeUiState(
    val wallpapers: List<Wallpaper> = emptyList(),
    val recentWallpapers: List<Wallpaper> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val unlockedIds: Set<String> = emptySet(),
    val categories: List<WallpaperCategory> = listOf(WallpaperCategory(id = null, name = "All")),
    val selectedCategoryId: Int? = null,
    val sort: WallpaperSort = WallpaperSort.LATEST,
    val loadState: LoadState = LoadState.Loading,
    val hasNext: Boolean = true,
    val isOnline: Boolean = true,
    val wallpaperToUnlock: Wallpaper? = null,
    val featuredWallpapers: List<Wallpaper> = emptyList(),
    val isPremiumPassActive: Boolean = false,
)
