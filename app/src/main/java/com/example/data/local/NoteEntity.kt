package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class NoteType {
    DOC,
    CHECKLIST,
    CODE,
    SKETCH,
    AUDIO
}

@Entity(tableName = "notes", indices = [Index("groupId")])
data class NoteEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String = "",
    val type: NoteType = NoteType.DOC,
    val colorHex: String = "#6366F1",
    val tag: String = "Architecture",
    /**
     * Stable reference to a [NoteGroupEntity]. `null` means "ungrouped" (All Notes).
     * This is the source of truth for grouping.
     */
    val groupId: String? = null,
    /**
     * Legacy human-readable group name. Kept in sync with the group for export/search
     * readability, but never used to decide which group a note belongs to.
     */
    val folder: String = "All Notes",
    val x: Float = 0f,
    val y: Float = 0f,
    val isPinned: Boolean = false,
    val isLocked: Boolean = false,
    val dueDateText: String? = null,
    val codeSnippet: String? = null,
    val zIndex: Int = 10,
    val strokeData: String? = null,
    val audioPath: String? = null,
    val audioDurationMs: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
