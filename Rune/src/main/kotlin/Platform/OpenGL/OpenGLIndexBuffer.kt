package rune.platforms.opengl

import org.lwjgl.opengl.GL33.*
import rune.renderer.gpu.IndexBuffer

class OpenGLIndexBuffer : IndexBuffer {
    private var rendererID: Int = 0
    private var indices: IntArray? = null
    private var count: Int = 0

    constructor(indices: IntArray, count: Int) {
        this.indices = indices
        this.count = count

        rendererID = glGenBuffers()
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, rendererID)
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW)
    }

    constructor(indexBuffer: List<Int>) {
        this.indices = indexBuffer.toIntArray()
        this.count = indexBuffer.size

        rendererID = glGenBuffers()
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, rendererID)
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices!!, GL_STATIC_DRAW)
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