package com.walli.wallpaper.ads

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
import com.walli.wallpaper.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdMobManager @Inject constructor(
    @ApplicationContext private val context: Context,
) : DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {
    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAppOpenAd = false
    private var appOpenLoadTime: Long = 0
    private var currentActivity: Activity? = null

    private var homeInterstitialAd: InterstitialAd? = null
    private var categoryInterstitialAd: InterstitialAd? = null
    private var setWallpaperInterstitialAd: InterstitialAd? = null

    private var homeRewardedAd: RewardedAd? = null
    private var categoryRewardedAd: RewardedAd? = null
    private var previewRewardedAd: RewardedAd? = null
    private var downloadRewardedAd: RewardedAd? = null
    private var proToolsRewardedAd: RewardedAd? = null

    // Multiple pools for unique Ad IDs
    private val nativeAdsPools = mutableMapOf<String, MutableList<NativeAd>>()
    private val assignedNativeAds = mutableMapOf<String, NativeAd>()
    private val maxNativeAdsPerId = 5
    private val maxConcurrentLoads = 4
    private var globalNativeLoadingCount = 0
    private val TAG = "AdMobManager"
    
    // Test ID for fallback if BuildConfig is empty
    private val TEST_NATIVE_ID = "ca-app-pub-3940256099942544/2247696110"
    
    private val _isRewardedLoaded = MutableStateFlow(false)
    val isRewardedLoaded = _isRewardedLoaded.asStateFlow()

    private var lastAdShowTime: Long = 0
    private var homeMeaningfulOpenCounter = 0
    private var categoryMeaningfulOpenCounter = 0
    private var nextHomeThreshold = (4..6).random()
    private var nextCategoryThreshold = (3..5).random()
    private val adCooldownMillis = (60..120).random() * 1000L

    private var isAppInForeground = false
    private var isShowingAd = false

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        isAppInForeground = true
        // Add a very small delay to ensure the Activity is fully resumed 
        // and the UI is ready before showing an App Open ad.
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (isAppInForeground) {
                showAppOpenAdIfAvailable()
            }
        }, 500)
    }

    override fun onStop(owner: LifecycleOwner) {
        isAppInForeground = false
    }

    fun warmUp() {
        loadAllAds()
        loadAppOpenAd()
    }

    private fun loadAllAds() {
        Log.d(TAG, "Loading all ads...")
        loadInterstitial(BuildConfig.ADMOB_INTERSTITIAL_HOME) { homeInterstitialAd = it }
        loadInterstitial(BuildConfig.ADMOB_INTERSTITIAL_CATEGORY) { categoryInterstitialAd = it }
        loadInterstitial(BuildConfig.ADMOB_INTERSTITIAL_SET_WALLPAPER) { setWallpaperInterstitialAd = it }

        loadRewarded(BuildConfig.ADMOB_REWARDED_HOME) { homeRewardedAd = it }
        loadRewarded(BuildConfig.ADMOB_REWARDED_CATEGORY) { categoryRewardedAd = it }
        loadRewarded(BuildConfig.ADMOB_REWARDED_PREVIEW) { previewRewardedAd = it }
        loadRewarded(BuildConfig.ADMOB_REWARDED_DOWNLOAD) { downloadRewardedAd = it }
        loadRewarded(BuildConfig.ADMOB_REWARDED_PRO_TOOLS) { proToolsRewardedAd = it }
        
        // Initial pool fill for all native IDs
        loadNativeAd(BuildConfig.ADMOB_NATIVE_HOME)
        loadNativeAd(BuildConfig.ADMOB_NATIVE_CATEGORIES)
        loadNativeAd(BuildConfig.ADMOB_NATIVE_WALLPAPERS)
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
                nextCategoryThreshold = (3..5).random()
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
                nextHomeThreshold = (4..6).random()
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
            }
        }
        ad.show(activity)
    }

    fun showHomeRewarded(activity: Activity?, onReward: () -> Unit, onDismiss: () -> Unit = {}, onError: (String) -> Unit = {}) {
        showRewarded(activity, homeRewardedAd, { homeRewardedAd = null }, BuildConfig.ADMOB_REWARDED_HOME, onReward, onDismiss, onError)
    }

    fun showCategoryRewarded(activity: Activity?, onReward: () -> Unit, onDismiss: () -> Unit = {}, onError: (String) -> Unit = {}) {
        showRewarded(activity, categoryRewardedAd, { categoryRewardedAd = null }, BuildConfig.ADMOB_REWARDED_CATEGORY, onReward, onDismiss, onError)
    }

    fun showPreviewRewarded(activity: Activity?, onReward: () -> Unit, onDismiss: () -> Unit = {}, onError: (String) -> Unit = {}) {
        showRewarded(activity, previewRewardedAd, { previewRewardedAd = null }, BuildConfig.ADMOB_REWARDED_PREVIEW, onReward, onDismiss, onError)
    }

    fun showDownloadRewarded(activity: Activity?, onReward: () -> Unit, onDismiss: () -> Unit = {}, onError: (String) -> Unit = {}) {
        showRewarded(activity, downloadRewardedAd, { downloadRewardedAd = null }, BuildConfig.ADMOB_REWARDED_DOWNLOAD, onReward, onDismiss, onError)
    }

    fun showProToolsRewarded(activity: Activity?, onReward: () -> Unit, onDismiss: () -> Unit = {}, onError: (String) -> Unit = {}) {
        showRewarded(activity, proToolsRewardedAd, { proToolsRewardedAd = null }, BuildConfig.ADMOB_REWARDED_PRO_TOOLS, onReward, onDismiss, onError)
    }

    private fun showRewarded(
        activity: Activity?,
        ad: RewardedAd?,
        clearAd: () -> Unit,
        adUnitId: String,
        onReward: () -> Unit,
        onDismiss: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (activity == null) {
            onDismiss()
            return
        }

        if (ad == null) {
            onError("Lord Krishna Wallpaper The reward isn't ready yet. Check your connection or try again in a moment.")
            if (isAppInForeground) loadRewarded(adUnitId) {
                when (adUnitId) {
                    BuildConfig.ADMOB_REWARDED_HOME -> homeRewardedAd = it
                    BuildConfig.ADMOB_REWARDED_CATEGORY -> categoryRewardedAd = it
                    BuildConfig.ADMOB_REWARDED_PREVIEW -> previewRewardedAd = it
                    BuildConfig.ADMOB_REWARDED_DOWNLOAD -> downloadRewardedAd = it
                    BuildConfig.ADMOB_REWARDED_PRO_TOOLS -> proToolsRewardedAd = it
                }
            }
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                clearAd()
                _isRewardedLoaded.value = false
                if (isAppInForeground) loadRewarded(adUnitId) {
                    when (adUnitId) {
                        BuildConfig.ADMOB_REWARDED_HOME -> homeRewardedAd = it
                        BuildConfig.ADMOB_REWARDED_CATEGORY -> categoryRewardedAd = it
                        BuildConfig.ADMOB_REWARDED_PREVIEW -> previewRewardedAd = it
                        BuildConfig.ADMOB_REWARDED_DOWNLOAD -> downloadRewardedAd = it
                        BuildConfig.ADMOB_REWARDED_PRO_TOOLS -> proToolsRewardedAd = it
                    }
                }
                onDismiss()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                clearAd()
                _isRewardedLoaded.value = false
                if (isAppInForeground) loadRewarded(adUnitId) {
                    when (adUnitId) {
                        BuildConfig.ADMOB_REWARDED_HOME -> homeRewardedAd = it
                        BuildConfig.ADMOB_REWARDED_CATEGORY -> categoryRewardedAd = it
                        BuildConfig.ADMOB_REWARDED_PREVIEW -> previewRewardedAd = it
                        BuildConfig.ADMOB_REWARDED_DOWNLOAD -> downloadRewardedAd = it
                        BuildConfig.ADMOB_REWARDED_PRO_TOOLS -> proToolsRewardedAd = it
                    }
                }
                onError("Failed to show ad. Please check your internet connection.")
                onDismiss()
            }
        }

        ad.show(activity, OnUserEarnedRewardListener {
            onReward()
        })
    }

    private fun loadInterstitial(adUnitId: String, onLoaded: (InterstitialAd) -> Unit) {
        if (adUnitId.isBlank()) return
        InterstitialAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    onLoaded(ad)
                }
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {}
            },
        )
    }

    private fun loadRewarded(adUnitId: String, onLoaded: (RewardedAd) -> Unit) {
        if (adUnitId.isBlank()) return
        RewardedAd.load(
            context,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    onLoaded(ad)
                    _isRewardedLoaded.value = true
                }
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {}
            }
        )
    }

    private fun loadNativeAd(adUnitId: String) {
        if (adUnitId.isBlank()) return
        
        val pool = synchronized(nativeAdsPools) {
            nativeAdsPools.getOrPut(adUnitId) { mutableListOf() }
        }

        if (pool.size >= maxNativeAdsPerId) return
        if (globalNativeLoadingCount >= maxConcurrentLoads) return

        val id = if (adUnitId.isBlank()) TEST_NATIVE_ID else adUnitId
        
        val adOptions = NativeAdOptions.Builder()
            .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
            .setVideoOptions(VideoOptions.Builder().setStartMuted(true).build())
            .setRequestMultipleImages(false)
            .setReturnUrlsForImageAssets(false)
            .build()

        globalNativeLoadingCount++
        
        AdLoader.Builder(context, id)
            .forNativeAd { ad ->
                synchronized(nativeAdsPools) {
                    pool.add(ad)
                }
                globalNativeLoadingCount--
                Log.d(TAG, "Native ad loaded for $adUnitId. Pool size: ${pool.size}")
            }
            .withAdListener(object : com.google.android.gms.ads.AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    globalNativeLoadingCount--
                    Log.e(TAG, "Native ad load failed for $adUnitId: ${error.message}")
                }
            })
            .withNativeAdOptions(adOptions)
            .build()
            .loadAd(AdRequest.Builder().build())
    }

    fun getNativeAd(adUnitId: String, key: String? = null): NativeAd? {
        if (key != null) {
            assignedNativeAds[key]?.let { return it }
        }

        val ad = synchronized(nativeAdsPools) {
            val pool = nativeAdsPools[adUnitId]
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
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                loadNativeAd(adUnitId)
            }
        }
        return ad
    }

    /** App Open Ad Logic */
    private fun loadAppOpenAd() {
        if (isLoadingAppOpenAd || isAppOpenAdAvailable() || BuildConfig.ADMOB_APP_OPEN.isBlank()) return

        isLoadingAppOpenAd = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            context,
            BuildConfig.ADMOB_APP_OPEN,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAppOpenAd = false
                    appOpenLoadTime = System.currentTimeMillis()
                    Log.d(TAG, "App Open Ad Loaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isLoadingAppOpenAd = false
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
                loadAppOpenAd()
            }

            override fun onAdShowedFullScreenContent() {
                isShowingAd = true
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
