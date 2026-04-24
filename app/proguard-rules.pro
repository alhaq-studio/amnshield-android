# DeenShield ProGuard Rules - Production v1.0.0
# Add project specific ProGuard rules here.

# Enable optimization
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Preserve line numbers for debugging crashes
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep annotations
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions

# ==================== Kotlin ====================
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Kotlin Coroutines
-keep class kotlinx.coroutines.** { *; }
-keep class kotlinx.coroutines.android.** { *; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-keepclassmembers class ** {
    @kotlin.coroutines.jvm.internal.DebugMetadata kotlin.coroutines.Continuation interceptContinuation(kotlin.coroutines.Continuation);
}
-dontwarn kotlinx.coroutines.**

# ==================== Android ====================
# Keep Activity, Service, BroadcastReceiver declarations
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.view.View
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends android.app.Fragment

# Keep accessibility service
-keep class com.alhaq.deenshield.services.** { *; }
-keepclassmembers class com.alhaq.deenshield.services.** { *; }

# Keep VPN service
-keep class * extends android.net.VpnService { *; }
-keepclassmembers class * extends android.net.VpnService { *; }

# Keep device admin receiver
-keep class * extends android.app.admin.DeviceAdminReceiver { *; }

# Keep AppWidget providers
-keep class * extends android.appwidget.AppWidgetProvider { *; }

# Keep view binding classes
-keep class com.alhaq.deenshield.databinding.** { *; }

# ==================== Google Play Services ====================
# Google Sign-In
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
-keep class com.google.api.client.** { *; }
-dontwarn com.google.api.client.**

# ==================== Google Play Billing ====================
-keep class com.android.billingclient.api.** { *; }
-keepclassmembers class com.android.billingclient.api.** { *; }

# ==================== Gson ====================
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep data classes for Gson serialization
-keep class com.alhaq.deenshield.models.** { *; }
-keepclassmembers class com.alhaq.deenshield.models.** { *; }
-keep class com.alhaq.deenshield.blockers.** { *; }
-keepclassmembers class com.alhaq.deenshield.blockers.** { 
    <fields>;
    <init>(...);
}

# ==================== TensorFlow Lite ====================
-keep class org.tensorflow.lite.** { *; }
-keep class org.tensorflow.lite.support.** { *; }
-keepclassmembers class org.tensorflow.lite.** { *; }
-dontwarn org.tensorflow.lite.**

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# ==================== Material Components ====================
-keep class com.google.android.material.** { *; }
-dontwarn com.google.android.material.**
-keepclassmembers class com.google.android.material.** { *; }

# ==================== MPAndroidChart ====================
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# ==================== Parcelable ====================
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# ==================== Serializable ====================
-keepnames class * implements java.io.Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ==================== Enum ====================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ==================== Keep Application Class ====================
-keep class com.alhaq.deenshield.DeenShield { *; }

# ==================== Premium & Billing ====================
-keep class com.alhaq.deenshield.premium.** { *; }
-keepclassmembers class com.alhaq.deenshield.premium.** { *; }

# ==================== Smart Features & Network ====================
-keep class com.alhaq.deenshield.smart.** { *; }
-keep class com.alhaq.deenshield.network.** { *; }
-keepclassmembers class com.alhaq.deenshield.network.** { *; }

# ==================== Utils & Managers ====================
-keep class com.alhaq.deenshield.utils.** { *; }
-keepclassmembers class com.alhaq.deenshield.utils.** { 
    public *;
}

# ==================== Remove Logging (Production) ====================
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# ==================== Warnings to Ignore ====================
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
