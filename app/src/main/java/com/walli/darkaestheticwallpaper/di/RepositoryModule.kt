package com.walli.darkaestheticwallpaper.di

import com.walli.darkaestheticwallpaper.data.repository.LibraryRepositoryImpl
import com.walli.darkaestheticwallpaper.data.repository.WallpaperRepositoryImpl
import com.walli.darkaestheticwallpaper.domain.repository.LibraryRepository
import com.walli.darkaestheticwallpaper.domain.repository.WallpaperRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindWallpaperRepository(
        impl: WallpaperRepositoryImpl,
    ): WallpaperRepository

    @Binds
    @Singleton
    abstract fun bindLibraryRepository(
        impl: LibraryRepositoryImpl,
    ): LibraryRepository
}
