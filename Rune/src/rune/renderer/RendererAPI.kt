package rune.renderer

import glm_.vec4.Vec4

enum class RendererPlatform {
    None,
    OpenGL
}

interface RendererAPI {
    fun init()
    fun setClearColor(color: Vec4)
    fun clear()
    fun drawIndexed(vao: VertexArray)
    fun setViewport(x: Int, y: Int, width: Int, height: Int)

    companion object {
        private val rendererAPI = RendererPlatform.OpenGL

        fun getAPI(): RendererPlatform {
            return rendererAPI
        }
    }
}