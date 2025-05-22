package rune.renderer.gpu

import rune.platforms.opengl.OpenGLVertexBuffer
import rune.renderer.Renderer
import rune.renderer.RendererPlatform
import rune.renderer.renderer3d.mesh.Vertex
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

        fun create(vertices: List<Vertex>): VertexBuffer {
            when (Renderer.getAPI()) {
                RendererPlatform.OpenGL -> return OpenGLVertexBuffer(vertices)
                RendererPlatform.None -> TODO()
            }
        }
    }
}