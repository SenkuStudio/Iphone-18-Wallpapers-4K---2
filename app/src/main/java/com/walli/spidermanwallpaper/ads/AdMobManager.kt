package com.walli.spidermanwallpaper.ads

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
import com.walli.spidermanwallpaper.BuildConfig
import com.walli.spidermanwallpaper.util.AnalyticsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private var setWallpaperInterstitialAd: InterstitialAd? = null

    // Rewarded Ad Pools
    private val rewardedAdsPools = mutableMapOf<String, MutableList<RewardedAd>>()
    private val maxRewardedPoolSize = 2

    // Native Ad Pools
    private var previewNativeAd: NativeAd? = null
    private val nativeAdsPools = mutableMapOf<String, MutableList<NativeAd>>()
    private val assignedNativeAds = mutableMapOf<String, NativeAd>()
    private val maxNativeAdsPerId = 5
    private val maxConcurrentLoads = 8
    private var globalNativeLoadingCount = 0
    private val TAG = "AdMobManager"
    
    // Test ID for fallback if BuildConfig is empty
    private val TEST_NATIVE_ID = "ca-app-pub-3940256099942544/2247696110"
    
    private val _isRewardedLoaded = MutableStateFlow(false)
    val isRewardedLoaded = _isRewardedLoaded.asStateFlow()

    private var lastAdShowTime: Long = 0
    private var homeMeaningfulOpenCounter = 0
    private var categoryMeaningfulOpenCounter = 0
    private var nextHomeThreshold = (6..9).random()
    private var nextCategoryThreshold = (5..8).random()
    private val adCooldownMillis = (90..150).random() * 1000L

    private var isAppInForeground = false
    private var isShowingAd = false
    private var isInitialized = false
    private val loadingIds = mutableSetOf<String>()

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        isAppInForeground = true
        if (isInitialized) {
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
        isInitialized = true
        loadAllAds()
        loadAppOpenAd()
    }

    private fun loadAllAds() {
        if (!isInitialized) return
        Log.d(TAG, "Loading all ads...")
        loadInterstitial(BuildConfig.ADMOB_INTERSTITIAL_HOME) { homeInterstitialAd = it }
        loadInterstitial(BuildConfig.ADMOB_INTERSTITIAL_CATEGORY) { categoryInterstitialAd = it }
        loadInterstitial(BuildConfig.ADMOB_INTERSTITIAL_SET_WALLPAPER) { setWallpaperInterstitialAd = it }

        // Fill Rewarded Pools
        loadRewarded(BuildConfig.ADMOB_REWARDED_HOME)
        loadRewarded(BuildConfig.ADMOB_REWARDED_CATEGORY)
        loadRewarded(BuildConfig.ADMOB_REWARDED_PREVIEW)
        loadRewarded(BuildConfig.ADMOB_REWARDED_DOWNLOAD)
        loadRewarded(BuildConfig.ADMOB_REWARDED_PRO_TOOLS)
        loadRewarded(BuildConfig.ADMOB_REWARDED_SET_WALLPAPER)
        
        // Initial pool fill for all native IDs
        // Prioritize Home ID aggressively
        repeat(3) { loadNativeAd(BuildConfig.ADMOB_NATIVE_HOME) }
        loadNativeAd(BuildConfig.ADMOB_NATIVE_CATEGORIES)
        loadNativeAd(BuildConfig.ADMOB_NATIVE_WALLPAPERS)
        loadNativeAd(BuildConfig.ADMOB_NATIVE_PREVIEW)
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

    fun maybeShowSetWallpaperInterstitial(activity: Activity?, onContinue: () -> Unit) {
        showInterstitial(activity, setWallpaperInterstitialAd, { setWallpaperInterstitialAd = null }, BuildConfig.ADMOB_INTERSTITIAL_SET_WALLPAPER, onContinue)
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
                    BuildConfig.ADMOB_INTERSTITIAL_SET_WALLPAPER -> setWallpaperInterstitialAd = it
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
                        BuildConfig.ADMOB_INTERSTITIAL_SET_WALLPAPER -> setWallpaperInterstitialAd = it
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
                        BuildConfig.ADMOB_INTERSTITIAL_SET_WALLPAPER -> setWallpaperInterstitialAd = it
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
                pool.removeAt(0)
            } else {
                null
            }
        }

        if (ad == null) {
            onError("Spiderman Wallpaper The reward isn't ready yet. Check your connection or try again in a moment.")
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

    private fun loadInterstitial(adUnitId: String, onLoaded: (InterstitialAd) -> Unit) {
        if (!isInitialized || adUnitId.isBlank()) return
        if (loadingIds.contains(adUnitId)) return
        
        loadingIds.add(adUnitId)
        InterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    loadingIds.remove(adUnitId)
                    Log.d(TAG, "Interstitial loaded from: ${ad.responseInfo.mediationAdapterClassName}")
                    analyticsManager.logAdLoad(adUnitId, "interstitial")
                    onLoaded(ad)
                }
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    loadingIds.remove(adUnitId)
                    analyticsManager.logAdFailed(adUnitId, "interstitial", loadAdError.message)
                }
            },
        )
    }

    private fun loadRewarded(adUnitId: String) {
        if (!isInitialized || adUnitId.isBlank()) return
        
        synchronized(rewardedAdsPools) {
            val pool = rewardedAdsPools.getOrPut(adUnitId) { mutableListOf() }
            if (pool.size >= maxRewardedPoolSize) return
        }

        if (loadingIds.contains(adUnitId)) return
        loadingIds.add(adUnitId)

        RewardedAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    loadingIds.remove(adUnitId)
                    Log.d(TAG, "Rewarded loaded from: ${ad.responseInfo.mediationAdapterClassName}")
                    synchronized(rewardedAdsPools) {
                        val pool = rewardedAdsPools.getOrPut(adUnitId) { mutableListOf() }
                        if (pool.size < maxRewardedPoolSize) {
                            pool.add(ad)
                        }
                    }
                    analyticsManager.logAdLoad(adUnitId, "rewarded")
                    updateRewardedState()
                    
                    // If we still have room in the pool, load another one
                    if (isAppInForeground) {
                        val currentSize = synchronized(rewardedAdsPools) { rewardedAdsPools[adUnitId]?.size ?: 0 }
                        if (currentSize < maxRewardedPoolSize) {
                            loadRewarded(adUnitId)
                        }
                    }
                }
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    loadingIds.remove(adUnitId)
                    analyticsManager.logAdFailed(adUnitId, "rewarded", loadAdError.message)
                }
            }
        )
    }

    fun loadNativeAd(adUnitId: String) {
        if (!isInitialized) return
        val id = if (adUnitId.isBlank()) TEST_NATIVE_ID else adUnitId
        
        val pool = synchronized(nativeAdsPools) {
            nativeAdsPools.getOrPut(id) { mutableListOf() }
        }

        if (pool.size >= maxNativeAdsPerId) return
        if (globalNativeLoadingCount >= maxConcurrentLoads) return
        if (loadingIds.contains(id)) return
        
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
                Log.d(TAG, "Native ad loaded from: ${ad.responseInfo?.mediationAdapterClassName}")
                synchronized(nativeAdsPools) {
                    pool.add(ad)
                }
                globalNativeLoadingCount--
                analyticsManager.logAdLoad(id, "native")
                Log.d(TAG, "Native ad loaded for $adUnitId. Pool size: ${pool.size}")
                
                // Proactive refill: if pool is still low, load another
                if (isAppInForeground && pool.size < 2) { // Reduced from 3 to 2 to be less aggressive
                    loadNativeAd(adUnitId)
                }
            }
            .withAdListener(object : com.google.android.gms.ads.AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    loadingIds.remove(id)
                    globalNativeLoadingCount--
                    analyticsManager.logAdFailed(id, "native", error.message)
                    Log.e(TAG, "Native ad load failed for $adUnitId: ${error.message}")
                    
                    // Retry after failure if pool is empty
                    if (isAppInForeground && pool.isEmpty()) {
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            loadNativeAd(adUnitId)
                        }, 10000) // Increased retry delay to 10s
                    }
                }
                
                override fun onAdImpression() {
                    analyticsManager.logAdImpression(id, "native")
                }
                
                override fun onAdClicked() {
                    analyticsManager.logEvent("ad_click", mapOf(
                        "ad_unit_id" to id,
                        "ad_type" to "native"
                    ))
                }
            })
            .withNativeAdOptions(adOptions)
            .build()
            .loadAd(AdRequest.Builder().build())
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

        if (isAppInForeground) {
            loadNativeAd(adUnitId)
        }
        return ad
    }

    /** App Open Ad Logic */
    private fun loadAppOpenAd() {
        if (!isInitialized || isLoadingAppOpenAd || isAppOpenAdAvailable() || BuildConfig.ADMOB_APP_OPEN.isBlank()) return

        isLoadingAppOpenAd = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            BuildConfig.ADMOB_APP_OPEN,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    Log.d(TAG, "App Open loaded from: ${ad.responseInfo.mediationAdapterClassName}")
                    appOpenAd = ad
                    isLoadingAppOpenAd = false
                    appOpenLoadTime = System.currentTimeMillis()
                    analyticsManager.logAdLoad(BuildConfig.ADMOB_APP_OPEN, "app_open")
                    Log.d(TAG, "App Open Ad Loaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAppOpenAd = false
                    analyticsManager.logAdFailed(BuildConfig.ADMOB_APP_OPEN, "app_open", loadAdError.message)
                    Log.e(TAG, "App Open Ad Failed to Load: ${loadAdError.message}")
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
