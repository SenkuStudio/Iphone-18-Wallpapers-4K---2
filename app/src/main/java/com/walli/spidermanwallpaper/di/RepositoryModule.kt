package com.walli.spidermanwallpaper.di

import com.walli.spidermanwallpaper.data.repository.LibraryRepositoryImpl
import com.walli.spidermanwallpaper.data.repository.WallpaperRepositoryImpl
import com.walli.spidermanwallpaper.domain.repository.LibraryRepository
import com.walli.spidermanwallpaper.domain.repository.WallpaperRepository
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
