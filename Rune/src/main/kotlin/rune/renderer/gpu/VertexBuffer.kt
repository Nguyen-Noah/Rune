package rune.renderer.gpu

import rune.platforms.opengl.GLVertexBuffer
import rune.renderer.Renderer
import rune.renderer.RendererPlatform
import rune.renderer.renderer3d.mesh.Vertex
import java.nio.ByteBuffer

interface VertexBuffer {
    val rendererID: Int

    fun bind()
    fun unbind()
    fun setData(vertices: ByteBuffer)

    companion object {
        fun create(size: Int): VertexBuffer {
            when (Renderer.getAPI()) {
                RendererPlatform.OpenGL -> return GLVertexBuffer(size)
                RendererPlatform.None -> TODO()
            }
        }

        fun create(vertices: FloatArray, size: Int): VertexBuffer {
            when (Renderer.getAPI()) {
                RendererPlatform.OpenGL -> return GLVertexBuffer(vertices, size)
                RendererPlatform.None -> TODO()
            }
        }

        fun create(vertices: List<Vertex>): VertexBuffer {
            when (Renderer.getAPI()) {
                RendererPlatform.OpenGL -> return GLVertexBuffer(vertices)
                RendererPlatform.None -> TODO()
            }
        }
    }
}