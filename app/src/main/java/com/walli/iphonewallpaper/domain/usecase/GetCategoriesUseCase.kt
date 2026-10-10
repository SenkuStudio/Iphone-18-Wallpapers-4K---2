package com.walli.iphonewallpaper.domain.usecase

import com.walli.iphonewallpaper.domain.repository.WallpaperRepository
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val repository: WallpaperRepository,
) {
    suspend operator fun invoke() = repository.getCategories()
}
