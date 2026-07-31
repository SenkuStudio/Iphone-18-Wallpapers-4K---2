package com.walli.spidermanwallpaper.domain.usecase

import com.walli.spidermanwallpaper.domain.model.Wallpaper
import com.walli.spidermanwallpaper.domain.repository.LibraryRepository
import javax.inject.Inject

class SaveRecentWallpaperUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    suspend operator fun invoke(wallpaper: Wallpaper) = repository.addRecent(wallpaper)
}
