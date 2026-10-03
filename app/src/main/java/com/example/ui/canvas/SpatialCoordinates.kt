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
