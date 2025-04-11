package rune.platforms.opengl

import org.lwjgl.opengl.GL45.*
import org.lwjgl.system.MemoryUtil
import rune.renderer.Framebuffer
import rune.renderer.FramebufferSpecification

// TODO: update all rendering API to take in specifications
class OpenGLFramebuffer(private val spec: FramebufferSpecification) : Framebuffer {
    companion object {
        val maxFramebufferSize = 8192       // TODO: get this number from the gpu
    }
    private var rendererId: Int = -1
    private var colorAttachment: Int = 0
    private var depthAttachment: Int = 0

    init {
        invalidate()
    }

    override fun invalidate() {
        if (rendererId != -1) {
            glDeleteFramebuffers(rendererId)
            glDeleteTextures(colorAttachment)
            glDeleteTextures(depthAttachment)
        }

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
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_STENCIL_ATTACHMENT, GL_TEXTURE_2D, depthAttachment, 0)

        // check framebuffer
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE)
            println("Framebuffer is incomplete")

        glBindFramebuffer(GL_FRAMEBUFFER, 0)
    }

    override fun resize(width: Int, height: Int) {
        if (width == 0 || height == 0 || width > maxFramebufferSize || height > maxFramebufferSize)
            return

        spec.width = width
        spec.height = height
        invalidate()
    }

    override fun bind() {
        glBindFramebuffer(GL_FRAMEBUFFER, rendererId)
        glViewport(0, 0, spec.width, spec.height)
    }

    override fun unbind() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0)
    }

    override fun getSpecification(): FramebufferSpecification = spec
    override fun getColorAttachment(): Int = colorAttachment
}