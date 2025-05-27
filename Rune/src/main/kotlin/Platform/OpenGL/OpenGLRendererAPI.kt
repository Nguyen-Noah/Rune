package rune.platforms.opengl

import glm_.mat4x4.Mat4
import glm_.vec3.Vec3
import glm_.vec4.Vec4
import org.lwjgl.opengl.GL45.*
import org.lwjgl.system.MemoryUtil
import rune.renderer.Renderer
import rune.renderer.RendererAPI
import rune.renderer.gpu.*
import rune.renderer.renderer2d.FLOAT_MAT4_SIZE
import rune.renderer.renderer3d.Model
import rune.scene.DirectionalLight
import rune.scene.SceneLights

class OpenGLRendererAPI : RendererAPI {
    // TODO: find a better place for this (maybe per model)
    private val transformBuf = UniformBuffer.create(FLOAT_MAT4_SIZE, U_TRANSFORM)
    private val matBuf = UniformBuffer.create(48, U_MATERIAL)

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
            // 1. binding the shader
            sm.material.shader.bind()

            // 2. bind the texture
            sm.material.textures.forEachIndexed { i, tex ->
                tex?.bind(i)
            }

            // 3. bind the PBR materials
            MemoryUtil.memAlloc(48).apply {
                putFloat(sm.material.ambient.r)
                putFloat(sm.material.ambient.g)
                putFloat(sm.material.ambient.b)
                putFloat(sm.material.ambient.a)

                putFloat(sm.material.diffuse.r)
                putFloat(sm.material.diffuse.g)
                putFloat(sm.material.diffuse.b)
                putFloat(sm.material.diffuse.a)

                putFloat(sm.material.specular.r)
                putFloat(sm.material.specular.g)
                putFloat(sm.material.specular.b)
                putFloat(sm.material.specular.a)

                flip()
                matBuf.setData(this)
                MemoryUtil.memFree(this)
            }

            // 4. upload the transform
            transformBuf.setData(transform)

            val byteOffset = (sm.indexOffset * Int.SIZE_BYTES).toLong()

            glDrawElementsBaseVertex(
                GL_TRIANGLES,
                sm.indexCount,
                GL_UNSIGNED_INT,
                byteOffset,
                0
            )
            sm.material.shader.unbind()
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