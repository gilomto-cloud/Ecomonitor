package com.river2.monitor

import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

val MODELS: Map<String, Double> = mapOf("River 2" to 256.0, "River 2 Max" to 512.0, "River 2 Pro" to 768.0)

val KEYS: Map<String, List<String>> = mapOf(
    "soc" to listOf("bms_emsStatus.lcdShowSoc", "pd.soc", "bms_bmsStatus.soc"),
    "inW" to listOf("pd.wattsInSum"),
    "outW" to listOf("pd.wattsOutSum"),
    "acIn" to listOf("inv.inputWatts"),
    "acOut" to listOf("inv.outputWatts"),
    "solar" to listOf("mppt.inWatts", "pd.pvChargerWatts"),
    "car" to listOf("pd.carWatts", "mppt.carOutWatts"),
    "chgMin" to listOf("bms_emsStatus.chgRemainTime", "pd.chgRemainTime"),
    "dsgMin" to listOf("bms_emsStatus.dsgRemainTime", "pd.dsgRemainTime"),
    "temp" to listOf("bms_bmsStatus.temp", "inv.outTemp"),
    "cycles" to listOf("bms_bmsStatus.cycles"),
    "soh" to listOf("bms_bmsStatus.soh"),
    "vol" to listOf("bms_bmsStatus.vol"),
    "amp" to listOf("bms_bmsStatus.amp"),
    "acInV" to listOf("inv.acInVol"),
    "acInF" to listOf("inv.acInFreq"),
    "solarV" to listOf("mppt.inVol"),
    "solarA" to listOf("mppt.inAmp"),
    "maxSoc" to listOf("bms_emsStatus.maxChargeSoc"),
    "minSoc" to listOf("bms_emsStatus.minDsgSoc"),
)

val USB_KEYS = listOf(
    "pd.usb1Watts", "pd.usb2Watts", "pd.qcUsb1Watts", "pd.qcUsb2Watts",
    "pd.typec1Watts", "pd.typec2Watts",
)

fun toD(v: Any?): Double? = when (v) {
    is Number -> v.toDouble()
    is String -> v.toDoubleOrNull()
    else -> null
}

fun pick(d: Map<String, Any?>, name: String): Double? {
    for (k in KEYS.getValue(name)) {
        toD(d[k])?.let { return it }
    }
    return null
}

fun flattenJson(prefix: String, o: Any?, out: MutableMap<String, Any?>) {
    when (o) {
        is JSONObject -> {
            val it = o.keys()
            while (it.hasNext()) {
                val k = it.next()
                flattenJson(if (prefix.isEmpty()) k else "$prefix.$k", o.opt(k), out)
            }
        }
        is JSONArray -> for (i in 0 until o.length()) flattenJson("$prefix[$i]", o.opt(i), out)
        else -> if (prefix.isNotEmpty()) out[prefix] = o
    }
}

data class Summary(
    val soc: Double?, val inW: Double, val outW: Double,
    val acIn: Double?, val acOut: Double?, val solar: Double?, val car: Double?, val usb: Double?,
    val temp: Double?, val cycles: Double?, val soh: Double?,
    val vol: Double?, val amp: Double?, val acInV: Double?, val acInF: Double?,
    val solarV: Double?, val solarA: Double?,
    val chgMin: Double?, val dsgMin: Double?, val maxSoc: Double?, val minSoc: Double?,
    val capWh: Double,
) {
    val net: Double get() = inW - outW
    val storedWh: Double get() = (capWh * (soc ?: 0.0)) / 100.0

    /** (label, minutes). Prefers device value, otherwise computes from capacity. */
    fun eta(): Pair<String, Double?> {
        val s = soc ?: return "Idle" to null
        if (abs(net) < 3) return "Idle" to null
        if (net > 0) {
            var m: Double? = chgMin
            if ((m == null) || (m <= 0) || (m >= 5999)) m = (max(0.0, (((maxSoc ?: 100.0) - s) / 100.0) * capWh) / net) * 60.0
            return "to full" to m
        }
        var m: Double? = dsgMin
        if ((m == null) || (m <= 0) || (m >= 5999)) m = (max(0.0, (((s - (minSoc ?: 0.0)) / 100.0) * capWh)) / (-net)) * 60.0
        return "remaining" to m
    }
}

fun summarize(d: Map<String, Any?>, capWh: Double): Summary {
    val usbList = USB_KEYS.mapNotNull { toD(d[it]) }
    val usb = if (usbList.isEmpty()) null else usbList.sum()
    val acIn = pick(d, "acIn")
    val acOut = pick(d, "acOut")
    val solar = pick(d, "solar")
    val car = pick(d, "car")
    val inW = pick(d, "inW") ?: ((acIn ?: 0.0) + (solar ?: 0.0))
    val outW = pick(d, "outW") ?: ((acOut ?: 0.0) + (car ?: 0.0) + (usb ?: 0.0))
    fun volts(v: Double?) = v?.let { if (abs(it) > 1000) it / 1000 else it }
    return Summary(
        soc = pick(d, "soc"), inW = inW, outW = outW,
        acIn = acIn, acOut = acOut, solar = solar, car = car, usb = usb,
        temp = pick(d, "temp"), cycles = pick(d, "cycles"), soh = pick(d, "soh"),
        vol = volts(pick(d, "vol")),
        amp = pick(d, "amp")?.let { if (abs(it) > 100) it / 1000 else it },
        acInV = volts(pick(d, "acInV")), acInF = pick(d, "acInF"),
        solarV = pick(d, "solarV")?.let { if (it > 1000) it / 1000 else it / 10 },
        solarA = pick(d, "solarA")?.let { if (it > 100) it / 1000 else it / 10 },
        chgMin = pick(d, "chgMin"), dsgMin = pick(d, "dsgMin"),
        maxSoc = pick(d, "maxSoc"), minSoc = pick(d, "minSoc"),
        capWh = capWh,
    )
}

fun fmt(v: Double?, unit: String = "", d: Int = 0): String =
    if (v == null) "—" else String.format(Locale.US, "%.${d}f%s", v, unit)

fun fmtMin(m: Double?): String {
    if (m == null) return "—"
    val i = m.toInt()
    return "${i / 60}h ${String.format(Locale.US, "%02d", i % 60)}m"
}

fun fmtWh(x: Double): String =
    if (x >= 1000) String.format(Locale.US, "%.2f kWh", x / 1000) else String.format(Locale.US, "%.0f Wh", x)

fun fmtHms(sec: Double): String {
    val s = sec.toInt()
    return "${s / 3600}h ${String.format(Locale.US, "%02d", (s % 3600) / 60)}m"
}

data class Sample(val t: Long, val soc: Double?, val inW: Double?, val outW: Double?, val solar: Double?, val acIn: Double?)

data class Energy(
    var date: String = "",
    var todayIn: Double = 0.0, var todayOut: Double = 0.0,
    var todaySolar: Double = 0.0, var todayAc: Double = 0.0,
    var solarSecs: Double = 0.0, var acSecs: Double = 0.0, var peakSolar: Double = 0.0,
    var sessSolar: Double = 0.0, var sessAc: Double = 0.0,
    var totalSolar: Double = 0.0, var totalAc: Double = 0.0,
    var days: Map<String, List<Double>> = emptyMap(),   // date -> [solar, ac, in, out] in Wh
)
