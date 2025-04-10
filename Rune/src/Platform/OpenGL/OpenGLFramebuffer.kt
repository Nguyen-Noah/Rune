package rune.platforms.opengl

import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL45.*
import org.lwjgl.system.MemoryUtil
import rune.renderer.Framebuffer
import rune.renderer.FramebufferSpecification

// TODO: update all rendering API to take in specifications
class OpenGLFramebuffer(private val spec: FramebufferSpecification) : Framebuffer {
    private var rendererId: Int = 0
    private var colorAttachment: Int = 0
    private var depthAttachment: Int = 0

    init {
        invalidate()
    }

    override fun invalidate() {
        rendererId = glCreateFramebuffers()
        glBindFramebuffer(GL_FRAMEBUFFER, rendererId)

        colorAttachment = glCreateTextures(GL_TEXTURE_2D)
        glBindTexture(GL_TEXTURE_2D, colorAttachment)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, spec.width, spec.height, 0, GL_RGBA, GL_UNSIGNED_BYTE, MemoryUtil.NULL)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)

        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, colorAttachment, 0)

        // depth buffer
        depthAttachment = glCreateTextures(GL_TEXTURE_2D)
        glBindTexture(GL_TEXTURE_2D, depthAttachment)
        glTexStorage2D(GL_TEXTURE_2D, 1, GL_DEPTH24_STENCIL8, spec.width, spec.height)
        //glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH24_STENCIL8, spec.width, spec.height, 0, GL_DEPTH_STENCIL, GL_UNSIGNED_INT_24_8, MemoryUtil.NULL)
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_TEXTURE_2D, depthAttachment, 0)

        // check framebuffer
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            println("Framebuffer is incomplete")

        glBindFramebuffer(GL_FRAMEBUFFER, 0)
    }

    override fun bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, rendererId)
    }

    override fun unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0)
    }

    override fun getSpecification(): FramebufferSpecification = spec
    override fun getColorAttachment(): Int = colorAttachment
}