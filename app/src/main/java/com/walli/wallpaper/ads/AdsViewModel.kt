package com.walli.wallpaper.ads

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdsViewModel @Inject constructor(
    private val adMobManager: AdMobManager,
) : ViewModel() {

    private val _isRewardedReady = MutableStateFlow(false)
    val isRewardedReady = _isRewardedReady.asStateFlow()

    init {
        adMobManager.warmUp()
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

    fun maybeShowSetWallpaperInterstitial(activity: Activity?, onContinue: () -> Unit) {
        adMobManager.maybeShowSetWallpaperInterstitial(activity, onContinue)
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

    fun getNativeAd(adUnitId: String, key: String? = null) = adMobManager.getNativeAd(adUnitId, key)

    fun preloadNativeAd(adUnitId: String) {
        adMobManager.loadNativeAd(adUnitId)
    }
}
