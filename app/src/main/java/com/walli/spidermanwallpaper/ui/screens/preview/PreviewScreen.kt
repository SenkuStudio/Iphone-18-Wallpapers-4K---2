@file:OptIn(ExperimentalMaterial3Api::class)

package com.walli.spidermanwallpaper.ui.screens.preview


import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.palette.graphics.Palette
import com.walli.spidermanwallpaper.util.blurhash.BlurhashDecoder
import coil3.SingletonImageLoader
import coil3.compose.SubcomposeAsyncImage
import coil3.asDrawable
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.request.crossfade
import com.walli.spidermanwallpaper.BuildConfig
import com.walli.spidermanwallpaper.ads.AdsViewModel
import com.walli.spidermanwallpaper.domain.model.WallpaperTarget
import androidx.compose.material3.ButtonDefaults
import com.walli.spidermanwallpaper.ui.components.NoInternetState
import com.walli.spidermanwallpaper.ui.components.EmptyState
import com.walli.spidermanwallpaper.ui.components.PremiumLoader
import com.walli.spidermanwallpaper.domain.model.Wallpaper
import com.walli.spidermanwallpaper.ui.components.NativeAdItem

import com.walli.spidermanwallpaper.util.findActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.walli.spidermanwallpaper.ui.components.UnlockPremiumDialog

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun PreviewRoute(
    onBack: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    viewModel: PreviewViewModel = hiltViewModel(),
    adsViewModel: AdsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val activity = context.findActivity()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var showSetSheet by remember { mutableStateOf(false) }

    val isRewardedReady by adsViewModel.isRewardedReady.collectAsStateWithLifecycle()
    var isAdLoading by remember { mutableStateOf(false) }

    // Preload Rewarded Ads for instant availability on click
    LaunchedEffect(Unit) {
        adsViewModel.preloadRewardedAd(BuildConfig.ADMOB_REWARDED_DOWNLOAD)
        adsViewModel.preloadRewardedAd(BuildConfig.ADMOB_REWARDED_SET_WALLPAPER)
        adsViewModel.preloadRewardedAd(BuildConfig.ADMOB_REWARDED_PREVIEW)
        adsViewModel.preloadRewardedAd(BuildConfig.ADMOB_REWARDED_PRO_TOOLS)
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { state.items.size },
    )

    // Fix: Jump to the initial index once items are loaded
    LaunchedEffect(state.items) {
        if (state.items.isNotEmpty() && state.initialIndex > 0 && pagerState.currentPage == 0) {
            pagerState.scrollToPage(state.initialIndex.coerceIn(0, state.items.lastIndex))
        }
    }

    fun showAdAndUnlock(wallpaper: Wallpaper) {
        val onReward: () -> Unit = {
            isAdLoading = false
            val actualPageInner = pagerState.currentPage % state.items.size
            when (state.pendingAction) {
                PendingAction.DOWNLOAD -> viewModel.download(actualPageInner)
                PendingAction.SET_WALLPAPER -> state.pendingWallpaperTarget?.let { 
                    viewModel.applyWallpaper(actualPageInner, it) 
                }
                else -> viewModel.unlockWallpaper(wallpaper)
            }
        }

        val onDismiss = { isAdLoading = false }
        val onError: (String) -> Unit = { message ->
            isAdLoading = false
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = "Retry",
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    showAdAndUnlock(wallpaper)
                }
            }
        }

        when (state.pendingAction) {
            PendingAction.DOWNLOAD -> adsViewModel.showDownloadRewarded(activity, onReward, onDismiss, onError)
            PendingAction.SET_WALLPAPER -> adsViewModel.showSetWallpaperRewarded(activity, onReward, onDismiss, onError)
            else -> adsViewModel.showPreviewRewarded(activity, onReward, onDismiss, onError)
        }
    }

    fun showProToolsAd() {
        isAdLoading = true
        adsViewModel.showProToolsRewarded(
            activity = activity,
            onReward = {
                isAdLoading = false
                viewModel.unlockEditTools()
                viewModel.setEditMode(true)
            },
            onDismiss = {
                isAdLoading = false
            },
            onError = { message ->
                isAdLoading = false
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = message,
                        actionLabel = "Retry",
                        duration = SnackbarDuration.Long
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        showProToolsAd()
                    }
                }
            }
        )
    }

    LaunchedEffect(isRewardedReady, isAdLoading) {
        if (isRewardedReady && isAdLoading) {
            state.wallpaperToUnlock?.let { wallpaper ->
                showAdAndUnlock(wallpaper)
            }
        }
    }

    // Color Palette state
    var dominantColor by remember { mutableStateOf(Color.Black) }
    var onDominantColor by remember { mutableStateOf(Color.White) }

    LaunchedEffect(state.wallpaperApplied) {
        if (state.wallpaperApplied) {
            viewModel.consumeWallpaperApplied()
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    if (state.items.isEmpty()) {
        EmptyState(
            title = "Nothing to preview",
            subtitle = "Open a wallpaper from Home or Favorites first.",
            actionText = "Go back",
            onAction = onBack,
        )
        return
    }

    LaunchedEffect(pagerState, state.items) {
        snapshotFlow { pagerState.settledPage }.collectLatest { actualPage ->
            if (state.items.isEmpty()) return@collectLatest
            val item = state.items[actualPage]
            if (item is Wallpaper) {
                val imageUrl = item.imageUrl

                viewModel.onPageSettled(actualPage)

                // Extract Palette from the current image
                val loader = SingletonImageLoader.get(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .size(128, 128) // Smaller size is sufficient for Palette extraction
                    .allowHardware(false) // Required to convert to bitmap
                    .build()

                val result = loader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = result.image.asDrawable(context.resources).toBitmap()
                    val palette = withContext(Dispatchers.Default) {
                        Palette.from(bitmap).maximumColorCount(16).generate()
                    }
                    val swatch = palette.vibrantSwatch ?: palette.dominantSwatch
                    swatch?.let { s ->
                        dominantColor = Color(s.rgb)
                        onDominantColor = Color(s.titleTextColor)
                    }
                }

                preloadAdjacentImages(
                    context = context,
                    urls = listOfNotNull(
                        (state.items.getOrNull(actualPage - 1) as? Wallpaper)?.imageUrl,
                        (state.items.getOrNull(actualPage + 1) as? Wallpaper)?.imageUrl,
                    ),
                )

                // Preload next ad if it's within the next 3 pages
                val nextItems = (1..3).mapNotNull { state.items.getOrNull(actualPage + it) }
                if (nextItems.any { it is String && it.startsWith("ad_preview_") }) {
                    adsViewModel.preloadNativeAd(BuildConfig.ADMOB_NATIVE_PREVIEW)
                }
            } else {
                // It's an ad
                dominantColor = Color.DarkGray
                onDominantColor = Color.White
            }
        }
    }

    val animatedDominantColor by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 600),
        label = "dominantColor"
    )

    if (showSetSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSetSheet = false },
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = {
                Surface(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Box(modifier = Modifier.size(width = 32.dp, height = 4.dp))
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Set Wallpaper",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    WallpaperTarget.entries.forEach { target ->
                        val icon = when (target) {
                            WallpaperTarget.HOME -> Icons.Rounded.Home
                            WallpaperTarget.LOCK -> Icons.Rounded.Lock
                            WallpaperTarget.BOTH -> Icons.Rounded.Wallpaper
                        }

                        Surface(
                            onClick = {
                                showSetSheet = false
                                val actualPage = pagerState.currentPage % state.items.size
                                viewModel.requestSetWallpaper(actualPage, target)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = target.label.replace(" screen", ""),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (!state.isOnline) {
            NoInternetState(
                modifier = Modifier.background(MaterialTheme.colorScheme.background),
                onRetry = null // Network monitor handles it
            )
        } else {
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                pageSpacing = 16.dp,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = !state.isEditMode,
                key = { page -> 
                    val item = state.items.getOrNull(page % state.items.size)
                    when (item) {
                        is Wallpaper -> item.id
                        is String -> item
                        else -> page
                    }
                }
            ) { page ->
                if (state.items.isEmpty()) return@HorizontalPager
                val actualPageInner = page % state.items.size
                val itemInner = state.items[actualPageInner]

                if (itemInner is String && itemInner.startsWith("ad_preview_")) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        NativeAdItem(
                            adUnitId = BuildConfig.ADMOB_NATIVE_PREVIEW,
                            key = itemInner,
                            modifier = Modifier.fillMaxWidth(),
                            onAdFailed = {
                                viewModel.removeAdItem(itemInner)
                            }
                        )
                    }
                } else if (itemInner is Wallpaper) {
                    val isLockedInner = itemInner.isPremium && !itemInner.isUnlocked

                    var isLoadedInner by remember(itemInner.id) { mutableStateOf(false) }
                    var retryCountInner by remember(itemInner.id) { mutableStateOf(0) }

                    val transformationInner = state.transformations[actualPageInner] ?: ImageTransformation()
                    val currentTransformationStateInner = rememberUpdatedState(transformationInner)

                    val placeholder = remember(itemInner.blurHash) {
                        itemInner.blurHash?.let {
                            BlurhashDecoder.decode(it, 4, 6)?.asImageBitmap()
                        }
                    }

                    val metrics = LocalContext.current.resources.displayMetrics
                    var baseScaleFill by remember(itemInner.id) {
                        val initialScale = if (placeholder != null) {
                            val scaleX = metrics.widthPixels.toFloat() / placeholder.width
                            val scaleY = metrics.heightPixels.toFloat() / placeholder.height
                            maxOf(scaleX, scaleY) / minOf(scaleX, scaleY)
                        } else 1f
                        mutableStateOf(initialScale)
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clipToBounds()
                            .background(Color.Black)
                            .pointerInput(actualPageInner, isLoadedInner, isLockedInner) {
                                if (!isLoadedInner || isLockedInner) return@pointerInput
                                detectTransformGestures(panZoomLock = false) { centroid, pan, zoom, rotation ->
                                    val current = currentTransformationStateInner.value
                                    val minAllowedScale = minOf(0.5f, 1f / baseScaleFill)
                                    val newScale = (current.scale * zoom).coerceIn(minAllowedScale, 5f)
                                    
                                    val pivot = centroid - Offset(size.width / 2f, size.height / 2f)
                                    val scaleChange = newScale / current.scale
                                    val zoomOffset = (current.offset - pivot) * scaleChange + pivot - current.offset
                                    val newOffset = current.offset + pan + zoomOffset
                                    
                                    viewModel.updateTransformation(
                                        actualPageInner,
                                        newScale,
                                        newOffset,
                                        current.rotation + rotation
                                    )
                                }
                            }
                            .pointerInput(actualPageInner, isLockedInner, state.isEditMode) {
                                detectTapGestures(
                                    onDoubleTap = { tapOffset ->
                                        if (!isLockedInner) {
                                            viewModel.handleDoubleTap(
                                                index = actualPageInner,
                                                tapOffset = tapOffset,
                                                center = Offset(size.width / 2f, size.height / 2f)
                                            )
                                        }
                                    },
                                    onTap = {
                                        if (isLockedInner) {
                                            viewModel.requestUnlock(actualPageInner)
                                        } else if (state.isEditMode) {
                                            viewModel.setEditMode(false)
                                        } else {
                                            viewModel.toggleControls()
                                        }
                                    }
                                )
                            },
                    ) {
                        val imageModifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (isLockedInner) {
                                    Modifier.blur(40.dp)
                                } else if (transformationInner.blur > 0f) {
                                    Modifier.blur(transformationInner.blur.dp)
                                } else {
                                    Modifier
                                }
                            )
                            .graphicsLayer {
                                val currentScale = if (isLoadedInner || placeholder != null) (transformationInner.scale * baseScaleFill) else 1f
                                val currentOffset = if (isLoadedInner && !isLockedInner) transformationInner.offset else Offset.Zero
                                val currentRotation = if (isLoadedInner && !isLockedInner) transformationInner.rotation else 0f
                                
                                scaleX = currentScale
                                scaleY = currentScale
                                translationX = currentOffset.x
                                translationY = currentOffset.y
                                rotationZ = currentRotation
                                
                                if (!isLockedInner && (transformationInner.brightness != 1f || transformationInner.contrast != 1f || transformationInner.saturation != 1f || transformationInner.hue != 0f || transformationInner.structure != 0f || transformationInner.details != 0f)) {
                                    val matrix = androidx.compose.ui.graphics.ColorMatrix()
                                    if (transformationInner.saturation != 1f) {
                                        matrix.setToSaturation(transformationInner.saturation)
                                    }
                                    if (transformationInner.brightness != 1f || transformationInner.contrast != 1f || transformationInner.structure != 0f || transformationInner.details != 0f) {
                                        val brightnessOffset = (transformationInner.brightness - 1f) * 255f
                                        val effectiveContrast = transformationInner.contrast * (1f + (transformationInner.structure * 0.08f) + (transformationInner.details * 0.04f))
                                        val bcMatrix = androidx.compose.ui.graphics.ColorMatrix(floatArrayOf(
                                            effectiveContrast, 0f, 0f, 0f, brightnessOffset,
                                            0f, effectiveContrast, 0f, 0f, brightnessOffset,
                                            0f, 0f, effectiveContrast, 0f, brightnessOffset,
                                            0f, 0f, 0f, 1f, 0f
                                        ))
                                        matrix.timesAssign(bcMatrix)
                                    }
                                    if (transformationInner.hue != 0f) {
                                        val cosVal = kotlin.math.cos(Math.toRadians(transformationInner.hue.toDouble())).toFloat()
                                        val sinVal = kotlin.math.sin(Math.toRadians(transformationInner.hue.toDouble())).toFloat()
                                        val lumR = 0.213f
                                        val lumG = 0.715f
                                        val lumB = 0.072f
                                        val mat = floatArrayOf(
                                            lumR + cosVal * (1 - lumR) + sinVal * (-lumR), lumG + cosVal * (-lumG) + sinVal * (-lumG), lumB + cosVal * (-lumB) + sinVal * (1 - lumB), 0f, 0f,
                                            lumR + cosVal * (-lumR) + sinVal * (0.143f), lumG + cosVal * (1 - lumG) + sinVal * (0.140f), lumB + cosVal * (-lumB) + sinVal * (-0.283f), 0f, 0f,
                                            lumR + cosVal * (-lumR) + sinVal * (-(1 - lumR)), lumG + cosVal * (-lumG) + sinVal * (lumG), lumB + cosVal * (1 - lumB) + sinVal * (lumB), 0f, 0f,
                                            0f, 0f, 0f, 1f, 0f
                                        )
                                        matrix.timesAssign(androidx.compose.ui.graphics.ColorMatrix(mat))
                                    }
                                    colorFilter = androidx.compose.ui.graphics.ColorFilter.colorMatrix(matrix)
                                }
                                clip = true
                            }

                        val sharedImageModifier = if (page == state.initialIndex) {
                            with(sharedTransitionScope) {
                                imageModifier.sharedElement(
                                    rememberSharedContentState(key = "image-${itemInner.id}"),
                                    animatedVisibilityScope = animatedVisibilityScope
                                )
                            }
                        } else {
                            imageModifier
                        }

                        if (!isLockedInner) {
                            coil3.compose.AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(itemInner.thumbnailUrl)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .blur(24.dp)
                                    .graphicsLayer { 
                                        alpha = 0.5f 
                                        clip = true
                                    },
                                contentScale = ContentScale.Crop,
                                alignment = Alignment.Center
                            )
                        }

                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(if (isLockedInner) itemInner.thumbnailUrl else itemInner.imageUrl)
                                .apply {
                                    if (retryCountInner > 0) {
                                        memoryCacheKey("${itemInner.id}_retry_$retryCountInner")
                                    }
                                }
                                .crossfade(true)
                                .build(),
                            contentDescription = itemInner.title,
                            modifier = sharedImageModifier,
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.Center,
                            onSuccess = { state ->
                                isLoadedInner = true
                                val size = state.painter.intrinsicSize
                                if (size.width > 0 && size.height > 0) {
                                    val scaleX = metrics.widthPixels.toFloat() / size.width
                                    val scaleY = metrics.heightPixels.toFloat() / size.height
                                    baseScaleFill = maxOf(scaleX, scaleY) / minOf(scaleX, scaleY)
                                }
                            },
                            loading = {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    placeholder?.let { bitmap ->
                                        androidx.compose.foundation.Image(
                                            bitmap = bitmap,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit,
                                            alignment = Alignment.Center
                                        )
                                    }
                                    
                                    coil3.compose.AsyncImage(
                                        model = ImageRequest.Builder(context)
                                            .data(itemInner.thumbnailUrl)
                                            .size(320, 480)
                                            .precision(coil3.size.Precision.INEXACT)
                                            .build(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit,
                                        alignment = Alignment.Center
                                    )

                                    Surface(
                                        shape = RoundedCornerShape(24.dp),
                                        color = Color.Black.copy(alpha = 0.2f),
                                        modifier = Modifier.size(80.dp),
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.background(
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        Color.White.copy(alpha = 0.15f),
                                                        Color.White.copy(alpha = 0.05f)
                                                    )
                                                )
                                            )
                                        ) {
                                            PremiumLoader(
                                                isPremium = itemInner.isPremium && !itemInner.isUnlocked,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            },
                            error = {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Warning,
                                        contentDescription = "Error",
                                        tint = Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    androidx.compose.material3.Button(
                                        onClick = { retryCountInner++ },
                                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                            containerColor = Color.White.copy(alpha = 0.2f),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Retry")
                                    }
                                }
                            }
                        )

                        if (isLockedInner) {
                            LockedOverlay(
                                isLoading = isAdLoading && state.wallpaperToUnlock?.id == itemInner.id,
                                onUnlock = { viewModel.requestUnlock(actualPageInner) }
                            )
                        }

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.6f),
                                            Color.Transparent,
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.75f),
                                        ),
                                    ),
                                ),
                        )
                    }
                }
            }

            // Top Controls
            AnimatedVisibility(
                visible = state.controlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                if (state.items.isNotEmpty()) {
                    val actualPage = pagerState.currentPage % state.items.size
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        FilledIconButton(
                            onClick = onBack,
                            modifier = Modifier.size(48.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = animatedDominantColor.copy(alpha = 0.95f),
                                contentColor = onDominantColor
                            )
                        ) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                        }
                        Spacer(modifier = Modifier.weight(1f))

                        val actualTransformation = state.transformations[actualPage]
                        if (actualTransformation != null && (actualTransformation.scale != 1f || actualTransformation.offset != Offset.Zero || actualTransformation.rotation != 0f)) {
                            FilledTonalButton(
                                onClick = { viewModel.resetTransformation(actualPage) },
                                colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                    containerColor = animatedDominantColor.copy(alpha = 0.95f),
                                    contentColor = onDominantColor
                                ),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                Text("Reset", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = animatedDominantColor.copy(alpha = 0.95f),
                            tonalElevation = 4.dp
                        ) {
                            Text(
                                text = "${actualPage + 1}/${state.items.size}",
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = onDominantColor,
                            )
                        }
                    }
                }
            }

            // Bottom Controls
            AnimatedVisibility(
                visible = state.controlsVisible && !state.isEditMode,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier.align(Alignment.BottomCenter),
            ) {
                if (state.items.isNotEmpty()) {
                    val actualPage = pagerState.currentPage % state.items.size
                    val item = state.items[actualPage]
                    
                    if (item is Wallpaper) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(16.dp),
                            shape = RoundedCornerShape(32.dp),
                            color = animatedDominantColor.copy(alpha = 0.95f),
                            tonalElevation = 12.dp,
                            shadowElevation = 8.dp
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                if (item.isPremium && !item.isUnlocked && !state.isPremiumPassActive) {
                                    Text(
                                        text = "Premium wallpaper · unlock with rewarded ad",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = onDominantColor,
                                        modifier = Modifier.padding(bottom = 12.dp),
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            scope.launch {
                                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                            }
                                        },
                                        enabled = pagerState.currentPage > 0,
                                        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                            containerColor = Color.Transparent,
                                            contentColor = onDominantColor,
                                            disabledContentColor = onDominantColor.copy(alpha = 0.38f)
                                        ),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.Rounded.ChevronLeft, contentDescription = null)
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = onDominantColor.copy(alpha = 0.1f),
                                    ) {
                                        Text(
                                            text = "${actualPage + 1} / ${state.items.size}",
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = onDominantColor,
                                        )
                                    }

                                    FilledTonalButton(
                                        onClick = {
                                            scope.launch {
                                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                            }
                                        },
                                        enabled = pagerState.currentPage < state.items.size - 1,
                                        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                            containerColor = Color.Transparent,
                                            contentColor = onDominantColor,
                                            disabledContentColor = onDominantColor.copy(alpha = 0.38f)
                                        ),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.Rounded.ChevronRight, contentDescription = null)
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                val screenWidth = LocalConfiguration.current.screenWidthDp

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            viewModel.requestDownload(actualPage)
                                        },
                                        modifier = Modifier.size(44.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                            containerColor = onDominantColor.copy(alpha = 0.15f),
                                            contentColor = onDominantColor
                                        ),
                                        enabled = !isAdLoading
                                    ) {
                                        if (isAdLoading && item.isPremium && !item.isUnlocked) {
                                             CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = onDominantColor
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Rounded.Download,
                                                contentDescription = "Download",
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            if (item.isPremium && !item.isUnlocked) {
                                                viewModel.requestUnlock(actualPage)
                                            } else {
                                                showSetSheet = true
                                            }
                                        },
                                        modifier = Modifier.size(44.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                            containerColor = onDominantColor.copy(alpha = 0.15f),
                                            contentColor = onDominantColor
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Wallpaper,
                                            contentDescription = "Set",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    FilledIconButton(
                                        onClick = { viewModel.toggleFavorite(actualPage) },
                                        modifier = Modifier.size(44.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = onDominantColor.copy(alpha = 0.15f),
                                            contentColor = onDominantColor
                                        )
                                    ) {
                                        Icon(
                                            imageVector = if (item.isFavorite) {
                                                Icons.Rounded.Favorite
                                            } else {
                                                Icons.Rounded.FavoriteBorder
                                            },
                                            contentDescription = "Favorite",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    FilledIconButton(
                                        onClick = {
                                            if (item.isPremium && !item.isUnlocked) {
                                                viewModel.requestUnlock(actualPage)
                                            } else {
                                                viewModel.share(actualPage)
                                            }
                                        },
                                        modifier = Modifier.size(44.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = onDominantColor.copy(alpha = 0.15f),
                                            contentColor = onDominantColor
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Share,
                                            contentDescription = "Share",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    FilledIconButton(
                                        onClick = { viewModel.setEditMode(!state.isEditMode) },
                                        modifier = Modifier.size(44.dp),
                                        colors = IconButtonDefaults.filledIconButtonColors(
                                            containerColor = if (state.isEditMode) onDominantColor else onDominantColor.copy(alpha = 0.15f),
                                            contentColor = if (state.isEditMode) animatedDominantColor else onDominantColor
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AutoFixHigh,
                                            contentDescription = "Edit Tools",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = "${item.downloads} downloads",
                                        color = onDominantColor.copy(alpha = 0.9f),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.Lock,
                                            contentDescription = null,
                                            tint = onDominantColor.copy(alpha = 0.7f),
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Text(
                                            text = "Tap image to hide controls",
                                            color = onDominantColor.copy(alpha = 0.7f),
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(start = 4.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (state.isWorking) {
                val currentItem = state.items.getOrNull(pagerState.currentPage % state.items.size)
                val isPremiumAction = currentItem is Wallpaper && currentItem.isPremium && !currentItem.isUnlocked
                val accentColor = if (isPremiumAction) Color(0xFFFFD700) else onDominantColor

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(enabled = false) { },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(32.dp),
                        color = animatedDominantColor.copy(alpha = 0.95f),
                        tonalElevation = 16.dp,
                        shadowElevation = 24.dp,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 48.dp, vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                        ) {
                            PremiumLoader(
                                isPremium = isPremiumAction,
                                color = onDominantColor,
                                label = null
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = state.workingLabel ?: "Processing…",
                                    color = onDominantColor,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                )
                            }

                            LinearProgressIndicator(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(4.dp),
                                color = accentColor,
                                trackColor = accentColor.copy(alpha = 0.2f),
                                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = if (state.isEditMode && state.controlsVisible) 240.dp else 112.dp)
        )

        val actualPage = pagerState.currentPage % state.items.size
        val transformation = state.transformations[actualPage] ?: ImageTransformation()
        var selectedTool by rememberSaveable { mutableStateOf("Brightness") }

        AnimatedVisibility(
            visible = state.isEditMode && state.controlsVisible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = animatedDominantColor.copy(alpha = 0.92f),
                tonalElevation = 12.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures { /* Consume taps inside the box */ }
                }
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tool Selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.setEditMode(false) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close",
                                tint = onDominantColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.resetTransformation(actualPage) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Reset",
                                tint = onDominantColor.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        listOf("Brightness", "Contrast", "Saturation", "Hue", "Blur", "Structure", "Details").forEach { tool ->
                            val isSelected = selectedTool == tool
                            val isLocked = tool != "Brightness" && !state.areEditToolsUnlocked
                            
                            Surface(
                                onClick = { selectedTool = tool },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) onDominantColor else Color.Transparent,
                                contentColor = if (isSelected) animatedDominantColor else onDominantColor,
                                modifier = Modifier.height(40.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    if (isLocked) {
                                        Icon(Icons.Rounded.Lock, null, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = tool,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Active Slider
                    when (selectedTool) {
                        "Brightness" -> EditSlider(
                            label = "Brightness",
                            value = transformation.brightness,
                            range = 0.5f..1.5f,
                            onValueChange = { viewModel.updateTransformation(actualPage, brightness = it) },
                            color = onDominantColor,
                            isLocked = false
                        )
                        "Contrast" -> EditSlider(
                            label = "Contrast",
                            value = transformation.contrast,
                            range = 0.5f..1.5f,
                            onValueChange = { viewModel.updateTransformation(actualPage, contrast = it) },
                            color = onDominantColor,
                            isLocked = !state.areEditToolsUnlocked
                        )
                        "Saturation" -> EditSlider(
                            label = "Saturation",
                            value = transformation.saturation,
                            range = 0f..2f,
                            onValueChange = { viewModel.updateTransformation(actualPage, saturation = it) },
                            color = onDominantColor,
                            isLocked = !state.areEditToolsUnlocked
                        )
                        "Hue" -> EditSlider(
                            label = "Hue",
                            value = transformation.hue,
                            range = 0f..360f,
                            onValueChange = { viewModel.updateTransformation(actualPage, hue = it) },
                            color = onDominantColor,
                            isLocked = !state.areEditToolsUnlocked
                        )
                        "Blur" -> EditSlider(
                            label = "Blur",
                            value = transformation.blur,
                            range = 0f..25f,
                            onValueChange = { viewModel.updateTransformation(actualPage, blur = it) },
                            color = onDominantColor,
                            isLocked = !state.areEditToolsUnlocked
                        )
                        "Structure" -> EditSlider(
                            label = "Structure",
                            value = transformation.structure,
                            range = 0f..10f,
                            onValueChange = { viewModel.updateTransformation(actualPage, structure = it) },
                            color = onDominantColor,
                            isLocked = !state.areEditToolsUnlocked
                        )
                        "Details" -> EditSlider(
                            label = "Details",
                            value = transformation.details,
                            range = 0f..10f,
                            onValueChange = { viewModel.updateTransformation(actualPage, details = it) },
                            color = onDominantColor,
                            isLocked = !state.areEditToolsUnlocked
                        )
                    }
                    
                    if (!state.areEditToolsUnlocked && selectedTool != "Brightness") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { showProToolsAd() },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = onDominantColor,
                                    contentColor = animatedDominantColor
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.Lock, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Unlock All Pro Tools", fontWeight = FontWeight.Bold)
                            }

                            Text(
                                text = "Watch rewarded ad to unlock editing tools for 24 hours",
                                style = MaterialTheme.typography.labelSmall,
                                color = onDominantColor.copy(alpha = 0.7f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    state.wallpaperToUnlock?.let { wallpaper ->
        val title = when (state.pendingAction) {
            PendingAction.DOWNLOAD -> "Download Wallpaper"
            PendingAction.SET_WALLPAPER -> "Set Wallpaper"
            else -> "Unlock Premium Content"
        }
        val description = when (state.pendingAction) {
            PendingAction.DOWNLOAD -> "Watch a short video to download this wallpaper to your gallery."
            PendingAction.SET_WALLPAPER -> "Watch a short video to apply this wallpaper to your device."
            else -> "Watch a short video to unlock this premium wallpaper."
        }
        val buttonText = when (state.pendingAction) {
            PendingAction.DOWNLOAD -> "Watch Ad & Download"
            PendingAction.SET_WALLPAPER -> "Watch Ad & Set"
            else -> "Watch Ad & Unlock"
        }

        UnlockPremiumDialog(
            wallpaper = wallpaper,
            title = title,
            description = description,
            buttonText = buttonText,
            isLoading = isAdLoading,
            onDismiss = {
                isAdLoading = false
                viewModel.dismissUnlockDialog()
            },
            onUnlock = {
                isAdLoading = true
            }
        )
    }
}

@Composable
private fun EditSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    color: Color,
    isLocked: Boolean = false,
    onLockClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isLocked, onClick = onLockClick)
            .alpha(if (isLocked) 0.5f else 1f)
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = color
                )
                if (isLocked) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "Locked",
                        tint = color,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Text(
                text = if (label == "Blur") String.format("%.0f", value) else String.format("%.2f", value),
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = if (isLocked) ({}) else onValueChange,
            valueRange = range,
            enabled = !isLocked,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = color.copy(alpha = 0.2f),
                disabledThumbColor = color.copy(alpha = 0.5f),
                disabledActiveTrackColor = color.copy(alpha = 0.3f)
            ),
            modifier = Modifier.height(36.dp)
        )
    }
}

@Composable
private fun LockedOverlay(
    isLoading: Boolean = false,
    onUnlock: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onUnlock
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.2f),
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = Color.White,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
            
            Text(
                text = "Premium Wallpaper",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            FilledTonalButton(
                onClick = onUnlock,
                enabled = !isLoading,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Loading...")
                } else {
                    Icon(Icons.Rounded.Wallpaper, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Unlock to View")
                }
            }
        }
    }
}

private fun preloadAdjacentImages(context: android.content.Context, urls: List<String>) {
    val loader = SingletonImageLoader.get(context)
    urls.forEach { url ->
        loader.enqueue(
            ImageRequest.Builder(context)
                .data(url)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build(),
        )
    }
}
