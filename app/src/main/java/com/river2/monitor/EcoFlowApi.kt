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
    private const val MAX_LOGIN_ATTEMPTS = 3
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
        val response = http.newCall(req).execute()
        val code = response.code
        val text = response.body.string()
        response.close()
        if (code !in 200..299) {
            throw IOException("HTTP $code: ${text.take(220).ifBlank { "empty response" }}")
        }
        val j = try { JSONObject(text) } catch (_: Exception) {
            throw IOException("HTTP $code: invalid response")
        }
        if (j.optString("code") != "0") {
            throw IOException("API ${j.optString("code")}: ${j.optString("message").ifBlank { "request failed" }}")
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

        var last: IOException? = null
        for (attempt in 1..MAX_LOGIN_ATTEMPTS) {
            for (b in bodies) {
                try {
                    val req = builder("$root/auth/login")
                        .post(b.toString().toRequestBody("application/json".toMediaType())).build()
                    val data = call(req)
                    val token = data.optString("token").takeIf { it.isNotBlank() }
                        ?: throw IOException("Login succeeded but token is missing")
                    val uid = data.optJSONObject("user")?.optString("userId")
                        ?: throw IOException("Login succeeded but userId is missing")
                    val c = call(builder("$root/iot-auth/app/certification?userId=$uid", token).get().build())
                    return Cert(
                        uid,
                        c.optString("url").takeIf { it.isNotBlank() } ?: throw IOException("Device certificate URL missing"),
                        c.optString("port", "8883").toIntOrNull() ?: 8883,
                        c.optString("certificateAccount").takeIf { it.isNotBlank() } ?: throw IOException("Certificate account missing"),
                        c.optString("certificatePassword").takeIf { it.isNotBlank() } ?: throw IOException("Certificate password missing"),
                    )
                } catch (ex: IOException) {
                    last = ex
                }
            }
            if (attempt < MAX_LOGIN_ATTEMPTS) {
                Thread.sleep((attempt * 500L))
            }
        }

        throw IOException("${last?.message ?: "Login failed"} — check email, password and region")
    }
}
