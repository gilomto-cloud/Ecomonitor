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

    private fun base(region: String): String =
        if (region == "Europe") "https://api-e.ecoflow.com" else "https://api.ecoflow.com"

    private fun buildRequest(
        url: String,
        token: String? = null,
        useBearer: Boolean = true,
    ): Request.Builder {
        val request = Request.Builder().url(url)
            .header("lang", "en_US").header("platform", "android")
            .header("sysversion", "11").header("version", "4.1.2.02")
            .header("phonemodel", "SM-X200").header("user-agent", "okhttp/3.14.9")
        if (token != null) {
            val authValue = if (useBearer) "Bearer $token" else token
            request.header("Authorization", authValue)
            request.header("token", token)
        }
        return request
    }

    private fun call(req: Request): JSONObject {
        val response = http.newCall(req).execute()
        return response.use {
            val code = it.code
            val text = it.body.string()
            if (code !in 200..299) {
                throw IOException("HTTP $code: ${text.take(220).ifBlank { "empty response" }}")
            }
            val json = try {
                JSONObject(text)
            } catch (_: Exception) {
                throw IOException("HTTP $code: invalid response")
            }
            val apiCode = json.optString("code", "")
            if (apiCode != "0" && apiCode != "200" && apiCode.isNotBlank()) {
                val msg = json.optString("message").ifBlank { json.optString("msg", "request failed") }
                throw IOException("API $apiCode: $msg")
            }
            json.optJSONObject("data") ?: json
        }
    }

    fun login(email: String, password: String, region: String): Cert {
        val e = email.trim()
        if (e.isBlank()) throw IOException("Email address cannot be empty")
        if (password.isEmpty()) throw IOException("Password cannot be empty")

        val encodedPassword = Base64.encodeToString(
            password.toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP,
        )

        var lastException: IOException? = null

        val baseUri = base(region)
        try {
            val body = JSONObject()
                .put("email", e)
                .put("password", encodedPassword)
                .put("scene", "IOT_APP")
                .put("userType", "ECOFLOW")
            val req = buildRequest("$baseUri/auth/login")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val data = call(req)
            val token = data.optString("token").takeIf { it.isNotBlank() }
                ?: data.optString("accessToken").takeIf { it.isNotBlank() }
                ?: throw IOException("Login response did not contain an access token")

            val userObj = data.optJSONObject("user")
            val uid = userObj?.optString("userId")?.takeIf { it.isNotBlank() }
                ?: userObj?.optString("id")?.takeIf { it.isNotBlank() }
                ?: data.optString("userId").takeIf { it.isNotBlank() }
                ?: data.optString("user_id").takeIf { it.isNotBlank() }
                ?: throw IOException("Login response did not contain a user ID")

            for (useBearer in listOf(true, false)) {
                try {
                    val certReq = buildRequest(
                        "$baseUri/iot-auth/app/certification?userId=$uid",
                        token,
                        useBearer,
                    ).get().build()
                    val c = call(certReq)
                    val certUrl = c.optString("url").takeIf { it.isNotBlank() }
                        ?: c.optString("server").takeIf { it.isNotBlank() }
                        ?: c.optString("mqttUrl").takeIf { it.isNotBlank() }
                    val certAccount = c.optString("certificateAccount").takeIf { it.isNotBlank() }
                        ?: c.optString("account").takeIf { it.isNotBlank() }
                        ?: c.optString("user").takeIf { it.isNotBlank() }
                    val certPass = c.optString("certificatePassword").takeIf { it.isNotBlank() }
                        ?: c.optString("password").takeIf { it.isNotBlank() }
                        ?: c.optString("pass").takeIf { it.isNotBlank() }

                    if (certUrl != null && certAccount != null && certPass != null) {
                        val port = c.optString("port").toIntOrNull() ?: 8883
                        return Cert(uid, certUrl, port, certAccount, certPass)
                    }
                    lastException = IOException("Certification response did not contain MQTT credentials")
                } catch (ex: IOException) {
                    lastException = ex
                }
            }
        } catch (ex: IOException) {
            lastException = ex
        }

        throw lastException ?: IOException("Login failed")
    }
}
