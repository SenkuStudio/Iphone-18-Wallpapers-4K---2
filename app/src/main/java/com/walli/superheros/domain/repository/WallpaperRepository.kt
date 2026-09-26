package com.walli.superheros.domain.repository

import com.walli.superheros.domain.model.PagedResult
import com.walli.superheros.domain.model.Wallpaper
import com.walli.superheros.domain.model.WallpaperCategory
import com.walli.superheros.domain.model.WallpaperSort
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
