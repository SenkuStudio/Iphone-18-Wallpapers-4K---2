package com.walli.superheros.domain.usecase

import com.walli.superheros.domain.model.Wallpaper
import com.walli.superheros.domain.repository.LibraryRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: LibraryRepository,
) {
    suspend operator fun invoke(wallpaper: Wallpaper): Boolean = repository.toggleFavorite(wallpaper)
}
