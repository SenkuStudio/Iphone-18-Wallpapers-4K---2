package com.walli.iphonewallpaper.ads.consent

import android.app.Activity
import android.content.Context
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.walli.iphonewallpaper.BuildConfig

class ConsentManager private constructor(context: Context) {
    private val consentInformation: ConsentInformation =
        UserMessagingPlatform.getConsentInformation(context)

    init {
        if (BuildConfig.DEBUG) {
            // Uncomment to reset consent state for testing
            // consentInformation.reset()
        }
    }

    /** Interface definition for a callback to be invoked when consent gathering is complete. */
    fun interface OnConsentGatheringCompleteListener {
        fun onConsentGatheringComplete(error: FormError?)
    }

    /** Helper variable to determine if the app can request ads. */
    fun canRequestAds(): Boolean {
        return consentInformation.canRequestAds()
    }

    /** Helper variable to determine if the privacy options form is required. */
    fun isPrivacyOptionsRequired(): Boolean {
        return consentInformation.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
    }

    /**
     * Helper method to call the UMP SDK methods to request consent information and load/show a
     * consent form if necessary.
     */
    fun gatherConsent(
        activity: Activity,
        onConsentGatheringCompleteListener: OnConsentGatheringCompleteListener
    ) {
        gatherConsentWithRetry(activity, 0, onConsentGatheringCompleteListener)
    }

    private fun gatherConsentWithRetry(
        activity: Activity,
        retryCount: Int,
        onConsentGatheringCompleteListener: OnConsentGatheringCompleteListener
    ) {
        val debugSettings = if (BuildConfig.DEBUG) {
            ConsentDebugSettings.Builder(activity)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                .addTestDeviceHashedId("D069094192660527394A79093A7F4978") // Replace with your test device ID from logcat
                .build()
        } else {
            null
        }

        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .apply {
                if (debugSettings != null) {
                    setConsentDebugSettings(debugSettings)
                }
            }
            .build()

        // Requesting an update to consent information.
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    // Consent gathering process is complete.
                    onConsentGatheringCompleteListener.onConsentGatheringComplete(
                        formError?.let { FormError(it.errorCode, it.message) }
                    )
                }
            },
            { requestConsentError ->
                // Retry for network errors (ConnectException-like) up to 3 times
                if (retryCount < 3) {
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        gatherConsentWithRetry(
                            activity,
                            retryCount + 1,
                            onConsentGatheringCompleteListener
                        )
                    }, 2000L * (retryCount + 1))
                } else {
                    onConsentGatheringCompleteListener.onConsentGatheringComplete(
                        FormError(requestConsentError.errorCode, requestConsentError.message)
                    )
                }
            }
        )
    }

    /** Helper method to call the UMP SDK method to show the privacy options form. */
    fun showPrivacyOptionsForm(
        activity: Activity,
        onConsentFormDismissedListener: ConsentForm.OnConsentFormDismissedListener
    ) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity, onConsentFormDismissedListener)
    }

    companion object {
        @Volatile private var instance: ConsentManager? = null

        fun getInstance(context: Context) =
            instance ?: synchronized(this) {
                instance ?: ConsentManager(context).also { instance = it }
            }
    }

    data class FormError(val errorCode: Int, val message: String)
}
