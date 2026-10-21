package com.river2.monitor

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Credentials and settings are stored encrypted on the device. */
@Suppress("DEPRECATION")
class Store(ctx: Context) {
    private val sp: SharedPreferences = EncryptedSharedPreferences.create(
        ctx, "river2_secure",
        MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    private fun s(k: String, d: String) = sp.getString(k, d) ?: d
    private fun i(k: String, d: Int) = sp.getInt(k, d)
    private fun f(k: String, d: Float) = sp.getFloat(k, d)

    var email: String
        get() = s("email", "")
        set(v) { sp.edit().putString("email", v).apply() }
    var password: String
        get() = s("password", "")
        set(v) { sp.edit().putString("password", v).apply() }
    var sn: String
        get() = s("sn", "")
        set(v) { sp.edit().putString("sn", v).apply() }
    var region: String
        get() = s("region", "Global")
        set(v) { sp.edit().putString("region", v).apply() }
    var model: String
        get() = s("model", "River 2")
        set(v) { sp.edit().putString("model", v).apply() }
    var currency: String
        get() = s("currency", "$")
        set(v) { sp.edit().putString("currency", v).apply() }
    var price: Float
        get() = f("price", 0f)
        set(v) { sp.edit().putFloat("price", v).apply() }
    var lowSoc: Int
        get() = i("low", 20)
        set(v) { sp.edit().putInt("low", v).apply() }
    var critSoc: Int
        get() = i("crit", 10)
        set(v) { sp.edit().putInt("crit", v).apply() }
    var fullSoc: Int
        get() = i("full", 100)
        set(v) { sp.edit().putInt("full", v).apply() }
    var highTemp: Int
        get() = i("temp", 45)
        set(v) { sp.edit().putInt("temp", v).apply() }
    var overloadW: Int
        get() = i("overload", 700)
        set(v) { sp.edit().putInt("overload", v).apply() }
    var showRawTab: Boolean
        get() = sp.getBoolean("showRawTab", false)
        set(v) { sp.edit().putBoolean("showRawTab", v).apply() }
    var githubRepo: String
        get() = s("githubRepo", "gilomto-cloud/EcoMonitor")
        set(v) { sp.edit().putString("githubRepo", v).apply() }
    var widgetSoc: Int
        get() = i("widgetSoc", 0)
        set(v) { sp.edit().putInt("widgetSoc", v).apply() }
    var widgetConnected: Boolean
        get() = sp.getBoolean("widgetConnected", false)
        set(v) { sp.edit().putBoolean("widgetConnected", v).apply() }
    var widgetStatus: String
        get() = s("widgetStatus", "Offline")
        set(v) { sp.edit().putString("widgetStatus", v).apply() }
    var widgetInW: Int
        get() = i("widgetInW", 0)
        set(v) { sp.edit().putInt("widgetInW", v).apply() }
    var widgetOutW: Int
        get() = i("widgetOutW", 0)
        set(v) { sp.edit().putInt("widgetOutW", v).apply() }
    var widgetSolar: Int
        get() = i("widgetSolar", 0)
        set(v) { sp.edit().putInt("widgetSolar", v).apply() }
    var widgetSavedWh: Double
        get() = sp.getFloat("widgetSavedWh", 0f).toDouble()
        set(v) { sp.edit().putFloat("widgetSavedWh", v.toFloat()).apply() }

    fun loadEnergy(): Energy {
        val today = LocalDate.now().toString()
        val e = Energy(date = today)
        val raw = s("energy", "")
        if (raw.isEmpty()) return e
        try {
            val j = JSONObject(raw)
            e.totalSolar = j.optDouble("totalSolar", 0.0)
            e.totalAc = j.optDouble("totalAc", 0.0)
            val days = HashMap<String, List<Double>>()
            val dj = j.optJSONObject("days")
            if (dj != null) {
                val it = dj.keys()
                while (it.hasNext()) {
                    val k = it.next()
                    val a = dj.getJSONArray(k)
                    days[k] = List(a.length()) { n -> a.getDouble(n) }
                }
            }
            e.days = days
            if (j.optString("date") == today) {
                e.todayIn = j.optDouble("todayIn", 0.0); e.todayOut = j.optDouble("todayOut", 0.0)
                e.todaySolar = j.optDouble("todaySolar", 0.0); e.todayAc = j.optDouble("todayAc", 0.0)
                e.solarSecs = j.optDouble("solarSecs", 0.0); e.acSecs = j.optDouble("acSecs", 0.0)
                e.peakSolar = j.optDouble("peakSolar", 0.0)
            }
        } catch (_: Exception) {}
        return e
    }

    fun saveEnergy(e: Energy) {
        val j = JSONObject()
            .put("date", e.date).put("todayIn", e.todayIn).put("todayOut", e.todayOut)
            .put("todaySolar", e.todaySolar).put("todayAc", e.todayAc)
            .put("solarSecs", e.solarSecs).put("acSecs", e.acSecs).put("peakSolar", e.peakSolar)
            .put("totalSolar", e.totalSolar).put("totalAc", e.totalAc)
        val dj = JSONObject()
        for ((k, v) in e.days) dj.put(k, JSONArray(v))
        j.put("days", dj)
        sp.edit().putString("energy", j.toString()).apply()
    }
}
