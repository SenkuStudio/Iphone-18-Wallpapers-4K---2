package com.walli.wallpaper.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
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
) : DefaultLifecycleObserver {
    private var homeInterstitialAd: InterstitialAd? = null
    private var categoryInterstitialAd: InterstitialAd? = null

    private var homeRewardedAd: RewardedAd? = null
    private var categoryRewardedAd: RewardedAd? = null
    private var previewRewardedAd: RewardedAd? = null
    private var downloadRewardedAd: RewardedAd? = null
    
    // Multiple pools for unique Ad IDs
    private val nativeAdsPools = mutableMapOf<String, MutableList<NativeAd>>()
    private val assignedNativeAds = mutableMapOf<String, NativeAd>()
    private val maxNativeAdsPerId = 10
    private val maxConcurrentLoads = 6
    private var globalNativeLoadingCount = 0
    private val nativeRetryDelays = mutableMapOf<String, Long>()
    private val maxRetryDelay = 30000L
    private val TAG = "AdMobManager"
    
    // Test ID for fallback if BuildConfig is empty
    private val TEST_NATIVE_ID = "ca-app-pub-3940256099942544/2247696110"
    
    private val _isRewardedLoaded = MutableStateFlow(false)
    val isRewardedLoaded = _isRewardedLoaded.asStateFlow()

    private var openCounter = 0
    private var isAppInForeground = false

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        isAppInForeground = true
    }

    override fun onStop(owner: LifecycleOwner) {
        isAppInForeground = false
    }

    fun warmUp() {
        if (isAppInForeground) {
            loadAllAds()
        }
    }

    private fun loadAllAds() {
        loadInterstitial(BuildConfig.ADMOB_INTERSTITIAL_HOME) { homeInterstitialAd = it }
        loadInterstitial(BuildConfig.ADMOB_INTERSTITIAL_CATEGORY) { categoryInterstitialAd = it }

        loadRewarded(BuildConfig.ADMOB_REWARDED_HOME) { homeRewardedAd = it }
        loadRewarded(BuildConfig.ADMOB_REWARDED_CATEGORY) { categoryRewardedAd = it }
        loadRewarded(BuildConfig.ADMOB_REWARDED_PREVIEW) { previewRewardedAd = it }
        loadRewarded(BuildConfig.ADMOB_REWARDED_DOWNLOAD) { downloadRewardedAd = it }
        
        // Initial pool fill for all native IDs
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            loadNativeAd(BuildConfig.ADMOB_NATIVE_HOME)
            loadNativeAd(BuildConfig.ADMOB_NATIVE_CATEGORIES)
            loadNativeAd(BuildConfig.ADMOB_NATIVE_WALLPAPERS)
        }
    }

    fun maybeShowCategoryInterstitial(activity: Activity?, onContinue: () -> Unit) {
        showInterstitial(activity, categoryInterstitialAd, { categoryInterstitialAd = null }, BuildConfig.ADMOB_INTERSTITIAL_CATEGORY, onContinue)
    }

    fun maybeShowHomeInterstitial(activity: Activity?, onContinue: () -> Unit) {
        openCounter += 1
        if (openCounter % 2 != 0) {
            onContinue()
            return
        }
        showInterstitial(activity, homeInterstitialAd, { homeInterstitialAd = null }, BuildConfig.ADMOB_INTERSTITIAL_HOME, onContinue)
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
                if (adUnitId == BuildConfig.ADMOB_INTERSTITIAL_HOME) homeInterstitialAd = it
                else if (adUnitId == BuildConfig.ADMOB_INTERSTITIAL_CATEGORY) categoryInterstitialAd = it
            }
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                clearAd()
                if (isAppInForeground) loadInterstitial(adUnitId) {
                    if (adUnitId == BuildConfig.ADMOB_INTERSTITIAL_HOME) homeInterstitialAd = it
                    else if (adUnitId == BuildConfig.ADMOB_INTERSTITIAL_CATEGORY) categoryInterstitialAd = it
                }
                onContinue()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                clearAd()
                if (isAppInForeground) loadInterstitial(adUnitId) {
                    if (adUnitId == BuildConfig.ADMOB_INTERSTITIAL_HOME) homeInterstitialAd = it
                    else if (adUnitId == BuildConfig.ADMOB_INTERSTITIAL_CATEGORY) categoryInterstitialAd = it
                }
                onContinue()
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
            onError("Ad is not ready yet. Please check your internet connection and try again.")
            if (isAppInForeground) loadRewarded(adUnitId) {
                when (adUnitId) {
                    BuildConfig.ADMOB_REWARDED_HOME -> homeRewardedAd = it
                    BuildConfig.ADMOB_REWARDED_CATEGORY -> categoryRewardedAd = it
                    BuildConfig.ADMOB_REWARDED_PREVIEW -> previewRewardedAd = it
                    BuildConfig.ADMOB_REWARDED_DOWNLOAD -> downloadRewardedAd = it
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
        if (!isAppInForeground || adUnitId.isBlank()) return
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
        if (!isAppInForeground || adUnitId.isBlank()) return
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
        if (!isAppInForeground || adUnitId.isBlank()) return
        
        val pool = synchronized(nativeAdsPools) {
            nativeAdsPools.getOrPut(adUnitId) { mutableListOf() }
        }

        val currentCount = pool.size
        if (currentCount >= maxNativeAdsPerId) return
        if (globalNativeLoadingCount >= maxConcurrentLoads) return

        val canLoadNow = (maxNativeAdsPerId - currentCount).coerceAtMost(maxConcurrentLoads - globalNativeLoadingCount)
        if (canLoadNow <= 0) return

        val id = if (adUnitId.isBlank()) TEST_NATIVE_ID else adUnitId
        
        val adOptions = NativeAdOptions.Builder()
            .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
            .setVideoOptions(VideoOptions.Builder().setStartMuted(true).build())
            .build()

        repeat(canLoadNow) {
            globalNativeLoadingCount++
            
            AdLoader.Builder(context, id)
                .forNativeAd { ad ->
                    synchronized(nativeAdsPools) {
                        pool.add(ad)
                    }
                    globalNativeLoadingCount--
                    nativeRetryDelays[adUnitId] = 2000L
                    Log.d(TAG, "Native ad loaded for $adUnitId. Pool size: ${pool.size}")
                    loadNativeAd(adUnitId)
                }
                .withAdListener(object : com.google.android.gms.ads.AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        globalNativeLoadingCount--
                        Log.e(TAG, "Native ad load failed for $adUnitId: ${error.message}")
                        
                        if (isAppInForeground) {
                            val delay = nativeRetryDelays[adUnitId] ?: 2000L
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                loadNativeAd(adUnitId)
                            }, delay)
                            nativeRetryDelays[adUnitId] = (delay * 2).coerceAtMost(maxRetryDelay)
                        }
                    }
                })
                .withNativeAdOptions(adOptions)
                .build()
                .loadAd(AdRequest.Builder().build())
        }
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
}
