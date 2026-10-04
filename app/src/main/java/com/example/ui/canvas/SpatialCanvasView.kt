package com.example.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.local.NoteEntity
import com.example.data.local.NoteLinkEntity
import com.example.data.local.NoteType
import com.example.ui.common.relativeTimeLabel
import com.example.ui.theme.KineticDarkBackground
import com.example.ui.theme.KineticPrimary
import com.example.ui.theme.KineticSecondary
import com.example.ui.theme.KineticTextMuted
import com.example.ui.theme.KineticTextSecondary
import kotlin.math.roundToInt

@Composable
fun SpatialCanvasView(
    notes: List<NoteEntity>,
    links: List<NoteLinkEntity>,
    zoom: Float,
    panX: Float,
    panY: Float,
    isLinkingMode: Boolean,
    linkSourceNoteId: String?,
    isSnapToGrid: Boolean,
    onTransformChange: (centroid: Offset, panDelta: Offset, zoomChange: Float) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    onCardMove: (noteId: String, newX: Float, newY: Float) -> Unit,
    onNoteClick: (NoteEntity) -> Unit,
    onCardTapInLinkingMode: (String) -> Unit,
    onDeleteNote: (String) -> Unit,
    onNewNoteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // In-memory reactive world coordinates for high-frequency direct finger tracking
    val cardPositions = remember { mutableStateMapOf<String, Offset>() }
    val activeDraggedCardIds = remember { mutableSetOf<String>() }
    var activeDraggedId by remember { mutableStateOf<String?>(null) }

    // External position reconciliation rule:
    // Update non-dragged cards from external emissions (database/undo/auto-sort); retain existing IDs
    LaunchedEffect(notes) {
        val currentIds = notes.map { it.id }.toSet()
        cardPositions.keys.retainAll(currentIds)
        notes.forEach { note ->
            if (note.id !in activeDraggedCardIds) {
                cardPositions[note.id] = Offset(note.x, note.y)
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(KineticDarkBackground)
            // Viewport pan & anchored pinch-to-zoom on spatial canvas background
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoomFactor, _ ->
                    onTransformChange(centroid, pan, zoomFactor)
                }
            }
            .testTag("spatial_canvas")
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val gridSize = SpatialGridConfig.DEFAULT_GRID_SPACING

        // 1. Architectural Dot-Matrix Canvas Grid Background
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = panX
                    translationY = panY
                    scaleX = zoom
                    scaleY = zoom
                }
        ) {
            val startX = -panX / zoom - 200f
            val endX = (-panX + widthPx) / zoom + 200f
            val startY = -panY / zoom - 200f
            val endY = (-panY + heightPx) / zoom + 200f

            val firstGridX = (startX / gridSize).toInt() * gridSize
            val firstGridY = (startY / gridSize).toInt() * gridSize

            var currX = firstGridX
            while (currX <= endX) {
                var currY = firstGridY
                while (currY <= endY) {
                    drawCircle(
                        color = Color(0x28FFFFFF),
                        radius = 1.3f,
                        center = Offset(currX, currY)
                    )
                    currY += gridSize
                }
                currX += gridSize
            }
        }

        // 2. Dashed Bézier Link Threads between connected notes (evaluated in Draw phase)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = panX
                    translationY = panY
                    scaleX = zoom
                    scaleY = zoom
                }
        ) {
            links.forEach { link ->
                val sourcePos = cardPositions[link.sourceId]
                val targetPos = cardPositions[link.targetId]

                if (sourcePos != null && targetPos != null) {
                    val anchors = calculateLinkAnchors(
                        sourcePos = sourcePos,
                        targetPos = targetPos,
                        cardWidth = SpatialGridConfig.CARD_WIDTH_WORLD,
                        cardHeight = SpatialGridConfig.CARD_HEIGHT_WORLD
                    )

                    val path = Path().apply {
                        moveTo(anchors.sourceAnchor.x, anchors.sourceAnchor.y)
                        cubicTo(
                            anchors.controlPoint1.x, anchors.controlPoint1.y,
                            anchors.controlPoint2.x, anchors.controlPoint2.y,
                            anchors.targetAnchor.x, anchors.targetAnchor.y
                        )
                    }

                    val threadColor = try {
                        Color(android.graphics.Color.parseColor(link.colorHex))
                    } catch (e: Exception) {
                        KineticSecondary
                    }

                    // Dashed bezier curve
                    drawPath(
                        path = path,
                        color = threadColor.copy(alpha = 0.85f),
                        style = Stroke(
                            width = 2.2f,
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                        )
                    )

                    // Anchors
                    drawCircle(color = threadColor, radius = 4f, center = anchors.sourceAnchor)
                    drawCircle(color = threadColor, radius = 4f, center = anchors.targetAnchor)
                }
            }
        }

        // 3. Movable Spatial Cards in World Space
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = panX
                    translationY = panY
                    scaleX = zoom
                    scaleY = zoom
                }
        ) {
            notes.forEach { note ->
                val isSelectedForLinking = isLinkingMode && linkSourceNoteId == note.id
                val isCardActive = activeDraggedId == note.id

                ReferenceNoteCardItem(
                    note = note,
                    getPosition = { cardPositions[note.id] ?: Offset(note.x, note.y) },
                    zoom = zoom,
                    isSelectedForLinking = isSelectedForLinking,
                    isActiveDragged = isCardActive,
                    onCardClick = {
                        if (isLinkingMode) {
                            onCardTapInLinkingMode(note.id)
                        } else {
                            onNoteClick(note)
                        }
                    },
                    onConnectClick = { onCardTapInLinkingMode(note.id) },
                    onDeleteClick = { onDeleteNote(note.id) },
                    onDragStart = {
                        activeDraggedCardIds.add(note.id)
                        activeDraggedId = note.id
                    },
                    onDragDelta = { localDelta ->
                        if (!isLinkingMode) {
                            val currentLive = cardPositions[note.id] ?: Offset(note.x, note.y)
                            // Direct 1:1 finger tracking: Compose delivers dragAmount in local layer coordinates!
                            val updated = accumulateLocalWorldDrag(currentLive, localDelta)
                            cardPositions[note.id] = updated
                        }
                    },
                    onDragEnd = {
                        activeDraggedCardIds.remove(note.id)
                        activeDraggedId = null
                        if (!isLinkingMode) {
                            val livePos = cardPositions[note.id] ?: Offset(note.x, note.y)
                            val finalPos = if (isSnapToGrid) {
                                snapToWorldGrid(livePos, SpatialGridConfig.DEFAULT_GRID_SPACING)
                            } else {
                                livePos
                            }
                            cardPositions[note.id] = finalPos
                            // Persist exactly one position update to Room
                            onCardMove(note.id, finalPos.x, finalPos.y)
                        }
                    },
                    onDragCancel = { initialWorldPos ->
                        activeDraggedCardIds.remove(note.id)
                        activeDraggedId = null
                        // Deterministic cancellation policy: restore to initial position prior to gesture
                        cardPositions[note.id] = initialWorldPos
                    }
                )
            }
        }

        // Empty Canvas State
        if (notes.isEmpty()) {
            Surface(
                color = Color(0xFF131722).copy(alpha = 0.95f),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x20FFFFFF)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Your canvas is empty",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Capture a thought and place it anywhere.",
                        color = KineticTextMuted,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.material3.Button(
                        onClick = onNewNoteClick,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = KineticPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ New Note", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }

        // 4. Floating Zoom HUD Pill on Bottom-Left - elevated to prevent overlap
        Surface(
            color = Color(0xFF131722).copy(alpha = 0.94f),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x28FFFFFF)),
            shadowElevation = 10.dp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 28.dp)
                .testTag("zoom_hud_capsule")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                IconButton(
                    onClick = onZoomOut,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Zoom Out",
                        tint = KineticTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = "${(zoom * 100).roundToInt()}%",
                    color = KineticSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                IconButton(
                    onClick = onZoomIn,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Zoom In",
                        tint = KineticTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .height(14.dp)
                        .width(1.dp)
                        .background(Color(0x28FFFFFF))
                        .padding(horizontal = 2.dp)
                )

                Text(
                    text = "Reset",
                    color = KineticTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable { onResetZoom() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun ReferenceNoteCardItem(
    note: NoteEntity,
    getPosition: () -> Offset,
    zoom: Float,
    isSelectedForLinking: Boolean,
    isActiveDragged: Boolean,
    onCardClick: () -> Unit,
    onConnectClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDragStart: () -> Unit,
    onDragDelta: (screenDelta: Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: (initialWorldPos: Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = try {
        Color(android.graphics.Color.parseColor(note.colorHex))
    } catch (e: Exception) {
        KineticPrimary
    }

    Surface(
        color = Color(0xFF131722).copy(alpha = 0.95f),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelectedForLinking) 2.dp else 1.dp,
            color = if (isSelectedForLinking) KineticSecondary else Color(0x22FFFFFF)
        ),
        shadowElevation = if (isActiveDragged) 16.dp else 8.dp,
        modifier = modifier
            // Active-card layering: dragged card appears on top of overlapping cards
            .zIndex(if (isActiveDragged) 100f else (note.zIndex.toFloat()))
            // Layout phase offset read: zero recomposition of card body during drag
            .offset {
                val pos = getPosition()
                IntOffset(pos.x.roundToInt(), pos.y.roundToInt())
            }
            .width(SpatialGridConfig.CARD_WIDTH_WORLD.dp)
            // Tap-to-edit. detectTapGestures only reports a tap when the pointer never
            // travelled past touch slop, and it bails out as soon as the drag detector below
            // consumes a move - so tap and drag can never fight each other. A tap that wobbles
            // by a few pixels is still a tap.
            .pointerInput(note.id) {
                detectTapGestures(onTap = { onCardClick() })
            }
            // Drag-to-move. Compose delivers dragAmount in this node's LOCAL (world) space
            // because the node lives inside the zoomed graphicsLayer, which is exactly what
            // accumulateLocalWorldDrag expects.
            .pointerInput(note.id, zoom) {
                var initialWorldPos = Offset.Zero

                detectDragGestures(
                    onDragStart = {
                        initialWorldPos = getPosition()
                        onDragStart()
                    },
                    onDragEnd = { onDragEnd() },
                    onDragCancel = { onDragCancel(initialWorldPos) },
                    onDrag = { change, dragAmount ->
                        // Consume so the parent canvas does not pan while a card is moving.
                        change.consume()
                        onDragDelta(dragAmount)
                    }
                )
            }
            .testTag("canvas_card_${note.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Header: Category Pill with Glowing Dot + Connect + Archive
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Category Tag Pill
                Surface(
                    color = Color(0xFF0F131D),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x1AFFFFFF))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = note.tag,
                            color = KineticTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Connect Thread Button
                IconButton(
                    onClick = onConnectClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "Connect",
                        tint = KineticTextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Delete / Archive Button
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Archive",
                        tint = KineticTextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Title
            Text(
                text = note.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Body Preview based on Note Type
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            ) {
                when (note.type) {
                    NoteType.CHECKLIST -> {
                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = "CHECKLIST",
                                color = KineticTextMuted,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = note.content.ifBlank { "Tap to add tasks" },
                                color = KineticTextSecondary,
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    NoteType.CODE -> {
                        Surface(
                            color = Color(0xFF090D15),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x18FFFFFF)),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Column(modifier = Modifier.padding(6.dp)) {
                                Text(
                                    text = note.codeSnippet ?: "const midX = (x1 + x2) / 2;",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    else -> {
                        if (note.isLocked) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Encrypted",
                                    tint = KineticTextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Encrypted Note",
                                    color = KineticTextMuted,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        } else {
                            Text(
                                text = note.content.ifBlank { "Tap card to edit notes, write markdown, and [[Link]]..." },
                                color = KineticTextSecondary,
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer: Timestamp + FOCUS ⛶ Trigger
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = Color(0x10FFFFFF),
                        shape = RoundedCornerShape(0.dp)
                    )
                    .padding(top = 4.dp)
            ) {
                Text(
                    text = relativeTimeLabel(note.updatedAt),
                    color = KineticTextMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.weight(1f))

                if (note.isPinned) {
                    Text(
                        text = "PINNED",
                        color = KineticPrimary.copy(alpha = 0.8f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
