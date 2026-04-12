package sandbox.panels.graph

import rune.terrain.nodes.TerrainNode
import rune.terrain.nodes.builtin.IdentityModifier
import rune.terrain.nodes.builtin.NoiseSource

/** Builds a linear [TerrainNode] pipeline from the ImGui graph: source → … → sink evaluated by [TerrainSystem]. */
object TerrainGraphCompiler {

    fun compile(graph: Graph, spec: TerrainBakeSpec): TerrainCompileOutcome {
        if (graph.nodes.isEmpty()) return TerrainCompileOutcome.Err("Graph is empty")
        val chain = orderedChain(graph)
            ?: return TerrainCompileOutcome.Err("Could not find a single source node (check for cycles)")
        if (chain.size != graph.nodes.size) {
            return TerrainCompileOutcome.Err("Terrain graph must be one connected chain (no branches or islands)")
        }
        if (chain.first().kind != TerrainGraphNodeKind.NOISE) {
            return TerrainCompileOutcome.Err("First node must be Noise (heightfield source)")
        }

        val terrain = ArrayList<TerrainNode>(chain.size)
        chain.forEachIndexed { index, gn ->
            when (gn.kind) {
                TerrainGraphNodeKind.NOISE -> {
                    if (index != 0) {
                        return TerrainCompileOutcome.Err("Noise node can only be at the start of the chain")
                    }
                    terrain.add(
                        NoiseSource(
                            width = spec.fieldWidth,
                            depth = spec.fieldDepth,
                            sizeX = spec.sizeX,
                            sizeZ = spec.sizeZ,
                            seed = gn.noiseSeed,
                            frequency = gn.noiseFrequency,
                            octaves = gn.noiseOctaves,
                            persistence = gn.noisePersistence,
                            lacunarity = gn.noiseLacunarity,
                            heightScale = gn.noiseHeightScale,
                            noiseType = when (gn.noiseTypeOrdinal) {
                                0 -> NoiseSource.NoiseType.FBM
                                1 -> NoiseSource.NoiseType.BILLOWED
                                else -> NoiseSource.NoiseType.FBM
                            },
                        ),
                    )
                }
                TerrainGraphNodeKind.IDENTITY -> {
                    if (index == 0) {
                        return TerrainCompileOutcome.Err("Identity cannot be the first node")
                    }
                    terrain.add(IdentityModifier())
                }
            }
        }

        for (i in 1 until terrain.size) {
            terrain[i].input = terrain[i - 1]
        }
        return TerrainCompileOutcome.Ok(terrain.last())
    }

    private fun orderedChain(graph: Graph): List<GraphNode>? {
        val sources = graph.nodes.values.filter { n ->
            graph.nodes.values.none { it.outputNodeId == n.nodeId }
        }
        if (sources.size != 1) return null

        val out = ArrayList<GraphNode>()
        var cur: GraphNode? = sources.first()
        val seen = HashSet<Long>()
        while (cur != null) {
            if (cur.nodeId in seen) return null
            seen.add(cur.nodeId)
            out.add(cur)
            val nextId = cur.outputNodeId ?: break
            cur = graph.nodes[nextId]
        }
        return out
    }
}
