package rune.platforms.opengl

import glm_.mat4x4.Mat4
import org.lwjgl.opengl.GL45.*
import org.lwjgl.system.MemoryUtil
import rune.renderer.SubmitRender
import rune.renderer.gpu.UniformBuffer
import rune.renderer.renderer2d.FLOAT_MAT4_SIZE
import java.nio.ByteBuffer

class GLUniformBuffer(private val size: Int, private val binding: Int, private val name: String) : UniformBuffer {
    private var rendererId: Int = -1

    init {
        val n = if (name.isNotEmpty()) {
            "[$name]"
        } else {
            ""
        }
        SubmitRender("GLUbo$n-init") {
            rendererId = glCreateBuffers().also { id ->
                glNamedBufferData(id, size.toLong(), GL_DYNAMIC_DRAW)
                glBindBufferBase(GL_UNIFORM_BUFFER, binding, id)
            }
        }
    }

    override fun setData(data: ByteBuffer, offset: Int) {
        glNamedBufferSubData(rendererId, offset.toLong(), data)
    }

    override fun setData(data: Mat4, offset: Int) {
        val n = if (name.isNotEmpty()) {
            "[$name]"
        } else {
            ""
        }
        SubmitRender("GLUbo$n-setData") {
            MemoryUtil.memAlloc(FLOAT_MAT4_SIZE).apply {
                data to this
                setData(this)
                MemoryUtil.memFree(this)
            }
        }
    }
}
