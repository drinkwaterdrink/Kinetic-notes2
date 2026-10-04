package com.example.data.repository

import com.example.data.local.ChecklistItemEntity
import com.example.data.local.NoteDao
import com.example.data.local.NoteEntity
import com.example.data.local.NoteGroupEntity
import com.example.data.local.NoteLinkEntity
import com.example.data.local.NoteType
import com.example.data.remote.GeminiService
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import java.util.UUID

data class LinkSuggestion(
    val targetTitle: String,
    val targetId: String?,
    val reason: String
)

class NoteRepository(
    private val noteDao: NoteDao,
    private val geminiService: GeminiService = GeminiService()
) {
    val allNotes: Flow<List<NoteEntity>> = noteDao.getAllNotes()
    val allLinks: Flow<List<NoteLinkEntity>> = noteDao.getAllLinks()
    val allGroups: Flow<List<NoteGroupEntity>> = noteDao.getAllGroups()

    fun getNoteById(id: String): Flow<NoteEntity?> = noteDao.getNoteById(id)

    suspend fun getNoteDirect(id: String): NoteEntity? = noteDao.getNoteDirect(id)

    suspend fun getAllNotesDirect(): List<NoteEntity> = noteDao.getAllNotesDirect()

    suspend fun getNoteByTitle(title: String): NoteEntity? = noteDao.getNoteByTitle(title)

    suspend fun createNote(
        title: String,
        content: String = "",
        type: NoteType = NoteType.DOC,
        colorHex: String = "#F59E0B",
        tag: String = "general",
        groupId: String? = null,
        folder: String = "All Notes",
        x: Float = 100f,
        y: Float = 100f
    ): String {
        val id = UUID.randomUUID().toString()
        val note = NoteEntity(
            id = id,
            title = title,
            content = content,
            type = type,
            colorHex = colorHex,
            tag = tag,
            groupId = groupId,
            folder = folder,
            x = x,
            y = y,
            updatedAt = System.currentTimeMillis()
        )
        noteDao.insertNote(note)
        return id
    }

    /**
     * Whole-entity save for an existing or new note.
     *
     * Uses UPSERT (insert-or-update) rather than INSERT OR REPLACE: REPLACE deletes the existing
     * row first, which cascades into note_links / checklist_items and destroys them.
     */
    suspend fun saveNote(note: NoteEntity) {
        noteDao.upsertNote(note.copy(updatedAt = System.currentTimeMillis()))
    }

    /**
     * Durable editor save. Only the text columns are written, so concurrent board moves
     * (x/y) and metadata changes are never overwritten by a stale editor snapshot.
     *
     * @return number of rows updated (0 when the note no longer exists).
     */
    suspend fun saveNoteText(
        id: String,
        title: String,
        content: String,
        updatedAt: Long = System.currentTimeMillis()
    ): Int = noteDao.updateNoteTitleAndContent(id, title, content, updatedAt)

    suspend fun insertNotes(notes: List<NoteEntity>) = noteDao.insertNotes(notes)
    suspend fun insertLinks(links: List<NoteLinkEntity>) = noteDao.insertLinks(links)
    suspend fun insertChecklistItems(items: List<ChecklistItemEntity>) = noteDao.insertChecklistItems(items)

    suspend fun deleteNote(id: String) {
        noteDao.deleteNoteById(id)
    }

    // ----- Groups -----

    suspend fun getAllGroupsDirect(): List<NoteGroupEntity> = noteDao.getAllGroupsDirect()

    suspend fun getGroupById(id: String): NoteGroupEntity? = noteDao.getGroupById(id)

    suspend fun getGroupByName(name: String): NoteGroupEntity? = noteDao.getGroupByName(name)

    /** Creates a group, or returns the existing one when the name is already taken. */
    suspend fun createGroup(name: String, colorHex: String, icon: String = "📁"): NoteGroupEntity {
        val trimmed = name.trim()
        noteDao.getGroupByName(trimmed)?.let { return it }
        val existingCount = noteDao.getAllGroupsDirect().size
        val group = NoteGroupEntity(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            colorHex = colorHex,
            icon = icon,
            orderIndex = existingCount
        )
        noteDao.upsertGroup(group)
        return group
    }

    suspend fun updateGroup(id: String, name: String, colorHex: String, icon: String): Int {
        val trimmed = name.trim()
        val rows = noteDao.updateGroupMeta(id, trimmed, colorHex, icon)
        // Keep the legacy readable folder column in step with the rename.
        noteDao.syncLegacyFolderName(id, trimmed)
        return rows
    }

    /** Normal delete: the group disappears, its notes survive and become ungrouped. */
    suspend fun deleteGroupKeepNotes(id: String) {
        noteDao.detachNotesFromGroup(id)
        noteDao.deleteGroupById(id)
    }

    /** Destructive delete: the group AND every note inside it are removed. */
    suspend fun deleteGroupAndNotes(id: String) {
        noteDao.deleteNotesInGroup(id)
        noteDao.deleteGroupById(id)
    }

    suspend fun countNotesInGroup(id: String): Int = noteDao.countNotesInGroup(id)

    suspend fun moveNoteToGroup(noteId: String, groupId: String?): Int {
        val folderName = groupId?.let { noteDao.getGroupById(it)?.name } ?: "All Notes"
        return noteDao.updateNoteGroup(noteId, groupId, folderName, System.currentTimeMillis())
    }

    suspend fun insertGroups(groups: List<NoteGroupEntity>) = noteDao.upsertGroups(groups)

    suspend fun updateNotePosition(id: String, x: Float, y: Float) {
        noteDao.updateNotePosition(id, x, y)
    }

    suspend fun togglePin(id: String, currentPin: Boolean) {
        noteDao.updatePinStatus(id, !currentPin)
    }

    suspend fun toggleLock(id: String, currentLock: Boolean) {
        noteDao.updateLockStatus(id, !currentLock)
    }

    // Links
    suspend fun createLink(sourceId: String, targetId: String, colorHex: String = "#6366F1") {
        if (sourceId == targetId) return
        val link = NoteLinkEntity(
            id = UUID.randomUUID().toString(),
            sourceId = sourceId,
            targetId = targetId,
            colorHex = colorHex
        )
        noteDao.insertLink(link)
    }

    suspend fun removeLinkBetween(id1: String, id2: String) {
        noteDao.deleteLinkBetween(id1, id2)
    }

    suspend fun removeLinkById(id: String) {
        noteDao.deleteLinkById(id)
    }

    // Checklist Items
    fun getChecklistItems(noteId: String): Flow<List<ChecklistItemEntity>> = noteDao.getChecklistItems(noteId)

    suspend fun getChecklistItemsDirect(noteId: String): List<ChecklistItemEntity> = noteDao.getChecklistItemsDirect(noteId)

    suspend fun addChecklistItem(noteId: String, text: String): String {
        val id = UUID.randomUUID().toString()
        val currentItems = noteDao.getChecklistItemsDirect(noteId)
        val item = ChecklistItemEntity(
            id = id,
            noteId = noteId,
            text = text,
            isChecked = false,
            orderIndex = currentItems.size
        )
        noteDao.insertChecklistItem(item)
        return id
    }

    suspend fun toggleChecklistItem(item: ChecklistItemEntity) {
        noteDao.updateChecklistItem(item.copy(isChecked = !item.isChecked))
    }

    suspend fun deleteChecklistItem(id: String) {
        noteDao.deleteChecklistItem(id)
    }

    // Gemini AI Features
    suspend fun suggestLinks(currentNote: NoteEntity, allNotes: List<NoteEntity>): Result<List<LinkSuggestion>> {
        val candidates = allNotes.filter { it.id != currentNote.id }
        if (candidates.isEmpty()) return Result.success(emptyList())

        val candidateTitles = candidates.joinToString(", ") { "\"${it.title}\"" }
        val prompt = """
You are an expert Obsidian/Roam knowledge graph architect.
Current Note:
Title: "${currentNote.title}"
Content: "${currentNote.content.take(500)}"

Existing Note Titles in Vault:
[$candidateTitles]

Suggest 2-3 existing notes to connect to "${currentNote.title}".
Format response as a JSON array of objects:
[
  {"targetTitle": "Exact Note Title", "reason": "1 short sentence explaining why they should link."}
]
Return ONLY JSON.
""".trimIndent()

        val response = geminiService.generateContent(prompt).getOrElse { return Result.failure(it) }
        val suggestions = mutableListOf<LinkSuggestion>()

        try {
            val jsonStart = response.indexOf('[')
            val jsonEnd = response.lastIndexOf(']')
            if (jsonStart >= 0 && jsonEnd > jsonStart) {
                val jsonStr = response.substring(jsonStart, jsonEnd + 1)
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val targetTitle = obj.optString("targetTitle")
                    val reason = obj.optString("reason")
                    val matchingNote = candidates.find { it.title.equals(targetTitle, ignoreCase = true) }
                    suggestions.add(
                        LinkSuggestion(
                            targetTitle = matchingNote?.title ?: targetTitle,
                            targetId = matchingNote?.id,
                            reason = reason
                        )
                    )
                }
            }
        } catch (_: Exception) {
            return Result.failure(IllegalStateException("AI provider returned invalid link suggestions"))
        }
        return Result.success(suggestions)
    }

    suspend fun beautifyNoteContent(title: String, content: String): Result<String> {
        return geminiService.beautifyAndFormatNote(title, content)
    }

    suspend fun synthesizeActiveSpace(spaceName: String, notes: List<NoteEntity>): Result<String> {
        return geminiService.synthesizeSpace(spaceName, notes)
    }

    suspend fun generateSummaryAndChecklist(content: String): Result<String> {
        val prompt = """
Analyze the following note content and produce:
1. A concise 2-sentence executive summary.
2. A bulleted action checklist (- [ ] task) of concrete next steps.

Content:
$content
""".trimIndent()

        return geminiService.generateContent(prompt)
    }

    suspend fun transcribeAudioMemo(durationMs: Long): Result<String> {
        val prompt = """
Transcribe and extract action items from a recorded audio voice memo ($durationMs ms) in Kinetic Notes.
Provide:
1. Key takeaways
2. 3 action checklist items
""".trimIndent()

        return geminiService.generateContent(prompt)
    }
}
