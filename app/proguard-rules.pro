# Add project specific ProGuard rules here.
-keep class com.jcraft.jsch.** { *; }
-keep class com.piconnect.app.data.api.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-dontwarn com.jcraft.**
