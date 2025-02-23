package rune.platforms.opengl

import org.lwjgl.opengl.GL45.*
import rune.renderer.IndexBuffer
import rune.renderer.VertexArray
import rune.renderer.VertexBuffer
import rune.renderer.VertexBufferLayout

class OpenGLVertexArray(
    vbo: VertexBuffer,
    private val layout: VertexBufferLayout
) : VertexArray {
    private val vao: Int = glGenVertexArrays()
    private var nextAttribIndex = 0
    private val vertexBuffers = mutableListOf<VertexBuffer>()
    private lateinit var indexBuffer: IndexBuffer

    init {
        glBindVertexArray(vao)
        addVertexBuffer(vbo, layout)
        glBindVertexArray(0)
    }

    override fun addVertexBuffer(vbo: VertexBuffer) {
        addVertexBuffer(vbo, layout)
    }

    fun addVertexBuffer(vbo: VertexBuffer, layout: VertexBufferLayout) {
        glBindVertexArray(vao)
        vbo.bind()

        layout.computeOffsets().forEach { (_, offset, attr) ->
            glEnableVertexAttribArray(nextAttribIndex)
            glVertexAttribPointer(
                nextAttribIndex,
                attr.count,
                attr.type,
                attr.normalized,
                layout.stride,
                offset.toLong()
            )
            nextAttribIndex++
        }

        vertexBuffers.add(vbo)

        vbo.unbind()
        glBindVertexArray(0)
    }

    override fun setIndexBuffer(ibo: IndexBuffer) {
        glBindVertexArray(vao)
        ibo.bind()
        indexBuffer = ibo
    }

    override fun getIndexBuffer(): IndexBuffer {
        return indexBuffer
    }

    override fun bind() {
        glBindVertexArray(vao)
    }

    override fun unbind() {
        glBindVertexArray(0)
    }
}