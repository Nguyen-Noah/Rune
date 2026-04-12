package sandbox.panels.graph

/** One visual node: pin IDs for the node editor, link via [outputNodeId], terrain data for compilation. */
data class GraphNode(
    val nodeId: Long,
    val inputPinId: Long,
    val outputPinId: Long,
    var kind: TerrainGraphNodeKind = TerrainGraphNodeKind.NOISE,
) {
    var outputNodeId: Long? = null

    var noiseSeed: Long = 42L
    var noiseFrequency: Float = 0.02f
    var noiseOctaves: Int = 6
    var noisePersistence: Float = 0.5f
    var noiseLacunarity: Float = 2f
    var noiseHeightScale: Float = 4f
    /** 0 = FBM, 1 = BILLOWED */
    var noiseTypeOrdinal: Int = 0
}