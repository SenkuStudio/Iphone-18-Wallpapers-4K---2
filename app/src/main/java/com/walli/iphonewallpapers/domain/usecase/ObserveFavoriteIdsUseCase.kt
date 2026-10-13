package com.walli.iphonewallpapers.domain.usecase

import com.walli.iphonewallpapers.domain.repository.LibraryRepository
import javax.inject.Inject

class ObserveFavoriteIdsUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    operator fun invoke() = repository.observeFavoriteIds()
}
