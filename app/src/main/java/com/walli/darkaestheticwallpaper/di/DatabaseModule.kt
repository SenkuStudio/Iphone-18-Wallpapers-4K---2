package com.walli.darkaestheticwallpaper.di

import android.content.Context
import androidx.room.Room
import com.walli.darkaestheticwallpaper.data.local.dao.CategoryDao
import com.walli.darkaestheticwallpaper.data.local.dao.FavoritesDao
import com.walli.darkaestheticwallpaper.data.local.dao.RecentDao
import com.walli.darkaestheticwallpaper.data.local.dao.WallpaperDao
import com.walli.darkaestheticwallpaper.data.local.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "spider_hero.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideFavoritesDao(database: AppDatabase): FavoritesDao = database.favoritesDao()

    @Provides
    fun provideRecentDao(database: AppDatabase): RecentDao = database.recentDao()

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideWallpaperDao(database: AppDatabase): WallpaperDao = database.wallpaperDao()
}
