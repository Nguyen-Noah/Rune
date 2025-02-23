package rune.renderer

import rune.rune.renderer.RenderCommand

class Renderer {


    companion object {
        fun beginScene() {
            println("Begin Scene")
        }

        fun endScene() {

        }

        fun submit(vao: VertexArray) {
            vao.bind()
            RenderCommand.drawIndexed(vao)
        }
        fun getAPI() = RendererAPI.getAPI()
    }
}