package rune.renderer

import rune.platform.opengl.OpenGLIndexBuffer

interface IndexBuffer {
    fun bind()
    fun unbind()
    abstract fun getCount(): Int

    companion object {
        fun create(indices: IntArray, count: Int): IndexBuffer {
            when (Renderer.getAPI()) {
                RendererAPI.OpenGL -> return OpenGLIndexBuffer(indices, count)
                RendererAPI.None -> TODO()
            }
        }
    }
}