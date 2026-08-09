package com.grxt.mobile

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class GRXTController(private val context: Context) {
    private val prefs = context.getSharedPreferences("grxt_mobile", Context.MODE_PRIVATE)

    var theme by mutableStateOf(loadTheme())
        private set
    var server by mutableStateOf(prefs.getString("server", "192.168.31.66:17831") ?: "192.168.31.66:17831")
        private set
    var token by mutableStateOf(prefs.getString("token", "") ?: "")
        private set
    var state by mutableStateOf(if (token.isBlank()) ConnectionState.DISCONNECTED else ConnectionState.CONNECTING)
        private set
    var device by mutableStateOf(DeviceInfo(name = prefs.getString("deviceName", "—") ?: "—"))
        private set
    var system by mutableStateOf(SystemInfo())
        private set
    var apps by mutableStateOf(emptyList<AppInfo>())
        private set
    var message by mutableStateOf("")
        private set
    var busy by mutableStateOf(false)
        private set

    private fun loadTheme(): ThemeMode = runCatching {
        ThemeMode.valueOf(prefs.getString("theme", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
    }.getOrDefault(ThemeMode.SYSTEM)

    fun setTheme(mode: ThemeMode) {
        theme = mode
        prefs.edit().putString("theme", mode.name).apply()
    }

    fun clearMessage() {
        message = ""
    }

    suspend fun test(address: String): Boolean {
        busy = true
        return try {
            val base = normalize(address)
            val json = request(base, "/health", "GET", null)
            val ok = json.optBoolean("ok", false)
            message = if (ok) "GRXT Node доступен" else "Node ответил некорректно"
            ok
        } catch (e: Exception) {
            message = friendly(e)
            false
        } finally {
            busy = false
        }
    }

    suspend fun pair(address: String, code: String): Boolean {
        if (!code.matches(Regex("\\d{6}"))) {
            message = "Код подключения должен содержать 6 цифр"
            return false
        }
        busy = true
        state = ConnectionState.CONNECTING
        return try {
            val base = normalize(address)
            val encodedCode = URLEncoder.encode(code, StandardCharsets.UTF_8.toString())
            val name = URLEncoder.encode("GRXT Mobile", StandardCharsets.UTF_8.toString())
            val json = request(base, "/api/v1/pair?code=$encodedCode&name=$name", "POST", null)
            token = json.getString("token")
            val deviceName = json.optString("deviceName", "GRXT PC")
            server = stripScheme(base)
            prefs.edit()
                .putString("server", server)
                .putString("token", token)
                .putString("deviceName", deviceName)
                .apply()
            device = device.copy(name = deviceName)
            message = "Устройство подключено"
            refresh()
            true
        } catch (e: HttpError) {
            state = if (e.code == 401 || e.code == 403) ConnectionState.AUTH_ERROR else ConnectionState.OFFLINE
            message = if (e.code == 403) "Код pairing неверный, истёк или уже использован" else friendly(e)
            false
        } catch (e: Exception) {
            state = ConnectionState.OFFLINE
            message = friendly(e)
            false
        } finally {
            busy = false
        }
    }

    suspend fun refresh() {
        if (token.isBlank()) {
            state = ConnectionState.DISCONNECTED
            return
        }
        state = ConnectionState.CONNECTING
        try {
            val base = normalize(server)
            val d = request(base, "/api/v1/device", "GET", token)
            val s = request(base, "/api/v1/system", "GET", token)
            val a = request(base, "/api/v1/apps", "GET", token)

            device = DeviceInfo(
                name = d.optString("deviceName", device.name),
                hostname = d.optString("hostname", "—"),
                os = d.optString("osName", "—"),
                osVersion = d.optString("osVersion", ""),
                arch = d.optString("arch", "—"),
                nodeVersion = d.optString("nodeVersion", "0.1.0"),
                javaVersion = d.optString("javaVersion", "—")
            )
            system = SystemInfo(
                cpu = s.optDouble("cpuPercent", 0.0),
                ram = s.optDouble("memoryPercent", 0.0),
                uptimeMs = s.optLong("uptimeMs", 0),
                processors = s.optInt("processors", 0)
            )
            val arr = a.optJSONArray("apps")
            val list = mutableListOf<AppInfo>()
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    list += AppInfo(
                        id = item.optString("id"),
                        label = item.optString("label", item.optString("id")),
                        running = item.optBoolean("running", false)
                    )
                }
            }
            apps = list
            state = ConnectionState.ONLINE
        } catch (e: HttpError) {
            state = if (e.code == 401 || e.code == 403) ConnectionState.AUTH_ERROR else ConnectionState.OFFLINE
            if (state == ConnectionState.AUTH_ERROR) {
                message = "Токен Node больше не действителен. Переподключи устройство."
            }
        } catch (_: Exception) {
            state = ConnectionState.OFFLINE
        }
    }

    suspend fun appAction(app: AppInfo) {
        if (token.isBlank()) return
        busy = true
        try {
            val op = if (app.running) "stop" else "start"
            val id = URLEncoder.encode(app.id, StandardCharsets.UTF_8.toString())
            request(normalize(server), "/api/v1/apps/$op?id=$id", "POST", token)
            message = if (app.running) "${app.label}: остановлено" else "${app.label}: запущено"
            refresh()
        } catch (e: Exception) {
            message = friendly(e)
        } finally {
            busy = false
        }
    }

    suspend fun power(action: String) {
        if (token.isBlank()) return
        busy = true
        try {
            request(normalize(server), "/api/v1/power/$action", "POST", token)
            message = when (action) {
                "lock" -> "Команда блокировки отправлена"
                "reboot" -> "Команда перезагрузки отправлена"
                else -> "Команда выключения отправлена"
            }
        } catch (e: HttpError) {
            message = if (e.code == 403 && action != "lock") {
                "Node запретил power action. Включи allowPowerActions=true в config/node.properties"
            } else {
                friendly(e)
            }
        } catch (e: Exception) {
            message = friendly(e)
        } finally {
            busy = false
        }
    }

    fun disconnect() {
        prefs.edit().remove("token").remove("deviceName").apply()
        token = ""
        state = ConnectionState.DISCONNECTED
        device = DeviceInfo()
        system = SystemInfo()
        apps = emptyList()
        message = "Устройство отключено от приложения"
    }

    private suspend fun request(
        base: String,
        path: String,
        method: String,
        bearer: String?
    ): JSONObject = withContext(Dispatchers.IO) {
        val connection = (URL(base + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 3500
            readTimeout = 5000
            setRequestProperty("Accept", "application/json")
            if (!bearer.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $bearer")
            }
            if (method == "POST") {
                doOutput = true
                setFixedLengthStreamingMode(0)
            }
        }
        try {
            if (method == "POST") {
                connection.outputStream.use { }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
            if (code !in 200..299) {
                val error = runCatching {
                    JSONObject(text).optString("error", text)
                }.getOrDefault(text)
                throw HttpError(code, error.ifBlank { "HTTP $code" })
            }
            JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }

    private fun normalize(raw: String): String {
        var value = raw.trim().removeSuffix("/")
        if (value.isBlank()) throw IllegalArgumentException("Укажи адрес GRXT Node")
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            value = "http://$value"
        }
        val url = URL(value)
        val host = url.host
        val port = if (url.port == -1) 17831 else url.port
        val displayHost = if (host.contains(":") && !host.startsWith("[")) "[$host]" else host
        return "${url.protocol}://$displayHost:$port"
    }

    private fun stripScheme(value: String): String =
        value.removePrefix("http://").removePrefix("https://")

    private fun friendly(e: Exception): String = when (e) {
        is HttpError -> "Node: ${e.message} (HTTP ${e.code})"
        else -> e.message?.takeIf { it.isNotBlank() } ?: "Ошибка подключения"
    }
}

class HttpError(val code: Int, override val message: String) : Exception(message)
