package com.walli.iphonewallpaper.domain.usecase

import com.walli.iphonewallpaper.domain.repository.LibraryRepository
import javax.inject.Inject

class ObserveFavoritesUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    operator fun invoke() = repository.observeFavorites()
}
