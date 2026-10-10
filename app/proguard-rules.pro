-keep class com.walli.iphonewallpaper.data.api.model.** { *; }
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

# Unity Ads
-keep class com.unity3d.ads.** { *; }
-keep class com.unity3d.services.** { *; }
-dontwarn com.unity3d.ads.**
-dontwarn com.unity3d.services.**
-dontwarn com.unity3d.services.ads.gmascar.**

# Prevent obfuscation of JavascriptInterface for ads
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Meta Audience Network
-keep class com.facebook.ads.** { *; }
-dontwarn com.facebook.ads.**
