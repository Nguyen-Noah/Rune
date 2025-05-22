package rune.renderer

import glm_.vec4.Vec4
import rune.platforms.opengl.OpenGLRendererAPI
import rune.renderer.gpu.VertexArray

object RenderCommand {
    private val rendererAPI = OpenGLRendererAPI()

    fun init() {
        rendererAPI.init()
    }

    fun setClearColor(color: Vec4) {
        rendererAPI.setClearColor(color)
    }

    fun clear() {
        rendererAPI.clear()
    }

    fun drawIndexed(vao: VertexArray, indexCount: Int = 0) {
        rendererAPI.drawIndexed(vao, indexCount)
    }

    fun drawLines(vao: VertexArray, vertexCount: Int) {
        rendererAPI.drawLines(vao, vertexCount)
    }

    fun setLineThickness(width: Float) {
        rendererAPI.setLineWidth(width)
    }

    fun setViewport(x: Int, y: Int, width: Int, height: Int) {
        rendererAPI.setViewport(x, y, width, height)
    }
}