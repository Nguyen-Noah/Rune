package sandbox.panels.graph

/** Editor node kinds; each maps to a [rune.terrain.nodes.TerrainNode] in [TerrainGraphCompiler]. */
enum class TerrainGraphNodeKind(val label: String) {
    NOISE("Noise"),
    IDENTITY("Identity"),
}
