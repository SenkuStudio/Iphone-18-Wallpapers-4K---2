package com.walli.spidermanwallpaperss.domain.usecase

import com.walli.spidermanwallpaperss.domain.repository.LibraryRepository
import javax.inject.Inject

class ObserveFavoritesUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    operator fun invoke() = repository.observeFavorites()
}
