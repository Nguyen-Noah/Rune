package rune.platforms.opengl

import org.lwjgl.opengl.GL45.*
import rune.renderer.gpu.VertexBuffer
import rune.renderer.renderer3d.mesh.Vertex
import java.nio.ByteBuffer

class OpenGLVertexBuffer : VertexBuffer {
    private var rendererID: Int = 0
    private var vertices: FloatArray? = null
    private var size: Int

    constructor(size: Int) {
        this.size = size

        rendererID = glCreateBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, rendererID)
        glBufferData(GL_ARRAY_BUFFER, size.toLong(), GL_DYNAMIC_DRAW)
    }

    constructor(vertices: FloatArray, size: Int) {
        this.vertices = vertices
        this.size = size

        rendererID = glCreateBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, rendererID)
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW)
    }

    constructor(vertices: List<Vertex>) {
        val floatsPerVertex = 8 // position, normal, texcoords
        val data = FloatArray(vertices.size * floatsPerVertex)

        var i = 0
        vertices.forEach { v ->
            data[i++] = v.position.x;  data[i++] = v.position.y;  data[i++] = v.position.z
            data[i++] = v.normal.x;  data[i++] = v.normal.y;  data[i++] = v.normal.z
            data[i++] = v.texCoords.x;   data[i++] = v.texCoords.y
        }

        size = data.size * Float.SIZE_BYTES

        rendererID = glCreateBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, rendererID)
        glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW)
    }

    override fun setData(vertices: ByteBuffer) {
        glBindBuffer(GL_ARRAY_BUFFER, rendererID)
        glBufferSubData(GL_ARRAY_BUFFER, 0L, vertices)
    }

    override fun getSize(): Int {
        return size
    }

    override fun bind() {
        glBindBuffer(GL_ARRAY_BUFFER, rendererID)
    }

    override fun unbind() {
        glBindBuffer(GL_ARRAY_BUFFER, 0)
    }
}