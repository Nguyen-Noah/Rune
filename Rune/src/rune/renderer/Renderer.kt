package rune.renderer

enum class RendererAPI {
    None,
    OpenGL
}

class Renderer {


    companion object {
        private val rendererAPI = RendererAPI.OpenGL

        fun getAPI(): RendererAPI {
            return rendererAPI
        }
    }
}