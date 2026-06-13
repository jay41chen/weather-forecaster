# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.weather.core.data.network.dto.**$$serializer { *; }
-keepclassmembers class com.weather.core.data.network.dto.** {
    *** Companion;
}
-keepclasseswithmembers class com.weather.core.data.network.dto.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit
-keepattributes Signature, Exceptions
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**

# Socket.IO / Engine.IO (uses reflection)
-keep class io.socket.** { *; }
-keep class io.engineio.** { *; }
-dontwarn io.socket.**
-dontwarn io.engineio.**
