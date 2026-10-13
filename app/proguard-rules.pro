# General Android & App Stability
-keepattributes SourceFile, LineNumberTable
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*

# API & Data Models
-keep class com.walli.iphonewallpapers.data.api.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Hilt/Dagger
-keep class kotlin.reflect.jvm.internal.** { *; }
-keep class dagger.hilt.android.internal.** { *; }

# OkHttp & Retrofit
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn org.conscrypt.**

# Google Mobile Ads SDK
-keep public class com.google.android.gms.ads.** {
   public *;
}

# For AdMob consent (UMP SDK)
-keep class com.google.android.ump.** {
  public *;
}

# For Mediation
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
