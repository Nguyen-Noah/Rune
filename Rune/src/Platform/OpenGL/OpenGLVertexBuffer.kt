package rune.platforms.opengl

import org.lwjgl.opengl.GL45.*
import rune.renderer.VertexBuffer

class OpenGLVertexBuffer(private val vertices: FloatArray, private val size: Int) : VertexBuffer {
    private var rendererID: Int = 0

    init {
        rendererID = glCreateBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, rendererID)
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW)
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