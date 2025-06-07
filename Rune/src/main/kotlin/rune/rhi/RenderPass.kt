package rune.rhi

import rune.platforms.opengl.GLRenderPass
import rune.renderer.RendererAPI
import rune.renderer.RendererPlatform
import rune.renderer.gpu.Framebuffer

data class RenderPassSpec(
//    val width: Int,
//    val height: Int,
    val targetFramebuffer: Framebuffer,
    val pipeline: Pipeline,
    val depthStencilAttachment: AttachmentFormat? = null,
    val debugName: String = "Unnamed-Pass"
)

class RenderPassSpecBuilder {
    var targetFramebuffer: Framebuffer? = null
    var pipeline: Pipeline? = null
    var depthStencilAttachment: AttachmentFormat? = null
    var debugName: String = "Unnamed-Pass"

    fun build(): RenderPass {
        val spec = RenderPassSpec(
            targetFramebuffer = targetFramebuffer!!,
            pipeline = pipeline!!,
            depthStencilAttachment = depthStencilAttachment,
            debugName = debugName
        )

        return when(RendererAPI.getAPI()) {
            RendererPlatform.OpenGL -> GLRenderPass(spec)
            RendererPlatform.None -> TODO()
        }
    }
}


interface RenderPass {
    val spec: RenderPassSpec
    val name: String
    val width: Int
    val height: Int

    fun resize(w: Int, h: Int)
    fun colorAttachmentRendererID(index: Int = 0): Int

    fun render(dt: Float)
}

fun renderPass(spec: RenderPassSpecBuilder.() -> Unit): RenderPass =
    RenderPassSpecBuilder().apply(spec).build()