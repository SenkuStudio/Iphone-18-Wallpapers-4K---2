package com.walli.iphonewallpapers.domain.usecase

import com.walli.iphonewallpapers.domain.repository.WallpaperRepository
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val repository: WallpaperRepository,
) {
    suspend operator fun invoke() = repository.getCategories()
}
