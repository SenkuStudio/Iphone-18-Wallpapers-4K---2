package com.walli.iphonewallpaper

import android.app.Application
import com.walli.iphonewallpaper.ads.AdMobManager
import com.google.android.gms.ads.MobileAds
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp
import okio.Path.Companion.toPath
import java.io.File
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class DarkAestheticWallsApp : Application(), SingletonImageLoader.Factory, Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var okHttpClient: OkHttpClient

    @Inject
    lateinit var adMobManager: AdMobManager

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(adMobManager)
        
        // Initialize Mobile Ads SDK as early as possible.
        MobileAds.initialize(this) {}
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(
                if (::workerFactory.isInitialized) workerFactory
                else throw IllegalStateException("HiltWorkerFactory not initialized")
            )
            .build()

    override fun newImageLoader(context: android.content.Context): ImageLoader {
        val imageCacheDir = File(context.cacheDir, "coil_image_cache").apply { mkdirs() }
        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
            }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(imageCacheDir.absolutePath.toPath())
                    .maxSizeBytes(250L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
