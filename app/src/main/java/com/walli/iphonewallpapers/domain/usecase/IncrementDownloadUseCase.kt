package com.walli.iphonewallpapers.domain.usecase

import com.walli.iphonewallpapers.domain.repository.WallpaperRepository
import javax.inject.Inject

class IncrementDownloadUseCase @Inject constructor(
    private val repository: WallpaperRepository,
) {
    suspend operator fun invoke(wallpaperId: String) = repository.incrementDownload(wallpaperId)
}
