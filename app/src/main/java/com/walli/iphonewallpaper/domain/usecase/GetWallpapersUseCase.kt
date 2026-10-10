package com.walli.iphonewallpaper.domain.usecase

import com.walli.iphonewallpaper.domain.model.WallpaperSort
import com.walli.iphonewallpaper.domain.repository.WallpaperRepository
import javax.inject.Inject

class GetWallpapersUseCase @Inject constructor(
    private val repository: WallpaperRepository,
) {
    suspend operator fun invoke(
        page: Int,
        limit: Int,
        categoryId: Int?,
        query: String?,
        sort: WallpaperSort,
    ) = repository.getWallpapers(page, limit, categoryId, query, sort)
}
