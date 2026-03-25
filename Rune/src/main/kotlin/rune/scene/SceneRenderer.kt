package rune.scene

import rune.components.DirectionalLightComponent
import rune.components.StaticMeshComponent
import rune.components.TransformComponent
import rune.renderer.AutoExposure
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

    val gBuffer = Framebuffer.create(framebuffer {
        width = 1280; height = 720

        attachments {
            color(AttachmentFormat.RGBA16F)   // position
            color(AttachmentFormat.RGBA16F)   // normal
            color(AttachmentFormat.RGBA8)     // albedo
            color(AttachmentFormat.RGBA8)     // material data
            depth(AttachmentFormat.DEPTH24STENCIL8)
        }
    })

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
    private lateinit var lightingPass: RenderPass

    private val exposureHistogram = AutoExposure()

    init {
        initPasses()
    }

    fun resizeViewport(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        if (width == framebuffer.spec.width && height == framebuffer.spec.height) return
        gBuffer.resize(width, height)
        framebuffer.resize(width, height)
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
            targetFramebuffer = gBuffer
            depthStencilAttachment = AttachmentFormat.DEPTH24STENCIL8
            pipeline = pipeline {
                debugName = "Geometry-Buffer"
                shader = Renderer.getShader("Geometry")
                layout = VertexLayout.build {
                    attr(0, BufferType.Float3)
                    attr(1, BufferType.Float3)
                    attr(2, BufferType.Float2)
                }
            }
        }

        //* Lighting Pass
        lightingPass = renderPass {
            debugName = "Lighting-Pass"
            targetFramebuffer = framebuffer
            pipeline = pipeline {
                debugName = "Lighting-Pass"
                shader = Renderer.getShader("Rune_PBR")
                layout = VertexLayout.build {
                    attr(0, BufferType.Float3)  // a_Position
                    attr(1, BufferType.Float2)  // a_TexCoords
                }
            }
        }
    }


    fun render(dt: Float) {
        //computePass()
        //Renderer.createEnvironmentMap("citrus_orchard_puresky_4k.hdr")

        renderGeometry()
        lightingPass()

//        try {
//            exposureHistogram.update(dt, framebuffer.getColorAttachment(), scene.viewportWidth, scene.viewportHeight)
//        } catch (_: IndexOutOfBoundsException) {
//
//        }

    }

    private fun renderGeometry() {
        Renderer.beginRenderPass(geometryPass, clear = true)

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

    private fun lightingPass() {
        Renderer.beginRenderPass(lightingPass, clear = true)

        // 1. bind the attachments from the gBuffer
        SubmitRender("SceneRenderer-bindAttachment") {
            gBuffer.bindAttachment(0)   // position
            gBuffer.bindAttachment(1)   // normal
            gBuffer.bindAttachment(2)   // specular/albedo
            gBuffer.bindDepth(3)         // depth

            envMap.bind(4)               // cube map
        }

        // 2. get the lights
        // TODO: Renderer.submitDirectionalLight, Renderer.submitSpotLight
        scene.world.family { all(DirectionalLightComponent, TransformComponent) }.forEach {
            // only supports a single light rn
            val dLight = it[DirectionalLightComponent]

            scene.lightEnvironment.light = DirectionalLight(
                dLight.color,
                dLight.diffuseIntensity,
                dLight.direction
            )
        }

        scene.lightEnvironment.bake()

        // 2. execute the shader
        Renderer.submitFullscreenQuad(lightingPass.spec.pipeline)

        Renderer.endRenderPass()
    }

    private fun render2D() {

    }
}