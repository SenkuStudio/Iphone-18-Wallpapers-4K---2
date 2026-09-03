package com.walli.spidermanwallpapers.domain.usecase

import com.walli.spidermanwallpapers.domain.repository.LibraryRepository
import javax.inject.Inject

class ObserveFavoriteIdsUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    operator fun invoke() = repository.observeFavoriteIds()
}
