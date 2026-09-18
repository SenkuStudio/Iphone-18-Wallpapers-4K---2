package com.walli.spidermanwallpaperss.ui.screens.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walli.spidermanwallpaperss.domain.model.WallpaperCategory
import com.walli.spidermanwallpaperss.domain.model.WallpaperSort
import com.walli.spidermanwallpaperss.domain.usecase.GetCategoriesUseCase
import com.walli.spidermanwallpaperss.domain.usecase.GetWallpapersUseCase
import com.walli.spidermanwallpaperss.util.NetworkMonitor
import com.walli.spidermanwallpaperss.ui.common.LoadState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoriesUiState(
    val categories: List<WallpaperCategory> = emptyList(),
    val loadState: LoadState = LoadState.Loading,
    val isOnline: Boolean = true,
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getWallpapersUseCase: GetWallpapersUseCase,
    private val networkMonitor: NetworkMonitor,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    init {
        observeNetwork()
        loadCategories()
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                _uiState.update { it.copy(isOnline = isOnline) }
                if (isOnline && _uiState.value.categories.isEmpty()) {
                    loadCategories()
                }
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(loadState = LoadState.Loading) }
            getCategoriesUseCase()
                .onSuccess { categories ->
                    _uiState.update {
                        it.copy(
                            categories = categories,
                            loadState = if (categories.isEmpty()) LoadState.Empty else LoadState.Idle,
                        )
                    }
                    // Fetch covers for categories that don't have one
                    fetchMissingCovers(categories)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(loadState = LoadState.Error(throwable.message ?: "Failed"))
                    }
                }
        }
    }

    private fun fetchMissingCovers(categories: List<WallpaperCategory>) {
        categories.forEach { category ->
            if (category.coverUrl == null && category.id != null) {
                viewModelScope.launch {
                    getWallpapersUseCase(
                        page = 1,
                        limit = 1,
                        categoryId = category.id,
                        query = null,
                        sort = WallpaperSort.LATEST
                    ).onSuccess { pagedResult ->
                        val firstWallpaper = pagedResult.items.firstOrNull()
                        if (firstWallpaper != null) {
                            updateCategoryCover(category.id, firstWallpaper.thumbnailUrl)
                        }
                    }
                }
            }
        }
    }

    private fun updateCategoryCover(categoryId: Int?, coverUrl: String) {
        _uiState.update { state ->
            val updatedCategories = state.categories.map {
                if (it.id == categoryId) it.copy(coverUrl = coverUrl) else it
            }
            state.copy(categories = updatedCategories)
        }
    }
}
