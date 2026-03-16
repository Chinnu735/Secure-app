package com.securefolder.core.crypto

import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM encryption engine.
 * Provides both text and streaming file encryption with authenticated encryption.
 * Each encryption operation uses a unique IV for maximum security.
 */
object CryptoManager {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val GCM_TAG_LENGTH = 128 // bits
    private const val GCM_IV_LENGTH = 12   // bytes (96 bits, NIST recommended)
    private const val CHUNK_SIZE = 8192    // bytes for streaming

    /**
     * Encrypt text data. Returns IV prepended to ciphertext.
     * Format: [IV_LENGTH(4 bytes)][IV][CIPHERTEXT]
     */
    fun encryptText(plainText: String, key: SecretKey = KeyStoreManager.getMasterKey()): ByteArray {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        // Pack: IV length + IV + ciphertext
        val buffer = ByteBuffer.allocate(4 + iv.size + cipherText.size)
        buffer.putInt(iv.size)
        buffer.put(iv)
        buffer.put(cipherText)
        return buffer.array()
    }

    /**
     * Decrypt text data. Expects IV prepended to ciphertext.
     */
    fun decryptText(encryptedData: ByteArray, key: SecretKey = KeyStoreManager.getMasterKey()): String {
        val buffer = ByteBuffer.wrap(encryptedData)
        val ivLength = buffer.int
        val iv = ByteArray(ivLength)
        buffer.get(iv)
        val cipherText = ByteArray(buffer.remaining())
        buffer.get(cipherText)

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val plainText = cipher.doFinal(cipherText)
        return String(plainText, Charsets.UTF_8)
    }

    /**
     * Encrypt raw bytes. Returns IV prepended to ciphertext.
     */
    fun encryptBytes(data: ByteArray, key: SecretKey = KeyStoreManager.getFileEncryptionKey()): ByteArray {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val cipherText = cipher.doFinal(data)

        val buffer = ByteBuffer.allocate(4 + iv.size + cipherText.size)
        buffer.putInt(iv.size)
        buffer.put(iv)
        buffer.put(cipherText)
        return buffer.array()
    }

    /**
     * Decrypt raw bytes.
     */
    fun decryptBytes(encryptedData: ByteArray, key: SecretKey = KeyStoreManager.getFileEncryptionKey()): ByteArray {
        val buffer = ByteBuffer.wrap(encryptedData)
        val ivLength = buffer.int
        val iv = ByteArray(ivLength)
        buffer.get(iv)
        val cipherText = ByteArray(buffer.remaining())
        buffer.get(cipherText)

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        return cipher.doFinal(cipherText)
    }

    /**
     * Streaming encryption for large files.
     * Writes IV first, then encrypted chunks.
     */
    fun encryptStream(
        inputStream: InputStream,
        outputStream: OutputStream,
        key: SecretKey = KeyStoreManager.getFileEncryptionKey()
    ) {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        // Write IV
        val iv = cipher.iv
        outputStream.write(ByteBuffer.allocate(4).putInt(iv.size).array())
        outputStream.write(iv)

        // Encrypt and write in chunks
        val buffer = ByteArray(CHUNK_SIZE)
        var bytesRead: Int
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            val encrypted = cipher.update(buffer, 0, bytesRead)
            if (encrypted != null) {
                outputStream.write(encrypted)
            }
        }

        // Write final block (includes GCM authentication tag)
        val finalBlock = cipher.doFinal()
        if (finalBlock != null) {
            outputStream.write(finalBlock)
        }

        outputStream.flush()
    }

    /**
     * Streaming decryption for large files.
     */
    fun decryptStream(
        inputStream: InputStream,
        outputStream: OutputStream,
        key: SecretKey = KeyStoreManager.getFileEncryptionKey()
    ) {
        // Read IV
        val ivLengthBytes = ByteArray(4)
        inputStream.read(ivLengthBytes)
        val ivLength = ByteBuffer.wrap(ivLengthBytes).int
        val iv = ByteArray(ivLength)
        inputStream.read(iv)

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))

        // Read all remaining encrypted data (GCM needs all data before doFinal for tag verification)
        val encryptedData = inputStream.readBytes()
        val decrypted = cipher.doFinal(encryptedData)
        outputStream.write(decrypted)
        outputStream.flush()
    }

    /**
     * Securely overwrite file data before deletion (DoD 5220.22-M inspired).
     * Writes random data, then zeros, then random data again.
     */
    fun secureDelete(outputStream: OutputStream, fileSize: Long) {
        val buffer = ByteArray(CHUNK_SIZE)
        val random = java.security.SecureRandom()

        repeat(3) { pass ->
            var remaining = fileSize
            while (remaining > 0) {
                val toWrite = minOf(remaining, CHUNK_SIZE.toLong()).toInt()
                if (pass == 1) {
                    // Pass 2: zeros
                    buffer.fill(0, 0, toWrite)
                } else {
                    // Pass 1 & 3: random data
                    random.nextBytes(buffer)
                }
                outputStream.write(buffer, 0, toWrite)
                remaining -= toWrite
            }
            outputStream.flush()
        }
    }
}
