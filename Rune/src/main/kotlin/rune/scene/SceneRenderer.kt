package rune.scene

import rune.components.StaticMeshComponent
import rune.components.TransformComponent
import rune.renderer.Renderer
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
    private val gPipeline = pipeline {
        debugName = "Geometry-Buffer"
        shader = Renderer.getShader("StaticMesh")
        layout = VertexLayout.build {
            attr(0, BufferType.Float3)
            attr(1, BufferType.Float3)
            attr(2, BufferType.Float2)
        }
    }

    private val geometryPass = renderPass {
        debugName = "Geometry-Buffer"
        targetFramebuffer = framebuffer
        depthStencilAttachment = AttachmentFormat.DEPTH24STENCIL8
        pipeline = gPipeline
    }


    fun render(dt: Float) {
        //computePass()
        //skyboxPass()
        renderGeometry()
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
}