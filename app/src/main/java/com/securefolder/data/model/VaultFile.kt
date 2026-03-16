package com.securefolder.data.model

/**
 * Represents an encrypted file stored in the vault.
 */
data class VaultFile(
    val id: Long = 0,
    val originalName: String = "",
    val encryptedPath: String = "",
    val mimeType: String = "",
    val fileSize: Long = 0,
    val thumbnailPath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isImage: Boolean get() = mimeType.startsWith("image/")
    val isVideo: Boolean get() = mimeType.startsWith("video/")
    val isAudio: Boolean get() = mimeType.startsWith("audio/")
    val isDocument: Boolean get() = !isImage && !isVideo && !isAudio

    val formattedSize: String
        get() {
            return when {
                fileSize < 1024 -> "$fileSize B"
                fileSize < 1024 * 1024 -> "${fileSize / 1024} KB"
                fileSize < 1024 * 1024 * 1024 -> "${fileSize / (1024 * 1024)} MB"
                else -> "${fileSize / (1024 * 1024 * 1024)} GB"
            }
        }
}
