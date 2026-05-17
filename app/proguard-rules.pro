# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Firebase
-keepattributes Signature
-keepattributes *Annotation*

# Firebase Auth
-keep class com.google.firebase.auth.** { *; }

# Firebase Realtime Database
-keep class com.google.firebase.database.** { *; }
-keepclassmembers class de.sudokuonline.app.data.model.** {
    *;
}

# Gson
-keepattributes Signature
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Google Mobile Ads (AdMob)
-keep class com.google.android.gms.ads.** { *; }

# Compose
-dontwarn androidx.compose.**

# Coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**