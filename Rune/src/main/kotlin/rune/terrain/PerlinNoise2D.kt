package rune.terrain

import kotlin.math.floor
import kotlin.random.Random

/**
 * Seeded 2D Perlin noise with fractal Brownian motion (sum of octaves).
 * Output is approximately in [-1, 1] for a single octave; FBM is normalized by sum of amplitudes.
 */
class PerlinNoise2D(seed: Long) {

    private val perm = IntArray(512)

    init {
        val p = IntArray(256) { it }
        val rnd = Random(seed)
        for (i in 255 downTo 1) {
            val j = rnd.nextInt(i + 1)
            val tmp = p[i]
            p[i] = p[j]
            p[j] = tmp
        }
        for (i in 0 until 512) {
            perm[i] = p[i and 255]
        }
    }

    fun noise(x: Float, y: Float): Float {
        val xi = floor(x).toInt() and 255
        val yi = floor(y).toInt() and 255
        val xf = x - floor(x)
        val yf = y - floor(y)

        val u = fade(xf)
        val v = fade(yf)

        val aa = perm[perm[xi] + yi]
        val ab = perm[perm[xi] + yi + 1]
        val ba = perm[perm[xi + 1] + yi]
        val bb = perm[perm[xi + 1] + yi + 1]

        val x1 = lerp(grad(aa, xf, yf), grad(ba, xf - 1f, yf), u)
        val x2 = lerp(grad(ab, xf, yf - 1f), grad(bb, xf - 1f, yf - 1f), u)
        return lerp(x1, x2, v)
    }

    /**
     * Sum of [octaves] noise layers; [persistence] scales amplitude each octave, [lacunarity] scales frequency.
     */
    fun fbm(
        x: Float,
        y: Float,
        octaves: Int,
        persistence: Float,
        lacunarity: Float,
    ): Float {
        var total = 0f
        var amplitude = 1f
        var frequency = 1f
        var norm = 0f
        val n = octaves.coerceAtLeast(1)
        repeat(n) {
            total += noise(x * frequency, y * frequency) * amplitude
            norm += amplitude
            amplitude *= persistence
            frequency *= lacunarity
        }
        return if (norm > 0f) total / norm else 0f
    }

    private fun fade(t: Float): Float =
        t * t * t * (t * (t * 6f - 15f) + 10f)

    private fun lerp(a: Float, b: Float, t: Float): Float = a + t * (b - a)

    private fun grad(hash: Int, x: Float, y: Float): Float {
        val h = hash and 3
        return when (h) {
            0 -> x + y
            1 -> -x + y
            2 -> x - y
            else -> -x - y
        }
    }
}
