package com.walli.spidermanwallpaper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.MobileAds
import com.walli.spidermanwallpaper.ads.AdMobManager
import com.walli.spidermanwallpaper.ads.consent.ConsentManager
import com.walli.spidermanwallpaper.data.settings.AppTheme
import com.walli.spidermanwallpaper.data.settings.SettingsManager
import com.walli.spidermanwallpaper.ui.navigation.WalliNavGraph
import com.walli.spidermanwallpaper.ui.navigation.WalliRoute
import com.walli.spidermanwallpaper.ui.theme.WalliTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var adMobManager: AdMobManager

    private lateinit var consentManager: ConsentManager
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        consentManager = ConsentManager.getInstance(this)
        consentManager.gatherConsent(this) { error ->
            if (error != null) {
                // Consent not obtained in under certain circumstances.
                // Log the error.
            }

            if (consentManager.canRequestAds()) {
                initializeMobileAdsSdk()
            }
        }

        // This method can be called even if the user hasn't given consent,
        // but it will only initialize the SDK if consent is already available.
        if (consentManager.canRequestAds()) {
            initializeMobileAdsSdk()
        }

        setContent {
            val theme by settingsManager.theme.collectAsState(initial = AppTheme.SYSTEM)
            val dynamicColor by settingsManager.dynamicColor.collectAsState(initial = false)
            val onboardingCompleted by settingsManager.onboardingCompleted.collectAsState(initial = null)
            val scope = rememberCoroutineScope()

            WalliTheme(
                theme = theme,
                dynamicColor = dynamicColor
            ) {
                if (onboardingCompleted != null) {
                    val navController = rememberNavController()
                    val startDestination = if (onboardingCompleted == true) {
                        WalliRoute.Home.route
                    } else {
                        WalliRoute.Onboarding.route
                    }

                     WalliNavGraph(
                        navController = navController,
                        startDestination = startDestination,
                        onOnboardingComplete = {
                            scope.launch {
                                settingsManager.setOnboardingCompleted(true)
                            }
                        }
                    )
                }
            }
        }
    }

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }

        // Initialize the Google Mobile Ads SDK.
        MobileAds.initialize(this) {
            // Mobile Ads initialized, warm up ads
            adMobManager.warmUp()
        }
    }
}
