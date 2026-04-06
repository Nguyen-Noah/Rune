package rune.scene

import rune.components.*
import rune.platforms.opengl.RenderCommandQueue
import rune.renderer.EditorCamera
import rune.renderer.Renderer
import rune.renderer.RendererAPI
import rune.renderer.RuneCamera
import rune.renderer.gpu.*
import rune.renderer.renderer2d.Renderer2D
import rune.rhi.*
import rune.terrain.TerrainRenderer

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

    val lightingBuffer = Framebuffer.create(framebuffer {
        width = 1280; height = 720

        attachments {
            color(AttachmentFormat.RGBA16F)
        }
    })

    val finalFramebuffer = Framebuffer.create(framebuffer {
        width = 1280; height = 720

        attachments {
            color(AttachmentFormat.SRGBA8)
            color(AttachmentFormat.R32I)
            depth(AttachmentFormat.DEPTH24STENCIL8)
        }
    })

    //! Temp
    private var envMap: Texture = Renderer.createEnvironmentMap("citrus_orchard_puresky_4k.hdr")

    private lateinit var skyBoxPass: RenderPass
    private lateinit var geometryPass: RenderPass
    private lateinit var terrainPipeline: Pipeline
    private lateinit var lightingPass: RenderPass
    private lateinit var tonemapPass: RenderPass
    private lateinit var composite2DPass: RenderPass

    init {
        initPasses()
    }

    fun resizeViewport(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        if (width == finalFramebuffer.spec.width && height == finalFramebuffer.spec.height) return
        gBuffer.resize(width, height)
        lightingBuffer.resize(width, height)
        finalFramebuffer.resize(width, height)
        Renderer2D.resize(width, height)
    }

    private fun initPasses() {

        //* Environment Pass
        skyBoxPass = renderPass {
            debugName = "Skybox"
            targetFramebuffer = lightingBuffer
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

        terrainPipeline = pipeline {
            debugName = "Terrain-GBuffer"
            shader = Renderer.getShader("Terrain")
            layout = VertexLayout.build {
                attr(0, BufferType.Float3)
                attr(1, BufferType.Float3)
                attr(2, BufferType.Float2)
            }
        }

        //* Lighting Pass
        lightingPass = renderPass {
            debugName = "Lighting-Pass"
            targetFramebuffer = lightingBuffer
            pipeline = pipeline {
                debugName = "Lighting-Pass"
                shader = Renderer.getShader("Rune_PBR")
                layout = VertexLayout.build {
                    attr(0, BufferType.Float3)  // a_Position
                    attr(1, BufferType.Float2)  // a_TexCoords
                }
            }
        }

        //* Final tonemap/gamma Pass
        tonemapPass = renderPass {
            debugName = "Tonemap-Pass"
            targetFramebuffer = finalFramebuffer
            drawOnlyFirstColorAttachment = true
            pipeline = pipeline {
                debugName = "Tonemap-Pass"
                shader = Renderer.getShader("Tonemap")
                depth = DepthState(test = false, write = false)
                layout = VertexLayout.build {
                    attr(0, BufferType.Float3)
                    attr(1, BufferType.Float2)
                }
            }
        }

        composite2DPass = renderPass {
            debugName = "Composite2D-Pass"
            targetFramebuffer = finalFramebuffer
            drawOnlyFirstColorAttachment = true
            pipeline = pipeline {
                debugName = "Composite2D-Pass"
                shader = Renderer.getShader("Composite2D")
                depth = DepthState(test = false, write = false)
                layout = VertexLayout.build {
                    attr(0, BufferType.Float3)
                    attr(1, BufferType.Float2)
                }
            }
        }
    }


    fun render(dt: Float, camera: EditorCamera, debugRender: Boolean) {
        Renderer.beginScene(camera)

        renderGeometry(debugRender)
        lightingPass()
        tonemapPass()
        test()
        Renderer.endScene()

        composite2DPass()
    }

    private fun renderGeometry(debugRender: Boolean = false) {
        Renderer.beginRenderPass(geometryPass, clear = true)

        if (debugRender)
            Renderer.toggleWireframe(PolygonMode.LINE)

        // TODO: s_DrawList?
        scene.world.family { all(StaticMeshComponent, TransformComponent) }.forEach {
            val model = it[StaticMeshComponent].model
            val tComp = it[TransformComponent]

            model?.let { m ->
                Renderer.renderStaticMesh(geometryPass.spec.pipeline, m.mesh, tComp.getTransform())
            }
        }

        TerrainRenderer.render(scene, terrainPipeline)

        if (debugRender)
            Renderer.toggleWireframe(PolygonMode.FILL)

        Renderer.endRenderPass()
    }

    private fun lightingPass() {
        Renderer.beginRenderPass(lightingPass, clear = true)

        gBuffer.bindAttachment(0)
        gBuffer.bindAttachment(1)
        gBuffer.bindAttachment(2)
        gBuffer.bindDepth(3)
        envMap.bind(4)

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

    private fun tonemapPass() {
        Renderer.beginRenderPass(tonemapPass)

        lightingBuffer.bindAttachment(0)

        Renderer.submitFullscreenQuad(tonemapPass.spec.pipeline)

        Renderer.endRenderPass()
    }

    private fun composite2DPass() {
        Renderer.beginRenderPass(composite2DPass, clear = false)

        Renderer2D.framebuffer.bindAttachment(0)

        Renderer.submitFullscreenQuad(composite2DPass.spec.pipeline)

        Renderer.endRenderPass()
    }

    private fun test() {
        scene.world.family { all(SpriteRendererComponent, TransformComponent) }.forEach {
            Renderer2D.drawSprite(it[TransformComponent].getTransform(), it[SpriteRendererComponent], it.id)
        }
        scene.world.family { all(CircleRendererComponent, TransformComponent) }.forEach {
            Renderer2D.drawCircle(it[TransformComponent].getTransform(), it[CircleRendererComponent], it.id)
        }
    }
}