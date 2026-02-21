package io.github.dovecoteescapee.byedpi.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.dovecoteescapee.byedpi.BuildConfig
import io.github.dovecoteescapee.byedpi.R
import io.github.dovecoteescapee.byedpi.data.Mode
import io.github.dovecoteescapee.byedpi.fragments.MainSettingsFragment
import io.github.dovecoteescapee.byedpi.utility.getPreferences
import io.github.dovecoteescapee.byedpi.utility.mode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getPreferences()
    
    var selectedMode by remember { mutableStateOf(prefs.mode()) }
    var selectedTheme by remember { mutableStateOf(prefs.getString("app_theme", "system") ?: "system") }
    var dnsIp by remember { mutableStateOf(prefs.getString("dns_ip", "1.1.1.1") ?: "1.1.1.1") }
    var ipv6Enabled by remember { mutableStateOf(prefs.getBoolean("ipv6_enable", false)) }
    var cmdSettingsEnabled by remember { mutableStateOf(prefs.getBoolean("byedpi_enable_cmd_settings", false)) }
    
    var showThemeDialog by remember { mutableStateOf(false) }
    var showModeDialog by remember { mutableStateOf(false) }
    var showDnsDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Same dark gradient background
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0A1929), Color(0xFF001E3C), Color(0xFF000000))
                )
            )
        )
        
        Column(modifier = Modifier.fillMaxSize()) {
            // iOS-style top bar
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White.copy(alpha = 0.9f))
                }
                Text(
                    "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.W600,
                    color = Color.White.copy(alpha = 0.95f),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(Modifier.height(8.dp))
                
                // General
                IOSSettingsGroup(title = "General") {
                    IOSSettingItem(
                        icon = Icons.Default.Palette,
                        title = "Theme",
                        value = when(selectedTheme) {
                            "system" -> "System"
                            "light" -> "Light"
                            "dark" -> "Dark"
                            else -> "System"
                        },
                        onClick = { showThemeDialog = true }
                    )
                    
                    IOSSettingItem(
                        icon = Icons.Default.Wifi,
                        title = "Connection Mode",
                        value = when(selectedMode) {
                            Mode.VPN -> "VPN"
                            Mode.Proxy -> "Proxy"
                        },
                        onClick = { showModeDialog = true }
                    )
                }
                
                // Network
                IOSSettingsGroup(title = "Network") {
                    IOSSettingItem(
                        icon = Icons.Default.Dns,
                        title = "DNS Server",
                        value = dnsIp,
                        onClick = { showDnsDialog = true }
                    )
                    
                    IOSSettingToggle(
                        icon = Icons.Default.Language,
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
                        icon = Icons.Default.Terminal,
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
                        icon = Icons.Default.Info,
                        title = "Version",
                        value = BuildConfig.VERSION_NAME,
                        onClick = {}
                    )
                    
                    IOSSettingItem(
                        icon = Icons.Default.Code,
                        title = "Developer",
                        value = "@storm_inc",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/storm_inc"))
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
    
    // Dialogs
    if (showThemeDialog) {
        IOSDialog(
            title = "Theme",
            onDismiss = { showThemeDialog = false },
            options = listOf("System" to "system", "Light" to "light", "Dark" to "dark"),
            selectedValue = selectedTheme,
            onSelect = { value ->
                selectedTheme = value
                prefs.edit().putString("app_theme", value).apply()
                MainSettingsFragment.setTheme(value)
                showThemeDialog = false
            }
        )
    }
    
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
                    Text("Save", color = Color(0xFF00E5FF))
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
            color = Color.White.copy(alpha = 0.08f),
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
    icon: ImageVector,
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
                    color = Color(0xFF00E5FF).copy(alpha = 0.2f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                    }
                }
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.W500,
                    color = Color.White
                )
            }
            
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

@Composable
private fun IOSSettingToggle(
    icon: ImageVector,
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
                color = Color(0xFF00E5FF).copy(alpha = 0.2f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
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
                        color = if (selectedValue == value) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color.Transparent
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
                                Icon(Icons.Default.Check, null, tint = Color(0xFF00E5FF), modifier = Modifier.size(20.dp))
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
