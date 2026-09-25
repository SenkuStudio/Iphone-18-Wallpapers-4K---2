package com.walli.darkaestheticwallpaper.domain.usecase

import com.walli.darkaestheticwallpaper.domain.model.Wallpaper
import com.walli.darkaestheticwallpaper.domain.repository.LibraryRepository
import javax.inject.Inject

class SaveRecentWallpaperUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    suspend operator fun invoke(wallpaper: Wallpaper) = repository.addRecent(wallpaper)
}
