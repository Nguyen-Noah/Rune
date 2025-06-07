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
        AttachmentFormat.RGBA16F        -> GLTexFmt(GL_RGBA16F,        GL_RGBA,        GL_HALF_FLOAT)
        AttachmentFormat.SRGBA8         -> GLTexFmt(GL_SRGB8_ALPHA8,   GL_RGBA,        GL_UNSIGNED_BYTE)
        AttachmentFormat.RGBA8          -> GLTexFmt(GL_RGBA8,          GL_RGBA,        GL_UNSIGNED_BYTE)
        AttachmentFormat.R32I           -> GLTexFmt(GL_R32I,           GL_RED_INTEGER, GL_INT)
        AttachmentFormat.DEPTH24STENCIL8-> GLTexFmt(GL_DEPTH24_STENCIL8,GL_DEPTH_STENCIL,GL_UNSIGNED_INT_24_8)
    }

// TODO: update all rendering API to take in specifications
class GLFramebuffer(override val spec: FramebufferSpecification) : Framebuffer {
    companion object {
        val maxFramebufferSize = 8192       // TODO: get this number from the gpu
    }

    override var rendererId: Int = -1

    private val colorFormats = mutableListOf<AttachmentFormat>()
    private val colorAttachments = mutableListOf<Int>()       // render IDs (should be UInt)

    private var depthAttachment: Int = 0

    init {
        spec.attachments.forEach {
            if (!isDepthFormat(it.format))
                colorFormats += it.format
        }
        invalidate()
    }

    /**
     * Something has changes within the framebuffer, and it needs to be updated
     */
    override fun invalidate() {
        SubmitRender("GLFbo-invalidate") {
            // Delete old
            if (rendererId != -1) {
                glDeleteFramebuffers(rendererId)
                colorAttachments.forEach { glDeleteTextures(it) }
                colorAttachments.clear()
                if (depthAttachment != 0) glDeleteTextures(depthAttachment)
            }

            rendererId = glCreateFramebuffers()

            glBindFramebuffer(GL_FRAMEBUFFER, rendererId)
            val ms = spec.samples > 1

            // Color
            spec.attachments
                .filterNot { isDepthFormat(it.format) }
                .forEachIndexed { i, texSpec ->
                    val id = glGenTextures()
                    colorAttachments += id
                    bindTexture(ms, id)

                    val g = texSpec.format.gl
                    attachColorTexture(id, spec.samples, g, spec.width, spec.height, i)
                }

            // Depth
            spec.attachments
                .firstOrNull { isDepthFormat(it.format) }
                ?.let { att ->
                    depthAttachment = glGenTextures()
                    bindTexture(ms, depthAttachment)
                    attachDepthTexture(
                        depthAttachment, spec.samples,
                        GL_DEPTH24_STENCIL8, GL_DEPTH_STENCIL_ATTACHMENT,
                        spec.width, spec.height
                    )
                }

            // MRT setup
            when (colorAttachments.size) {
                0 -> glDrawBuffer(GL_NONE)
                1 -> glDrawBuffer(GL_COLOR_ATTACHMENT0)
                else -> {
                    // MRT case
                    require(colorAttachments.size <= 4)
                    val bufs = IntArray(colorAttachments.size) { GL_COLOR_ATTACHMENT0 + it }
                    glDrawBuffers(bufs)
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

        val pixel = MemoryStack.stackPush().use { stack ->
            val buf: IntBuffer = stack.mallocInt(1)

            glReadPixels(x, y, 1,1, GL_RED_INTEGER, GL_INT, buf)
            buf[0]
        }

        return pixel
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
        MemoryStack.stackPush().use { stack ->
            val clearValue: IntBuffer = stack.ints(value)
            val fmt = colorFormats[attachmentIndex].gl

            SubmitRender {
                glClearTexImage(
                    colorAttachments[attachmentIndex],
                    0,
                    fmt.base,
                    GL_INT,
                    clearValue
                )
            }
        }
    }

    override fun getColorAttachment(index: Int): Int = colorAttachments[index]
    override fun getColorAttachments(): List<AttachmentSpec> = spec.attachments


    // utils
    private fun textureTarget(multiSample: Boolean) =
        if (multiSample) GL_TEXTURE_2D_MULTISAMPLE else GL_TEXTURE_2D

    private fun createTextures(multiSample: Boolean, colorAttachments: IntArray) =
        glCreateTextures(textureTarget(multiSample), colorAttachments)

    private fun bindTexture(multiSample: Boolean, colorAttachment: Int) =
        glBindTexture(textureTarget(multiSample), colorAttachment)

    private fun attachColorTexture(
        id: Int, samples: Int, g: GLTexFmt, w: Int, h: Int, slot: Int
    ) {
        val ms = samples > 1
        if (ms) {
            glTexImage2DMultisample(GL_TEXTURE_2D_MULTISAMPLE, samples, g.internal, w, h, false)
        } else {
            glTexImage2D(GL_TEXTURE_2D, 0, g.internal, w, h, 0, g.base, g.type, 0L)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        }
        glFramebufferTexture2D(
            GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + slot, textureTarget(ms), id, 0
        )
    }

    private fun attachDepthTexture(
        depthAttachment: Int, samples: Int, glFormat: Int, attachPoint: Int,
        w: Int, h: Int
    ) {
        val ms = samples > 1
        if (ms) {
            glTexImage2DMultisample(GL_TEXTURE_2D_MULTISAMPLE, samples, glFormat, w, h, false)
        } else {
            glTexStorage2D(GL_TEXTURE_2D, 1, glFormat, w, h)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        }
        glFramebufferTexture2D(GL_FRAMEBUFFER, attachPoint, textureTarget(ms), depthAttachment, 0)
    }

    private fun isDepthFormat(format: AttachmentFormat): Boolean {
        return when (format) {
            AttachmentFormat.DEPTH24STENCIL8    -> true
            else                                        -> false
        }
    }
}
