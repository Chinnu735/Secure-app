package com.securefolder.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securefolder.ui.theme.*

/**
 * Custom PIN keypad with haptic feedback and animated interactions.
 */
@Composable
fun PinKeypad(
    onDigitClick: (String) -> Unit,
    onDeleteClick: () -> Unit,
    onBiometricClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val digits = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("bio", "0", "del")
    )

    Column(
        modifier = modifier.padding(horizontal = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        digits.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { key ->
                    when (key) {
                        "del" -> {
                            PinKey(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDeleteClick()
                                }
                            ) {
                                Icon(
                                    Icons.Default.Backspace,
                                    contentDescription = "Delete",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        "bio" -> {
                            if (onBiometricClick != null) {
                                PinKey(
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onBiometricClick()
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Fingerprint,
                                        contentDescription = "Biometric",
                                        tint = CyanAccent,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        else -> {
                            PinKey(
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDigitClick(key)
                                }
                            ) {
                                Text(
                                    text = key,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinKey(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(CardDark)
            .border(1.dp, GlassBorder, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, color = CyanAccent),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * PIN dots display showing entered digits.
 */
@Composable
fun PinDots(
    pinLength: Int,
    enteredLength: Int,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {
    val errorShake = remember { Animatable(0f) }

    LaunchedEffect(isError) {
        if (isError) {
            errorShake.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    (-10f) at 50
                    10f at 100
                    (-10f) at 150
                    10f at 200
                    (-5f) at 250
                    5f at 300
                    0f at 400
                }
            )
        }
    }

    Row(
        modifier = modifier.offset(x = errorShake.value.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(pinLength) { index ->
            val filled = index < enteredLength
            val dotColor = when {
                isError -> RedAlert
                filled -> CyanAccent
                else -> TextTertiary
            }

            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (filled) dotColor else Color.Transparent)
                    .border(2.dp, dotColor, CircleShape)
            )
        }
    }
}

/**
 * Security status card with glassmorphism effect.
 */
@Composable
fun SecurityStatusCard(
    isSecure: Boolean,
    securityDetails: List<Pair<String, Boolean>>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSecure) CardDark else Color(0xFF2D1B1B)
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                if (isSecure) listOf(GreenSecure, TealAccent)
                                else listOf(RedAlert, OrangeWarning)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isSecure) "🛡️" else "⚠️",
                        fontSize = 22.sp
                    )
                }
                Column {
                    Text(
                        text = if (isSecure) "All Systems Secure" else "Security Warning",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isSecure) GreenSecureLight else RedAlertLight,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isSecure) "Your data is protected" else "Potential threats detected",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            securityDetails.forEach { (label, secure) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Text(
                        text = if (secure) "✓ Secure" else "✗ Risk",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (secure) GreenSecure else RedAlert,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * Dashboard feature card with icon and count.
 */
@Composable
fun FeatureCard(
    title: String,
    subtitle: String,
    icon: String,
    count: Int = 0,
    gradientColors: List<Color> = listOf(CyanAccent, TealAccent),
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
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
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(gradientColors)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 24.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            if (count > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceVariantDark)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = CyanAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
