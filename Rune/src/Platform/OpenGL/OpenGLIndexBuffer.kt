package rune.platform.opengl

import org.lwjgl.opengl.GL33.*
import org.lwjgl.system.MemoryUtil
import rune.renderer.IndexBuffer

class OpenGLIndexBuffer(private val indices: IntArray, private val count: Int) : IndexBuffer {
    private var rendererID: Int = 0

    init {
        rendererID = glGenBuffers()
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, rendererID)
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW)
    }

    override fun getCount(): Int {
        return count
    }

    override fun bind() {
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, rendererID)
    }

    override fun unbind() {
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
    }
}