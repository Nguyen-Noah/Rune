package rune.platforms.opengl

import org.lwjgl.opengl.GL45.*
import rune.renderer.pipeline.Pipeline
import rune.renderer.pipeline.PipelineSpec

class OpenGLPipeline(override val spec: PipelineSpec) : Pipeline {
    val shader = spec.shader
    val vao = spec.vao
    val fbo = spec.targetFramebuffer

    fun invalidate() {
        shader.bind()

        vao.bind()

        glBindFramebuffer(GL_FRAMEBUFFER, fbo.rendererId)
    }
}