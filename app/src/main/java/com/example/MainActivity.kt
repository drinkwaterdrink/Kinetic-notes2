package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import com.example.ui.editor.FullScreenNoteEditorScreen
import com.example.ui.viewmodel.FolderItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.NoteEntity
import com.example.data.local.NoteType
import com.example.ui.canvas.SpatialCanvasView
import com.example.ui.components.AskGeminiBottomBar
import com.example.ui.editor.NoteDetailSheet
import com.example.ui.graph.ObsidianGraphView
import com.example.ui.grid.KeepGridView
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.KineticDarkBackground
import com.example.ui.theme.KineticDarkBorder
import com.example.ui.theme.KineticDarkSurface
import com.example.ui.theme.KineticDarkSurfaceVariant
import com.example.ui.theme.KineticPrimary
import com.example.ui.theme.KineticSecondary
import com.example.ui.theme.KineticTextMuted
import com.example.ui.theme.KineticTextPrimary
import com.example.ui.theme.KineticTextSecondary
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.NotesViewModel
import com.example.ui.viewmodel.VaultViewMode

class MainActivity : ComponentActivity() {
    private val viewModel: NotesViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                KineticCanvasApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun KineticCanvasApp(viewModel: NotesViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allNotes by viewModel.allNotes.collectAsStateWithLifecycle()
    val allLinks by viewModel.allLinks.collectAsStateWithLifecycle()
    val filteredNotes by viewModel.filteredNotes.collectAsStateWithLifecycle()
    val graphState by viewModel.graphState.collectAsStateWithLifecycle()
    val customFolders by viewModel.customFolders.collectAsStateWithLifecycle()
    val selectedGroupName by viewModel.selectedGroupName.collectAsStateWithLifecycle()

    var showCreateDialog by remember { mutableStateOf(false) }
    var folderMenuExpanded by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userNotice) {
        uiState.userNotice?.let { notice ->
            snackbarHostState.showSnackbar(notice)
            viewModel.dismissNotice()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // The header and bottom actions own their system-bar padding explicitly. Applying
        // Scaffold's default insets as well would double-count status/navigation bars.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = KineticDarkBackground,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main View Area: Spatial Board is the Default Home!
            when (uiState.viewMode) {
                VaultViewMode.CANVAS -> {
                    SpatialCanvasView(
                        notes = filteredNotes,
                        links = allLinks,
                        zoom = uiState.canvasZoom,
                        panX = uiState.canvasPanX,
                        panY = uiState.canvasPanY,
                        isLinkingMode = uiState.isLinkingMode,
                        linkSourceNoteId = uiState.linkSourceNoteId,
                        isSnapToGrid = uiState.isSnapToGrid,
                        onTransformChange = { centroid, panDelta, zoomChange ->
                            viewModel.updateCanvasTransform(centroid, panDelta, zoomChange)
                        },
                        onZoomIn = { viewModel.zoomIn() },
                        onZoomOut = { viewModel.zoomOut() },
                        onFitNotes = { viewModel.fitNotesToViewport() },
                        onViewportMeasured = { width, height -> viewModel.onViewportMeasured(width, height) },
                        onCardMove = { id, x, y ->
                            viewModel.moveCardOnCanvas(id, x, y)
                        },
                        onNoteClick = { note ->
                            viewModel.openNote(note)
                        },
                        onCardTapInLinkingMode = { id ->
                            viewModel.onCardTappedInLinkingMode(id)
                        },
                        onDeleteNote = { id ->
                            viewModel.deleteNoteById(id)
                        },
                        onNewNoteClick = {
                            viewModel.createNewNoteAtViewportCenter()
                        }
                    )
                }

                VaultViewMode.GRID -> {
                    KeepGridView(
                        notes = filteredNotes,
                        searchQuery = uiState.searchQuery,
                        selectedFolder = selectedGroupName,
                        folders = customFolders,
                        unlockedNoteIds = uiState.unlockedNoteIds,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onFolderSelect = { name ->
                            viewModel.selectGroup(customFolders.firstOrNull { it.name.equals(name, ignoreCase = true) }?.id)
                        },
                        onNoteClick = { note ->
                            viewModel.openNote(note)
                        },
                        onTogglePin = { viewModel.togglePin(it) }
                    )
                }

                VaultViewMode.GRAPH -> {
                    ObsidianGraphView(
                        graphState = graphState,
                        notes = allNotes,
                        onNodeDrag = { id, offset ->
                            viewModel.graphEngine.onNodeDrag(id, offset)
                        },
                        onNodeRelease = { id ->
                            viewModel.graphEngine.onNodeRelease(id)
                        },
                        onOpenNote = { note ->
                            viewModel.openNote(note)
                        }
                    )
                }
            }

            // Compact phone toolbar: the active group is primary; secondary tools stay in
            // conventional actions instead of consuming the whole top of the canvas.
            Surface(
                color = Color(0xFF0C1017).copy(alpha = 0.97f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x18FFFFFF)),
                shadowElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = 8.dp)
                ) {
                    // Small mark, not a large product title.
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Brush.linearGradient(listOf(KineticPrimary, KineticSecondary))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("K", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    Box {
                        Surface(
                            color = Color(0xFF131722),
                            shape = RoundedCornerShape(9.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x24FFFFFF)),
                            modifier = Modifier
                                .clickable { folderMenuExpanded = true }
                                .testTag("group_selector")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                Text(selectedGroupName, color = KineticTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select group", tint = KineticTextMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                        DropdownMenu(
                            expanded = folderMenuExpanded,
                            onDismissRequest = { folderMenuExpanded = false },
                            modifier = Modifier.background(Color(0xFF131722))
                        ) {
                            customFolders.forEach { folder ->
                                val count = if (folder.id == null) allNotes.size else allNotes.count { it.groupId == folder.id }
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                            Box(Modifier.size(8.dp).clip(CircleShape).background(parseToolbarColor(folder.colorHex)))
                                            Spacer(Modifier.width(8.dp))
                                            Text(folder.name, color = if (uiState.selectedGroupId == folder.id) KineticSecondary else Color.White, fontSize = 12.sp)
                                            Spacer(Modifier.weight(1f))
                                            Text("$count", color = KineticTextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectGroup(folder.id)
                                        folderMenuExpanded = false
                                    }
                                )
                            }
                            HorizontalDivider(color = Color(0x18FFFFFF))
                            DropdownMenuItem(
                                text = { Text("Manage groups…", color = KineticSecondary, fontSize = 12.sp) },
                                onClick = {
                                    folderMenuExpanded = false
                                    showSettings = true
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = { viewModel.setViewMode(VaultViewMode.GRID) },
                        modifier = Modifier.size(44.dp).testTag("btn_search_notes")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search notes", tint = KineticTextSecondary, modifier = Modifier.size(19.dp))
                    }

                    // Three compact view choices remain directly discoverable.
                    listOf(
                        VaultViewMode.CANVAS to Icons.Default.ViewQuilt,
                        VaultViewMode.GRID to Icons.Default.GridView,
                        VaultViewMode.GRAPH to Icons.Default.Hub
                    ).forEach { (mode, icon) ->
                        IconButton(
                            onClick = { viewModel.setViewMode(mode) },
                            modifier = Modifier
                                .size(36.dp)
                                .background(if (uiState.viewMode == mode) Color(0x30FFFFFF) else Color.Transparent, RoundedCornerShape(8.dp))
                        ) {
                            Icon(icon, contentDescription = mode.name.lowercase().replaceFirstChar { it.uppercase() }, tint = if (uiState.viewMode == mode) Color.White else KineticTextMuted, modifier = Modifier.size(17.dp))
                        }
                    }

                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier.size(44.dp).testTag("btn_settings")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = KineticTextSecondary, modifier = Modifier.size(19.dp))
                    }

                    Box {
                        IconButton(onClick = { showOverflowMenu = true }, modifier = Modifier.size(44.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = KineticTextSecondary, modifier = Modifier.size(19.dp))
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                            modifier = Modifier.background(Color(0xFF141824))
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (uiState.isSnapToGrid) "Disable snap align" else "Enable snap align", color = Color.White, fontSize = 12.sp) },
                                onClick = { showOverflowMenu = false; viewModel.toggleSnapToGrid() }
                            )
                            DropdownMenuItem(
                                text = { Text(if (uiState.isLinkingMode) "Exit thread link mode" else "Thread linking mode", color = Color.White, fontSize = 12.sp) },
                                onClick = { showOverflowMenu = false; viewModel.toggleLinkingMode() }
                            )
                            DropdownMenuItem(
                                text = { Text("Fit notes to screen", color = Color.White, fontSize = 12.sp) },
                                onClick = { showOverflowMenu = false; viewModel.fitNotesToViewport() }
                            )
                            DropdownMenuItem(
                                text = { Text("Auto-arrange board", color = Color.White, fontSize = 12.sp) },
                                onClick = { showOverflowMenu = false; viewModel.autoSortBoard() }
                            )
                            DropdownMenuItem(
                                text = { Text("Quick create note type…", color = KineticSecondary, fontSize = 12.sp) },
                                onClick = { showOverflowMenu = false; showCreateDialog = true }
                            )
                        }
                    }
                }
            }

            // Bottom Controls Bar (Zoom HUD is on bottom-left; AI Sparkle + Primary FAB are on bottom-right)
            if (!uiState.isFocusSheetOpen) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(end = 16.dp, bottom = 12.dp)
                ) {
                    // Secondary Compact AI Sparkle Button
                    Surface(
                        color = Color(0xFF131722).copy(alpha = 0.94f),
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x35A855F7)),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .size(46.dp)
                            .clickable { viewModel.toggleAskGeminiExpanded() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "AI Studio",
                                tint = Color(0xFFA855F7),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Primary Persistent "+ New Note" FAB
                    FloatingActionButton(
                        onClick = { viewModel.createNewNoteAtViewportCenter() },
                        containerColor = KineticPrimary,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("fab_new_note")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "New Note", modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Note", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Floating "Ask Gemini" Bottom Capsule Bar (Anchored above FAB when expanded)
            if (uiState.isAskGeminiExpanded && !uiState.isFocusSheetOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp)
                ) {
                    AskGeminiBottomBar(
                        isExpanded = true,
                        isLoading = uiState.isAiLoading,
                        aiOutput = uiState.aiSummaryOutput,
                        onToggleExpand = { viewModel.toggleAskGeminiExpanded() },
                        onAutoSort = { viewModel.autoSortBoard() },
                        onSynthesizeSpace = { viewModel.synthesizeActiveSpace() },
                        onBeautifyCurrentNote = { viewModel.beautifyCurrentNote() },
                        onSubmitQuery = { viewModel.handleAskGeminiQuery(it) }
                    )
                }
            }

            // Dedicated Full-Screen Native Editor
            if (uiState.isFocusSheetOpen && uiState.selectedNote != null) {
                uiState.selectedNote?.let { note ->
                    FullScreenNoteEditorScreen(
                        note = note,
                        checklistItems = uiState.activeChecklist,
                        suggestedLinks = uiState.aiSuggestedLinks,
                        aiSummaryOutput = uiState.aiSummaryOutput,
                        isAiLoading = uiState.isAiLoading,
                        availableFolders = customFolders,
                        saveStatus = uiState.editorSave.status,
                        onClose = { viewModel.closeFocusSheet() },
                        onDraftChanged = { viewModel.markEditorDirty() },
                        onSaveDraft = { title, content ->
                            viewModel.saveEditorDraft(note.id, title, content)
                        },
                        onSaveAndClose = { title, content ->
                            viewModel.saveEditorDraftAndClose(note.id, title, content)
                        },
                        onUpdateNote = { viewModel.updateSelectedNote(it) },
                        onMoveToGroup = { groupId -> viewModel.moveNoteToGroup(note.id, groupId) },
                        onDeleteNote = { viewModel.deleteSelectedNote() },
                        onTogglePin = { viewModel.togglePin(note) },
                        onToggleLock = { viewModel.toggleLock(note) },
                        onWikiLinkClick = { title -> viewModel.openNoteByTitle(title) },
                        onAddChecklistItem = { text -> viewModel.addChecklistItem(text) },
                        onToggleChecklistItem = { item -> viewModel.toggleChecklistItem(item) },
                        onDeleteChecklistItem = { item -> viewModel.deleteChecklistItem(item) },
                        onBeautifyNote = { viewModel.beautifyCurrentNote() },
                        onRequestAiLinks = { viewModel.requestAiLinkSuggestions() },
                        onAcceptAiLink = { suggestion -> viewModel.acceptAiLink(suggestion) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Quick Create Card Dialog
            if (showCreateDialog) {
                AlertDialog(
                    onDismissRequest = { showCreateDialog = false },
                    title = {
                        Text(
                            text = "Add Spatial Card",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CreateCardTypeRow(
                                title = "Text / Spec Card",
                                subtitle = "Architecture spec with [[WikiLinks]]",
                                icon = Icons.Default.Description,
                                color = Color(0xFF6366F1),
                                onClick = {
                                    showCreateDialog = false
                                    viewModel.createNewNote(NoteType.DOC)
                                }
                            )

                            CreateCardTypeRow(
                                title = "Code Component",
                                subtitle = "Syntax snippet with live copy",
                                icon = Icons.Default.Code,
                                color = Color(0xFF38BDF8),
                                onClick = {
                                    showCreateDialog = false
                                    viewModel.createNewNote(NoteType.CODE)
                                }
                            )

                            CreateCardTypeRow(
                                title = "Action Checklist",
                                subtitle = "Sprint deliverables with progress bar",
                                icon = Icons.Default.Checklist,
                                color = Color(0xFF10B981),
                                onClick = {
                                    showCreateDialog = false
                                    viewModel.createNewNote(NoteType.CHECKLIST)
                                }
                            )

                            CreateCardTypeRow(
                                title = "Design Critique Note",
                                subtitle = "Priority card with due date tracking",
                                icon = Icons.Default.AutoAwesome,
                                color = Color(0xFFEC4899),
                                onClick = {
                                    showCreateDialog = false
                                    viewModel.createNewNote(NoteType.DOC)
                                }
                            )
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showCreateDialog = false }) {
                            Text("Cancel", color = KineticTextSecondary)
                        }
                    },
                    containerColor = Color(0xFF141824),
                    shape = RoundedCornerShape(20.dp)
                )
            }

            // Settings is a full-screen conventional destination, not a hidden menu item.
            if (showSettings) {
                SettingsScreen(
                    groups = customFolders,
                    onBack = { showSettings = false },
                    onCreateGroup = { name, color -> viewModel.createGroup(name, color) },
                    onUpdateGroup = { id, name, color -> viewModel.updateGroup(id, name, color) },
                    onDeleteGroupKeepNotes = { id -> viewModel.deleteGroupKeepNotes(id) },
                    onDeleteGroupAndNotes = { id -> viewModel.deleteGroupAndNotes(id) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun CreateCardTypeRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xFF1A1F2E),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x18FFFFFF)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(subtitle, color = KineticTextMuted, fontSize = 10.sp)
            }
        }
    }
}

private fun parseToolbarColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    KineticSecondary
}
