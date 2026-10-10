package com.walli.iphonewallpaper.di

import com.walli.iphonewallpaper.data.repository.LibraryRepositoryImpl
import com.walli.iphonewallpaper.data.repository.WallpaperRepositoryImpl
import com.walli.iphonewallpaper.domain.repository.LibraryRepository
import com.walli.iphonewallpaper.domain.repository.WallpaperRepository
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
