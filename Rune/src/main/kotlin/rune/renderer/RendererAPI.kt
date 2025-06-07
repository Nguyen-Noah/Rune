package rune.renderer

import glm_.mat4x4.Mat4
import glm_.vec4.Vec4
import rune.renderer.gpu.VertexArray
import rune.renderer.renderer3d.Mesh
import rune.renderer.renderer3d.Model
import rune.rhi.Pipeline
import rune.rhi.RenderPass
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
    fun renderStaticMesh(pipeline: Pipeline, mesh: Mesh, transform: Mat4)

    fun beginRenderPass(pass: RenderPass, clear: Boolean = false)
    fun endRenderPass()

    companion object {
        private val rendererAPI = RendererPlatform.OpenGL

        fun getAPI(): RendererPlatform {
            return rendererAPI
        }
    }
}