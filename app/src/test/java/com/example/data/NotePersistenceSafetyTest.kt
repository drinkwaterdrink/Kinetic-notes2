package com.example.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ChecklistItemEntity
import com.example.data.local.NoteDatabase
import com.example.data.local.NoteEntity
import com.example.data.local.NoteLinkEntity
import com.example.data.repository.NoteRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Regression tests for the "editing a note destroys its children" class of bug.
 *
 * Root cause being guarded against: `@Insert(onConflict = REPLACE)` deletes the conflicting
 * row before re-inserting it, and the delete cascades into `note_links` / `checklist_items`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NotePersistenceSafetyTest {

    private lateinit var database: NoteDatabase
    private lateinit var repository: NoteRepository

    private val noteId = "note-under-test"
    private val otherNoteId = "other-note"

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        database = Room.inMemoryDatabaseBuilder(app, NoteDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = NoteRepository(database.noteDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun seedNoteWithChildren() {
        val dao = database.noteDao()
        dao.insertNotes(
            listOf(
                NoteEntity(id = noteId, title = "Spec", content = "body", x = 10f, y = 20f),
                NoteEntity(id = otherNoteId, title = "Linked", content = "other")
            )
        )
        dao.insertLink(NoteLinkEntity(id = "link-1", sourceId = noteId, targetId = otherNoteId))
        dao.insertChecklistItems(
            listOf(
                ChecklistItemEntity(id = "item-1", noteId = noteId, text = "first", orderIndex = 0),
                ChecklistItemEntity(id = "item-2", noteId = noteId, text = "second", orderIndex = 1)
            )
        )
    }

    @Test
    fun savingExistingNoteKeepsLinksAndChecklistItems() = runTest {
        seedNoteWithChildren()
        val dao = database.noteDao()

        val existing = dao.getNoteDirect(noteId)!!
        repository.saveNote(existing.copy(content = "edited body"))

        assertEquals("edited body", dao.getNoteDirect(noteId)?.content)
        assertEquals(
            "Checklist items must survive saving an existing note",
            2,
            dao.getChecklistItemsDirect(noteId).size
        )
        assertEquals(
            "Links must survive saving an existing note",
            1,
            database.noteDao().getAllLinksDirect().count { it.sourceId == noteId }
        )
    }

    @Test
    fun repeatedSavesKeepChildRowsStable() = runTest {
        seedNoteWithChildren()
        val dao = database.noteDao()

        repeat(10) { i ->
            val existing = dao.getNoteDirect(noteId)!!
            repository.saveNote(existing.copy(content = "revision $i"))
        }

        assertEquals("revision 9", dao.getNoteDirect(noteId)?.content)
        assertEquals(2, dao.getChecklistItemsDirect(noteId).size)
        assertEquals(1, dao.getAllLinksDirect().size)
    }

    @Test
    fun saveNoteTextOnlyTouchesTextColumnsAndKeepsChildren() = runTest {
        seedNoteWithChildren()
        val dao = database.noteDao()

        // Simulates a board drag that lands while the editor is open.
        dao.updateNotePosition(noteId, 480f, 640f, System.currentTimeMillis())

        val rows = repository.saveNoteText(noteId, "New title", "New content", updatedAt = 1_234L)

        val saved = dao.getNoteDirect(noteId)!!
        assertEquals(1, rows)
        assertEquals("New title", saved.title)
        assertEquals("New content", saved.content)
        assertEquals(1_234L, saved.updatedAt)
        assertEquals("Editor save must not clobber the board position", 480f, saved.x, 0.001f)
        assertEquals("Editor save must not clobber the board position", 640f, saved.y, 0.001f)
        assertEquals(2, dao.getChecklistItemsDirect(noteId).size)
        assertEquals(1, dao.getAllLinksDirect().size)
    }

    @Test
    fun saveNoteTextOnMissingNoteIsANoOp() = runTest {
        val rows = repository.saveNoteText("does-not-exist", "t", "c")
        assertEquals(0, rows)
    }

    @Test
    fun upsertCreatesTheRowWhenItDoesNotExistYet() = runTest {
        val dao = database.noteDao()
        repository.saveNote(NoteEntity(id = "brand-new", title = "Fresh", content = "c"))

        val created = dao.getNoteDirect("brand-new")
        assertNotNull(created)
        assertEquals("Fresh", created?.title)
    }

    /**
     * Characterisation test documenting WHY `saveNote` must not use INSERT OR REPLACE.
     * If this ever starts failing, SQLite/Room changed their cascade behaviour and the
     * comment on [com.example.data.local.NoteDao.insertNote] should be revisited.
     */
    @Test
    fun legacyInsertReplaceDestroysChildRows() = runTest {
        seedNoteWithChildren()
        val dao = database.noteDao()

        val existing = dao.getNoteDirect(noteId)!!
        dao.insertNote(existing.copy(content = "replaced"))

        val survivingItems = dao.getChecklistItemsDirect(noteId)
        val survivingLinks = dao.getAllLinksDirect()
        assertTrue(
            "INSERT OR REPLACE is expected to cascade-delete children " +
                "(items=${survivingItems.size}, links=${survivingLinks.size})",
            survivingItems.isEmpty() && survivingLinks.isEmpty()
        )
    }
}
