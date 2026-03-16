package com.securefolder.features.vault

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securefolder.core.crypto.CryptoManager
import com.securefolder.data.model.VaultFile
import com.securefolder.ui.theme.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Encrypted file vault screen.
 * Import, view, and manage encrypted files (photos, videos, documents).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var vaultFiles by remember { mutableStateOf(loadVaultFiles(context)) }
    var selectedFile by remember { mutableStateOf<VaultFile?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<VaultFile?>(null) }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            importAndEncryptFile(context, uri)
        }
        vaultFiles = loadVaultFiles(context)
    }

    Scaffold(
        containerColor = PrimaryDark,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🔒", fontSize = 20.sp)
                        Text(
                            "Encrypted Vault",
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    Text(
                        text = "${vaultFiles.size} files",
                        style = MaterialTheme.typography.labelMedium,
                        color = CyanAccent,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryDark)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { filePickerLauncher.launch("*/*") },
                containerColor = CyanAccent,
                contentColor = PrimaryDark
            ) {
                Icon(Icons.Default.Add, contentDescription = "Import Files")
            }
        }
    ) { padding ->
        if (vaultFiles.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🔐", fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your vault is empty",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Import files to encrypt and protect them",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CyanAccent
                        )
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import Files")
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(vaultFiles) { file ->
                    VaultFileCard(
                        vaultFile = file,
                        onClick = { selectedFile = file },
                        onDelete = {
                            fileToDelete = file
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    "Secure Delete",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "This will permanently and securely erase \"${fileToDelete?.originalName}\". This cannot be undone.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        fileToDelete?.let { f ->
                            secureDeleteFile(context, f)
                            vaultFiles = loadVaultFiles(context)
                        }
                        showDeleteDialog = false
                        fileToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = RedAlert)
                ) {
                    Text("Delete Forever")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceDark
        )
    }
}

@Composable
private fun VaultFileCard(
    vaultFile: VaultFile,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // File type icon
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceVariantDark),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when {
                        vaultFile.isImage -> "🖼️"
                        vaultFile.isVideo -> "🎬"
                        vaultFile.isAudio -> "🎵"
                        else -> "📄"
                    },
                    fontSize = 36.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // File info
            Text(
                text = vaultFile.originalName,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = vaultFile.formattedSize,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = RedAlert.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ---- File operations ----

private fun getVaultDir(context: android.content.Context): File {
    return File(context.filesDir, "encrypted_vault").also {
        if (!it.exists()) it.mkdirs()
    }
}

private fun getMetadataDir(context: android.content.Context): File {
    return File(context.filesDir, "vault_metadata").also {
        if (!it.exists()) it.mkdirs()
    }
}

private fun importAndEncryptFile(context: android.content.Context, uri: Uri) {
    try {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
        val cursor = contentResolver.query(uri, null, null, null, null)
        val displayName = cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) it.getString(nameIndex) else "unnamed"
            } else "unnamed"
        } ?: "unnamed"

        val timestamp = System.currentTimeMillis()
        val encryptedFileName = "vault_${timestamp}.enc"
        val encryptedFile = File(getVaultDir(context), encryptedFileName)

        // Encrypt file
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(encryptedFile).use { output ->
                CryptoManager.encryptStream(input, output)
            }
        }

        // Save metadata
        val metadata = "$displayName|$mimeType|${encryptedFile.length()}|$timestamp"
        val encryptedMetadata = CryptoManager.encryptText(metadata)
        val metadataFile = File(getMetadataDir(context), "${encryptedFileName}.meta")
        metadataFile.writeBytes(encryptedMetadata)

    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun loadVaultFiles(context: android.content.Context): List<VaultFile> {
    val vaultDir = getVaultDir(context)
    val metadataDir = getMetadataDir(context)
    val files = mutableListOf<VaultFile>()

    vaultDir.listFiles()?.forEach { encFile ->
        try {
            val metaFile = File(metadataDir, "${encFile.name}.meta")
            if (metaFile.exists()) {
                val decryptedMeta = CryptoManager.decryptText(metaFile.readBytes())
                val parts = decryptedMeta.split("|")
                if (parts.size >= 4) {
                    files.add(
                        VaultFile(
                            id = parts[3].toLong(),
                            originalName = parts[0],
                            encryptedPath = encFile.absolutePath,
                            mimeType = parts[1],
                            fileSize = parts[2].toLong(),
                            createdAt = parts[3].toLong()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    return files.sortedByDescending { it.createdAt }
}

private fun secureDeleteFile(context: android.content.Context, vaultFile: VaultFile) {
    try {
        val encFile = File(vaultFile.encryptedPath)
        val metaFile = File(getMetadataDir(context), "${encFile.name}.meta")

        // Overwrite with random data before deletion
        if (encFile.exists()) {
            FileOutputStream(encFile).use { output ->
                CryptoManager.secureDelete(output, encFile.length())
            }
            encFile.delete()
        }
        metaFile.delete()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
