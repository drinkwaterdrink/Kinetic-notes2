package com.example.ui.viewmodel

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.NoteDatabase
import com.example.data.local.NoteEntity
import com.example.data.local.NoteGroupEntity
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
        val app = ApplicationProvider.getApplicationContext<Application>()
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

    private suspend fun awaitUntil(timeoutMs: Long = 5_000, condition: suspend () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            if (condition()) return true
            Thread.sleep(20)
        }
        return condition()
    }

    @Test
    fun durableGroupSurvivesViewModelRecreation() = runTest {
        val group = repository.createGroup("Design Sprint", "#6366F1")
        assertTrue(repository.getAllGroupsDirect().any { it.id == group.id })

        val recreated = NotesViewModel(ApplicationProvider.getApplicationContext(), repository)
        assertTrue(awaitUntil { recreated.customFolders.value.any { it.id == group.id } })
    }

    @Test
    fun deleteGroupKeepsNotesAndMakesThemUngrouped() = runTest {
        val group = repository.createGroup("Design Sprint", "#6366F1")
        val noteId = repository.createNote(
            title = "Wireframe",
            content = "Spec",
            tag = group.name,
            type = NoteType.DOC,
            groupId = group.id,
            folder = group.name
        )

        viewModel.deleteGroupKeepNotes(group.id)
        assertTrue(awaitUntil { repository.getGroupById(group.id) == null })

        val note = repository.getNoteDirect(noteId)
        assertNotNull(note)
        assertEquals(null, note?.groupId)
        assertEquals("All Notes", note?.folder)
    }

    @Test
    fun deleteGroupAndNotesDeletesContainedNotes() = runTest {
        val group = repository.createGroup("Temporary Sprint", "#EC4899")
        val noteId = repository.createNote(
            title = "Temp Doc",
            content = "Delete me",
            tag = group.name,
            groupId = group.id,
            folder = group.name
        )

        viewModel.deleteGroupAndNotes(group.id)
        assertTrue(awaitUntil { repository.getGroupById(group.id) == null })
        assertEquals(null, repository.getNoteDirect(noteId))
    }

    @Test
    fun renamingGroupKeepsStableIdAndUpdatesReadableNoteName() = runTest {
        val group = repository.createGroup("Old Name", "#6366F1")
        val noteId = repository.createNote(
            title = "Note",
            groupId = group.id,
            folder = group.name,
            tag = group.name
        )

        repository.updateGroup(group.id, "New Name", "#10B981", "📁")

        val renamed = repository.getGroupById(group.id)
        val note = repository.getNoteDirect(noteId)
        assertEquals(group.id, renamed?.id)
        assertEquals("New Name", renamed?.name)
        assertEquals("New Name", note?.folder)
        assertEquals(group.id, note?.groupId)
    }

    @Test
    fun createNewNoteOpensEditorImmediately() = runTest {
        viewModel.onViewportMeasured(1080f, 1800f)
        viewModel.createNewNoteAtViewportCenter(NoteType.DOC)

        assertTrue(
            "Editor should be open",
            awaitUntil { viewModel.uiState.value.isFocusSheetOpen }
        )
        val uiState = viewModel.uiState.value
        assertNotNull("Selected note should be populated", uiState.selectedNote)
        assertEquals("New Note", uiState.selectedNote?.title)
    }
}
