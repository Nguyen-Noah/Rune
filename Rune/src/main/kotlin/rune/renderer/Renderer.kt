package rune.renderer

import glm_.mat4x4.Mat4
import rune.platforms.opengl.OpenGLShader
import rune.renderer.RenderCommand

class Renderer {

    companion object {
        private var sceneData: SceneData? = null

        data class SceneData(
            var viewProjectionMatrix: Mat4 = Mat4(1.0)
        )

        fun init() {
            RenderCommand.init()
            Renderer2D.init()
        }

        fun getAPI() = RendererAPI.getAPI()

        fun beginScene(camera: OrthographicCamera) {
            if (sceneData == null) {
                sceneData = SceneData()
            }
            sceneData!!.viewProjectionMatrix = camera.getViewProjectionMatrix()
        }

        fun endScene() {

        }

        fun submit(shader: Shader, vao: VertexArray, transform: Mat4 = Mat4(1.0)) {
            shader.bind()
            // TODO: change this back once shaders are abstracted to different rendering platforms
            (shader as OpenGLShader).uploadUniform {
                uniform("u_ViewProjection", sceneData!!.viewProjectionMatrix)
                uniform("u_ModelMatrix", transform)
            }
            vao.bind()
            RenderCommand.drawIndexed(vao)
        }

        fun onWindowResize(width: Int, height: Int) {
            RenderCommand.setViewport(0, 0, width, height)
        }
    }
}