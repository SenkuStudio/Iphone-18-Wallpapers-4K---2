package com.walli.spidermanwallpapers.domain.usecase

import com.walli.spidermanwallpapers.domain.repository.WallpaperRepository
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val repository: WallpaperRepository,
) {
    suspend operator fun invoke() = repository.getCategories()
}
