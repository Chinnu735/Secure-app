package com.securefolder.core.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Hardware-backed key management using Android Keystore.
 * Keys are stored in TEE (Trusted Execution Environment) or StrongBox if available.
 * Keys are non-exportable and bound to device hardware.
 */
object KeyStoreManager {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val MASTER_KEY_ALIAS = "secure_folder_master_key"
    private const val FILE_ENCRYPTION_KEY_ALIAS = "secure_folder_file_key"
    private const val DATABASE_KEY_ALIAS = "secure_folder_db_key"
    private const val NOTE_ENCRYPTION_KEY_ALIAS = "secure_folder_note_key"

    private val keyStore: KeyStore by lazy {
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
    }

    /**
     * Get or create the master encryption key.
     * This key is hardware-backed and non-exportable.
     */
    fun getMasterKey(): SecretKey {
        return getOrCreateKey(MASTER_KEY_ALIAS)
    }

    /**
     * Get or create the file encryption key.
     */
    fun getFileEncryptionKey(): SecretKey {
        return getOrCreateKey(FILE_ENCRYPTION_KEY_ALIAS)
    }

    /**
     * Get or create the database encryption key.
     */
    fun getDatabaseKey(): SecretKey {
        return getOrCreateKey(DATABASE_KEY_ALIAS)
    }

    /**
     * Get or create the note encryption key.
     */
    fun getNoteEncryptionKey(): SecretKey {
        return getOrCreateKey(NOTE_ENCRYPTION_KEY_ALIAS)
    }

    /**
     * Check if StrongBox (dedicated hardware security module) is available.
     */
    fun isStrongBoxAvailable(): Boolean {
        return try {
            val spec = KeyGenParameterSpec.Builder(
                "strongbox_test",
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setIsStrongBoxBacked(true)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()

            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            keyGenerator.init(spec)
            keyGenerator.generateKey()
            keyStore.deleteEntry("strongbox_test")
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Delete all keys - used during self-destruct/panic wipe.
     */
    fun deleteAllKeys() {
        listOf(
            MASTER_KEY_ALIAS,
            FILE_ENCRYPTION_KEY_ALIAS,
            DATABASE_KEY_ALIAS,
            NOTE_ENCRYPTION_KEY_ALIAS
        ).forEach { alias ->
            try {
                if (keyStore.containsAlias(alias)) {
                    keyStore.deleteEntry(alias)
                }
            } catch (e: Exception) {
                // Best effort deletion
            }
        }
    }

    /**
     * Check if master key exists (i.e., app has been set up).
     */
    fun isMasterKeyCreated(): Boolean {
        return keyStore.containsAlias(MASTER_KEY_ALIAS)
    }

    private fun getOrCreateKey(alias: String): SecretKey {
        // Return existing key if available
        keyStore.getEntry(alias, null)?.let { entry ->
            if (entry is KeyStore.SecretKeyEntry) {
                return entry.secretKey
            }
        }

        // Generate new hardware-backed key
        val builder = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)

        // Use StrongBox if available for maximum hardware security
        try {
            builder.setIsStrongBoxBacked(true)
        } catch (e: Exception) {
            // StrongBox not available, fall back to TEE
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        keyGenerator.init(builder.build())
        return keyGenerator.generateKey()
    }
}
