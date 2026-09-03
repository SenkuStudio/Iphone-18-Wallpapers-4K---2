package com.walli.spidermanwallpapers.domain.usecase

import com.walli.spidermanwallpapers.domain.model.Wallpaper
import com.walli.spidermanwallpapers.domain.repository.LibraryRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    suspend operator fun invoke(wallpaper: Wallpaper): Boolean = repository.toggleFavorite(wallpaper)
}
