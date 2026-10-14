package com.river2.monitor

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import java.util.Locale
import kotlin.math.max

// Tech Giant Premium Color Palette
private val EMERALD = Color(0xFF10B981)
private val CYAN = Color(0xFF38BDF8)
private val AMBER = Color(0xFFF59E0B)
private val ORANGE = Color(0xFFF97316)
private val INDIGO = Color(0xFF818CF8)
private val CRIMSON = Color(0xFFEF4444)

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

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(32.dp))
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text("⚡", fontSize = 28.sp)
            }
            Text("EcoFlow River 2", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text(
                "Sign in with your EcoFlow app account to connect directly to your River 2 power station.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        email, { email = it }, label = { Text("EcoFlow email") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    )
                    OutlinedTextField(
                        pw, { pw = it }, label = { Text("EcoFlow password") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        visualTransformation = PasswordVisualTransformation()
                    )
                    OutlinedTextField(
                        sn, { sn = it.trim() }, label = { Text("Device Serial Number (SN)") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text("Region", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Global", "Europe").forEach { r ->
                            FilterChip(
                                selected = region == r, onClick = { region = r }, label = { Text(r) },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            )
                        }
                    }

                    Text("Device Model", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MODELS.keys.forEach { m ->
                            FilterChip(
                                selected = model == m, onClick = { model = m }, label = { Text(m, fontSize = 12.sp) },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            )
                        }
                    }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EMERALD, contentColor = Color.Black)
            ) {
                Text("Connect Power Station", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

// ===================================================================== main
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(store: Store, onLogout: () -> Unit) {
    val ui by Repo.state.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    val ctx = LocalContext.current
    val tabs = listOf(
        "Home" to "⚡",
        "Solar & AC" to "☀",
        "History" to "📈",
        "Raw" to "📋",
        "Settings" to "⚙"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("River 2", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                                // Live Connection Dot Pill
                                Surface(
                                    shape = CircleShape,
                                    color = if (ui.connected) EMERALD.copy(alpha = 0.15f) else CRIMSON.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, if (ui.connected) EMERALD.copy(alpha = 0.4f) else CRIMSON.copy(alpha = 0.4f)),
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(if (ui.connected) EMERALD else CRIMSON)
                                        )
                                        Text(
                                            if (ui.connected) "LIVE" else "OFFLINE",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (ui.connected) EMERALD else CRIMSON
                                        )
                                    }
                                }
                            }
                            Text(ui.status, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                },
                actions = {
                    Surface(
                        onClick = { MonitorService.start(ctx) },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text("⟳", fontSize = 18.sp) }
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        onClick = {
                            ctx.stopService(Intent(ctx, MonitorService::class.java))
                            store.password = ""
                            onLogout()
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text("⏻", fontSize = 16.sp, color = CRIMSON) }
                    }
                    Spacer(Modifier.width(12.dp))
                },
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
            ) {
                tabs.forEachIndexed { i, (name, icon) ->
                    NavigationBarItem(
                        selected = tab == i, onClick = { tab = i },
                        icon = { Text(icon, fontSize = 18.sp) },
                        label = { Text(name, fontSize = 10.sp, fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        )
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            when (tab) {
                0 -> Dashboard(ui, store)
                1 -> SolarAc(ui, store)
                2 -> HistoryScreen(ui)
                3 -> RawScreen(ui)
                else -> SettingsScreen(store)
            }
        }
    }
}

@Composable
fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    subValue: String? = null,
    accentColor: Color? = null,
) {
    Card(
        modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = accentColor ?: MaterialTheme.colorScheme.onSurface)
            if (subValue != null) {
                Spacer(Modifier.height(2.dp))
                Text(subValue, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        title.uppercase(Locale.US),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
    )
}

// ===================================================================== dashboard
@Composable
fun Gauge(s: Summary?) {
    val soc = s?.soc
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val activeColor = when {
        soc == null -> Color.Gray
        soc <= 15 -> CRIMSON
        soc <= 30 -> AMBER
        else -> EMERALD
    }
    val (label, mins) = s?.eta() ?: ("Idle" to null)
    val net = s?.net ?: 0.0

    Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(18.dp)) {
            val strokeWidth = 26f
            val st = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            // Background track arc
            drawArc(trackColor, 135f, 270f, false, style = st)
            // Active gauge arc
            if (soc != null) drawArc(activeColor, 135f, 270f * (soc.toFloat() / 100f), false, style = st)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(fmt(soc, "%"), fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
            Text("BATTERY LEVEL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
            Spacer(Modifier.height(8.dp))

            // Time Remaining Pill Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    if (mins == null) label else "${fmtMin(mins)} $label",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            // Power Flow Net Status
            if (s != null && (kotlin.math.abs(net) >= 3)) {
                Spacer(Modifier.height(6.dp))
                val isCharging = net > 0
                Text(
                    text = if (isCharging) "▲ Charging +${net.toInt()} W" else "▼ Discharging ${net.toInt()} W",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCharging) EMERALD else ORANGE
                )
            }
        }
    }
}

@Composable
fun Dashboard(ui: UiState, store: Store) {
    val (s, e) = ui.summary to ui.energy
    val price = store.price.toDouble()
    val cur = store.currency
    fun money(wh: Double) = if (price > 0) String.format(Locale.US, "%s%.2f", cur, (wh / 1000) * price) else "set price in ⚙"

    val savedTodayStr = if (price > 0) "${money(e.todaySolar)} (${fmtWh(e.todaySolar)})" else money(e.todaySolar)
    val totalSavedStr = if (price > 0) "${money(e.totalSolar)} (${fmtWh(e.totalSolar)})" else money(e.totalSolar)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Hero Battery Gauge Card
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                Gauge(s)
            }
        }

        // Hero Savings Card (Feature Highlight)
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            border = BorderStroke(1.dp, EMERALD.copy(alpha = 0.4f)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(EMERALD.copy(alpha = 0.12f), CYAN.copy(alpha = 0.05f))
                        )
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("🌱", fontSize = 16.sp)
                        Text("ELECTRICITY SAVED (SOLAR)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EMERALD, letterSpacing = 1.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Saved Today", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(savedTodayStr, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = EMERALD)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Savings", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(totalSavedStr, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        // Section 1: Power Overview
        SectionHeader("Power Summary")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile("TOTAL INPUT", fmt(s?.inW, " W"), Modifier.weight(1f), accentColor = CYAN)
            MetricTile("TOTAL OUTPUT", fmt(s?.outW, " W"), Modifier.weight(1f), accentColor = ORANGE)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile("NET POWER", if (s == null) "—" else String.format(Locale.US, "%+.0f W", s.net), Modifier.weight(1f))
            MetricTile("STORED ENERGY", if (s == null) "—" else "${fmt(s.storedWh)} / ${fmt(s.capWh)} Wh", Modifier.weight(1f))
        }

        // Section 2: Input & Output Breakdown
        SectionHeader("Input & Output Sources")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile("☀ SOLAR IN", fmt(s?.solar, " W"), Modifier.weight(1f), accentColor = AMBER)
            MetricTile("🔌 AC IN", fmt(s?.acIn, " W"), Modifier.weight(1f), accentColor = INDIGO)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile("AC OUTPUT", fmt(s?.acOut, " W"), Modifier.weight(1f))
            MetricTile("12V DC OUT", fmt(s?.car, " W"), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile("USB OUT", fmt(s?.usb, " W"), Modifier.weight(1f))
            MetricTile("BATTERY TEMP", fmt(s?.temp, " °C"), Modifier.weight(1f))
        }

        // Section 3: Battery Health
        SectionHeader("Battery Health")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile("CYCLES / HEALTH", "${fmt(s?.cycles)} / ${fmt(s?.soh, "%")}", Modifier.weight(1f))
            MetricTile("PACK V / A", "${fmt(s?.vol, " V", 1)} / ${fmt(s?.amp, " A", 1)}", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
    }
}

// ===================================================================== solar & ac
@Composable
fun RowKV(k: String, v: String) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(k, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(v, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
    val solarPct = if (tot > 0) (e.todaySolar / tot).toFloat() else 0f

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Solar Panel Card
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, AMBER.copy(alpha = 0.4f)),
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("☀", fontSize = 20.sp)
                    Text("Solar Panel", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AMBER)
                }
                Spacer(Modifier.height(12.dp))
                RowKV("Producing Now", fmt(s?.solar, " W"))
                RowKV("Panel Voltage", fmt(s?.solarV, " V", 1))
                RowKV("Panel Current", fmt(s?.solarA, " A", 2))
                RowKV("Peak Today", fmt(e.peakSolar, " W"))
                RowKV("Produced Today", fmtWh(e.todaySolar))
                RowKV("Produced This Session", fmtWh(e.sessSolar))
                RowKV("Total Solar Produced", fmtWh(e.totalSolar))
                RowKV("Sun Hours Today", fmtHms(e.solarSecs))
                RowKV("Solar Value Today", money(e.todaySolar))

                Spacer(Modifier.height(8.dp))
                Text("Solar Share of Input Today", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { solarPct },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = AMBER,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }

        // AC Charging Card
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, INDIGO.copy(alpha = 0.4f)),
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🔌", fontSize = 20.sp)
                    Text("AC Wall Charging", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = INDIGO)
                }
                Spacer(Modifier.height(12.dp))
                RowKV("Drawing Now", fmt(s?.acIn, " W"))
                RowKV("AC Voltage", fmt(s?.acInV, " V"))
                RowKV("AC Frequency", fmt(s?.acInF, " Hz"))
                RowKV("AC Active?", if ((s?.acIn ?: 0.0) > 5) "Yes" else "No")
                RowKV("Used Today", fmtWh(e.todayAc))
                RowKV("Used This Session", fmtWh(e.sessAc))
                RowKV("Total AC Used", fmtWh(e.totalAc))
                RowKV("Charging Time Today", fmtHms(e.acSecs))
                RowKV("AC Cost Today", money(e.todayAc))
                RowKV("AC Cost Total", money(e.totalAc))
            }
        }

        // Daily Totals Card
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("Daily Energy History", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                RowKV("Date (solar / AC / in / out)", "")
                e.days.keys.asSequence().sortedDescending().take(30).forEach { d ->
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
    val axisColor = MaterialTheme.colorScheme.outlineVariant

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Power History", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1, 6, 24).forEach { h ->
                FilterChip(
                    selected = hours == h, onClick = { hours = h }, label = { Text("${h}h") },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("● Battery", color = EMERALD, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("● In W", color = CYAN, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("● Out W", color = ORANGE, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("● Solar W", color = AMBER, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text("● AC W", color = INDIGO, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        val now = System.currentTimeMillis()
        val t0 = now - hours * 3_600_000L
        val pts = ui.history.filter { it.t >= t0 }
        val wMax = if (pts.isEmpty()) 100.0 else max(100.0, pts.maxOf {
            maxOf(it.inW ?: 0.0, it.outW ?: 0.0, it.solar ?: 0.0, it.acIn ?: 0.0)
        })

        Card(
            Modifier.fillMaxWidth().weight(1f).padding(top = 12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Canvas(Modifier.fillMaxSize().padding(16.dp)) {
                for (i in 0..4) {
                    val y = size.height * i / 4
                    drawLine(axisColor, Offset(0f, y), Offset(size.width, y), 1f)
                }
                if (pts.size < 2) return@Canvas
                fun line(color: Color, top: Double, get: (Sample) -> Double?) {
                    val path = Path()
                    var started = false
                    for (p in pts) {
                        val v = get(p) ?: continue
                        val x = ((p.t - t0).toFloat() / (now - t0)) * size.width
                        val y = size.height * (1f - (v.coerceIn(0.0, top) / top).toFloat())
                        if (!started) { path.moveTo(x, y); started = true } else path.lineTo(x, y)
                    }
                    drawPath(path, color, style = Stroke(width = 4f, cap = StrokeCap.Round))
                }
                line(EMERALD, 100.0) { it.soc }
                line(CYAN, wMax) { it.inW }
                line(ORANGE, wMax) { it.outW }
                line(AMBER, wMax) { it.solar }
                line(INDIGO, wMax) { it.acIn }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Scale: Battery % (0–100%), Watts scaled to max value in view (${wMax.toInt()} W).",
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ===================================================================== raw
@Composable
fun RawScreen(ui: UiState) {
    var q by remember { mutableStateOf("") }
    var showLog by remember { mutableStateOf(ui.raw.isEmpty()) }
    @Suppress("DEPRECATION")
    val clip = LocalClipboardManager.current

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !showLog, onClick = { showLog = false }, label = { Text("Values") },
                shape = RoundedCornerShape(20.dp)
            )
            FilterChip(
                selected = showLog, onClick = { showLog = true }, label = { Text("Log") },
                shape = RoundedCornerShape(20.dp)
            )
            if (showLog) {
                Spacer(Modifier.weight(1f))
                TextButton({ clip.setText(AnnotatedString(ui.debug.joinToString("\n"))) }) { Text("Copy Log") }
            }
        }

        Spacer(Modifier.height(10.dp))

        if (showLog) {
            Text("Connection log (newest first). Credentials are masked.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyColumn(Modifier.padding(top = 8.dp)) {
                items(ui.debug) { line ->
                    Text(line, fontSize = 11.sp, modifier = Modifier.padding(vertical = 3.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            return
        }

        val rows = ui.raw.entries.sortedBy { it.key }.filter {
            q.isBlank() || it.key.contains(q, true) || it.value.toString().contains(q, true)
        }

        OutlinedTextField(
            q, { q = it }, label = { Text("Filter MQTT parameters") }, singleLine = true,
            shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
        )

        LazyColumn(Modifier.padding(top = 10.dp)) {
            items(rows) { r ->
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(r.key, fontSize = 12.sp, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(r.value.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

// ===================================================================== settings
@Composable
fun SettingsScreen(store: Store) {
    var model by remember { mutableStateOf(store.model) }
    var price by remember { mutableStateOf(store.price.toString()) }
    var cur by remember { mutableStateOf(store.currency) }
    var low by remember { mutableStateOf(store.lowSoc.toString()) }
    var crit by remember { mutableStateOf(store.critSoc.toString()) }
    var full by remember { mutableStateOf(store.fullSoc.toString()) }
    var temp by remember { mutableStateOf(store.highTemp.toString()) }
    var over by remember { mutableStateOf(store.overloadW.toString()) }
    val num = KeyboardOptions(keyboardType = KeyboardType.Number)
    var savedMessage by remember { mutableStateOf(false) }

    @Composable
    fun field(label: String, v: String, set: (String) -> Unit, kb: KeyboardOptions = num) =
        OutlinedTextField(
            v, set, label = { Text(label) }, singleLine = true,
            shape = RoundedCornerShape(12.dp), keyboardOptions = kb, modifier = Modifier.fillMaxWidth()
        )

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("App & Device Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text("Configure device model, currency, electricity pricing, and alert thresholds.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))

        Text("Device Model", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MODELS.keys.forEach { m ->
                FilterChip(
                    selected = model == m, onClick = { model = m }, label = { Text(m, fontSize = 11.sp) },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
        field("Electricity price per kWh", price, { price = it }, KeyboardOptions(keyboardType = KeyboardType.Decimal))
        field("Currency symbol", cur, { cur = it }, KeyboardOptions.Default)
        field("Low battery alert %", low, { low = it })
        field("Critical alert %", crit, { crit = it })
        field("Fully charged at %", full, { full = it })
        field("High temperature °C", temp, { temp = it })
        field("Overload alert (W)", over, { over = it })

        Button(
            onClick = {
                store.model = model
                store.price = price.toFloatOrNull() ?: 0f
                store.currency = cur
                store.lowSoc = low.toIntOrNull() ?: 20
                store.critSoc = crit.toIntOrNull() ?: 10
                store.fullSoc = full.toIntOrNull() ?: 100
                store.highTemp = temp.toIntOrNull() ?: 45
                store.overloadW = over.toIntOrNull() ?: 700
                savedMessage = true
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Save Settings", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        if (savedMessage) {
            Text("Settings saved successfully!", color = EMERALD, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(Modifier.height(32.dp))
    }
}
