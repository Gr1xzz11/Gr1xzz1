package com.grxt.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GRXTApp(controller: GRXTController) {
    var tab by remember { mutableStateOf(Tab.HOME) }

    LaunchedEffect(controller.token) {
        if (controller.token.isNotBlank()) {
            while (true) {
                controller.refresh()
                delay(5000)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("GRXT", fontWeight = FontWeight.Bold)
                        Text(
                            "Mobile 0.5.0 · ${statusText(controller.state)}",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = {},
                        label = {
                            Text(item.title, style = MaterialTheme.typography.labelSmall)
                        }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (controller.message.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(controller.message, modifier = Modifier.weight(1f))
                        TextButton(onClick = controller::clearMessage) {
                            Text("OK")
                        }
                    }
                }
            }

            when (tab) {
                Tab.HOME -> HomeScreen(controller)
                Tab.DEVICES -> DevicesScreen(controller)
                Tab.APPS -> AppsScreen(controller)
                Tab.ACTIONS -> ActionsScreen(controller)
                Tab.REMOTE -> RemoteScreen()
                Tab.SETTINGS -> SettingsScreen(controller)
            }
        }
    }
}

@Composable
private fun HomeScreen(controller: GRXTController) {
    val scope = rememberCoroutineScope()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Центр управления",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Реальные данные от GRXT Node",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item { DeviceCard(controller) }

        if (controller.state == ConnectionState.ONLINE) {
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard("CPU", "${controller.system.cpu.toInt()}%", Modifier.weight(1f))
                    MetricCard("RAM", "${controller.system.ram.toInt()}%", Modifier.weight(1f))
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard("UPTIME", uptime(controller.system.uptimeMs), Modifier.weight(1f))
                    MetricCard("CPU", "${controller.system.processors} потоков", Modifier.weight(1f))
                }
            }
            item {
                Button(
                    onClick = { scope.launch { controller.refresh() } },
                    enabled = !controller.busy
                ) {
                    Text("Обновить")
                }
            }
        } else if (controller.token.isBlank()) {
            item {
                Text("Открой вкладку «ПК» и подключи GRXT Node.")
            }
        }
    }
}

@Composable
private fun DeviceCard(controller: GRXTController) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (controller.device.name == "—") "GRXT Node" else controller.device.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    statusText(controller.state),
                    color = if (controller.state == ConnectionState.ONLINE) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            Text(controller.server, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (controller.state == ConnectionState.ONLINE) {
                Text("${controller.device.os} ${controller.device.osVersion} · ${controller.device.arch}")
                Text(
                    "${controller.device.hostname} · Node ${controller.device.nodeVersion}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(5.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DevicesScreen(controller: GRXTController) {
    val scope = rememberCoroutineScope()
    var address by remember(controller.server) { mutableStateOf(controller.server) }
    var code by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Устройства",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        if (controller.token.isNotBlank()) {
            item { DeviceCard(controller) }
            item {
                OutlinedButton(
                    onClick = controller::disconnect,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Отключить устройство")
                }
            }
        } else {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Подключить GRXT Node",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("IP / адрес Node") },
                            placeholder = { Text("192.168.31.66:17831") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = code,
                            onValueChange = {
                                code = it.filter(Char::isDigit).take(6)
                            },
                            label = { Text("Pairing code") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { scope.launch { controller.test(address) } },
                                enabled = !controller.busy,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Проверить")
                            }
                            Button(
                                onClick = {
                                    scope.launch { controller.pair(address, code) }
                                },
                                enabled = !controller.busy && code.length == 6,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Подключить")
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    "Код показывается GRXT Node при запуске. Он одноразовый.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AppsScreen(controller: GRXTController) {
    val scope = rememberCoroutineScope()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "Программы",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        if (controller.state != ConnectionState.ONLINE) {
            item { Text("Node не подключён или недоступен.") }
        } else if (controller.apps.isEmpty()) {
            item {
                Text(
                    "Белый список приложений пуст. Добавь программы в config/apps.properties на GRXT Node.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(controller.apps, key = { it.id }) { app ->
                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(app.label, fontWeight = FontWeight.Bold)
                            Text(
                                if (app.running) "RUNNING" else "STOPPED",
                                color = if (app.running) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        Button(
                            onClick = { scope.launch { controller.appAction(app) } },
                            enabled = !controller.busy
                        ) {
                            Text(if (app.running) "Стоп" else "Старт")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionsScreen(controller: GRXTController) {
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "Управление питанием",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            ActionButton(
                "Заблокировать ПК",
                controller.state == ConnectionState.ONLINE && !controller.busy
            ) { confirm = "lock" }
        }
        item {
            ActionButton(
                "Перезагрузить ПК",
                controller.state == ConnectionState.ONLINE && !controller.busy
            ) { confirm = "reboot" }
        }
        item {
            ActionButton(
                "Выключить ПК",
                controller.state == ConnectionState.ONLINE && !controller.busy
            ) { confirm = "shutdown" }
        }
        item {
            Text(
                "Reboot и shutdown требуют allowPowerActions=true на Node.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    confirm?.let { action ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text("Подтвердить действие") },
            text = {
                Text(
                    when (action) {
                        "lock" -> "Заблокировать компьютер?"
                        "reboot" -> "Перезагрузить компьютер?"
                        else -> "Выключить компьютер?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirm = null
                        scope.launch { controller.power(action) }
                    }
                ) {
                    Text("Выполнить")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null }) {
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
private fun ActionButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text)
    }
}

@Composable
private fun RemoteScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "GRXT Remote",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Remote Desktop ещё не активен",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "GRXT Node v0.1.0 пока не передаёт экран и ввод. В v0.5 приложение не рисует фейковый Remote: здесь появится поток после добавления video/input API в Node.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(controller: GRXTController) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "Настройки",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Text(
                "Тема",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        items(ThemeMode.entries) { mode ->
            OutlinedButton(
                onClick = { controller.setTheme(mode) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (controller.theme == mode) "${mode.title} · выбрано" else mode.title)
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text("GRXT Server", fontWeight = FontWeight.Bold)
                    Text("Не настроен", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "v0.5 подключается к Node напрямую по LAN. VPS Relay — следующий серверный этап.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item {
            Text("Версия 0.5.0", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun statusText(state: ConnectionState): String = when (state) {
    ConnectionState.DISCONNECTED -> "DISCONNECTED"
    ConnectionState.CONNECTING -> "CONNECTING"
    ConnectionState.ONLINE -> "ONLINE"
    ConnectionState.OFFLINE -> "OFFLINE"
    ConnectionState.AUTH_ERROR -> "AUTH ERROR"
}

private fun uptime(ms: Long): String {
    val totalMinutes = ms / 60_000
    val days = totalMinutes / 1440
    val hours = (totalMinutes % 1440) / 60
    val minutes = totalMinutes % 60
    return if (days > 0) "${days}д ${hours}ч" else "${hours}ч ${minutes}м"
}
