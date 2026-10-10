package com.walli.iphonewallpaper.data.api

import com.walli.iphonewallpaper.data.api.model.ApiResponse
import com.walli.iphonewallpaper.data.api.model.CategoryDto
import com.walli.iphonewallpaper.data.api.model.DownloadAckDto
import com.walli.iphonewallpaper.data.api.model.WallpaperDto
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface WallpaperApi {
    @GET("api/v1/wallpapers")
    suspend fun getWallpapers(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("category_id") categoryId: Int? = null,
        @Query("query") query: String? = null,
        @Query("sort") sort: String? = null,
    ): ApiResponse<List<WallpaperDto>>

    @GET("api/v1/categories")
    suspend fun getCategories(): ApiResponse<List<CategoryDto>>

    @POST("api/v1/wallpapers/{id}/download")
    suspend fun incrementDownload(@Path("id") wallpaperId: String): DownloadAckDto
}
