package com.river2.monitor

import kotlinx.coroutines.flow.update
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttAsyncClient
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.json.JSONObject
import java.util.UUID
import javax.net.ssl.SSLSocketFactory

private val TYPE_PREFIX = mapOf(
    "pdStatus" to "pd", "bmsStatus" to "bms_bmsStatus", "emsStatus" to "bms_emsStatus",
    "invStatus" to "inv", "mpptStatus" to "mppt",
)

class MqttSession(
    private val cert: Cert,
    private val sn: String,
    private val onData: (Map<String, Any?>) -> Unit,
    private val onStatus: (String, Boolean) -> Unit,
    private val onLog: (String) -> Unit,
) {
    private var client: MqttAsyncClient? = null
    private val data = HashMap<String, Any?>()
    private var lastEmit = 0L
    @Volatile var lastMsg = 0L
    @Volatile var connected = false

    private fun topic(kind: String) = "/app/${cert.uid}/$sn/thing/property/$kind"

    fun start() {
        val cid = "ANDROID_${UUID.randomUUID().toString().uppercase()}_${cert.uid}"
        val c = MqttAsyncClient("ssl://${cert.url}:${cert.port}", cid, MemoryPersistence())
        val opts = MqttConnectOptions().apply {
            userName = cert.user
            password = cert.pass.toCharArray()
            isAutomaticReconnect = true
            isCleanSession = true
            keepAliveInterval = 60
            connectionTimeout = 20
            socketFactory = SSLSocketFactory.getDefault()
        }
        c.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                connected = true
                onLog("MQTT connected (reconnect=$reconnect) to ${cert.url}:${cert.port}")
                onStatus("Connected — waiting for data…", true)
                c.subscribe(
                    arrayOf(
                        "/app/device/property/$sn",
                        "/app/device/status/$sn",
                        topic("get_reply"),
                        topic("set_reply"),
                        "/app/pad/property/$sn",
                        "/app/${cert.uid}/$sn/thing/property/#",
                    ),
                    intArrayOf(0, 0, 0, 0, 0, 0),
                )
                requestQuotas()
            }

            override fun connectionLost(cause: Throwable?) {
                connected = false
                onLog("MQTT connection lost: ${cause?.message}")
                onStatus("Connection lost — reconnecting…", false)
            }

            override fun messageArrived(t: String?, m: MqttMessage?) {
                if (m != null) {
                    val text = String(m.payload)
                    onLog("RX $t  ${text.take(500)}")
                    handle(t, text)
                }
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {}
        })
        client = c
        c.connect(opts).waitForCompletion(25000)
    }

    private fun handle(topicName: String?, text: String) {
        val m = try { JSONObject(text) } catch (_: Exception) { return }

        if ((topicName != null) && (topicName.endsWith("/set_reply") || topicName.contains("/thing/property/"))) {
            val code = m.optString("code", "")
            val msg = m.optString("message", "")
            if ((code == "0") || (code == "200")) {
                onLog("Set reply: SUCCESS ($msg)")
                Repo.state.update { it.copy(cmdResult = "✓ Command confirmed by device") }
            } else if (code.isNotEmpty()) {
                onLog("Set reply: ERROR code $code ($msg)")
                Repo.state.update { it.copy(cmdResult = "✗ Command rejected by device: code $code ${msg.ifEmpty { "param error" }}") }
            }
        }

        val flat = HashMap<String, Any?>()
        val code = if (m.has("code")) m.opt("code")?.toString() else null
        if ((code != null) && (code != "0") && (code != "200") && !m.has("params") && !m.has("data")) {
            onStatus("Cloud reply: code $code ${m.optString("message")}", true)
            return
        }
        val d = m.optJSONObject("data")
        val qm = d?.optJSONObject("quotaMap")
        if (qm != null) {
            flattenJson("", qm, flat)
        } else {
            val p = m.optJSONObject("params") ?: d ?: return
            flattenJson("", p, flat)
            val pre = TYPE_PREFIX[m.optString("typeCode")]
            if (pre != null && flat.keys.none { it.contains('.') }) {
                val remapped = HashMap<String, Any?>()
                for ((k, v) in flat) remapped["$pre.$k"] = v
                flat.clear(); flat.putAll(remapped)
            }
        }
        if (flat.isEmpty()) return
        val snapshot: Map<String, Any?>
        synchronized(data) {
            data.putAll(flat)
            lastMsg = System.currentTimeMillis()
            if (lastMsg - lastEmit < 1000) return
            lastEmit = lastMsg
            snapshot = HashMap(data)
        }
        onData(snapshot)
    }

    private fun publish(kind: String, body: JSONObject) {
        val c = client
        if (c == null || !c.isConnected) {
            throw IllegalStateException("Not connected to EcoFlow cloud yet")
        }
        body.put("from", "Android").put("id", (100000..999999).random()).put("sn", sn)
        if (!body.has("version")) body.put("version", "1.1")
        val msg = MqttMessage(body.toString().toByteArray()).apply { qos = 1 }
        val mainTopic = topic(kind)
        onLog("TX $mainTopic  $body")
        val token = c.publish(mainTopic, msg)
        token.waitForCompletion(5000)

        // For "set" commands, also publish to /app/pad/property/SN for direct cloud routing
        if (kind == "set") {
            try {
                val padTopic = "/app/pad/property/$sn"
                onLog("TX $padTopic  $body")
                c.publish(padTopic, msg).waitForCompletion(3000)
            } catch (e: Exception) {
                onLog("Pad publish note: ${e.message}")
            }
        }
    }

    fun requestQuotas() {
        if (!connected) return
        try {
            publish("get", JSONObject().put("moduleType", 0).put("operateType", "latestQuotas")
                .put("params", JSONObject()).put("version", "1.1"))
        } catch (_: Exception) {}
    }

    fun send(body: JSONObject) {
        publish("set", body)
    }

    fun stop() {
        try { client?.disconnect(); client?.close() } catch (_: Exception) {}
    }
}
