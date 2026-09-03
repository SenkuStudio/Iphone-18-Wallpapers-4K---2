package com.walli.spidermanwallpapers.domain.usecase

import com.walli.spidermanwallpapers.domain.model.WallpaperSort
import com.walli.spidermanwallpapers.domain.repository.WallpaperRepository
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
