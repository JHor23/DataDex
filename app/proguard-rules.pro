# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Moshi reflection DTOs & KotlinJsonAdapterFactory
-keep class kotlin.Metadata { *; }
-keepclassmembers class com.example.data.**Dto { *; }
-keep @com.squareup.moshi.Json class * { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# Retrofit service interface & return types
-keepattributes Signature,Exceptions,*Annotation*
-keep interface com.example.data.PokeApiService { *; }

# Kotlinx Serialization routes & ScanHistoryItem
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.example.**$$serializer { *; }
-keepclassmembers class com.example.** { *** Companion; }
-keepclasseswithmembers class com.example.** { kotlinx.serialization.KSerializer serializer(...); }
-keep @kotlinx.serialization.Serializable class com.example.** { *; }

# ML Kit & Firebase Component Discovery (R8 full mode strips <init>() without explicit members)
-keep class * implements com.google.firebase.components.ComponentRegistrar {
    public <init>();
    public java.util.List getComponents();
}
-keepnames class * implements com.google.firebase.components.ComponentRegistrar
-keep class com.google.mlkit.common.internal.MlKitComponentDiscoveryService
-keep class com.google.mlkit.common.internal.MlKitInitProvider { *; }
-keep class com.google.mlkit.common.sdkinternal.MlKitContext { *; }
-keep class com.google.android.gms.dynamite.descriptors.** { *; }
-keep class com.google.mlkit.vision.text.bundled.** { *; }
-keep class com.google.mlkit.vision.vkp.** { *; }
-keepclasseswithmembers class * {
    native <methods>;
}

