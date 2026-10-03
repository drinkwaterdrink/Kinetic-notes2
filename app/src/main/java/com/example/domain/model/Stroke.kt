package com.example.domain.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

enum class InkTool {
    PEN,
    MARKER,
    ERASER
}

@JsonClass(generateAdapter = true)
data class StrokePoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1.0f
)

@JsonClass(generateAdapter = true)
data class InkingStroke(
    val color: Long, // Color ARGB as Long
    val width: Float,
    val tool: String, // PEN, MARKER, ERASER
    val points: List<StrokePoint>
) {
    fun toPath(): Path {
        val path = Path()
        if (points.isEmpty()) return path

        path.moveTo(points.first().x, points.first().y)
        if (points.size == 1) {
            path.lineTo(points.first().x + 0.5f, points.first().y + 0.5f)
            return path
        }

        // Catmull-Rom / Bézier smoothing across sampled coordinates
        for (i in 0 until points.size - 1) {
            val p0 = points.getOrElse(i - 1) { points[i] }
            val p1 = points[i]
            val p2 = points[i + 1]
            val p3 = points.getOrElse(i + 2) { p2 }

            // Midpoint quadratic / cubic smoothing
            val midX = (p1.x + p2.x) / 2f
            val midY = (p1.y + p2.y) / 2f
            path.quadraticTo(p1.x, p1.y, midX, midY)
        }

        path.lineTo(points.last().x, points.last().y)
        return path
    }
}

object StrokeSerializer {
    private val moshi = Moshi.Builder().build()
    private val listType = Types.newParameterizedType(List::class.java, InkingStroke::class.java)
    private val adapter = moshi.adapter<List<InkingStroke>>(listType)

    fun serialize(strokes: List<InkingStroke>): String {
        return try {
            adapter.toJson(strokes)
        } catch (e: Exception) {
            "[]"
        }
    }

    fun deserialize(json: String?): List<InkingStroke> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
