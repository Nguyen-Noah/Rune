package rune.terrain

/**
 * Parameters for sampling 2D Perlin FBM into a heightmap grid.
 *
 * Heights use world-aligned coordinates: vertex sits at
 * `((ix/gridX - 0.5) * sizeX, (iz/gridZ - 0.5) * sizeZ)` in the horizontal plane; noise is evaluated at (wx, wz).
 */
data class ProceduralTerrainParams(
    val seed: Long = 42L,
    /** Noise frequency in world space (higher = more hills per unit distance). */
    val frequency: Float = 0.04f,
    val octaves: Int = 6,
    val persistence: Float = 0.5f,
    val lacunarity: Float = 2f,
    /** Multiplies normalized noise to world height. */
    val heightScale: Float = 12f,
    /** Shifts the noise field in world X. */
    val offsetX: Float = 0f,
    /** Shifts the noise field in world Z. */
    val offsetZ: Float = 0f,
)
