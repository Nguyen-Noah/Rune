package rune.renderer

import glm_.vec2.Vec2
import glm_.vec3.Vec3
import glm_.vec4.Vec4

data class QuadVertex(
    val position: Vec3 = Vec3(0.0f, 0.0f, 0.0f),
    val color: Vec4 = Vec4(1.0f),
    val texCoords: Vec2 = Vec2(0.0f, 0.0f),
    val texIndex: Float = 0f,       // default to white texture
    val tilingFactor: Float = 1f
) {
    companion object {
        const val FLOAT_COUNT = 3 + 4 + 2 + 1 + 1
        const val BYTE_SIZE = FLOAT_COUNT * 4
    }
}

class QuadVertexBufferWriter(maxQuads: Int) {
    private val maxVertices = maxQuads
    // Preallocate the underlying float array large enough to hold all vertices.
    private val buffer = FloatArray(maxVertices * QuadVertex.FLOAT_COUNT)
    private var vertexCount = 0

    fun reset() {
        vertexCount = 0
    }

    fun write(
        position: Vec3,
        color: Vec4,
        texCoords: Vec2,
        texIndex: Float,
        tilingFactor: Float,
    ) {
        if (vertexCount >= maxVertices) {
            throw IndexOutOfBoundsException("Exceeded maximum vertex count")
        }
        val offset = vertexCount * QuadVertex.FLOAT_COUNT

        buffer[offset + 0] = position.x
        buffer[offset + 1] = position.y
        buffer[offset + 2] = position.z

        buffer[offset + 3] = color.r
        buffer[offset + 4] = color.g
        buffer[offset + 5] = color.b
        buffer[offset + 6] = color.a

        buffer[offset + 7] = texCoords.x
        buffer[offset + 8] = texCoords.y

        buffer[offset + 9] = texIndex

        buffer[offset + 10] = tilingFactor

        vertexCount++
    }

    // Returns the underlying FloatArray without rebuilding it.
    fun getBuffer(): FloatArray = buffer

    // Total data size in bytes for the vertices written so far.
    fun getSizeInBytes(): Int = vertexCount * QuadVertex.BYTE_SIZE
}