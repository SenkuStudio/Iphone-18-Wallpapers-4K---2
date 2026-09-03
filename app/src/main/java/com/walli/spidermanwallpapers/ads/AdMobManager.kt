package com.walli.spidermanwallpapers.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.walli.spidermanwallpapers.BuildConfig
import com.walli.spidermanwallpapers.util.AnalyticsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdMobManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyticsManager: AnalyticsManager
) : DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAppOpenAd = false
    private var appOpenLoadTime: Long = 0
    private var currentActivity: Activity? = null

    private var homeInterstitialAd: InterstitialAd? = null
    private var categoryInterstitialAd: InterstitialAd? = null

    // Rewarded Ad Pools
    private val rewardedAdsPools = mutableMapOf<String, MutableList<RewardedAd>>()
    private val maxRewardedPoolSize = 3

    // Native Ad Pools
    private var previewNativeAd: NativeAd? = null
    private val nativeAdsPools = mutableMapOf<String, MutableList<NativeAd>>()
    private val assignedNativeAds = object : LinkedHashMap<String, NativeAd>(15, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, NativeAd>?): Boolean {
            if (size > 10) { // Limit cache to 10 ads to prevent memory leaks
                eldest?.value?.destroy()
                return true
            }
            return false
        }
    }
    private val maxNativeAdsPerId = 2
    private val maxConcurrentLoads = 4
    private var globalNativeLoadingCount = 0
    private val TAG = "AdMobManager"
    
    // Test ID for fallback if BuildConfig is empty
    private val TEST_NATIVE_ID = "ca-app-pub-3940256099942544/2247696110"
    
    private val _isRewardedLoaded = MutableStateFlow(false)
    val isRewardedLoaded = _isRewardedLoaded.asStateFlow()

    private val _nativeAdLoadedSignal = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val nativeAdLoadedSignal = _nativeAdLoadedSignal.asSharedFlow()

    private val _nativeAdFailedSignal = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val nativeAdFailedSignal = _nativeAdFailedSignal.asSharedFlow()

    private var lastAdShowTime: Long = 0
    private var homeMeaningfulOpenCounter = 0
    private var categoryMeaningfulOpenCounter = 0
    private var nextHomeThreshold = (6..9).random()
    private var nextCategoryThreshold = (5..8).random()
    private val adCooldownMillis = 30000L // 30s cooldown between interstitials

    private var isAppInForeground = false
    private var isShowingAd = false
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized = _isInitialized.asStateFlow()

    private val loadingIds = mutableSetOf<String>()

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        isAppInForeground = true
        if (_isInitialized.value) {
            // Add a very small delay to ensure the Activity is fully resumed
            // and the UI is ready before showing an App Open ad.
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (isAppInForeground) {
                    showAppOpenAdIfAvailable()
                }
            }, 500)
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        isAppInForeground = false
    }

    fun warmUp() {
        if (_isInitialized.value) return
        _isInitialized.value = true
        Log.d(TAG, "AdMob Manager Initialized. Warming up essential ads.")
        loadAppOpenAd()
        
        // Preload all critical native ad units
        loadNativeAd(BuildConfig.ADMOB_NATIVE_HOME)
        loadNativeAd(BuildConfig.ADMOB_NATIVE_CATEGORIES)
        loadNativeAd(BuildConfig.ADMOB_NATIVE_WALLPAPERS)
        loadNativeAd(BuildConfig.ADMOB_NATIVE_PREVIEW)

        // Preload all interstitial units
        loadInterstitial(BuildConfig.ADMOB_INTERSTITIAL_HOME) { homeInterstitialAd = it }
        loadInterstitial(BuildConfig.ADMOB_INTERSTITIAL_CATEGORY) { categoryInterstitialAd = it }

        // Preload ALL rewarded ad units
        loadRewarded(BuildConfig.ADMOB_REWARDED_HOME)
        loadRewarded(BuildConfig.ADMOB_REWARDED_CATEGORY)
        loadRewarded(BuildConfig.ADMOB_REWARDED_DOWNLOAD)
        loadRewarded(BuildConfig.ADMOB_REWARDED_SET_WALLPAPER)
        loadRewarded(BuildConfig.ADMOB_REWARDED_PREVIEW)
        loadRewarded(BuildConfig.ADMOB_REWARDED_PRO_TOOLS)
    }

    private fun loadAllAds() {
        // Removed aggressive preloading of all IDs to improve Match Rate
    }

    fun maybeShowCategoryInterstitial(activity: Activity?, onContinue: () -> Unit) {
        categoryMeaningfulOpenCounter++

        val currentTime = System.currentTimeMillis()
        val isCooldownOver = (currentTime - lastAdShowTime) >= adCooldownMillis
        val isThresholdReached = categoryMeaningfulOpenCounter >= nextCategoryThreshold

        if (!isThresholdReached || !isCooldownOver || categoryInterstitialAd == null || activity == null) {
            onContinue()
            if (categoryInterstitialAd == null && isAppInForeground) {
                loadInterstitial(BuildConfig.ADMOB_INTERSTITIAL_CATEGORY) { categoryInterstitialAd = it }
            }
            return
        }

        showInterstitial(
            activity,
            categoryInterstitialAd,
            {
                categoryInterstitialAd = null
                lastAdShowTime = System.currentTimeMillis()
                categoryMeaningfulOpenCounter = 0
                nextCategoryThreshold = (5..8).random()
            },
            BuildConfig.ADMOB_INTERSTITIAL_CATEGORY,
            onContinue
        )
    }

    fun maybeShowHomeInterstitial(activity: Activity?, onContinue: () -> Unit) {
        homeMeaningfulOpenCounter++

        val currentTime = System.currentTimeMillis()
        val isCooldownOver = (currentTime - lastAdShowTime) >= adCooldownMillis
        val isThresholdReached = homeMeaningfulOpenCounter >= nextHomeThreshold

        if (!isThresholdReached || !isCooldownOver || homeInterstitialAd == null || activity == null) {
            onContinue()
            // If not loaded, ensure we try to load for next time
            if (homeInterstitialAd == null && isAppInForeground) {
                loadInterstitial(BuildConfig.ADMOB_INTERSTITIAL_HOME) { homeInterstitialAd = it }
            }
            return
        }

        showInterstitial(
            activity,
            homeInterstitialAd,
            {
                homeInterstitialAd = null
                lastAdShowTime = System.currentTimeMillis()
                homeMeaningfulOpenCounter = 0
                nextHomeThreshold = (6..9).random()
            },
            BuildConfig.ADMOB_INTERSTITIAL_HOME,
            onContinue
        )
    }

    private fun showInterstitial(
        activity: Activity?,
        ad: InterstitialAd?,
        clearAd: () -> Unit,
        adUnitId: String,
        onContinue: () -> Unit
    ) {
        if (activity == null || ad == null) {
            onContinue()
            if (isAppInForeground) loadInterstitial(adUnitId) {
                when (adUnitId) {
                    BuildConfig.ADMOB_INTERSTITIAL_HOME -> homeInterstitialAd = it
                    BuildConfig.ADMOB_INTERSTITIAL_CATEGORY -> categoryInterstitialAd = it
                }
            }
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                isShowingAd = false
                clearAd()
                if (isAppInForeground) loadInterstitial(adUnitId) {
                    when (adUnitId) {
                        BuildConfig.ADMOB_INTERSTITIAL_HOME -> homeInterstitialAd = it
                        BuildConfig.ADMOB_INTERSTITIAL_CATEGORY -> categoryInterstitialAd = it
                    }
                }
                onContinue()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                isShowingAd = false
                clearAd()
                analyticsManager.logAdFailed(adUnitId, "interstitial_show", adError.message)
                if (isAppInForeground) loadInterstitial(adUnitId) {
                    when (adUnitId) {
                        BuildConfig.ADMOB_INTERSTITIAL_HOME -> homeInterstitialAd = it
                        BuildConfig.ADMOB_INTERSTITIAL_CATEGORY -> categoryInterstitialAd = it
                    }
                }
                onContinue()
            }

            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
                analyticsManager.logAdImpression(adUnitId, "interstitial")
            }
        }
        ad.show(activity)
    }

    fun showHomeRewarded(activity: Activity?, onReward: () -> Unit, onDismiss: () -> Unit = {}, onError: (String) -> Unit = {}) {
        showRewarded(activity, BuildConfig.ADMOB_REWARDED_HOME, onReward, onDismiss, onError)
    }

    fun showCategoryRewarded(activity: Activity?, onReward: () -> Unit, onDismiss: () -> Unit = {}, onError: (String) -> Unit = {}) {
        showRewarded(activity, BuildConfig.ADMOB_REWARDED_CATEGORY, onReward, onDismiss, onError)
    }

    fun showPreviewRewarded(activity: Activity?, onReward: () -> Unit, onDismiss: () -> Unit = {}, onError: (String) -> Unit = {}) {
        showRewarded(activity, BuildConfig.ADMOB_REWARDED_PREVIEW, onReward, onDismiss, onError)
    }

    fun showDownloadRewarded(activity: Activity?, onReward: () -> Unit, onDismiss: () -> Unit = {}, onError: (String) -> Unit = {}) {
        showRewarded(activity, BuildConfig.ADMOB_REWARDED_DOWNLOAD, onReward, onDismiss, onError)
    }

    fun showProToolsRewarded(activity: Activity?, onReward: () -> Unit, onDismiss: () -> Unit = {}, onError: (String) -> Unit = {}) {
        showRewarded(activity, BuildConfig.ADMOB_REWARDED_PRO_TOOLS, onReward, onDismiss, onError)
    }

    fun showSetWallpaperRewarded(activity: Activity?, onReward: () -> Unit, onDismiss: () -> Unit = {}, onError: (String) -> Unit = {}) {
        showRewarded(activity, BuildConfig.ADMOB_REWARDED_SET_WALLPAPER, onReward, onDismiss, onError)
    }

    private fun showRewarded(
        activity: Activity?,
        adUnitId: String,
        onReward: () -> Unit,
        onDismiss: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (activity == null || adUnitId.isBlank()) {
            onDismiss()
            return
        }

        val ad = synchronized(rewardedAdsPools) {
            val pool = rewardedAdsPools[adUnitId]
            if (pool != null && pool.isNotEmpty()) {
                val removedAd = pool.removeAt(0)
                // Proactively start loading the next ad immediately
                if (isAppInForeground) loadRewarded(adUnitId)
                removedAd
            } else {
                null
            }
        }

        if (ad == null) {
            onError("Spider Hero Wallpapers 4K The reward isn't ready yet. Check your connection or try again in a moment.")
            if (isAppInForeground) loadRewarded(adUnitId)
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                updateRewardedState()
                if (isAppInForeground) loadRewarded(adUnitId)
                onDismiss()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                updateRewardedState()
                analyticsManager.logAdFailed(adUnitId, "rewarded_show", adError.message)
                if (isAppInForeground) loadRewarded(adUnitId)
                onError("Failed to show ad. Please check your internet connection.")
                onDismiss()
            }

            override fun onAdShowedFullScreenContent() {
                analyticsManager.logAdImpression(adUnitId, "rewarded")
            }
        }

        ad.show(activity, OnUserEarnedRewardListener {
            onReward()
        })
    }

    private fun updateRewardedState() {
        val anyAvailable = synchronized(rewardedAdsPools) {
            rewardedAdsPools.values.any { it.isNotEmpty() }
        }
        _isRewardedLoaded.value = anyAvailable
    }

    fun createAdRequest(collapsible: String? = null): AdRequest {
        val builder = AdRequest.Builder()
            .addKeyword("spiderman")
            .addKeyword("marvel")
            .addKeyword("wallpaper")
            .addKeyword("superhero")
            .addKeyword("4k")
        
        if (collapsible != null) {
            val extras = Bundle().apply {
                putString("collapsible", collapsible)
            }
            builder.addNetworkExtrasBundle(com.google.ads.mediation.admob.AdMobAdapter::class.java, extras)
        }
        
        return builder.build()
    }

    private fun canRequestAd(adUnitId: String): Boolean {
        return _isInitialized.value && adUnitId.isNotBlank()
    }

    private fun handleAdLoadFailure(adUnitId: String, error: String) {
        analyticsManager.logAdFailed(adUnitId, "load", error)
        Log.e(TAG, "Ad load failed for $adUnitId: $error")
    }

    private fun handleAdLoadSuccess(adUnitId: String) {
        // Governor logic removed
    }

    private fun loadInterstitial(adUnitId: String, onLoaded: (InterstitialAd) -> Unit) {
        if (loadingIds.contains(adUnitId) || !canRequestAd(adUnitId)) return

        loadingIds.add(adUnitId)
        InterstitialAd.load(
            context,
            adUnitId,
            createAdRequest(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    loadingIds.remove(adUnitId)
                    handleAdLoadSuccess(adUnitId)
                    Log.d(TAG, "Interstitial loaded: ${ad.responseInfo.mediationAdapterClassName}")
                    analyticsManager.logAdLoad(adUnitId, "interstitial")
                    onLoaded(ad)
                }
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    loadingIds.remove(adUnitId)
                    handleAdLoadFailure(adUnitId, loadAdError.message)
                }
            },
        )
    }

    fun loadRewarded(adUnitId: String) {
        if (adUnitId.isBlank()) return
        
        synchronized(rewardedAdsPools) {
            val pool = rewardedAdsPools.getOrPut(adUnitId) { mutableListOf() }
            if (pool.size >= maxRewardedPoolSize) return
        }

        if (loadingIds.contains(adUnitId) || !canRequestAd(adUnitId)) return

        loadingIds.add(adUnitId)
        RewardedAd.load(
            context,
            adUnitId,
            createAdRequest(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    loadingIds.remove(adUnitId)
                    handleAdLoadSuccess(adUnitId)
                    synchronized(rewardedAdsPools) {
                        val pool = rewardedAdsPools.getOrPut(adUnitId) { mutableListOf() }
                        if (pool.size < maxRewardedPoolSize) {
                            pool.add(ad)
                        }
                    }
                    analyticsManager.logAdLoad(adUnitId, "rewarded")
                    updateRewardedState()
                    
                    // Fill the pool if not full yet
                    if (isAppInForeground) {
                        synchronized(rewardedAdsPools) {
                            val pool = rewardedAdsPools[adUnitId]
                            if (pool != null && pool.size < maxRewardedPoolSize) {
                                loadRewarded(adUnitId)
                            }
                        }
                    }
                }
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    loadingIds.remove(adUnitId)
                    handleAdLoadFailure(adUnitId, loadAdError.message)
                }
            }
        )
    }

    fun loadNativeAd(adUnitId: String) {
        val id = if (adUnitId.isBlank()) TEST_NATIVE_ID else adUnitId
        
        val pool = synchronized(nativeAdsPools) {
            nativeAdsPools.getOrPut(id) { mutableListOf() }
        }

        // Optimization for Match Rate:
        if (pool.size >= maxNativeAdsPerId) return
        if (globalNativeLoadingCount >= maxConcurrentLoads) return
        
        if (loadingIds.contains(id)) return
        
        if (!canRequestAd(id)) {
            _nativeAdFailedSignal.tryEmit(id)
            return
        }
        
        loadingIds.add(id)
        val adOptions = NativeAdOptions.Builder()
            .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
            .setVideoOptions(VideoOptions.Builder().setStartMuted(true).build())
            .setRequestMultipleImages(false)
            .setReturnUrlsForImageAssets(false)
            .build()

        globalNativeLoadingCount++
        
        AdLoader.Builder(context, id)
            .forNativeAd { ad ->
                loadingIds.remove(id)
                handleAdLoadSuccess(id)
                globalNativeLoadingCount--
                
                synchronized(nativeAdsPools) {
                    pool.add(ad)
                }
                _nativeAdLoadedSignal.tryEmit(id)
                analyticsManager.logAdLoad(id, "native")

                // Fill the pool if not full yet
                if (isAppInForeground) {
                    synchronized(nativeAdsPools) {
                        val currentPool = nativeAdsPools[id]
                        if (currentPool != null && currentPool.size < maxNativeAdsPerId) {
                            loadNativeAd(id)
                        }
                    }
                }
            }
            .withAdListener(object : com.google.android.gms.ads.AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadingIds.remove(id)
                    globalNativeLoadingCount--
                    handleAdLoadFailure(id, error.message)
                    _nativeAdFailedSignal.tryEmit(id)
                }
                
                override fun onAdImpression() {
                    analyticsManager.logAdImpression(id, "native")
                }
            })
            .withNativeAdOptions(adOptions)
            .build()
            .loadAd(createAdRequest())
    }

    fun getNativeAd(adUnitId: String, key: String? = null): NativeAd? {
        val id = if (adUnitId.isBlank()) TEST_NATIVE_ID else adUnitId

        if (key != null) {
            val assignedAd = assignedNativeAds[key]
            if (assignedAd != null) {
                return assignedAd
            }
        }

        val ad = synchronized(nativeAdsPools) {
            val pool = nativeAdsPools[id]
            if (pool != null && pool.isNotEmpty()) {
                pool.removeAt(0)
            } else {
                null
            }
        }

        if (ad != null && key != null) {
            assignedNativeAds[key] = ad
        }

        // Trigger load for next time, but with a slight delay to avoid
        // sudden request spikes when multiple ads are consumed during scrolling.
        if (_isInitialized.value && isAppInForeground) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (isAppInForeground) loadNativeAd(adUnitId)
            }, 200)
        }
        return ad
    }

    /** App Open Ad Logic */
    private fun loadAppOpenAd() {
        val adUnitId = BuildConfig.ADMOB_APP_OPEN
        if (isLoadingAppOpenAd || isAppOpenAdAvailable() || adUnitId.isBlank()) return
        if (!canRequestAd(adUnitId)) return

        isLoadingAppOpenAd = true
        AppOpenAd.load(
            context,
            adUnitId,
            createAdRequest(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    handleAdLoadSuccess(adUnitId)
                    appOpenAd = ad
                    isLoadingAppOpenAd = false
                    appOpenLoadTime = System.currentTimeMillis()
                    analyticsManager.logAdLoad(adUnitId, "app_open")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAppOpenAd = false
                    handleAdLoadFailure(adUnitId, loadAdError.message)
                }
            }
        )
    }

    private fun showAppOpenAdIfAvailable() {
        if (isShowingAd || !isAppOpenAdAvailable()) {
            loadAppOpenAd()
            return
        }

        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
                loadAppOpenAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isShowingAd = false
                analyticsManager.logAdFailed(BuildConfig.ADMOB_APP_OPEN, "app_open_show", adError.message)
                loadAppOpenAd()
            }

            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
                analyticsManager.logAdImpression(BuildConfig.ADMOB_APP_OPEN, "app_open")
            }
        }

        currentActivity?.let {
            appOpenAd?.show(it)
        } ?: run {
            loadAppOpenAd()
        }
    }

    private fun isAppOpenAdAvailable(): Boolean {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4)
    }

    private fun wasLoadTimeLessThanNHoursAgo(numHours: Long): Boolean {
        val dateDifference = System.currentTimeMillis() - appOpenLoadTime
        val numMilliSecondsPerHour: Long = 3600000
        return dateDifference < numMilliSecondsPerHour * numHours
    }

    /** Activity Lifecycle Callbacks to track current activity */
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) { currentActivity = activity }
    override fun onActivityResumed(activity: Activity) { currentActivity = activity }
    override fun onActivityPaused(activity: Activity) { currentActivity = null }
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) { currentActivity = null }
}
