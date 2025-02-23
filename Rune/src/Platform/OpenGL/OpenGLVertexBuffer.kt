package rune.platforms.opengl

import org.lwjgl.opengl.GL45.*
import rune.renderer.VertexBuffer

class OpenGLVertexBuffer(private val vertices: FloatArray, private val size: Int) : VertexBuffer {
    private var rendererID: Int = 0

    init {
        // create the buffer object
        rendererID = glCreateBuffers()

        // bind the buffer to GL_ARRAY_BUFFER
        glBindBuffer(GL_ARRAY_BUFFER, rendererID)

        // upload the data
        // in modern lwjgl, we usually call the overload that takes a buffer
        // rather than passing 'size' and a raw pointer
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