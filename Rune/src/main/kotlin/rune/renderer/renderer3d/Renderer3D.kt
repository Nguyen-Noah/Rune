package rune.renderer.renderer3d

import glm_.mat4x4.Mat4
import rune.platforms.opengl.OpenGLRendererAPI
import rune.renderer.Renderer
import rune.renderer.gpu.Shader

object Renderer3D {
    private val rendererAPI = OpenGLRendererAPI()

    fun init() {/* TODO */}

    fun beginScene() {

    }

    fun endScene() {

    }

    fun renderStaticMesh(model: Model, transform: Mat4, entityId: Int = -1) {
        rendererAPI.renderStaticMesh(model, transform, entityId)
    }
}