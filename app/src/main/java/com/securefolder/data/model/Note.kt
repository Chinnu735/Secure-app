package com.securefolder.data.model

/**
 * Represents an encrypted note stored in the vault.
 */
data class Note(
    val id: Long = 0,
    val title: String = "",
    val encryptedContent: ByteArray = ByteArray(0),
    val category: String = "General",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Note) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
