package rune.renderer

import rune.platforms.opengl.OpenGLFramebuffer

interface Framebuffer {
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

