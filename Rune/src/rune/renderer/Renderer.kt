package rune.renderer

import glm_.mat4x4.Mat4
import rune.platforms.opengl.OpenGLShader
import rune.rune.renderer.RenderCommand

class Renderer {

    companion object {
        private var sceneData: SceneData? = null

        fun init() {
            RenderCommand.init()
        }

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
        fun getAPI() = RendererAPI.getAPI()

        data class SceneData(
            var viewProjectionMatrix: Mat4 = Mat4(1.0)
        )
    }
}