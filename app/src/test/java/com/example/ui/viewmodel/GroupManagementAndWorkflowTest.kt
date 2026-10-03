package com.example.ui.viewmodel

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.NoteDatabase
import com.example.data.local.NoteEntity
import com.example.data.local.NoteType
import com.example.data.repository.NoteRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GroupManagementAndWorkflowTest {

    private lateinit var database: NoteDatabase
    private lateinit var repository: NoteRepository
    private lateinit var viewModel: NotesViewModel

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        database = Room.inMemoryDatabaseBuilder(app, NoteDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = NoteRepository(database.noteDao())
        viewModel = NotesViewModel(app, repository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testDissolveGroupPreservesAllNotes() = runTest {
        // Create a custom group
        val groupName = "Design Sprint"
        viewModel.createCustomFolder(groupName, "#6366F1")
        assertTrue(viewModel.customFolders.value.any { it.name == groupName })

        // Create 2 notes inside that group
        val id1 = repository.createNote(title = "Wireframe", content = "Spec", tag = groupName, type = NoteType.DOC)
        val id2 = repository.createNote(title = "Color palette", content = "Tokens", tag = groupName, type = NoteType.DOC)

        // Assign their folders
        val note1 = repository.getNoteDirect(id1)!!.copy(folder = groupName)
        val note2 = repository.getNoteDirect(id2)!!.copy(folder = groupName)
        repository.saveNote(note1)
        repository.saveNote(note2)

        // Wait for allNotes flow update
        val notesBefore = repository.allNotes.first().filter { it.folder == groupName }
        assertEquals(2, notesBefore.size)

        // Dissolve group: Group definition must be deleted, BUT notes MUST REMAIN!
        viewModel.dissolveGroup(groupName)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        // Verify group is removed from customFolders list
        assertFalse(viewModel.customFolders.value.any { it.name == groupName })

        // Verify notes still exist in the repository and are now in "All Notes"
        val note1After = repository.getNoteDirect(id1)
        val note2After = repository.getNoteDirect(id2)
        assertNotNull(note1After)
        assertNotNull(note2After)
        assertEquals("All Notes", note1After?.folder)
        assertEquals("All Notes", note2After?.folder)
    }

    @Test
    fun testDeleteGroupAndNotesDeletesContainedNotes() = runTest {
        val groupName = "Temporary Sprint"
        viewModel.createCustomFolder(groupName, "#EC4899")

        val id1 = repository.createNote(title = "Temp Doc", content = "Delete me", tag = groupName)
        val note1 = repository.getNoteDirect(id1)!!.copy(folder = groupName)
        repository.saveNote(note1)

        val notesBefore = repository.allNotes.first().filter { it.folder == groupName }
        assertEquals(1, notesBefore.size)

        // Delete group AND notes
        viewModel.deleteGroupAndNotes(groupName)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        // Verify group and contained notes are deleted
        assertFalse(viewModel.customFolders.value.any { it.name == groupName })
        val note1After = repository.getNoteDirect(id1)
        assertEquals(null, note1After)
    }

    @Test
    fun testCreateNewNoteOpensEditorImmediately() = runTest {
        // Tapping + creates note near viewport center and immediately opens editor
        viewModel.createNewNoteAtViewportCenter(NoteType.DOC)

        var attempts = 0
        while (!viewModel.uiState.value.isFocusSheetOpen && attempts < 50) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            Thread.sleep(50)
            attempts++
        }

        val uiState = viewModel.uiState.value
        assertTrue("Editor should be open", uiState.isFocusSheetOpen)
        assertNotNull("Selected note should be populated", uiState.selectedNote)
        assertEquals("New Note", uiState.selectedNote?.title)
    }
}
