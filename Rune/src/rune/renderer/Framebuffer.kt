package rune.renderer

import rune.platforms.opengl.OpenGLFramebuffer

data class FramebufferSpecification(
    val width: Int,
    val height: Int,
    val samples: Int = 1,
    val swapChainTarget: Boolean = false,   // are we rendering to the screen or nah

)

interface Framebuffer {
    fun invalidate()
    fun bind()
    fun unbind()
    fun getSpecification(): FramebufferSpecification
    fun getColorAttachment(): Int

    companion object {
        fun create(spec: FramebufferSpecification): Framebuffer {
            when (Renderer.getAPI()) {
                RendererPlatform.OpenGL -> return OpenGLFramebuffer(spec)
                RendererPlatform.None -> TODO()
            }
        }
    }
}
