package com.securefolder.features.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securefolder.core.crypto.KeyStoreManager
import com.securefolder.core.crypto.SecurePreferences
import com.securefolder.ui.theme.*

/**
 * Settings screen with security controls:
 * Stealth mode, self-destruct, intruder detection, biometric, auto-lock, and panic wipe.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val securePrefs = remember { SecurePreferences(context) }

    var stealthMode by remember { mutableStateOf(securePrefs.isStealthModeEnabled) }
    var selfDestruct by remember { mutableStateOf(securePrefs.isSelfDestructEnabled) }
    var intruderDetection by remember { mutableStateOf(securePrefs.isIntruderDetectionEnabled) }
    var biometricUnlock by remember { mutableStateOf(securePrefs.isBiometricEnabled) }
    var maxAttempts by remember { mutableIntStateOf(securePrefs.maxAttempts) }
    var showPanicDialog by remember { mutableStateOf(false) }
    var showMaxAttemptsDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = PrimaryDark,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("⚙️", fontSize = 20.sp)
                        Text(
                            "Settings",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryDark)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Security section
            SectionHeader("SECURITY")

            // Biometric Unlock
            SettingToggle(
                icon = Icons.Default.Fingerprint,
                title = "Biometric Unlock",
                subtitle = "Use fingerprint or face to unlock",
                checked = biometricUnlock,
                accentColor = CyanAccent,
                onCheckedChange = {
                    biometricUnlock = it
                    securePrefs.isBiometricEnabled = it
                }
            )

            // Self-Destruct
            SettingToggle(
                icon = Icons.Default.DeleteForever,
                title = "Self-Destruct",
                subtitle = "Wipe all data after $maxAttempts failed attempts",
                checked = selfDestruct,
                accentColor = RedAlert,
                onCheckedChange = {
                    selfDestruct = it
                    securePrefs.isSelfDestructEnabled = it
                }
            )

            // Max Attempts
            if (selfDestruct) {
                SettingAction(
                    icon = Icons.Default.Pin,
                    title = "Maximum Attempts",
                    subtitle = "$maxAttempts attempts before self-destruct",
                    accentColor = OrangeWarning,
                    onClick = { showMaxAttemptsDialog = true }
                )
            }

            // Intruder Detection
            SettingToggle(
                icon = Icons.Default.CameraFront,
                title = "Intruder Detection",
                subtitle = "Capture photo on failed unlock attempts",
                checked = intruderDetection,
                accentColor = OrangeWarning,
                onCheckedChange = {
                    intruderDetection = it
                    securePrefs.isIntruderDetectionEnabled = it
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Privacy section
            SectionHeader("PRIVACY")

            // Stealth Mode
            SettingToggle(
                icon = Icons.Default.VisibilityOff,
                title = "Stealth Mode",
                subtitle = "Disguise as Calculator app",
                checked = stealthMode,
                accentColor = Color(0xFF8B5CF6),
                onCheckedChange = {
                    stealthMode = it
                    securePrefs.isStealthModeEnabled = it
                    toggleStealthMode(context, it)
                }
            )

            if (stealthMode) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceVariantDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "📱 Access via Dialer Code",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Dial *#*#7378#*#* to open Secure Folder",
                            style = MaterialTheme.typography.bodySmall,
                            color = CyanAccent
                        )
                    }
                }
            }

            // Screenshot protection info
            SettingInfo(
                icon = Icons.Default.Screenshot,
                title = "Screenshot Protection",
                subtitle = "Always ON — Screenshots & screen recording blocked",
                accentColor = GreenSecure
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Encryption section
            SectionHeader("ENCRYPTION")

            SettingInfo(
                icon = Icons.Default.Key,
                title = "Encryption Algorithm",
                subtitle = "AES-256-GCM with hardware-backed keys",
                accentColor = CyanAccent
            )

            SettingInfo(
                icon = Icons.Default.Security,
                title = "Key Storage",
                subtitle = if (KeyStoreManager.isStrongBoxAvailable())
                    "StrongBox (dedicated security chip)"
                else
                    "TEE (Trusted Execution Environment)",
                accentColor = GreenSecure
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Danger zone
            SectionHeader("DANGER ZONE")

            // Panic Wipe
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D1B1B)),
                onClick = { showPanicDialog = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(RedAlert, Color(0xFFDC2626)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Panic Wipe",
                            style = MaterialTheme.typography.titleMedium,
                            color = RedAlert,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Immediately destroy all data and encryption keys",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Version info
            Text(
                text = "Secure Folder v1.0.0",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = "100% on-device • Zero telemetry • Open source",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }

    // Panic Wipe confirmation dialog
    if (showPanicDialog) {
        AlertDialog(
            onDismissRequest = { showPanicDialog = false },
            title = {
                Text("⚠️ PANIC WIPE", color = RedAlert, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "This will PERMANENTLY DESTROY all encrypted data, notes, files, and encryption keys. This action CANNOT be undone.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        performPanicWipe(context)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = RedAlert)
                ) { Text("WIPE EVERYTHING", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showPanicDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // Max attempts dialog
    if (showMaxAttemptsDialog) {
        val attemptOptions = listOf(3, 5, 7, 10, 15, 20)
        AlertDialog(
            onDismissRequest = { showMaxAttemptsDialog = false },
            title = {
                Text(
                    "Maximum Attempts",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    attemptOptions.forEach { option ->
                        TextButton(
                            onClick = {
                                maxAttempts = option
                                securePrefs.maxAttempts = option
                                showMaxAttemptsDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "$option attempts",
                                color = if (option == maxAttempts) CyanAccent else TextSecondary,
                                fontWeight = if (option == maxAttempts) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            containerColor = SurfaceDark
        )
    }
}

// ---- Composable Helpers ----

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = TextTertiary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingToggle(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    accentColor: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = accentColor,
                    checkedTrackColor = accentColor.copy(alpha = 0.3f)
                )
            )
        }
    }
}

@Composable
private fun SettingAction(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextTertiary)
        }
    }
}

@Composable
private fun SettingInfo(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accentColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Medium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = accentColor)
            }
        }
    }
}

// ---- Utility Functions ----

private fun toggleStealthMode(context: Context, enable: Boolean) {
    val pm = context.packageManager

    // Disable real launcher activity
    pm.setComponentEnabledSetting(
        ComponentName(context, "com.securefolder.MainActivity"),
        if (enable) PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        else PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
        PackageManager.DONT_KILL_APP
    )

    // Enable calculator alias
    pm.setComponentEnabledSetting(
        ComponentName(context, "com.securefolder.CalculatorAlias"),
        if (enable) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        PackageManager.DONT_KILL_APP
    )
}

private fun performPanicWipe(context: Context) {
    try {
        KeyStoreManager.deleteAllKeys()
        SecurePreferences(context).clearAll()
        context.filesDir.deleteRecursively()
        context.cacheDir.deleteRecursively()
        context.databaseList().forEach { context.deleteDatabase(it) }
        android.os.Process.killProcess(android.os.Process.myPid())
    } catch (e: Exception) {
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}
