package com.walli.darkaestheticwallpaper.domain.usecase

import com.walli.darkaestheticwallpaper.domain.model.Wallpaper
import com.walli.darkaestheticwallpaper.domain.repository.WallpaperRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveLatestCachedWallpapersUseCase @Inject constructor(
    private val repository: WallpaperRepository,
) {
    operator fun invoke(limit: Int): Flow<List<Wallpaper>> = repository.getLatestCachedWallpapers(limit)
}
