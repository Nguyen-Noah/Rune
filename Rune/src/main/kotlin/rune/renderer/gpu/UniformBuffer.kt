package rune.renderer.gpu

import glm_.mat4x4.Mat4
import rune.platforms.opengl.OpenGLUniformBuffer
import rune.renderer.RendererAPI
import rune.renderer.RendererPlatform
import java.nio.ByteBuffer

interface UniformBuffer {
    fun setData(data: ByteBuffer, offset: Int = 0)
    fun setData(data: Mat4, offset: Int = 0)

    companion object {
        fun create(size: Int, binding: Int): UniformBuffer {
            return when (RendererAPI.getAPI()) {
                RendererPlatform.OpenGL -> OpenGLUniformBuffer(size, binding)
                RendererPlatform.None -> TODO()
            }
        }
    }
}