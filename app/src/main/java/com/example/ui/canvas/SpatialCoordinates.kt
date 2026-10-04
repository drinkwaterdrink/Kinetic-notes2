package com.example.ui.canvas

import androidx.compose.ui.geometry.Offset
import com.example.data.local.NoteEntity
import kotlin.math.roundToInt

/**
 * Single, explicit spatial coordinate contract for Kinetic Canvas.
 *
 * Coordinate definitions:
 * - World Coordinates (Float): Persistent card positions, connection anchor points,
 *   and logical board dimensions. Independent of viewport pan and zoom.
 * - Screen Coordinates (Float): Raw pointer events, touch centroids, and viewport dimensions.
 * - Viewport State: Viewport pan (in screen pixels) and zoom scale factor (0.4x - 2.0x).
 */
object SpatialGridConfig {
    const val DEFAULT_GRID_SPACING = 26f
    const val MIN_ZOOM = 0.4f
    const val MAX_ZOOM = 2.0f
    const val CARD_WIDTH_WORLD = 210f
    const val CARD_HEIGHT_WORLD = 140f

    /**
     * Smallest dot spacing drawn on screen. Below this the grid is halved in density
     * (level of detail) so zooming out can never explode the dot count or turn the
     * background into noise.
     */
    const val MIN_GRID_SPACING_PX = 22f
}

data class ViewportTransform(
    val panX: Float = 0f,
    val panY: Float = 0f,
    val zoom: Float = 1.0f
) {
    fun worldToScreen(worldPoint: Offset): Offset {
        return Offset(
            x = worldPoint.x * zoom + panX,
            y = worldPoint.y * zoom + panY
        )
    }

    fun screenToWorld(screenPoint: Offset): Offset {
        return Offset(
            x = (screenPoint.x - panX) / zoom,
            y = (screenPoint.y - panY) / zoom
        )
    }

    fun screenDeltaToWorldDelta(screenDelta: Offset): Offset {
        return Offset(
            x = screenDelta.x / zoom,
            y = screenDelta.y / zoom
        )
    }

    fun worldDeltaToScreenDelta(worldDelta: Offset): Offset {
        return Offset(
            x = worldDelta.x * zoom,
            y = worldDelta.y * zoom
        )
    }

    /**
     * Anchored pinch-to-zoom calculation: preserves the exact world point beneath
     * the screen centroid while simultaneously applying any translation delta.
     */
    fun withAnchoredTransform(
        centroidScreen: Offset,
        panDeltaScreen: Offset,
        zoomChange: Float,
        minZoom: Float = SpatialGridConfig.MIN_ZOOM,
        maxZoom: Float = SpatialGridConfig.MAX_ZOOM
    ): ViewportTransform {
        val targetZoom = (zoom * zoomChange).coerceIn(minZoom, maxZoom)

        // World point under screen centroid before zoom:
        // W = (C - pan) / zoom
        // After zoom: C = W * targetZoom + newPan
        // Therefore: newPan = C - W * targetZoom + panDelta
        val worldUnderCentroidX = (centroidScreen.x - panX) / zoom
        val worldUnderCentroidY = (centroidScreen.y - panY) / zoom

        val newPanX = centroidScreen.x - (worldUnderCentroidX * targetZoom) + panDeltaScreen.x
        val newPanY = centroidScreen.y - (worldUnderCentroidY * targetZoom) + panDeltaScreen.y

        return ViewportTransform(
            panX = newPanX,
            panY = newPanY,
            zoom = targetZoom
        )
    }
}

/**
 * Grid snap helper in pure world coordinates.
 */
fun snapToWorldGrid(
    worldPoint: Offset,
    gridSize: Float = SpatialGridConfig.DEFAULT_GRID_SPACING
): Offset {
    val snappedX = (worldPoint.x / gridSize).roundToInt() * gridSize
    val snappedY = (worldPoint.y / gridSize).roundToInt() * gridSize
    return Offset(snappedX, snappedY)
}

/**
 * When pointer input is attached directly to a child inside a graphicsLayer(scale = zoom),
 * Compose delivers pointer drag deltas ALREADY transformed into the layer's local (world) space:
 * localDelta = screenDelta / zoom.
 *
 * Therefore, accumulating localDelta directly onto liveWorldPosition:
 * liveWorldPosition += localDelta
 * ensures that apparent card displacement on screen (localDelta * zoom) strictly equals
 * the physical finger displacement (screenDelta) across all zoom levels (40%, 55%, 100%, 150%).
 */
fun accumulateLocalWorldDrag(
    currentLiveWorldPos: Offset,
    localWorldDelta: Offset
): Offset {
    return Offset(
        x = currentLiveWorldPos.x + localWorldDelta.x,
        y = currentLiveWorldPos.y + localWorldDelta.y
    )
}

/**
 * Accumulates screen delta when pointer input is captured outside the graphicsLayer.
 */
fun accumulateWorldDrag(
    currentLiveWorldPos: Offset,
    screenDelta: Offset,
    zoom: Float
): Offset {
    val worldDx = screenDelta.x / zoom
    val worldDy = screenDelta.y / zoom
    return Offset(
        x = currentLiveWorldPos.x + worldDx,
        y = currentLiveWorldPos.y + worldDy
    )
}

/**
 * Computes Bézier connection anchors dynamically based on card world geometry.
 */
data class LinkAnchors(
    val sourceAnchor: Offset,
    val targetAnchor: Offset,
    val controlPoint1: Offset,
    val controlPoint2: Offset
)

fun calculateLinkAnchors(
    sourcePos: Offset,
    targetPos: Offset,
    cardWidth: Float = SpatialGridConfig.CARD_WIDTH_WORLD,
    cardHeight: Float = SpatialGridConfig.CARD_HEIGHT_WORLD
): LinkAnchors {
    val sCenter = Offset(sourcePos.x + cardWidth / 2f, sourcePos.y + cardHeight / 2f)
    val tCenter = Offset(targetPos.x + cardWidth / 2f, targetPos.y + cardHeight / 2f)

    val sAnchor = if (sCenter.x < tCenter.x) Offset(sourcePos.x + cardWidth, sCenter.y) else Offset(sourcePos.x, sCenter.y)
    val tAnchor = if (sCenter.x < tCenter.x) Offset(targetPos.x, tCenter.y) else Offset(targetPos.x + cardWidth, tCenter.y)

    val dx = kotlin.math.abs(tAnchor.x - sAnchor.x) * 0.5f
    val c1 = if (sAnchor.x < tAnchor.x) Offset(sAnchor.x + dx, sAnchor.y) else Offset(sAnchor.x - dx, sAnchor.y)
    val c2 = if (sAnchor.x < tAnchor.x) Offset(tAnchor.x - dx, tAnchor.y) else Offset(tAnchor.x + dx, tAnchor.y)

    return LinkAnchors(
        sourceAnchor = sAnchor,
        targetAnchor = tAnchor,
        controlPoint1 = c1,
        controlPoint2 = c2
    )
}

/**
 * Axis-aligned bounds of a set of cards, in world coordinates.
 */
data class WorldBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(1f)
    val height: Float get() = (bottom - top).coerceAtLeast(1f)
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
}

/**
 * World bounds covering every supplied note card, or null when there is nothing to frame.
 */
fun noteWorldBounds(
    notes: List<NoteEntity>,
    cardWidth: Float = SpatialGridConfig.CARD_WIDTH_WORLD,
    cardHeight: Float = SpatialGridConfig.CARD_HEIGHT_WORLD
): WorldBounds? {
    if (notes.isEmpty()) return null
    var left = Float.MAX_VALUE
    var top = Float.MAX_VALUE
    var right = -Float.MAX_VALUE
    var bottom = -Float.MAX_VALUE
    notes.forEach { note ->
        if (note.x < left) left = note.x
        if (note.y < top) top = note.y
        if (note.x + cardWidth > right) right = note.x + cardWidth
        if (note.y + cardHeight > bottom) bottom = note.y + cardHeight
    }
    return WorldBounds(left, top, right, bottom)
}

/**
 * "Fit Notes": centres [bounds] in the viewport and picks the largest zoom (within limits)
 * that keeps everything on screen with a comfortable padding margin.
 */
fun fitViewportToBounds(
    bounds: WorldBounds,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    paddingPx: Float = 72f,
    minZoom: Float = SpatialGridConfig.MIN_ZOOM,
    maxZoom: Float = SpatialGridConfig.MAX_ZOOM
): ViewportTransform {
    val usableWidth = (viewportWidthPx - paddingPx * 2f).coerceAtLeast(1f)
    val usableHeight = (viewportHeightPx - paddingPx * 2f).coerceAtLeast(1f)

    val zoom = kotlin.math.min(usableWidth / bounds.width, usableHeight / bounds.height)
        .coerceIn(minZoom, maxZoom)

    return ViewportTransform(
        panX = viewportWidthPx / 2f - bounds.centerX * zoom,
        panY = viewportHeightPx / 2f - bounds.centerY * zoom,
        zoom = zoom
    )
}

/**
 * True when at least one card overlaps the visible viewport rectangle. Used to offer a
 * "Find notes" escape hatch after the user has panned away into empty space.
 */
fun isAnyNoteVisible(
    notes: List<NoteEntity>,
    viewport: ViewportTransform,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    cardWidth: Float = SpatialGridConfig.CARD_WIDTH_WORLD,
    cardHeight: Float = SpatialGridConfig.CARD_HEIGHT_WORLD
): Boolean {
    if (notes.isEmpty() || viewportWidthPx <= 0f || viewportHeightPx <= 0f) return true
    return notes.any { note ->
        val topLeft = viewport.worldToScreen(Offset(note.x, note.y))
        val bottomRight = viewport.worldToScreen(Offset(note.x + cardWidth, note.y + cardHeight))
        bottomRight.x > 0f && topLeft.x < viewportWidthPx &&
            bottomRight.y > 0f && topLeft.y < viewportHeightPx
    }
}

/**
 * Where a brand new card should be dropped: visually centred in the current viewport,
 * with a small cascade so repeated taps do not stack cards exactly on top of each other.
 */
fun spawnPositionForNewCard(
    viewport: ViewportTransform,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    cascadeOffsetWorld: Float = 0f,
    cardWidth: Float = SpatialGridConfig.CARD_WIDTH_WORLD,
    cardHeight: Float = SpatialGridConfig.CARD_HEIGHT_WORLD
): Offset {
    val centerWorld = viewport.screenToWorld(Offset(viewportWidthPx / 2f, viewportHeightPx / 2f))
    return Offset(
        x = centerWorld.x - cardWidth / 2f + cascadeOffsetWorld,
        y = centerWorld.y - cardHeight / 2f + cascadeOffsetWorld
    )
}
