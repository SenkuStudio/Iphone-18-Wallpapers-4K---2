package com.walli.superheros.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.walli.superheros.data.local.dao.CategoryDao
import com.walli.superheros.data.local.dao.FavoritesDao
import com.walli.superheros.data.local.dao.RecentDao
import com.walli.superheros.data.local.dao.WallpaperDao
import com.walli.superheros.data.local.entity.CategoryEntity
import com.walli.superheros.data.local.entity.FavoriteWallpaperEntity
import com.walli.superheros.data.local.entity.RecentWallpaperEntity
import com.walli.superheros.data.local.entity.WallpaperEntity

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
