package com.example.ui.viewmodel

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.ChecklistItemEntity
import com.example.data.local.NoteDatabase
import com.example.data.local.NoteEntity
import com.example.data.local.NoteLinkEntity
import com.example.data.repository.NoteRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/**
 * Save / autosave contract:
 * typing -> editor draft (no write) -> debounce or Save -> durable write
 * Back -> flush pending draft BEFORE leaving the editor.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EditorSaveFlowTest {

    private lateinit var database: NoteDatabase
    private lateinit var repository: NoteRepository
    private lateinit var viewModel: NotesViewModel

    private val noteId = "editor-note"

    @Before
    fun setUp() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        database = Room.inMemoryDatabaseBuilder(app, NoteDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = NoteRepository(database.noteDao())
        viewModel = NotesViewModel(app, repository)

        runBlocking {
            val dao = database.noteDao()
            dao.insertNotes(
                listOf(
                    NoteEntity(id = noteId, title = "Original", content = "Original body", x = 5f, y = 7f),
                    NoteEntity(id = "target", title = "Target", content = "")
                )
            )
            dao.insertLink(NoteLinkEntity(id = "l1", sourceId = noteId, targetId = "target"))
            dao.insertChecklistItem(ChecklistItemEntity(id = "c1", noteId = noteId, text = "task"))
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    /** Robolectric + Room both need real time to settle, so poll instead of sleeping blindly. */
    private fun awaitUntil(timeoutMs: Long = 5_000, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            ShadowLooper.idleMainLooper()
            if (condition()) return true
            Thread.sleep(20)
        }
        ShadowLooper.idleMainLooper()
        return condition()
    }

    @Test
    fun markEditorDirtyReportsUnsavedWithoutWritingToTheDatabase() = runTest {
        val note = repository.getNoteDirect(noteId)!!
        viewModel.openNote(note)
        ShadowLooper.idleMainLooper()
        assertEquals(EditorSaveStatus.SAVED, viewModel.uiState.value.editorSave.status)

        viewModel.markEditorDirty()

        assertEquals(EditorSaveStatus.UNSAVED, viewModel.uiState.value.editorSave.status)
        assertEquals(
            "Marking the draft dirty must not touch Room",
            "Original body",
            repository.getNoteDirect(noteId)?.content
        )
    }

    @Test
    fun saveEditorDraftPersistsTextAndReportsSaved() = runTest {
        val note = repository.getNoteDirect(noteId)!!
        viewModel.openNote(note)
        viewModel.markEditorDirty()

        viewModel.saveEditorDraft(noteId, "Edited title", "Edited body")

        assertTrue(
            "Editor should reach SAVED",
            awaitUntil { viewModel.uiState.value.editorSave.status == EditorSaveStatus.SAVED }
        )
        val saved = repository.getNoteDirect(noteId)!!
        assertEquals("Edited title", saved.title)
        assertEquals("Edited body", saved.content)
        // The in-memory selection must mirror what was written.
        assertEquals("Edited title", viewModel.uiState.value.selectedNote?.title)
        // And the edit must not have destroyed the note's children.
        assertEquals(1, database.noteDao().getChecklistItemsDirect(noteId).size)
        assertEquals(1, database.noteDao().getAllLinksDirect().size)
    }

    @Test
    fun backFlushesPendingDraftBeforeClosingTheEditor() = runTest {
        val note = repository.getNoteDirect(noteId)!!
        viewModel.openNote(note)
        viewModel.markEditorDirty()

        viewModel.saveEditorDraftAndClose(noteId, "Flushed", "Flushed body")

        assertTrue(
            "Editor should close after the flush",
            awaitUntil { !viewModel.uiState.value.isFocusSheetOpen }
        )
        val saved = repository.getNoteDirect(noteId)!!
        assertEquals("Flushed", saved.title)
        assertEquals("Flushed body", saved.content)
        assertFalse(viewModel.uiState.value.isFocusSheetOpen)
        assertEquals(null, viewModel.uiState.value.selectedNote)
    }

    @Test
    fun editorSaveDoesNotOverwriteAPositionUpdateMadeWhileEditing() = runTest {
        val note = repository.getNoteDirect(noteId)!!
        viewModel.openNote(note)
        viewModel.markEditorDirty()

        // Board move lands while the editor holds a stale snapshot (x=5, y=7).
        repository.updateNotePosition(noteId, 900f, 950f)
        viewModel.saveEditorDraft(noteId, "Title", "Body")

        assertTrue(awaitUntil { viewModel.uiState.value.editorSave.status == EditorSaveStatus.SAVED })
        val saved = repository.getNoteDirect(noteId)!!
        assertEquals(900f, saved.x, 0.001f)
        assertEquals(950f, saved.y, 0.001f)
        assertEquals("Body", saved.content)
    }
}
