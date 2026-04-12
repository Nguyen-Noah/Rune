package rune.terrain.nodes

import rune.terrain.types.HeightField

/**
 * A node in the terrain graph. Each node reads a HeightField,
 * modifies it (heights and/or masks), and passes it along.
 *
 * Nodes are evaluated lazily - pulling from their input only when
 * their own output is requested
 */
abstract class TerrainNode {
    /**
     * The upstream node we pull data from. Null = graph root
     */
    var input: TerrainNode? = null

    /**
     * Params exposed for editing (name -> current value).
     */
    abstract val params: Map<String, Any>

    /**
     * Evaluate this node: pull from input, apply our operation,
     * return the modified HeightField
     */
    open fun evaluate(): HeightField {
        val field = input?.evaluate() ?: error("Node has no input and isn't a source")
        process(field)
        return field
    }

    /**
     * Modify the HeightField in place
     */
    protected abstract fun process(field: HeightField)
}
