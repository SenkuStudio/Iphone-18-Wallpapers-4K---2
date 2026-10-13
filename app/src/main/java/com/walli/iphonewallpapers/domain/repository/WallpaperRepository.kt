package com.walli.iphonewallpapers.domain.repository

import com.walli.iphonewallpapers.domain.model.PagedResult
import com.walli.iphonewallpapers.domain.model.Wallpaper
import com.walli.iphonewallpapers.domain.model.WallpaperCategory
import com.walli.iphonewallpapers.domain.model.WallpaperSort
import kotlinx.coroutines.flow.Flow

interface WallpaperRepository {
    suspend fun getWallpapers(
        page: Int,
        limit: Int,
        categoryId: Int?,
        query: String?,
        sort: WallpaperSort,
    ): Result<PagedResult<Wallpaper>>

    fun getLatestCachedWallpapers(limit: Int): Flow<List<Wallpaper>>

    suspend fun getCategories(): Result<List<WallpaperCategory>>

    suspend fun incrementDownload(wallpaperId: String): Result<Unit>
}
