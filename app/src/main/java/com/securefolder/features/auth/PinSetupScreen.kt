package com.securefolder.features.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securefolder.SecureFolderApp
import com.securefolder.core.crypto.SecurePreferences
import com.securefolder.core.security.DeviceBindingManager
import com.securefolder.ui.components.PinDots
import com.securefolder.ui.components.PinKeypad
import com.securefolder.ui.theme.*
import java.security.MessageDigest
import java.security.SecureRandom
import android.util.Base64

/**
 * First-time PIN setup screen.
 * Creates a 6-digit PIN with PBKDF2-like hashing stored in encrypted prefs.
 */
@Composable
fun PinSetupScreen(onSetupComplete: () -> Unit) {
    val context = SecureFolderApp.instance
    val securePrefs = remember { SecurePreferences(context) }

    var step by remember { mutableIntStateOf(0) } // 0 = create, 1 = confirm
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    val pinLength = 6

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

            // Shield icon
            Text(
                text = "🛡️",
                fontSize = 64.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (step == 0) "Create Your PIN" else "Confirm Your PIN",
                style = MaterialTheme.typography.headlineLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (step == 0)
                    "Choose a 6-digit PIN to protect your vault"
                else
                    "Enter your PIN again to confirm",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // PIN dots
            PinDots(
                pinLength = pinLength,
                enteredLength = if (step == 0) pin.length else confirmPin.length,
                isError = isError
            )

            // Error message
            errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = RedAlert,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // PIN keypad
            PinKeypad(
                onDigitClick = { digit ->
                    isError = false
                    errorMessage = null
                    if (step == 0) {
                        if (pin.length < pinLength) {
                            pin += digit
                            if (pin.length == pinLength) {
                                step = 1
                            }
                        }
                    } else {
                        if (confirmPin.length < pinLength) {
                            confirmPin += digit
                            if (confirmPin.length == pinLength) {
                                if (confirmPin == pin) {
                                    // Save PIN hash
                                    val salt = generateSalt()
                                    val hash = hashPin(confirmPin, salt)
                                    securePrefs.pinSalt = salt
                                    securePrefs.pinHash = hash
                                    securePrefs.isSetupComplete = true
                                    // BIND TO THIS DEVICE — app will only work on this Samsung M51
                                    DeviceBindingManager.bindToDevice(context)
                                    onSetupComplete()
                                } else {
                                    isError = true
                                    errorMessage = "PINs don't match. Try again."
                                    confirmPin = ""
                                    pin = ""
                                    step = 0
                                }
                            }
                        }
                    }
                },
                onDeleteClick = {
                    isError = false
                    errorMessage = null
                    if (step == 0 && pin.isNotEmpty()) {
                        pin = pin.dropLast(1)
                    } else if (step == 1 && confirmPin.isNotEmpty()) {
                        confirmPin = confirmPin.dropLast(1)
                    }
                }
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

/**
 * Generate a cryptographically secure salt.
 */
private fun generateSalt(): String {
    val salt = ByteArray(32)
    SecureRandom().nextBytes(salt)
    return Base64.encodeToString(salt, Base64.NO_WRAP)
}

/**
 * Hash PIN with salt using SHA-256 (multiple iterations for key stretching).
 */
fun hashPin(pin: String, salt: String): String {
    val saltBytes = Base64.decode(salt, Base64.NO_WRAP)
    var data = (pin + String(saltBytes, Charsets.UTF_8)).toByteArray(Charsets.UTF_8)
    val digest = MessageDigest.getInstance("SHA-256")

    // Key stretching: 10000 iterations
    repeat(10000) {
        data = digest.digest(data)
    }

    return Base64.encodeToString(data, Base64.NO_WRAP)
}
