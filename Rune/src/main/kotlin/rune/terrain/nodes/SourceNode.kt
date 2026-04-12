package rune.terrain.nodes

import rune.terrain.types.HeightField

/**
 * Source nodes create a HeightField from nothing.
 * They sit at the root of the graph.
 */
abstract class SourceNode(
    val width: Int,
    val depth: Int,
    val sizeX: Float,
    val sizeZ: Float
) : TerrainNode() {

    override fun evaluate(): HeightField {
        val field = HeightField(width, depth, sizeX, sizeZ)
        process(field)
        return field
    }
}
