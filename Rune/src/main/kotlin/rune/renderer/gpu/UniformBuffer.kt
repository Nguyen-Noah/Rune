package rune.renderer.gpu

import glm_.mat4x4.Mat4
import rune.platforms.opengl.GLUniformBuffer
import rune.renderer.RendererAPI
import rune.renderer.RendererPlatform
import java.nio.ByteBuffer

enum class UType(val bytes: Int, val align: Int) {
    Float(4, 4),
    Vec2(8, 8),
    Vec3(12, 16),
    Vec4(16, 16),
    Mat3(36, 64),
    Mat4(64, 64),
}

interface UniformBuffer {
    fun setData(data: ByteBuffer, offset: Int = 0)
    fun setData(data: Mat4, offset: Int = 0)

    companion object {
        fun create(size: Int, binding: Int, name: String = ""): UniformBuffer {
            return when (RendererAPI.getAPI()) {
                RendererPlatform.OpenGL -> GLUniformBuffer(size, binding, name)
                RendererPlatform.None -> TODO()
            }
        }
    }
}
