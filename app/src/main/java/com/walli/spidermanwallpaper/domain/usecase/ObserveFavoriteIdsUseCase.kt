package com.walli.spidermanwallpaper.domain.usecase

import com.walli.spidermanwallpaper.domain.repository.LibraryRepository
import javax.inject.Inject

class ObserveFavoriteIdsUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    operator fun invoke() = repository.observeFavoriteIds()
}
