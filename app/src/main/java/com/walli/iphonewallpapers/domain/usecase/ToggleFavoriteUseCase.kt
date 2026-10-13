package com.walli.iphonewallpapers.domain.usecase

import com.walli.iphonewallpapers.domain.model.Wallpaper
import com.walli.iphonewallpapers.domain.repository.LibraryRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    suspend operator fun invoke(wallpaper: Wallpaper): Boolean = repository.toggleFavorite(wallpaper)
}
