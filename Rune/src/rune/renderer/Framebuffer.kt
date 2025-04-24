package rune.renderer

import rune.platforms.opengl.OpenGLFramebuffer

interface Framebuffer {
    fun invalidate()
    fun resize(width: Int, height: Int)
    fun bind()
    fun unbind()
    fun getSpecification(): FramebufferSpecification
    fun getColorAttachment(index: Int = 0): Int

    companion object {
        fun create(spec: FramebufferSpecification): Framebuffer {
            when (Renderer.getAPI()) {
                RendererPlatform.OpenGL -> return OpenGLFramebuffer(spec)
                RendererPlatform.None -> TODO()
            }
        }
    }
}

