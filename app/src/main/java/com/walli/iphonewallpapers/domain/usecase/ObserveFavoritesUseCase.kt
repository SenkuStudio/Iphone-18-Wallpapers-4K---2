package com.walli.iphonewallpapers.domain.usecase

import com.walli.iphonewallpapers.domain.repository.LibraryRepository
import javax.inject.Inject

class ObserveFavoritesUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    operator fun invoke() = repository.observeFavorites()
}
