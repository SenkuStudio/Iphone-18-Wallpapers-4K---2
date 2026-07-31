package com.walli.spidermanwallpaper.preview

import com.walli.spidermanwallpaper.domain.model.Wallpaper
import com.walli.spidermanwallpaper.domain.model.WallpaperSort
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PreviewSession(
    val items: List<Wallpaper> = emptyList(),
    val initialIndex: Int = 0,
    val source: String = "home",
    val page: Int = 1,
    val hasNext: Boolean = true,
    val categoryId: Int? = null,
    val query: String? = null,
    val sort: WallpaperSort = WallpaperSort.LATEST,
)

@Singleton
class PreviewSessionManager @Inject constructor() {
    private val _session = MutableStateFlow(PreviewSession())
    val session: StateFlow<PreviewSession> = _session.asStateFlow()

    fun open(
        items: List<Wallpaper>,
        initialIndex: Int,
        source: String,
        page: Int = 1,
        hasNext: Boolean = true,
        categoryId: Int? = null,
        query: String? = null,
        sort: WallpaperSort = WallpaperSort.LATEST,
    ) {
        _session.value = PreviewSession(
            items = items,
            initialIndex = initialIndex,
            source = source,
            page = page,
            hasNext = hasNext,
            categoryId = categoryId,
            query = query,
            sort = sort,
        )
    }

    fun updateItems(items: List<Wallpaper>, page: Int, hasNext: Boolean) {
        _session.update { current ->
            current.copy(
                items = items,
                page = page,
                hasNext = hasNext
            )
        }
    }
}
