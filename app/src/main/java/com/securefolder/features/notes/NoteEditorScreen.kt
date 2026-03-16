package com.securefolder.features.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.securefolder.data.model.Note
import com.securefolder.ui.theme.*

/**
 * Note editor screen with encrypted save.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val existingNote = remember {
        if (noteId > 0) loadNoteById(context, noteId) else null
    }

    var title by remember { mutableStateOf(existingNote?.first?.title ?: "") }
    var content by remember { mutableStateOf(existingNote?.second ?: "") }
    var isSaved by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = PrimaryDark,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (noteId > 0) "Edit Note" else "New Note",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        // Auto-save on back
                        if (title.isNotBlank() || content.isNotBlank()) {
                            val note = Note(
                                id = existingNote?.first?.id ?: 0L,
                                title = title,
                                category = "General",
                                createdAt = existingNote?.first?.createdAt ?: System.currentTimeMillis()
                            )
                            saveNote(context, note, content)
                        }
                        onBack()
                    }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (title.isNotBlank() || content.isNotBlank()) {
                            val note = Note(
                                id = existingNote?.first?.id ?: 0L,
                                title = title,
                                category = "General",
                                createdAt = existingNote?.first?.createdAt ?: System.currentTimeMillis()
                            )
                            saveNote(context, note, content)
                            isSaved = true
                        }
                    }) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Save",
                            tint = if (isSaved) GreenSecure else CyanAccent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryDark)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            // Title field
            BasicTextField(
                value = title,
                onValueChange = {
                    title = it
                    isSaved = false
                },
                textStyle = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                ),
                cursorBrush = SolidColor(CyanAccent),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                decorationBox = { innerTextField ->
                    Box {
                        if (title.isEmpty()) {
                            Text(
                                text = "Title",
                                style = TextStyle(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextTertiary
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Divider(
                color = SurfaceVariantDark,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Encryption indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text("🔒", fontSize = 12.sp)
                Text(
                    text = "End-to-end encrypted • AES-256-GCM",
                    style = MaterialTheme.typography.labelSmall,
                    color = TealAccent
                )
            }

            // Content field
            BasicTextField(
                value = content,
                onValueChange = {
                    content = it
                    isSaved = false
                },
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    color = TextPrimary,
                    lineHeight = 24.sp
                ),
                cursorBrush = SolidColor(CyanAccent),
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                decorationBox = { innerTextField ->
                    Box {
                        if (content.isEmpty()) {
                            Text(
                                text = "Start typing…",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    color = TextTertiary
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}
