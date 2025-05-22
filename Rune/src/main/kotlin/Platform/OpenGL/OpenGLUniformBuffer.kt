package rune.platforms.opengl

import org.lwjgl.opengl.GL45.*
import rune.renderer.gpu.UniformBuffer
import java.nio.ByteBuffer

class OpenGLUniformBuffer(private val size: Int, private val binding: Int) : UniformBuffer {
    private var rendererId = glCreateBuffers().also { id ->
        glNamedBufferData(id, size.toLong(), GL_DYNAMIC_DRAW)
        glBindBufferBase(GL_UNIFORM_BUFFER, binding, id)
    }

    override fun setData(data: ByteBuffer, offset: Int) {
        glNamedBufferSubData(rendererId, offset.toLong(), data)
    }
}
