package rune.terrain.types

import kotlin.math.sqrt

/**
 *
 */
class HeightField(
    val width: Int,
    val depth: Int,
    val sizeX: Float,
    val sizeZ: Float
) {
    /**
     *  Stores the heights in an array given a width and depth
     */
    val heights = FloatArray(width * depth)

    /**
     *  Stores the different masks in a given height field
     */
    private val masks = mutableMapOf<String, FloatArray>()

    /**
     *  Retrieves a mask by name, if it doesn't exist then creates the mask
     */
    fun getOrCreateMask(name: String): FloatArray =
        masks.getOrPut(name) { FloatArray(width * depth) }

    /**
     *
     */
    fun getMask(name: String): FloatArray? = masks[name]

    /**
     *  Returns the name of all masks
     */
    fun maskNames(): Set<String> = masks.keys

    /**
     * Sample height with bilinear interpolation at world-space coords
     */
    fun sampleWorld(worldX: Float, worldZ: Float): Float {
        val u = (worldX / sizeX + 0.5f) * (width - 1)
        val v = (worldZ / sizeZ + 0.5f) * (depth - 1)
        return bilinearSample(heights, u, v)
    }

    /**
     * Compute finite-difference gradient at grid coords
     */
    fun gradient(ix: Int, iz: Int): Pair<Float, Float> {
        val xl = (ix - 1).coerceAtLeast(0)
        val xr = (ix + 1).coerceAtMost(width - 1)
        val zd = (iz - 1).coerceAtLeast(0)
        val zu = (iz + 1).coerceAtMost(depth - 1)
        val dx = (heights[iz * width + xr] - heights[iz * width + xl]) /
                ((xr - xl) * (sizeX / (width - 1)))
        val dz = (heights[zu * width + ix] - heights[zd * width + ix]) /
                ((zu - zd) * (sizeZ / (depth - 1)))
        return dx to dz
    }

    fun slope(ix: Int, iz: Int): Float {
        val (dx, dz) = gradient(ix, iz)
        return sqrt(dx * dx + dz * dz)
    }

    private fun bilinearSample(h: FloatArray, u: Float, v: Float): Float {
        val x0 = u.toInt().coerceIn(0, width - 2)
        val z0 = v.toInt().coerceIn(0, depth - 2)
        val fx = (u - x0).coerceIn(0f, 1f)
        val fz = (v - z0).coerceIn(0f, 1f)
        val a = h[z0 * width + x0]
        val b = h[z0 * width + x0 + 1]
        val c = h[(z0 + 1) * width + x0]
        val d = h[(z0 + 1) * width + x0 + 1]
        return a * (1 - fx) * (1 - fz) + b * fx * (1 - fz) +
                c * (1 - fx) * fz + d * fx * fz
    }
}