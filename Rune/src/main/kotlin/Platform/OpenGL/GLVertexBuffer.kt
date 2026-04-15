package rune.platforms.opengl

import org.lwjgl.opengl.GL45.*
import rune.renderer.gpu.VertexBuffer
import rune.renderer.renderer3d.mesh.Vertex
import java.nio.ByteBuffer

class GLVertexBuffer : VertexBuffer, GLBuffer {
    override var rendererID: Int = 0
    override val target: Int = GL_ARRAY_BUFFER
    override var size: Int

    constructor(size: Int) {
        this.size = size
        rendererID = glCreateBuffers()

        glBindBuffer(target, rendererID)
        glBufferData(target, size.toLong(), GL_DYNAMIC_DRAW)
    }

    constructor(vertices: FloatArray) : this(vertices, vertices.size * Float.SIZE_BYTES)

    constructor(vertices: FloatArray, size: Int) {
        this.size = size
        rendererID = glCreateBuffers()

        glBindBuffer(target, rendererID)
        glBufferData(target, vertices, GL_STATIC_DRAW)
    }

    constructor(vertices: List<Vertex>) {
        val floatsPerVertex = 16 // position, normal, bitangent, tangent, texcoords x2
        val data = FloatArray(vertices.size * floatsPerVertex)

        var i = 0
        vertices.forEach { v ->
            data[i++] = v.position.x;  data[i++] = v.position.y;  data[i++] = v.position.z
            data[i++] = v.normal.x;  data[i++] = v.normal.y;  data[i++] = v.normal.z
            data[i++] = v.bitangent.x;  data[i++] = v.bitangent.y;  data[i++] = v.bitangent.z
            data[i++] = v.tangent.x;  data[i++] = v.tangent.y;  data[i++] = v.tangent.z
            data[i++] = v.texCoords.x;   data[i++] = v.texCoords.y
            data[i++] = v.texCoords1.x;   data[i++] = v.texCoords1.y
        }

        size = data.size * Float.SIZE_BYTES

        rendererID = glCreateBuffers()
        glBindBuffer(target, rendererID)
        glBufferData(target, data, GL_STATIC_DRAW)
    }

    override fun setData(vertices: ByteBuffer) {
        glBindBuffer(target, rendererID)
        glBufferSubData(target, 0L, vertices)
    }

    override fun bind() = glBindBuffer(target, rendererID)
    override fun unbind() = glBindBuffer(target, 0)
}