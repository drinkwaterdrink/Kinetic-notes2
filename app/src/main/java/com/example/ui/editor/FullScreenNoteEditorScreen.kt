package com.example.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import com.example.ui.theme.KineticDarkBackground
import com.example.ui.theme.KineticDarkBorder
import com.example.ui.theme.KineticDarkSurface
import com.example.ui.theme.KineticDarkSurfaceVariant
import com.example.ui.theme.KineticPrimary
import com.example.ui.theme.KineticSecondary
import com.example.ui.theme.KineticTextMuted
import com.example.ui.theme.KineticTextPrimary
import com.example.ui.theme.KineticTextSecondary
import com.example.ui.viewmodel.EditorSaveStatus
import com.example.ui.viewmodel.FolderItem
import kotlinx.coroutines.delay

@Composable
fun FullScreenNoteEditorScreen(
    note: NoteEntity,
    checklistItems: List<ChecklistItemEntity>,
    suggestedLinks: List<LinkSuggestion>,
    aiSummaryOutput: String?,
    isAiLoading: Boolean,
    availableFolders: List<FolderItem> = emptyList(),
    saveStatus: EditorSaveStatus = EditorSaveStatus.SAVED,
    onClose: () -> Unit,
    onDraftChanged: () -> Unit = {},
    onSaveDraft: (title: String, content: String) -> Unit = { _, _ -> },
    onSaveAndClose: (title: String, content: String) -> Unit = { _, _ -> },
    onUpdateNote: (NoteEntity) -> Unit,
    onMoveToGroup: (String?) -> Unit = {},
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
    val clipboardManager = LocalClipboardManager.current

    // Editor-local draft. Keystrokes mutate ONLY this state; the database is written by the
    // debounced autosave below, by the Save button, or by Back/Done - never per keystroke.
    var title by remember(note.id) { mutableStateOf(note.title) }
    var content by remember(note.id) { mutableStateOf(note.content) }
    var isDirty by remember(note.id) { mutableStateOf(false) }

    val currentTitle by rememberUpdatedState(title)
    val currentContent by rememberUpdatedState(content)

    fun onDraftEdited() {
        if (!isDirty) isDirty = true
        onDraftChanged()
    }

    fun flushNow() {
        if (isDirty || saveStatus == EditorSaveStatus.ERROR) {
            isDirty = false
            onSaveDraft(currentTitle, currentContent)
        }
    }

    // Debounced durable autosave: restarts on every keystroke, fires once typing pauses.
    LaunchedEffect(note.id, title, content) {
        if (!isDirty) return@LaunchedEffect
        delay(AUTOSAVE_DEBOUNCE_MS)
        isDirty = false
        onSaveDraft(currentTitle, currentContent)
    }

    // Safe Back: flush the pending draft, and only leave once the write has been issued.
    BackHandler {
        if (isDirty || saveStatus == EditorSaveStatus.ERROR) {
            isDirty = false
            onSaveAndClose(currentTitle, currentContent)
        } else {
            onClose()
        }
    }

    fun closeWithFlush() {
        if (isDirty || saveStatus == EditorSaveStatus.ERROR) {
            isDirty = false
            onSaveAndClose(currentTitle, currentContent)
        } else {
            onClose()
        }
    }

    // Metadata changes (colour, group, type...) must carry the live draft so an in-flight
    // edit is never overwritten by a stale snapshot of the note.
    fun updateMetadata(updated: NoteEntity) {
        isDirty = false
        onUpdateNote(updated.copy(title = currentTitle, content = currentContent))
    }

    /** Formatting controls update the local draft; autosave remains the only text write path. */
    fun applyContentChange(updatedContent: String) {
        content = updatedContent
        onDraftEdited()
    }
    var activeTab by remember(note.id) {
        mutableStateOf(if (note.type == NoteType.CHECKLIST) "checklist" else "doc")
    }
    var showFolderMenu by remember { mutableStateOf(false) }

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

    val scrollState = rememberScrollState()

    // Regex for finding [[WikiLinks]] in content
    val wikiLinks by remember(content) {
        derivedStateOf {
            val regex = Regex("\\[\\[([^\\]]+)\\]\\]")
            regex.findAll(content).map { it.groupValues[1] }.distinct().toList()
        }
    }

    // Full-screen native container
    Surface(
        color = Color(0xFF0A0D14),
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("fullscreen_editor_screen")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0D14))
        ) {
            // 1. Sticky Top Bar: Back, Group Pill, Color Accents, Pin, Lock, Done
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF101422))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                IconButton(
                    onClick = { closeWithFlush() },
                    modifier = Modifier.testTag("btn_close_editor")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = KineticTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Group Pill with Dropdown
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
                        availableFolders.forEach { folder ->
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
                                    onMoveToGroup(folder.id)
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
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(
                                    width = if (isSel) 2.dp else 0.dp,
                                    color = if (isSel) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    updateMetadata(note.copy(colorHex = hex))
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Pin Note Toggle
                IconButton(
                    onClick = onTogglePin,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pin",
                        tint = if (note.isPinned) Color(0xFFF59E0B) else KineticTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Privacy Lock Toggle
                IconButton(
                    onClick = onToggleLock,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Lock",
                        tint = if (note.isLocked) KineticSecondary else KineticTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // AI Beautify Button
                IconButton(
                    onClick = onBeautifyNote,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Beautify Note",
                        tint = Color(0xFFA855F7),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Explicit Save: immediate durable flush, independent of the debounce.
                Button(
                    onClick = { flushNow() },
                    enabled = isDirty || saveStatus == EditorSaveStatus.ERROR,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KineticPrimary,
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFF1B2030),
                        disabledContentColor = KineticTextMuted
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("btn_save_note")
                ) {
                    Text("Save", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Done Button (flushes, then leaves)
                Button(
                    onClick = { closeWithFlush() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF222838),
                        contentColor = Color.White
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Done", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // 2. Scrollable Body containing Title, Metadata, Note Type Switcher, and Editor Body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Title Field
                BasicTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        onDraftEdited()
                    },
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Default
                    ),
                    cursorBrush = SolidColor(KineticPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_focus_title"),
                    decorationBox = { innerTextField ->
                        if (title.isEmpty()) {
                            Text("Note Title...", color = KineticTextMuted, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        innerTextField()
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Metadata Sub-Header & Tabs
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val wordCount = content.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }.size
                    SaveStatusChip(
                        status = saveStatus,
                        isDirty = isDirty,
                        modifier = Modifier.testTag("editor_save_status")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$wordCount words",
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

                // WikiLinks Badges row
                if (wikiLinks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Text("LINKS: ", color = KineticTextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        wikiLinks.forEach { linkTitle ->
                            Surface(
                                color = KineticSecondary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, KineticSecondary.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .clickable { onWikiLinkClick(linkTitle) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Default.Link, contentDescription = null, tint = KineticSecondary, modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("[[$linkTitle]]", color = KineticSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Main Content: Checklist or Markdown Document
                if (activeTab == "checklist") {
                    ChecklistEditor(
                        items = checklistItems,
                        onAddItem = onAddChecklistItem,
                        onToggleItem = onToggleChecklistItem,
                        onDeleteItem = onDeleteChecklistItem
                    )
                } else {
                    BasicTextField(
                        value = content,
                        onValueChange = {
                            content = it
                            onDraftEdited()
                        },
                        textStyle = TextStyle(
                            color = KineticTextPrimary,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            fontFamily = FontFamily.Default
                        ),
                        cursorBrush = SolidColor(KineticPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 350.dp)
                            .testTag("input_markdown_content"),
                        decorationBox = { innerTextField ->
                            if (content.isEmpty()) {
                                Text(
                                    text = "Write your thoughts using Markdown and [[WikiLinks]] to connect notes...",
                                    color = KineticTextMuted,
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                // AI Suggested Links pill card if available
                if (suggestedLinks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = Color(0xFF131726),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x356366F1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = KineticSecondary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Suggested Note Connections", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            suggestedLinks.forEach { suggestion ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("[[${suggestion.targetTitle}]]", color = KineticSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Text(suggestion.reason, color = KineticTextMuted, fontSize = 10.sp)
                                    }
                                    Button(
                                        onClick = { onAcceptAiLink(suggestion) },
                                        colors = ButtonDefaults.buttonColors(containerColor = KineticPrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Link", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Formatting / Action Bar - Anchored immediately above the IME Keyboard!
            Surface(
                color = Color(0xFF101420),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x18FFFFFF)),
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    // Bold
                    IconButton(
                        onClick = {
                            val updated = "$content**bold**"
                            content = updated
                            applyContentChange(updated)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.FormatBold, contentDescription = "Bold", tint = KineticTextSecondary, modifier = Modifier.size(18.dp))
                    }

                    // Italic
                    IconButton(
                        onClick = {
                            val updated = "$content*italic*"
                            content = updated
                            applyContentChange(updated)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.FormatItalic, contentDescription = "Italic", tint = KineticTextSecondary, modifier = Modifier.size(18.dp))
                    }

                    // Heading
                    IconButton(
                        onClick = {
                            val updated = "$content\n## Heading\n"
                            content = updated
                            applyContentChange(updated)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Title, contentDescription = "Heading", tint = KineticTextSecondary, modifier = Modifier.size(18.dp))
                    }

                    // Quote
                    IconButton(
                        onClick = {
                            val updated = "$content\n> Quote block\n"
                            content = updated
                            applyContentChange(updated)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.FormatQuote, contentDescription = "Quote", tint = KineticTextSecondary, modifier = Modifier.size(18.dp))
                    }

                    // Code Block
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .clickable {
                                val updated = "$content\n```typescript\n// code snippet\n```\n"
                                content = updated
                                applyContentChange(updated)
                            }
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Text("</>", color = KineticSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    // Insert [[WikiLink]]
                    Surface(
                        color = KineticSecondary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, KineticSecondary.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .clickable {
                                val updated = "$content[[Link]]"
                                content = updated
                                applyContentChange(updated)
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("+ [[Link]]", color = KineticSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Copy Note
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString("${note.title}\n\n$content"))
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = KineticTextSecondary, modifier = Modifier.size(16.dp))
                    }

                    // Delete Note
                    IconButton(
                        onClick = onDeleteNote,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}


/** How long typing must pause before the draft is flushed to Room. */
const val AUTOSAVE_DEBOUNCE_MS = 700L

/**
 * Visible saved / unsaved / saving / error state for the open note.
 * [isDirty] is the editor-local truth and always wins over a stale SAVED status.
 */
@Composable
fun SaveStatusChip(
    status: EditorSaveStatus,
    isDirty: Boolean,
    modifier: Modifier = Modifier
) {
    val effective = if (isDirty && status != EditorSaveStatus.ERROR) EditorSaveStatus.UNSAVED else status
    val (label, tint) = when (effective) {
        EditorSaveStatus.SAVED -> "Saved" to KineticTextMuted
        EditorSaveStatus.UNSAVED -> "Unsaved" to Color(0xFFF59E0B)
        EditorSaveStatus.SAVING -> "Saving…" to KineticSecondary
        EditorSaveStatus.ERROR -> "Save failed" to Color(0xFFEF4444)
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(tint)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace
        )
    }
}
