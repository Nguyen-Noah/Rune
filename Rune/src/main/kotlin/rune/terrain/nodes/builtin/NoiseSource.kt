package rune.terrain.nodes.builtin

import rune.terrain.PerlinNoise2D
import rune.terrain.nodes.SourceNode
import rune.terrain.types.HeightField
import kotlin.math.abs

class NoiseSource(
    width: Int,
    depth: Int,
    sizeX: Float,
    sizeZ: Float,
    var seed: Long = 42L,
    var frequency: Float = 0.05f,
    var octaves: Int = 6,
    var persistence: Float = 0.5f,
    var lacunarity: Float = 2f,
    var heightScale: Float = 12f,
    var noiseType: NoiseType = NoiseType.FBM
) : SourceNode(width, depth, sizeX, sizeZ) {
    enum class NoiseType { FBM, RIDGED, BILLOWED }

    override val params: Map<String, Any>
        get() = mapOf(
            "seed" to seed,
            "frequency" to frequency,
            "octaves" to octaves,
            "noiseType" to noiseType
        )

    override fun process(field: HeightField) {
        val noise = PerlinNoise2D(seed)
        for (iz in 0 until field.depth) {
            for (ix in 0 until field.width) {
                val wx = (ix.toFloat() / (field.width - 1) - 0.5f) * field.sizeX
                val wz = (iz.toFloat() / (field.depth - 1) - 0.5f) * field.sizeZ
                val raw = when (noiseType) {
                    NoiseType.FBM -> noise.fbm(wx * frequency, wz * frequency, octaves, persistence, lacunarity)
                    NoiseType.RIDGED -> TODO()
                    NoiseType.BILLOWED -> abs(noise.fbm(wx * frequency, wz * frequency, octaves, persistence, lacunarity))
                }
                field.heights[iz * field.width + ix] = raw * heightScale
            }
        }
    }
}