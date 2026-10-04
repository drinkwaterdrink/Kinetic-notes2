package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    fun getNoteById(id: String): Flow<NoteEntity?>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteDirect(id: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE title = :title LIMIT 1")
    suspend fun getNoteByTitle(title: String): NoteEntity?

    @Query("SELECT * FROM notes")
    suspend fun getAllNotesDirect(): List<NoteEntity>

    /**
     * DANGEROUS for existing rows: SQLite's REPLACE conflict strategy DELETEs the conflicting
     * row before inserting the new one. With foreign keys enabled (Room enables them by default)
     * that delete cascades into [NoteLinkEntity] and [ChecklistItemEntity], silently destroying
     * a note's links and checklist items.
     *
     * Only use this for rows that are known not to exist yet (brand new notes / seeding).
     * For saving an edited note use [upsertNote] or a targeted UPDATE query instead.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteEntity>)

    /**
     * Safe whole-entity save: inserts when the row is new, otherwise performs a real UPDATE.
     * The row is never deleted, so foreign-key children (links, checklist items) survive.
     */
    @Upsert
    suspend fun upsertNote(note: NoteEntity)

    @Upsert
    suspend fun upsertNotes(notes: List<NoteEntity>)

    @Update
    suspend fun updateNote(note: NoteEntity)

    /**
     * Targeted editor save. Touches only the text columns, so it can never clobber a position
     * update that happened on the board while the editor was open.
     */
    @Query("UPDATE notes SET title = :title, content = :content, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateNoteTitleAndContent(id: String, title: String, content: String, updatedAt: Long): Int

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: String)

    @Query("UPDATE notes SET x = :x, y = :y, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateNotePosition(id: String, x: Float, y: Float, updatedAt: Long = System.currentTimeMillis())

    // ----- Groups (durable, stable-id based) -----

    @Query("SELECT * FROM note_groups ORDER BY orderIndex ASC, name ASC")
    fun getAllGroups(): Flow<List<NoteGroupEntity>>

    @Query("SELECT * FROM note_groups ORDER BY orderIndex ASC, name ASC")
    suspend fun getAllGroupsDirect(): List<NoteGroupEntity>

    @Query("SELECT * FROM note_groups WHERE id = :id")
    suspend fun getGroupById(id: String): NoteGroupEntity?

    @Query("SELECT * FROM note_groups WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getGroupByName(name: String): NoteGroupEntity?

    @Upsert
    suspend fun upsertGroup(group: NoteGroupEntity)

    @Upsert
    suspend fun upsertGroups(groups: List<NoteGroupEntity>)

    @Query("UPDATE note_groups SET name = :name, colorHex = :colorHex, icon = :icon WHERE id = :id")
    suspend fun updateGroupMeta(id: String, name: String, colorHex: String, icon: String): Int

    @Query("DELETE FROM note_groups WHERE id = :id")
    suspend fun deleteGroupById(id: String)

    @Query("SELECT COUNT(*) FROM notes WHERE groupId = :groupId")
    suspend fun countNotesInGroup(groupId: String): Int

    /** Normal "Delete group": the notes are KEPT and become ungrouped (All Notes). */
    @Query("UPDATE notes SET groupId = NULL, folder = 'All Notes' WHERE groupId = :groupId")
    suspend fun detachNotesFromGroup(groupId: String)

    /** Destructive "Delete group + notes". */
    @Query("DELETE FROM notes WHERE groupId = :groupId")
    suspend fun deleteNotesInGroup(groupId: String)

    @Query("UPDATE notes SET groupId = :groupId, folder = :folderName, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateNoteGroup(id: String, groupId: String?, folderName: String, updatedAt: Long): Int

    /** Keeps the legacy readable folder column in step with a renamed group. */
    @Query("UPDATE notes SET folder = :folderName WHERE groupId = :groupId")
    suspend fun syncLegacyFolderName(groupId: String, folderName: String)

    @Query("UPDATE notes SET isPinned = :isPinned WHERE id = :id")
    suspend fun updatePinStatus(id: String, isPinned: Boolean)

    @Query("UPDATE notes SET isLocked = :isLocked WHERE id = :id")
    suspend fun updateLockStatus(id: String, isLocked: Boolean)

    // Note Links (Obsidian Graph Threads)
    @Query("SELECT * FROM note_links")
    fun getAllLinks(): Flow<List<NoteLinkEntity>>

    @Query("SELECT * FROM note_links")
    suspend fun getAllLinksDirect(): List<NoteLinkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLink(link: NoteLinkEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLinks(links: List<NoteLinkEntity>)

    @Query("DELETE FROM note_links WHERE id = :id")
    suspend fun deleteLinkById(id: String)

    @Query("DELETE FROM note_links WHERE (sourceId = :id1 AND targetId = :id2) OR (sourceId = :id2 AND targetId = :id1)")
    suspend fun deleteLinkBetween(id1: String, id2: String)

    // Checklist Items
    @Query("SELECT * FROM checklist_items WHERE noteId = :noteId ORDER BY orderIndex ASC")
    fun getChecklistItems(noteId: String): Flow<List<ChecklistItemEntity>>

    @Query("SELECT * FROM checklist_items WHERE noteId = :noteId ORDER BY orderIndex ASC")
    suspend fun getChecklistItemsDirect(noteId: String): List<ChecklistItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItem(item: ChecklistItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChecklistItems(items: List<ChecklistItemEntity>)

    @Update
    suspend fun updateChecklistItem(item: ChecklistItemEntity)

    @Query("DELETE FROM checklist_items WHERE id = :id")
    suspend fun deleteChecklistItem(id: String)

    @Query("DELETE FROM checklist_items WHERE noteId = :noteId")
    suspend fun deleteChecklistItemsForNote(noteId: String)
}
