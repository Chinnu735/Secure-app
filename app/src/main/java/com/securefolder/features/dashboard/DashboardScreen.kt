package com.securefolder.features.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securefolder.SecureFolderApp
import com.securefolder.core.security.DeviceBindingManager
import com.securefolder.ui.components.FeatureCard
import com.securefolder.ui.components.SecurityStatusCard
import com.securefolder.ui.theme.*

/**
 * Main dashboard showing security status, Knox info, and feature access.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToVault: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToBrowser: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val securityManager = SecureFolderApp.instance.securityManager
    val securityStatus = remember { securityManager.refreshStatus() }
    val deviceIntegrity = remember { DeviceBindingManager.verifyFullIntegrity(context) }

    val securityDetails = remember {
        listOf(
            "Root Access" to !securityStatus.isRooted,
            "Debugger" to !securityStatus.isDebuggerAttached,
            "Emulator" to !securityStatus.isEmulator,
            "App Integrity" to securityStatus.isSignatureValid,
            "ADB" to !securityStatus.isAdbEnabled,
            "Device Bound" to deviceIntegrity.isDeviceBound,
            "Knox Security" to deviceIntegrity.knoxStatus.isAvailable
        )
    }

    // Pulsing animation for security shield
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Scaffold(
        containerColor = PrimaryDark,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = "🛡️", fontSize = 24.sp)
                        Text(
                            text = "Secure Folder",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryDark
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Security Status Card
            SecurityStatusCard(
                isSecure = securityStatus.overallSecure && deviceIntegrity.isDeviceBound,
                securityDetails = securityDetails
            )

            // Samsung Knox Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (deviceIntegrity.knoxStatus.isAvailable)
                        CardDark else Color(0xFF2D1B1B)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFF1428A0).copy(alpha = pulseAlpha),
                                            CyanAccent.copy(alpha = pulseAlpha)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🔷", fontSize = 20.sp)
                        }
                        Column {
                            Text(
                                text = "Samsung Knox Security",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (deviceIntegrity.knoxStatus.isAvailable)
                                    "Knox ${deviceIntegrity.knoxStatus.version} • Active"
                                else "Knox status: checking...",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (deviceIntegrity.knoxStatus.isAvailable)
                                    CyanAccent else OrangeWarning
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Knox details
                    KnoxDetailRow("Verified Boot", deviceIntegrity.knoxStatus.verifiedBootState)
                    KnoxDetailRow("SELinux", deviceIntegrity.knoxStatus.seLinuxStatus)
                    KnoxDetailRow("Warranty", deviceIntegrity.knoxStatus.warrantyBit)
                    KnoxDetailRow("Security Patch", deviceIntegrity.securityPatchLevel)
                }
            }

            // Device Binding Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(GreenSecure, TealAccent)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📱", fontSize = 18.sp)
                    }
                    Column {
                        Text(
                            text = "Device-Locked",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${deviceIntegrity.deviceModel} • ${deviceIntegrity.androidVersion}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GreenSecure
                        )
                        Text(
                            text = "This app only works on YOUR device",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextTertiary
                        )
                    }
                }
            }

            // Hardware Security Badge
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        CyanAccent.copy(alpha = pulseAlpha),
                                        TealAccent.copy(alpha = pulseAlpha)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🔑", fontSize = 18.sp)
                    }
                    Column {
                        Text(
                            text = "Hardware-Backed Encryption",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "AES-256-GCM • Android Keystore • " +
                                    if (deviceIntegrity.isHardwareBackedKeystore) "StrongBox" else "TEE",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanAccent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Section header
            Text(
                text = "SECURE FEATURES",
                style = MaterialTheme.typography.labelMedium,
                color = TextTertiary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp)
            )

            // Feature Cards
            FeatureCard(
                title = "Encrypted Vault",
                subtitle = "Photos, videos, documents — all encrypted",
                icon = "🔒",
                gradientColors = listOf(CyanAccent, Color(0xFF3B82F6)),
                onClick = onNavigateToVault
            )

            FeatureCard(
                title = "Secure Notes",
                subtitle = "E2E encrypted private notes",
                icon = "📝",
                gradientColors = listOf(TealAccent, GreenSecure),
                onClick = onNavigateToNotes
            )

            FeatureCard(
                title = "Secure Browser",
                subtitle = "Private browsing — no history, no tracking",
                icon = "🌐",
                gradientColors = listOf(Color(0xFF8B5CF6), Color(0xFFEC4899)),
                onClick = onNavigateToBrowser
            )

            FeatureCard(
                title = "Settings & Security",
                subtitle = "Stealth mode, self-destruct, intruder detection",
                icon = "⚙️",
                gradientColors = listOf(OrangeWarning, RedAlert),
                onClick = onNavigateToSettings
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Footer
            Text(
                text = "Device-locked • Knox secured • On-device only • Zero telemetry",
                style = MaterialTheme.typography.labelSmall,
                color = TextTertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun KnoxDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextTertiary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = when (value.lowercase()) {
                "green", "enforcing", "intact" -> GreenSecure
                "orange", "tripped" -> RedAlert
                else -> TextSecondary
            },
            fontWeight = FontWeight.Medium
        )
    }
}
