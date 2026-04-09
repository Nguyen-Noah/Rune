package rune.platforms.opengl

import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import org.lwjgl.opengl.GL45.*
import org.lwjgl.system.MemoryUtil
import rune.renderer.gpu.Std140Type
import rune.renderer.gpu.UniformBuffer
import java.nio.ByteBuffer

class GLUniformBuffer(override val size: Int, private val binding: Int, private val name: String) : UniformBuffer {
    private var rendererId: Int = -1

    init {
        val n = if (name.isNotEmpty()) "[$name]" else ""

        RenderCommandQueue.enqueue("GLUbo$n-init") {
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
        val n = if (name.isNotEmpty()) "[$name]" else ""

        RenderCommandQueue.enqueue("GLUbo$n-setData") {
            MemoryUtil.memAlloc(Std140Type.Mat4.size).apply {
                data to this
                setData(this, offset)
                MemoryUtil.memFree(this)
            }
        }
    }

    override fun setData(data: Vec3, offset: Int) {
        val n = if (name.isNotEmpty()) "[$name]" else ""

        RenderCommandQueue.enqueue("GLUbo$n-setData") {
            MemoryUtil.memAlloc(Std140Type.Vec3.size).apply {
                data to this
                setData(this, offset)
                MemoryUtil.memFree(this)
            }
        }
    }

    override fun setData(data: Boolean, offset: Int) {
        val n = if (name.isNotEmpty()) "[$name]" else ""

        RenderCommandQueue.enqueue("GLUbo$n-setData") {
            MemoryUtil.memAlloc(Std140Type.Bool.size).apply {
                putInt(if (data) 1 else 0)
                flip()
                setData(this, offset)
                MemoryUtil.memFree(this)
            }
        }
    }
}
