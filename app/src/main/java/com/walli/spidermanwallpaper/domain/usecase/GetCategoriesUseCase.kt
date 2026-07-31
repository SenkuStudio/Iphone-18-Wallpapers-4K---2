package com.walli.spidermanwallpaper.domain.usecase

import com.walli.spidermanwallpaper.domain.repository.WallpaperRepository
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val repository: WallpaperRepository,
) {
    suspend operator fun invoke() = repository.getCategories()
}
