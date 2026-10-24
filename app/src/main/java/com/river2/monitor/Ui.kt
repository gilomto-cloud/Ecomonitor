package com.river2.monitor

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds

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

fun getAppVersion(context: android.content.Context): String {
    return try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        pInfo.versionName ?: "1.0.0"
    } catch (_: Exception) {
        "1.0.0"
    }
}

// ===================================================================== login
@Composable
fun LoginScreen(store: Store, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val appVersion = remember { getAppVersion(ctx) }
    var email by remember { mutableStateOf(store.email) }
    var pw by remember { mutableStateOf(store.password) }
    var sn by remember { mutableStateOf(store.sn) }
    var region by remember { mutableStateOf(store.region) }
    var model by remember { mutableStateOf(store.model) }

    var passwordVisible by remember { mutableStateOf(false) }
    var showSnInfo by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))

            // Hero Branding Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    )
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(
                                EMERALD.copy(alpha = 0.4f),
                                CYAN.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(24.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Glowing Icon Container
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(EMERALD.copy(alpha = 0.2f), CYAN.copy(alpha = 0.15f))
                                    )
                                )
                                .border(
                                    1.dp,
                                    EMERALD.copy(alpha = 0.5f),
                                    RoundedCornerShape(16.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Bolt,
                                contentDescription = null,
                                tint = EMERALD,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // App Badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, EMERALD.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(EMERALD)
                                )
                                Text(
                                    "ECOFLOW LINK",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "EcoMonitor",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Sign in with your EcoFlow account to connect directly to your River 2 power station telemetry.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // Main Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "ACCOUNT CREDENTIALS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EMERALD,
                        letterSpacing = 1.sp
                    )

                    // Email Field
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("EcoFlow Email") },
                        placeholder = { Text("name@example.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Email,
                                contentDescription = null,
                                tint = if (email.isNotBlank()) EMERALD else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (email.isNotEmpty()) {
                                IconButton(onClick = { email = "" }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Clear,
                                        contentDescription = "Clear email",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EMERALD,
                            focusedLabelColor = EMERALD
                        )
                    )

                    // Password Field
                    OutlinedTextField(
                        value = pw,
                        onValueChange = { pw = it },
                        label = { Text("EcoFlow Password") },
                        placeholder = { Text("Enter account password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = if (pw.isNotEmpty()) EMERALD else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EMERALD,
                            focusedLabelColor = EMERALD
                        )
                    )

                    // Serial Number Field
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = sn,
                            onValueChange = { sn = it.trim() },
                            label = { Text("Device Serial Number (SN)") },
                            placeholder = { Text("e.g., R331ZEB...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.QrCode,
                                    contentDescription = null,
                                    tint = if (sn.isNotBlank()) EMERALD else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { showSnInfo = !showSnInfo }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = "SN Information",
                                        tint = if (showSnInfo) EMERALD else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Ascii,
                                imeAction = ImeAction.Done
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EMERALD,
                                focusedLabelColor = EMERALD
                            )
                        )

                        // Helper/Info Expandable Card
                        AnimatedVisibility(
                            visible = showSnInfo,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = EMERALD,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        "Find your Serial Number on the barcode sticker located on the bottom/back of your River 2 station, or in the EcoFlow app under Device Settings -> Info.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 1.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Region Selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "SERVER REGION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EMERALD,
                            letterSpacing = 1.sp
                        )

                        val regionOptions = mapOf(
                            "Global" to "US, Asia & Int'l",
                            "Europe" to "European Union"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            regionOptions.forEach { (r, desc) ->
                                val selected = region == r
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { region = r },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        color = if (selected) EMERALD else MaterialTheme.colorScheme.outlineVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (selected) {
                                            Icon(
                                                imageVector = Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                r,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                desc,
                                                fontSize = 10.sp,
                                                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Device Model Selector
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "DEVICE MODEL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EMERALD,
                            letterSpacing = 1.sp
                        )

                        val capacities = mapOf(
                            "River 2" to "256 Wh",
                            "River 2 Max" to "512 Wh",
                            "River 2 Pro" to "768 Wh"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MODELS.keys.forEach { m ->
                                val selected = model == m
                                val capacity = capacities[m] ?: ""
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { model = m },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(
                                        width = if (selected) 1.5.dp else 1.dp,
                                        color = if (selected) EMERALD else MaterialTheme.colorScheme.outlineVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            m,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                        Text(
                                            capacity,
                                            fontSize = 10.sp,
                                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Security & Privacy Trust Callout
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(EMERALD.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = EMERALD,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "256-bit AES Encrypted Storage",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Your credentials are securely stored locally via Android EncryptedSharedPreferences and transmitted directly to official EcoFlow servers.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            val formValid = email.isNotBlank() && pw.isNotEmpty() && sn.isNotBlank()

            // Primary Connect Action CTA
            Button(
                onClick = {
                    if (formValid && !isConnecting) {
                        isConnecting = true
                        store.email = email.trim()
                        store.password = pw
                        store.sn = sn.trim()
                        store.region = region
                        store.model = model
                        MonitorService.start(ctx)
                        onDone()
                    }
                },
                enabled = formValid && !isConnecting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EMERALD,
                    contentColor = Color.Black,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 6.dp
                )
            ) {
                if (isConnecting) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.Black,
                            strokeWidth = 2.5.dp
                        )
                        Text("Connecting Station...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Power,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text("Connect Power Station", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }

            // Footer
            Text(
                "EcoMonitor v$appVersion • Open-Source EcoFlow Telemetry",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
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
    LaunchedEffect(store.githubRepo) {
        val um = UpdateManager(ctx, store.githubRepo)
        um.checkForUpdates()
    }

    val showRaw = store.showRawTab
    val tabs = remember(showRaw) {
        buildList {
            add("Home" to "⚡")
            add("Solar & AC" to "☀")
            add("History" to "📈")
            if (showRaw) add("Raw" to "📋")
            add("Settings" to "⚙")
        }
    }

    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to disconnect from your EcoFlow account and sign out?", fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    ctx.stopService(Intent(ctx, MonitorService::class.java))
                    store.password = ""
                    onLogout()
                }) {
                    Text("Sign Out", color = CRIMSON, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }

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
                                Text("EcoMonitor", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                                // Live Connection Dot Pill
                                Surface(
                                    shape = CircleShape,
                                    color = if (ui.connected) EMERALD.copy(alpha = 0.15f) else CRIMSON.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, if (ui.connected) EMERALD.copy(alpha = 0.4f) else CRIMSON.copy(alpha = 0.4f)),
                                ) {
                                    Row(
                                        Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Box(
                                            Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(if (ui.connected) EMERALD else CRIMSON),
                                        )
                                        Text(
                                            if (ui.connected) "LIVE" else "OFFLINE",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (ui.connected) EMERALD else CRIMSON,
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
                        modifier = Modifier.size(36.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text("⟳", fontSize = 18.sp) }
                    }
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        onClick = { showLogoutDialog = true },
                        shape = CircleShape,
                        color = CRIMSON.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, CRIMSON.copy(alpha = 0.3f)),
                        modifier = Modifier.size(36.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) { Text("🚪", fontSize = 16.sp) }
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
            val currentTabName = tabs.getOrNull(tab)?.first ?: "Home"
            when (currentTabName) {
                "Home" -> Dashboard(ui, store)
                "Solar & AC" -> SolarAc(ui, store)
                "History" -> HistoryScreen(ui, store)
                "Raw" -> RawScreen(ui)
                "Settings" -> SettingsScreen(store, onLogout)
                else -> Dashboard(ui, store)
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
            drawArc(trackColor, 135f, 270f, useCenter = false, style = st)
            // Active gauge arc
            soc?.let {
                val safeSoc = it.toFloat().coerceIn(0f, 100f)
                drawArc(activeColor, 135f, 270f * (safeSoc / 100f), useCenter = false, style = st)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (net > 2) {
                    Text("⚡ ", fontSize = 28.sp, color = EMERALD)
                }
                Text(
                    fmt(soc, "%"),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                "BATTERY LEVEL",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
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
            if ((s != null) && (kotlin.math.abs(net) >= 3)) {
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
fun Dashboard(
    ui: UiState,
    store: Store,
) {
    val (s, e) = ui.summary to ui.energy
    val price = store.price.toDouble()
    val cur = store.currency
    fun money(wh: Double) = if (price > 0) String.format(Locale.US, "%s%.2f", cur, (wh / 1000.0) * price) else ""

    val savedTodayStr = if (price > 0) "${money(e.todaySolar)} (${fmtWh(e.todaySolar)})" else fmtWh(e.todaySolar)
    val totalSavedStr = if (price > 0) "${money(e.totalSolar)} (${fmtWh(e.totalSolar)})" else fmtWh(e.totalSolar)

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
            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                // Connection & Model Status Chip
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (ui.connected) EMERALD.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(0.5.dp, if (ui.connected) EMERALD.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        "${store.model} • ${if (ui.connected) "Connected" else "Disconnected"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (ui.connected) EMERALD else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
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
                    if (price <= 0) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "💡 Tip: Set electricity price per kWh in Settings ⚙ to estimate $ currency savings.",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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

        val netVal = s?.net ?: 0.0
        val netColor = when {
            netVal > 2 -> EMERALD
            netVal < -2 -> ORANGE
            else -> MaterialTheme.colorScheme.onSurface
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile("NET POWER", if (s == null) "—" else String.format(Locale.US, "%+.0f W", s.net), Modifier.weight(1f), accentColor = netColor)
            MetricTile("STORED ENERGY", if (s == null) "—" else "${fmtWh(s.storedWh)} / ${fmtWh(s.capWh)}", Modifier.weight(1f))
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
    val (s, e) = ui.summary to ui.energy
    val price = store.price.toDouble()
    val cur = store.currency
    fun money(wh: Double) = if (price > 0) String.format(Locale.US, "%s%.2f", cur, (wh / 1000.0) * price) else "Set price in ⚙"
    val tot = e.todaySolar + e.todayAc
    val solarPct = if (tot > 0) (e.todaySolar / tot).toFloat() else 0f

    val isSolarActive = (s?.solar ?: 0.0) > 1.0
    val isAcActive = (s?.acIn ?: 0.0) > 5.0

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Quick Live Input Comparison Header Tiles
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricTile(
                label = "SOLAR INPUT",
                value = fmt(s?.solar, " W"),
                subValue = if (isSolarActive) "● Generating" else "Idle",
                accentColor = AMBER,
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = "AC GRID INPUT",
                value = fmt(s?.acIn, " W"),
                subValue = if (isAcActive) "● Active" else "Standby",
                accentColor = INDIGO,
                modifier = Modifier.weight(1f)
            )
        }

        // Solar Panel Card
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, AMBER.copy(alpha = 0.4f)),
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("☀", fontSize = 20.sp)
                        Text("Solar Panel", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AMBER)
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSolarActive) EMERALD.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(0.5.dp, if (isSolarActive) EMERALD.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            if (isSolarActive) "● Generating" else "Idle",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSolarActive) EMERALD else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
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
                RowKV("Total Solar Value Saved", money(e.totalSolar))

                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Solar Share of Input Today", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${(solarPct * 100).toInt()}% Solar",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AMBER
                    )
                }
                Spacer(Modifier.height(6.dp))
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
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("🔌", fontSize = 20.sp)
                        Text("AC Wall Charging", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = INDIGO)
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isAcActive) INDIGO.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(0.5.dp, if (isAcActive) INDIGO.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            if (isAcActive) "● Fast Charging" else "Standby",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAcActive) INDIGO else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                RowKV("Drawing Now", fmt(s?.acIn, " W"))
                RowKV("AC Voltage", fmt(s?.acInV, " V"))
                RowKV("AC Frequency", fmt(s?.acInF, " Hz"))
                RowKV("AC Active?", if (isAcActive) "Yes" else "No")
                RowKV("Used Today", fmtWh(e.todayAc))
                RowKV("Used This Session", fmtWh(e.sessAc))
                RowKV("Total AC Used", fmtWh(e.totalAc))
                RowKV("Charging Time Today", fmtHms(e.acSecs))
                RowKV("AC Cost Today", money(e.todayAc))
                RowKV("AC Cost Total", money(e.totalAc))
            }
        }
    }
}

// ===================================================================== history
@Composable
fun HistoryScreen(ui: UiState, store: Store? = null) {
    var hours by remember { mutableIntStateOf(1) }
    var showSoc by remember { mutableStateOf(true) }
    var showInW by remember { mutableStateOf(true) }
    var showOutW by remember { mutableStateOf(true) }
    var showSolar by remember { mutableStateOf(true) }
    var showAcIn by remember { mutableStateOf(true) }

    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val e = ui.energy

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Power History", fontSize = 20.sp, fontWeight = FontWeight.Bold)

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

        Text("Metrics to display", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = showSoc, onClick = { showSoc = !showSoc },
                label = { Text("Battery") },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EMERALD.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                )
            )
            FilterChip(
                selected = showInW, onClick = { showInW = !showInW },
                label = { Text("In W") },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CYAN.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                )
            )
            FilterChip(
                selected = showOutW, onClick = { showOutW = !showOutW },
                label = { Text("Out W") },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ORANGE.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                )
            )
            FilterChip(
                selected = showSolar, onClick = { showSolar = !showSolar },
                label = { Text("Solar W") },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AMBER.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                )
            )
            FilterChip(
                selected = showAcIn, onClick = { showAcIn = !showAcIn },
                label = { Text("AC W") },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = INDIGO.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.onSurface,
                )
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (showSoc) Text("● Battery", color = EMERALD, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            if (showInW) Text("● In W", color = CYAN, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            if (showOutW) Text("● Out W", color = ORANGE, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            if (showSolar) Text("● Solar W", color = AMBER, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            if (showAcIn) Text("● AC W", color = INDIGO, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        val now = System.currentTimeMillis()
        val t0 = now - (hours * 3_600_000L)
        val pts = remember(ui.history, hours) { ui.history.filter { it.t >= t0 } }
        val activeMaxValues = remember(pts, showInW, showOutW, showSolar, showAcIn) {
            pts.map { p ->
                listOfNotNull(
                    if (showInW) p.inW else null,
                    if (showOutW) p.outW else null,
                    if (showSolar) p.solar else null,
                    if (showAcIn) p.acIn else null,
                ).maxOrNull() ?: 0.0
            }
        }
        val wMax = historyPowerScaleMax(
            store?.model,
            activeMaxValues
        ).coerceAtLeast(1.0)

        Card(
            Modifier.fillMaxWidth().height(260.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Box(Modifier.fillMaxSize().padding(16.dp)) {
                val chartBgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
                val gridColor = axisColor.copy(alpha = 0.35f)
                val gridStrongColor = axisColor.copy(alpha = 0.7f)
                val linePath = remember { Path() }
                val fillPath = remember { Path() }
                Canvas(Modifier.fillMaxSize()) {
                    drawRoundRect(chartBgColor, cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f))
                    for (i in 0..4) {
                        val y = (size.height * i) / 4f
                        drawLine(if (i == 0 || i == 4) gridStrongColor else gridColor, Offset(0f, y), Offset(size.width, y), 1f)
                        val x = (size.width * i) / 4f
                        drawLine(gridColor.copy(alpha = 0.25f), Offset(x, 0f), Offset(x, size.height), 0.5f)
                    }
                    if (pts.size < 2) return@Canvas
                    val timeSpan = maxOf(1L, now - t0)
                    fun line(color: Color, top: Double, get: (Sample) -> Double?) {
                        linePath.reset()
                        fillPath.reset()
                        var started = false
                        for (p in pts) {
                            val v = get(p) ?: continue
                            val x = (((p.t - t0).toFloat() / timeSpan) * size.width).coerceIn(0f, size.width)
                            val y = size.height * (1f - (v.coerceIn(0.0, top) / top).toFloat())
                            if (!started) {
                                linePath.moveTo(x, y)
                                fillPath.moveTo(x, size.height)
                                fillPath.lineTo(x, y)
                                started = true
                            } else {
                                linePath.lineTo(x, y)
                                fillPath.lineTo(x, y)
                            }
                        }
                        if (!started) return
                        fillPath.lineTo(size.width, size.height)
                        fillPath.close()
                        drawPath(
                            fillPath,
                            Brush.verticalGradient(listOf(color.copy(alpha = 0.38f), color.copy(alpha = 0.04f))),
                        )
                        drawPath(linePath, color, style = Stroke(width = 3.5f, cap = StrokeCap.Round))
                    }
                    if (showSoc) line(EMERALD, 100.0) { it.soc }
                    if (showInW) line(CYAN, wMax) { it.inW }
                    if (showOutW) line(ORANGE, wMax) { it.outW }
                    if (showSolar) line(AMBER, wMax) { it.solar }
                    if (showAcIn) line(INDIGO, wMax) { it.acIn }
                }

                // Y-Axis Watts Scale Overlay
                Column(
                    Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${wMax.toInt()} W", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f), fontWeight = FontWeight.Bold)
                    Text("${(wMax * 0.75).toInt()} W", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    Text("${(wMax * 0.5).toInt()} W", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    Text("${(wMax * 0.25).toInt()} W", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    Text("0 W", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                }

                // Battery % Scale Overlay on Right Side if Battery enabled
                if (showSoc) {
                    Column(
                        Modifier.fillMaxHeight().align(Alignment.TopEnd),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.End
                    ) {
                        Text("100%", fontSize = 9.sp, color = EMERALD, fontWeight = FontWeight.Bold)
                        Text("75%", fontSize = 9.sp, color = EMERALD.copy(alpha = 0.7f))
                        Text("50%", fontSize = 9.sp, color = EMERALD.copy(alpha = 0.7f))
                        Text("25%", fontSize = 9.sp, color = EMERALD.copy(alpha = 0.7f))
                        Text("0%", fontSize = 9.sp, color = EMERALD.copy(alpha = 0.7f))
                    }
                }

                // Empty State Overlay if insufficient samples
                if (pts.size < 2) {
                    Text(
                        "Collecting power history samples...",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val sdf = SimpleDateFormat(if (hours > 6) "MM-dd HH:mm" else "HH:mm", Locale.US)
            Text(sdf.format(Date(t0)), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(sdf.format(Date(t0 + (now - t0) / 4)), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(sdf.format(Date(t0 + (now - t0) / 2)), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(sdf.format(Date(t0 + 3 * (now - t0) / 4)), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(sdf.format(Date(now)), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Text(
            "Scale: Battery % (0–100%), Watts scaled to max value in view (${wMax.toInt()} W).",
            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Summary stats for selected time window
        val peakSolarWin = pts.mapNotNull { it.solar }.maxOrNull() ?: 0.0
        val peakInWin = pts.mapNotNull { it.inW }.maxOrNull() ?: 0.0
        val peakOutWin = pts.mapNotNull { it.outW }.maxOrNull() ?: 0.0
        val minSocWin = pts.mapNotNull { it.soc }.minOrNull()
        val maxSocWin = pts.mapNotNull { it.soc }.maxOrNull()

        if (pts.isNotEmpty()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (showSolar) StatChip("Peak Solar", "${peakSolarWin.toInt()} W", AMBER)
                if (showInW) StatChip("Peak In", "${peakInWin.toInt()} W", CYAN)
                if (showOutW) StatChip("Peak Out", "${peakOutWin.toInt()} W", ORANGE)
                if (showSoc && minSocWin != null && maxSocWin != null) {
                    StatChip("Battery Range", "${minSocWin.toInt()}%–${maxSocWin.toInt()}%", EMERALD)
                }
            }
        }

        // Daily Energy History Section with Calendar View Toggle
        var showCalendar by remember { mutableStateOf(true) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = showCalendar, onClick = { showCalendar = true }, label = { Text("Calendar View") },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
            FilterChip(
                selected = !showCalendar, onClick = { showCalendar = false }, label = { Text("List View") },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        }

        if (showCalendar) {
            CalendarHistoryCard(e, store)
        } else {
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
                        RowKV(d, "${fmtWh(r.getOrNull(0) ?: 0.0)} / ${fmtWh(r.getOrNull(1) ?: 0.0)} / ${fmtWh(r.getOrNull(2) ?: 0.0)} / ${fmtWh(r.getOrNull(3) ?: 0.0)}")
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarHistoryCard(e: Energy, store: Store? = null) {
    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now().toString()) }
    val todayStr = remember { LocalDate.now().toString() }

    val price = store?.price?.toDouble() ?: 0.0
    val cur = store?.currency ?: "$"
    fun money(wh: Double): String =
        if (price > 0) String.format(Locale.US, " (%s%.2f)", cur, (wh / 1000.0) * price) else ""

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(18.dp)) {
            // Month Header with Prev/Next Navigation
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Daily Energy Calendar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    TextButton(onClick = { currentYearMonth = currentYearMonth.minusMonths(1) }) {
                        Text("‹", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)
                    Text(
                        currentYearMonth.format(monthFormatter),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = { currentYearMonth = currentYearMonth.plusMonths(1) }) {
                        Text("›", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Monthly Energy Totals Summary Banner
            val monthPrefix = currentYearMonth.toString()
            val monthRecords = e.days.filterKeys { it.startsWith(monthPrefix) }
            val monthSolar = monthRecords.values.sumOf { it.getOrNull(0) ?: 0.0 }
            val monthAc = monthRecords.values.sumOf { it.getOrNull(1) ?: 0.0 }

            if (monthSolar > 0 || monthAc > 0) {
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Month Total:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Solar ${fmtWh(monthSolar)}${money(monthSolar)}  •  AC ${fmtWh(monthAc)}${money(monthAc)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(8.dp))
            } else {
                Spacer(Modifier.height(8.dp))
            }

            // Days of Week Header
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach { dayName ->
                    Text(
                        dayName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(36.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Calendar Month Grid
            val firstDayOfMonth = currentYearMonth.atDay(1)
            val daysInMonth = currentYearMonth.lengthOfMonth()
            val startOffset = firstDayOfMonth.dayOfWeek.value % 7
            val totalCells = startOffset + daysInMonth
            val rowsCount = (totalCells + 6) / 7

            for (row in 0 until rowsCount) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNum = cellIndex - startOffset + 1
                        if (cellIndex < startOffset || dayNum > daysInMonth) {
                            Box(Modifier.size(36.dp))
                        } else {
                            val date = currentYearMonth.atDay(dayNum)
                            val dateStr = date.toString()
                            val r = e.days[dateStr]
                            val isSelected = dateStr == selectedDate
                            val isToday = dateStr == todayStr

                            val solarVal = r?.getOrNull(0) ?: 0.0
                            val acVal = r?.getOrNull(1) ?: 0.0
                            val inVal = r?.getOrNull(2) ?: 0.0
                            val outVal = r?.getOrNull(3) ?: 0.0
                            val hasData = solarVal > 0 || acVal > 0 || inVal > 0 || outVal > 0

                            Surface(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                                    hasData -> EMERALD.copy(alpha = 0.12f)
                                    else -> Color.Transparent
                                },
                                border = when {
                                    isSelected -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                    isToday -> BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                                    else -> null
                                },
                                onClick = { selectedDate = dateStr }
                            ) {
                                Column(
                                    Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        "$dayNum",
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                                            isToday -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    if (solarVal > 0 || acVal > 0) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (solarVal > 0) {
                                                Box(
                                                    Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(EMERALD)
                                                )
                                            }
                                            if (acVal > 0) {
                                                Box(
                                                    Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(INDIGO)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(12.dp))

            // Selected Day Details
            val selectedRecord = e.days[selectedDate]
            val parsedDate = try {
                LocalDate.parse(selectedDate).format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.US))
            } catch (_: Exception) {
                selectedDate
            }

            val selSolar = selectedRecord?.getOrNull(0) ?: 0.0
            val selAc = selectedRecord?.getOrNull(1) ?: 0.0
            val selIn = selectedRecord?.getOrNull(2) ?: 0.0
            val selOut = selectedRecord?.getOrNull(3) ?: 0.0

            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (selectedDate == todayStr) "$parsedDate (Today)" else parsedDate,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (selectedRecord == null) {
                        Text("No data", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (selectedRecord != null) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        EnergyStatItem("Solar", fmtWh(selSolar), EMERALD, money(selSolar))
                        EnergyStatItem("AC Charge", fmtWh(selAc), INDIGO, money(selAc))
                        EnergyStatItem("Grid In", fmtWh(selIn), CYAN)
                        EnergyStatItem("Grid Out", fmtWh(selOut), ORANGE)
                    }
                } else {
                    Text(
                        "No energy history recorded for this date.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun EnergyStatItem(label: String, value: String, color: Color, subValue: String = "") {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        if (subValue.isNotEmpty()) {
            Text(subValue.trim(), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun StatChip(label: String, value: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

// ===================================================================== raw
@Composable
fun RawScreen(ui: UiState) {
    var q by remember { mutableStateOf("") }
    var showLog by remember { mutableStateOf(ui.raw.isEmpty()) }
    var copyNotice by remember { mutableStateOf<String?>(null) }
    @Suppress("DEPRECATION")
    val clip = LocalClipboardManager.current

    val rows = remember(ui.raw, q) {
        ui.raw.entries.asSequence().sortedBy { it.key }.filter {
            q.isBlank() || it.key.contains(q, ignoreCase = true) || it.value.toString().contains(q, ignoreCase = true)
        }.toList()
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !showLog, onClick = { showLog = false },
                    label = { Text("Values (${ui.raw.size})") },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                )
                FilterChip(
                    selected = showLog, onClick = { showLog = true },
                    label = { Text("Log (${ui.debug.size})") },
                    shape = RoundedCornerShape(20.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                )
            }

            if (showLog && ui.debug.isNotEmpty()) {
                TextButton(onClick = {
                    clip.setText(AnnotatedString(ui.debug.joinToString("\n")))
                    copyNotice = "Log copied!"
                }) {
                    Text("Copy Log")
                }
            } else if (!showLog && rows.isNotEmpty()) {
                TextButton(onClick = {
                    val text = rows.joinToString("\n") { "${it.key}: ${it.value}" }
                    clip.setText(AnnotatedString(text))
                    copyNotice = "Values copied!"
                }) {
                    Text("Copy All")
                }
            }
        }

        if (copyNotice != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                copyNotice!!,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = EMERALD,
                modifier = Modifier.align(Alignment.End)
            )
            LaunchedEffect(copyNotice) {
                kotlinx.coroutines.delay(2000.milliseconds)
                copyNotice = null
            }
        }

        Spacer(Modifier.height(8.dp))

        if (showLog) {
            Text("Connection log (newest first). Credentials are masked.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            if (ui.debug.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No log entries recorded yet.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn {
                    items(ui.debug) { line ->
                        Text(line, fontSize = 11.sp, modifier = Modifier.padding(vertical = 3.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            return
        }

        OutlinedTextField(
            q, { q = it },
            label = { Text("Filter MQTT parameters") },
            singleLine = true,
            trailingIcon = {
                if (q.isNotEmpty()) {
                    TextButton(onClick = { q = "" }) {
                        Text("✕", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        if (rows.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(top = 32.dp), contentAlignment = Alignment.TopCenter) {
                Text(
                    if (q.isBlank()) "No MQTT parameters received yet." else "No parameters match '$q'",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn {
                items(rows) { r ->
                    Card(
                        onClick = {
                            clip.setText(AnnotatedString("${r.key}: ${r.value}"))
                            copyNotice = "Copied '${r.key}'"
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(r.key, fontSize = 12.sp, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(8.dp))
                            Text(r.value.toString(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
    }
}

// ===================================================================== settings
@Composable
fun SettingsScreen(store: Store, onLogout: (() -> Unit)? = null) {
    val ctx = LocalContext.current
    val appVersion = remember { getAppVersion(ctx) }
    var model by remember { mutableStateOf(store.model) }
    var price by remember { mutableStateOf(if (store.price == 0f) "" else store.price.toString()) }
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
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    "v$appVersion",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Section 1: Account & Connection Info
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("ACCOUNT & CONNECTION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp)
                RowKV("EcoFlow Account", store.email.ifEmpty { "Not set" })
                RowKV("Device Serial (S/N)", store.sn.ifEmpty { "Not set" })
                RowKV("Region", store.region)

                if (onLogout != null) {
                    Spacer(Modifier.height(4.dp))
                    Button(
                        onClick = onLogout,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CRIMSON.copy(alpha = 0.12f), contentColor = CRIMSON),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign Out / Switch Account", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Section 2: Device Model & Pricing
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("DEVICE & PRICING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp)

                Text("Device Model", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MODELS.keys.forEach { m ->
                        FilterChip(
                            selected = model == m, onClick = { model = m }, label = { Text(m, fontSize = 11.sp) },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                field("Electricity price per kWh", price, { price = it; savedMessage = false }, KeyboardOptions(keyboardType = KeyboardType.Decimal))
                field("Currency symbol", cur, { cur = it; savedMessage = false }, KeyboardOptions.Default)
            }
        }

        // Section 3: Alerts & Safety Thresholds
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("ALERT THRESHOLDS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp)

                field("Low battery alert %", low, { low = it; savedMessage = false })
                field("Critical alert %", crit, { crit = it; savedMessage = false })
                field("Fully charged at %", full, { full = it; savedMessage = false })
                field("High temperature °C", temp, { temp = it; savedMessage = false })
                field("Overload alert (W)", over, { over = it; savedMessage = false })
            }
        }

        // Section 4: Advanced & Debug
        var showRaw by remember { mutableStateOf(store.showRawTab) }
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("ADVANCED & DEBUG", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp)

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Show Raw Telemetry Tab", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Exposes raw MQTT parameters & debug logs for developers", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = showRaw,
                        onCheckedChange = { showRaw = it; savedMessage = false }
                    )
                }
            }
        }

        // Save Button & Confirmation Banner
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
                store.showRawTab = showRaw
                savedMessage = true
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Save Settings", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        if (savedMessage) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = EMERALD.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, EMERALD.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "✓ Settings saved successfully!",
                    color = EMERALD,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Text(
            "EcoMonitor v$appVersion",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(32.dp))
    }
}

private fun modelPowerMax(model: String?): Double {
    val normalizedModel = model?.trim()?.takeIf { it.isNotEmpty() } ?: "River 2"
    return MODELS[normalizedModel] ?: MODELS.values.maxOrNull() ?: 256.0
}

private fun historyPowerScaleMax(model: String?, values: List<Double>): Double {
    val modelMax = modelPowerMax(model)
    val sampleMax = values.maxOrNull() ?: 0.0
    return max(modelMax, sampleMax)
}
