package com.walli.iphonewallpapers.data.repository

import com.walli.iphonewallpapers.data.api.model.CategoryDto
import com.walli.iphonewallpapers.data.api.model.WallpaperDto
import com.walli.iphonewallpapers.data.local.entity.FavoriteWallpaperEntity
import com.walli.iphonewallpapers.data.local.entity.RecentWallpaperEntity
import com.walli.iphonewallpapers.domain.model.Wallpaper
import com.walli.iphonewallpapers.domain.model.WallpaperCategory

internal fun WallpaperDto.toDomainOrNull(): Wallpaper? {
    if (id.isBlank() || imageUrl.isBlank() || thumbnailUrl.isBlank()) return null
    return Wallpaper(
        id = id,
        title = title.ifBlank { "Untitled" },
        category = category.name.ifBlank { "Uncategorized" },
        imageUrl = imageUrl,
        thumbnailUrl = thumbnailUrl,
        downloads = downloads,
        createdAt = createdAt,
        isPremium = isPremium,
        isFeatured = isFeatured,
        blurHash = blurHash,
    )
}

internal fun CategoryDto.toDomain(): WallpaperCategory = WallpaperCategory(
    id = id,
    name = name,
    slug = slug,
    coverUrl = coverUrl,
)

internal fun Wallpaper.toFavoriteEntity(now: Long): FavoriteWallpaperEntity = FavoriteWallpaperEntity(
    id = id,
    title = title,
    category = category,
    imageUrl = imageUrl,
    thumbnailUrl = thumbnailUrl,
    downloads = downloads,
    createdAt = createdAt,
    isPremium = isPremium,
    addedAt = now,
    blurHash = blurHash,
)

internal fun FavoriteWallpaperEntity.toDomain(): Wallpaper = Wallpaper(
    id = id,
    title = title,
    category = category,
    imageUrl = imageUrl,
    thumbnailUrl = thumbnailUrl,
    downloads = downloads,
    createdAt = createdAt,
    isPremium = isPremium,
    isFavorite = true,
    blurHash = blurHash,
)

internal fun Wallpaper.toRecentEntity(now: Long): RecentWallpaperEntity = RecentWallpaperEntity(
    id = id,
    title = title,
    category = category,
    imageUrl = imageUrl,
    thumbnailUrl = thumbnailUrl,
    downloads = downloads,
    createdAt = createdAt,
    isPremium = isPremium,
    viewedAt = now,
    blurHash = blurHash,
)

internal fun RecentWallpaperEntity.toDomain(): Wallpaper = Wallpaper(
    id = id,
    title = title,
    category = category,
    imageUrl = imageUrl,
    thumbnailUrl = thumbnailUrl,
    downloads = downloads,
    createdAt = createdAt,
    isPremium = isPremium,
    blurHash = blurHash,
)
