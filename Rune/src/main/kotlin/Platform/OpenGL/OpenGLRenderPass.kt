package rune.platforms.opengl

import org.lwjgl.opengl.GL45.*
import rune.renderer.RenderPass
import rune.renderer.RenderPassSpec

class OpenGLRenderPass(private val spec: RenderPassSpec): RenderPass {
    private val fbo = spec.pipeline.spec.targetFramebuffer

    override fun begin() {
        // * Setting the viewport
        glBindFramebuffer(GL_FRAMEBUFFER, fbo.rendererId)
        val fboSpec = fbo.getSpecification()
        glViewport(0, 0, fboSpec.width, fboSpec.height)

        // * Clearing the buffer
        glClearColor(spec.clearColor.r, spec.clearColor.g, spec.clearColor.b, spec.clearColor.a)
        var mask = GL_COLOR_BUFFER_BIT
        if (spec.clearDepth) mask = mask or GL_DEPTH_BUFFER_BIT
        glClear(mask)

        // * Checking for Depth Buffer
        if (spec.pipeline.spec.enableDepthTest)
            glEnable(GL_DEPTH_TEST)
        else
            glDisable(GL_DEPTH_TEST)

        // * Binding the shader
        spec.pipeline.spec.shader.bind()
        spec.pipeline.spec.vao.bind()
    }

    override fun end() {
        glBindFramebuffer(GL_FRAMEBUFFER, 0)
    }
}