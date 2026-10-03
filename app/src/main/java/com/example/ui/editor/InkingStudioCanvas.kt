package com.example.ui.editor

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.InkTool
import com.example.domain.model.InkingStroke
import com.example.domain.model.StrokePoint
import com.example.domain.model.StrokeSerializer
import com.example.ui.theme.KineticDarkBorder
import com.example.ui.theme.KineticDarkSurface
import com.example.ui.theme.KineticDarkSurfaceVariant
import com.example.ui.theme.KineticPrimary
import com.example.ui.theme.KineticSecondary
import com.example.ui.theme.KineticTextMuted
import com.example.ui.theme.KineticTextPrimary
import com.example.ui.theme.KineticTextSecondary

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun InkingStudioCanvas(
    initialStrokeData: String?,
    onStrokesSaved: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTool by remember { mutableStateOf(InkTool.PEN) }
    var currentColor by remember { mutableStateOf(Color(0xFF38BDF8)) } // Cyan default ink
    var strokeWidth by remember { mutableFloatStateOf(6f) }

    val strokes = remember { mutableStateListOf<InkingStroke>() }
    val redoStack = remember { mutableStateListOf<InkingStroke>() }

    var currentPoints by remember { mutableStateOf<List<StrokePoint>>(emptyList()) }

    // Load initial strokes
    LaunchedEffect(initialStrokeData) {
        if (strokes.isEmpty() && !initialStrokeData.isNullOrBlank()) {
            val deserialized = StrokeSerializer.deserialize(initialStrokeData)
            strokes.addAll(deserialized)
        }
    }

    val inkColors = listOf(
        Color(0xFFF1F5F9), // White
        Color(0xFF38BDF8), // Cyan
        Color(0xFF6366F1), // Electric Indigo
        Color(0xFFF59E0B), // Amber
        Color(0xFF10B981), // Emerald
        Color(0xFFF43F5E), // Rose
        Color(0xFF8B5CF6)  // Violet
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("inking_studio_canvas")
    ) {
        // Inking Studio Floating Palette Toolbar
        Surface(
            color = KineticDarkSurfaceVariant,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, KineticDarkBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                // Tool Selectors & Undo/Redo/Clear
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Pen Tool
                    Surface(
                        color = if (currentTool == InkTool.PEN) KineticPrimary else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clickable { currentTool = InkTool.PEN }
                            .padding(horizontal = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Pen",
                                tint = if (currentTool == InkTool.PEN) Color.White else KineticTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pen", fontSize = 11.sp, color = if (currentTool == InkTool.PEN) Color.White else KineticTextSecondary)
                        }
                    }

                    // Marker / Highlighter Tool
                    Surface(
                        color = if (currentTool == InkTool.MARKER) KineticSecondary else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clickable { currentTool = InkTool.MARKER }
                            .padding(horizontal = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Draw,
                                contentDescription = "Marker",
                                tint = if (currentTool == InkTool.MARKER) Color.Black else KineticTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Marker", fontSize = 11.sp, color = if (currentTool == InkTool.MARKER) Color.Black else KineticTextSecondary)
                        }
                    }

                    // Eraser Tool
                    Surface(
                        color = if (currentTool == InkTool.ERASER) KineticDarkBorder else Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clickable { currentTool = InkTool.ERASER }
                            .padding(horizontal = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = "Eraser",
                                tint = if (currentTool == InkTool.ERASER) Color.White else KineticTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Eraser", fontSize = 11.sp, color = if (currentTool == InkTool.ERASER) Color.White else KineticTextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Undo
                    IconButton(
                        onClick = {
                            if (strokes.isNotEmpty()) {
                                val removed = strokes.removeAt(strokes.lastIndex)
                                redoStack.add(removed)
                                onStrokesSaved(StrokeSerializer.serialize(strokes))
                            }
                        },
                        enabled = strokes.isNotEmpty(),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo", tint = if (strokes.isNotEmpty()) KineticTextPrimary else KineticTextMuted, modifier = Modifier.size(16.dp))
                    }

                    // Redo
                    IconButton(
                        onClick = {
                            if (redoStack.isNotEmpty()) {
                                val restored = redoStack.removeAt(redoStack.lastIndex)
                                strokes.add(restored)
                                onStrokesSaved(StrokeSerializer.serialize(strokes))
                            }
                        },
                        enabled = redoStack.isNotEmpty(),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Redo, contentDescription = "Redo", tint = if (redoStack.isNotEmpty()) KineticTextPrimary else KineticTextMuted, modifier = Modifier.size(16.dp))
                    }

                    // Clear Canvas
                    IconButton(
                        onClick = {
                            if (strokes.isNotEmpty()) {
                                strokes.clear()
                                redoStack.clear()
                                onStrokesSaved(StrokeSerializer.serialize(strokes))
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = KineticTextMuted, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Color Swatches & Stroke Width Slider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    inkColors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (currentColor == color) 2.dp else 0.dp,
                                    color = if (currentColor == color) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { currentColor = color }
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Slider(
                        value = strokeWidth,
                        onValueChange = { strokeWidth = it },
                        valueRange = 2f..28f,
                        colors = SliderDefaults.colors(
                            thumbColor = KineticSecondary,
                            activeTrackColor = KineticPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${strokeWidth.toInt()}px", fontSize = 11.sp, color = KineticTextMuted)
                }
            }
        }

        // Hardware-Accelerated Drawing Canvas Layer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(KineticDarkSurface)
                .border(1.dp, KineticDarkBorder, RoundedCornerShape(14.dp))
                .pointerInteropFilter { event ->
                    val x = event.x
                    val y = event.y
                    val pressure = event.pressure

                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            currentPoints = listOf(StrokePoint(x, y, pressure))
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            currentPoints = currentPoints + StrokePoint(x, y, pressure)
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (currentPoints.isNotEmpty()) {
                                if (currentTool == InkTool.ERASER) {
                                    // Remove strokes touched by eraser
                                    val eraserRadius = strokeWidth * 2f
                                    strokes.removeAll { stroke ->
                                        stroke.points.any { pt ->
                                            currentPoints.any { ePt ->
                                                val dx = pt.x - ePt.x
                                                val dy = pt.y - ePt.y
                                                dx * dx + dy * dy < eraserRadius * eraserRadius
                                            }
                                        }
                                    }
                                } else {
                                    val actualColor = if (currentTool == InkTool.MARKER) {
                                        currentColor.copy(alpha = 0.35f)
                                    } else {
                                        currentColor
                                    }
                                    val newStroke = InkingStroke(
                                        color = actualColor.toArgb().toLong(),
                                        width = if (currentTool == InkTool.MARKER) strokeWidth * 2.2f else strokeWidth,
                                        tool = currentTool.name,
                                        points = currentPoints
                                    )
                                    strokes.add(newStroke)
                                    redoStack.clear()
                                }
                                currentPoints = emptyList()
                                onStrokesSaved(StrokeSerializer.serialize(strokes))
                            }
                            true
                        }
                        else -> false
                    }
                }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        // Hardware layer rasterization for latency reduction
                    }
            ) {
                // Render finished strokes
                strokes.forEach { stroke ->
                    val strokeColor = Color(stroke.color.toInt())
                    val isMarker = stroke.tool == "MARKER"
                    val path = stroke.toPath()

                    drawPath(
                        path = path,
                        color = strokeColor,
                        style = Stroke(
                            width = stroke.width,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        ),
                        blendMode = if (isMarker) BlendMode.SrcOver else BlendMode.SrcOver
                    )
                }

                // Render current active stroke in progress
                if (currentPoints.isNotEmpty() && currentTool != InkTool.ERASER) {
                    val activeColor = if (currentTool == InkTool.MARKER) currentColor.copy(alpha = 0.35f) else currentColor
                    val tempStroke = InkingStroke(
                        color = activeColor.toArgb().toLong(),
                        width = if (currentTool == InkTool.MARKER) strokeWidth * 2.2f else strokeWidth,
                        tool = currentTool.name,
                        points = currentPoints
                    )
                    drawPath(
                        path = tempStroke.toPath(),
                        color = activeColor,
                        style = Stroke(
                            width = tempStroke.width,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            if (strokes.isEmpty() && currentPoints.isEmpty()) {
                Text(
                    text = "Draw with S-Pen stylus or finger • Low-latency smoothing",
                    color = KineticTextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
