# Paho MQTT
-keep class org.eclipse.paho.client.mqttv3.** { *; }
-keep interface org.eclipse.paho.client.mqttv3.** { *; }
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
