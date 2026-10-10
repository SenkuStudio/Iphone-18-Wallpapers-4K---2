package com.walli.iphonewallpaper.domain.usecase

import com.walli.iphonewallpaper.domain.model.Wallpaper
import com.walli.iphonewallpaper.domain.repository.LibraryRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    suspend operator fun invoke(wallpaper: Wallpaper): Boolean = repository.toggleFavorite(wallpaper)
}
