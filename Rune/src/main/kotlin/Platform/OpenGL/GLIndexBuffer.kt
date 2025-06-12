package rune.platforms.opengl

import org.lwjgl.opengl.GL33.*
import rune.renderer.SubmitRender
import rune.renderer.gpu.IndexBuffer
import rune.rhi.IndexType

class GLIndexBuffer : IndexBuffer, GLIndexable {
    override var rendererID: Int = 0
    override val indexType: IndexType
    override val target: Int = GL_ELEMENT_ARRAY_BUFFER
    override val size: Int // bytes

    private var count: Int = 0

    constructor(indices: IntArray, count: Int) {
        this.count = count
        rendererID = glGenBuffers()
        size = count * Int.SIZE_BYTES
        indexType = IndexType.UINT32

        glBindBuffer(target, rendererID)
        glBufferData(target, indices, GL_STATIC_DRAW)
    }

    constructor(indexBuffer: List<Int>) : this(indexBuffer.toIntArray(), indexBuffer.size)

    override fun bind() = glBindBuffer(target, rendererID)
    override fun unbind() = glBindBuffer(target, 0)
}