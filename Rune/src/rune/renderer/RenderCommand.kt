package rune.renderer

import glm_.vec4.Vec4
import rune.platforms.opengl.OpenGLRendererAPI
import rune.renderer.VertexArray

class RenderCommand {
    companion object {
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

        fun drawIndexed(vao: VertexArray) {
            rendererAPI.drawIndexed(vao)
        }

        fun setViewport(x: Int, y: Int, width: Int, height: Int) {
            rendererAPI.setViewport(x, y, width, height)
        }
    }
}