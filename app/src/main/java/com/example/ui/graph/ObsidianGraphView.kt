package com.example.ui.graph

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.NoteEntity
import com.example.domain.physics.GraphNode
import com.example.domain.physics.GraphState
import com.example.ui.theme.KineticDarkBackground
import com.example.ui.theme.KineticDarkBorder
import com.example.ui.theme.KineticDarkSurface
import com.example.ui.theme.KineticPrimary
import com.example.ui.theme.KineticSecondary
import com.example.ui.theme.KineticTextMuted
import com.example.ui.theme.KineticTextPrimary
import com.example.ui.theme.KineticTextSecondary
import kotlin.math.sqrt

@Composable
fun ObsidianGraphView(
    graphState: GraphState,
    notes: List<NoteEntity>,
    onNodeDrag: (nodeId: String, newPos: Offset) -> Unit,
    onNodeRelease: (nodeId: String) -> Unit,
    onOpenNote: (NoteEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedNodeId by remember { mutableStateOf<String?>(null) }
    val notesMap = remember(notes) { notes.associateBy { it.id } }

    val selectedNote = selectedNodeId?.let { notesMap[it] }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(KineticDarkBackground)
            .testTag("obsidian_graph_view")
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        // Canvas rendering physics nodes and edges
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(graphState) {
                    detectTapGestures { tapOffset ->
                        // Hit test on nodes
                        var foundNode: GraphNode? = null
                        for (node in graphState.nodes.values) {
                            val dx = node.x - tapOffset.x
                            val dy = node.y - tapOffset.y
                            if (sqrt(dx * dx + dy * dy) <= node.radius + 15f) {
                                foundNode = node
                                break
                            }
                        }
                        selectedNodeId = foundNode?.id
                    }
                }
                .pointerInput(graphState) {
                    var draggedNodeId: String? = null
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            for (node in graphState.nodes.values) {
                                val dx = node.x - startOffset.x
                                val dy = node.y - startOffset.y
                                if (sqrt(dx * dx + dy * dy) <= node.radius + 15f) {
                                    draggedNodeId = node.id
                                    selectedNodeId = node.id
                                    break
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            draggedNodeId?.let { id ->
                                change.consume()
                                val node = graphState.nodes[id]
                                if (node != null) {
                                    onNodeDrag(id, Offset(node.x + dragAmount.x, node.y + dragAmount.y))
                                }
                            }
                        },
                        onDragEnd = {
                            draggedNodeId?.let { onNodeRelease(it) }
                            draggedNodeId = null
                        },
                        onDragCancel = {
                            draggedNodeId?.let { onNodeRelease(it) }
                            draggedNodeId = null
                        }
                    )
                }
        ) {
            // Draw subtle ambient physics boundary ring
            drawCircle(
                color = Color(0x0C6366F1),
                radius = 350f,
                center = Offset(width / 2f, height / 2f)
            )

            // Draw spring edges between nodes
            graphState.edges.forEach { edge ->
                val source = graphState.nodes[edge.sourceId]
                val target = graphState.nodes[edge.targetId]

                if (source != null && target != null) {
                    val isConnectedToSelected = selectedNodeId != null &&
                            (selectedNodeId == source.id || selectedNodeId == target.id)

                    val edgeColor = if (isConnectedToSelected) {
                        KineticSecondary
                    } else {
                        Color(0x3594A3B8)
                    }

                    drawLine(
                        color = edgeColor,
                        start = Offset(source.x, source.y),
                        end = Offset(target.x, target.y),
                        strokeWidth = if (isConnectedToSelected) 2.5f else 1.2f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // Draw nodes with dynamic radius & glowing auras
            graphState.nodes.values.forEach { node ->
                val isSelected = selectedNodeId == node.id
                val nodeColor = try {
                    Color(android.graphics.Color.parseColor(node.colorHex))
                } catch (e: Exception) {
                    KineticPrimary
                }

                val center = Offset(node.x, node.y)

                // Outer glow aura
                drawCircle(
                    color = nodeColor.copy(alpha = if (isSelected) 0.45f else 0.18f),
                    radius = node.radius + (if (isSelected) 10f else 6f),
                    center = center
                )

                // Node core circle
                drawCircle(
                    color = if (isSelected) Color.White else nodeColor,
                    radius = node.radius,
                    center = center
                )

                // Inner core accent
                drawCircle(
                    color = nodeColor,
                    radius = node.radius * 0.6f,
                    center = center
                )
            }
        }

        // Obsidian Top HUD Header
        Surface(
            color = KineticDarkSurface.copy(alpha = 0.9f),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, KineticDarkBorder),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Hub,
                    contentDescription = "Graph",
                    tint = KineticPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${graphState.nodes.size} Nodes • ${graphState.edges.size} Threads",
                    color = KineticTextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "Physics",
                    tint = KineticSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Force Equilibrium",
                    color = KineticTextMuted,
                    fontSize = 11.sp
                )
            }
        }

        // Selected Node Inspection Card (Obsidian focus preview)
        AnimatedVisibility(
            visible = selectedNote != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 88.dp, start = 16.dp, end = 16.dp)
        ) {
            selectedNote?.let { note ->
                val nodeColor = try {
                    Color(android.graphics.Color.parseColor(note.colorHex))
                } catch (e: Exception) {
                    KineticPrimary
                }

                Surface(
                    color = KineticDarkSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, nodeColor),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("graph_node_inspector")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(nodeColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = note.title,
                                color = KineticTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "#${note.tag} • ${note.type.name.lowercase()}",
                                color = KineticTextMuted,
                                fontSize = 11.sp
                            )
                        }

                        Button(
                            onClick = { onOpenNote(note) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = KineticPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_graph_open_note")
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInFull,
                                contentDescription = "Open",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
