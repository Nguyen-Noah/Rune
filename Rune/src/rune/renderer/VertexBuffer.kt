package rune.renderer

import rune.platform.opengl.OpenGLVertexBuffer

interface VertexBuffer {
    fun bind()
    fun unbind()
    abstract fun getSize(): Int

    companion object {
        fun create(vertices: FloatArray, size: Int): VertexBuffer {
            when (Renderer.getAPI()) {
                RendererAPI.OpenGL -> return OpenGLVertexBuffer(vertices, size)
                RendererAPI.None -> TODO()
            }
        }
    }
}