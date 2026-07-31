-keep class com.walli.spidermanwallpaper.data.api.model.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-dontwarn org.conscrypt.**
