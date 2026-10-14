package com.river2.monitor

import org.json.JSONObject

/**
 * Best-effort control payloads (moduleType / operateType / params layout).
 * Formats differ between River 2 firmware versions — if a command is rejected,
 * adjust the entries below.
 */
data class Cmd(
    val id: String, val label: String, val toggle: Boolean,
    val module: Int, val op: String,
    val lo: Int = 0, val hi: Int = 1, val step: Int = 1, val unit: String = "", val def: Int = 0,
    val params: (Int) -> Map<String, Any>,
) {
    fun body(sn: String, value: Int): JSONObject =
        JSONObject().put("sn", sn).put("moduleType", module).put("operateType", op)
            .put("params", JSONObject(params(value)))
            .put("version", "1.1")
}

val COMMANDS = listOf(
    Cmd("ac", "AC output", true, 3, "acOutCfg") {
        mapOf("enabled" to it, "out_voltage" to 255, "out_freq" to 255, "xboost" to 255, "acOut" to it)
    },
    Cmd("xboost", "X-Boost", true, 3, "acOutCfg") {
        mapOf("enabled" to 255, "out_voltage" to 255, "out_freq" to 255, "xboost" to it)
    },
    Cmd("dc", "12V DC output", true, 5, "dcOutCfg") {
        mapOf("enabled" to it, "carState" to it, "dcOut" to it)
    },
    Cmd("beep", "Beep sound", true, 1, "quietMode") {
        mapOf("enabled" to (if (it == 1) 0 else 1), "quietMode" to (if (it == 1) 0 else 1))
    },
    Cmd("max_soc", "Charge limit", false, 2, "upsConfig", 50, 100, 1, "%", 100) {
        mapOf("maxChgSoc" to it, "maxChargeSoc" to it, "maxSoc" to it)
    },
    Cmd("min_soc", "Discharge limit", false, 2, "dsgCfg", 0, 30, 1, "%", 0) {
        mapOf("minDsgSoc" to it, "minDischargeSoc" to it, "minSoc" to it)
    },
    Cmd("ac_chg", "AC charge power", false, 3, "acChgCfg", 100, 940, 10, "W", 300) {
        mapOf("chgWatts" to it, "slowChgWatts" to it, "chgPauseFlag" to 0)
    },
    Cmd("lcd", "Screen timeout", false, 1, "lcdCfg", 0, 1800, 10, "s", 30) {
        mapOf("delayOff" to it, "brightLevel" to 255, "brighLevel" to 255)
    },
    Cmd("standby", "Unit standby", false, 1, "standbyTime", 0, 1440, 10, "min", 0) {
        mapOf("standbyMins" to it, "standbyMin" to it)
    },
    Cmd("ac_standby", "AC standby", false, 3, "acStandby", 0, 1440, 10, "min", 0) {
        mapOf("standbyMins" to it, "standbyMin" to it)
    },
)
