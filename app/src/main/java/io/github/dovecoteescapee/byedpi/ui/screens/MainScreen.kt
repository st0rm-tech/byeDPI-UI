package io.github.dovecoteescapee.byedpi.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.dovecoteescapee.byedpi.BuildConfig
import io.github.dovecoteescapee.byedpi.R
import io.github.dovecoteescapee.byedpi.data.AppStatus
import io.github.dovecoteescapee.byedpi.data.Mode
import io.github.dovecoteescapee.byedpi.fragments.MainSettingsFragment
import io.github.dovecoteescapee.byedpi.services.ServiceManager
import io.github.dovecoteescapee.byedpi.ui.theme.*
import io.github.dovecoteescapee.byedpi.ui.viewmodel.MainViewModel
import io.github.dovecoteescapee.byedpi.utility.getPreferences
import io.github.dovecoteescapee.byedpi.utility.mode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onNavigateToSettings: () -> Unit, onSaveLogs: () -> Unit, viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var connectionTime by remember { mutableStateOf(0L) }
    
    val vpnPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            ServiceManager.start(context, Mode.VPN)
        } else {
            Toast.makeText(context, R.string.vpn_permission_denied, Toast.LENGTH_SHORT).show()
            viewModel.updateStatus()
        }
    }

    LaunchedEffect(Unit) { viewModel.updateStatus() }
    
    LaunchedEffect(uiState.appStatus) {
        if (uiState.appStatus == AppStatus.Running) {
            while (true) {
                delay(1.seconds)
                connectionTime++
            }
        } else {
            connectionTime = 0
        }
    }

    uiState.errorMessage?.let { error ->
        LaunchedEffect(error) {
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    var selectedTab by remember { mutableStateOf(0) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // iOS-style gradient mesh background
        IOSGradientMesh(uiState.appStatus)
        
        Column(modifier = Modifier.fillMaxSize()) {
            // Content based on selected tab
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> {
                        // Home content
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Top bar with logs button
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "ByeDPI",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.W600,
                                    color = Color.White.copy(alpha = 0.95f)
                                )
                                IconButton(onClick = onSaveLogs) {
                                    Icon(Icons.Outlined.Description, null, tint = Color.White.copy(alpha = 0.7f))
                                }
                            }
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                Spacer(Modifier.height(20.dp))
                
                // Large status text (iOS style)
                IOSStatusHeader(uiState.appStatus, uiState.mode)
                
                Spacer(Modifier.height(12.dp))
                
                // Hero visual
                IOSHeroVisual(uiState.appStatus)
                
                Spacer(Modifier.height(24.dp))
                
                // Stats in glassmorphic cards
                if (uiState.appStatus == AppStatus.Running) {
                    IOSStatsCards(connectionTime, uiState.mode)
                }
                
                // Connection button
                IOSConnectionButton(uiState.appStatus, uiState.mode) {
                    when (uiState.appStatus) {
                        AppStatus.Halted -> {
                            val preferences = context.getPreferences()
                            when (preferences.mode()) {
                                Mode.VPN -> {
                                    val intentPrepare = VpnService.prepare(context)
                                    if (intentPrepare != null) {
                                        vpnPermissionLauncher.launch(intentPrepare)
                                    } else {
                                        ServiceManager.start(context, Mode.VPN)
                                    }
                                }
                                Mode.Proxy -> ServiceManager.start(context, Mode.Proxy)
                            }
                        }
                        AppStatus.Running -> ServiceManager.stop(context)
                    }
                }
                
                // Proxy info
                AnimatedVisibility(
                    visible = uiState.mode == Mode.Proxy || uiState.appStatus == AppStatus.Running,
                    enter = fadeIn(tween(400)) + expandVertically(),
                    exit = fadeOut(tween(300)) + shrinkVertically()
                ) {
                    IOSProxyCard(uiState.proxyAddress)
                }
                
                // Quick actions
                if (uiState.appStatus == AppStatus.Running) {
                    IOSQuickActions(
                        onReconnect = {
                            ServiceManager.stop(context)
                            scope.launch {
                                delay(500)
                                ServiceManager.start(context, context.getPreferences().mode())
                            }
                        },
                        onChangeMode = {
                            val preferences = context.getPreferences()
                            val newMode = if (preferences.mode() == Mode.VPN) Mode.Proxy else Mode.VPN
                            preferences.edit().putString("byedpi_mode", newMode.toString().lowercase()).apply()
                            ServiceManager.stop(context)
                            scope.launch {
                                delay(500)
                                ServiceManager.start(context, newMode)
                            }
                        }
                    )
                }
                
                                Spacer(Modifier.height(100.dp))
                            }
                        }
                    }
                    1 -> {
                        // Settings tab - show settings in same screen
                        Column(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 24.dp)
                                    .padding(top = 40.dp, bottom = 120.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "Settings",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.W700,
                                    color = Color.White,
                                    letterSpacing = (-0.5).sp
                                )
                                
                                Spacer(Modifier.height(8.dp))
                                
                                // Settings content inline
                                SettingsContent(onNavigateToSettings, onSaveLogs)
                            }
                        }
                    }
                }
            }
            
            // iOS-style bottom navigation
            IOSBottomNavigation(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    }
}

@Composable
private fun IOSBottomNavigation(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.wrapContentWidth(),
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF1A1A1A),
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IOSNavItem(
                    icon = painterResource(R.drawable.ic_home),
                    label = "Home",
                    selected = selectedTab == 0,
                    onClick = { onTabSelected(0) }
                )
                IOSNavItem(
                    icon = painterResource(R.drawable.ic_settings_hex),
                    label = "Settings",
                    selected = selectedTab == 1,
                    onClick = { onTabSelected(1) }
                )
            }
        }
    }
}

@Composable
private fun IOSNavItem(
    icon: androidx.compose.ui.graphics.painter.Painter,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) Color(0xFF00E676) else Color.Transparent,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "bgColor"
    )
    
    val iconColor by animateColorAsState(
        targetValue = if (selected) Color.Black else Color.White.copy(alpha = 0.6f),
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "iconColor"
    )
    
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
            AnimatedVisibility(
                visible = selected,
                enter = fadeIn(tween(300)) + expandHorizontally(tween(300)),
                exit = fadeOut(tween(200)) + shrinkHorizontally(tween(200))
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.W600,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
private fun IOSGradientMesh(status: AppStatus) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Dominant black gradient with subtle green hints
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D0D0D),
                        Color(0xFF050505),
                        Color(0xFF000000)
                    )
                )
            )
        )
    }
}

@Composable
private fun IOSStatusHeader(status: AppStatus, mode: Mode) {
    AnimatedContent(
        targetState = status to mode,
        transitionSpec = {
            (fadeIn(tween(500)) + slideInVertically { -it / 4 })
                .togetherWith(fadeOut(tween(300)) + slideOutVertically { it / 4 })
        },
        label = "header"
    ) { (currentStatus, currentMode) ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = when (currentStatus) {
                    AppStatus.Halted -> "Not Protected"
                    AppStatus.Running -> "You're Protected"
                },
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.W700,
                color = Color.White,
                letterSpacing = (-0.5).sp
            )
            
            // Glassmorphic pill
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1A1A1A).copy(alpha = 0.8f),
                modifier = Modifier.blur(0.5.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(8.dp).background(
                            if (currentStatus == AppStatus.Running) Color(0xFF00E676) else Color(0xFF9E9E9E),
                            CircleShape
                        )
                    )
                    Text(
                        when (currentMode) {
                            Mode.VPN -> "VPN Mode"
                            Mode.Proxy -> "Proxy Mode"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.W600,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@Composable
private fun IOSHeroVisual(status: AppStatus) {
    val scale by animateFloatAsState(
        targetValue = if (status == AppStatus.Running) 1f else 0.92f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "scale"
    )
    
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = if (status == AppStatus.Running) 0.8f else 0.4f,
        animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.scale(scale).padding(vertical = 20.dp)
    ) {
        // Outer glow rings - subtle green only when connected
        Box(
            modifier = Modifier.size(280.dp).blur(80.dp)
                .background(
                    if (status == AppStatus.Running) Color(0xFF00E676).copy(alpha = glowAlpha * 0.15f)
                    else Color(0xFF2A2A2A).copy(alpha = glowAlpha * 0.2f),
                    CircleShape
                )
        )
        
        Box(
            modifier = Modifier.size(220.dp).blur(60.dp)
                .background(
                    if (status == AppStatus.Running) Color(0xFF00E676).copy(alpha = glowAlpha * 0.2f)
                    else Color(0xFF1F1F1F).copy(alpha = glowAlpha * 0.3f),
                    CircleShape
                )
        )
        
        // Main glassmorphic circle
        Surface(
            modifier = Modifier.size(180.dp),
            shape = CircleShape,
            color = Color(0xFF1A1A1A).copy(alpha = 0.5f),
            tonalElevation = 0.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF2A2A2A).copy(alpha = 0.8f),
                                Color(0xFF1A1A1A).copy(alpha = 0.4f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (status == AppStatus.Running) Icons.Filled.Lock else Icons.Outlined.LockOpen,
                    null,
                    modifier = Modifier.size(80.dp),
                    tint = if (status == AppStatus.Running) Color(0xFF00E676) else Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun IOSStatsCards(connectionTime: Long, mode: Mode) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IOSGlassCard(
            icon = Icons.Filled.Timer,
            label = "Uptime",
            value = formatTime(connectionTime),
            modifier = Modifier.weight(1f)
        )
        IOSGlassCard(
            icon = if (mode == Mode.VPN) Icons.Filled.VpnKey else Icons.Filled.Hub,
            label = "Connection",
            value = if (mode == Mode.VPN) "VPN" else "Proxy",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun IOSGlassCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF1A1A1A),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, tint = Color(0xFF00E676), modifier = Modifier.size(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.W700,
                    color = Color.White
                )
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.W500
                )
            }
        }
    }
}

@Composable
private fun IOSConnectionButton(status: AppStatus, mode: Mode, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "scale"
    )
    
    val buttonText = when (status) {
        AppStatus.Halted -> "Connect Now"
        AppStatus.Running -> "Disconnect"
    }
    
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(60.dp).scale(scale),
        shape = RoundedCornerShape(30.dp),
        color = if (status == AppStatus.Running) {
            Color(0xFFFF3B30)
        } else {
            Color(0xFF00E676)
        },
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                buttonText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.W600,
                color = Color.White,
                letterSpacing = 0.2.sp
            )
        }
    }
}

@Composable
private fun IOSProxyCard(address: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF1A1A1A),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Proxy Address",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    fontWeight = FontWeight.W500
                )
                Text(
                    address,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.W600,
                    color = Color.White
                )
            }
            Box(
                modifier = Modifier.size(10.dp)
                    .background(Color(0xFF00E676), CircleShape)
            )
        }
    }
}

@Composable
private fun IOSQuickActions(onReconnect: () -> Unit, onChangeMode: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            onClick = onReconnect,
            modifier = Modifier.weight(1f).height(54.dp),
            shape = RoundedCornerShape(27.dp),
            color = Color(0xFF1A1A1A),
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Refresh, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Reconnect", fontWeight = FontWeight.W600, color = Color.White)
            }
        }
        Surface(
            onClick = onChangeMode,
            modifier = Modifier.weight(1f).height(54.dp),
            shape = RoundedCornerShape(27.dp),
            color = Color(0xFF1A1A1A),
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.SwapHoriz, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Switch", fontWeight = FontWeight.W600, color = Color.White)
            }
        }
    }
}

private fun formatTime(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) String.format("%02d:%02d:%02d", hours, minutes, secs)
           else String.format("%02d:%02d", minutes, secs)
}


@Composable
private fun SettingsContent(onNavigateToSettings: () -> Unit, onSaveLogs: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getPreferences()
    
    var selectedMode by remember { mutableStateOf(prefs.mode()) }
    var dnsIp by remember { mutableStateOf(prefs.getString("dns_ip", "1.1.1.1") ?: "1.1.1.1") }
    var ipv6Enabled by remember { mutableStateOf(prefs.getBoolean("ipv6_enable", false)) }
    var cmdSettingsEnabled by remember { mutableStateOf(prefs.getBoolean("byedpi_enable_cmd_settings", false)) }
    
    var showModeDialog by remember { mutableStateOf(false) }
    var showDnsDialog by remember { mutableStateOf(false) }
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Network
        IOSSettingsGroup(title = "Network") {
            IOSSettingItem(
                iconRes = R.drawable.ic_wifi,
                title = "Connection Mode",
                value = when(selectedMode) {
                    Mode.VPN -> "VPN"
                    Mode.Proxy -> "Proxy"
                },
                onClick = { showModeDialog = true }
            )
            
            IOSSettingItem(
                iconRes = R.drawable.ic_server,
                title = "DNS Server",
                value = dnsIp,
                onClick = { showDnsDialog = true }
            )
            
            IOSSettingToggle(
                iconRes = R.drawable.ic_cloudflare,
                title = "IPv6 Support",
                checked = ipv6Enabled,
                onCheckedChange = {
                    ipv6Enabled = it
                    prefs.edit().putBoolean("ipv6_enable", it).apply()
                }
            )
        }
        
        // Advanced
        IOSSettingsGroup(title = "Advanced") {
            IOSSettingToggle(
                iconRes = R.drawable.ic_terminal,
                title = "Command Settings",
                checked = cmdSettingsEnabled,
                onCheckedChange = {
                    cmdSettingsEnabled = it
                    prefs.edit().putBoolean("byedpi_enable_cmd_settings", it).apply()
                }
            )
        }
        
        // About
        IOSSettingsGroup(title = "About") {
            IOSSettingItem(
                iconRes = R.drawable.ic_info_circle,
                title = "Version",
                value = BuildConfig.VERSION_NAME,
                onClick = {}
            )
            
            IOSSettingItem(
                iconRes = R.drawable.ic_telegram,
                title = "Developer",
                value = "@storm_inc",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/storm_inc"))
                    context.startActivity(intent)
                }
            )
            
            IOSSettingItem(
                iconRes = R.drawable.ic_info_circle,
                title = "Save Logs",
                value = "",
                onClick = onSaveLogs
            )
        }
    }
    
    // Dialogs
    if (showModeDialog) {
        IOSDialog(
            title = "Connection Mode",
            onDismiss = { showModeDialog = false },
            options = listOf("VPN" to "vpn", "Proxy" to "proxy"),
            selectedValue = if (selectedMode == Mode.VPN) "vpn" else "proxy",
            onSelect = { value ->
                selectedMode = if (value == "vpn") Mode.VPN else Mode.Proxy
                prefs.edit().putString("byedpi_mode", value).apply()
                showModeDialog = false
            }
        )
    }
    
    if (showDnsDialog) {
        var tempDns by remember { mutableStateOf(dnsIp) }
        AlertDialog(
            onDismissRequest = { showDnsDialog = false },
            title = { Text("DNS Server", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = tempDns,
                    onValueChange = { tempDns = it },
                    label = { Text("IP Address") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White.copy(alpha = 0.8f)
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    dnsIp = tempDns
                    prefs.edit().putString("dns_ip", tempDns).apply()
                    showDnsDialog = false
                }) {
                    Text("Save", color = Color(0xFF00E676))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDnsDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = Color(0xFF1A1A1A)
        )
    }
}

@Composable
private fun IOSSettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.W600,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            letterSpacing = 1.sp
        )
        
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF1A1A1A),
            tonalElevation = 0.dp
        ) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun IOSSettingItem(
    iconRes: Int,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF00E676).copy(alpha = 0.2f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.W500,
                    color = Color.White
                )
            }
            
            if (value.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                    Icon(
                        Icons.Default.ChevronRight,
                        null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun IOSSettingToggle(
    iconRes: Int,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF00E676).copy(alpha = 0.2f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.W500,
                color = Color.White
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF00E676),
                uncheckedThumbColor = Color.White.copy(alpha = 0.8f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
            )
        )
    }
}

@Composable
private fun IOSDialog(
    title: String,
    onDismiss: () -> Unit,
    options: List<Pair<String, String>>,
    selectedValue: String,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = Color.White, fontWeight = FontWeight.W600) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { (label, value) ->
                    Surface(
                        onClick = { onSelect(value) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedValue == value) Color(0xFF00E676).copy(alpha = 0.2f) else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                label,
                                color = Color.White,
                                fontWeight = if (selectedValue == value) FontWeight.W600 else FontWeight.W400
                            )
                            if (selectedValue == value) {
                                Icon(Icons.Default.Check, null, tint = Color(0xFF00E676), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        containerColor = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(24.dp)
    )
}
