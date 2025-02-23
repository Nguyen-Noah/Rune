package rune.renderer

import rune.platforms.opengl.OpenGLVertexBuffer

interface VertexBuffer {
    fun bind()
    fun unbind()
    abstract fun getSize(): Int

    companion object {
        fun create(vertices: FloatArray, size: Int): VertexBuffer {
            when (Renderer.getAPI()) {
                RendererPlatform.OpenGL -> return OpenGLVertexBuffer(vertices, size)
                RendererPlatform.None -> TODO()
            }
        }
    }
}