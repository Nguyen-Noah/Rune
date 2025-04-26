package rune.renderer

import rune.platforms.opengl.OpenGLVertexBuffer
import java.nio.ByteBuffer

interface VertexBuffer {
    fun bind()
    fun unbind()
    fun getSize(): Int
    fun setData(vertices: ByteBuffer)

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