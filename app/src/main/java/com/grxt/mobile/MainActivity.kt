package com.grxt.mobile

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 37) {
            requestPermissions(arrayOf("android.permission.ACCESS_LOCAL_NETWORK"), 100)
        }
        setContent {
            val controller = remember { GRXTController(applicationContext) }
            GRXTTheme(controller.theme) {
                GRXTApp(controller)
            }
        }
    }
}

data class DeviceInfo(
    val name: String = "—",
    val hostname: String = "—",
    val os: String = "—",
    val osVersion: String = "",
    val arch: String = "—",
    val nodeVersion: String = "—",
    val javaVersion: String = "—"
)

data class SystemInfo(
    val cpu: Double = 0.0,
    val ram: Double = 0.0,
    val uptimeMs: Long = 0,
    val processors: Int = 0
)

data class AppInfo(val id: String, val label: String, val running: Boolean)

enum class ConnectionState { DISCONNECTED, CONNECTING, ONLINE, OFFLINE, AUTH_ERROR }

enum class ThemeMode(val title: String) {
    SYSTEM("System"), DARK("GRXT Dark"), OLED("OLED Black"), LIGHT("GRXT Light"), CYBER("Cyber")
}

enum class Tab(val title: String) {
    HOME("Главная"), DEVICES("ПК"), APPS("Проги"), ACTIONS("Действия"), REMOTE("Remote"), SETTINGS("Настр.")
}

@Composable
fun GRXTTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val colors = when (mode) {
        ThemeMode.SYSTEM -> if (systemDark) grxtDarkScheme() else grxtLightScheme()
        ThemeMode.DARK -> grxtDarkScheme()
        ThemeMode.OLED -> darkColorScheme(
            primary = Color(0xFF6CFF8F),
            background = Color.Black,
            surface = Color.Black,
            surfaceVariant = Color(0xFF101010),
            onBackground = Color.White,
            onSurface = Color.White
        )
        ThemeMode.LIGHT -> grxtLightScheme()
        ThemeMode.CYBER -> darkColorScheme(
            primary = Color(0xFF00E5FF),
            secondary = Color(0xFFFF4FD8),
            background = Color(0xFF060912),
            surface = Color(0xFF0D1321),
            surfaceVariant = Color(0xFF141E31),
            onBackground = Color(0xFFEAFBFF),
            onSurface = Color(0xFFEAFBFF)
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}

private fun grxtDarkScheme() = darkColorScheme(
    primary = Color(0xFF63F58A),
    secondary = Color(0xFF8BCF9C),
    background = Color(0xFF0B0E0C),
    surface = Color(0xFF111512),
    surfaceVariant = Color(0xFF1A211C)
)

private fun grxtLightScheme() = lightColorScheme(
    primary = Color(0xFF126B32),
    secondary = Color(0xFF3A7650),
    background = Color(0xFFF7FAF7),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE7EFE8)
)
