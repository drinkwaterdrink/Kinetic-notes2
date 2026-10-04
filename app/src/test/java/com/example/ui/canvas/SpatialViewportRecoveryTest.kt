package com.example.ui.canvas

import androidx.compose.ui.geometry.Offset
import com.example.data.local.NoteEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpatialViewportRecoveryTest {
    @Test
    fun boundsIncludeWholeCards() {
        val bounds = noteWorldBounds(
            listOf(NoteEntity(id = "a", title = "A", x = 20f, y = 40f), NoteEntity(id = "b", title = "B", x = 400f, y = 300f))
        )!!
        assertEquals(20f, bounds.left, 0.001f)
        assertEquals(40f, bounds.top, 0.001f)
        assertEquals(610f, bounds.right, 0.001f)
        assertEquals(440f, bounds.bottom, 0.001f)
    }

    @Test
    fun fitCentersBoundsWithPadding() {
        val bounds = WorldBounds(0f, 0f, 500f, 300f)
        val viewport = fitViewportToBounds(bounds, viewportWidthPx = 1000f, viewportHeightPx = 800f, paddingPx = 100f)
        assertEquals(1.6f, viewport.zoom, 0.001f)
        val center = viewport.worldToScreen(Offset(bounds.centerX, bounds.centerY))
        assertEquals(500f, center.x, 0.001f)
        assertEquals(400f, center.y, 0.001f)
    }

    @Test
    fun fitNeverExceedsZoomBounds() {
        val huge = fitViewportToBounds(WorldBounds(0f, 0f, 10_000f, 10_000f), 400f, 400f)
        assertEquals(SpatialGridConfig.MIN_ZOOM, huge.zoom, 0.001f)
        val tiny = fitViewportToBounds(WorldBounds(0f, 0f, 1f, 1f), 400f, 400f)
        assertEquals(SpatialGridConfig.MAX_ZOOM, tiny.zoom, 0.001f)
    }

    @Test
    fun offScreenDetectionAndSpawnUseSameViewportContract() {
        val notes = listOf(NoteEntity(id = "a", title = "A", x = 100f, y = 100f))
        val viewport = ViewportTransform(panX = 0f, panY = 0f, zoom = 1f)
        assertTrue(isAnyNoteVisible(notes, viewport, 500f, 500f))
        assertFalse(isAnyNoteVisible(notes, ViewportTransform(panX = -1_000f, panY = -1_000f, zoom = 1f), 500f, 500f))
        val spawn = spawnPositionForNewCard(viewport, 500f, 500f)
        assertEquals(145f, spawn.x, 0.001f)
        assertEquals(180f, spawn.y, 0.001f)
    }
}
