package rune.renderer.renderer3d

import glm_.mat4x4.Mat4
import rune.platforms.opengl.OpenGLRendererAPI
import rune.renderer.gpu.Shader

object Renderer3D {

    private val staticShader = Shader.create("assets/shaders/StaticMesh.glsl")
    private val rendererAPI = OpenGLRendererAPI()

    fun beginScene() {

    }

    fun endScene() {

    }

    fun renderStaticMesh(model: Model, transform: Mat4, entityId: Int = -1) {
        staticShader.bind()
        rendererAPI.renderStaticMesh(model, transform, entityId)
        staticShader.unbind()
    }
}