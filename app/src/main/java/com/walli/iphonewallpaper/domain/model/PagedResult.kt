package com.walli.iphonewallpaper.domain.model

data class PagedResult<T>(
    val items: List<T>,
    val page: Int,
    val limit: Int,
    val hasNext: Boolean,
)
