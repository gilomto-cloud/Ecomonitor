package com.river2.monitor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ModelsTest {
    @Test
    fun settings_validation_returns_parsed_values_for_valid_input() {
        val result = validateSettings("0.25", "20", "10", "100", "45", "700")

        assertEquals(
            SettingsValues(0.25f, 20, 10, 100, 45, 700),
            result.settings,
        )
        assertNull(result.error)
    }

    @Test
    fun settings_validation_rejects_invalid_values_without_defaults() {
        val invalidPrice = validateSettings("-1", "20", "10", "100", "45", "700")
        val invalidSocOrder = validateSettings("0", "10", "20", "100", "45", "700")
        val invalidTemperature = validateSettings("0", "20", "10", "100", "101", "700")
        val invalidOverload = validateSettings("0", "20", "10", "100", "45", "0")

        assertNull(invalidPrice.settings)
        assertNotNull(invalidPrice.error)
        assertNull(invalidSocOrder.settings)
        assertNotNull(invalidSocOrder.error)
        assertNull(invalidTemperature.settings)
        assertNotNull(invalidTemperature.error)
        assertNull(invalidOverload.settings)
        assertNotNull(invalidOverload.error)
    }

    @Test
    fun model_catalog_includes_delta_2_series_capacity_and_output_ratings() {
        assertEquals(1024.0, MODELS.getValue("DELTA 2"), 0.0001)
        assertEquals(2048.0, MODELS.getValue("DELTA 2 Max"), 0.0001)
        assertEquals(1800.0, MODEL_POWER_MAX_W.getValue("DELTA 2"), 0.0001)
        assertEquals(2400.0, MODEL_POWER_MAX_W.getValue("DELTA 2 Max"), 0.0001)
    }

    @Test
    fun model_catalog_includes_river_3_and_delta_3_series() {
        assertEquals(245.0, MODELS.getValue("River 3"), 0.0001)
        assertEquals(286.0, MODELS.getValue("River 3 Plus"), 0.0001)
        assertEquals(572.0, MODELS.getValue("River 3 Max"), 0.0001)
        assertEquals(858.0, MODELS.getValue("River 3 Max Plus"), 0.0001)
        assertEquals(300.0, MODEL_POWER_MAX_W.getValue("River 3"), 0.0001)
        assertEquals(600.0, MODEL_POWER_MAX_W.getValue("River 3 Max Plus"), 0.0001)

        assertEquals(1024.0, MODELS.getValue("DELTA 3 Classic"), 0.0001)
        assertEquals(1024.0, MODELS.getValue("DELTA 3 1000 Air"), 0.0001)
        assertEquals(2048.0, MODELS.getValue("DELTA 3 Max"), 0.0001)
        assertEquals(2048.0, MODELS.getValue("DELTA 3 2000 Air"), 0.0001)
        assertEquals(3072.0, MODELS.getValue("DELTA 3 Ultra Plus"), 0.0001)
        assertEquals(1800.0, MODEL_POWER_MAX_W.getValue("DELTA 3 Classic"), 0.0001)
        assertEquals(1800.0, MODEL_POWER_MAX_W.getValue("DELTA 3 1000 Air"), 0.0001)
        assertEquals(3000.0, MODEL_POWER_MAX_W.getValue("DELTA 3 Max Plus"), 0.0001)
        assertEquals(2400.0, MODEL_POWER_MAX_W.getValue("DELTA 3 2000 Air"), 0.0001)
        assertEquals(3600.0, MODEL_POWER_MAX_W.getValue("DELTA 3 Ultra"), 0.0001)
    }

    @Test
    fun summarize_uses_device_values_and_handles_missing_data() {
        val result = summarize(
            mapOf(
                "bms_emsStatus.lcdShowSoc" to 78,
                "pd.wattsInSum" to 350,
                "pd.wattsOutSum" to 140,
                "inv.inputWatts" to 120,
                "bms_bmsStatus.temp" to 32,
                "bms_bmsStatus.cycles" to 980,
                "bms_bmsStatus.soh" to 95,
                "bms_bmsStatus.vol" to 54.6,
                "bms_bmsStatus.amp" to 16.2,
                "inv.acInVol" to 230,
                "inv.acInFreq" to 60,
                "mppt.inVol" to 450,
                "mppt.inAmp" to 12,
                "bms_emsStatus.maxChargeSoc" to 100,
                "bms_emsStatus.minDsgSoc" to 10,
                "pd.usb1Watts" to 20,
                "pd.usb2Watts" to 25,
            ),
            256.0,
        )

        assertEquals(78.0, result.soc ?: 0.0, 0.0001)
        assertEquals(350.0, result.inW, 0.0001)
        assertEquals(140.0, result.outW, 0.0001)
        assertEquals(120.0, result.acIn ?: 0.0, 0.0001)
        assertEquals(32.0, result.temp ?: 0.0, 0.0001)
        assertEquals(980.0, result.cycles ?: 0.0, 0.0001)
        assertEquals(95.0, result.soh ?: 0.0, 0.0001)
        assertEquals(54.6, result.vol ?: 0.0, 0.0001)
        assertEquals(16.2, result.amp ?: 0.0, 0.0001)
        assertEquals(230.0, result.acInV ?: 0.0, 0.0001)
        assertEquals(60.0, result.acInF ?: 0.0, 0.0001)
        assertEquals(45.0, result.solarV ?: 0.0, 0.0001)
        assertEquals(1.2, result.solarA ?: 0.0, 0.0001)
        assertEquals(100.0, result.maxSoc ?: 0.0, 0.0001)
        assertEquals(10.0, result.minSoc ?: 0.0, 0.0001)
        assertEquals(45.0, result.usb ?: 0.0, 0.0001)
        assertEquals(210.0, result.net, 0.0001)
    }

    @Test
    fun summarize_accounts_for_missing_fields_without_crashing() {
        val result = summarize(emptyMap(), 256.0)
        assertNull(result.soc)
        assertEquals(0.0, result.inW, 0.0001)
        assertEquals(0.0, result.outW, 0.0001)
        assertNull(result.acIn)
        assertNull(result.acOut)
        assertNull(result.solar)
        assertNull(result.car)
        assertNull(result.usb)
        assertNull(result.temp)
        assertEquals(256.0, result.capWh, 0.0001)
    }

    @Test
    fun eta_uses_capacity_when_device_estimate_is_missing() {
        val summary = Summary(
            soc = 60.0,
            inW = 0.0,
            outW = 0.0,
            acIn = null,
            acOut = null,
            solar = null,
            car = null,
            usb = null,
            temp = null,
            cycles = null,
            soh = null,
            vol = null,
            amp = null,
            acInV = null,
            acInF = null,
            solarV = null,
            solarA = null,
            chgMin = null,
            dsgMin = null,
            maxSoc = 100.0,
            minSoc = 10.0,
            capWh = 256.0,
        )

        val chargingSummary = summary.copy(inW = 200.0, outW = 0.0)
        val (label, mins) = chargingSummary.eta()
        assertEquals("to full", label)
        assertNotNull(mins)

        val dischargingSummary = summary.copy(inW = 0.0, outW = 200.0)
        val (label2, mins2) = dischargingSummary.eta()
        assertEquals("remaining", label2)
        assertNotNull(mins2)
    }
}
