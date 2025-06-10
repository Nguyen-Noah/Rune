package rune.scene

import rune.components.StaticMeshComponent
import rune.components.TransformComponent
import rune.platforms.opengl.GLPipeline
import rune.renderer.Renderer
import rune.renderer.SubmitRender
import rune.renderer.gpu.*
import rune.rhi.*

data class SceneRendererSpec(
    val viewportWidth: Int,
    val viewportHeight: Int,

)

class SceneRenderer(var scene: Scene, spec: SceneRendererSpec) {
    data class Statistics(
        var drawCalls: Int = 0,
        var meshes: Int = 0,
        var totalGPUTime: Float = 0.0f
    )
    val stats = Statistics()

    val framebuffer = Framebuffer.create(framebuffer {
        width = 1280; height = 720

        attachments {
            color(AttachmentFormat.SRGBA8)
            color(AttachmentFormat.R32I)
            depth(AttachmentFormat.DEPTH24STENCIL8)
        }
    })

    //! Temp
    //private var envMap: Texture = Renderer.createEnvironmentMap("qwantani_noon_4k.hdr")
    private var envMap: Texture = Renderer.createEnvironmentMap("citrus_orchard_puresky_4k.hdr")
    //private var envMap: Texture = Renderer.createEnvironmentMap("symmetrical_garden_02_4k.hdr")

    private lateinit var skyBoxPass: RenderPass
    private lateinit var geometryPass: RenderPass

    init {
        initPasses()
    }

    private fun initPasses() {

        //* Environment Pass
        skyBoxPass = renderPass {
            debugName = "Skybox"
            targetFramebuffer = framebuffer
            pipeline = pipeline {
                debugName = "Skybox"
                shader = Renderer.getShader("Skybox")
                layout = VertexLayout.build {
                    attr(0, BufferType.Float3)
                    attr(1, BufferType.Float2)
                }
            }
        }

        //* Geometry Pass
        geometryPass = renderPass {
            debugName = "Geometry-Buffer"
            targetFramebuffer = framebuffer
            depthStencilAttachment = AttachmentFormat.DEPTH24STENCIL8
            pipeline = pipeline {
                debugName = "Geometry-Buffer"
                shader = Renderer.getShader("StaticMesh")
                layout = VertexLayout.build {
                    attr(0, BufferType.Float3)
                    attr(1, BufferType.Float3)
                    attr(2, BufferType.Float2)
                }
            }
        }
    }


    fun render(dt: Float) {
        //computePass()
        //Renderer.createEnvironmentMap("citrus_orchard_puresky_4k.hdr")
        skyBoxPass()
        renderGeometry()
    }

    private fun skyBoxPass() {
        Renderer.beginRenderPass(skyBoxPass, clear = true)
        Renderer.renderSkybox(skyBoxPass, envMap)
        Renderer.endRenderPass()
    }

    private fun renderGeometry() {
        Renderer.beginRenderPass(geometryPass)

        // TODO: s_DrawList?
        scene.world.family { all(StaticMeshComponent, TransformComponent) }.forEach {
            val model = it[StaticMeshComponent].model
            val tComp = it[TransformComponent]

            model?.let { m ->
                Renderer.renderStaticMesh(geometryPass.spec.pipeline, m.mesh, tComp.getTransform())
            }
        }

        Renderer.endRenderPass()
    }
}