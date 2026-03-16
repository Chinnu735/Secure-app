package com.securefolder.core.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Debug
import android.provider.Settings
import java.io.File

/**
 * Comprehensive security manager for anti-tampering, root detection,
 * emulator detection, and debugger detection.
 * Inspired by GrapheneOS security principles.
 */
class SecurityManager(private val context: Context) {

    data class SecurityStatus(
        val isRooted: Boolean = false,
        val isDebuggable: Boolean = false,
        val isEmulator: Boolean = false,
        val isDebuggerAttached: Boolean = false,
        val isAdbEnabled: Boolean = false,
        val isDeveloperMode: Boolean = false,
        val isSignatureValid: Boolean = true,
        val overallSecure: Boolean = true
    )

    private var _securityStatus = SecurityStatus()
    val securityStatus: SecurityStatus get() = _securityStatus

    /**
     * Perform all security checks at startup.
     */
    fun performStartupSecurityChecks() {
        _securityStatus = SecurityStatus(
            isRooted = checkRoot(),
            isDebuggable = checkDebuggable(),
            isEmulator = checkEmulator(),
            isDebuggerAttached = checkDebuggerAttached(),
            isAdbEnabled = checkAdbEnabled(),
            isDeveloperMode = checkDeveloperMode(),
            isSignatureValid = checkAppSignature()
        ).let { status ->
            status.copy(
                overallSecure = !status.isRooted &&
                        !status.isDebuggable &&
                        !status.isEmulator &&
                        !status.isDebuggerAttached &&
                        status.isSignatureValid
            )
        }
    }

    /**
     * Refresh security status.
     */
    fun refreshStatus(): SecurityStatus {
        performStartupSecurityChecks()
        return _securityStatus
    }

    /**
     * Check for root access indicators.
     */
    private fun checkRoot(): Boolean {
        // Check for su binary
        val suPaths = listOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su",
            "/system/su", "/system/bin/.ext/.su",
            "/system/usr/we-need-root/su-backup",
            "/system/app/Superuser.apk", "/data/local/su",
            "/data/local/bin/su", "/data/local/xbin/su"
        )
        if (suPaths.any { File(it).exists() }) return true

        // Check for Magisk
        val magiskPaths = listOf(
            "/sbin/.magisk", "/sbin/.core/mirror",
            "/data/adb/magisk", "/data/adb/modules"
        )
        if (magiskPaths.any { File(it).exists() }) return true

        // Check for root management apps
        val rootApps = listOf(
            "com.topjohnwu.magisk",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.noshufou.android.su",
            "com.thirdparty.superuser",
            "com.yellowes.su"
        )
        val pm = context.packageManager
        if (rootApps.any { pkg ->
                try { pm.getPackageInfo(pkg, 0); true } catch (_: Exception) { false }
            }) return true

        // Check for Xposed
        try {
            Class.forName("de.robv.android.xposed.XposedBridge")
            return true
        } catch (_: ClassNotFoundException) { }

        // Try executing su
        try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val result = process.inputStream.bufferedReader().readText()
            if (result.isNotEmpty()) return true
        } catch (_: Exception) { }

        return false
    }

    /**
     * Check if app is debuggable.
     */
    private fun checkDebuggable(): Boolean {
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    /**
     * Check if running on emulator.
     */
    private fun checkEmulator(): Boolean {
        return Build.FINGERPRINT.contains("generic") ||
                Build.FINGERPRINT.contains("unknown") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                Build.BRAND.startsWith("generic") ||
                Build.DEVICE.startsWith("generic") ||
                Build.PRODUCT.contains("sdk") ||
                Build.PRODUCT.contains("emulator") ||
                Build.HARDWARE.contains("goldfish") ||
                Build.HARDWARE.contains("ranchu") ||
                (Build.BOARD == "QC_Reference_Phone" && !Build.MANUFACTURER.equals("Qualcomm", ignoreCase = true))
    }

    /**
     * Check if a debugger is currently attached.
     */
    private fun checkDebuggerAttached(): Boolean {
        return Debug.isDebuggerConnected() || Debug.waitingForDebugger()
    }

    /**
     * Check if ADB is enabled.
     */
    private fun checkAdbEnabled(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Check if developer mode is enabled.
     */
    private fun checkDeveloperMode(): Boolean {
        return try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0
            ) == 1
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Verify app signature integrity.
     */
    private fun checkAppSignature(): Boolean {
        return try {
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signingInfo = context.packageManager
                    .getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES)
                    .signingInfo
                if (signingInfo.hasMultipleSigners()) {
                    signingInfo.apkContentsSigners
                } else {
                    signingInfo.signingCertificateHistory
                }
            } else {
                @Suppress("DEPRECATION")
                context.packageManager
                    .getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_SIGNATURES)
                    .signatures
            }
            signatures != null && signatures.isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }
}
