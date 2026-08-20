package com.walli.spidermanwallpaper.ui.screens.preview

import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walli.spidermanwallpaper.data.local.datastore.WallpaperPreferences
import com.walli.spidermanwallpaper.domain.model.Wallpaper
import com.walli.spidermanwallpaper.domain.model.WallpaperTarget
import com.walli.spidermanwallpaper.domain.usecase.IncrementDownloadUseCase
import com.walli.spidermanwallpaper.domain.usecase.ObserveFavoriteIdsUseCase
import com.walli.spidermanwallpaper.domain.usecase.SaveRecentWallpaperUseCase
import com.walli.spidermanwallpaper.domain.usecase.ToggleFavoriteUseCase
import com.walli.spidermanwallpaper.util.NetworkMonitor
import com.walli.spidermanwallpaper.preview.PreviewSessionManager
import com.walli.spidermanwallpaper.wallpaper.ShareManager
import com.walli.spidermanwallpaper.wallpaper.WallpaperApplier
import com.walli.spidermanwallpaper.wallpaper.WallpaperDownloader
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ImageTransformation(
    val scale: Float = 1f,
    val offset: Offset = Offset.Zero,
    val rotation: Float = 0f,
    val brightness: Float = 1f,
    val contrast: Float = 1f,
    val blur: Float = 0f,
    val saturation: Float = 1f,
    val hue: Float = 0f,
    val structure: Float = 0f,
    val details: Float = 0f,
)

data class PreviewUiState(
    val items: List<Any> = emptyList(), // Can be Wallpaper or Ad placeholder String
    val initialIndex: Int = 0,
    val controlsVisible: Boolean = true,
    val isWorking: Boolean = false,
    val workingLabel: String? = null,
    val message: String? = null,
    val isOnline: Boolean = true,
    val transformations: Map<Int, ImageTransformation> = emptyMap(),
    val wallpaperToUnlock: Wallpaper? = null,
    val wallpaperApplied: Boolean = false,
    val isEditMode: Boolean = false,
    val areEditToolsUnlocked: Boolean = false,
    val isPremiumPassActive: Boolean = false,
)

@HiltViewModel
class PreviewViewModel @Inject constructor(
    private val previewSessionManager: PreviewSessionManager,
    private val getWallpapersUseCase: com.walli.spidermanwallpaper.domain.usecase.GetWallpapersUseCase,
    observeFavoriteIdsUseCase: ObserveFavoriteIdsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val saveRecentWallpaperUseCase: SaveRecentWallpaperUseCase,
    private val incrementDownloadUseCase: IncrementDownloadUseCase,
    private val wallpaperDownloader: WallpaperDownloader,
    private val wallpaperApplier: WallpaperApplier,
    private val shareManager: ShareManager,
    private val networkMonitor: NetworkMonitor,
    private val wallpaperPreferences: WallpaperPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PreviewUiState())
    val uiState: StateFlow<PreviewUiState> = _uiState.asStateFlow()

    private var favoriteIds = emptySet<String>()
    private var unlockedIds = emptySet<String>()
    private var requestInFlight = false
    private val removedAdKeys = mutableSetOf<String>()

    init {
        observeNetwork()
        viewModelScope.launch {
            combine(
                previewSessionManager.session,
                observeFavoriteIdsUseCase(),
                wallpaperPreferences.unlockedWallpaperIds,
                wallpaperPreferences.editToolsUnlockTime,
                wallpaperPreferences.premiumPassUnlockTime
            ) { session, favorites, unlocks, editUnlockTime, premiumPassTime ->
                val isEditUnlocked = System.currentTimeMillis() - editUnlockTime < 24 * 60 * 60 * 1000
                val isPremiumPassActive = System.currentTimeMillis() - premiumPassTime < 24 * 60 * 60 * 1000
                Quintuple(session, favorites, unlocks, isEditUnlocked, isPremiumPassActive)
            }.collect { (session, favorites, unlocks, isEditUnlocked, isPremiumPassActive) ->
                favoriteIds = favorites
                unlockedIds = unlocks
                _uiState.update { current ->
                    val itemsWithAds = mutableListOf<Any>()
                    var targetInitialIndex = session.initialIndex
                    
                    session.items.forEachIndexed { index, wallpaper ->
                        itemsWithAds.add(wallpaper.copy(
                            isFavorite = wallpaper.id in favorites,
                            isUnlocked = (wallpaper.id in unlocks) || isPremiumPassActive
                        ))
                        
                        if (index == session.initialIndex) {
                            targetInitialIndex = itemsWithAds.size - 1
                        }

                        // Insert ad every 4 wallpapers to match grid
                        if ((index + 1) % 4 == 0) {
                            val adKey = "ad_preview_${index / 4}"
                            if (adKey !in removedAdKeys) {
                                itemsWithAds.add(adKey)
                            }
                        }
                    }

                    current.copy(
                        items = itemsWithAds,
                        initialIndex = targetInitialIndex,
                        areEditToolsUnlocked = isEditUnlocked || isPremiumPassActive,
                        isPremiumPassActive = isPremiumPassActive
                    )
                }
            }
        }
    }

    private data class Quintuple<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)

    private fun observeNetwork() {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                _uiState.update { it.copy(isOnline = isOnline) }
            }
        }
    }

    fun toggleControls() {
        _uiState.update { it.copy(controlsVisible = !it.controlsVisible) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun onPageSettled(index: Int) {
        viewModelScope.launch {
            val item = _uiState.value.items.getOrNull(index)
            if (item is Wallpaper) {
                saveRecentWallpaperUseCase(item)
            }
            
            // Automatically reset transformations and exit edit mode when moving to a new image
            _uiState.update { it.copy(
                transformations = emptyMap(),
                isEditMode = false
            ) }
            
            // Check if we need to load more items
            val session = previewSessionManager.session.value
            if (session.hasNext && !requestInFlight && index >= _uiState.value.items.size - 4) {
                loadMoreItems(session)
            }
        }
    }

    fun removeAdItem(key: String) {
        removedAdKeys.add(key)
        _uiState.update { current ->
            current.copy(items = current.items.filterNot { it is String && it == key })
        }
    }

    private fun loadMoreItems(session: com.walli.spidermanwallpaper.preview.PreviewSession) {
        viewModelScope.launch {
            requestInFlight = true
            val nextPage = session.page + 1
            getWallpapersUseCase(
                page = nextPage,
                limit = 30, // Standard page size
                categoryId = session.categoryId,
                query = session.query,
                sort = session.sort,
            )
                .onSuccess { page ->
                    val newItems = (session.items + page.items).distinctBy { it.id }
                    previewSessionManager.updateItems(
                        items = newItems,
                        page = page.page,
                        hasNext = page.hasNext
                    )
                }
            requestInFlight = false
        }
    }

    fun requestUnlock(index: Int) {
        val item = _uiState.value.items.getOrNull(index)
        if (item is Wallpaper && item.isPremium && !item.isUnlocked) {
            _uiState.update { it.copy(wallpaperToUnlock = item) }
        }
    }

    fun dismissUnlockDialog() {
        _uiState.update { it.copy(wallpaperToUnlock = null) }
    }

    fun unlockWallpaper(wallpaper: Wallpaper) {
        viewModelScope.launch {
            wallpaperPreferences.unlockWallpaper(wallpaper.id)
            _uiState.update { current ->
                val newItems = current.items.map {
                    if (it is Wallpaper && it.id == wallpaper.id) {
                        it.copy(isUnlocked = true)
                    } else it
                }
                current.copy(
                    items = newItems,
                    wallpaperToUnlock = null
                )
            }
        }
    }

    fun unlockPremiumPass() {
        viewModelScope.launch {
            wallpaperPreferences.unlockPremiumPass()
            _uiState.update { it.copy(wallpaperToUnlock = null) }
        }
    }

    fun toggleFavorite(index: Int) {
        val item = _uiState.value.items.getOrNull(index)
        if (item is Wallpaper) {
            viewModelScope.launch {
                toggleFavoriteUseCase(item)
                _uiState.update { current ->
                    val newItems = current.items.map {
                        if (it is Wallpaper && it.id == item.id) {
                            it.copy(isFavorite = !item.isFavorite)
                        } else it
                    }
                    current.copy(items = newItems)
                }
            }
        }
    }

    fun download(index: Int) {
        val item = _uiState.value.items.getOrNull(index)
        if (item is Wallpaper) {
            val transformation = _uiState.value.transformations[index] ?: ImageTransformation()
            viewModelScope.launch {
                setWorking(label = "Downloading…")
                wallpaperDownloader.download(
                    url = item.imageUrl,
                    title = item.title,
                    scale = transformation.scale,
                    offsetX = transformation.offset.x,
                    offsetY = transformation.offset.y,
                    rotation = transformation.rotation,
                    brightness = transformation.brightness,
                    contrast = transformation.contrast,
                    blur = transformation.blur,
                    saturation = transformation.saturation,
                    hue = transformation.hue,
                    structure = transformation.structure,
                    details = transformation.details,
                    applier = wallpaperApplier
                )
                    .onSuccess {
                        incrementDownloadUseCase(item.id)
                        setMessage("Saved to Downloads")
                    }
                    .onFailure { throwable ->
                        setMessage(throwable.message ?: "Download failed")
                    }
                clearWorking()
            }
        }
    }

    fun updateTransformation(
        index: Int,
        scale: Float? = null,
        offset: Offset? = null,
        rotation: Float? = null,
        brightness: Float? = null,
        contrast: Float? = null,
        blur: Float? = null,
        saturation: Float? = null,
        hue: Float? = null,
        structure: Float? = null,
        details: Float? = null,
    ) {
        _uiState.update { current ->
            val transformations = current.transformations.toMutableMap()
            val existing = transformations[index] ?: ImageTransformation()
            transformations[index] = existing.copy(
                scale = scale ?: existing.scale,
                offset = offset ?: existing.offset,
                rotation = rotation ?: existing.rotation,
                brightness = brightness ?: existing.brightness,
                contrast = contrast ?: existing.contrast,
                blur = blur ?: existing.blur,
                saturation = saturation ?: existing.saturation,
                hue = hue ?: existing.hue,
                structure = structure ?: existing.structure,
                details = details ?: existing.details,
            )
            current.copy(transformations = transformations)
        }
    }

    fun setEditMode(enabled: Boolean) {
        _uiState.update { it.copy(isEditMode = enabled) }
    }

    fun unlockEditTools() {
        viewModelScope.launch {
            wallpaperPreferences.unlockEditTools()
        }
    }

    fun resetTransformation(index: Int) {
        _uiState.update { current ->
            current.copy(
                transformations = current.transformations - index
            )
        }
    }

    fun handleDoubleTap(index: Int, tapOffset: Offset = Offset.Zero, center: Offset = Offset.Zero) {
        _uiState.update { current ->
            val currentTransformation = current.transformations[index] ?: ImageTransformation()
            val newTransformation = if (currentTransformation.scale > 1.05f) { // Slightly more than 1 to handle precision
                ImageTransformation(
                    brightness = currentTransformation.brightness,
                    contrast = currentTransformation.contrast,
                    blur = currentTransformation.blur,
                    saturation = currentTransformation.saturation,
                    hue = currentTransformation.hue,
                    structure = currentTransformation.structure,
                    details = currentTransformation.details
                )
            } else {
                val targetScale = 2.5f
                // Zoom at tap location relative to center pivot
                val newOffset = (tapOffset - center) * (1f - targetScale)
                currentTransformation.copy(scale = targetScale, offset = newOffset)
            }
            current.copy(
                transformations = current.transformations + (index to newTransformation)
            )
        }
    }

    fun consumeWallpaperApplied() {
        _uiState.update { it.copy(wallpaperApplied = false) }
    }

    fun applyWallpaper(index: Int, target: WallpaperTarget) {
        val item = _uiState.value.items.getOrNull(index)
        if (item is Wallpaper) {
            val transformation = _uiState.value.transformations[index] ?: ImageTransformation()
            viewModelScope.launch {
                setWorking(label = "Applying wallpaper…")
                wallpaperApplier.apply(
                    url = item.imageUrl,
                    target = target,
                    scale = transformation.scale,
                    offsetX = transformation.offset.x,
                    offsetY = transformation.offset.y,
                    rotation = transformation.rotation,
                    brightness = transformation.brightness,
                    contrast = transformation.contrast,
                    blur = transformation.blur,
                    saturation = transformation.saturation,
                    hue = transformation.hue,
                    structure = transformation.structure,
                    details = transformation.details,
                )
                    .onSuccess {
                        setMessage("Wallpaper applied to ${target.label.lowercase()}")
                        _uiState.update { it.copy(wallpaperApplied = true) }
                    }
                    .onFailure { throwable ->
                        setMessage(throwable.message ?: "Wallpaper apply failed")
                    }
                clearWorking()
            }
        }
    }

    fun share(index: Int) {
        val item = _uiState.value.items.getOrNull(index)
        if (item is Wallpaper) {
            val transformation = _uiState.value.transformations[index] ?: ImageTransformation()
            viewModelScope.launch {
                setWorking(label = "Preparing share…")
                shareManager.share(
                    url = item.imageUrl,
                    title = item.title,
                    scale = transformation.scale,
                    offsetX = transformation.offset.x,
                    offsetY = transformation.offset.y,
                    rotation = transformation.rotation,
                    brightness = transformation.brightness,
                    contrast = transformation.contrast,
                    blur = transformation.blur,
                    saturation = transformation.saturation,
                    hue = transformation.hue,
                    structure = transformation.structure,
                    details = transformation.details,
                    applier = wallpaperApplier
                )
                    .onFailure { throwable ->
                        setMessage(throwable.message ?: "Share failed")
                    }
                clearWorking()
            }
        }
    }

    private fun setWorking(label: String) {
        _uiState.update { it.copy(isWorking = true, workingLabel = label) }
    }

    private fun clearWorking() {
        _uiState.update { it.copy(isWorking = false, workingLabel = null) }
    }

    private fun setMessage(text: String) {
        _uiState.update { it.copy(message = text) }
    }
}
