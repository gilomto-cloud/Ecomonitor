package com.river2.monitor

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.update
import java.util.Locale
import kotlin.math.max

private val GREEN = Color(0xFF2ECC71)
private val YELLOW = Color(0xFFF1C40F)
private val RED = Color(0xFFE74C3C)
private val BLUE = Color(0xFF3DA5FF)
private val ORANGE = Color(0xFFFF9F43)
private val PURPLE = Color(0xFFB07CFF)

@Composable
fun App(store: Store) {
    var configured by remember {
        mutableStateOf(store.email.isNotEmpty() && store.password.isNotEmpty() && store.sn.isNotEmpty())
    }
    if (!configured) LoginScreen(store) { configured = true }
    else MainScreen(store) { configured = false }
}

// ===================================================================== login
@Composable
fun LoginScreen(store: Store, onDone: () -> Unit) {
    val ctx = LocalContext.current
    var email by remember { mutableStateOf(store.email) }
    var pw by remember { mutableStateOf(store.password) }
    var sn by remember { mutableStateOf(store.sn) }
    var region by remember { mutableStateOf(store.region) }
    var model by remember { mutableStateOf(store.model) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Spacer(Modifier.size(24.dp))
        Text("EcoFlow River 2", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(
            "Sign in with your EcoFlow app account. Credentials are stored encrypted on this phone " +
                "and only sent to EcoFlow.",
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(email, { email = it }, label = { Text("EcoFlow email") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
        OutlinedTextField(pw, { pw = it }, label = { Text("EcoFlow password") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation())
        OutlinedTextField(sn, { sn = it.trim() }, label = { Text("Device serial number (SN)") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        Text("Region (must match your EcoFlow account)")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Global", "Europe").forEach { r ->
                FilterChip(selected = region == r, onClick = { region = r }, label = { Text(r) })
            }
        }
        Text("Model")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MODELS.keys.forEach { m ->
                FilterChip(selected = model == m, onClick = { model = m }, label = { Text(m) })
            }
        }
        Button(
            onClick = {
                store.email = email.trim(); store.password = pw; store.sn = sn
                store.region = region; store.model = model
                MonitorService.start(ctx)
                onDone()
            },
            enabled = email.isNotBlank() && pw.isNotEmpty() && sn.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Connect") }
    }
}

// ===================================================================== main
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(store: Store, onLogout: () -> Unit) {
    val ui by Repo.state.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val tabs = listOf("Home" to "🔋", "Solar & AC" to "☀", "History" to "📈", "Raw" to "📋")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("River 2", fontWeight = FontWeight.Bold)
                        Text(ui.status, fontSize = 11.sp, maxLines = 2)
                    }
                },
                actions = {
                    TextButton({ MonitorService.start(ctx) }) { Text("⟳") }
                    TextButton({ showSettings = true }) { Text("⚙") }
                    TextButton({
                        ctx.stopService(Intent(ctx, MonitorService::class.java))
                        store.password = ""
                        onLogout()
                    }) { Text("⏻") }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, (name, icon) ->
                    NavigationBarItem(
                        selected = tab == i, onClick = { tab = i },
                        icon = { Text(icon) }, label = { Text(name, fontSize = 10.sp, maxLines = 1) },
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                0 -> Dashboard(ui)
                1 -> SolarAc(ui, store)
                2 -> HistoryScreen(ui)
                else -> RawScreen(ui)
            }
        }
    }
    if (showSettings) SettingsDialog(store) { showSettings = false }
}

@Composable
fun Tile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TileGrid(items: List<Pair<String, String>>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { r ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                r.forEach { (l, v) -> Tile(l, v, Modifier.weight(1f)) }
                if (r.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

// ===================================================================== dashboard
@Composable
fun Gauge(s: Summary?) {
    val soc = s?.soc
    val track = MaterialTheme.colorScheme.surfaceVariant
    val col = when {
        soc == null -> Color.Gray
        soc <= 15 -> RED
        soc <= 30 -> YELLOW
        else -> GREEN
    }
    val (label, mins) = s?.eta() ?: ("" to null)
    Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(16.dp)) {
            val st = Stroke(width = 28f, cap = StrokeCap.Round)
            drawArc(track, 135f, 270f, false, style = st)
            if (soc != null) drawArc(col, 135f, 270f * (soc / 100).toFloat(), false, style = st)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(fmt(soc, "%"), fontSize = 44.sp, fontWeight = FontWeight.Bold)
            Text(if (mins == null) label else "${fmtMin(mins)} $label",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun Dashboard(ui: UiState) {
    val s = ui.summary
    val e = ui.energy
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Gauge(s)
        TileGrid(
            listOf(
                "Total input" to fmt(s?.inW, " W"), "Total output" to fmt(s?.outW, " W"),
                "Net power" to (if (s == null) "—" else String.format(Locale.US, "%+.0f W", s.net)),
                "Stored" to (if (s == null) "—" else "${fmt(s.storedWh)} / ${fmt(s.capWh)} Wh"),
                "☀ Solar in" to fmt(s?.solar, " W"), "🔌 AC in" to fmt(s?.acIn, " W"),
                "AC out" to fmt(s?.acOut, " W"), "12V DC out" to fmt(s?.car, " W"),
                "USB out" to fmt(s?.usb, " W"), "Battery temp" to fmt(s?.temp, " °C"),
                "Cycles / health" to "${fmt(s?.cycles)} / ${fmt(s?.soh, "%")}",
                "Pack V / A" to "${fmt(s?.vol, " V", 1)} / ${fmt(s?.amp, " A", 1)}",
                "☀ Solar today" to fmtWh(e.todaySolar), "🔌 AC used today" to fmtWh(e.todayAc),
            ),
        )
    }
}

// ===================================================================== solar & ac
@Composable
fun RowKV(k: String, v: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(k)
        Text(v, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SolarAc(ui: UiState, store: Store) {
    val s = ui.summary
    val e = ui.energy
    val price = store.price.toDouble()
    val cur = store.currency
    fun money(wh: Double) = if (price > 0) String.format(Locale.US, "%s%.2f", cur, wh / 1000 * price) else "set price in ⚙"
    val tot = e.todaySolar + e.todayAc
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("☀  Solar panel", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                RowKV("Producing now", fmt(s?.solar, " W"))
                RowKV("Panel voltage", fmt(s?.solarV, " V", 1))
                RowKV("Panel current", fmt(s?.solarA, " A", 2))
                RowKV("Peak today", fmt(e.peakSolar, " W"))
                RowKV("Produced today", fmtWh(e.todaySolar))
                RowKV("Produced this session", fmtWh(e.sessSolar))
                RowKV("Produced (total in app)", fmtWh(e.totalSolar))
                RowKV("Sun hours today", fmtHms(e.solarSecs))
                RowKV("Value today", money(e.todaySolar))
                RowKV("Solar share of input today", if (tot > 0) fmt(100 * e.todaySolar / tot, "%") else "—")
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("🔌  AC charging (wall)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                RowKV("Drawing now", fmt(s?.acIn, " W"))
                RowKV("AC voltage", fmt(s?.acInV, " V"))
                RowKV("AC frequency", fmt(s?.acInF, " Hz"))
                RowKV("Charging from AC?", if ((s?.acIn ?: 0.0) > 5) "Yes" else "No")
                RowKV("Used today", fmtWh(e.todayAc))
                RowKV("Used this session", fmtWh(e.sessAc))
                RowKV("Used (total in app)", fmtWh(e.totalAc))
                RowKV("AC charging time today", fmtHms(e.acSecs))
                RowKV("Cost today", money(e.todayAc))
                RowKV("Cost total", money(e.totalAc))
            }
        }
        Text(
            "Energy is measured by the app (power × time) while it runs. Time when the app " +
                "service is stopped is not counted.",
            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("Daily totals", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                RowKV("Date  (solar / AC / in / out)", "")
                e.days.keys.sortedDescending().take(30).forEach { d ->
                    val r = e.days.getValue(d)
                    RowKV(d, "${fmtWh(r[0])} / ${fmtWh(r[1])} / ${fmtWh(r[2])} / ${fmtWh(r[3])}")
                }
            }
        }
    }
}

// ===================================================================== history
@Composable
fun HistoryScreen(ui: UiState) {
    var hours by remember { mutableIntStateOf(1) }
    val axis = MaterialTheme.colorScheme.outlineVariant
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1, 6, 24).forEach { h ->
                FilterChip(selected = hours == h, onClick = { hours = h }, label = { Text("${h}h") })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 6.dp)) {
            Text("● Battery %", color = GREEN, fontSize = 12.sp)
            Text("● In W", color = BLUE, fontSize = 12.sp)
            Text("● Out W", color = ORANGE, fontSize = 12.sp)
            Text("● Solar W", color = RED, fontSize = 12.sp)
            Text("● AC W", color = PURPLE, fontSize = 12.sp)
        }
        val now = System.currentTimeMillis()
        val t0 = now - hours * 3_600_000L
        val pts = ui.history.filter { it.t >= t0 }
        Canvas(Modifier.fillMaxWidth().weight(1f).padding(top = 12.dp)) {
            for (i in 0..4) {
                val y = size.height * i / 4
                drawLine(axis, Offset(0f, y), Offset(size.width, y), 1f)
            }
            if (pts.size < 2) return@Canvas
            val wMax = max(100.0, pts.maxOf {
                maxOf(it.inW ?: 0.0, it.outW ?: 0.0, it.solar ?: 0.0, it.acIn ?: 0.0)
            })
            fun line(color: Color, top: Double, get: (Sample) -> Double?) {
                val path = Path()
                var started = false
                for (p in pts) {
                    val v = get(p) ?: continue
                    val x = ((p.t - t0).toFloat() / (now - t0)) * size.width
                    val y = size.height * (1f - (v.coerceIn(0.0, top) / top).toFloat())
                    if (!started) { path.moveTo(x, y); started = true } else path.lineTo(x, y)
                }
                drawPath(path, color, style = Stroke(width = 4f))
            }
            line(GREEN, 100.0) { it.soc }
            line(BLUE, wMax) { it.inW }
            line(ORANGE, wMax) { it.outW }
            line(RED, wMax) { it.solar }
            line(PURPLE, wMax) { it.acIn }
        }
        Text(
            "Left scale 0–100% for battery, watts scaled to the highest value in view.",
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ===================================================================== controls
@Composable
fun Controls(ui: UiState, store: Store) {
    var pending by remember { mutableStateOf<Pair<Cmd, Int>?>(null) }
    val maxAcWatts = when (store.model) {
        "River 2 Max" -> 660
        "River 2 Pro" -> 940
        else -> 360
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "⚠ Control commands are sent via EcoFlow cloud MQTT. Check the unit or Raw > Log tab to see responses.",
            color = YELLOW, fontSize = 13.sp,
        )
        if (!ui.connected) {
            Text("⚠ Not connected to EcoFlow cloud. Controls require an active connection.", color = RED, fontSize = 13.sp)
        }
        COMMANDS.forEach { c ->
            key(c.id) {
                val hi = if (c.id == "ac_chg") maxAcWatts else c.hi
                if (c.toggle) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(c.label, Modifier.weight(1f), fontSize = 16.sp)
                        Button({ pending = c to 1 }, enabled = ui.connected) { Text("ON") }
                        Spacer(Modifier.width(8.dp))
                        Button({ pending = c to 0 }, enabled = ui.connected) { Text("OFF") }
                    }
                } else {
                    var v by remember { mutableFloatStateOf(c.def.coerceIn(c.lo, hi).toFloat()) }
                    Column {
                        Text("${c.label}: ${v.toInt()} ${c.unit}", fontSize = 16.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Slider(
                                v, { v = it }, Modifier.weight(1f),
                                valueRange = c.lo.toFloat()..hi.toFloat(),
                                steps = max(0, (hi - c.lo) / c.step - 1),
                                enabled = ui.connected,
                            )
                            Spacer(Modifier.width(8.dp))
                            Button({ pending = c to v.toInt() }, enabled = ui.connected) { Text("Apply") }
                        }
                    }
                }
            }
        }
        if (ui.cmdResult.isNotEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Text(ui.cmdResult, Modifier.padding(12.dp), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    pending?.let { (c, v) ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Confirm") },
            text = { Text("Send '${c.label}' = $v to the River 2?") },
            confirmButton = {
                TextButton({
                    val sender = Repo.sender
                    if (sender == null) {
                        Repo.state.update { it.copy(cmdResult = "✗ Service not connected. Ensure app is signed in.") }
                    } else {
                        sender(c.body(store.sn, v))
                    }
                    pending = null
                }) { Text("Send") }
            },
            dismissButton = { TextButton({ pending = null }) { Text("Cancel") } },
        )
    }
}

// ===================================================================== raw
@Composable
fun RawScreen(ui: UiState) {
    var q by remember { mutableStateOf("") }
    var showLog by remember { mutableStateOf(ui.raw.isEmpty()) }
    val clip = LocalClipboardManager.current
    if (showLog) {
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = false, onClick = { showLog = false }, label = { Text("Values") })
                FilterChip(selected = true, onClick = {}, label = { Text("Log") })
                TextButton({ clip.setText(AnnotatedString(ui.debug.joinToString("\n"))) }) { Text("Copy log") }
            }
            Text("Connection log (newest first). Passwords are never logged.",
                fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyColumn(Modifier.padding(top = 6.dp)) {
                items(ui.debug) { line -> Text(line, fontSize = 10.sp, modifier = Modifier.padding(vertical = 2.dp)) }
            }
        }
        return
    }
    val rows = ui.raw.entries.sortedBy { it.key }.filter {
        q.isBlank() || it.key.contains(q, true) || it.value.toString().contains(q, true)
    }
    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = true, onClick = {}, label = { Text("Values") })
            FilterChip(selected = false, onClick = { showLog = true }, label = { Text("Log") })
        }
        OutlinedTextField(q, { q = it }, label = { Text("Filter") }, singleLine = true,
            modifier = Modifier.fillMaxWidth())
        LazyColumn(Modifier.padding(top = 8.dp)) {
            items(rows) { r ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(r.key, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text(r.value.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ===================================================================== settings
@Composable
fun SettingsDialog(store: Store, onClose: () -> Unit) {
    var model by remember { mutableStateOf(store.model) }
    var price by remember { mutableStateOf(store.price.toString()) }
    var cur by remember { mutableStateOf(store.currency) }
    var low by remember { mutableStateOf(store.lowSoc.toString()) }
    var crit by remember { mutableStateOf(store.critSoc.toString()) }
    var full by remember { mutableStateOf(store.fullSoc.toString()) }
    var temp by remember { mutableStateOf(store.highTemp.toString()) }
    var over by remember { mutableStateOf(store.overloadW.toString()) }
    val num = KeyboardOptions(keyboardType = KeyboardType.Number)

    @Composable
    fun field(label: String, v: String, set: (String) -> Unit, kb: KeyboardOptions = num) =
        OutlinedTextField(v, set, label = { Text(label) }, singleLine = true,
            keyboardOptions = kb, modifier = Modifier.fillMaxWidth())

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Settings") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MODELS.keys.forEach { m ->
                        FilterChip(selected = model == m, onClick = { model = m }, label = { Text(m, fontSize = 11.sp) })
                    }
                }
                field("Electricity price per kWh", price, { price = it }, KeyboardOptions(keyboardType = KeyboardType.Decimal))
                field("Currency symbol", cur, { cur = it }, KeyboardOptions.Default)
                field("Low battery alert %", low, { low = it })
                field("Critical alert %", crit, { crit = it })
                field("Fully charged at %", full, { full = it })
                field("High temperature °C", temp, { temp = it })
                field("Overload alert (W)", over, { over = it })
            }
        },
        confirmButton = {
            TextButton({
                store.model = model
                store.price = price.toFloatOrNull() ?: 0f
                store.currency = cur
                store.lowSoc = low.toIntOrNull() ?: 20
                store.critSoc = crit.toIntOrNull() ?: 10
                store.fullSoc = full.toIntOrNull() ?: 100
                store.highTemp = temp.toIntOrNull() ?: 45
                store.overloadW = over.toIntOrNull() ?: 700
                onClose()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClose) { Text("Cancel") } },
    )
}
