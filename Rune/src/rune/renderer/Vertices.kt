package rune.renderer

import org.lwjgl.opengl.GL45.GL_INT
import org.lwjgl.opengl.GL45.GL_FLOAT
import org.lwjgl.opengl.GL45.GL_UNSIGNED_BYTE
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

enum class BufferType(val comps: Int, val bytes: Int, val glType: Int) {
    Float1(1, 4, GL_FLOAT),   Float2(2, 4, GL_FLOAT),   Float3(3, 4, GL_FLOAT),   Float4(4, 4, GL_FLOAT),
    Int1  (1, 4, GL_INT),     Int2  (2, 4, GL_INT),     Int3  (3, 4, GL_INT),     Int4  (4, 4, GL_INT),
    Bool1 (1, 1, GL_UNSIGNED_BYTE);
    val sizeBytes get() = comps * bytes
}

class VertexLayout private constructor(val attributes: List<Attr>) {
    class Attr(val loc: Int, val component: BufferType, var offset: Int = 0)
    val stride = attributes.sumOf { it.component.sizeBytes }

    companion object {
        fun build(block: Builder.() -> Unit): VertexLayout = Builder().apply(block).build()
    }
    class Builder {
        private val attrs = mutableListOf<Attr>()
        fun attr(loc: Int, c: BufferType) { attrs += Attr(loc, c) }
        fun build(): VertexLayout {
            var off = 0
            attrs.forEach { it.offset = off; off += it.component.sizeBytes }
            return VertexLayout(attrs)
        }
    }
}

class VertexBufferWriter(val maxVertices: Int, val layout: VertexLayout) {
    private val capacity = maxVertices * layout.stride
    private val buf: ByteBuffer = MemoryUtil.memAlloc(capacity)
    private var vertices = 0

    fun reset() { vertices = 0; buf.clear() }

    fun putFloat(f: Float) = buf.putFloat(f)
    fun putInt(i: Int) = buf.putInt(i)
    //fun putBool(b: Boolean) = buf.put(b.toInt().toByte())

    fun write(block: VertexBufferWriter.() -> Unit) {
        require(vertices++ < maxVertices) { "vertex overflow" }
        block()
    }

    /** Slice ready for GL upload */
    fun slice(): ByteBuffer  {
        val bytesUsed = sizeBytes()
        return (buf.duplicate() as ByteBuffer).apply {
            position(0)
            limit(bytesUsed)
        }
    }
    fun sizeBytes(): Int = vertices * layout.stride
    fun free() = MemoryUtil.memFree(buf)
}
