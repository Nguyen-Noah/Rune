package rune.platforms.opengl

import org.lwjgl.opengl.GL45.*
import rune.rhi.RenderPass
import rune.rhi.RenderPassSpec

data class GLRenderPass(override val spec: RenderPassSpec) : RenderPass {
    private val fbo = spec.targetFramebuffer
    private var drawBuffers: IntArray = buildDrawBuffers(spec)
    override val height: Int = fbo.spec.height
    override val width: Int = fbo.spec.width

    override val name: String = spec.debugName


    fun bind() {
        spec.pipeline.bind()
        fbo.bind()
    }
    fun unbind() {
        spec.pipeline.unbind()
        fbo.unbind()
    }

    override fun resize(w: Int, h: Int) {
        fbo.resize(w, h)
        drawBuffers = buildDrawBuffers(spec)
    }

    override fun colorAttachmentRendererID(index: Int): Int =
        fbo.getColorAttachment(index)

    override fun render(dt: Float) {
        TODO()
    }


    private fun buildDrawBuffers(s: RenderPassSpec): IntArray {
        val colorAttachments = s.targetFramebuffer.getColorAttachments()
        return IntArray(colorAttachments.size) { i -> GL_COLOR_ATTACHMENT0 + i }
    }


    internal val glFbo get() = fbo
    internal val glDrawBuffers get() = drawBuffers
}