package com.river2.monitor

import android.util.Base64
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class Cert(val uid: String, val url: String, val port: Int, val user: String, val pass: String)

/** Unofficial EcoFlow app login (same flow other community projects use). */
object EcoFlowApi {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun base(region: String) =
        if (region == "Europe") "https://api-e.ecoflow.com" else "https://api.ecoflow.com"

    private fun builder(url: String, token: String? = null): Request.Builder {
        val b = Request.Builder().url(url)
            .header("lang", "en-us").header("platform", "android")
            .header("sysversion", "11").header("version", "4.1.2.02")
            .header("phonemodel", "SM-X200").header("user-agent", "okhttp/3.14.9")
        if (token != null) b.header("Authorization", "Bearer $token")
        return b
    }

    private fun call(req: Request): JSONObject {
        val (code, text) = http.newCall(req).execute().use { r -> r.code to r.body.string() }
        val j = try { JSONObject(text) } catch (_: Exception) {
            throw IOException("HTTP $code: invalid response")
        }
        if (j.optString("code") != "0") {
            throw IOException("API ${j.optString("code")}: ${j.optString("message")}")
        }
        return j.optJSONObject("data") ?: JSONObject()
    }

    fun login(email: String, password: String, region: String): Cert {
        val root = base(region)
        val pw = Base64.encodeToString(password.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val e = email.trim()
        val bodies = listOf(
            JSONObject().put("appVersion", "4.1.2.02").put("email", e).put("os", "android")
                .put("osVersion", "30").put("password", pw).put("scene", "IOT_APP")
                .put("userType", "ECOFLOW"),
            JSONObject().put("email", e).put("password", pw).put("scene", "IOT_APP")
                .put("userType", "ECOFLOW"),
        )
        var data: JSONObject? = null
        var last: Exception? = null
        for (b in bodies) {
            try {
                val req = builder("$root/auth/login")
                    .post(b.toString().toRequestBody("application/json".toMediaType())).build()
                data = call(req)
                break
            } catch (ex: IOException) {
                last = ex
            }
        }
        val d = data ?: throw IOException("${last?.message} — check email, password and region")
        val token = d.getString("token")
        val uid = d.getJSONObject("user").getString("userId")
        val c = call(builder("$root/iot-auth/app/certification?userId=$uid", token).get().build())
        return Cert(
            uid, c.getString("url"), c.optString("port", "8883").toInt(),
            c.getString("certificateAccount"), c.getString("certificatePassword"),
        )
    }
}
