package com.river2.monitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class MonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private var session: MqttSession? = null
    private lateinit var store: Store
    private var energy = Energy()
    private var lastT = 0L
    private var lastSave = 0L
    private var lastSample = 0L
    private val history = ArrayDeque<Sample>()
    private val flags = HashSet<String>()
    private val debugLog = ArrayDeque<String>()

    companion object {
        fun start(ctx: Context) {
            ContextCompat.startForegroundService(ctx, Intent(ctx, MonitorService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        store = Store(this)
        energy = store.loadEnergy()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("monitor", "Live status", NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(
            NotificationChannel("alerts", "Alerts", NotificationManager.IMPORTANCE_HIGH))
    }

    override fun onStartCommand(intent: Intent?, fl: Int, startId: Int): Int {
        ServiceCompat.startForeground(
            this, 1, notif("Connecting…"), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        Repo.sender = { body -> send(body) }
        connect()
        return START_STICKY
    }

    override fun onDestroy() {
        store.saveEnergy(energy)
        session?.stop()
        scope.cancel()
        Repo.sender = null
        Repo.state.value = UiState()
        super.onDestroy()
    }

    // ---------------------------------------------------------------- connection
    private fun connect() {
        job?.cancel()
        session?.stop()
        session = null
        job = scope.launch {
            try {
                status("Logging in…", false)
                val cert = EcoFlowApi.login(store.email, store.password, store.region)
                log("Login OK (userId ${cert.uid}), broker ${cert.url}:${cert.port}")
                val s = MqttSession(cert, store.sn, ::onRaw, ::status, ::log)
                session = s
                val began = System.currentTimeMillis()
                s.start()
                while (isActive) {
                    delay(10_000)
                    s.requestQuotas()
                    val now = System.currentTimeMillis()
                    if (s.lastMsg == 0L && now - began > 25_000) {
                        status("Connected, but the cloud sent no data yet. Open the official " +
                            "EcoFlow app once to wake the device, then come back. " +
                            "Check Raw > Log for details.", true)
                    } else if (s.lastMsg > 0 && now - s.lastMsg > 90_000) {
                        status("⚠ No data for 90 s (device offline or asleep?)", false)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                status("⚠ ${e.message ?: e.javaClass.simpleName}", false)
            }
        }
    }

    private fun log(line: String) {
        val t = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        synchronized(debugLog) {
            debugLog.addFirst("$t  $line")
            while (debugLog.size > 80) debugLog.removeLast()
            val copy = debugLog.toList()
            Repo.state.update { it.copy(debug = copy) }
        }
    }

    private fun status(msg: String, connected: Boolean) {
        Repo.state.update { it.copy(status = msg, connected = connected) }
    }

    private fun send(body: JSONObject) {
        scope.launch {
            try {
                val s = session ?: throw IllegalStateException("Not connected to device")
                Repo.state.update { it.copy(cmdResult = "Sending command…") }
                s.send(body)
                if (body.optString("operateType") == "dcOutCfg") {
                    delay(300)
                    try {
                        val clone = JSONObject(body.toString())
                        clone.put("moduleType", 1)
                        s.send(clone)
                    } catch (_: Exception) {}
                }
                Repo.state.update { it.copy(cmdResult = "✓ Command sent — waiting for device confirmation…") }
                delay(2500)
                s.requestQuotas()
            } catch (e: Exception) {
                Repo.state.update { it.copy(cmdResult = "✗ ${e.message ?: "Failed to send command"}") }
            }
        }
    }

    // ---------------------------------------------------------------- data
    private fun onRaw(raw: Map<String, Any?>) {
        val s = summarize(raw, MODELS[store.model] ?: 256.0)
        val now = System.currentTimeMillis()
        integrate(s, now)
        var newSample = false
        if (now - lastSample >= 15_000) {
            lastSample = now
            newSample = true
            history.addLast(Sample(now, s.soc, s.inW, s.outW, s.solar, s.acIn))
            while (history.size > 5800) history.removeFirst()
        }
        checkAlerts(s)
        if (now - lastSave > 60_000) {
            store.saveEnergy(energy)
            lastSave = now
        }
        val t = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        val prev = Repo.state.value
        Repo.state.value = prev.copy(
            status = "● Connected — updated $t", connected = true, raw = raw, summary = s,
            energy = energy.copy(), history = if (newSample) history.toList() else prev.history,
        )
        getSystemService(NotificationManager::class.java).notify(
            1, notif("${fmt(s.soc, "%")}  •  in ${fmt(s.inW, " W")}  •  out ${fmt(s.outW, " W")}"))
    }

    private fun integrate(s: Summary, now: Long) {
        val e = energy
        val today = LocalDate.now().toString()
        if (e.date != today) {
            e.date = today
            e.todayIn = 0.0; e.todayOut = 0.0; e.todaySolar = 0.0; e.todayAc = 0.0
            e.solarSecs = 0.0; e.acSecs = 0.0; e.peakSolar = 0.0
        }
        val solar = s.solar ?: 0.0
        val ac = s.acIn ?: 0.0
        e.peakSolar = maxOf(e.peakSolar, solar)
        if (lastT > 0) {
            val dt = (now - lastT) / 1000.0
            if (dt > 0 && dt <= 60) {
                e.todayIn += s.inW * dt / 3600; e.todayOut += s.outW * dt / 3600
                val ws = solar * dt / 3600
                val wa = ac * dt / 3600
                e.todaySolar += ws; e.sessSolar += ws; e.totalSolar += ws
                e.todayAc += wa; e.sessAc += wa; e.totalAc += wa
                if (solar > 1) e.solarSecs += dt
                if (ac > 5) e.acSecs += dt
            }
        }
        lastT = now
        val days = HashMap(e.days)
        days[today] = listOf(e.todaySolar, e.todayAc, e.todayIn, e.todayOut)
        if (days.size > 90) days.keys.sorted().dropLast(90).forEach { days.remove(it) }
        e.days = days
    }

    // ---------------------------------------------------------------- alerts
    private fun edge(key: String, id: Int, active: Boolean, clear: Boolean, title: String, msg: String) {
        val was = key in flags
        if (active && !was) {
            flags.add(key)
            val n = NotificationCompat.Builder(this, "alerts")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title).setContentText(msg)
                .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build()
            getSystemService(NotificationManager::class.java).notify(id, n)
        } else if (clear && was) {
            flags.remove(key)
        }
    }

    private fun checkAlerts(s: Summary) {
        val soc = s.soc
        if (soc != null) {
            val dis = s.net < 0
            edge("low", 10, soc <= store.lowSoc && dis, soc > store.lowSoc + 3,
                "Battery low", "River 2 is at ${soc.toInt()}%")
            edge("crit", 11, soc <= store.critSoc && dis, soc > store.critSoc + 3,
                "Battery critical", "River 2 is at ${soc.toInt()}% — plug in power!")
            edge("full", 12, soc >= store.fullSoc && s.inW > 0, soc < store.fullSoc - 3,
                "Fully charged", "River 2 reached ${soc.toInt()}%")
        }
        s.temp?.let {
            edge("temp", 13, it >= store.highTemp, it < store.highTemp - 5,
                "High temperature", "Battery at ${it.toInt()}°C")
        }
        edge("over", 14, s.outW >= store.overloadW, s.outW < store.overloadW * 0.9,
            "High load", "Output is ${s.outW.toInt()} W")
    }

    private fun notif(text: String): Notification {
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, "monitor")
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentTitle("River 2").setContentText(text)
            .setOngoing(true).setOnlyAlertOnce(true).setContentIntent(pi).build()
    }
}
