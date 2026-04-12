package rune.terrain

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import rune.renderer.renderer3d.Model
import rune.terrain.nodes.TerrainNode
import rune.terrain.types.HeightField

/**
 * Builds terrain meshes from height data, [HeightField], or a [TerrainNode] graph.
 */
object TerrainSystem {

    fun createModel(config: TerrainConfig): Model =
        HeightmapMeshBuilder.buildModel(config)

    fun createModel(field: HeightField, meshName: String = "Terrain"): Model =
        HeightmapMeshBuilder.buildModel(TerrainConfig.fromHeightField(field, meshName))

    /**
     * Evaluates the graph from [root] (must be a [rune.terrain.nodes.SourceNode] or chain ending in one)
     * and builds a mesh from the resulting heights (masks do not affect the mesh yet).
     */
    fun createModel(root: TerrainNode, meshName: String = "Terrain"): Model =
        createModel(root.evaluate(), meshName)

    fun evaluate(root: TerrainNode): HeightField = root.evaluate()

    /**
     * Fills a height array using seeded Perlin FBM in world space (see [ProceduralTerrainParams]).
     */
    fun perlinHeights(
        gridX: Int,
        gridZ: Int,
        sizeX: Float,
        sizeZ: Float,
        params: ProceduralTerrainParams = ProceduralTerrainParams(),
    ): FloatArray {
        val width = gridX + 1
        val depth = gridZ + 1
        val heights = FloatArray(width * depth)
        val noise = PerlinNoise2D(params.seed)
        for (row in 0 until depth) {
            for (column in 0 until width) {
                val wx = (column.toFloat() / gridX - 0.5f) * sizeX + params.offsetX
                val wz = (row.toFloat() / gridZ - 0.5f) * sizeZ + params.offsetZ
                val n = noise.fbm(
                    wx * params.frequency,
                    wz * params.frequency,
                    params.octaves,
                    params.persistence,
                    params.lacunarity,
                )
                heights[row * width + column] = n * params.heightScale
            }
        }
        return heights
    }

    /** Builds a [TerrainConfig] with Perlin FBM heights. */
    fun createProceduralConfig(
        gridX: Int,
        gridZ: Int,
        sizeX: Float,
        sizeZ: Float,
        params: ProceduralTerrainParams = ProceduralTerrainParams(),
        meshName: String = "ProceduralTerrain",
    ): TerrainConfig {
        val heights = perlinHeights(gridX, gridZ, sizeX, sizeZ, params)
        return TerrainConfig(
            gridX = gridX,
            gridZ = gridZ,
            sizeX = sizeX,
            sizeZ = sizeZ,
            heights = heights,
            meshName = meshName,
        )
    }

    /** Smooth hills + gentle crater for visual testing (non-Perlin). */
    fun demoHeights(gridX: Int, gridZ: Int): FloatArray {
        val w = gridX + 1
        val d = gridZ + 1
        val h = FloatArray(w * d)
        for (iz in 0 until d) {
            for (ix in 0 until w) {
                val u = ix.toFloat() / gridX * 2f - 1f
                val v = iz.toFloat() / gridZ * 2f - 1f
                val r = sqrt(u * u + v * v)
                val wave = sin(u * 4.5f) * cos(v * 4.5f) * 0.65f
                val mound = (1f - r.coerceIn(0f, 1f)) * 1.4f
                val dip = if (r < 0.35f) -(0.35f - r) * 2f else 0f
                h[iz * w + ix] = wave + mound + dip
            }
        }
        return h
    }
}
