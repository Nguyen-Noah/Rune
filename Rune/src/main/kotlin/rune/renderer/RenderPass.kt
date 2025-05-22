package rune.renderer

import rune.platforms.opengl.OpenGLRenderPass

interface RenderPass {
    fun begin()
    fun end()

    companion object {
        fun create(spec: RenderPassSpec): RenderPass {
            return when (Renderer.getAPI()) {
                RendererPlatform.OpenGL -> OpenGLRenderPass(spec)
                RendererPlatform.None -> TODO()
            }
        }
    }
}