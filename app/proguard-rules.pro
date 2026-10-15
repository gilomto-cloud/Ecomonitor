# Paho MQTT
-keep class org.eclipse.paho.client.mqttv3.MqttAsyncClient { *; }
-keep class org.eclipse.paho.client.mqttv3.MqttCallbackExtended { *; }
-keep class org.eclipse.paho.client.mqttv3.MqttConnectOptions { *; }
-keep class org.eclipse.paho.client.mqttv3.MqttMessage { *; }
-keep class org.eclipse.paho.client.mqttv3.IMqttDeliveryToken { *; }
-keep class org.eclipse.paho.client.mqttv3.persist.MemoryPersistence { *; }
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
