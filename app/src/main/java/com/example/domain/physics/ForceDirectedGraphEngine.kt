package com.example.domain.physics

import androidx.compose.ui.geometry.Offset
import com.example.data.local.NoteEntity
import com.example.data.local.NoteLinkEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class GraphNode(
    val id: String,
    val title: String,
    val colorHex: String,
    val tag: String,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    val degree: Int = 1,
    var isPinned: Boolean = false
) {
    val radius: Float get() = min(42f, 18f + (degree * 4f))
}

data class GraphEdge(
    val id: String,
    val sourceId: String,
    val targetId: String,
    val colorHex: String
)

data class GraphState(
    val nodes: Map<String, GraphNode> = emptyMap(),
    val edges: List<GraphEdge> = emptyList(),
    val energy: Float = 0f
)

class ForceDirectedGraphEngine(
    private val scope: CoroutineScope
) {
    private val _graphState = MutableStateFlow(GraphState())
    val graphState: StateFlow<GraphState> = _graphState.asStateFlow()

    private var simulationJob: Job? = null

    // Physics parameters
    private val kRepulsion = 120000f // Coulomb repulsion constant
    private val kSpring = 0.045f    // Hooke spring constant
    private val naturalLength = 180f // Rest length of springs
    private val damping = 0.82f      // Velocity damping factor
    private val maxVelocity = 25f

    fun updateGraph(notes: List<NoteEntity>, links: List<NoteLinkEntity>, boundsWidth: Float = 800f, boundsHeight: Float = 800f) {
        val currentNodes = _graphState.value.nodes.toMutableMap()
        val degreeMap = mutableMapOf<String, Int>()

        links.forEach { link ->
            degreeMap[link.sourceId] = (degreeMap[link.sourceId] ?: 0) + 1
            degreeMap[link.targetId] = (degreeMap[link.targetId] ?: 0) + 1
        }

        val updatedNodes = mutableMapOf<String, GraphNode>()
        val angleStep = (2 * Math.PI / max(1, notes.size)).toFloat()

        notes.forEachIndexed { index, note ->
            val existing = currentNodes[note.id]
            val degree = max(1, degreeMap[note.id] ?: 1)
            if (existing != null) {
                updatedNodes[note.id] = existing.copy(
                    title = note.title,
                    colorHex = note.colorHex,
                    tag = note.tag,
                    degree = degree
                )
            } else {
                val radius = 200f + (index % 3) * 60f
                val angle = index * angleStep
                val initX = boundsWidth / 2f + (radius * kotlin.math.cos(angle))
                val initY = boundsHeight / 2f + (radius * kotlin.math.sin(angle))
                updatedNodes[note.id] = GraphNode(
                    id = note.id,
                    title = note.title,
                    colorHex = note.colorHex,
                    tag = note.tag,
                    x = initX,
                    y = initY,
                    degree = degree
                )
            }
        }

        val edges = links.map {
            GraphEdge(
                id = it.id,
                sourceId = it.sourceId,
                targetId = it.targetId,
                colorHex = it.colorHex
            )
        }

        _graphState.value = GraphState(nodes = updatedNodes, edges = edges)
        startSimulation()
    }

    fun onNodeDrag(nodeId: String, newPos: Offset) {
        val nodes = _graphState.value.nodes.toMutableMap()
        nodes[nodeId]?.let { node ->
            node.x = newPos.x
            node.y = newPos.y
            node.vx = 0f
            node.vy = 0f
            node.isPinned = true
            _graphState.value = _graphState.value.copy(nodes = nodes)
        }
    }

    fun onNodeRelease(nodeId: String) {
        val nodes = _graphState.value.nodes.toMutableMap()
        nodes[nodeId]?.let { node ->
            node.isPinned = false
        }
    }

    private fun startSimulation() {
        if (simulationJob?.isActive == true) return

        simulationJob = scope.launch(Dispatchers.Default) {
            var stepCount = 0
            while (isActive && stepCount < 300) {
                val currentState = _graphState.value
                val nodes = currentState.nodes.values.toList()
                val edges = currentState.edges
                if (nodes.size < 2) {
                    delay(50)
                    continue
                }

                // 1. Calculate Coulomb Repulsion between all node pairs
                val fx = FloatArray(nodes.size)
                val fy = FloatArray(nodes.size)

                for (i in nodes.indices) {
                    for (j in i + 1 until nodes.size) {
                        val n1 = nodes[i]
                        val n2 = nodes[j]
                        val dx = n2.x - n1.x
                        val dy = n2.y - n1.y
                        val distSq = max(100f, dx * dx + dy * dy)
                        val dist = sqrt(distSq)

                        val force = kRepulsion / distSq
                        val fX = (dx / dist) * force
                        val fY = (dy / dist) * force

                        fx[i] -= fX
                        fy[i] -= fY
                        fx[j] += fX
                        fy[j] += fY
                    }
                }

                // 2. Calculate Hooke's Spring Attraction along edges
                val nodeIndexMap = nodes.mapIndexed { index, node -> node.id to index }.toMap()

                for (edge in edges) {
                    val i1 = nodeIndexMap[edge.sourceId] ?: continue
                    val i2 = nodeIndexMap[edge.targetId] ?: continue

                    val n1 = nodes[i1]
                    val n2 = nodes[i2]
                    val dx = n2.x - n1.x
                    val dy = n2.y - n1.y
                    val dist = max(1f, sqrt(dx * dx + dy * dy))

                    val displacement = dist - naturalLength
                    val springForce = kSpring * displacement
                    val fX = (dx / dist) * springForce
                    val fY = (dy / dist) * springForce

                    fx[i1] += fX
                    fy[i1] += fY
                    fx[i2] -= fX
                    fy[i2] -= fY
                }

                // 3. Central gravity pull towards (400, 400)
                val centerX = 400f
                val centerY = 400f
                for (i in nodes.indices) {
                    val node = nodes[i]
                    val dx = centerX - node.x
                    val dy = centerY - node.y
                    fx[i] += dx * 0.003f
                    fy[i] += dy * 0.003f
                }

                // 4. Integrate velocities & update positions
                var totalEnergy = 0f
                val newNodesMap = mutableMapOf<String, GraphNode>()

                for (i in nodes.indices) {
                    val node = nodes[i]
                    if (!node.isPinned) {
                        node.vx = (node.vx + fx[i]) * damping
                        node.vy = (node.vy + fy[i]) * damping

                        // Clamp max velocity
                        node.vx = node.vx.coerceIn(-maxVelocity, maxVelocity)
                        node.vy = node.vy.coerceIn(-maxVelocity, maxVelocity)

                        node.x += node.vx
                        node.y += node.vy
                    }
                    totalEnergy += (node.vx * node.vx + node.vy * node.vy)
                    newNodesMap[node.id] = node
                }

                _graphState.value = currentState.copy(nodes = newNodesMap, energy = totalEnergy)
                stepCount++
                delay(16) // ~60 FPS simulation step
            }
        }
    }
}
