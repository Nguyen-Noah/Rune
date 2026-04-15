package rune.platforms.opengl

import glm_.mat4x4.Mat4
import glm_.vec4.Vec4
import org.lwjgl.opengl.GL45.*
import org.lwjgl.system.MemoryUtil
import rune.project.ProjectManager
import rune.renderer.Renderer
import rune.renderer.RendererAPI
import rune.renderer.TextureType
import rune.renderer.gpu.*
import rune.renderer.renderer3d.Mesh
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
    private val transformBuf: UniformBuffer = UniformBuffer.create(Std140Layouts.Transform, U_TRANSFORM, "Transform")
    private val matBuf: UniformBuffer = UniformBuffer.create(Std140Layouts.PbrMaterial, U_MATERIAL, "Material")
    private val geometryUvBuf: UniformBuffer =
        UniformBuffer.create(Std140Layouts.GeometryUvSets, U_GEOMETRY_UV, "GeometryUvSets")

    private var activePass: RenderPass? = null

    override fun init() {
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        glEnable(GL_DEPTH_TEST)

        // TODO: linear -> sRGB on write
        // glEnable(GL_FRAMEBUFFER_SRGB)
    }

    override fun setClearColor(color: Vec4) {
        glClearColor(color.r, color.g, color.b, color.a)
    }

    override fun clear() {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
    }

    override fun drawIndexed(pass: RenderPass, indexCount: Int) {
        pass.bind()
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0L)
    }

    override fun setViewport(x: Int, y: Int, width: Int, height: Int) {
        glViewport(x, y, width, height)
    }

    override fun drawLines(pass: RenderPass, vertexCount: Int) {
        pass.bind()
        glDrawArrays(GL_LINES, 0, vertexCount)
    }

    override fun setLineWidth(width: Float) {
        glLineWidth(width)
    }

    override fun renderStaticMesh(pipeline: Pipeline, mesh: Mesh, transform: Mat4) {
        // TODO: redo this -> maybe make the model own its own pipeline
        //(pipeline as GLPipeline).attachVertexBuffer(mesh.buffers.vbo.rendererID)

        transformBuf.setData(transform, Std140Layouts.Transform, "u_ModelTransform")
        pipeline.shader.bind()
        mesh.subMeshes.forEach { sm ->

            MemoryUtil.memAlloc(matBuf.size).apply {
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

            RenderCommandQueue.enqueue("GLAPI-PushUV") {
                val albedoUv   = sm.material.textureUvChannel[TextureType.Albedo.ordinal]
                val normalUv   = sm.material.textureUvChannel[TextureType.Normal.ordinal]
                val specularUv = sm.material.textureUvChannel[TextureType.Specular.ordinal]

                MemoryUtil.memAlloc(geometryUvBuf.size).apply {
                    putInt(albedoUv)
                    putInt(normalUv)
                    putInt(specularUv)
                    position(12)
                    putInt(0)
                    flip()
                    geometryUvBuf.setData(this)
                    MemoryUtil.memFree(this)
                }
            }

            RenderCommandQueue.enqueue("GLAPI-RenderStaticMesh") {
                pipeline.bind()
                pipeline.attachVBO(mesh.buffers.vbo)
                mesh.buffers.ibo.bind()

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
        // Bind/clear must be deferred like draw commands: sync bind + deferred draws would leave
        // the default framebuffer bound when flush runs after endRenderPass unbinds.
        RenderCommandQueue.enqueue("GLAPI-BeginPass-Bind") {
            pass.bind()
        }
        if (clear) {
            RenderCommandQueue.enqueue("GLAPI-BeginPass-Clear") {
                Renderer.setClearColor(Vec4(0.0f, 0.0f, 0.0f, 1.0f))   // TODO: put this in the pass
                Renderer.clear()
            }
        }
    }

    override fun endRenderPass() {
        val pass = activePass ?: error("endRenderPass without matching beginRenderPass")
        activePass = null
        RenderCommandQueue.enqueue("GLAPI-EndPass-Unbind") {
            pass.unbind()
        }
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
        val hdrPath = if (ProjectManager.isOpen) {
            ProjectManager.current.skyboxes.resolve(file).toString()
        } else {
            "assets/skyboxes/$file"
        }
        val srcTex = Texture2D.create(hdrPath)

        val pipeline = ComputePipeline.create(shader)

        pipeline.begin()
        RenderCommandQueue.enqueue("GLAPI-EnvMapBind") {
            srcTex.bind(1)
            (envMap as GLTextureCube).open()
            pipeline.dispatch(groupsX = size / 8, groupsY = size / 4, groupsZ = 6)
            pipeline.end()
        }

        return envMap
    }

    override fun renderSkybox(pass: RenderPass, envMap: Texture) {

        RenderCommandQueue.enqueue("GLAPI-bindSkybox") {
            pass.spec.pipeline.bind()
            pass.spec.pipeline.attachVBO(fullscreenQuad.vbo)
            fullscreenQuad.ibo.bind()
        }
        pass.spec.pipeline.spec.shader.bind()


        RenderCommandQueue.enqueue("Render-Skybox") {
            glDepthFunc(GL_LEQUAL)
            glDepthMask(false)

            envMap.bind(slot = GL_TEXTURE_CUBE_MAP)
            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0)

            glDepthFunc(GL_LESS)
            glDepthMask(true)
        }
    }

    override fun submitFullscreenQuad(pipeline: Pipeline) {
        val pass = activePass ?: error("submitFullscreenQuad requires an active render pass (call beginRenderPass first)")
        RenderCommandQueue.enqueue("GLAPI-FullscreenQuadPrep") {
            pass.bind()
            pipeline.attachVBO(fullscreenQuad.vbo)
            fullscreenQuad.ibo.bind()
        }
        pipeline.spec.shader.bind()

        RenderCommandQueue.enqueue("GLAPI-SubmitFullscreenQuad") {
            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0)
        }
    }

    override fun toggleWireframe(mode: PolygonMode) {
        RenderCommandQueue.enqueue { glPolygonMode(GL_FRONT_AND_BACK, mode.gl) }
    }

    override fun enableBlend() {
        RenderCommandQueue.enqueue { glEnable(GL_BLEND) }
    }

    override fun disableBlend() {
        RenderCommandQueue.enqueue { glDisable(GL_BLEND) }
    }
}