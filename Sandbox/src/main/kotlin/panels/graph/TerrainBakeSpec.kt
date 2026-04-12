package sandbox.panels.graph

/** World size and vertex resolution passed into compiled [rune.terrain.nodes.builtin.NoiseSource] instances. */
data class TerrainBakeSpec(
    val fieldWidth: Int,
    val fieldDepth: Int,
    val sizeX: Float,
    val sizeZ: Float,
    val meshName: String,
)
