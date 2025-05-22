package rune.platforms.opengl

import glm_.mat4x4.Mat4
import glm_.vec4.Vec4
import org.lwjgl.opengl.GL45.*
import rune.renderer.Renderer
import rune.renderer.RendererAPI
import rune.renderer.gpu.VertexArray
import rune.renderer.renderer3d.Model

class OpenGLRendererAPI : RendererAPI {
    override fun init() {
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        glEnable(GL_DEPTH_TEST)
    }

    override fun setClearColor(color: Vec4) {
        glClearColor(color.r, color.g, color.b, color.a)
    }

    override fun clear() {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
    }

    override fun drawIndexed(vao: VertexArray, indexCount: Int) {
        vao.bind()
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0L)
    }

    override fun setViewport(x: Int, y: Int, width: Int, height: Int) {
        glViewport(x, y, width, height)
    }

    override fun drawLines(vao: VertexArray, vertexCount: Int) {
        vao.bind()
        glDrawArrays(GL_LINES, 0, vertexCount)
    }

    override fun setLineWidth(width: Float) {
        glLineWidth(width)
    }

    override fun renderStaticMesh(model: Model, transform: Mat4, entityId: Int) {
        model.vao.bind()

        model.mesh.subMeshes.forEach { sm ->
            // 1. bind the material
            sm.material.texture.bind()

            val byteOffset = (sm.indexOffset * Int.SIZE_BYTES).toLong()

            glDrawElementsBaseVertex(
                GL_TRIANGLES,
                sm.indexCount,
                GL_UNSIGNED_INT,
                byteOffset,
                0
            )
            Renderer.stats.drawCalls++
        }

        model.vao.unbind()
    }

    override fun beginRenderPass() {
        TODO("Not yet implemented")
    }

    override fun endRenderPass() {
        TODO("Not yet implemented")
    }

    override fun renderGeometry() {
        TODO("Not yet implemented")
    }
}