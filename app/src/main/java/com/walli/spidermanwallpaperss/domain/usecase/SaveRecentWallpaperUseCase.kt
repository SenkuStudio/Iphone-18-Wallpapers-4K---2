package com.walli.spidermanwallpaperss.domain.usecase

import com.walli.spidermanwallpaperss.domain.model.Wallpaper
import com.walli.spidermanwallpaperss.domain.repository.LibraryRepository
import javax.inject.Inject

class SaveRecentWallpaperUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    suspend operator fun invoke(wallpaper: Wallpaper) = repository.addRecent(wallpaper)
}
