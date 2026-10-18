# Paho MQTT
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep class org.eclipse.paho.client.mqttv3.** { *; }
-keep class org.eclipse.paho.client.mqttv3.logging.** { *; }
-keep class org.eclipse.paho.client.mqttv3.logging.JSR47Logger { <init>(); *; }
-keep class org.eclipse.paho.client.mqttv3.logging.LoggerFactory { *; }
-keep interface org.eclipse.paho.client.mqttv3.logging.Logger { *; }
-dontwarn org.eclipse.paho.client.mqttv3.**

# OkHttp
-dontwarn okhttp3.**

# JSON
-keep class org.json.** { *; }
-dontwarn org.json.**

# Security Crypto / Tink
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.api.client.**
-dontwarn org.joda.time.**
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }

# App Data Models & Logic
-keep class com.river2.monitor.** { *; }
