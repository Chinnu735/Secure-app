package com.securefolder.core.crypto

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted SharedPreferences wrapper.
 * All keys and values are encrypted using EncryptedSharedPreferences
 * backed by Android Keystore master key.
 */
class SecurePreferences(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_folder_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // PIN Management
    var pinHash: String?
        get() = prefs.getString(KEY_PIN_HASH, null)
        set(value) = prefs.edit().putString(KEY_PIN_HASH, value).apply()

    var pinSalt: String?
        get() = prefs.getString(KEY_PIN_SALT, null)
        set(value) = prefs.edit().putString(KEY_PIN_SALT, value).apply()

    // Authentication settings
    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    var failedAttempts: Int
        get() = prefs.getInt(KEY_FAILED_ATTEMPTS, 0)
        set(value) = prefs.edit().putInt(KEY_FAILED_ATTEMPTS, value).apply()

    // Security settings
    var isSelfDestructEnabled: Boolean
        get() = prefs.getBoolean(KEY_SELF_DESTRUCT_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SELF_DESTRUCT_ENABLED, value).apply()

    var maxAttempts: Int
        get() = prefs.getInt(KEY_MAX_ATTEMPTS, 10)
        set(value) = prefs.edit().putInt(KEY_MAX_ATTEMPTS, value).apply()

    var isIntruderDetectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_INTRUDER_DETECTION, true)
        set(value) = prefs.edit().putBoolean(KEY_INTRUDER_DETECTION, value).apply()

    // Stealth mode
    var isStealthModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_STEALTH_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_STEALTH_MODE, value).apply()

    // Auto-lock timeout (in milliseconds)
    var autoLockTimeout: Long
        get() = prefs.getLong(KEY_AUTO_LOCK_TIMEOUT, 60_000L) // default: 1 minute
        set(value) = prefs.edit().putLong(KEY_AUTO_LOCK_TIMEOUT, value).apply()

    // App setup state
    var isSetupComplete: Boolean
        get() = prefs.getBoolean(KEY_SETUP_COMPLETE, false)
        set(value) = prefs.edit().putBoolean(KEY_SETUP_COMPLETE, value).apply()

    // Last unlock timestamp
    var lastUnlockTimestamp: Long
        get() = prefs.getLong(KEY_LAST_UNLOCK, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_UNLOCK, value).apply()

    /**
     * Clear all preferences - used during self-destruct.
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    /**
     * Reset failed attempts counter.
     */
    fun resetFailedAttempts() {
        failedAttempts = 0
    }

    companion object {
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_SELF_DESTRUCT_ENABLED = "self_destruct_enabled"
        private const val KEY_MAX_ATTEMPTS = "max_attempts"
        private const val KEY_INTRUDER_DETECTION = "intruder_detection"
        private const val KEY_STEALTH_MODE = "stealth_mode"
        private const val KEY_AUTO_LOCK_TIMEOUT = "auto_lock_timeout"
        private const val KEY_SETUP_COMPLETE = "setup_complete"
        private const val KEY_LAST_UNLOCK = "last_unlock"
    }
}
