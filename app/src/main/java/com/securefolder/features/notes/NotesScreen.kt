package com.securefolder.features.notes

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securefolder.core.crypto.CryptoManager
import com.securefolder.data.model.Note
import com.securefolder.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Secure notes list screen with encrypted storage.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    onBack: () -> Unit,
    onEditNote: (Long) -> Unit,
    onNewNote: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var notes by remember { mutableStateOf(loadNotes(context)) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }

    Scaffold(
        containerColor = PrimaryDark,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📝", fontSize = 20.sp)
                        Text(
                            "Secure Notes",
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
                        text = "${notes.size} notes",
                        style = MaterialTheme.typography.labelMedium,
                        color = TealAccent,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryDark)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewNote,
                containerColor = TealAccent,
                contentColor = PrimaryDark
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Note")
            }
        }
    ) { padding ->
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📝", fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No secure notes yet",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Create encrypted notes to protect your thoughts",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(notes) { note ->
                    NoteCard(
                        note = note,
                        onClick = { onEditNote(note.id) },
                        onDelete = {
                            noteToDelete = note
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }

    if (showDeleteDialog && noteToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Note", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This note will be permanently deleted.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        noteToDelete?.let { deleteNote(context, it) }
                        notes = loadNotes(context)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = RedAlert)
                ) { Text("Delete") }
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
private fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.US) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.title.ifEmpty { "Untitled" },
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = RedAlert.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Decrypted preview
            val preview = try {
                CryptoManager.decryptText(note.encryptedContent).take(100)
            } catch (e: Exception) {
                "Encrypted content"
            }
            Text(
                text = preview,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "🔒 ${note.category}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TealAccent
                )
                Text(
                    text = dateFormat.format(Date(note.updatedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextTertiary
                )
            }
        }
    }
}

// ---- Note Storage Operations ----

private fun getNotesDir(context: android.content.Context): File {
    return File(context.filesDir, "encrypted_notes").also {
        if (!it.exists()) it.mkdirs()
    }
}

internal fun loadNotes(context: android.content.Context): List<Note> {
    val notesDir = getNotesDir(context)
    val notes = mutableListOf<Note>()

    notesDir.listFiles()?.filter { it.extension == "note" }?.forEach { file ->
        try {
            val data = file.readBytes()
            val decrypted = CryptoManager.decryptText(data)
            val parts = decrypted.split("|||")
            if (parts.size >= 5) {
                notes.add(
                    Note(
                        id = parts[0].toLong(),
                        title = parts[1],
                        encryptedContent = CryptoManager.encryptText(parts[2]),
                        category = parts[3],
                        createdAt = parts[4].toLong(),
                        updatedAt = if (parts.size > 5) parts[5].toLong() else parts[4].toLong()
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    return notes.sortedByDescending { it.updatedAt }
}

internal fun saveNote(context: android.content.Context, note: Note, plainContent: String) {
    val notesDir = getNotesDir(context)
    val noteId = if (note.id == 0L) System.currentTimeMillis() else note.id
    val now = System.currentTimeMillis()
    val data = "$noteId|||${note.title}|||$plainContent|||${note.category}|||${note.createdAt}|||$now"
    val encrypted = CryptoManager.encryptText(data)
    File(notesDir, "note_$noteId.note").writeBytes(encrypted)
}

internal fun deleteNote(context: android.content.Context, note: Note) {
    val notesDir = getNotesDir(context)
    File(notesDir, "note_${note.id}.note").delete()
}

internal fun loadNoteById(context: android.content.Context, noteId: Long): Pair<Note, String>? {
    val notesDir = getNotesDir(context)
    val file = File(notesDir, "note_$noteId.note")
    if (!file.exists()) return null

    return try {
        val data = file.readBytes()
        val decrypted = CryptoManager.decryptText(data)
        val parts = decrypted.split("|||")
        if (parts.size >= 5) {
            val note = Note(
                id = parts[0].toLong(),
                title = parts[1],
                encryptedContent = CryptoManager.encryptText(parts[2]),
                category = parts[3],
                createdAt = parts[4].toLong(),
                updatedAt = if (parts.size > 5) parts[5].toLong() else parts[4].toLong()
            )
            Pair(note, parts[2])
        } else null
    } catch (e: Exception) {
        null
    }
}
