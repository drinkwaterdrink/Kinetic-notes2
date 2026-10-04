package com.example.ui.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.example.data.local.NoteEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Interaction contract for the spatial board:
 *   tap a card   -> open it in the editor (no OPEN button, no long press, no Focus control)
 *   drag a card  -> move it, and never open the editor
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SpatialCanvasInteractionTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val note = NoteEntity(id = "card-1", title = "Tap me", content = "body", x = 60f, y = 60f)

    private fun setCanvas(
        onNoteClick: (NoteEntity) -> Unit = {},
        onCardMove: (String, Float, Float) -> Unit = { _, _, _ -> },
        isLinkingMode: Boolean = false,
        onCardTapInLinkingMode: (String) -> Unit = {}
    ) {
        composeRule.setContent {
            SpatialCanvasView(
                notes = listOf(note),
                links = emptyList(),
                zoom = 1f,
                panX = 0f,
                panY = 0f,
                isLinkingMode = isLinkingMode,
                linkSourceNoteId = null,
                isSnapToGrid = false,
                onTransformChange = { _, _, _ -> },
                onZoomIn = {},
                onZoomOut = {},
                onFitNotes = {},
                onCardMove = onCardMove,
                onNoteClick = onNoteClick,
                onCardTapInLinkingMode = onCardTapInLinkingMode,
                onDeleteNote = {},
                onNewNoteClick = {}
            )
        }
    }

    @Test
    fun tappingACardOpensItInTheEditor() {
        var opened: NoteEntity? = null
        setCanvas(onNoteClick = { opened = it })

        composeRule.onNodeWithTag("canvas_card_card-1").performClick()
        composeRule.waitForIdle()

        assertEquals("card-1", opened?.id)
    }

    @Test
    fun smallFingerWobbleStillCountsAsATap() {
        var opened: NoteEntity? = null
        var moved = false
        setCanvas(onNoteClick = { opened = it }, onCardMove = { _, _, _ -> moved = true })

        composeRule.onNodeWithTag("canvas_card_card-1").performTouchInput {
            down(center)
            moveBy(Offset(2f, 2f))
            up()
        }
        composeRule.waitForIdle()

        assertEquals("A 2px wobble must still open the note", "card-1", opened?.id)
        assertTrue("A wobble must not be persisted as a move", !moved)
    }

    @Test
    fun draggingACardMovesItWithoutOpeningTheEditor() {
        var opened: NoteEntity? = null
        var movedTo: Pair<Float, Float>? = null
        setCanvas(
            onNoteClick = { opened = it },
            onCardMove = { _, x, y -> movedTo = x to y }
        )

        composeRule.onNodeWithTag("canvas_card_card-1").performTouchInput {
            down(center)
            moveBy(Offset(120f, 90f))
            moveBy(Offset(30f, 20f))
            up()
        }
        composeRule.waitForIdle()

        assertEquals("Dragging must not open the editor", null, opened)
        assertTrue("Dragging must persist a new position", movedTo != null)
        val (x, y) = movedTo!!
        assertTrue("Card should have moved right/down from (60,60) but was ($x,$y)", x > 60f && y > 60f)
    }

    @Test
    fun tappingACardInLinkingModeLinksInsteadOfOpening() {
        var opened: NoteEntity? = null
        var linkTarget: String? = null
        setCanvas(
            onNoteClick = { opened = it },
            isLinkingMode = true,
            onCardTapInLinkingMode = { linkTarget = it }
        )

        composeRule.onNodeWithTag("canvas_card_card-1").performClick()
        composeRule.waitForIdle()

        assertEquals("card-1", linkTarget)
        assertEquals(null, opened)
    }
}
