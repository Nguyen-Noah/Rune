package rune.platforms.opengl

import glm_.mat4x4.Mat4
import org.lwjgl.opengl.GL45.*
import org.lwjgl.system.MemoryUtil
import rune.renderer.gpu.UniformBuffer
import rune.renderer.renderer2d.FLOAT_MAT4_SIZE
import java.nio.ByteBuffer

class OpenGLUniformBuffer(private val size: Int, private val binding: Int) : UniformBuffer {
    private var rendererId = glCreateBuffers().also { id ->
        glNamedBufferData(id, size.toLong(), GL_DYNAMIC_DRAW)
        glBindBufferBase(GL_UNIFORM_BUFFER, binding, id)
    }

    override fun setData(data: ByteBuffer, offset: Int) {
        glNamedBufferSubData(rendererId, offset.toLong(), data)
    }

    override fun setData(data: Mat4, offset: Int) {
        MemoryUtil.memAlloc(FLOAT_MAT4_SIZE).apply {
            data to this
            setData(this)
            MemoryUtil.memFree(this)
        }
    }
}