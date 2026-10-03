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

    var showCreateDialog by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var newGroupColor by remember { mutableStateOf("#6366F1") }
    var folderMenuExpanded by remember { mutableStateOf(false) }
    var editingGroup by remember { mutableStateOf<FolderItem?>(null) }
    var renameGroupName by remember { mutableStateOf("") }
    var showDissolveDialog by remember { mutableStateOf(false) }
    var showDeleteGroupDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userNotice) {
        uiState.userNotice?.let { notice ->
            snackbarHostState.showSnackbar(notice)
            viewModel.dismissNotice()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        onResetZoom = { viewModel.resetCanvasView() },
                        onCardMove = { id, x, y ->
                            viewModel.moveCardOnCanvas(id, x, y)
                        },
                        onNoteClick = { note ->
                            viewModel.openNote(note)
                        },
                        onFocusClick = { note ->
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
                        selectedFolder = uiState.selectedFolder,
                        folders = customFolders,
                        unlockedNoteIds = uiState.unlockedNoteIds,
                        onSearchChange = { viewModel.setSearchQuery(it) },
                        onFolderSelect = { viewModel.setSelectedFolder(it) },
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

            // Top Header Ribbon matching the reference video and HTML canvas
            Surface(
                color = Color(0xFF0C1017).copy(alpha = 0.96f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x18FFFFFF)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // KC Squircle Avatar Badge
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF6366F1), Color(0xFFA855F7), Color(0xFF06B6D4))
                                )
                            )
                            .padding(1.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF0D1018), RoundedCornerShape(9.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "KC",
                                color = Color(0xFF818CF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Title & Version Badge
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Kinetic Canvas",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.2).sp
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Surface(
                                color = Color(0x2006B6D4),
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x4006B6D4))
                            ) {
                                Text(
                                    text = "v4.0 Pro",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = "Spatial Board • ${filteredNotes.size} cards",
                            color = KineticTextMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Folder / Spaces Switcher Dropdown
                    Box {
                        Surface(
                            color = Color(0xFF131722),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x24FFFFFF)),
                            modifier = Modifier.clickable { folderMenuExpanded = true }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = uiState.selectedFolder,
                                    color = KineticTextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Folder",
                                    tint = KineticTextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = folderMenuExpanded,
                            onDismissRequest = { folderMenuExpanded = false },
                            modifier = Modifier
                                .background(Color(0xFF131722))
                                .border(1.dp, Color(0x20FFFFFF), RoundedCornerShape(8.dp))
                        ) {
                            customFolders.forEach { folder ->
                                val count = if (folder.name == "All Notes") allNotes.size else allNotes.count {
                                    it.folder.equals(folder.name, ignoreCase = true) || it.tag.equals(folder.name, ignoreCase = true)
                                }
                                val folderCol = try {
                                    Color(android.graphics.Color.parseColor(folder.colorHex))
                                } catch (e: Exception) {
                                    KineticSecondary
                                }

                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            if (folder.name != "All Notes") {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(folderCol)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }
                                            Text(
                                                text = folder.name,
                                                color = if (uiState.selectedFolder == folder.name) KineticSecondary else KineticTextPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = if (uiState.selectedFolder == folder.name) FontWeight.Bold else FontWeight.Normal
                                            )
                                            Spacer(modifier = Modifier.weight(1f))
                                            Text(
                                                text = "$count",
                                                color = KineticTextMuted,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    },
                                    onClick = {
                                        viewModel.setSelectedFolder(folder.name)
                                        folderMenuExpanded = false
                                    }
                                )
                            }

                            HorizontalDivider(color = Color(0x18FFFFFF))

                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "New Group",
                                            tint = KineticSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "+ New Group",
                                            color = KineticSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                },
                                onClick = {
                                    folderMenuExpanded = false
                                    newGroupName = ""
                                    showCreateGroupDialog = true
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Thread Linking Mode Toggle Button
                    IconButton(
                        onClick = { viewModel.toggleLinkingMode() },
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                if (uiState.isLinkingMode) Color(0x3506B6D4) else Color(0x14FFFFFF),
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                if (uiState.isLinkingMode) KineticSecondary else Color(0x20FFFFFF),
                                RoundedCornerShape(8.dp)
                            )
                            .testTag("btn_top_thread_link")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Thread Linking Mode",
                            tint = if (uiState.isLinkingMode) KineticSecondary else KineticTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // View Mode Switcher (Canvas, Grid, Graph)
                    Surface(
                        color = Color(0xFF121622),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1AFFFFFF))
                    ) {
                        Row(modifier = Modifier.padding(2.dp)) {
                            IconButton(
                                onClick = { viewModel.setViewMode(VaultViewMode.CANVAS) },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        if (uiState.viewMode == VaultViewMode.CANVAS) Color(0x30FFFFFF) else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ViewQuilt,
                                    contentDescription = "Canvas",
                                    tint = if (uiState.viewMode == VaultViewMode.CANVAS) Color.White else KineticTextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.setViewMode(VaultViewMode.GRID) },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        if (uiState.viewMode == VaultViewMode.GRID) Color(0x30FFFFFF) else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GridView,
                                    contentDescription = "Grid",
                                    tint = if (uiState.viewMode == VaultViewMode.GRID) Color.White else KineticTextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.setViewMode(VaultViewMode.GRAPH) },
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        if (uiState.viewMode == VaultViewMode.GRAPH) Color(0x30FFFFFF) else Color.Transparent,
                                        RoundedCornerShape(6.dp)
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Hub,
                                    contentDescription = "Graph",
                                    tint = if (uiState.viewMode == VaultViewMode.GRAPH) Color(0xFFA855F7) else KineticTextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // Overflow Menu Button
                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More", tint = KineticTextSecondary, modifier = Modifier.size(18.dp))
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false },
                            modifier = Modifier
                                .background(Color(0xFF141824))
                                .border(1.dp, Color(0x20FFFFFF), RoundedCornerShape(8.dp))
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (uiState.isSnapToGrid) "📐 Disable Snap Align" else "📐 Enable Snap Align",
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.toggleSnapToGrid()
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (uiState.isLinkingMode) "🔗 Exit Thread Link Mode" else "🔗 Thread Linking Mode",
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.toggleLinkingMode()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("✨ Auto-Arrange Board", color = Color.White, fontSize = 12.sp) },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.autoSortBoard()
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("📝 Quick Create Note Type...", color = KineticSecondary, fontSize = 12.sp) },
                                onClick = {
                                    showOverflowMenu = false
                                    showCreateDialog = true
                                }
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
                        .padding(end = 16.dp, bottom = 24.dp)
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
                        onClose = { viewModel.closeFocusSheet() },
                        onUpdateNote = { viewModel.updateSelectedNote(it) },
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

            // Create Custom Group / Folder Dialog
            if (showCreateGroupDialog) {
                val groupColors = listOf("#6366F1", "#38BDF8", "#10B981", "#EC4899", "#8B5CF6", "#F59E0B")
                AlertDialog(
                    onDismissRequest = { showCreateGroupDialog = false },
                    title = {
                        Text(
                            text = "New Note Group",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Create a custom group space to categorize and organize your notes on the canvas.",
                                color = KineticTextMuted,
                                fontSize = 12.sp
                            )

                            OutlinedTextField(
                                value = newGroupName,
                                onValueChange = { newGroupName = it },
                                placeholder = { Text("Group Name (e.g. Design, Ideas)", color = KineticTextMuted, fontSize = 13.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF0F131D),
                                    unfocusedContainerColor = Color(0xFF0F131D),
                                    focusedBorderColor = KineticPrimary,
                                    unfocusedBorderColor = Color(0x20FFFFFF),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = "Color Accent",
                                color = KineticTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                groupColors.forEach { hex ->
                                    val col = Color(android.graphics.Color.parseColor(hex))
                                    val isSel = newGroupColor == hex
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(col)
                                            .border(
                                                width = if (isSel) 2.dp else 0.dp,
                                                color = if (isSel) Color.White else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable { newGroupColor = hex }
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (newGroupName.isNotBlank()) {
                                    viewModel.createCustomFolder(newGroupName, newGroupColor)
                                    showCreateGroupDialog = false
                                    newGroupName = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = KineticPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Create Group", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreateGroupDialog = false }) {
                            Text("Cancel", color = KineticTextSecondary)
                        }
                    },
                    containerColor = Color(0xFF141824),
                    shape = RoundedCornerShape(20.dp)
                )
            }

            // Group Management Dialog (Rename, Dissolve, Delete)
            editingGroup?.let { group ->
                val noteCountInGroup = allNotes.count { it.folder.equals(group.name, ignoreCase = true) }
                AlertDialog(
                    onDismissRequest = { editingGroup = null },
                    title = {
                        Text(
                            text = "Manage Group: ${group.name}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(
                                text = "Currently contains $noteCountInGroup note${if (noteCountInGroup == 1) "" else "s"}.",
                                color = KineticTextMuted,
                                fontSize = 12.sp
                            )

                            OutlinedTextField(
                                value = renameGroupName,
                                onValueChange = { renameGroupName = it },
                                label = { Text("Rename Group", color = KineticTextMuted, fontSize = 11.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF0F131D),
                                    unfocusedContainerColor = Color(0xFF0F131D),
                                    focusedBorderColor = KineticPrimary,
                                    unfocusedBorderColor = Color(0x20FFFFFF),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Dissolve Action (Ordinary safe delete)
                            Surface(
                                color = Color(0x18F59E0B),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x40F59E0B)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDissolveDialog = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Dissolve Group", color = Color(0xFFFBBF24), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Removes the group container. All notes inside are kept.", color = KineticTextMuted, fontSize = 10.sp)
                                    }
                                }
                            }

                            // Delete Group + Notes Action (Destructive)
                            Surface(
                                color = Color(0x18EF4444),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x40EF4444)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showDeleteGroupDialog = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Delete Group + Notes", color = Color(0xFFF87171), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("Permanently deletes this group and its $noteCountInGroup notes.", color = KineticTextMuted, fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (renameGroupName.isNotBlank() && renameGroupName != group.name) {
                                    viewModel.renameGroup(group.name, renameGroupName)
                                }
                                editingGroup = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = KineticPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Save", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { editingGroup = null }) {
                            Text("Close", color = KineticTextSecondary)
                        }
                    },
                    containerColor = Color(0xFF141824),
                    shape = RoundedCornerShape(20.dp)
                )
            }

            // Dissolve Group Confirmation Dialog (Ordinary safe group removal)
            if (showDissolveDialog && editingGroup != null) {
                AlertDialog(
                    onDismissRequest = { showDissolveDialog = false },
                    title = {
                        Text("Dissolve Group?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    },
                    text = {
                        Text(
                            "Dissolve this group? The notes inside will be kept.",
                            color = KineticTextSecondary,
                            fontSize = 13.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                editingGroup?.let { viewModel.dissolveGroup(it.name) }
                                showDissolveDialog = false
                                editingGroup = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Dissolve", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDissolveDialog = false }) {
                            Text("Cancel", color = KineticTextSecondary)
                        }
                    },
                    containerColor = Color(0xFF141824),
                    shape = RoundedCornerShape(20.dp)
                )
            }

            // Delete Group + Notes Confirmation Dialog (Destructive)
            if (showDeleteGroupDialog && editingGroup != null) {
                val count = allNotes.count { it.folder.equals(editingGroup?.name, ignoreCase = true) }
                AlertDialog(
                    onDismissRequest = { showDeleteGroupDialog = false },
                    title = {
                        Text("Delete Group & Notes?", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    },
                    text = {
                        Text(
                            "This will permanently delete '${editingGroup?.name}' and its $count note(s). This cannot be undone.",
                            color = KineticTextSecondary,
                            fontSize = 13.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                editingGroup?.let { viewModel.deleteGroupAndNotes(it.name) }
                                showDeleteGroupDialog = false
                                editingGroup = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Delete All", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteGroupDialog = false }) {
                            Text("Cancel", color = KineticTextSecondary)
                        }
                    },
                    containerColor = Color(0xFF141824),
                    shape = RoundedCornerShape(20.dp)
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
