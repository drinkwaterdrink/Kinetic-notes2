package com.example.ui.editor

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.data.local.NoteEntity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DeleteNoteConfirmationTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val note = NoteEntity(
        id = "delete-note",
        title = "Keep this draft",
        content = "Unsaved editor content"
    )

    private fun setEditor(onDelete: () -> Unit) {
        composeRule.setContent {
            FullScreenNoteEditorScreen(
                note = note,
                checklistItems = emptyList(),
                suggestedLinks = emptyList(),
                aiSummaryOutput = null,
                isAiLoading = false,
                onClose = {},
                onUpdateNote = {},
                onDeleteNote = onDelete,
                onTogglePin = {},
                onToggleLock = {},
                onWikiLinkClick = {},
                onAddChecklistItem = {},
                onToggleChecklistItem = {},
                onDeleteChecklistItem = {},
                onBeautifyNote = {},
                onRequestAiLinks = {},
                onAcceptAiLink = {}
            )
        }
    }

    @Test
    fun deleteButtonOnlyOpensConfirmationAndCancelPreservesTheDraft() {
        var deleteCalls = 0
        setEditor { deleteCalls++ }

        composeRule.onNodeWithTag("btn_delete_note").performClick()
        composeRule.onNodeWithTag("confirm_delete_note").fetchSemanticsNode()
        assertEquals("Opening confirmation must not delete", 0, deleteCalls)

        composeRule.onNodeWithTag("cancel_delete_note").performClick()
        assertEquals(0, composeRule.onAllNodesWithTag("confirm_delete_note").fetchSemanticsNodes().size)
        assertEquals("Cancel must leave the draft and note intact", 0, deleteCalls)
    }

    @Test
    fun confirmDispatchesExactlyOneDeleteOperation() {
        var deleteCalls = 0
        setEditor { deleteCalls++ }

        composeRule.onNodeWithTag("btn_delete_note").performClick()
        composeRule.onNodeWithTag("confirm_delete_note").performClick()
        composeRule.waitForIdle()

        assertEquals("Confirm must dispatch the intended delete once", 1, deleteCalls)
        assertEquals(0, composeRule.onAllNodesWithTag("confirm_delete_note").fetchSemanticsNodes().size)
    }
}
