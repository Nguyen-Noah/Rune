package rune.platforms.opengl

import org.lwjgl.opengl.GL45.*
import org.lwjgl.system.MemoryUtil
import rune.renderer.QuadVertex
import rune.renderer.VertexBuffer

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

    override fun setData(vertices: FloatArray, size: Int) {
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