package com.securefolder.features.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.securefolder.SecureFolderApp
import com.securefolder.core.biometric.BiometricHelper
import com.securefolder.core.crypto.SecurePreferences
import com.securefolder.ui.components.PinDots
import com.securefolder.ui.components.PinKeypad
import com.securefolder.ui.theme.*

/**
 * Lock screen with PIN entry and biometric authentication.
 * Tracks failed attempts for intruder detection and self-destruct.
 */
@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val context = SecureFolderApp.instance
    val securePrefs = remember { SecurePreferences(context) }
    val biometricHelper = remember { BiometricHelper(context) }
    val activityContext = LocalContext.current

    var pin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSelfDestructWarning by remember { mutableStateOf(false) }
    val pinLength = 6

    val failedAttempts = securePrefs.failedAttempts
    val maxAttempts = securePrefs.maxAttempts
    val remainingAttempts = maxAttempts - failedAttempts

    // Show biometric prompt on launch if enabled
    LaunchedEffect(Unit) {
        if (securePrefs.isBiometricEnabled && biometricHelper.isBiometricAvailable()) {
            val activity = activityContext as? FragmentActivity
            activity?.let {
                biometricHelper.authenticate(
                    activity = it,
                    onSuccess = {
                        securePrefs.resetFailedAttempts()
                        securePrefs.lastUnlockTimestamp = System.currentTimeMillis()
                        onUnlocked()
                    },
                    onError = { _, _ -> },
                    onFailed = { }
                )
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PrimaryDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // Lock icon
            Text(
                text = "🔐",
                fontSize = 64.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Secure Folder",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Enter your PIN to unlock",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // PIN dots
            PinDots(
                pinLength = pinLength,
                enteredLength = pin.length,
                isError = isError
            )

            // Error / warning messages
            Spacer(modifier = Modifier.height(16.dp))

            errorMessage?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = RedAlert,
                    textAlign = TextAlign.Center
                )
            }

            if (failedAttempts > 0 && securePrefs.isSelfDestructEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$remainingAttempts attempts remaining",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (remainingAttempts <= 3) RedAlert else OrangeWarning,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // PIN keypad
            PinKeypad(
                onDigitClick = { digit ->
                    isError = false
                    errorMessage = null
                    if (pin.length < pinLength) {
                        pin += digit
                        if (pin.length == pinLength) {
                            // Verify PIN
                            val storedHash = securePrefs.pinHash
                            val storedSalt = securePrefs.pinSalt
                            if (storedHash != null && storedSalt != null) {
                                val inputHash = hashPin(pin, storedSalt)
                                if (inputHash == storedHash) {
                                    // Correct PIN
                                    securePrefs.resetFailedAttempts()
                                    securePrefs.lastUnlockTimestamp = System.currentTimeMillis()
                                    onUnlocked()
                                } else {
                                    // Wrong PIN
                                    securePrefs.failedAttempts = failedAttempts + 1
                                    isError = true
                                    pin = ""

                                    if (securePrefs.isSelfDestructEnabled &&
                                        securePrefs.failedAttempts >= maxAttempts
                                    ) {
                                        // SELF-DESTRUCT: Wipe all data
                                        performSelfDestruct(context)
                                    } else {
                                        errorMessage = "Incorrect PIN"

                                        // Intruder detection
                                        if (securePrefs.isIntruderDetectionEnabled &&
                                            securePrefs.failedAttempts >= 3
                                        ) {
                                            // Camera capture would happen here in production
                                            // intruderDetector.captureIntruder()
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                onDeleteClick = {
                    isError = false
                    errorMessage = null
                    if (pin.isNotEmpty()) {
                        pin = pin.dropLast(1)
                    }
                },
                onBiometricClick = if (securePrefs.isBiometricEnabled &&
                    biometricHelper.isBiometricAvailable()
                ) {
                    {
                        val activity = activityContext as? FragmentActivity
                        activity?.let {
                            biometricHelper.authenticate(
                                activity = it,
                                onSuccess = {
                                    securePrefs.resetFailedAttempts()
                                    securePrefs.lastUnlockTimestamp = System.currentTimeMillis()
                                    onUnlocked()
                                },
                                onError = { _, _ -> },
                                onFailed = {
                                    isError = true
                                    errorMessage = "Biometric failed"
                                }
                            )
                        }
                    }
                } else null
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

/**
 * Self-destruct: wipe all data, keys, and preferences.
 */
private fun performSelfDestruct(context: android.content.Context) {
    try {
        // Delete all encryption keys
        com.securefolder.core.crypto.KeyStoreManager.deleteAllKeys()

        // Clear encrypted preferences
        SecurePreferences(context).clearAll()

        // Delete all files in app storage
        context.filesDir.deleteRecursively()
        context.cacheDir.deleteRecursively()

        // Delete databases
        context.databaseList().forEach { dbName ->
            context.deleteDatabase(dbName)
        }

        // Force close app
        android.os.Process.killProcess(android.os.Process.myPid())
    } catch (e: Exception) {
        // Even if cleanup fails, kill process
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}
