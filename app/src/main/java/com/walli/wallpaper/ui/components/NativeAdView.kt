package com.walli.wallpaper.ui.components

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
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
import com.walli.wallpaper.R
import com.walli.wallpaper.ads.AdsViewModel

@Composable
fun NativeAdItem(
    adUnitId: String,
    modifier: Modifier = Modifier,
    key: String? = null,
    adsViewModel: AdsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val TAG = "NativeAdItem"
    
    var nativeAd by remember(key) { mutableStateOf<NativeAd?>(null) }
    var isLoading by remember(key) { mutableStateOf(true) }
    var isError by remember(key) { mutableStateOf(false) }

    LaunchedEffect(adUnitId, key) {
        if (nativeAd != null) return@LaunchedEffect
        
        isError = false
        val pooledAd = adsViewModel.getNativeAd(adUnitId, key)
        if (pooledAd != null) {
            nativeAd = pooledAd
            isLoading = false
        } else {
            val id = if (adUnitId.isBlank()) "ca-app-pub-3940256099942544/2247696110" else adUnitId
            val adLoader = AdLoader.Builder(context, id)
                .forNativeAd { ad ->
                    nativeAd = ad
                    isLoading = false
                    isError = false
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        isLoading = false
                        isError = true
                    }
                })
                .withNativeAdOptions(NativeAdOptions.Builder()
                    .setVideoOptions(VideoOptions.Builder().setStartMuted(true).build())
                    .build())
                .build()
            adLoader.loadAd(AdRequest.Builder().build())
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
                        .wrapContentHeight()
                        .defaultMinSize(minHeight = 120.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        0.5.dp, 
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                ) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        factory = { ctx ->
                            val adView = LayoutInflater.from(ctx).inflate(R.layout.ad_unified, null) as NativeAdView
                            populateNativeAdView(ad, adView)
                            adView
                        },
                        update = { adView ->
                            populateNativeAdView(ad, adView)
                        }
                    )
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
fun NativeAdShimmer(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .shimmerEffect()
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Box(
                        modifier = Modifier
                            .width(150.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .shimmerEffect()
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .shimmerEffect()
            )
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .shimmerEffect()
            )
        }
    }
}

private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
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
        visibility = View.VISIBLE
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

    nativeAd.mediaContent?.let {
        adView.mediaView?.setMediaContent(it)
        adView.mediaView?.visibility = View.VISIBLE
    } ?: run {
        adView.mediaView?.visibility = View.GONE
    }

    if (nativeAd.starRating == null) {
        adView.starRatingView?.visibility = View.GONE
    } else {
        (adView.starRatingView as? RatingBar)?.rating = nativeAd.starRating!!.toFloat()
        adView.starRatingView?.visibility = View.VISIBLE
    }

    if (nativeAd.advertiser == null) {
        adView.advertiserView?.visibility = View.GONE
    } else {
        (adView.advertiserView as? TextView)?.text = nativeAd.advertiser
        adView.advertiserView?.visibility = View.VISIBLE
    }

    adView.setNativeAd(nativeAd)
}
