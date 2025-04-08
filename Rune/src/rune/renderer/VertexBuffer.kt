package rune.renderer

import rune.platforms.opengl.OpenGLVertexBuffer

interface VertexBuffer {
    fun bind()
    fun unbind()
    fun getSize(): Int
    fun setData(vertices: FloatArray, size: Int)

    companion object {
        fun create(size: Int): VertexBuffer {
            when (Renderer.getAPI()) {
                RendererPlatform.OpenGL -> return OpenGLVertexBuffer(size)
                RendererPlatform.None -> TODO()
            }
        }

        fun create(vertices: FloatArray, size: Int): VertexBuffer {
            when (Renderer.getAPI()) {
                RendererPlatform.OpenGL -> return OpenGLVertexBuffer(vertices, size)
                RendererPlatform.None -> TODO()
            }
        }
    }
}