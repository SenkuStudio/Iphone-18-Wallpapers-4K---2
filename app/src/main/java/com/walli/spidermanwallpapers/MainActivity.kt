package com.walli.spidermanwallpapers

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.MobileAds
import com.walli.spidermanwallpapers.ads.AdMobManager
import com.walli.spidermanwallpapers.ads.consent.ConsentManager
import com.walli.spidermanwallpapers.data.settings.AppTheme
import com.walli.spidermanwallpapers.data.settings.SettingsManager
import com.walli.spidermanwallpapers.ui.navigation.SpidermanNavGraph
import com.walli.spidermanwallpapers.ui.navigation.SpidermanRoute
import com.walli.spidermanwallpapers.ui.theme.SpidermanTheme
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
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (isFinishing) return

        setContent {
            val theme by settingsManager.theme.collectAsState(initial = AppTheme.SYSTEM)
            val dynamicColor by settingsManager.dynamicColor.collectAsState(initial = false)
            val onboardingCompleted by settingsManager.onboardingCompleted.collectAsState(initial = null)
            val scope = rememberCoroutineScope()

            SpidermanTheme(
                theme = theme,
                dynamicColor = dynamicColor
            ) {
                if (onboardingCompleted != null) {
                    val navController = rememberNavController()
                    val startDestination = if (onboardingCompleted == true) {
                        SpidermanRoute.Home.route
                    } else {
                        SpidermanRoute.Onboarding.route
                    }

                     SpidermanNavGraph(
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
