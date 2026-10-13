package com.walli.iphonewallpapers.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.walli.iphonewallpapers.data.local.dao.CategoryDao
import com.walli.iphonewallpapers.data.local.dao.FavoritesDao
import com.walli.iphonewallpapers.data.local.dao.RecentDao
import com.walli.iphonewallpapers.data.local.dao.WallpaperDao
import com.walli.iphonewallpapers.data.local.entity.CategoryEntity
import com.walli.iphonewallpapers.data.local.entity.FavoriteWallpaperEntity
import com.walli.iphonewallpapers.data.local.entity.RecentWallpaperEntity
import com.walli.iphonewallpapers.data.local.entity.WallpaperEntity

@Database(
    entities = [
        FavoriteWallpaperEntity::class,
        RecentWallpaperEntity::class,
        CategoryEntity::class,
        WallpaperEntity::class
    ],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoritesDao(): FavoritesDao
    abstract fun recentDao(): RecentDao
    abstract fun categoryDao(): CategoryDao
    abstract fun wallpaperDao(): WallpaperDao
}
