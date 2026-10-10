package com.walli.iphonewallpaper.domain.usecase

import com.walli.iphonewallpaper.domain.repository.WallpaperRepository
import javax.inject.Inject

class IncrementDownloadUseCase @Inject constructor(
    private val repository: WallpaperRepository,
) {
    suspend operator fun invoke(wallpaperId: String) = repository.incrementDownload(wallpaperId)
}
