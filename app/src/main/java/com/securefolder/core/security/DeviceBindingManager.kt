package com.securefolder.core.security

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import com.securefolder.core.crypto.CryptoManager
import com.securefolder.core.crypto.KeyStoreManager
import java.io.File
import java.security.MessageDigest

/**
 * Device Binding — Locks app to a specific physical device.
 *
 * On first run, generates a unique device fingerprint from hardware identifiers
 * and stores it encrypted. On every subsequent launch, verifies the fingerprint
 * matches — if it doesn't, the app refuses to open. This makes the app unusable
 * if copied to any other device.
 *
 * Samsung M51 Knox-aware: Uses Samsung Knox hardware identifiers when available.
 */
object DeviceBindingManager {

    private const val BINDING_FILE = "device_binding.enc"
    private const val KNOX_ATTESTATION_FILE = "knox_attestation.enc"

    /**
     * Checks if this device is the authorized device.
     * Returns true if first run (binding will be created) or device matches.
     */
    fun isAuthorizedDevice(context: Context): Boolean {
        val bindingFile = File(context.filesDir, BINDING_FILE)

        if (!bindingFile.exists()) {
            // First run — bind to this device
            return true
        }

        return try {
            val storedFingerprint = CryptoManager.decryptText(bindingFile.readBytes())
            val currentFingerprint = generateDeviceFingerprint(context)
            storedFingerprint == currentFingerprint
        } catch (e: Exception) {
            // If decryption fails (different device, different keys), deny access
            false
        }
    }

    /**
     * Bind the app to this specific device.
     * Call this on first successful PIN setup.
     */
    fun bindToDevice(context: Context) {
        val fingerprint = generateDeviceFingerprint(context)
        val encrypted = CryptoManager.encryptText(fingerprint)
        File(context.filesDir, BINDING_FILE).writeBytes(encrypted)

        // Also store Knox attestation if available
        storeKnoxAttestation(context)
    }

    /**
     * Verify device binding and Knox integrity.
     */
    fun verifyFullIntegrity(context: Context): DeviceIntegrityResult {
        val isDeviceBound = isAuthorizedDevice(context)
        val knoxStatus = checkKnoxStatus(context)
        val isHardwareKeystore = KeyStoreManager.isStrongBoxAvailable()

        return DeviceIntegrityResult(
            isDeviceBound = isDeviceBound,
            knoxStatus = knoxStatus,
            isHardwareBackedKeystore = isHardwareKeystore,
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            securityPatchLevel = Build.VERSION.SECURITY_PATCH,
            kernelVersion = System.getProperty("os.version") ?: "unknown"
        )
    }

    /**
     * Generate a unique device fingerprint using multiple hardware identifiers.
     * This fingerprint is unique per device and cannot be replicated.
     */
    @SuppressLint("HardwareIds")
    private fun generateDeviceFingerprint(context: Context): String {
        val components = StringBuilder()

        // 1. Android ID (unique per app installation per device)
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        components.append("AID:$androidId|")

        // 2. Build fingerprint (OS + device specific)
        components.append("BFP:${Build.FINGERPRINT}|")

        // 3. Hardware identifiers
        components.append("BOARD:${Build.BOARD}|")
        components.append("BRAND:${Build.BRAND}|")
        components.append("DEVICE:${Build.DEVICE}|")
        components.append("HARDWARE:${Build.HARDWARE}|")
        components.append("MANUFACTURER:${Build.MANUFACTURER}|")
        components.append("MODEL:${Build.MODEL}|")
        components.append("PRODUCT:${Build.PRODUCT}|")

        // 4. SoC info (unique hardware)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            components.append("SOC:${Build.SOC_MANUFACTURER}:${Build.SOC_MODEL}|")
        }

        // 5. Display info
        components.append("DISPLAY:${Build.DISPLAY}|")

        // 6. Bootloader version
        components.append("BOOTLOADER:${Build.BOOTLOADER}|")

        // 7. Radio version (baseband — unique per device unit)
        val radioVersion = Build.getRadioVersion()
        if (radioVersion != null) {
            components.append("RADIO:$radioVersion|")
        }

        // 8. Samsung Knox specific — Knox container ID
        try {
            val knoxVersion = getKnoxVersion()
            components.append("KNOX:$knoxVersion|")
        } catch (_: Exception) {}

        // Hash all components with SHA-512 for a fixed-length fingerprint
        val digest = MessageDigest.getInstance("SHA-512")
        val hash = digest.digest(components.toString().toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Get Samsung Knox version if available.
     */
    private fun getKnoxVersion(): String {
        return try {
            // Samsung Knox version is stored in system property
            val knoxVersionProp = System.getProperty("net.knoxvpn.tun.version")
            if (knoxVersionProp != null) return "v$knoxVersionProp"

            // Try Knox API version via reflection
            val knoxClass = Class.forName("com.samsung.android.knox.EnterpriseDeviceManager")
            val versionField = knoxClass.getField("KNOX_VERSION_CODES")
            versionField.get(null)?.toString() ?: "present"
        } catch (_: Exception) {
            // Try reading Knox version from build props
            try {
                val process = Runtime.getRuntime().exec(arrayOf("getprop", "ro.boot.warranty_bit"))
                val result = process.inputStream.bufferedReader().readText().trim()
                if (result.isNotEmpty()) "warranty:$result" else "unavailable"
            } catch (_: Exception) {
                "unavailable"
            }
        }
    }

    /**
     * Store Knox attestation data (Samsung specific).
     */
    private fun storeKnoxAttestation(context: Context) {
        try {
            val attestation = StringBuilder()
            attestation.append("knox_version:${getKnoxVersion()}\n")
            attestation.append("security_patch:${Build.VERSION.SECURITY_PATCH}\n")
            attestation.append("bootloader:${Build.BOOTLOADER}\n")
            attestation.append("verified_boot:${getVerifiedBootState()}\n")
            attestation.append("se_linux:${getSeLinuxStatus()}\n")
            attestation.append("timestamp:${System.currentTimeMillis()}\n")

            val encrypted = CryptoManager.encryptText(attestation.toString())
            File(context.filesDir, KNOX_ATTESTATION_FILE).writeBytes(encrypted)
        } catch (_: Exception) {}
    }

    /**
     * Check Samsung Knox security status.
     */
    private fun checkKnoxStatus(context: Context): KnoxStatus {
        val knoxVersion = getKnoxVersion()
        val isKnoxAvailable = knoxVersion != "unavailable"
        val verifiedBoot = getVerifiedBootState()
        val seLinux = getSeLinuxStatus()

        return KnoxStatus(
            isAvailable = isKnoxAvailable,
            version = knoxVersion,
            verifiedBootState = verifiedBoot,
            seLinuxStatus = seLinux,
            warrantyBit = getWarrantyBit()
        )
    }

    /**
     * Get verified boot state.
     */
    private fun getVerifiedBootState(): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("getprop", "ro.boot.verifiedbootstate"))
            process.inputStream.bufferedReader().readText().trim().ifEmpty { "unknown" }
        } catch (_: Exception) { "unknown" }
    }

    /**
     * Get SELinux enforcement status.
     */
    private fun getSeLinuxStatus(): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("getenforce"))
            process.inputStream.bufferedReader().readText().trim().ifEmpty { "unknown" }
        } catch (_: Exception) { "unknown" }
    }

    /**
     * Get Samsung warranty bit (Knox flag — 0x0 = untouched, 0x1 = tripped).
     */
    private fun getWarrantyBit(): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("getprop", "ro.boot.warranty_bit"))
            val result = process.inputStream.bufferedReader().readText().trim()
            when (result) {
                "0" -> "intact"
                "1" -> "tripped"
                else -> result.ifEmpty { "unknown" }
            }
        } catch (_: Exception) { "unknown" }
    }

    data class KnoxStatus(
        val isAvailable: Boolean,
        val version: String,
        val verifiedBootState: String,
        val seLinuxStatus: String,
        val warrantyBit: String
    )

    data class DeviceIntegrityResult(
        val isDeviceBound: Boolean,
        val knoxStatus: KnoxStatus,
        val isHardwareBackedKeystore: Boolean,
        val deviceModel: String,
        val androidVersion: String,
        val securityPatchLevel: String,
        val kernelVersion: String
    ) {
        val isFullySecure: Boolean
            get() = isDeviceBound &&
                    knoxStatus.warrantyBit != "tripped" &&
                    knoxStatus.verifiedBootState != "orange" &&
                    isHardwareBackedKeystore
    }
}
