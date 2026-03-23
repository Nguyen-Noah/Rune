package rune.platforms.opengl

import org.lwjgl.opengl.GL45.*
import org.lwjgl.system.MemoryStack
import rune.renderer.gpu.AttachmentSpec
import rune.renderer.gpu.Framebuffer
import rune.renderer.gpu.FramebufferSpecification
import rune.renderer.SubmitRender
import rune.rhi.AttachmentFormat
import java.nio.IntBuffer

private data class GLTexFmt(val internal: Int, val base: Int, val type: Int)

private val AttachmentFormat.gl: GLTexFmt
    get() = when (this) {
        AttachmentFormat.RGBA16F         -> GLTexFmt(GL_RGBA16F,         GL_RGBA,         GL_HALF_FLOAT)
        AttachmentFormat.SRGBA8          -> GLTexFmt(GL_SRGB8_ALPHA8,    GL_RGBA,         GL_UNSIGNED_BYTE)
        AttachmentFormat.RGBA8           -> GLTexFmt(GL_RGBA8,           GL_RGBA,         GL_UNSIGNED_BYTE)
        AttachmentFormat.R32I            -> GLTexFmt(GL_R32I,            GL_RED_INTEGER,  GL_INT)
        AttachmentFormat.DEPTH24STENCIL8 -> GLTexFmt(GL_DEPTH24_STENCIL8,GL_DEPTH_STENCIL,GL_UNSIGNED_INT_24_8)
    }

private val AttachmentFormat.isDepth: Boolean
    get() = when (this) {
        AttachmentFormat.DEPTH24STENCIL8 -> true
        else -> false
    }

private val AttachmentFormat.isInteger: Boolean
    get() = when (this) {
        AttachmentFormat.R32I -> true
        else -> false
    }

/** Maps depth formats to their GL attachment point. */
private val AttachmentFormat.depthAttachPoint: Int
    get() = when (this) {
        AttachmentFormat.DEPTH24STENCIL8 -> GL_DEPTH_STENCIL_ATTACHMENT
        else -> error("Not a depth format: $this")
    }

// TODO: update all rendering API to take in specifications
class GLFramebuffer(override val spec: FramebufferSpecification) : Framebuffer {
    companion object {
        val maxFramebufferSize = 8192       // TODO: get this number from the gpu
    }

    override var rendererId: Int = -1

    private val colorFormats = mutableListOf<AttachmentFormat>()
    private val colorAttachments = mutableListOf<Int>()

    private var depthAttachment: Int = 0

    init {
        spec.attachments.forEach {
            if (!it.format.isDepth)
                colorFormats += it.format
        }
        invalidate()
    }

    override fun invalidate() {
        SubmitRender("GLFbo-invalidate") {
            destroy()

            rendererId = glCreateFramebuffers()
            glBindFramebuffer(GL_FRAMEBUFFER, rendererId)

            val ms = spec.samples > 1

            // Color attachments
            spec.attachments
                .filterNot { it.format.isDepth }
                .forEachIndexed { i, texSpec ->
                    val id = glGenTextures()
                    colorAttachments += id
                    bindTexture(ms, id)
                    attachTexture(id, spec.samples, texSpec.format, spec.width, spec.height, GL_COLOR_ATTACHMENT0 + i)
                }

            // Depth attachment
            spec.attachments
                .firstOrNull { it.format.isDepth }
                ?.let { att ->
                    depthAttachment = glGenTextures()
                    bindTexture(ms, depthAttachment)
                    attachTexture(depthAttachment, spec.samples, att.format, spec.width, spec.height, att.format.depthAttachPoint)
                }

            // MRT draw-buffer setup
            when (colorAttachments.size) {
                0 -> glDrawBuffer(GL_NONE)
                1 -> glDrawBuffer(GL_COLOR_ATTACHMENT0)
                else -> {
                    require(colorAttachments.size <= 4)
                    glDrawBuffers(IntArray(colorAttachments.size) { GL_COLOR_ATTACHMENT0 + it })
                }
            }

            check(glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE) {
                "Framebuffer is incomplete!"
            }
            glBindFramebuffer(GL_FRAMEBUFFER, 0)
        }
    }

    override fun readPixel(attachmentIndex: Int, x: Int, y: Int): Int {
        require(attachmentIndex < colorAttachments.size)
        glReadBuffer(GL_COLOR_ATTACHMENT0 + attachmentIndex)

        return MemoryStack.stackPush().use { stack ->
            val buf: IntBuffer = stack.mallocInt(1)
            glReadPixels(x, y, 1, 1, GL_RED_INTEGER, GL_INT, buf)
            buf[0]
        }
    }

    override fun resize(width: Int, height: Int) {
        if (width == 0 || height == 0 || width > maxFramebufferSize || height > maxFramebufferSize)
            return

        spec.width = width
        spec.height = height
        invalidate()
    }

    override fun bind() {
        SubmitRender("GLFbo-bind") {
            glBindFramebuffer(GL_FRAMEBUFFER, rendererId)
            glViewport(0, 0, spec.width, spec.height)
        }
    }

    override fun unbind() {
        SubmitRender("GLFbo-unbind") { glBindFramebuffer(GL_FRAMEBUFFER, 0) }
    }

    override fun clearAttachment(attachmentIndex: Int, value: Int) {
        val fmt = colorFormats[attachmentIndex]
        val g = fmt.gl

        MemoryStack.stackPush().use { stack ->
            SubmitRender("GLFbo-clearAttachment") {
                if (fmt.isInteger) {
                    glClearTexImage(colorAttachments[attachmentIndex], 0, g.base, GL_INT, stack.ints(value))
                } else {
                    glClearTexImage(colorAttachments[attachmentIndex], 0, g.base, GL_FLOAT, stack.floats(value.toFloat()))
                }
            }
        }
    }

    override fun getColorAttachment(index: Int): Int = colorAttachments[index]
    override fun getColorAttachments(): List<AttachmentSpec> = spec.attachments

    override fun bindAttachment(index: Int) {
        glBindTextureUnit(index, colorAttachments[index])
    }

    override fun bindDepth(unit: Int) {
        glActiveTexture(GL_TEXTURE0 + unit)
        glBindTexture(GL_TEXTURE_2D, depthAttachment)
    }

    private fun destroy() {
        if (rendererId == -1) return
        glDeleteFramebuffers(rendererId)
        colorAttachments.forEach { glDeleteTextures(it) }
        colorAttachments.clear()
        if (depthAttachment != 0) {
            glDeleteTextures(depthAttachment)
            depthAttachment = 0
        }
        rendererId = -1
    }

    private fun textureTarget(multiSample: Boolean) =
        if (multiSample) GL_TEXTURE_2D_MULTISAMPLE else GL_TEXTURE_2D

    private fun bindTexture(multiSample: Boolean, id: Int) =
        glBindTexture(textureTarget(multiSample), id)

    /**
     * Allocates storage and attaches a texture (color or depth) to the bound FBO.
     */
    private fun attachTexture(
        id: Int, samples: Int, format: AttachmentFormat,
        w: Int, h: Int, attachPoint: Int
    ) {
        val ms = samples > 1
        val g = format.gl

        if (ms) {
            glTexImage2DMultisample(GL_TEXTURE_2D_MULTISAMPLE, samples, g.internal, w, h, false)
        } else {
            glTexStorage2D(GL_TEXTURE_2D, 1, g.internal, w, h)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        }

        glFramebufferTexture2D(GL_FRAMEBUFFER, attachPoint, textureTarget(ms), id, 0)
    }
}