package sandbox.panels.graph

/** In-memory node graph for the Sandbox editor; not used by Rune at runtime except via [TerrainGraphCompiler]. */
class Graph {
    var nextNodeId: Long = 1
    var nextPinId: Long = 100

    val nodes = HashMap<Long, GraphNode>()
    init {
        createGraphNode(TerrainGraphNodeKind.NOISE)
    }

    fun createGraphNode(kind: TerrainGraphNodeKind = TerrainGraphNodeKind.NOISE): GraphNode {
        val node = GraphNode(nextNodeId++, nextPinId++, nextPinId++, kind)
        nodes[node.nodeId] = node
        return node
    }

    fun findByInput(inputPinId: Long): GraphNode? =
        nodes.values.find { it.inputPinId == inputPinId }

    fun findByOutput(outputPinId: Long): GraphNode? =
        nodes.values.find { it.outputPinId == outputPinId }
}
