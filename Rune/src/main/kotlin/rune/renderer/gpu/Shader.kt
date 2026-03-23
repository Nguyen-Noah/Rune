package rune.renderer.gpu

import rune.platforms.opengl.OpenGLShader
import rune.renderer.Renderer
import rune.renderer.RendererPlatform

data class UniformInfo(
    val name:    String,
    val size:    Int,
    val binding: Int
)

data class SamplerInfo(
    val name:    String,
    val binding: Int,
    val set:     Int
)

data class SsboInfo(
    val name:    String,
    val binding: Int
)

data class ShaderReflection(
    val ubos:    Map<String, UniformInfo> = emptyMap(),
    val samplers: Map<String, SamplerInfo> = emptyMap(),
    val ssbos:   Map<String, SsboInfo>    = emptyMap()
)

abstract class Shader {
    abstract fun bind()
    abstract fun unbind()
    abstract fun getName(): String

    abstract val reflection: ShaderReflection

    fun uboBinding(name: String): Int =
        reflection.ubos[name]?.binding
            ?: error("Shader '${getName()}': no UBO named '$name'")

    fun samplerBinding(name: String): Int =
        reflection.samplers[name]?.binding
            ?: error("Shader '${getName()}': no sampler named '$name'")

    fun ssboBinding(name: String): Int =
        reflection.ssbos[name]?.binding
            ?: error("Shader '${getName()}': no SSBO named '$name'")

    companion object {
        internal var currentProgram = 0

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