package rune.renderer.gpu

import glm_.mat3x3.Mat3
import glm_.mat4x4.Mat4
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import rune.platforms.opengl.OpenGLUniformBuffer
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
        fun create(size: Int, binding: Int): UniformBuffer {
            return when (RendererAPI.getAPI()) {
                RendererPlatform.OpenGL -> OpenGLUniformBuffer(size, binding)
                RendererPlatform.None -> TODO()
            }
        }
    }
}
