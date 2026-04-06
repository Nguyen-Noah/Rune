package rune.platforms.opengl

import kool.BYTES
import org.lwjgl.opengl.GL45.*
import rune.renderer.StorageBuffer
import java.nio.ByteBuffer
import java.nio.IntBuffer

class GLStorageBuffer(
    private val size: Int,
): StorageBuffer {
    override var rendererId: Int = -1

    init {
        RenderCommandQueue.enqueue("SSBO-init") {
            rendererId = glCreateBuffers()
            glNamedBufferData(rendererId, size * Int.BYTES.toLong(), GL_DYNAMIC_COPY)
        }
    }

    override fun bind(binding: Int) {
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, binding, rendererId)
    }

    override fun clear() {
        glClearNamedBufferData(rendererId, GL_R32UI, GL_RED_INTEGER, GL_UNSIGNED_INT, null as IntBuffer?)
    }

    override fun setData(data: ByteBuffer, offset: Int, name: String) {
        val tag = if (name.isNotEmpty()) "[$name]" else ""
        RenderCommandQueue.enqueue("GLSSBO$tag-setData") { glNamedBufferSubData(rendererId, offset.toLong(), data) }
    }
}