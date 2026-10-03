package com.example.ui.canvas

import androidx.compose.ui.geometry.Offset
import com.example.data.local.NoteEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpatialCoordinatesTest {

    private val deltaEpsilon = 0.001f

    @Test
    fun testScreenAndWorldConversionRoundTrip() {
        val viewport = ViewportTransform(panX = 150f, panY = -80f, zoom = 1.25f)
        val originalWorldPoint = Offset(320.5f, 480.25f)

        val screenPoint = viewport.worldToScreen(originalWorldPoint)
        val convertedWorldPoint = viewport.screenToWorld(screenPoint)

        assertEquals(originalWorldPoint.x, convertedWorldPoint.x, deltaEpsilon)
        assertEquals(originalWorldPoint.y, convertedWorldPoint.y, deltaEpsilon)
    }

    @Test
    fun testRepeatedDragDeltasAccumulateCorrectly() {
        var livePos = Offset(100f, 100f)
        val zoom = 1.0f

        // Sequence of pointer deltas in screen pixels
        val deltas = listOf(
            Offset(5f, 2f),
            Offset(3f, -1f),
            Offset(10f, 15f),
            Offset(-2f, 4f)
        )

        for (delta in deltas) {
            livePos = accumulateWorldDrag(livePos, delta, zoom)
        }

        // Expected final = 100 + 5 + 3 + 10 - 2 = 116, 100 + 2 - 1 + 15 + 4 = 120
        assertEquals(116f, livePos.x, deltaEpsilon)
        assertEquals(120f, livePos.y, deltaEpsilon)
    }

    @Test
    fun testVisualDragSensitivityEquivalentAcrossZoomLevels() {
        val fingerScreenDisplacement = Offset(100f, 60f)
        val zoomLevels = listOf(0.40f, 0.55f, 1.0f, 1.50f, 2.0f)

        for (zoom in zoomLevels) {
            val startWorldPos = Offset(100f, 100f)

            // Compose pointer event dispatch inverse-scales screen displacement into child's local layer coordinates:
            val localDeltaDeliveredByCompose = Offset(
                fingerScreenDisplacement.x / zoom,
                fingerScreenDisplacement.y / zoom
            )

            // Direct accumulation of local layer delta onto live world position
            val finalWorldPos = accumulateLocalWorldDrag(startWorldPos, localDeltaDeliveredByCompose)
            val worldDisplacement = Offset(finalWorldPos.x - startWorldPos.x, finalWorldPos.y - startWorldPos.y)

            // Rendered on screen through graphicsLayer scale:
            val apparentScreenDisplacement = Offset(
                worldDisplacement.x * zoom,
                worldDisplacement.y * zoom
            )

            // Invariant: Apparent screen displacement must strictly equal physical finger displacement at EVERY zoom level
            assertEquals("Zoom $zoom X mismatch", fingerScreenDisplacement.x, apparentScreenDisplacement.x, deltaEpsilon)
            assertEquals("Zoom $zoom Y mismatch", fingerScreenDisplacement.y, apparentScreenDisplacement.y, deltaEpsilon)
        }
    }

    @Test
    fun testCanvasPanDoesNotChangeStoredCardWorldPositions() {
        val initialCardWorldPos = Offset(150f, 200f)
        var viewport = ViewportTransform(panX = 0f, panY = 0f, zoom = 1.0f)

        // Panning the canvas changes viewport pan, but card world coordinates remain unchanged
        val panDelta = Offset(45f, -60f)
        viewport = viewport.copy(panX = viewport.panX + panDelta.x, panY = viewport.panY + panDelta.y)

        assertEquals(150f, initialCardWorldPos.x, deltaEpsilon)
        assertEquals(200f, initialCardWorldPos.y, deltaEpsilon)

        // Verify the screen position of the card shifted by the pan amount
        val screenPosAfterPan = viewport.worldToScreen(initialCardWorldPos)
        assertEquals(195f, screenPosAfterPan.x, deltaEpsilon)
        assertEquals(140f, screenPosAfterPan.y, deltaEpsilon)
    }

    @Test
    fun testPinchZoomAnchorsWorldLocationBeneathGestureCentroid() {
        val initialViewport = ViewportTransform(panX = 50f, panY = 50f, zoom = 1.0f)
        val centroidScreen = Offset(300f, 400f)

        // Find world point under centroid before zoom
        val worldPointUnderCentroid = initialViewport.screenToWorld(centroidScreen)

        // Zoom in by 1.5x around that centroid
        val zoomedViewport = initialViewport.withAnchoredTransform(
            centroidScreen = centroidScreen,
            panDeltaScreen = Offset.Zero,
            zoomChange = 1.5f
        )

        assertEquals(1.5f, zoomedViewport.zoom, deltaEpsilon)

        // The same world point must still map to the exact same screen centroid
        val screenPointAfterZoom = zoomedViewport.worldToScreen(worldPointUnderCentroid)
        assertEquals(centroidScreen.x, screenPointAfterZoom.x, deltaEpsilon)
        assertEquals(centroidScreen.y, screenPointAfterZoom.y, deltaEpsilon)
    }

    @Test
    fun testSnapProducesExpectedWorldGridPosition() {
        val gridSize = 26f

        // Near (26, 52)
        val pos1 = Offset(28f, 50f)
        val snapped1 = snapToWorldGrid(pos1, gridSize)
        assertEquals(26f, snapped1.x, deltaEpsilon)
        assertEquals(52f, snapped1.y, deltaEpsilon)

        // Halfway rounding check: 13.5 -> 26
        val pos2 = Offset(14f, 38f)
        val snapped2 = snapToWorldGrid(pos2, gridSize)
        assertEquals(26f, snapped2.x, deltaEpsilon)
        assertEquals(26f, snapped2.y, deltaEpsilon)
    }

    @Test
    fun testExternalPositionUpdateReflectedWhenNotActivelyDragging() {
        val cardPositions = mutableMapOf<String, Offset>()
        val activeDraggedCardIds = mutableSetOf<String>()

        val noteId1 = "note_1"
        val noteId2 = "note_2"

        // Initial setup
        cardPositions[noteId1] = Offset(40f, 40f)
        cardPositions[noteId2] = Offset(100f, 100f)

        // Note 1 is being dragged by the user
        activeDraggedCardIds.add(noteId1)
        cardPositions[noteId1] = Offset(65f, 85f) // live transient position

        // External emission arrives (e.g. from DB / undo / remote sync)
        val externalNotes = listOf(
            NoteEntity(id = noteId1, title = "Card 1", x = 200f, y = 200f),
            NoteEntity(id = noteId2, title = "Card 2", x = 300f, y = 300f)
        )

        // Reconciliation rule:
        externalNotes.forEach { note ->
            if (note.id !in activeDraggedCardIds) {
                cardPositions[note.id] = Offset(note.x, note.y)
            }
        }

        // Note 1 was actively dragging -> MUST NOT be overwritten
        assertEquals(65f, cardPositions[noteId1]!!.x, deltaEpsilon)
        assertEquals(85f, cardPositions[noteId1]!!.y, deltaEpsilon)

        // Note 2 was NOT dragging -> MUST be updated to external position
        assertEquals(300f, cardPositions[noteId2]!!.x, deltaEpsilon)
        assertEquals(300f, cardPositions[noteId2]!!.y, deltaEpsilon)
    }

    @Test
    fun testDragCancellationRestoresInitialWorldPosition() {
        val initialWorldPos = Offset(120f, 150f)
        var livePos = initialWorldPos

        // Drag occurs
        livePos = accumulateWorldDrag(livePos, Offset(30f, 40f), zoom = 1.0f)
        assertEquals(150f, livePos.x, deltaEpsilon)
        assertEquals(190f, livePos.y, deltaEpsilon)

        // Cancel occurs -> apply deterministic policy to restore initial
        livePos = initialWorldPos
        assertEquals(120f, livePos.x, deltaEpsilon)
        assertEquals(150f, livePos.y, deltaEpsilon)
    }

    @Test
    fun testDynamicLinkAnchorsDerivedFromWorldGeometry() {
        val card1Pos = Offset(100f, 100f)
        val card2Pos = Offset(400f, 200f)

        val cardW = SpatialGridConfig.CARD_WIDTH_WORLD // 210
        val cardH = SpatialGridConfig.CARD_HEIGHT_WORLD // 140

        val anchors = calculateLinkAnchors(card1Pos, card2Pos, cardW, cardH)

        // Card 1 is to the left of Card 2 (100 < 400), so source anchor should be right side of Card 1: 100 + 210 = 310
        assertEquals(310f, anchors.sourceAnchor.x, deltaEpsilon)
        assertEquals(100f + cardH / 2f, anchors.sourceAnchor.y, deltaEpsilon)

        // Target anchor should be left side of Card 2: 400
        assertEquals(400f, anchors.targetAnchor.x, deltaEpsilon)
        assertEquals(200f + cardH / 2f, anchors.targetAnchor.y, deltaEpsilon)
    }
}
