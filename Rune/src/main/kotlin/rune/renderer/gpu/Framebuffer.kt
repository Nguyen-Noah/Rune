package rune.renderer.gpu

import rune.platforms.opengl.OpenGLFramebuffer
import rune.renderer.Renderer
import rune.renderer.RendererPlatform

interface Framebuffer {
    var rendererId: Int

    fun invalidate()
    fun resize(width: Int, height: Int)
    fun bind()
    fun unbind()
    fun getSpecification(): FramebufferSpecification
    fun getColorAttachment(index: Int = 0): Int
    fun readPixel(attachmentIndex: Int, x: Int, y: Int): Int
    fun clearAttachment(attachmentIndex: Int, value: Int)

    companion object {
        fun create(spec: FramebufferSpecification): Framebuffer {
            when (Renderer.getAPI()) {
                RendererPlatform.OpenGL -> return OpenGLFramebuffer(spec)
                RendererPlatform.None -> TODO()
            }
        }
    }
}

