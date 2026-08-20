package com.walli.spidermanwallpaper.ui.screens.home


import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.rememberDrawerState
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.walli.spidermanwallpaper.R
import com.walli.spidermanwallpaper.ads.AdsViewModel
import androidx.compose.ui.platform.LocalConfiguration
import com.walli.spidermanwallpaper.domain.model.WallpaperSort
import com.walli.spidermanwallpaper.ui.common.LoadState
import com.walli.spidermanwallpaper.ui.components.BannerAd
import com.walli.spidermanwallpaper.ui.components.NativeAdItem
import com.walli.spidermanwallpaper.ui.components.EmptyState
import com.walli.spidermanwallpaper.ui.components.FeaturedHeroCard
import com.walli.spidermanwallpaper.ui.components.NoInternetState
import com.walli.spidermanwallpaper.ui.components.WallpaperCard
import com.walli.spidermanwallpaper.ui.components.WallpaperCardShimmer

import com.walli.spidermanwallpaper.util.findActivity
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import androidx.compose.runtime.snapshotFlow
import com.walli.spidermanwallpaper.BuildConfig
import com.walli.spidermanwallpaper.ui.components.UnlockPremiumDialog

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun HomeRoute(
    onOpenPreview: () -> Unit,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    initialCategoryId: Int? = null,
    viewModel: HomeViewModel = hiltViewModel(),
    adsViewModel: AdsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context.findActivity()

    LaunchedEffect(initialCategoryId) {
        if (initialCategoryId != null) {
            viewModel.selectCategory(com.walli.spidermanwallpaper.domain.model.WallpaperCategory(id = initialCategoryId))
        }
    }

    val onWallpaperClick: (com.walli.spidermanwallpaper.domain.model.Wallpaper) -> Unit = { wallpaper ->
        val connectivityManager = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        val isOnline = capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        val isUnlocked = state.unlockedIds.contains(wallpaper.id) || state.isPremiumPassActive
        if (!isOnline && wallpaper.isPremium && !isUnlocked) {
            android.widget.Toast.makeText(context, "Internet required to unlock premium content", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            if (wallpaper.isPremium && !isUnlocked) {
                viewModel.onWallpaperClick(wallpaper)
            } else {
                adsViewModel.maybeShowHomeInterstitial(activity) {
                    val index = state.wallpapers.indexOfFirst { it.id == wallpaper.id }
                    if (index != -1) {
                        viewModel.openPreview(index)
                        onOpenPreview()
                    }
                }
            }
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val isRewardedReady by adsViewModel.isRewardedReady.collectAsStateWithLifecycle()
    var isAdLoading by remember { mutableStateOf(false) }

    val showAdAndUnlock: (com.walli.spidermanwallpaper.domain.model.Wallpaper) -> Unit = { wallpaper ->
        adsViewModel.showHomeRewarded(
            activity = activity,
            onReward = {
                viewModel.unlockWallpaper(wallpaper)
                // Skip interstitial when opening preview after a rewarded ad
                viewModel.openPreview(state.wallpapers.indexOf(wallpaper))
                onOpenPreview()
            },
            onDismiss = {
                isAdLoading = false
            },
            onError = { message ->
                isAdLoading = false
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        // Disable gestures when closed to prevent accidental touch interception
        // especially after returning from external activities.
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            DrawerContent(
                onItemClick = { item: DrawerItem ->
                    scope.launch {
                        // 1. Close the drawer with animation.
                        drawerState.close()
                        
                        // 2. Sufficient delay to ensure the animation finishes and the 
                        // Compose UI removes the touch-blocking scrim before focus is lost.
                        kotlinx.coroutines.delay(300)
                        
                        // 3. Trigger the external action
                        handleDrawerAction(context, item)
                    }
                }
            )
        }
    ) {
        HomeScreen(
            state = state,
            sharedTransitionScope = sharedTransitionScope,
            animatedVisibilityScope = animatedVisibilityScope,
            onRefresh = viewModel::refresh,
            onLoadMore = viewModel::loadMore,
            onSortSelected = viewModel::changeSort,
            onCategorySelected = viewModel::selectCategory,
            onWallpaperClick = onWallpaperClick,
            onMenuClick = { scope.launch { drawerState.open() } }
        )
    }

    state.wallpaperToUnlock?.let { wallpaper ->
        UnlockPremiumDialog(
            wallpaper = wallpaper,
            isLoading = isAdLoading,
            onDismiss = {
                isAdLoading = false
                viewModel.dismissUnlockDialog()
            },
            onUnlock = {
                if (isRewardedReady) {
                    showAdAndUnlock(wallpaper)
                } else {
                    isAdLoading = true
                }
            }
        )
    }
}

@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
private fun HomeScreen(
    state: HomeUiState,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onSortSelected: (WallpaperSort) -> Unit,
    onCategorySelected: (com.walli.spidermanwallpaper.domain.model.WallpaperCategory) -> Unit,
    onWallpaperClick: (com.walli.spidermanwallpaper.domain.model.Wallpaper) -> Unit,
    onMenuClick: () -> Unit,
) {
    val gridState = rememberLazyGridState()
    val pullState = rememberPullToRefreshState()
    val configuration = LocalConfiguration.current
    val columns = when {
        configuration.screenWidthDp >= 900 -> 4
        configuration.screenWidthDp >= 600 -> 3
        else -> 2
    }

    ObserveGridPagination(
        gridState = gridState,
        totalCount = state.wallpapers.size,
        hasNext = state.hasNext,
        onLoadMore = onLoadMore,
    )

    Scaffold(
        topBar = {
            HomeTopBar(onMenuClick = onMenuClick)
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (!state.isOnline) {
            NoInternetState(
                modifier = Modifier.padding(padding),
                onRetry = onRefresh
            )
        } else {
            when {
                state.loadState is LoadState.Error && state.wallpapers.isEmpty() -> {
                    val message = (state.loadState as LoadState.Error).message
                    EmptyState(
                        title = "Couldn’t load wallpapers",
                        subtitle = message,
                        actionText = "Retry",
                        onAction = onRefresh,
                    )
                }

                else -> {
                    PullToRefreshBox(
                        isRefreshing = state.loadState is LoadState.Refreshing,
                        onRefresh = onRefresh,
                        state = pullState,
                        modifier = Modifier.padding(padding)
                    ) {
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Fixed(columns),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 100.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            if (state.featuredWallpapers.isNotEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    val featuredWallpaper = state.featuredWallpapers.first()
                                    FeaturedHeroCard(
                                        wallpaper = featuredWallpaper,
                                        isFavorite = state.favoriteIds.contains(featuredWallpaper.id),
                                        isUnlocked = state.unlockedIds.contains(featuredWallpaper.id) || state.isPremiumPassActive,
                                        onClick = { onWallpaperClick(featuredWallpaper) },
                                        sharedTransitionScope = sharedTransitionScope,
                                        animatedVisibilityScope = animatedVisibilityScope
                                    )
                                }
                            } else if (state.loadState is LoadState.Loading) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    com.walli.spidermanwallpaper.ui.components.FeaturedHeroShimmer()
                                }
                            }

                            if (state.recentWallpapers.isNotEmpty()) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                        Text(
                                            text = "Continue Exploring",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        ) {
                                            items(state.recentWallpapers.take(8)) { wallpaper ->
                                                Box(modifier = Modifier.width(140.dp)) {
                                                    WallpaperCard(
                                                        wallpaper = wallpaper,
                                                        isFavorite = state.favoriteIds.contains(wallpaper.id),
                                                        isUnlocked = state.unlockedIds.contains(wallpaper.id) || state.isPremiumPassActive,
                                                        onClick = { onWallpaperClick(wallpaper) },
                                                        sharedTransitionScope = sharedTransitionScope,
                                                        animatedVisibilityScope = animatedVisibilityScope
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Explore",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                        )
                                    }

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                                    ) {
                                        items(state.categories) { category ->
                                            FilterChip(
                                                selected = state.selectedCategoryId == category.id,
                                                onClick = { onCategorySelected(category) },
                                                label = { 
                                                    Text(
                                                        category.name.orEmpty(),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    ) 
                                                },
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                        }
                                    }

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                                    ) {
                                        items(WallpaperSort.entries) { sort ->
                                            FilterChip(
                                                selected = state.sort == sort,
                                                onClick = { onSortSelected(sort) },
                                                label = { 
                                                    Text(
                                                        sort.label,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    ) 
                                                },
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            when {
                                state.loadState is LoadState.Loading && state.wallpapers.isEmpty() -> {
                                    items(6) {
                                        WallpaperCardShimmer()
                                    }
                                }

                                state.loadState is LoadState.Empty -> {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        EmptyState(
                                            title = "No results",
                                            subtitle = "Try a different keyword, sort, or category.",
                                            modifier = Modifier.height(280.dp),
                                        )
                                    }
                                }

                                else -> {
                                    state.wallpapers.forEachIndexed { index, wallpaper ->
                                        item(
                                            key = wallpaper.id,
                                            contentType = "wallpaper"
                                        ) {
                                            WallpaperCard(
                                                wallpaper = wallpaper,
                                                isFavorite = state.favoriteIds.contains(wallpaper.id),
                                                isUnlocked = state.unlockedIds.contains(wallpaper.id) || state.isPremiumPassActive,
                                                onClick = { onWallpaperClick(wallpaper) },
                                                sharedTransitionScope = sharedTransitionScope,
                                                animatedVisibilityScope = animatedVisibilityScope
                                            )
                                        }

                                        // Insert Native Ad every 4 items
                                        if (index > 0 && (index + 1) % 5 == 0) {
                                            item(
                                                key = "native-ad-${index}",
                                                contentType = "native_ad"
                                            ) {
                                                NativeAdItem(
                                                    adUnitId = BuildConfig.ADMOB_NATIVE_HOME,
                                                    key = "home-grid-ad-$index",
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }

                                    // Add shimmers at the bottom when loading more (Appending)
                                    if (state.loadState is LoadState.Appending) {
                                        items(2) {
                                            WallpaperCardShimmer()
                                        }
                                    }
                                }
                            }

                            item(span = { GridItemSpan(maxLineSpan) }) {
                                if (state.loadState is LoadState.Appending || state.wallpapers.isNotEmpty()) {
                                    BannerAd(
                                        adUnitId = BuildConfig.ADMOB_BANNER_HOME_BOTTOM,
                                        modifier = Modifier.padding(top = 16.dp),
                                        collapsible = "bottom"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(onMenuClick: () -> Unit) {
    CenterAlignedTopAppBar(
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Rounded.Menu, contentDescription = "Menu")
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.logoappspiderman),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Spiderman Wallpapers",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

private enum class DrawerItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HowToUse("How to use", Icons.Rounded.Info),
    Instagram("Instagram", Icons.Rounded.CameraAlt),
    ShareApp("Share app", Icons.Rounded.Share),
    RateUs("Rate us", Icons.Rounded.Star),
    MoreApps("More apps", Icons.Rounded.Apps),
    ContactUs("Contact us", Icons.Rounded.Email)
}

@Composable
private fun DrawerContent(onItemClick: (DrawerItem) -> Unit) {
    ModalDrawerSheet {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.logoappspiderman),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Spiderman Wallpapers",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        DrawerItem.entries.forEach { item ->
            NavigationDrawerItem(
                label = { Text(item.label) },
                selected = false,
                onClick = { onItemClick(item) },
                icon = { Icon(item.icon, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            text = "Version 1.0.0",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}

private fun handleDrawerAction(context: android.content.Context, item: DrawerItem) {
    val activity = context.findActivity()
    when (item) {
        DrawerItem.HowToUse -> {
            Toast.makeText(context, "Welcome to Spiderman Wallpapers 4K! Swipe to explore and tap to preview wallpapers.", Toast.LENGTH_LONG).show()
        }
        DrawerItem.Instagram -> {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/senku_studio"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
        DrawerItem.ShareApp -> {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Check out Spiderman Wallpapers 4K App")
                putExtra(Intent.EXTRA_TEXT, "Download Spiderman Wallpapers 4K for amazing wallpapers: https://play.google.com/store/apps/details?id=com.walli.spidermanwallpaper")
            }
            context.startActivity(Intent.createChooser(intent, "Share via"))
        }
        DrawerItem.RateUs -> {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.walli.spidermanwallpaper"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                if (activity != null) {
                    activity.startActivity(intent)
                } else {
                    context.startActivity(intent)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Play Store not found", Toast.LENGTH_SHORT).show()
            }
        }
        DrawerItem.MoreApps -> {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/dev?id=8066128835537801410"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                if (activity != null) {
                    activity.startActivity(intent)
                } else {
                    context.startActivity(intent)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Play Store not found", Toast.LENGTH_SHORT).show()
            }
        }
        DrawerItem.ContactUs -> {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:skstudioapp7@gmail.com")
                putExtra(Intent.EXTRA_SUBJECT, "Spiderman Wallpapers 4K Feedback")
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
private fun ObserveGridPagination(
    gridState: LazyGridState,
    totalCount: Int,
    hasNext: Boolean,
    onLoadMore: () -> Unit,
) {
    LaunchedEffect(gridState, totalCount, hasNext) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .map { lastVisible -> hasNext && totalCount > 0 && lastVisible >= totalCount - 4 }
            .distinctUntilChanged()
            .filter { it }
            .collect { onLoadMore() }
    }
}
