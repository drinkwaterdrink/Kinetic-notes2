package com.example.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ChecklistItemEntity
import com.example.data.local.NoteEntity
import com.example.data.local.NoteType
import com.example.data.repository.LinkSuggestion
import com.example.ui.theme.KineticDarkBorder
import com.example.ui.theme.KineticDarkSurface
import com.example.ui.theme.KineticDarkSurfaceVariant
import com.example.ui.theme.KineticPrimary
import com.example.ui.theme.KineticSecondary
import com.example.ui.theme.KineticTextMuted
import com.example.ui.theme.KineticTextPrimary
import com.example.ui.theme.KineticTextSecondary
import com.example.ui.viewmodel.FolderItem

@Composable
fun NoteDetailSheet(
    note: NoteEntity,
    checklistItems: List<ChecklistItemEntity>,
    suggestedLinks: List<LinkSuggestion>,
    aiSummaryOutput: String?,
    isAiLoading: Boolean,
    availableFolders: List<FolderItem> = emptyList(),
    onClose: () -> Unit,
    onUpdateNote: (NoteEntity) -> Unit,
    onDeleteNote: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleLock: () -> Unit,
    onWikiLinkClick: (String) -> Unit,
    onAddChecklistItem: (String) -> Unit,
    onToggleChecklistItem: (ChecklistItemEntity) -> Unit,
    onDeleteChecklistItem: (ChecklistItemEntity) -> Unit,
    onBeautifyNote: () -> Unit,
    onRequestAiLinks: () -> Unit,
    onAcceptAiLink: (LinkSuggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onClose()
    }

    val clipboardManager = LocalClipboardManager.current
    var title by remember(note.id) { mutableStateOf(note.title) }
    var content by remember(note.id) { mutableStateOf(note.content) }
    var activeTab by remember(note.id) {
        mutableStateOf(if (note.type == NoteType.CHECKLIST) "checklist" else "doc")
    }
    var showFolderMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember(note.id) { mutableStateOf(false) }

    val accentColor = try {
        Color(android.graphics.Color.parseColor(note.colorHex))
    } catch (e: Exception) {
        KineticPrimary
    }

    val accents = listOf(
        "#6366F1", // Indigo
        "#38BDF8", // Cyan
        "#10B981", // Emerald
        "#EC4899", // Rose
        "#8B5CF6", // Violet
        "#F59E0B"  // Amber
    )

    // Solid opaque full-screen container - completely shields against background canvas bleed
    Surface(
        color = Color(0xFF0C1018),
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .testTag("executive_focus_modal")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0C1018))
        ) {
            // 1. Top Header Sticky Ribbon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF101422))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("btn_close_focus")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = KineticTextPrimary
                    )
                }

                // Folder / Group Selector Pill with Dropdown
                Box {
                    Surface(
                        color = Color(0xFF161B2C),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x28FFFFFF)),
                        modifier = Modifier.clickable { showFolderMenu = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = note.folder.ifBlank { note.tag },
                                color = KineticTextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Change Group",
                                tint = KineticTextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showFolderMenu,
                        onDismissRequest = { showFolderMenu = false },
                        modifier = Modifier
                            .background(Color(0xFF141824))
                            .border(1.dp, Color(0x20FFFFFF), RoundedCornerShape(8.dp))
                    ) {
                        availableFolders.filter { it.name != "All Notes" }.forEach { folder ->
                            val folderCol = try {
                                Color(android.graphics.Color.parseColor(folder.colorHex))
                            } catch (e: Exception) {
                                KineticSecondary
                            }
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(folderCol)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(folder.name, color = Color.White, fontSize = 12.sp)
                                    }
                                },
                                onClick = {
                                    showFolderMenu = false
                                    onUpdateNote(note.copy(folder = folder.name, tag = folder.name))
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Color accent dots
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    accents.forEach { hex ->
                        val col = Color(android.graphics.Color.parseColor(hex))
                        val isSel = note.colorHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(
                                    width = if (isSel) 2.dp else 0.dp,
                                    color = if (isSel) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    onUpdateNote(note.copy(colorHex = hex))
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Pin toggle
                IconButton(onClick = onTogglePin, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = "Pin",
                        tint = if (note.isPinned) accentColor else KineticTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Lock toggle
                IconButton(onClick = onToggleLock, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = if (note.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Lock",
                        tint = if (note.isLocked) KineticSecondary else KineticTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // AI Beautify Button (Surprise me feature!)
                Button(
                    onClick = onBeautifyNote,
                    enabled = !isAiLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KineticPrimary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(32.dp)
                        .padding(horizontal = 4.dp)
                        .testTag("btn_ai_beautify")
                ) {
                    if (isAiLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Beautify", modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Beautify", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Done Button
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF222838),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Done", fontSize = 11.sp)
                }
            }

            // 2. Title & Live Metadata Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F131D))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        onUpdateNote(note.copy(title = it))
                    },
                    placeholder = {
                        Text("Note Title...", color = KineticTextMuted, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    },
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_focus_title")
                )

                // Sub-header metadata
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    val wordCount = content.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.size
                    Text(
                        text = "Updated just now • $wordCount words • ${note.folder.ifBlank { note.tag }}",
                        color = KineticTextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Mode tabs (Document vs Tasks)
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF141824), RoundedCornerShape(8.dp))
                            .padding(2.dp)
                    ) {
                        Surface(
                            color = if (activeTab == "doc") KineticPrimary else Color.Transparent,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.clickable { activeTab = "doc" }
                        ) {
                            Text(
                                "📝 Note",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Surface(
                            color = if (activeTab == "checklist") Color(0xFF10B981) else Color.Transparent,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.clickable { activeTab = "checklist" }
                        ) {
                            Text(
                                "☑️ Tasks",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // 3. Main Focus Content Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF0C1018))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                if (activeTab == "checklist") {
                    ChecklistEditor(
                        items = checklistItems,
                        onAddItem = onAddChecklistItem,
                        onToggleItem = onToggleChecklistItem,
                        onDeleteItem = onDeleteChecklistItem
                    )
                } else {
                    MarkdownWikiEditor(
                        content = content,
                        onContentChange = {
                            content = it
                            onUpdateNote(note.copy(content = it))
                        },
                        onWikiLinkClick = onWikiLinkClick
                    )
                }
            }

            // 4. Bottom Focus Bar Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF101420))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Copy Action
                Button(
                    onClick = {
                        val full = "# ${note.title}\n\n${note.content}"
                        clipboardManager.setText(AnnotatedString(full))
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF191F30),
                        contentColor = KineticTextSecondary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Insert [[Link]]
                Button(
                    onClick = {
                        val updated = content + " [[Sprint Deliverables]]"
                        content = updated
                        onUpdateNote(note.copy(content = updated))
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF191F30),
                        contentColor = KineticSecondary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("+ [[Link]]", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // AI Link Suggestions Button
                Button(
                    onClick = onRequestAiLinks,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF191F30),
                        contentColor = KineticPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.AddLink, contentDescription = "AI Links", modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto-Link", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.weight(1f))

                // Archive Note
                IconButton(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("btn_delete_note")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFF87171),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    if (showDeleteConfirmation) {
        DeleteNoteConfirmationDialog(
            noteTitle = note.title,
            onDismissRequest = { showDeleteConfirmation = false },
            onConfirm = {
                showDeleteConfirmation = false
                onDeleteNote()
            }
        )
    }
}
