package com.walli.spidermanwallpaper.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walli.spidermanwallpaper.data.local.datastore.WallpaperPreferences
import com.walli.spidermanwallpaper.domain.model.Wallpaper
import com.walli.spidermanwallpaper.domain.usecase.ObserveFavoriteIdsUseCase
import com.walli.spidermanwallpaper.domain.usecase.ObserveFavoritesUseCase
import com.walli.spidermanwallpaper.domain.usecase.ToggleFavoriteUseCase
import com.walli.spidermanwallpaper.preview.PreviewSessionManager
import com.walli.spidermanwallpaper.ui.common.LoadState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FavoritesUiState(
    val items: List<Wallpaper> = emptyList(),
    val favoriteIds: Set<String> = emptySet(),
    val unlockedIds: Set<String> = emptySet(),
    val loadState: LoadState = LoadState.Loading,
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    observeFavoritesUseCase: ObserveFavoritesUseCase,
    observeFavoriteIdsUseCase: ObserveFavoriteIdsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val previewSessionManager: PreviewSessionManager,
    private val wallpaperPreferences: WallpaperPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeFavoritesUseCase().collect { items ->
                _uiState.update {
                    it.copy(
                        items = items,
                        loadState = if (items.isEmpty()) LoadState.Empty else LoadState.Idle,
                    )
                }
            }
        }

        viewModelScope.launch {
            combine(
                observeFavoriteIdsUseCase(),
                wallpaperPreferences.unlockedWallpaperIds
            ) { favorites, unlocks ->
                _uiState.update { state ->
                    state.copy(
                        favoriteIds = favorites,
                        unlockedIds = unlocks
                    )
                }
            }.collect {}
        }
    }

    fun openPreview(index: Int) {
        previewSessionManager.open(
            items = _uiState.value.items,
            initialIndex = index,
            source = "favorites",
            hasNext = false
        )
    }

    fun toggleFavorite(wallpaper: Wallpaper) {
        viewModelScope.launch {
            toggleFavoriteUseCase(wallpaper)
        }
    }
}
