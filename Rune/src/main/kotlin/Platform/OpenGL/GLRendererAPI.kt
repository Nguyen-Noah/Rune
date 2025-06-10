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
import rune.renderer.renderer3d.Mesh
import rune.renderer.SubmitRender
import rune.renderer.renderer3d.MeshFactory
import rune.renderer.renderer3d.ibo
import rune.renderer.renderer3d.vbo
import rune.rhi.*

class GLRendererAPI : RendererAPI {

    private val fullscreenQuad = RendererAPI.FullscreenQuad(
        vbo = VertexBuffer.create(
            floatArrayOf(
                -1f,           -1f,          0f,      0f, 0f,
                -1f + 2f,      -1f,          0f,      1f, 0f,
                -1f + 2f,      -1f + 2f,     0f,      1f, 1f,
                -1f,           -1f + 2f,     0f,      0f, 1f,
            )
        ),
        ibo = IndexBuffer.create(listOf(0, 1, 2, 2, 3, 0))
    )

    // TODO: find a better place for this (maybe per model)
    private val transformBuf: UniformBuffer = UniformBuffer.create(FLOAT_MAT4_SIZE, U_TRANSFORM, "Transform")
    private val matBuf: UniformBuffer = UniformBuffer.create(48, U_MATERIAL, "Material")

    private var activePass: RenderPass? = null

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

    override fun renderStaticMesh(pipeline: Pipeline, mesh: Mesh, transform: Mat4) {
        pipeline.bind()
        (pipeline as GLPipeline).apply { attachVertexBuffer(mesh.buffers.vbo.rendererID) }
        mesh.buffers.ibo.bind()

        transformBuf.setData(transform)

        mesh.subMeshes.forEach { sm ->
            sm.material.shader.bind()

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

            SubmitRender("GLAPI-RenderStaticMesh") {
                sm.material.textures.forEachIndexed { i, tex -> tex?.bind(i) }

                val byteOffset = (sm.indexOffset * Int.SIZE_BYTES).toLong()

                glDrawElementsBaseVertex(
                    GL_TRIANGLES,
                    sm.indexCount,
                    GL_UNSIGNED_INT,
                    byteOffset,
                    0
                )
            }
        }
    }

    override fun beginRenderPass(pass: RenderPass, clear: Boolean) {
        activePass = pass
        //pass.spec.targetFramebuffer.bind()
        (pass as GLRenderPass).bind()

        if (clear) {
            SubmitRender("GLAPI-BeginPass-Clear") {
                Renderer.setClearColor(Vec4(0.1f, 0.7f, 0.1f, 1.0f))   // TODO: put this in the pass
                Renderer.clear()
            }
        }
    }

    override fun endRenderPass() {
        require(activePass != null)
        (activePass!! as GLRenderPass).unbind()
    }

    override fun createEnvironmentMap(file: String): Texture {
        val shader = Renderer.getShader("EquirectangularToSkybox")

        // TODO: RendererConfig
        val size = 1024
        val spec = TextureSpec(
            format = AttachmentFormat.RGBA16F,
            width = size,
            height = size,
            filter = Filter.LINEAR
        )
        val envMap = TextureCube.create(spec)
        val srcTex = Texture2D.create("assets/skyboxes/$file")

        val pipeline = ComputePipeline.create(shader)

        pipeline.begin()

        SubmitRender("GLAPI-EnvMapBind") {
            srcTex.bind(1)
            (envMap as GLTextureCube).open()
        }
        pipeline.dispatch(groupsX = size / 8, groupsY = size / 4, groupsZ = 6)
        //pipeline.end()

        return envMap
    }

    override fun renderSkybox(pass: RenderPass, envMap: Texture) {
        SubmitRender("GLAPI-depthFunc") {
            glDepthFunc(GL_LEQUAL)
            glDepthMask(false)
        }
        
        pass.spec.pipeline.bind()
        (pass.spec.pipeline as GLPipeline).attachVertexBuffer(fullscreenQuad.vbo.rendererID)
        (pass.spec.pipeline as GLPipeline).spec.shader.bind()

        fullscreenQuad.ibo.bind()

        SubmitRender("GLAPI-bindEnv") { envMap.bind(slot = GL_TEXTURE_CUBE_MAP) }

        SubmitRender("GLAPI-DrawSkybox") {
            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0)
        }

        SubmitRender("GLAPI-depthFunc") {
            glDepthFunc(GL_LESS)
            glDepthMask(true)
        }
    }
}