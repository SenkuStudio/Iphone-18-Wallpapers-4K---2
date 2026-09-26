package com.walli.superheros.ui.components

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.walli.superheros.R
import com.walli.superheros.ads.AdsViewModel
import com.walli.superheros.util.AnalyticsManager

@Composable
fun NativeAdItem(
    adUnitId: String,
    modifier: Modifier = Modifier,
    key: String? = null,
    contentAspectRatio: Float = 0.68f,
    adsViewModel: AdsViewModel = hiltViewModel(),
    onAdFailed: (() -> Unit)? = null
) {
    val TAG = "NativeAdItem"
    
    // We can't easily get AnalyticsManager via hiltViewModel() if it's not a ViewModel.
    // However, AdMobManager already logs impressions/loads for pooled ads.
    // For direct loads in this component, we can add a simple analytics call.
    
    var nativeAd by remember(key) { mutableStateOf<NativeAd?>(null) }
    var isLoading by remember(key) { mutableStateOf(true) }
    var isError by remember(key) { mutableStateOf(false) }
    
    val isInitialized by adsViewModel.isInitialized.collectAsState()

    LaunchedEffect(adUnitId, key, isInitialized) {
        if (!isInitialized) return@LaunchedEffect
        if (nativeAd != null) return@LaunchedEffect
        
        isError = false
        val pooledAd = adsViewModel.getNativeAd(adUnitId, key)
        if (pooledAd != null) {
            nativeAd = pooledAd
            isLoading = false
        } else {
            // AD REQUEST GOVERNOR:
            // Wait before preloading to ensure the user hasn't quickly scrolled past this slot.
            // This preserves Match Rate by only requesting ads for visible or nearly-visible slots.
            kotlinx.coroutines.delay(800)
            if (nativeAd == null) {
                adsViewModel.preloadNativeAd(adUnitId)
            }
        }
    }

    // Listen for new ads being loaded into the pool
    LaunchedEffect(adUnitId) {
        adsViewModel.nativeAdLoadedSignal.collect { loadedId ->
            if (loadedId == adUnitId && nativeAd == null) {
                val newAd = adsViewModel.getNativeAd(adUnitId, key)
                if (newAd != null) {
                    nativeAd = newAd
                    isLoading = false
                    isError = false
                }
            }
        }
    }

    // Listen for failures to stop shimmer
    LaunchedEffect(adUnitId) {
        adsViewModel.nativeAdFailedSignal.collect { failedId ->
            if (failedId == adUnitId && nativeAd == null) {
                isLoading = false
                isError = true
                onAdFailed?.invoke()
            }
        }
    }

    AnimatedContent(
        targetState = Pair(nativeAd, isLoading),
        transitionSpec = {
            fadeIn(animationSpec = tween(600)) togetherWith fadeOut(animationSpec = tween(600))
        },
        label = "NativeAdTransition"
    ) { (ad, loading) ->
        when {
            ad != null -> {
                Surface(
                    modifier = modifier
                        .fillMaxWidth()
                        .aspectRatio(contentAspectRatio),
                    color = Color(0xFF121212), // Even darker for better contrast
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        2.dp, 
                        Color(0xFFE50914).copy(alpha = 0.6f) 
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp) // Increased safety margin to avoid corner clipping
                    ) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                val adView = LayoutInflater.from(ctx).inflate(R.layout.ad_wallpaper_grid, null) as NativeAdView
                                try {
                                    populateNativeAdView(ad, adView)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error populating native ad in factory: ${e.message}")
                                }
                                adView
                            },
                            update = { adView ->
                                try {
                                    populateNativeAdView(ad, adView)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error populating native ad in update: ${e.message}")
                                }
                            },
                            onRelease = { adView ->
                                try {
                                    adView.destroy()
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error destroying ad view: ${e.message}")
                                }
                            }
                        )
                    }
                }
            }
            loading -> {
                NativeAdShimmer(modifier = modifier)
            }
            isError -> {
                Spacer(modifier = Modifier.height(0.dp))
            }
        }
    }
}

@Composable
fun NativeAdShimmer(
    modifier: Modifier = Modifier,
    aspectRatio: Float = 0.68f
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shimmerEffect()
        )
    }
}

private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
    // If the adView is already being used for this ad, we can skip some work
    // but AdMob recommends re-setting if views might have been recycled.

    adView.headlineView = adView.findViewById(R.id.ad_headline)
    adView.bodyView = adView.findViewById(R.id.ad_body)
    adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
    adView.iconView = adView.findViewById(R.id.ad_app_icon)
    adView.priceView = adView.findViewById(R.id.ad_price)
    adView.starRatingView = adView.findViewById(R.id.ad_stars)
    adView.storeView = adView.findViewById(R.id.ad_store)
    adView.advertiserView = adView.findViewById(R.id.ad_advertiser)
    adView.mediaView = adView.findViewById<MediaView>(R.id.ad_media)

    (adView.headlineView as? TextView)?.apply {
        text = nativeAd.headline
        visibility = if (nativeAd.headline != null) View.VISIBLE else View.INVISIBLE
    }
    
    if (nativeAd.body == null) {
        adView.bodyView?.visibility = View.GONE
    } else {
        adView.bodyView?.visibility = View.VISIBLE
        (adView.bodyView as? TextView)?.text = nativeAd.body
    }

    if (nativeAd.callToAction == null) {
        adView.callToActionView?.visibility = View.INVISIBLE
    } else {
        adView.callToActionView?.visibility = View.VISIBLE
        (adView.callToActionView as? Button)?.text = nativeAd.callToAction
    }

    if (nativeAd.icon == null) {
        adView.iconView?.visibility = View.GONE
    } else {
        (adView.iconView as? ImageView)?.setImageDrawable(nativeAd.icon?.drawable)
        adView.iconView?.visibility = View.VISIBLE
    }

    val mediaContent = nativeAd.mediaContent
    if (mediaContent != null) {
        adView.mediaView?.let { mv ->
            mv.setMediaContent(mediaContent)
            mv.visibility = View.VISIBLE
        }
    } else {
        adView.mediaView?.visibility = View.GONE
    }

    if (nativeAd.starRating == null) {
        adView.starRatingView?.visibility = View.GONE
    } else {
        (adView.starRatingView as? RatingBar)?.rating = nativeAd.starRating?.toFloat() ?: 0f
        adView.starRatingView?.visibility = View.VISIBLE
    }

    if (nativeAd.advertiser == null) {
        if (nativeAd.store == null) {
            adView.advertiserView?.visibility = View.GONE
        } else {
            (adView.advertiserView as? TextView)?.text = nativeAd.store
            adView.advertiserView?.visibility = View.VISIBLE
        }
    } else {
        (adView.advertiserView as? TextView)?.text = nativeAd.advertiser
        adView.advertiserView?.visibility = View.VISIBLE
    }

    // Assign the native ad object to the native ad view.
    try {
        adView.setNativeAd(nativeAd)
    } catch (e: Exception) {
        Log.e("NativeAdItem", "Failed to setNativeAd: ${e.message}")
    }
}
