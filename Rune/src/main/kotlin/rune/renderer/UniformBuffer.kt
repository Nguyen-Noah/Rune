package rune.renderer

import rune.platforms.opengl.OpenGLUniformBuffer
import java.nio.ByteBuffer

interface UniformBuffer {
    fun setData(data: ByteBuffer, offset: Int = 0)

    companion object {
        fun create(size: Int, binding: Int): UniformBuffer {
            return when (RendererAPI.getAPI()) {
                RendererPlatform.OpenGL -> OpenGLUniformBuffer(size, binding)
                RendererPlatform.None -> TODO()
            }
        }
    }
}