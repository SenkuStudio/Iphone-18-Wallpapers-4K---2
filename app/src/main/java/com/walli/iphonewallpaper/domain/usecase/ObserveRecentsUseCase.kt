package com.walli.iphonewallpaper.domain.usecase

import com.walli.iphonewallpaper.domain.repository.LibraryRepository
import javax.inject.Inject

class ObserveRecentsUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    operator fun invoke() = repository.observeRecents()
}
