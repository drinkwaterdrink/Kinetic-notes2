package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ChecklistItemEntity
import com.example.data.local.NoteDatabase
import com.example.data.local.NoteEntity
import com.example.data.local.NoteLinkEntity
import com.example.data.local.NoteType
import com.example.data.local.SampleData
import com.example.data.repository.LinkSuggestion
import com.example.data.repository.NoteRepository
import com.example.domain.physics.ForceDirectedGraphEngine
import com.example.domain.physics.GraphState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class VaultViewMode {
    CANVAS, // Spatial Board (Default Home!)
    GRID,   // Samsung Notes Clean Grid
    GRAPH   // Obsidian Knowledge Graph
}

data class FolderItem(
    val name: String,
    val colorHex: String,
    val icon: String = "📁",
    val isCustom: Boolean = false
)

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

data class NotesUiState(
    val viewMode: VaultViewMode = VaultViewMode.CANVAS, // Default: Spatial Board
    val selectedFolder: String = "All Notes",
    val searchQuery: String = "",
    val selectedTag: String? = null,
    val selectedNote: NoteEntity? = null,
    val isFocusSheetOpen: Boolean = false,
    val isLinkingMode: Boolean = false,
    val linkSourceNoteId: String? = null,
    val isSnapToGrid: Boolean = true,
    val canvasZoom: Float = 0.85f, // Clean default zoom
    val canvasPanX: Float = 20f,
    val canvasPanY: Float = 40f,
    val unlockedNoteIds: Set<String> = emptySet(),
    val isAiLoading: Boolean = false,
    val aiSuggestedLinks: List<LinkSuggestion> = emptyList(),
    val aiSummaryOutput: String? = null,
    val activeChecklist: List<ChecklistItemEntity> = emptyList(),
    val userNotice: String? = null,
    val isAskGeminiExpanded: Boolean = false,
    val editorSave: EditorSaveState = EditorSaveState()
)

class NotesViewModel(
    application: Application,
    injectedRepository: NoteRepository?
) : AndroidViewModel(application) {
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

    private val _customFolders = MutableStateFlow<List<FolderItem>>(
        listOf(
            FolderItem("All Notes", "#818CF8", "📂", isCustom = false),
            FolderItem("Architecture", "#6366F1", "🏛️", isCustom = false),
            FolderItem("Product", "#10B981", "🚀", isCustom = false),
            FolderItem("Code", "#38BDF8", "💻", isCustom = false),
            FolderItem("Research", "#F59E0B", "🔬", isCustom = false),
            FolderItem("Priority", "#EC4899", "⚡", isCustom = false)
        )
    )
    val customFolders: StateFlow<List<FolderItem>> = _customFolders.asStateFlow()

    // Filtered notes based on active folder/space, tag, and search
    val filteredNotes: StateFlow<List<NoteEntity>> = combine(
        allNotes,
        _uiState
    ) { notes, state ->
        notes.filter { note ->
            val matchesFolder = state.selectedFolder == "All Notes" ||
                    note.folder.equals(state.selectedFolder, ignoreCase = true) ||
                    note.tag.equals(state.selectedFolder, ignoreCase = true)
            val matchesTag = state.selectedTag == null ||
                    note.tag.equals(state.selectedTag, ignoreCase = true)
            val matchesSearch = state.searchQuery.isBlank() ||
                    note.title.contains(state.searchQuery, ignoreCase = true) ||
                    note.content.contains(state.searchQuery, ignoreCase = true) ||
                    note.tag.contains(state.searchQuery, ignoreCase = true)
            matchesFolder && matchesTag && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            try {
                if (repository.getAllNotesDirect().isEmpty()) {
                    repository.insertNotes(SampleData.getInitialNotes())
                    repository.insertLinks(SampleData.getInitialLinks())
                    repository.insertChecklistItems(SampleData.getInitialChecklist())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        viewModelScope.launch {
            combine(allNotes, allLinks) { notes, links ->
                Pair(notes, links)
            }.collect { (notes, links) ->
                if (notes.isNotEmpty()) {
                    graphEngine.updateGraph(notes, links)
                }
            }
        }
    }

    fun setViewMode(mode: VaultViewMode) {
        _uiState.value = _uiState.value.copy(viewMode = mode)
    }

    fun setSelectedFolder(folder: String) {
        _uiState.value = _uiState.value.copy(selectedFolder = folder, selectedTag = null)
    }

    fun createCustomFolder(name: String, colorHex: String, icon: String = "📁") {
        val trimmed = name.trim()
        if (trimmed.isNotBlank() && _customFolders.value.none { it.name.equals(trimmed, ignoreCase = true) }) {
            val updated = _customFolders.value + FolderItem(name = trimmed, colorHex = colorHex, icon = icon, isCustom = true)
            _customFolders.value = updated
            setSelectedFolder(trimmed)
            _uiState.value = _uiState.value.copy(userNotice = "Group '$trimmed' created! ✨")
        }
    }

    fun renameGroup(oldName: String, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank() || oldName == "All Notes" || oldName == trimmed) return
        val current = _customFolders.value.map {
            if (it.name.equals(oldName, ignoreCase = true)) it.copy(name = trimmed) else it
        }
        _customFolders.value = current
        viewModelScope.launch {
            allNotes.value.filter { it.folder.equals(oldName, ignoreCase = true) }.forEach { note ->
                repository.saveNote(note.copy(folder = trimmed, tag = trimmed))
            }
            if (_uiState.value.selectedFolder.equals(oldName, ignoreCase = true)) {
                _uiState.value = _uiState.value.copy(selectedFolder = trimmed)
            }
            _uiState.value = _uiState.value.copy(userNotice = "Group renamed to '$trimmed'")
        }
    }

    fun dissolveGroup(groupName: String) {
        if (groupName == "All Notes") return
        _customFolders.value = _customFolders.value.filter { it.name != groupName }
        viewModelScope.launch {
            // Notes are KEPT. Only the group container is dissolved.
            repository.updateNotesFolder(oldFolder = groupName, newFolder = "All Notes")
            if (_uiState.value.selectedFolder.equals(groupName, ignoreCase = true)) {
                _uiState.value = _uiState.value.copy(selectedFolder = "All Notes")
            }
            _uiState.value = _uiState.value.copy(userNotice = "Dissolved '$groupName'. The notes inside were kept.")
        }
    }

    fun deleteGroupAndNotes(groupName: String) {
        if (groupName == "All Notes") return
        _customFolders.value = _customFolders.value.filter { it.name != groupName }
        viewModelScope.launch {
            repository.deleteNotesByFolder(groupName)
            if (_uiState.value.selectedFolder.equals(groupName, ignoreCase = true)) {
                _uiState.value = _uiState.value.copy(selectedFolder = "All Notes")
            }
            _uiState.value = _uiState.value.copy(userNotice = "Deleted group '$groupName' and its notes.")
        }
    }

    fun deleteCustomFolder(folderName: String) {
        dissolveGroup(folderName)
    }

    fun updateNoteFolder(noteId: String, folderName: String) {
        viewModelScope.launch {
            val note = allNotes.value.find { it.id == noteId } ?: return@launch
            val updated = note.copy(folder = folderName, tag = folderName)
            repository.saveNote(updated)
            if (_uiState.value.selectedNote?.id == noteId) {
                _uiState.value = _uiState.value.copy(selectedNote = updated)
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
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

            val zoom = _uiState.value.canvasZoom.coerceAtLeast(0.4f)
            val panX = _uiState.value.canvasPanX
            val panY = _uiState.value.canvasPanY
            // Center of viewport in world units + deterministic offset for multiple notes
            val approxScreenCenterX = 450f
            val approxScreenCenterY = 650f
            val offsetStep = (count % 5) * 24f
            val worldCenterX = (approxScreenCenterX - panX) / zoom + offsetStep
            val worldCenterY = (approxScreenCenterY - panY) / zoom + offsetStep

            val folder = if (_uiState.value.selectedFolder != "All Notes") _uiState.value.selectedFolder else defaultTag

            val id = repository.createNote(
                title = defaultTitle,
                content = if (type == NoteType.CHECKLIST) "Milestone tasks" else "",
                type = type,
                colorHex = colorHex,
                tag = folder,
                x = worldCenterX,
                y = worldCenterY
            )
            repository.getNoteDirect(id)?.let { note ->
                val noteWithFolder = note.copy(folder = folder)
                repository.saveNote(noteWithFolder)
                openNote(noteWithFolder)
            }
        }
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
                repository.saveNoteText(id = noteId, title = title, content = content, updatedAt = savedAt)
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
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        editorSave = it.editorSave.copy(
                            status = EditorSaveStatus.ERROR,
                            errorMessage = e.message ?: "Could not save note"
                        )
                    )
                }
            } finally {
                onSaved?.invoke()
            }
        }
    }

    /** Flushes the pending draft and only then leaves the editor. */
    fun saveEditorDraftAndClose(noteId: String, title: String, content: String) {
        saveEditorDraft(noteId, title, content) { closeFocusSheet() }
    }

    fun deleteSelectedNote() {
        val note = _uiState.value.selectedNote ?: return
        viewModelScope.launch {
            repository.deleteNote(note.id)
            closeFocusSheet()
        }
    }

    fun deleteNoteById(id: String) {
        viewModelScope.launch {
            repository.deleteNote(id)
            if (_uiState.value.selectedNote?.id == id) {
                closeFocusSheet()
            }
        }
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

    fun resetCanvasView() {
        _uiState.value = _uiState.value.copy(
            canvasPanX = 20f,
            canvasPanY = 40f,
            canvasZoom = 0.85f
        )
    }

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
    fun autoSortBoard() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAiLoading = true)
            val notes = allNotes.value

            // Neatly arrange cards into 2-column or thematic quadrants
            val colW = 260f
            val rowH = 220f
            val startX = 40f
            val startY = 70f

            notes.forEachIndexed { index, note ->
                val col = index % 2
                val row = index / 2
                val newX = startX + col * colW
                val newY = startY + row * rowH
                repository.updateNotePosition(note.id, newX, newY)
            }

            _uiState.value = _uiState.value.copy(
                isAiLoading = false,
                userNotice = "Gemini auto-sorted board cards into structured quadrants! ✨",
                isAskGeminiExpanded = false
            )
        }
    }

    fun beautifyCurrentNote() {
        val note = _uiState.value.selectedNote ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAiLoading = true)
            val beautified = repository.beautifyNoteContent(note.title, note.content)
            val updated = note.copy(content = beautified)
            updateSelectedNote(updated)
            _uiState.value = _uiState.value.copy(
                isAiLoading = false,
                userNotice = "Note reformatted & elevated with neat Markdown! ✨"
            )
        }
    }

    fun synthesizeActiveSpace() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAiLoading = true)
            val summary = repository.synthesizeActiveSpace(_uiState.value.selectedFolder, filteredNotes.value)
            _uiState.value = _uiState.value.copy(
                isAiLoading = false,
                aiSummaryOutput = summary,
                isAskGeminiExpanded = true,
                userNotice = "Active space synthesized by Gemini"
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
                    val summary = repository.synthesizeActiveSpace(query, filteredNotes.value)
                    _uiState.value = _uiState.value.copy(
                        isAiLoading = false,
                        aiSummaryOutput = summary,
                        isAskGeminiExpanded = true
                    )
                }
            }
        }
    }

    fun requestAiLinkSuggestions() {
        val note = _uiState.value.selectedNote ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAiLoading = true)
            val suggestions = repository.suggestLinks(note, allNotes.value)
            _uiState.value = _uiState.value.copy(
                isAiLoading = false,
                aiSuggestedLinks = suggestions
            )
        }
    }

    fun acceptAiLink(suggestion: LinkSuggestion) {
        val currentNote = _uiState.value.selectedNote ?: return
        viewModelScope.launch {
            val targetId = suggestion.targetId ?: run {
                repository.createNote(
                    title = suggestion.targetTitle,
                    content = "# ${suggestion.targetTitle}\nLinked concept from ${currentNote.title}",
                    type = NoteType.DOC,
                    colorHex = "#6366F1"
                )
            }
            repository.createLink(currentNote.id, targetId)
            _uiState.value = _uiState.value.copy(
                aiSuggestedLinks = _uiState.value.aiSuggestedLinks.filter { it.targetTitle != suggestion.targetTitle },
                userNotice = "Connected thread to [[${suggestion.targetTitle}]]"
            )
        }
    }
}
