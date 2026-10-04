package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ChecklistItemEntity
import com.example.data.local.NoteDatabase
import com.example.data.local.NoteEntity
import com.example.data.local.NoteGroupEntity
import com.example.data.local.NoteLinkEntity
import com.example.data.local.NoteType
import com.example.data.repository.LinkSuggestion
import com.example.data.repository.NoteRepository
import com.example.domain.physics.ForceDirectedGraphEngine
import com.example.domain.physics.GraphState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class VaultViewMode {
    CANVAS, // Spatial Board (Default Home!)
    GRID,   // Samsung Notes Clean Grid
    GRAPH   // Obsidian Knowledge Graph
}

/**
 * UI projection of a [NoteGroupEntity]. [id] is `null` for the synthetic "All Notes" entry,
 * which is a filter rather than a stored group.
 */
data class FolderItem(
    val id: String?,
    val name: String,
    val colorHex: String,
    val icon: String = "📁",
    val isCustom: Boolean = true
) {
    companion object {
        val AllNotes = FolderItem(id = null, name = "All Notes", colorHex = "#818CF8", icon = "📂", isCustom = false)
    }
}

/**
 * Durability state of the note currently open in the editor.
 *
 * Contract:
 * typing -> [UNSAVED] (editor-local draft only, no database write)
 * debounce elapsed / Save pressed / Back pressed -> [SAVING] -> [SAVED]
 * write failed -> [ERROR] (draft is kept in the editor so nothing is lost)
 */
enum class EditorSaveStatus {
    SAVED,
    UNSAVED,
    SAVING,
    ERROR
}

data class EditorSaveState(
    val status: EditorSaveStatus = EditorSaveStatus.SAVED,
    val lastSavedAt: Long? = null,
    val errorMessage: String? = null
)

enum class AiPreviewKind {
    BOARD_LAYOUT,
    NOTE_CONTENT
}

data class AiPreview(
    val kind: AiPreviewKind,
    val title: String,
    val description: String,
    val noteId: String? = null,
    val originalContent: String? = null,
    val proposedContent: String? = null,
    val plannedPositions: Map<String, Pair<Float, Float>> = emptyMap()
)

data class AiUndo(
    val kind: AiPreviewKind,
    val noteId: String? = null,
    val previousContent: String? = null,
    val previousPositions: Map<String, Pair<Float, Float>> = emptyMap()
)

data class NotesUiState(
    val viewMode: VaultViewMode = VaultViewMode.CANVAS, // Default: Spatial Board
    /** `null` means "All Notes" (no group filter). */
    val selectedGroupId: String? = null,
    val searchQuery: String = "",
    val recentSearches: List<String> = emptyList(),
    val selectedTag: String? = null,
    val selectedNote: NoteEntity? = null,
    val isFocusSheetOpen: Boolean = false,
    val isLinkingMode: Boolean = false,
    val linkSourceNoteId: String? = null,
    val isSnapToGrid: Boolean = true,
    val canvasZoom: Float = 0.85f, // Clean default zoom
    val canvasPanX: Float = 20f,
    val canvasPanY: Float = 40f,
    /** Measured size of the board viewport in screen px (0 until the canvas reports it). */
    val viewportWidthPx: Float = 0f,
    val viewportHeightPx: Float = 0f,
    val unlockedNoteIds: Set<String> = emptySet(),
    val isAiLoading: Boolean = false,
    val aiSuggestedLinks: List<LinkSuggestion> = emptyList(),
    val aiSummaryOutput: String? = null,
    val aiPreview: AiPreview? = null,
    val canUndoAiChange: Boolean = false,
    val activeChecklist: List<ChecklistItemEntity> = emptyList(),
    val userNotice: String? = null,
    val isAskGeminiExpanded: Boolean = false,
    val editorSave: EditorSaveState = EditorSaveState()
)

class NotesViewModel(
    application: Application,
    injectedRepository: NoteRepository?
) : AndroidViewModel(application) {

    companion object {
        const val DEFAULT_PAN_X = 20f
        const val DEFAULT_PAN_Y = 40f
        const val DEFAULT_ZOOM = 0.85f
        const val FIT_NOTES_PADDING_PX = 96f
    }

    constructor(application: Application) : this(application, null)

    private val database = NoteDatabase.getDatabase(application, viewModelScope)
    private val repository = injectedRepository ?: NoteRepository(database.noteDao())
    val graphEngine = ForceDirectedGraphEngine(viewModelScope)

    val allNotes: StateFlow<List<NoteEntity>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLinks: StateFlow<List<NoteLinkEntity>> = repository.allLinks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val graphState: StateFlow<GraphState> = graphEngine.graphState

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()
    private var lastAiUndo: AiUndo? = null
    private val deletingNoteIds = mutableSetOf<String>()

    /** Durable groups, straight from Room. */
    val groups: StateFlow<List<NoteGroupEntity>> = repository.allGroups
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** "All Notes" + every durable group, as consumed by the toolbar / grid / editor. */
    val customFolders: StateFlow<List<FolderItem>> = groups
        .map { list ->
            listOf(FolderItem.AllNotes) + list.map {
                FolderItem(id = it.id, name = it.name, colorHex = it.colorHex, icon = it.icon, isCustom = true)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, listOf(FolderItem.AllNotes))

    /** Display name of the active group filter. */
    val selectedGroupName: StateFlow<String> = combine(groups, _uiState) { list, state ->
        list.firstOrNull { it.id == state.selectedGroupId }?.name ?: "All Notes"
    }.stateIn(viewModelScope, SharingStarted.Eagerly, "All Notes")

    // Filtered notes based on active group, tag, and search
    val filteredNotes: StateFlow<List<NoteEntity>> = combine(
        allNotes,
        _uiState
    ) { notes, state ->
        notes.filter { note ->
            val canInspectProtectedText = !note.isLocked || note.id in state.unlockedNoteIds
            val matchesGroup = state.selectedGroupId == null || note.groupId == state.selectedGroupId
            val matchesTag = state.selectedTag == null ||
                    (canInspectProtectedText && note.tag.equals(state.selectedTag, ignoreCase = true))
            val matchesSearch = state.searchQuery.isBlank() ||
                    (canInspectProtectedText && (
                        note.title.contains(state.searchQuery, ignoreCase = true) ||
                            note.content.contains(state.searchQuery, ignoreCase = true) ||
                            note.tag.contains(state.searchQuery, ignoreCase = true) ||
                            note.folder.contains(state.searchQuery, ignoreCase = true) ||
                            note.type.name.contains(state.searchQuery, ignoreCase = true)
                        ))
            matchesGroup && matchesTag && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // NOTE: demo content is seeded exactly once, by NoteDatabase.onCreate, for a brand new
        // database file. The ViewModel deliberately does NOT re-seed an empty table - deleting
        // every note used to resurrect the sample notes on the next launch.
        viewModelScope.launch {
            combine(allNotes, allLinks, _uiState) { notes, links, state ->
                val eligibleIds = notes
                    .filter { !it.isLocked || it.id in state.unlockedNoteIds }
                    .map { it.id }
                    .toSet()
                val eligibleNotes = notes.filter { it.id in eligibleIds }
                val eligibleLinks = links.filter { it.sourceId in eligibleIds && it.targetId in eligibleIds }
                Pair(eligibleNotes, eligibleLinks)
            }.collect { (notes, links) ->
                graphEngine.updateGraph(notes, links)
            }
        }
    }

    fun setViewMode(mode: VaultViewMode) {
        _uiState.value = _uiState.value.copy(viewMode = mode)
    }

    /** Refresh graph physics bounds after Compose measures the browsing viewport. */
    fun updateGraphBounds(width: Float, height: Float) {
        graphEngine.updateBounds(width, height)
        val eligibleNotes = allNotes.value.filter(::isAiEligible)
        val eligibleIds = eligibleNotes.map { it.id }.toSet()
        val eligibleLinks = allLinks.value.filter {
            it.sourceId in eligibleIds && it.targetId in eligibleIds
        }
        graphEngine.updateGraph(eligibleNotes, eligibleLinks)
    }

    fun selectGroup(groupId: String?) {
        _uiState.update { it.copy(selectedGroupId = groupId, selectedTag = null) }
    }

    fun createGroup(name: String, colorHex: String, icon: String = "📁") {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val existing = repository.getGroupByName(trimmed)
            if (existing != null) {
                _uiState.update {
                    it.copy(selectedGroupId = existing.id, userNotice = "Group '$trimmed' already exists")
                }
                return@launch
            }
            val created = repository.createGroup(trimmed, colorHex, icon)
            _uiState.update {
                it.copy(selectedGroupId = created.id, userNotice = "Group '$trimmed' created")
            }
        }
    }

    /** Rename and/or recolour a group. The group keeps its stable id. */
    fun updateGroup(groupId: String, name: String, colorHex: String, icon: String = "📁") {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val clash = repository.getGroupByName(trimmed)
            if (clash != null && clash.id != groupId) {
                _uiState.update { it.copy(userNotice = "Another group is already called '$trimmed'") }
                return@launch
            }
            repository.updateGroup(groupId, trimmed, colorHex, icon)
            _uiState.update { it.copy(userNotice = "Group updated") }
        }
    }

    /**
     * Normal "Delete group": the group container is removed and every note it held is KEPT,
     * becoming ungrouped (All Notes).
     */
    fun deleteGroupKeepNotes(groupId: String) {
        viewModelScope.launch {
            val name = repository.getGroupById(groupId)?.name ?: "Group"
            val kept = repository.countNotesInGroup(groupId)
            repository.deleteGroupKeepNotes(groupId)
            _uiState.update {
                it.copy(
                    selectedGroupId = if (it.selectedGroupId == groupId) null else it.selectedGroupId,
                    userNotice = "Deleted '$name'. $kept note${if (kept == 1) "" else "s"} kept in All Notes."
                )
            }
        }
    }

    /** Destructive variant: removes the group AND the notes inside it. */
    fun deleteGroupAndNotes(groupId: String) {
        viewModelScope.launch {
            val name = repository.getGroupById(groupId)?.name ?: "Group"
            val removed = repository.countNotesInGroup(groupId)
            repository.deleteGroupAndNotes(groupId)
            _uiState.update {
                it.copy(
                    selectedGroupId = if (it.selectedGroupId == groupId) null else it.selectedGroupId,
                    userNotice = "Deleted '$name' and $removed note${if (removed == 1) "" else "s"}."
                )
            }
        }
    }

    fun moveNoteToGroup(noteId: String, groupId: String?) {
        viewModelScope.launch {
            repository.moveNoteToGroup(noteId, groupId)
            val refreshed = repository.getNoteDirect(noteId)
            _uiState.update { state ->
                if (state.selectedNote?.id == noteId && refreshed != null) {
                    state.copy(selectedNote = refreshed)
                } else {
                    state
                }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun rememberSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        _uiState.update { state ->
            state.copy(recentSearches = (listOf(trimmed) + state.recentSearches
                .filterNot { it.equals(trimmed, ignoreCase = true) }).take(5))
        }
    }

    fun setSelectedTag(tag: String?) {
        _uiState.value = _uiState.value.copy(
            selectedTag = if (_uiState.value.selectedTag == tag) null else tag
        )
    }

    fun toggleAskGeminiExpanded() {
        _uiState.value = _uiState.value.copy(
            isAskGeminiExpanded = !_uiState.value.isAskGeminiExpanded
        )
    }

    fun openNote(note: NoteEntity) {
        _uiState.value = _uiState.value.copy(
            selectedNote = note,
            isFocusSheetOpen = true,
            aiSuggestedLinks = emptyList(),
            aiSummaryOutput = null,
            aiPreview = null,
            editorSave = EditorSaveState(status = EditorSaveStatus.SAVED)
        )
        viewModelScope.launch {
            loadChecklist(note.id)
        }
    }

    fun openNoteByTitle(title: String) {
        viewModelScope.launch {
            val existing = repository.getNoteByTitle(title)
            if (existing != null) {
                openNote(existing)
            } else {
                val newId = repository.createNote(
                    title = title,
                    content = "# $title\nCreated from [[WikiLink]] reference.\n",
                    type = NoteType.DOC,
                    colorHex = "#6366F1",
                    tag = "Architecture"
                )
                repository.getNoteDirect(newId)?.let { created ->
                    _uiState.value.selectedNote?.let { source ->
                        repository.createLink(source.id, created.id)
                    }
                    openNote(created)
                }
            }
        }
    }

    fun closeFocusSheet() {
        _uiState.update {
            it.copy(
                isFocusSheetOpen = false,
                selectedNote = null,
                aiSuggestedLinks = emptyList(),
                aiSummaryOutput = null,
                aiPreview = null,
                editorSave = EditorSaveState()
            )
        }
    }

    fun createNewNoteAtViewportCenter(type: NoteType = NoteType.DOC) {
        viewModelScope.launch {
            val count = allNotes.value.size
            val (colorHex, defaultTitle, defaultTag) = when (type) {
                NoteType.DOC -> Triple("#6366F1", "New Note", "Architecture")
                NoteType.CODE -> Triple("#38BDF8", "Code Snippet", "Code")
                NoteType.CHECKLIST -> Triple("#10B981", "Action Checklist", "Product")
                NoteType.SKETCH -> Triple("#8B5CF6", "Sketch", "Research")
                NoteType.AUDIO -> Triple("#EC4899", "Voice Memo", "Priority")
            }

            val state = _uiState.value
            // Real measured viewport when the board has reported one, otherwise a sane fallback.
            val viewportWidth = if (state.viewportWidthPx > 0f) state.viewportWidthPx else 1080f
            val viewportHeight = if (state.viewportHeightPx > 0f) state.viewportHeightPx else 2000f
            val cascade = (count % 5) * 18f
            val spawn = com.example.ui.canvas.spawnPositionForNewCard(
                viewport = com.example.ui.canvas.ViewportTransform(
                    panX = state.canvasPanX,
                    panY = state.canvasPanY,
                    zoom = state.canvasZoom
                ),
                viewportWidthPx = viewportWidth,
                viewportHeightPx = viewportHeight,
                cascadeOffsetWorld = cascade
            )

            val groupId = state.selectedGroupId
            val groupName = groupId?.let { repository.getGroupById(it)?.name }

            val id = repository.createNote(
                title = defaultTitle,
                content = if (type == NoteType.CHECKLIST) "Milestone tasks" else "",
                type = type,
                colorHex = colorHex,
                tag = groupName ?: defaultTag,
                groupId = groupId,
                folder = groupName ?: "All Notes",
                x = spawn.x,
                y = spawn.y
            )
            repository.getNoteDirect(id)?.let { note -> openNote(note) }
        }
    }

    /** Reported by the board so new notes and Fit Notes can use the real viewport. */
    fun onViewportMeasured(widthPx: Float, heightPx: Float) {
        if (widthPx <= 0f || heightPx <= 0f) return
        val state = _uiState.value
        if (state.viewportWidthPx == widthPx && state.viewportHeightPx == heightPx) return
        _uiState.update { it.copy(viewportWidthPx = widthPx, viewportHeightPx = heightPx) }
    }

    fun createNewNote(type: NoteType = NoteType.DOC) {
        createNewNoteAtViewportCenter(type)
    }

    /**
     * Whole-entity save used for editor metadata changes (colour, group, pin, type...).
     * Text edits must go through [saveEditorDraft] instead so that individual keystrokes
     * never hit the database.
     */
    fun updateSelectedNote(updated: NoteEntity) {
        viewModelScope.launch {
            _uiState.update { it.copy(selectedNote = updated) }
            repository.saveNote(updated)
        }
    }

    /**
     * Called by the editor the first time a draft diverges from what is on disk.
     * Pure UI state - deliberately performs no database work.
     */
    fun markEditorDirty() {
        if (_uiState.value.editorSave.status == EditorSaveStatus.UNSAVED) return
        _uiState.update {
            it.copy(editorSave = it.editorSave.copy(status = EditorSaveStatus.UNSAVED, errorMessage = null))
        }
    }

    /**
     * Durable flush of the editor draft: debounced autosave, the explicit Save button and
     * Back/Done all funnel through here.
     *
     * @param onSaved invoked on the main dispatcher once the write has completed (or failed),
     *   which is what makes "Back flushes pending changes before leaving" safe.
     */
    fun saveEditorDraft(
        noteId: String,
        title: String,
        content: String,
        onSaved: (() -> Unit)? = null
    ) {
        _uiState.update { it.copy(editorSave = it.editorSave.copy(status = EditorSaveStatus.SAVING)) }
        viewModelScope.launch {
            val savedAt = System.currentTimeMillis()
            try {
                val rows = repository.saveNoteText(id = noteId, title = title, content = content, updatedAt = savedAt)
                check(rows == 1) { "Note no longer exists" }
                _uiState.update { state ->
                    val selected = state.selectedNote
                    state.copy(
                        selectedNote = if (selected?.id == noteId) {
                            selected.copy(title = title, content = content, updatedAt = savedAt)
                        } else {
                            selected
                        },
                        editorSave = EditorSaveState(status = EditorSaveStatus.SAVED, lastSavedAt = savedAt)
                    )
                }
                // Only close after a successful durable write. On failure the draft remains
                // in the editor and the visible ERROR state gives the user a retry path.
                onSaved?.invoke()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        editorSave = it.editorSave.copy(
                            status = EditorSaveStatus.ERROR,
                            errorMessage = e.message ?: "Could not save note"
                        )
                    )
                }
            }
        }
    }

    /** Flushes the pending draft and only then leaves the editor. */
    fun saveEditorDraftAndClose(noteId: String, title: String, content: String) {
        saveEditorDraft(noteId, title, content) { closeFocusSheet() }
    }

    private fun deleteNoteAndCloseIfSelected(id: String) {
        // The dialog also guards its confirm button, but keep the invariant at the ViewModel
        // boundary so multiple UI entry points cannot dispatch duplicate destructive work.
        if (!deletingNoteIds.add(id)) return
        viewModelScope.launch {
            try {
                repository.deleteNote(id)
                if (_uiState.value.selectedNote?.id == id) {
                    closeFocusSheet()
                }
                _uiState.update { it.copy(userNotice = "Note deleted.") }
            } catch (error: Exception) {
                // Keep the editor mounted on failure; its local draft remains intact and the
                // snackbar gives the user a visible retry/error path.
                _uiState.update {
                    it.copy(userNotice = "Could not delete note: ${error.message ?: "unknown error"}")
                }
            } finally {
                deletingNoteIds.remove(id)
            }
        }
    }

    fun deleteSelectedNote() {
        _uiState.value.selectedNote?.let { deleteNoteAndCloseIfSelected(it.id) }
    }

    fun deleteNoteById(id: String) {
        deleteNoteAndCloseIfSelected(id)
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            repository.togglePin(note.id, note.isPinned)
            if (_uiState.value.selectedNote?.id == note.id) {
                _uiState.value = _uiState.value.copy(
                    selectedNote = _uiState.value.selectedNote?.copy(isPinned = !note.isPinned)
                )
            }
        }
    }

    fun toggleLock(note: NoteEntity) {
        viewModelScope.launch {
            repository.toggleLock(note.id, note.isLocked)
            val updatedUnlocked = _uiState.value.unlockedNoteIds.toMutableSet()
            if (note.isLocked) {
                updatedUnlocked.add(note.id)
            } else {
                updatedUnlocked.remove(note.id)
            }
            _uiState.value = _uiState.value.copy(unlockedNoteIds = updatedUnlocked)
        }
    }

    fun unlockNoteTemporary(noteId: String) {
        val updated = _uiState.value.unlockedNoteIds + noteId
        _uiState.value = _uiState.value.copy(unlockedNoteIds = updated)
    }

    // Canvas Transformations
    fun updateCanvasTransform(centroidScreen: androidx.compose.ui.geometry.Offset, panDeltaScreen: androidx.compose.ui.geometry.Offset, zoomChange: Float) {
        val currentViewport = com.example.ui.canvas.ViewportTransform(
            panX = _uiState.value.canvasPanX,
            panY = _uiState.value.canvasPanY,
            zoom = _uiState.value.canvasZoom
        )
        val newViewport = currentViewport.withAnchoredTransform(
            centroidScreen = centroidScreen,
            panDeltaScreen = panDeltaScreen,
            zoomChange = zoomChange
        )
        _uiState.value = _uiState.value.copy(
            canvasPanX = newViewport.panX,
            canvasPanY = newViewport.panY,
            canvasZoom = newViewport.zoom
        )
    }

    fun updateCanvasTransform(panDeltaX: Float, panDeltaY: Float, zoomChange: Float) {
        updateCanvasTransform(
            centroidScreen = androidx.compose.ui.geometry.Offset.Zero,
            panDeltaScreen = androidx.compose.ui.geometry.Offset(panDeltaX, panDeltaY),
            zoomChange = zoomChange
        )
    }

    fun zoomIn() {
        val newZoom = (_uiState.value.canvasZoom + 0.15f).coerceAtMost(com.example.ui.canvas.SpatialGridConfig.MAX_ZOOM)
        _uiState.value = _uiState.value.copy(canvasZoom = newZoom)
    }

    fun zoomOut() {
        val newZoom = (_uiState.value.canvasZoom - 0.15f).coerceAtLeast(com.example.ui.canvas.SpatialGridConfig.MIN_ZOOM)
        _uiState.value = _uiState.value.copy(canvasZoom = newZoom)
    }

    /**
     * "Fit Notes": frames the notes that are actually on the board right now (respecting the
     * active group/search filter) instead of jumping to a hard-coded pan/zoom.
     * Falls back to the default view when there is nothing to frame.
     */
    fun fitNotesToViewport() {
        val state = _uiState.value
        val notes = filteredNotes.value
        val width = state.viewportWidthPx
        val height = state.viewportHeightPx
        val bounds = com.example.ui.canvas.noteWorldBounds(notes)

        if (bounds == null || width <= 0f || height <= 0f) {
            _uiState.update {
                it.copy(
                    canvasPanX = DEFAULT_PAN_X,
                    canvasPanY = DEFAULT_PAN_Y,
                    canvasZoom = DEFAULT_ZOOM,
                    userNotice = if (notes.isEmpty()) "No notes to fit" else null
                )
            }
            return
        }

        val fitted = com.example.ui.canvas.fitViewportToBounds(
            bounds = bounds,
            viewportWidthPx = width,
            viewportHeightPx = height,
            paddingPx = FIT_NOTES_PADDING_PX
        )
        _uiState.update {
            it.copy(canvasPanX = fitted.panX, canvasPanY = fitted.panY, canvasZoom = fitted.zoom)
        }
    }

    /** True when the user has panned/zoomed away from every visible note. */
    fun areNotesOffScreen(): Boolean {
        val state = _uiState.value
        return !com.example.ui.canvas.isAnyNoteVisible(
            notes = filteredNotes.value,
            viewport = com.example.ui.canvas.ViewportTransform(
                panX = state.canvasPanX,
                panY = state.canvasPanY,
                zoom = state.canvasZoom
            ),
            viewportWidthPx = state.viewportWidthPx,
            viewportHeightPx = state.viewportHeightPx
        )
    }

    fun resetCanvasView() = fitNotesToViewport()

    fun toggleSnapToGrid() {
        _uiState.value = _uiState.value.copy(isSnapToGrid = !_uiState.value.isSnapToGrid)
    }

    fun moveCardOnCanvas(noteId: String, newX: Float, newY: Float) {
        val finalPos = if (_uiState.value.isSnapToGrid) {
            com.example.ui.canvas.snapToWorldGrid(
                androidx.compose.ui.geometry.Offset(newX, newY),
                com.example.ui.canvas.SpatialGridConfig.DEFAULT_GRID_SPACING
            )
        } else {
            androidx.compose.ui.geometry.Offset(newX, newY)
        }

        viewModelScope.launch {
            repository.updateNotePosition(noteId, finalPos.x, finalPos.y)
        }
    }

    // Thread Linking
    fun toggleLinkingMode() {
        val nextMode = !_uiState.value.isLinkingMode
        _uiState.value = _uiState.value.copy(
            isLinkingMode = nextMode,
            linkSourceNoteId = null,
            userNotice = if (nextMode) "Thread Mode: Tap 1st note, then 2nd note" else null
        )
    }

    fun onCardTappedInLinkingMode(noteId: String) {
        val sourceId = _uiState.value.linkSourceNoteId
        if (sourceId == null) {
            _uiState.value = _uiState.value.copy(
                linkSourceNoteId = noteId,
                userNotice = "Tap target note to connect thread"
            )
        } else if (sourceId != noteId) {
            viewModelScope.launch {
                val sourceNote = allNotes.value.find { it.id == sourceId }
                repository.createLink(sourceId, noteId, sourceNote?.colorHex ?: "#6366F1")
                _uiState.value = _uiState.value.copy(
                    isLinkingMode = false,
                    linkSourceNoteId = null,
                    userNotice = "Dynamic thread linked! ⚡"
                )
            }
        }
    }

    fun dismissNotice() {
        _uiState.value = _uiState.value.copy(userNotice = null)
    }

    // Checklist
    private fun loadChecklist(noteId: String) {
        viewModelScope.launch {
            repository.getChecklistItems(noteId).collect { items ->
                _uiState.value = _uiState.value.copy(activeChecklist = items)
            }
        }
    }

    fun addChecklistItem(text: String) {
        val note = _uiState.value.selectedNote ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.addChecklistItem(note.id, text.trim())
        }
    }

    fun toggleChecklistItem(item: ChecklistItemEntity) {
        viewModelScope.launch {
            repository.toggleChecklistItem(item)
        }
    }

    fun deleteChecklistItem(item: ChecklistItemEntity) {
        viewModelScope.launch {
            repository.deleteChecklistItem(item.id)
        }
    }

    // Gemini AI Features
    /** Protected notes are not eligible for provider prompts until a real authenticated
     * protection session exists. The current Boolean lock is only a UI label. */
    private fun isAiEligible(note: NoteEntity): Boolean =
        !note.isLocked || note.id in _uiState.value.unlockedNoteIds

    private fun aiEligibleNotes(): List<NoteEntity> = allNotes.value.filter(::isAiEligible)

    private fun rejectProtectedAiAction() {
        _uiState.update {
            it.copy(
                isAiLoading = false,
                aiPreview = null,
                userNotice = "Unlock this note before using Assistant"
            )
        }
    }

    /**
     * AI never changes durable notes from a shortcut. It first creates a reviewable plan;
     * the user must explicitly Apply it. This matters especially for spatial layouts where a
     * surprising arrangement can otherwise be difficult to recover from.
     */
    fun autoSortBoard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAiLoading = true) }
            val notes = aiEligibleNotes()
            val plannedPositions = notes.mapIndexed { index, note ->
                val col = index % 2
                val row = index / 2
                note.id to Pair(40f + col * 260f, 70f + row * 220f)
            }.toMap()

            _uiState.update {
                it.copy(
                    isAiLoading = false,
                    aiPreview = AiPreview(
                        kind = AiPreviewKind.BOARD_LAYOUT,
                        title = "Suggested board layout",
                        description = "Preview a two-column arrangement for ${notes.size} card${if (notes.size == 1) "" else "s"}. Nothing has moved yet.",
                        plannedPositions = plannedPositions
                    ),
                    canUndoAiChange = false,
                    isAskGeminiExpanded = true
                )
            }
        }
    }

    fun beautifyCurrentNote() {
        val note = _uiState.value.selectedNote ?: return
        if (!isAiEligible(note)) {
            rejectProtectedAiAction()
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isAiLoading = true) }
            val result = repository.beautifyNoteContent(note.title, note.content)
            result.fold(
                onSuccess = { beautified ->
                    _uiState.update {
                        it.copy(
                            isAiLoading = false,
                            aiPreview = AiPreview(
                                kind = AiPreviewKind.NOTE_CONTENT,
                                title = "Formatting preview",
                                description = "Review the proposed formatting before it replaces this note.",
                                noteId = note.id,
                                originalContent = note.content,
                                proposedContent = beautified
                            ),
                            canUndoAiChange = false,
                            isAskGeminiExpanded = true
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isAiLoading = false,
                            aiPreview = null,
                            userNotice = error.message ?: "Assistant unavailable"
                        )
                    }
                }
            )
        }
    }

    fun cancelAiPreview() {
        _uiState.update { it.copy(aiPreview = null, isAiLoading = false) }
    }

    fun applyAiPreview() {
        val preview = _uiState.value.aiPreview ?: return
        viewModelScope.launch {
            val previousPositions = allNotes.value.associate { it.id to Pair(it.x, it.y) }
            when (preview.kind) {
                AiPreviewKind.BOARD_LAYOUT -> {
                    preview.plannedPositions.forEach { (id, position) ->
                        repository.updateNotePosition(id, position.first, position.second)
                    }
                    lastAiUndo = AiUndo(
                        kind = preview.kind,
                        previousPositions = previousPositions
                    )
                    _uiState.update { it.copy(userNotice = "Board layout applied. You can undo it from Assistant.") }
                }
                AiPreviewKind.NOTE_CONTENT -> {
                    val note = allNotes.value.firstOrNull { it.id == preview.noteId }
                    val proposed = preview.proposedContent ?: return@launch
                    if (note != null) {
                        repository.saveNoteText(note.id, note.title, proposed)
                        _uiState.update { state ->
                            state.copy(selectedNote = if (state.selectedNote?.id == note.id) note.copy(content = proposed) else state.selectedNote)
                        }
                        lastAiUndo = AiUndo(
                            kind = preview.kind,
                            noteId = note.id,
                            previousContent = preview.originalContent
                        )
                        _uiState.update { it.copy(userNotice = "Formatting applied. You can undo it from Assistant.") }
                    }
                }
            }
            _uiState.update { it.copy(aiPreview = null, canUndoAiChange = true, isAiLoading = false) }
        }
    }

    fun undoAiChange() {
        val undo = lastAiUndo ?: return
        viewModelScope.launch {
            when (undo.kind) {
                AiPreviewKind.BOARD_LAYOUT -> undo.previousPositions.forEach { (id, position) ->
                    repository.updateNotePosition(id, position.first, position.second)
                }
                AiPreviewKind.NOTE_CONTENT -> {
                    val note = allNotes.value.firstOrNull { it.id == undo.noteId }
                    val content = undo.previousContent
                    if (note != null && content != null) {
                        repository.saveNoteText(note.id, note.title, content)
                        _uiState.update { state ->
                            state.copy(selectedNote = if (state.selectedNote?.id == note.id) note.copy(content = content) else state.selectedNote)
                        }
                    }
                }
            }
            lastAiUndo = null
            _uiState.update { it.copy(canUndoAiChange = false, userNotice = "Assistant change undone") }
        }
    }

    fun synthesizeActiveSpace() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAiLoading = true) }
            val result = repository.synthesizeActiveSpace(
                selectedGroupName.value,
                filteredNotes.value.filter { !it.isLocked || it.id in _uiState.value.unlockedNoteIds }
            )
            result.fold(
                onSuccess = { summary ->
                    _uiState.update {
                        it.copy(
                            isAiLoading = false,
                            aiSummaryOutput = summary,
                            isAskGeminiExpanded = true,
                            userNotice = "Active space synthesized by Assistant"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isAiLoading = false,
                            aiSummaryOutput = null,
                            userNotice = error.message ?: "Assistant unavailable"
                        )
                    }
                }
            )
        }
    }

    fun handleAskGeminiQuery(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAiLoading = true)
            when {
                query.contains("sort", ignoreCase = true) || query.contains("organize", ignoreCase = true) -> {
                    autoSortBoard()
                }
                query.contains("summarize", ignoreCase = true) || query.contains("synthesis", ignoreCase = true) -> {
                    synthesizeActiveSpace()
                }
                query.contains("tidy", ignoreCase = true) || query.contains("format", ignoreCase = true) -> {
                    if (_uiState.value.selectedNote != null) {
                        beautifyCurrentNote()
                    } else {
                        autoSortBoard()
                    }
                }
                else -> {
                    val result = repository.synthesizeActiveSpace(
                        query,
                        filteredNotes.value.filter { !it.isLocked || it.id in _uiState.value.unlockedNoteIds }
                    )
                    result.fold(
                        onSuccess = { summary ->
                            _uiState.update {
                                it.copy(
                                    isAiLoading = false,
                                    aiSummaryOutput = summary,
                                    isAskGeminiExpanded = true
                                )
                            }
                        },
                        onFailure = { error ->
                            _uiState.update {
                                it.copy(
                                    isAiLoading = false,
                                    aiSummaryOutput = null,
                                    userNotice = error.message ?: "Assistant unavailable"
                                )
                            }
                        }
                    )
                }
            }
        }
    }

    fun requestAiLinkSuggestions() {
        val note = _uiState.value.selectedNote ?: return
        if (!isAiEligible(note)) {
            rejectProtectedAiAction()
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAiLoading = true)
            val result = repository.suggestLinks(note, aiEligibleNotes())
            result.fold(
                onSuccess = { suggestions ->
                    _uiState.update {
                        it.copy(isAiLoading = false, aiSuggestedLinks = suggestions)
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isAiLoading = false,
                            aiSuggestedLinks = emptyList(),
                            userNotice = error.message ?: "Assistant unavailable"
                        )
                    }
                }
            )
        }
    }

    fun acceptAiLink(suggestion: LinkSuggestion) {
        val currentNote = _uiState.value.selectedNote ?: return
        val targetId = suggestion.targetId
        if (targetId == null || allNotes.value.none { it.id == targetId }) {
            _uiState.update {
                it.copy(userNotice = "That Assistant link no longer points to an existing note")
            }
            return
        }
        viewModelScope.launch {
            try {
                repository.createLink(currentNote.id, targetId)
                _uiState.update {
                    it.copy(
                        aiSuggestedLinks = it.aiSuggestedLinks.filter { item -> item.targetId != targetId },
                        userNotice = "Connected thread to [[${suggestion.targetTitle}]]"
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(userNotice = error.message ?: "Could not create Assistant link")
                }
            }
        }
    }
}
