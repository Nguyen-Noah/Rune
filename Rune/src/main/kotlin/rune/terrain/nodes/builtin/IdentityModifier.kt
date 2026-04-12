package rune.terrain.nodes.builtin

import rune.terrain.nodes.TerrainNode
import rune.terrain.types.HeightField

/** Non-source node that leaves [HeightField.heights] unchanged; used to extend evaluation chains. */
class IdentityModifier : TerrainNode() {
    override val params: Map<String, Any> get() = emptyMap()

    override fun process(field: HeightField) {}
}
