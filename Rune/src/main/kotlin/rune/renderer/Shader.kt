package rune.renderer

import glm_.mat4x4.Mat4
import glm_.vec2.Vec2
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import rune.platforms.opengl.OpenGLShader

class UniformUploader(private val shader: Shader) {
    fun uniform(name: String, value: Float) = shader.uploadUniform(name, value)
    fun uniform(name: String, value: Int) = shader.uploadUniform(name, value)
    fun uniform(name: String, values: IntArray) = shader.uploadUniform(name, values)
    fun uniform(name: String, value: Vec2) = shader.uploadUniform(name, value)
    fun uniform(name: String, value: Vec3) = shader.uploadUniform(name, value)
    fun uniform(name: String, value: Vec4) = shader.uploadUniform(name, value)
    fun uniform(name: String, value: Mat4) = shader.uploadUniform(name, value)
}

abstract class Shader {
    abstract fun bind()
    abstract fun unbind()
    abstract fun getName(): String
    open fun uploadUniform(block: UniformUploader.() -> Unit) {
        val uploader = UniformUploader(this)
        uploader.block()
    }

    abstract fun uploadUniform(name: String, value: Float)
    abstract fun uploadUniform(name: String, value: Int)
    abstract fun uploadUniform(name: String, values: IntArray)
    abstract fun uploadUniform(name: String, value: Vec2)
    abstract fun uploadUniform(name: String, value: Vec3)
    abstract fun uploadUniform(name: String, value: Vec4)
    abstract fun uploadUniform(name: String, value: Mat4)

    companion object {
        fun create(name: String, vertexSrc: String, fragmentSrc: String): Shader {
            return when (Renderer.getAPI()) {
                RendererPlatform.OpenGL -> OpenGLShader(name, vertexSrc, fragmentSrc)
                RendererPlatform.None -> TODO()
            }
        }
        fun create(path: String): Shader {
            return when (Renderer.getAPI()) {
                RendererPlatform.OpenGL -> OpenGLShader(path)
                RendererPlatform.None -> TODO()
            }
        }
    }
}