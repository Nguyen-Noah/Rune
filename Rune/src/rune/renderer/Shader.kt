package rune.renderer

import rune.platforms.opengl.OpenGLShader

interface Shader {
    fun bind()
    fun unbind()
    fun getName(): String

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