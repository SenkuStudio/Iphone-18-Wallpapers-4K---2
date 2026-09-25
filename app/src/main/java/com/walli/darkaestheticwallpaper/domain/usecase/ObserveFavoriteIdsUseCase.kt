package com.walli.darkaestheticwallpaper.domain.usecase

import com.walli.darkaestheticwallpaper.domain.repository.LibraryRepository
import javax.inject.Inject

class ObserveFavoriteIdsUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    operator fun invoke() = repository.observeFavoriteIds()
}
