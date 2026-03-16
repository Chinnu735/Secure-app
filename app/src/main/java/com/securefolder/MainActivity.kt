package com.securefolder

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import com.securefolder.core.crypto.SecurePreferences
import com.securefolder.core.security.DeviceBindingManager
import com.securefolder.core.security.ScreenProtection
import com.securefolder.ui.navigation.Routes
import com.securefolder.ui.navigation.SecureFolderNavGraph
import com.securefolder.ui.theme.*

/**
 * Main Activity — single activity architecture.
 * Enables FLAG_SECURE to block screenshots and screen recording.
 * Verifies device binding before allowing access.
 * Routes to PIN setup or lock screen based on setup state.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable screenshot/screen recording protection
        ScreenProtection.enableScreenProtection(this)

        // Prevent showing content in recent apps
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

        val securePrefs = SecurePreferences(this)

        // DEVICE BINDING CHECK — if app is already set up, verify this is the authorized device
        if (securePrefs.isSetupComplete && !DeviceBindingManager.isAuthorizedDevice(this)) {
            // UNAUTHORIZED DEVICE — show blocked screen, do NOT allow any access
            setContent {
                SecureFolderTheme {
                    UnauthorizedDeviceScreen()
                }
            }
            return
        }

        // Determine start destination
        val startDestination = when {
            !securePrefs.isSetupComplete -> Routes.PIN_SETUP
            else -> Routes.LOCK_SCREEN
        }

        setContent {
            SecureFolderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = PrimaryDark
                ) {
                    val navController = rememberNavController()
                    SecureFolderNavGraph(
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Could trigger auto-lock here based on settings
    }

    override fun onStop() {
        super.onStop()
        // Clear clipboard for security
        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            clipboard.clearPrimaryClip()
        }
    }
}

/**
 * Screen shown when app detects it's running on an unauthorized device.
 * No way to bypass — keys won't decrypt since they're hardware-bound.
 */
@Composable
private fun UnauthorizedDeviceScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PrimaryDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(48.dp)
        ) {
            Text("🚫", fontSize = 80.sp)
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Unauthorized Device",
                style = MaterialTheme.typography.headlineLarge,
                color = RedAlert,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "This app is bound to a specific device and cannot be used on this one.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "All encryption keys are hardware-bound and cannot be transferred.",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = CardDark)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🔒", fontSize = 20.sp)
                    Text(
                        text = "Device-Locked Security",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyanAccent,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Samsung Knox • Hardware Keystore",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextTertiary
                    )
                }
            }
        }
    }
}
