package rune.renderer

import org.lwjgl.opengl.GL45.*

data class VertexAttribute(
    val name: String,
    val count: Int,
    val type: Int = GL_FLOAT,
    val normalized: Boolean = false
)
class VertexBufferLayout {
    private val attributes = mutableListOf<VertexAttribute>()
    val stride: Int
        get() = attributes.sumOf { it.count * 4 }

    fun attribute(name: String, count: Int, type: Int = GL_FLOAT, normalized: Boolean = false) {
        attributes.add(VertexAttribute(name, count, type, normalized))
    }

    fun computeOffsets(): List<Triple<Int, Int, VertexAttribute>> {
        var offset = 0
        return attributes.mapIndexed { index, attr ->
            val triple = Triple(index, offset, attr)
            offset += attr.count * 4
            triple
        }
    }

    internal fun getAttributes(): List<VertexAttribute> = attributes
}

fun bufferLayout(init: VertexBufferLayout.() -> Unit): VertexBufferLayout {
    return VertexBufferLayout().apply(init)
}