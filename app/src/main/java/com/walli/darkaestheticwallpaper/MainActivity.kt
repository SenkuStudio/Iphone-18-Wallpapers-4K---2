package com.walli.darkaestheticwallpaper

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
import com.walli.darkaestheticwallpaper.ads.AdMobManager
import com.walli.darkaestheticwallpaper.ads.consent.ConsentManager
import com.walli.darkaestheticwallpaper.data.settings.AppTheme
import com.walli.darkaestheticwallpaper.data.settings.SettingsManager
import com.walli.darkaestheticwallpaper.ui.navigation.SpidermanNavGraph
import com.walli.darkaestheticwallpaper.ui.navigation.SpidermanRoute
import com.walli.darkaestheticwallpaper.ui.theme.HeroicTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var settingsManager: SettingsManager

    @Inject
    lateinit var adMobManager: AdMobManager

    private lateinit var consentManager: ConsentManager

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

            HeroicTheme(
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
                // Consent not obtained
            }
            
            if (consentManager.canRequestAds()) {
                adMobManager.warmUp()
            }
        }
    }
}
