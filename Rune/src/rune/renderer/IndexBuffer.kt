package rune.renderer

import rune.platforms.opengl.OpenGLIndexBuffer

interface IndexBuffer {
    fun bind()
    fun unbind()
    abstract fun getCount(): Int

    companion object {
        fun create(indices: IntArray, count: Int): IndexBuffer {
            when (Renderer.getAPI()) {
                RendererPlatform.OpenGL -> return OpenGLIndexBuffer(indices, count)
                RendererPlatform.None -> TODO()
            }
        }
    }
}