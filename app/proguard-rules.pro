-keep class com.walli.spidermanwallpaper.data.api.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn org.conscrypt.**

# Google Mobile Ads SDK
-keep public class com.google.android.gms.ads.** {
   public *;
}

# For AdMob consent (UMP SDK)
-keep class com.google.android.ump.** {
  public *;
}

# For Mediation (if any, safe to include)
-keep class com.google.ads.mediation.** {
  public *;
}

# Prevent obfuscation of JavascriptInterface for ads
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
