package sandbox.panels.graph

import rune.terrain.nodes.TerrainNode

/** Result of turning the editor [Graph] into a concrete [TerrainNode] chain (sink = [Ok.root]). */
sealed class TerrainCompileOutcome {
    data class Ok(val root: TerrainNode) : TerrainCompileOutcome()
    data class Err(val message: String) : TerrainCompileOutcome()
}
