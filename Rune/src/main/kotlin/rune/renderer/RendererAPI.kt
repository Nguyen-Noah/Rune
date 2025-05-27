package rune.renderer

import glm_.mat4x4.Mat4
import glm_.vec4.Vec4
import rune.renderer.gpu.VertexArray
import rune.renderer.renderer3d.Model
import rune.scene.SceneLights

enum class RendererPlatform {
    None,
    OpenGL
}

interface RendererAPI {
    fun init()
    fun setClearColor(color: Vec4)
    fun clear()
    fun drawIndexed(vao: VertexArray, indexCount: Int = 0)
    fun setViewport(x: Int, y: Int, width: Int, height: Int)
    fun drawLines(vao: VertexArray, vertexCount: Int)
    fun setLineWidth(width: Float)
    fun renderStaticMesh(model: Model, transform: Mat4, entityId: Int = -1)

    fun beginRenderPass()
    fun endRenderPass()

    fun renderGeometry()

    companion object {
        private val rendererAPI = RendererPlatform.OpenGL

        fun getAPI(): RendererPlatform {
            return rendererAPI
        }
    }
}