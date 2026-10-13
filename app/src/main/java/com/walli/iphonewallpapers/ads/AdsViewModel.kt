package com.walli.iphonewallpapers.ads

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.walli.iphonewallpapers.util.AnalyticsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdsViewModel @Inject constructor(
    private val adMobManager: AdMobManager,
    private val analyticsManager: AnalyticsManager
) : ViewModel() {

    private val _isRewardedReady = MutableStateFlow(false)
    val isRewardedReady = _isRewardedReady.asStateFlow()

    val isInitialized = adMobManager.isInitialized

    init {
        // Removed adMobManager.warmUp() from here. 
        // It is called in MainActivity after proper SDK initialization and consent.
        observeAdStatus()
    }

    private fun observeAdStatus() {
        viewModelScope.launch {
            adMobManager.isRewardedLoaded.collect { isLoaded ->
                _isRewardedReady.value = isLoaded
            }
        }
    }

    fun maybeShowHomeInterstitial(activity: Activity?, onContinue: () -> Unit) {
        adMobManager.maybeShowHomeInterstitial(activity, onContinue)
    }

    fun maybeShowCategoryInterstitial(activity: Activity?, onContinue: () -> Unit) {
        adMobManager.maybeShowCategoryInterstitial(activity, onContinue)
    }

    fun showHomeRewarded(
        activity: Activity?,
        onReward: () -> Unit,
        onDismiss: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        adMobManager.showHomeRewarded(activity, onReward, onDismiss, onError)
    }

    fun showCategoryRewarded(
        activity: Activity?,
        onReward: () -> Unit,
        onDismiss: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        adMobManager.showCategoryRewarded(activity, onReward, onDismiss, onError)
    }

    fun showPreviewRewarded(
        activity: Activity?,
        onReward: () -> Unit,
        onDismiss: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        adMobManager.showPreviewRewarded(activity, onReward, onDismiss, onError)
    }

    fun showDownloadRewarded(
        activity: Activity?,
        onReward: () -> Unit,
        onDismiss: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        adMobManager.showDownloadRewarded(activity, onReward, onDismiss, onError)
    }

    fun showProToolsRewarded(
        activity: Activity?,
        onReward: () -> Unit,
        onDismiss: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        adMobManager.showProToolsRewarded(activity, onReward, onDismiss, onError)
    }

    fun showSetWallpaperRewarded(
        activity: Activity?,
        onReward: () -> Unit,
        onDismiss: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        adMobManager.showSetWallpaperRewarded(activity, onReward, onDismiss, onError)
    }

    fun getNativeAd(adUnitId: String, key: String? = null) = adMobManager.getNativeAd(adUnitId, key)

    // Add a state to trigger UI recomposition when ads are loaded or failed
    val nativeAdLoadedSignal = adMobManager.nativeAdLoadedSignal
    val nativeAdFailedSignal = adMobManager.nativeAdFailedSignal

    fun preloadNativeAd(adUnitId: String) {
        adMobManager.loadNativeAd(adUnitId)
    }

    fun preloadRewardedAd(adUnitId: String) {
        adMobManager.loadRewarded(adUnitId)
    }

    fun createAdRequest(collapsible: String? = null) = adMobManager.createAdRequest(collapsible)

    fun logNativeAdImpression(adUnitId: String) {
        analyticsManager.logAdImpression(adUnitId, "native")
    }

    fun logNativeAdLoad(adUnitId: String) {
        analyticsManager.logAdLoad(adUnitId, "native")
    }

    fun logNativeAdFailed(adUnitId: String, error: String) {
        analyticsManager.logAdFailed(adUnitId, "native", error)
    }
}
