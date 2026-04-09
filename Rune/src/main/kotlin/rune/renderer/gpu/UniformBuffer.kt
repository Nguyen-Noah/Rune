package rune.renderer.gpu

import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import rune.platforms.opengl.GLUniformBuffer
import rune.renderer.Renderer
import rune.renderer.RendererAPI
import rune.renderer.RendererPlatform
import java.nio.ByteBuffer

interface UniformBuffer {
    val size: Int

    fun setData(data: ByteBuffer, offset: Int = 0)
    fun setData(data: Mat4, offset: Int = 0)
    fun setData(data: Vec3, offset: Int = 0)
    /** std140 `bool`: writes 0 or 1 as 32-bit (matches GLSL uniform block layout). */
    fun setData(data: Boolean, offset: Int = 0)

    companion object {
        fun create(layout: Std140Layout, binding: Int, name: String = ""): UniformBuffer =
            create(layout.size, binding, name)

        fun create(size: Int, binding: Int, name: String = ""): UniformBuffer {
            return when (Renderer.getAPI()) {
                RendererPlatform.OpenGL -> GLUniformBuffer(size, binding, name)
                RendererPlatform.None -> TODO()
            }
        }
    }
}

fun UniformBuffer.setData(data: Mat4, layout: Std140Layout, member: String) =
    setData(data, layout[member])

fun UniformBuffer.setData(data: Vec3, layout: Std140Layout, member: String) =
    setData(data, layout[member])

fun UniformBuffer.setData(data: Boolean, layout: Std140Layout, member: String) =
    setData(data, layout[member])
